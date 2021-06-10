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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import org.crunchycookie.orion.worker.WorkerGrpc;
import org.crunchycookie.orion.worker.WorkerOuterClass.File;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileMetaData;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadRequest;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadResponse;
import org.crunchycookie.orion.worker.WorkerServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

@RunWith(JUnit4.class)
public class WorkerServiceTest {

  /**
   * This rule manages automatic graceful shutdown for the registered channel at the end of test.
   */
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
  private final String TASK_ID = "hello-world";
  private final String FILE_EXECUTABLE_NAME = "hello-world-task";
  private final String FILE_EXECUTABLE_TYPE = "jar";
  private final String FILE_INPUT_NAME = "in";
  private final String FILE_INPUT_TYPE = "txt";
  private WorkerServer server;
  private ManagedChannel inProcessChannel;
  private Collection<FileUploadRequest> fileUploadRequests;

  @Before
  public void setUp() throws Exception {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();
    fileUploadRequests = new ArrayList<>();
    // Use directExecutor for both InProcessServerBuilder and InProcessChannelBuilder can reduce the
    // usage timeouts and latches in test. But we still add timeout and latches where they would be
    // needed if no directExecutor were used, just for demo purpose.
    server = new WorkerServer(InProcessServerBuilder.forName(serverName).directExecutor(), 0);
    server.start();
    // Create a client channel and register for automatic graceful shutdown.
    inProcessChannel = grpcCleanup.register(
        InProcessChannelBuilder.forName(serverName).directExecutor().build());
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void uploadTest() {
    @SuppressWarnings("unchecked")
    StreamObserver<FileUploadResponse> responseObserver =
        (StreamObserver<FileUploadResponse>) mock(StreamObserver.class);
    WorkerGrpc.WorkerStub stub = WorkerGrpc.newStub(inProcessChannel);
    ArgumentCaptor<FileUploadResponse> fileUploadResponseCaptor = ArgumentCaptor
        .forClass(FileUploadResponse.class);

    // Set request and response observers.
    StreamObserver<FileUploadRequest> requestObserver = stub.upload(responseObserver);

    // Set executable in the payload.
    setMetadataFileOfTheExecutableJar(requestObserver);

    // Set executable jar in the payload.
    setTheExecutableJar(requestObserver);

    // Make sure that none of the file upload responses were called.
    verify(responseObserver, never()).onNext(any(FileUploadResponse.class));

    // Set completion of the upload.
    requestObserver.onCompleted();

    // Allow some ms to let client receive the response.
    verify(responseObserver, timeout(100)).onNext(fileUploadResponseCaptor.capture());
    FileUploadResponse summary = fileUploadResponseCaptor.getValue();
    Assert.assertEquals(FILE_EXECUTABLE_NAME, summary.getMetadata().getName());

    // Asset that the process completed.
    verify(responseObserver, timeout(100)).onCompleted();
    // Assert none of the exceptions were thrown.
    verify(responseObserver, never()).onError(any(Throwable.class));
  }

  private void setTheExecutableJar(StreamObserver<FileUploadRequest> requestObserver) {
    // Then we set the actual executable file.
    try (InputStream executableStream = this.getClass().getClassLoader()
        .getResourceAsStream("hello-world-task.jar")) {
      int streamElementSize = 1 * 1024 * 1024;
      byte[] buffer = new byte[streamElementSize];
      try (BufferedInputStream bis = new BufferedInputStream(executableStream)) {
        while ((bis.read(buffer)) > 0) {
          File chunk = File.newBuilder().setContent(ByteString.copyFrom(buffer)).build();
          // Then the upload request.
          FileUploadRequest chunkRequest = FileUploadRequest.newBuilder()
              .setFile(chunk)
              .build();
          // Let's set chunk request in the payload.
          requestObserver.onNext(chunkRequest);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void setMetadataFileOfTheExecutableJar(
      StreamObserver<FileUploadRequest> requestObserver) {
    // Let's upload the executable. First, lets create the metadata file.
    FileMetaData executableMetadata = FileMetaData.newBuilder()
        .setName(FILE_EXECUTABLE_NAME)
        .setType(FILE_EXECUTABLE_TYPE)
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
