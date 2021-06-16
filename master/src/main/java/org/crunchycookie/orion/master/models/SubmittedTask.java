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

package org.crunchycookie.orion.master.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus.TaskStatus;
import org.crunchycookie.orion.master.utils.RESTUtils.ResourceParams;

/**
 * Represents a task submitted from a client.
 */
public class SubmittedTask {

  // Unique Id for the task.
  private UUID taskId;

  // Input files of the task. This also includes the executable file.
  private List<TaskFile> taskFiles;

  // Identifier for the executable.
  private TaskFileMetadata executable;

  // Details on minimum resources expected by the task. Ex: Min 2 GB ram is needed, etc.
  private Map<ResourceParams, String> resourceRequirements;

  // Set of new files created after the execution.
  private List<TaskFile> outputFiles;

  // Unique ID of the worker node where this task is assigned.
  private UUID workerId;

  // Current status of this task.
  private SubmittedTaskStatus status;

  public SubmittedTask(UUID taskId, List<TaskFile> taskFiles, TaskFileMetadata executable) {
    this.taskId = taskId;
    this.taskFiles = taskFiles;
    this.executable = executable;
    resourceRequirements = new HashMap<>();
    status = new SubmittedTaskStatus(taskId, TaskStatus.PENDING);
  }

  public List<TaskFile> getOutputFiles() {
    return outputFiles;
  }

  public void setOutputFiles(List<TaskFile> outputFiles) {
    this.outputFiles = outputFiles;
  }

  public SubmittedTaskStatus getStatus() {
    return status;
  }

  public void setStatus(SubmittedTaskStatus status) {
    this.status = status;
  }

  public UUID getWorkerId() {
    return workerId;
  }

  public void setWorkerId(UUID workerId) {
    this.workerId = workerId;
  }

  public Map<ResourceParams, String> getResourceRequirements() {
    return resourceRequirements;
  }

  public void setResourceRequirements(
      Map<ResourceParams, String> resourceRequirements) {
    this.resourceRequirements = resourceRequirements;
  }

  public String getResourceRequirement(String key) {
    return resourceRequirements.get(key);
  }

  public void setResourceRequirement(ResourceParams key, String value) {
    this.resourceRequirements.put(key, value);
  }

  public List<TaskFile> getTaskFiles() {
    return taskFiles;
  }

  public void setTaskFiles(List<TaskFile> taskFiles) {
    this.taskFiles = taskFiles;
  }

  public TaskFileMetadata getExecutable() {
    return executable;
  }

  public void setExecutable(TaskFileMetadata executable) {
    this.executable = executable;
  }

  public UUID getTaskId() {
    return taskId;
  }
}
