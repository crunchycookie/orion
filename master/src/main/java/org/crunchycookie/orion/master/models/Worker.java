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

/**
 * This class represents a Worker microservice running on a node. Followings are the
 * responsibilities of this model class.
 * <p/>
 * 1. Being able to connect to the corresponding microservice via a gRPC connection and execute
 * tasks.
 */
package org.crunchycookie.orion.master.models;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.worker.WorkerGrpc;
import org.crunchycookie.orion.worker.WorkerGrpc.WorkerBlockingStub;
import org.crunchycookie.orion.worker.WorkerGrpc.WorkerStub;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileMetaData;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileUploadResponse;
import org.crunchycookie.orion.worker.WorkerOuterClass.Result;
import org.crunchycookie.orion.worker.WorkerOuterClass.Task;

/**
 * This class represents a Worker microservice. It is capable of talking to the microservice via
 * gRPC.
 */
public class Worker {

  private static final Logger LOG = LogManager.getLogger(Worker.class);

  private final WorkerBlockingStub blockingStub;
  private final WorkerStub asyncStub;

  public Worker(Channel channel) {
    blockingStub = WorkerGrpc.newBlockingStub(channel);
    asyncStub = WorkerGrpc.newStub(channel);
  }

  public Worker(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public Worker(ManagedChannelBuilder<?> channelBuilder) {
    Channel channel = channelBuilder.build();
    blockingStub = WorkerGrpc.newBlockingStub(channel);
    asyncStub = WorkerGrpc.newStub(channel);
  }

  /**
   * Upload task files to the Worker.
   *
   * @param fileUploadRequest Details including file metadata and the input stream.
   * @return Status of the upload process.
   */
  public FileUploadResponse upload(FileUploadRequest fileUploadRequest) {
    // TODO: 2021-06-14 Implement
    return null;
  }

  /**
   * Trigger task execution in the Worker. Task execution is trigger by executing a shell script.
   * This script should take care of executing any other uploaded files, etc.
   *
   * @param task Details of the executable shell script.
   * @return Result of the triggered process.
   */
  public Result execute(Task task) {
    // TODO: 2021-06-14 Implement
    return null;
  }

  /**
   * Monitor the trigger process in the Worker.
   *
   * @param task Details of the executable shell script.
   * @return Result of the triggered process.
   */
  public Result monitor(Task task) {
    // TODO: 2021-06-14 Implement
    return null;
  }

  /**
   * Download a file from the Worker node.
   *
   * @param fileMetaData Details of the file needs to be downloaded.
   * @return Details of the file and it's output stream.
   */
  public FileDownloadResponse download(FileMetaData fileMetaData) {
    // TODO: 2021-06-14 Implement
    return null;
  }
}
