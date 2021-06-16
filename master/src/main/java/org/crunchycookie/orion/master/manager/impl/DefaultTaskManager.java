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

package org.crunchycookie.orion.master.manager.impl;

import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskScheduler;
import static org.crunchycookie.orion.master.utils.MasterUtils.handleClientExceptionScenario;

import java.util.List;
import java.util.UUID;
import org.crunchycookie.orion.master.exception.MasterClientException;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.manager.TaskManager;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.TaskFile;
import org.crunchycookie.orion.master.models.TaskFileMetadata;
import org.crunchycookie.orion.master.models.WorkerMetaData;
import org.crunchycookie.orion.master.utils.MasterUtils;

public class DefaultTaskManager implements TaskManager {

  private DefaultTaskManager() {
  }

  public enum DefaultTaskManagerSingleton {
    INSTANCE;
    TaskManager taskManager = new DefaultTaskManager();

    public TaskManager get() {
      return taskManager;
    }
  }

  @Override
  public WorkerMetaData getTaskLimitations() throws MasterException {
    return null;
  }

  @Override
  public SubmittedTaskStatus submit(SubmittedTask submittedTask) throws MasterException {

    UUID taskId = submittedTask.getTaskId();
    validateInputParams(submittedTask, taskId);
    return getTaskScheduler().schedule(submittedTask);
  }

  @Override
  public SubmittedTaskStatus getTaskStatus(String uniqueTaskId) throws MasterException {
    return null;
  }

  @Override
  public List<TaskFile> getFiles(String uniqueTaskId, List<TaskFileMetadata> fileInformation)
      throws MasterException {
    return null;
  }

  private void validateInputParams(SubmittedTask submittedTask, UUID taskId)
      throws MasterClientException {
    if (taskId == null) {
      handleClientExceptionScenario("Submitted task must be associated to a valid task");
    }
    if (submittedTask.getExecutable().getFileName().isBlank()) {
      handleClientExceptionScenario("Submitted task must provide a valid executable file");
    }
  }
}
