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

import static org.crunchycookie.orion.worker.utils.WorkerUtils.getResponseStatus;
import static org.crunchycookie.orion.worker.utils.WorkerUtils.handleResponse;
import static org.crunchycookie.orion.worker.utils.WorkerUtils.streamInChunks;

import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.InputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.worker.WorkerGrpc.WorkerImplBase;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileMetaData;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadRequest;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadResponse;
import org.crunchycookie.orion.worker.WorkerOuterClass.Result;
import org.crunchycookie.orion.worker.WorkerOuterClass.Status;
import org.crunchycookie.orion.worker.WorkerOuterClass.Task;
import org.crunchycookie.orion.worker.service.observer.FileUploadRequestObserver;
import org.crunchycookie.orion.worker.store.constants.TaskExecutionManagerConstants.OperationStatus;
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
  public StreamObserver<FileUploadRequest> upload(StreamObserver<FileUploadResponse>
      responseObserver) {
    return new FileUploadRequestObserver(responseObserver);
  }

  /**
   * Execution of the tasks is based on shell scripts, as almost every task can be achieved via a
   * shell script.
   * <p/>
   * Prior executing this action, all the necessary files should be uploaded to the node via the
   * {@link #upload(StreamObserver)} action. The mandatory file in this process is the shell script.
   * This is a .sh file.
   * <p/>
   * After that, this action is invoked. It will navigate to the workspace where the script resides
   * and execute it in a new process. Any new file generated from the execution is then streamed
   * back to the client who called this action.
   * <p/>
   *
   * @param request          Includes information about the executable shell script.
   * @param responseObserver Observer sent to the caller to obtain streaming results.
   */
  @Override
  public void execute(Task request, StreamObserver<Result> responseObserver) {
    try {
      OperationStatus status = WorkerUtils.getTaskExecutionManager().execute(
          request.getExecutableShellScriptMetadata()
      );
      handleResponse(responseObserver, getResponseStatus(status));
    } catch (Throwable t) {
      LOG.error("Failed executing the task", t);
      handleResponse(responseObserver, Status.FAILED);
    }
  }

  /**
   * Check status of the requested {@link Task} and obtain the response.
   *
   * @param request
   * @param responseObserver
   */
  @Override
  public void monitor(Task request, StreamObserver<Result> responseObserver) {
    try {
      OperationStatus status = WorkerUtils.getTaskExecutionManager().getStatus(
          request.getExecutableShellScriptMetadata()
      );
      handleResponse(responseObserver, getResponseStatus(status));
    } catch (Throwable t) {
      LOG.error("Failed monitoring the task", t);
      handleResponse(responseObserver, Status.FAILED);
    }
  }

  /**
   * Download files. Files can be the results of the execution task, etc. Following is the element
   * order in the stream.
   * <p/>
   * 1. First element is the Metadata of the downloading file.
   * <p/>
   * 2. Subsequent elements are the actual file in 1 MB byte chunks.
   * <p/>
   * 3. Final element is the result of the operation. Unless its success, file is corrupted.
   *
   * @param request          {@link FileMetaData} describing the file.
   * @param responseObserver A stream of the file.
   */
  @Override
  public void download(FileMetaData request, StreamObserver<Result> responseObserver) {
    try {
      // Send file metadata as the first element in the stream.
      responseObserver.onNext(Result.newBuilder().setOutputFileMetaData(request).build());
      // Obtain input source for the file.
      InputStream fileInputStream = WorkerUtils.getTaskExecutionManager().get(request).getValue();
      // Stream input source over the gRPC connection.
      streamInChunks(responseObserver, fileInputStream);
      // Set the result as the last element, and conclude streaming.
      handleResponse(responseObserver, Status.SUCCESS);
    } catch (Throwable t) {
      LOG.error("Failed getting the file", t);
      handleResponse(responseObserver, Status.FAILED);
    }
  }
}
