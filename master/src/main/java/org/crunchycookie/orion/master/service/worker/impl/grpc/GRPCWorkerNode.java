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

import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.TaskFileMetadata;
import org.crunchycookie.orion.master.models.file.TaskFile;
import org.crunchycookie.orion.master.service.worker.WorkerNode;

/**
 * This class represents a worker node which communicates with the worker node via gRPC.
 */
public class GRPCWorkerNode extends GRPCWorkerClient implements WorkerNode {

  private UUID id;
  private UUID taskId;
  private TaskFileMetadata executable;
  private List<TaskFileMetadata> inputFiles;
  private List<TaskFileMetadata> outputFiles;

  public GRPCWorkerNode(String host, String port) {

    super(host, port);
    id = UUID.randomUUID();
  }

  @Override
  public void dispatch(SubmittedTask submittedTask) throws MasterException {

    upload(submittedTask.getTaskFiles());
    execute(submittedTask.getExecutable());
    updateNodeStatus(submittedTask);
  }

  @Override
  public SubmittedTask obtain() throws MasterException {

    // Downloading files may change the status in the worker, thus obtaining it first.
    WorkerNodeStatus status = getStatus();

    SubmittedTask submittedTask = new SubmittedTask(taskId, download(inputFiles), executable);
    submittedTask.setOutputFiles(download(outputFiles));
    submittedTask.setStatus(new SubmittedTaskStatus(taskId, getTaskStatus(status)));

    return submittedTask;
  }

  @Override
  public WorkerNodeStatus getStatus() {

    return monitor(executable);
  }

  @Override
  public UUID getId() {

    return this.id;
  }

  @Override
  public Optional<UUID> getTaskId() {

    return Optional.empty();
  }

  private void updateNodeStatus(SubmittedTask submittedTask) {

    this.taskId = submittedTask.getTaskId();
    this.executable = submittedTask.getExecutable();
    this.outputFiles = submittedTask.getOutputFiles().stream().map(TaskFile::getMeta).collect(
        Collectors.toList());
    this.inputFiles = submittedTask.getTaskFiles().stream().map(TaskFile::getMeta).collect(
        Collectors.toList());
  }
}
