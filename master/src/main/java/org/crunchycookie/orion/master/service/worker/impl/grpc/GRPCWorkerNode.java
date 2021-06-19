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
import java.util.stream.Collectors;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.file.TaskFile;
import org.crunchycookie.orion.master.models.file.TaskFileMetadata;
import org.crunchycookie.orion.master.service.worker.WorkerNode;

/**
 * This class represents a worker node which communicates with the worker node via gRPC.
 */
public class GRPCWorkerNode extends GRPCWorkerClient implements WorkerNode {

  private String id;
//  private UUID taskId;
//  private TaskFileMetadata executable;
//  private List<TaskFileMetadata> inputFiles;
//  private List<TaskFileMetadata> outputFiles;

  public GRPCWorkerNode(String host, String port) {

    super(host, port);
    id = getWorkerUniqueId(host, port);
  }

  private String getWorkerUniqueId(String host, String port) {
    return host + "###" + port;
  }

  @Override
  public void dispatch(SubmittedTask submittedTask) throws MasterException {

    upload(submittedTask.getTaskFiles());
    execute(submittedTask.getExecutable());
  }

  @Override
  public SubmittedTask obtain(SubmittedTask submittedTask) throws MasterException {

    // Downloading files may change the status in the worker, thus obtaining it first.
    WorkerNodeStatus status = getStatus(submittedTask);

    // Download files from the worker.
    List<TaskFile> downloadedInputFiles = download(submittedTask.getTaskFiles().stream()
        .map(TaskFile::getMeta).collect(Collectors.toList()));
    List<TaskFile> downloadedOutputFiles = download(submittedTask.getOutputFiles().stream()
        .map(TaskFile::getMeta).collect(Collectors.toList()));

    // Update the task.
    submittedTask.setTaskFiles(downloadedInputFiles);
    submittedTask.setOutputFiles(downloadedOutputFiles);
    submittedTask.setStatus(new SubmittedTaskStatus(
        submittedTask.getTaskId(),
        getTaskStatus(status)
    ));

    return submittedTask;
  }

  @Override
  public WorkerNodeStatus getStatus(SubmittedTask submittedTask) {

    TaskFileMetadata executable = submittedTask.getExecutable();
    TaskFileMetadata taskToCheckStatus = executable != null ? executable : new TaskFileMetadata(
        "",
        "",
        null
    );
    return monitor(taskToCheckStatus);
  }

  @Override
  public String getId() {

    return this.id;
  }

//  @Override
//  public Optional<UUID> getTaskId() {
//
//    return taskId != null ? Optional.of(taskId) : Optional.empty();
//  }

//  private void updateNodeStatus(SubmittedTask submittedTask) {
//
//    this.taskId = submittedTask.getTaskId();
//    this.executable = submittedTask.getExecutable();
//    this.outputFiles = submittedTask.getOutputFiles().stream().map(TaskFile::getMeta).collect(
//        Collectors.toList());
//    this.inputFiles = submittedTask.getTaskFiles().stream().map(TaskFile::getMeta).collect(
//        Collectors.toList());
//  }
}
