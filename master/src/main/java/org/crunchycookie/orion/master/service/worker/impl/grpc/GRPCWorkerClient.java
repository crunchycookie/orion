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

package org.crunchycookie.orion.master.service.worker.impl.grpc;

import static org.crunchycookie.orion.master.constants.MasterConstants.ErrorCodes.ERROR_COM_WORKER_METHOD_INVOCATION_FAILED;
import static org.crunchycookie.orion.master.constants.MasterConstants.ErrorCodes.INTERNAL_SERVER_ERROR;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.File;
import org.crunchycookie.orion.master.models.TaskFileIterated;
import org.crunchycookie.orion.master.models.TaskFileStream;
import org.crunchycookie.orion.master.models.TaskFileMetadata;
import org.crunchycookie.orion.master.models.file.IteratingTaskFile;
import org.crunchycookie.orion.master.models.file.TaskFile;
import org.crunchycookie.orion.master.service.worker.WorkerNode.WorkerNodeStatus;
import org.crunchycookie.orion.worker.WorkerGrpc;
import org.crunchycookie.orion.worker.WorkerGrpc.WorkerBlockingStub;
import org.crunchycookie.orion.worker.WorkerGrpc.WorkerStub;
import org.crunchycookie.orion.worker.WorkerOuterClass;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileMetaData;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadRequest;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadResponse;
import org.crunchycookie.orion.worker.WorkerOuterClass.Result;
import org.crunchycookie.orion.worker.WorkerOuterClass.Status;
import org.crunchycookie.orion.worker.WorkerOuterClass.Task;

/**
 * This clas represents the client communicates with the worker via GRPC.
 */
public class GRPCWorkerClient {

  private final WorkerBlockingStub blockingStub;
  private final WorkerStub asyncStub;

  public GRPCWorkerClient(Channel channel) {

    blockingStub = WorkerGrpc.newBlockingStub(channel);
    asyncStub = WorkerGrpc.newStub(channel);
  }

  public GRPCWorkerClient(String host, String port) {

    this(ManagedChannelBuilder
        .forAddress(host, Integer.parseInt(port))
        .usePlaintext()
    );
  }

  public GRPCWorkerClient(ManagedChannelBuilder<?> channelBuilder) {
    Channel channel = channelBuilder.build();
    blockingStub = WorkerGrpc.newBlockingStub(channel);
    asyncStub = WorkerGrpc.newStub(channel);
  }

  private class ResponseCatcher<T> {

    private T response;
    private Throwable error;

    public Throwable getError() {
      return error;
    }

    public void setError(Throwable error) {
      this.error = error;
    }

    public T getResponse() {
      return response;
    }

    public void setResponse(T response) {
      this.response = response;
    }

    public boolean isSuccess() {
      return error == null && response != null;
    }
  }

  protected List<WorkerNodeStatus> upload(List<TaskFile> files) throws MasterException {

    List<WorkerNodeStatus> responses = new ArrayList<>();
    for (TaskFile file : files) {
      responses.add(uploadFile(file));
    }
    return responses;
  }

  protected WorkerNodeStatus execute(TaskFileMetadata executable) {

    FileMetaData executableMeta = FileMetaData.newBuilder()
        .setName(executable.getFileName())
        .setType(executable.getFileType())
        .setTaskId(executable.getTaskId().toString())
        .build();
    Task task = Task.newBuilder()
        .setExecutableShellScriptMetadata(executableMeta)
        .build();

    Result result = blockingStub.execute(task);

    return getWorkerNodeStatus(result.getTaskStatus());
  }

  protected WorkerNodeStatus monitor(TaskFileMetadata executable) {

    FileMetaData executableMeta = FileMetaData.newBuilder()
        .setName(executable.getFileName())
        .setType(executable.getFileType())
        .setTaskId(executable.getTaskId().toString())
        .build();
    Task task = Task.newBuilder()
        .setExecutableShellScriptMetadata(executableMeta)
        .build();

    Result result = blockingStub.monitor(task);

    return getWorkerNodeStatus(result.getTaskStatus());
  }

  protected List<TaskFile> download(List<TaskFileMetadata> filesToDownload) throws MasterException {

    List<TaskFile> downloadedFiles = new ArrayList<>();
    for (TaskFileMetadata fileToDownload : filesToDownload) {
       getDownloadedFile(fileToDownload).ifPresent(downloadedFiles::add);
    }
    return downloadedFiles;
  }

  private Optional<TaskFile> getDownloadedFile(TaskFileMetadata meta)
      throws MasterException {

    FileMetaData downloadFileMetaData = FileMetaData.newBuilder()
        .setName(meta.getFileName())
        .setType(meta.getFileType())
        .setTaskId(meta.getTaskId().toString())
        .build();
    Iterator<Result> responseItr;
    try {
      responseItr = blockingStub.download(downloadFileMetaData);
    } catch (StatusRuntimeException e) {
      throw new MasterException(ERROR_COM_WORKER_METHOD_INVOCATION_FAILED,
          "Failed to download the file due to rpc failure", e);
    }
    Status transferStatus = responseItr.next().getTaskStatus();
    if (transferStatus.equals(Status.SUCCESS)) {
      FileMetaData downloadedFileMeta = responseItr.next().getOutputFileMetaData();
      return Optional.of(new IteratingTaskFile(
          new TaskFileMetadata(
              downloadedFileMeta.getName(),
              downloadedFileMeta.getType(),
              UUID.fromString(downloadedFileMeta.getTaskId())
          ),
          responseItr
      ));
    }
    return Optional.empty();
  }

