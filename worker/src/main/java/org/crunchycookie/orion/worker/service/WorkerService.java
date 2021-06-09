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

import io.grpc.stub.StreamObserver;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.worker.WorkerGrpc.WorkerImplBase;
import org.crunchycookie.orion.worker.WorkerOuterClass.File;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileMetaData;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadRequest;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadResponse;
import org.crunchycookie.orion.worker.WorkerOuterClass.Result;
import org.crunchycookie.orion.worker.WorkerOuterClass.Status;
import org.crunchycookie.orion.worker.WorkerOuterClass.Task;
import org.crunchycookie.orion.worker.exception.WorkerRuntimeException;
import org.crunchycookie.orion.worker.utils.WorkerUtils;

/**
 * This is the Worker service exposing the functionality of a worker node in the Orion RMS.
 */
public class WorkerService extends WorkerImplBase {

  private static final Logger LOG = LogManager.getLogger(WorkerService.class);

  /**
   * Before {@link Task} execution, a worker needs a few mandatory files like the programme
   * executable, input files if any. Those files are uploaded to the {@link WorkerService} service
   * via this API.
   *
   * @param responseObserver The entity provided from the gRPC framework where we put the {@link
   *                         FileUploadResponse} object.
   * @return The {@link StreamObserver} object containing logic on how to process the incoming
   * {@link FileUploadRequest} stream.
   */
  @Override
  public StreamObserver<FileUploadRequest> upload(
      StreamObserver<FileUploadResponse> responseObserver) {

    return new StreamObserver<>() {
      private FileMetaData fileMetaData = null;
      private FileOutputStream fileOutputStream = null;

      @Override
      public void onNext(FileUploadRequest fileUploadRequest) {

        if (fileUploadRequest.hasMetadata()) {
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
        handleResponse(Status.FAILED);
      }

      @Override
      public void onCompleted() {
        if (closeStream()) {
          handleResponse(Status.SUCCESS);
        }
      }

      private boolean isStreamContentReadyToWrite(FileUploadRequest fileUploadRequest) {
        return fileOutputStream != null && fileUploadRequest.hasFile();
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
        if (fileOutputStream != null) {
          try {
            fileOutputStream.close();
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
          fileOutputStream.write(file.getContent().toByteArray());
          // Should we flush, is it handled automatically?
        } catch (IOException e) {
          onError(new WorkerRuntimeException(
              "Failed to write the content for the file: " + fileMetaData.getName(), e));
        }
      }

      private void initFileStream(FileMetaData fileMetaData) {
        WorkerUtils.getStore().store(fileMetaData).ifPresentOrElse(
            fs -> fileOutputStream = fs,
            () -> onError(new WorkerRuntimeException("Failed to open the file stream"))
        );
      }
    };
  }

  @Override
  public void execute(Task request, StreamObserver<Result> responseObserver) {
    super.execute(request, responseObserver);
  }
}
