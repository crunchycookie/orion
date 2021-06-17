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

import static org.crunchycookie.orion.master.utils.MasterUtils.getCentralStore;
import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskDistributor;
import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskScheduler;
import static org.crunchycookie.orion.master.utils.MasterUtils.getWorkerPoolManager;
import static org.crunchycookie.orion.master.utils.MasterUtils.handleClientExceptionScenario;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.crunchycookie.orion.master.exception.MasterClientException;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.manager.TaskManager;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus.TaskStatus;
import org.crunchycookie.orion.master.models.TaskFileMetadata;
import org.crunchycookie.orion.master.models.WorkerMetaData;
import org.crunchycookie.orion.master.models.file.TaskFile;

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
  public void sync() throws MasterException {

    // Obtain all in-progress tasks.
    List<SubmittedTask> inProgressTasks = getCentralStore().get(TaskStatus.IN_PROGRESS);

    // Get current status of in-progress tasks.
    List<SubmittedTaskStatus> latestStatus = getWorkerPoolManager().getStatus(inProgressTasks);

    // Get the list of success tasks.
    List<SubmittedTask> successTasks = getTasksFromWorkers(latestStatus, TaskStatus.SUCCESS);

    /*
     Store success tasks in the central store. This will replace the existing entry thus now
     includes output files and updated status.
     */
    getCentralStore().store(successTasks);

    /*
     Get the list of failed tasks. The returned object will only include the task id since the
     worker failed thus being unable to obtain files.
     */
    List<SubmittedTask> failedTasks = getTasksFromWorkers(latestStatus, TaskStatus.FAILED);

    // Now lets update the failed tasks from the proper objects in the central store.
    failedTasks = getCentralStore().get(failedTasks.stream()
        .map(SubmittedTask::getTaskId)
        .collect(Collectors.toList())
    );

    // Re-schedule failed tasks.
    for (SubmittedTask failedTask : failedTasks) {
      this.submit(failedTask);
    }

    // Obtain next scheduled task and ask task distributor to distribute it.
    getTaskDistributor().distribute(
        getCentralStore().get(getTaskScheduler().next())
    );
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
  public SubmittedTaskStatus getTaskStatus(UUID uniqueTaskId) throws MasterException {

    return getCentralStore().getStatus(uniqueTaskId);
  }

  @Override
  public List<TaskFile> getFiles(UUID uniqueTaskId, List<TaskFileMetadata> fileInformation)
      throws MasterException {

    return getCentralStore().getFiles(uniqueTaskId, fileInformation);
  }

  private List<SubmittedTask> getTasksFromWorkers(List<SubmittedTaskStatus> latestStatus,
      TaskStatus status) throws MasterException {
    List<SubmittedTask> successTasks = getWorkerPoolManager().getTasks(
        latestStatus.stream()
            .filter(st -> st.getStatus() == status)
            .map(SubmittedTaskStatus::getTaskId)
            .collect(Collectors.toList())
    );
    return successTasks;
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
