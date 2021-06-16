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

package org.crunchycookie.orion.master.service.worker.impl;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.TaskFile;
import org.crunchycookie.orion.master.models.TaskFileMetadata;
import org.crunchycookie.orion.master.service.worker.WorkerNode;
import org.crunchycookie.orion.worker.WorkerGrpc;
import org.crunchycookie.orion.worker.WorkerGrpc.WorkerBlockingStub;
import org.crunchycookie.orion.worker.WorkerGrpc.WorkerStub;

/**
 * This class represents a worker node which communicates with the worker node via gRPC.
 */
public class GRPCWorkerNode implements WorkerNode {

  private final WorkerBlockingStub blockingStub;
  private final WorkerStub asyncStub;

  private UUID id = UUID.randomUUID();
  private UUID taskId;
  private WorkerNodeStatus status = WorkerNodeStatus.IDLE;

  public GRPCWorkerNode(Channel channel) {
    blockingStub = WorkerGrpc.newBlockingStub(channel);
    asyncStub = WorkerGrpc.newStub(channel);
  }

  public GRPCWorkerNode(String host, String port) {
    this(ManagedChannelBuilder.forAddress(host, Integer.parseInt(port)).usePlaintext());
  }

  public GRPCWorkerNode(ManagedChannelBuilder<?> channelBuilder) {
    Channel channel = channelBuilder.build();
    blockingStub = WorkerGrpc.newBlockingStub(channel);
    asyncStub = WorkerGrpc.newStub(channel);
  }

  @Override
  public void dispatch(SubmittedTask submittedTask) throws MasterException {

    upload(submittedTask.getTaskFiles());
    execute(submittedTask.getExecutable());

    updateNodeStatus(submittedTask.getTaskId(), WorkerNodeStatus.EXECUTING);
  }

  @Override
  public WorkerNodeStatus getStatus() {

    return this.status;
  }

  @Override
  public UUID getId() {

    return this.id;
  }

  @Override
  public Optional<UUID> getTaskId() {

    return Optional.empty();
  }

  protected void upload(List<TaskFile> files) {

  }

  protected void execute(TaskFileMetadata executable) {

  }

  protected void monitor() {

  }

  protected void download() {

  }

  private void updateNodeStatus(UUID taskId, WorkerNodeStatus status) {

    this.taskId = taskId;
    this.status = status;
  }
}