  private WorkerNodeStatus getWorkerNodeStatus(Status status) {
    return switch (status) {
      case IN_PROGRESS -> WorkerNodeStatus.EXECUTING;
      default -> WorkerNodeStatus.FAILED;
    };
  }

  private WorkerNodeStatus uploadFile(TaskFile file) throws MasterException {

    final ResponseCatcher<FileUploadResponse> responseCatcher = new ResponseCatcher<>();
    final CountDownLatch finishLatch = new CountDownLatch(1);

    // Prepare response observer.
    StreamObserver<FileUploadResponse> responseObserver = new StreamObserver<>() {
      @Override
      public void onNext(FileUploadResponse uploadResponse) {
        responseCatcher.setResponse(uploadResponse);
      }

      @Override
      public void onError(Throwable t) {
        responseCatcher.setError(t);
        finishLatch.countDown();
      }

      @Override
      public void onCompleted() {
        finishLatch.countDown();
      }
    };

    // Call worker.
    StreamObserver<FileUploadRequest> requestObserver = asyncStub.upload(responseObserver);

    try {
      // Set file metadata in the payload.
      setMetadataOfFile(requestObserver, file.getMeta(), finishLatch);

      // Set file in the payload.
      setTheFile(requestObserver, file.next(), finishLatch);

      // Mark the end of requests.
      requestObserver.onCompleted();

      // Receiving happens asynchronously
      finishLatch.await(1, TimeUnit.MINUTES);
    } catch (RuntimeException e) {
      // Cancel RPC
      requestObserver.onError(e);
      throw new MasterException(ERROR_COM_WORKER_METHOD_INVOCATION_FAILED, "File upload failed", e);
    } catch (InterruptedException e) {
      throw new MasterException(INTERNAL_SERVER_ERROR, "Error while waiting during streaming", e);
    }

    // Handle response.
    if (!responseCatcher.isSuccess() || responseCatcher.getResponse().getStatus()
        .equals(Status.FAILED)) {
      throw new MasterException(ERROR_COM_WORKER_METHOD_INVOCATION_FAILED,
          "Error occurred while uploading files to the remote worker", responseCatcher.getError());
    }
    return getWorkerNodeStatus(responseCatcher.getResponse().getStatus());
  }

  private void setMetadataOfFile(StreamObserver<FileUploadRequest> requestObserver,
      TaskFileMetadata meta, CountDownLatch finishLatch) throws InterruptedException {

    // Let's upload the executable. First, lets create the metadata file.
    FileMetaData executableMetadata = FileMetaData.newBuilder()
        .setName(meta.getFileName())
        .setType(meta.getFileType())
        .setTaskId(meta.getTaskId().toString())
        .build();

    // Then the upload request.
    FileUploadRequest executableMetadataRequest = FileUploadRequest.newBuilder()
        .setMetadata(executableMetadata)
        .build();

    // Let's set metadata in the payload.
    requestObserver.onNext(executableMetadataRequest);

    // Wait sometime before sending the next element.
    handleStreamingInterval(finishLatch);
  }

  private void handleStreamingInterval(CountDownLatch finishLatch) throws InterruptedException {

    // Sleep for a bit before sending the next one.
    Thread.sleep(ThreadLocalRandom.current().nextInt(1000) + 500);
    if (finishLatch.getCount() == 0) {
      // RPC completed or errored before we finished sending.
      // Sending further requests won't error, but they will just be thrown away.
      // Error will be handled by the onError method.
      return;
    }
  }

  private void setTheFile(StreamObserver<FileUploadRequest> requestObserver,
      InputStream fileDataStream, CountDownLatch finishLatch) throws InterruptedException {

    // Then we set the actual executable file.
    try (InputStream executableStream = fileDataStream) {
      byte[] buffer = new byte[1024 * 1024];
      try (BufferedInputStream bis = new BufferedInputStream(executableStream)) {
        int lengthOfReadBytes;
        do {
          lengthOfReadBytes = bis.read(buffer);
          if (lengthOfReadBytes > 0) {
            setTheChunk(requestObserver, Arrays.copyOfRange(buffer, 0, lengthOfReadBytes),
                finishLatch);
          }
        } while (lengthOfReadBytes > 0);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void setTheChunk(StreamObserver<FileUploadRequest> requestObserver, byte[] buffer,
      CountDownLatch finishLatch) throws InterruptedException {

    WorkerOuterClass.File chunk = WorkerOuterClass.File.newBuilder()
        .setContent(ByteString.copyFrom(buffer)).build();
    // Then the upload request.
    FileUploadRequest chunkRequest = FileUploadRequest.newBuilder()
        .setFile(chunk)
        .build();
    // Let's set chunk request in the payload.
    requestObserver.onNext(chunkRequest);

    // Wait sometime before sending the next element.
    handleStreamingInterval(finishLatch);
  }
}
