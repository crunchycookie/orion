/*
 * Copyright 2021 crunchycookie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.crunchycookie.orion.worker.service;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.worker.WorkerGrpc;
import org.crunchycookie.orion.worker.WorkerOuterClass;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileMetaData;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadRequest;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadResponse;
import org.crunchycookie.orion.worker.WorkerOuterClass.Result;
import org.crunchycookie.orion.worker.WorkerOuterClass.Status;
import org.crunchycookie.orion.worker.WorkerOuterClass.Task;
import org.crunchycookie.orion.worker.WorkerServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4.class)
public class WorkerServiceTest {

  private static final Logger LOG = LogManager.getLogger(WorkerServiceTest.class);

  /**
   * This rule manages automatic graceful shutdown for the registered channel at the end of test.
   */
  @ClassRule
  public static final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
  private static WorkerServer server;
  private static ManagedChannel inProcessChannel;
  private final String TASK_ID = "hello-world";
  private final String FILE_EXECUTABLE_NAME = "execute-task";
  private final String FILE_EXECUTABLE_TYPE = "sh";
  private final int K = 1024;
  private final int M = 1 * K * K;
  private final int ONE_MB_IN_BYTES = M;

  @BeforeClass
  public static void setUp() throws Exception {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();
    // Use directExecutor for both InProcessServerBuilder and InProcessChannelBuilder can reduce the
    // usage timeouts and latches in test. But we still add timeout and latches where they would be
    // needed if no directExecutor were used, just for demo purpose.
    server = new WorkerServer(InProcessServerBuilder.forName(serverName).directExecutor(), 0);
    server.start();
    // Create a client channel and register for automatic graceful shutdown.
    inProcessChannel = grpcCleanup.register(
        InProcessChannelBuilder.forName(serverName).directExecutor().build());
  }

  @AfterClass
  public static void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void step1_uploadTest() {
    LOG.info("Running upload test");
    WorkerGrpc.WorkerStub stub = WorkerGrpc.newStub(inProcessChannel);
    uploadFile("execute-task.sh", stub);
    uploadFile("in.txt", stub);
  }

  @Test(expected = Test.None.class)
  public void step2_executeTest() throws InterruptedException {
    LOG.info("Running execute test");
    WorkerGrpc.WorkerBlockingStub worker = WorkerGrpc.newBlockingStub(inProcessChannel);

    // Build the description of the task to be executed.
    Task task = Task.newBuilder().setExecutableShellScriptMetadata(FileMetaData.newBuilder()
        .setName(FILE_EXECUTABLE_NAME)
        .setType(FILE_EXECUTABLE_TYPE)
        .setTaskId(TASK_ID)
        .build()
    ).build();

    Result result = worker.execute(task);

    // Wait some time hoping that by then the task will be completed.
    Thread.sleep(2000);

    Assert.assertNotNull(result);
    Assert.assertEquals(Status.SUCCESS, result.getTaskStatus());

    assertFileExistence("out.txt");
    assertFileExistence("log.txt");
    assertFileExistence("error-log.txt");
  }

  @Test
  public void step3_monitorTest() {
    LOG.info("Running monitor test");
    WorkerGrpc.WorkerBlockingStub worker = WorkerGrpc.newBlockingStub(inProcessChannel);

    // Build the description of the task to be executed.
    Task task = Task.newBuilder().setExecutableShellScriptMetadata(FileMetaData.newBuilder()
        .setName(FILE_EXECUTABLE_NAME)
        .setType(FILE_EXECUTABLE_TYPE)
        .setTaskId(TASK_ID)
        .build()
    ).build();

    Result result = worker.monitor(task);

    Assert.assertNotNull(result);
    Assert.assertEquals(Status.NOT_EXECUTING, result.getTaskStatus());
  }

  @Test(expected = Test.None.class)
  public void step4_downloadTest() throws InterruptedException, IOException {
    LOG.info("Running download test");
    // Results are in the order of: FileMeta, ...<each-file-part>, <download-task-status>.
    List<Result> streamedResults = new ArrayList<>();
    final CountDownLatch latch = new CountDownLatch(1);
    StreamObserver<Result> responseObserver =
        new StreamObserver<Result>() {
          @Override
          public void onNext(Result result) {
            streamedResults.add(result);
          }

          @Override
          public void onError(Throwable t) {
            fail();
          }

          @Override
          public void onCompleted() {
            latch.countDown();
          }
        };
    // Build the file that requires downloading.
    FileMetaData executionResultFileMeta = FileMetaData.newBuilder()
        .setName("out")
        .setType("txt")
        .setTaskId(TASK_ID)
        .build();
    WorkerGrpc.WorkerStub worker = WorkerGrpc.newStub(inProcessChannel);

    // Download the file.
    worker.download(executionResultFileMeta, responseObserver);

    // Wait until streaming is done. Here the total time is assumed one second.
    assertTrue(latch.await(1, TimeUnit.SECONDS));

    // Assert.
    compareAndAssertReceivedFile(streamedResults, executionResultFileMeta);
  }

  private void compareAndAssertReceivedFile(List<Result> streamedResults,
      FileMetaData executionResultFileMeta)
      throws IOException {
    // Assert request and received file names.
    assertTrue(streamedResults.get(0).getOutputFileMetaData().getName()
        .equals(executionResultFileMeta.getName()));
    // Assert action results.
    assertTrue(streamedResults.get(streamedResults.size() - 1).getTaskStatus() == Status.SUCCESS);

    // Assert received file content.
    List<byte[]> receivedFileChunks = streamedResults.subList(1, streamedResults.size() - 1)
        .stream().map(r -> r.getOutputFile().getContent().toByteArray())
        .collect(Collectors.toList());

    // Write received file.
    File receivedFile = new File("temp_received_file.txt");
    if (receivedFile.exists()) {
      FileUtils.forceDelete(receivedFile);
    }
    writeReceivedFile(receivedFileChunks, receivedFile);

    // Read reference file.
    String reference = readFirstLine(getFileStreamFromTestResources("in.txt")).get();
    String received = readFirstLine(new FileInputStream(receivedFile)).get();
    Assert.assertTrue(reference.equals(received));

    // Cleanup temp file.
    FileUtils.forceDelete(receivedFile);
  }

  private InputStream getFileStreamFromTestResources(String fileName) {
    return this.getClass().getClassLoader().getResourceAsStream(fileName);
  }

  private Optional<String> readFirstLine(InputStream inputStream) {
    String referenceString = null;
    try (InputStream in = inputStream;
        Scanner sc = new Scanner(in);
    ) {
      while (sc.hasNextLine()) {
        referenceString = sc.nextLine();
        break;
      }
    } catch (IOException e) {
      fail();
    }
    return Optional.of(referenceString);
  }

  private void writeReceivedFile(List<byte[]> recievedFileChunks, File recievedFile)
      throws IOException {
    try (FileOutputStream s = new FileOutputStream(recievedFile)) {
      for (byte[] chunk : recievedFileChunks) {
        s.write(chunk);
        s.flush();
      }
    }
  }

  private void assertFileExistence(String file) {
    File out = new File("target/classes/tasks" + File.separator + TASK_ID + File.separator + file);
    assertTrue(out.exists());
  }

  private void uploadFile(String fileName, WorkerGrpc.WorkerStub stub) {
    @SuppressWarnings("unchecked")
    StreamObserver<FileUploadResponse> responseObserver =
        (StreamObserver<FileUploadResponse>) mock(StreamObserver.class);
    ArgumentCaptor<FileUploadResponse> fileUploadResponseCaptor = ArgumentCaptor
        .forClass(FileUploadResponse.class);

    // Set request and response observers.
    StreamObserver<FileUploadRequest> requestObserver = stub.upload(responseObserver);

    // Set file metadata in the payload.
    setMetadataOfFile(requestObserver, fileName);

    // Set file in the payload.
    setTheFile(requestObserver, fileName);

    // Make sure that none of the file upload responses were called.
    verify(responseObserver, never()).onNext(any(FileUploadResponse.class));

    // Set completion of the upload.
    requestObserver.onCompleted();

    // Allow some ms to let client receive the response.
    verify(responseObserver, timeout(100)).onNext(fileUploadResponseCaptor.capture());
    FileUploadResponse summary = fileUploadResponseCaptor.getValue();
    Assert.assertEquals(fileName,
        summary.getMetadata().getName() + "." + summary.getMetadata().getType());

    // Asset that the process completed.
    verify(responseObserver, timeout(100)).onCompleted();
    // Assert none of the exceptions were thrown.
    verify(responseObserver, never()).onError(any(Throwable.class));
  }

  private void setTheFile(StreamObserver<FileUploadRequest> requestObserver, String fileName) {
    // Then we set the actual executable file.
    try (InputStream executableStream = getFileFromTestResources(fileName)) {
      byte[] buffer = new byte[ONE_MB_IN_BYTES];
      try (BufferedInputStream bis = new BufferedInputStream(executableStream)) {
        int lengthOfReadBytes;
        do {
          lengthOfReadBytes = bis.read(buffer);
          if (lengthOfReadBytes > 0) {
            setTheChunk(requestObserver, Arrays.copyOfRange(buffer, 0, lengthOfReadBytes));
          }
        } while (lengthOfReadBytes > 0);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private InputStream getFileFromTestResources(String fileName) {
    return this.getClass().getClassLoader().getResourceAsStream(fileName);
  }

  private void setTheChunk(StreamObserver<FileUploadRequest> requestObserver, byte[] buffer) {
    WorkerOuterClass.File chunk = WorkerOuterClass.File.newBuilder()
        .setContent(ByteString.copyFrom(buffer)).build();
    // Then the upload request.
    FileUploadRequest chunkRequest = FileUploadRequest.newBuilder()
        .setFile(chunk)
        .build();
    // Let's set chunk request in the payload.
    requestObserver.onNext(chunkRequest);
  }

  private void setMetadataOfFile(StreamObserver<FileUploadRequest> requestObserver, String file) {
    // Let's upload the executable. First, lets create the metadata file.
    FileMetaData executableMetadata = FileMetaData.newBuilder()
        .setName(file.split("\\.")[0])
        .setType(file.split("\\.")[1])
        .setTaskId(TASK_ID)
        .build();

    // Then the upload request.
    FileUploadRequest executableMetadataRequest = FileUploadRequest.newBuilder()
        .setMetadata(executableMetadata)
        .build();

    // Let's set metadata in the payload.
    requestObserver.onNext(executableMetadataRequest);
  }
}
