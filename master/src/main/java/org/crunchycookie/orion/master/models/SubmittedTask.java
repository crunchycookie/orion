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
import org.crunchycookie.orion.master.utils.RESTUtils.ResourceParams;

/**
 * Represents a task submitted from a client.
 */
public class SubmittedTask {

  private UUID taskId;
  private List<TaskFile> taskFiles;
  private TaskFileMetadata executable;
  private Map<ResourceParams, String> resourceRequirements;

  public SubmittedTask(UUID taskId, List<TaskFile> taskFiles, TaskFileMetadata executable) {
    this.taskId = taskId;
    this.taskFiles = taskFiles;
    this.executable = executable;
    resourceRequirements = new HashMap<>();
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
