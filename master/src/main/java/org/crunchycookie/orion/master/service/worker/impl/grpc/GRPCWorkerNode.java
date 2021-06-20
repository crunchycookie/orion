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

import static org.crunchycookie.orion.master.constants.MasterConstants.ComponentID.COMPONENT_ID_WORKER_NODE;
import static org.crunchycookie.orion.master.utils.MasterUtils.getLogMessage;
import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskStatus;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.master.constants.MasterConstants.ComponentID;
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

  private static final Logger logger = LogManager.getLogger(GRPCWorkerNode.class);

  private String id;

  public GRPCWorkerNode(String host, String port) {

    super(host, port);
    id = getWorkerUniqueId(host, port);
  }

  @Override
  public void dispatch(SubmittedTask submittedTask) throws MasterException {

    logger.info(getLogMessage(getComponentId(), submittedTask.getTaskId(), "Dispatching the task",
        "WorkerNode: " + getId()));
    upload(submittedTask.getTaskFiles());
    execute(submittedTask.getExecutable());
  }

  @Override
  public SubmittedTask obtain(SubmittedTask submittedTask) throws MasterException {

    logger.info(getLogMessage(getComponentId(), submittedTask.getTaskId(), "Obtaining the task", "WorkerNode: " + getId()));

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

    logger.info(
        getLogMessage(getComponentId(), submittedTask == null ? null : submittedTask.getTaskId(),
            "Getting the worker node status", "WorkerNode: " + getId()));

    TaskFileMetadata executable = submittedTask == null || submittedTask.getExecutable() == null ?
        new TaskFileMetadata(
            "",
            "",
            null
        ) : submittedTask.getExecutable();
    return monitor(executable);
  }

  @Override
  public String getId() {

    return this.id;
  }

  private ComponentID getComponentId() {
    return COMPONENT_ID_WORKER_NODE;
  }

  private String getWorkerUniqueId(String host, String port) {
    return host + "###" + port;
  }
}
