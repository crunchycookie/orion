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

package org.crunchycookie.orion.worker.service.observer;

import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.worker.WorkerOuterClass.File;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileMetaData;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadRequest;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadResponse;
import org.crunchycookie.orion.worker.WorkerOuterClass.Status;
import org.crunchycookie.orion.worker.exception.WorkerRuntimeException;
import org.crunchycookie.orion.worker.exception.WorkerServerException;
import org.crunchycookie.orion.worker.utils.WorkerUtils;

public class FileUploadRequestObserver implements StreamObserver<FileUploadRequest> {

  private static final Logger LOG = LogManager.getLogger(FileUploadRequestObserver.class);

  private FileMetaData fileMetaData = null;
  private OutputStream outputStream = null;
  private StreamObserver<FileUploadResponse> responseObserver = null;

  public FileUploadRequestObserver(StreamObserver<FileUploadResponse> responseObserver) {
    this.responseObserver = responseObserver;
  }

  @Override
  public void onNext(FileUploadRequest fileUploadRequest) {

    if (fileUploadRequest.hasMetadata()) {
      fileMetaData = fileUploadRequest.getMetadata();
      initFileStream(fileUploadRequest.getMetadata());
    }
    if (!isStreamContentReadyToWrite(fileUploadRequest)) {
      LOG.warn("Skipped the uploading the stream element: " + fileUploadRequest + ", "
          + "since storage is not initialized");
      return;
    }
    writeStreamContent(fileUploadRequest.getFile());
  }

  @Override
  public void onError(Throwable throwable) {
    LOG.error("File uploading failed", throwable);
    handleResponse(Status.FAILED);
  }

  @Override
  public void onCompleted() {
    if (closeStream()) {
      handleResponse(Status.SUCCESS);
    }
  }

  private boolean isStreamContentReadyToWrite(FileUploadRequest fileUploadRequest) {
    return outputStream != null && fileUploadRequest.hasFile();
  }

  private void handleResponse(Status failed) {
    responseObserver.onNext(
        FileUploadResponse.newBuilder()
            .setMetadata(fileMetaData)
            .setStatus(failed)
            .build()
    );
    responseObserver.onCompleted();
  }

  private boolean closeStream() {
    if (outputStream != null) {
      try {
        outputStream.close();
        return true;
      } catch (IOException e) {
        onError(new WorkerRuntimeException(
            "Failed to close the file: " + fileMetaData.getName(), e));
      }
    }
    return false;
  }

  private void writeStreamContent(File file) {
    try {
      outputStream.write(file.getContent().toByteArray());
      outputStream.flush();
    } catch (IOException e) {
      onError(new WorkerRuntimeException(
          "Failed to write the content for the file: " + fileMetaData.getName(), e));
    }
  }

  private void initFileStream(FileMetaData fileMetaData) {
    try {
      WorkerUtils.getTaskExecutionManager().store(fileMetaData).ifPresentOrElse(
          fs -> outputStream = fs,
          () -> onError(new WorkerRuntimeException("Failed to open the file stream"))
      );
    } catch (WorkerServerException e) {
      onError(e);
    }
  }
}
