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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.master.RESTfulEndpoint;
import org.crunchycookie.orion.master.exception.MasterClientException;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.manager.TaskManager;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus.TaskStatus;
import org.crunchycookie.orion.master.models.WorkerMetaData;
import org.crunchycookie.orion.master.models.file.TaskFile;
import org.crunchycookie.orion.master.models.file.TaskFileMetadata;
import org.crunchycookie.orion.master.utils.MasterUtils;
import org.crunchycookie.orion.master.utils.RESTUtils.ResourceParams;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class DefaultTaskManager implements TaskManager {

  private static final Logger LOG = LogManager.getLogger(DefaultTaskManager.class);

  public enum DefaultTaskManagerSingleton {
    INSTANCE;
    TaskManager taskManager = new DefaultTaskManager();

    public TaskManager get() {
      return taskManager;
    }
  }

  public static TaskManager getInstant() {

    return DefaultTaskManagerSingleton.INSTANCE.get();
  }

  @Override
  @Scheduled(fixedDelay = 2000, initialDelay = 1000)
  public void sync() throws MasterException {

    Instant syncStart = Instant.now();

    // Obtain all in-progress tasks.
    List<SubmittedTask> inProgressTasks = getCentralStore().get(TaskStatus.IN_PROGRESS);

    // Get the current status of those tasks.
    List<SubmittedTaskStatus> latestStatus = getWorkerPoolManager().getStatus(inProgressTasks);

    // Figure out what are the current success tasks, and obtain them from the worker nodes.
    List<SubmittedTask> successTasks = getTasksFromWorkerNodes(inProgressTasks, latestStatus,
        TaskStatus.SUCCESS);

    /*
     Store success tasks in the central store. This will replace the existing entry thus now
     includes output files and updated status.
     */
    getCentralStore().store(successTasks);

    /*
     Get the list of failed tasks. The returned SubmittedTask object will only include the task id
     since the worker failed thus being unable to obtain files.
     */
    List<SubmittedTask> failedTasks = getTasksFromWorkerNodes(inProgressTasks, latestStatus,
        TaskStatus.FAILED);

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
    if (getTaskScheduler().hasNext()) {
      getTaskDistributor().distribute(getCentralStore().get(
          getTaskScheduler().next().get()
      ));
    }

    Instant syncCompletion = Instant.now();
    if (MasterUtils.isDebugEnabled(LOG)) {
      LOG.info("Synced in " + ChronoUnit.MILLIS.between(syncStart, syncCompletion) / 1000);
    }
  }

  @Override
  public WorkerMetaData getTaskLimitations() throws MasterException {

    WorkerMetaData workerMetaData = new WorkerMetaData();
    workerMetaData.setMaxResourceCapacities(
        Map.of(
            ResourceParams.MEMORY, RESTfulEndpoint.configs.getConfig("WorkerNode.capacity.MEMORY"),
            ResourceParams.STORAGE,
            RESTfulEndpoint.configs.getConfig("WorkerNode.capacity.STORAGE"),
            ResourceParams.DEADLINE,
            RESTfulEndpoint.configs.getConfig("WorkerNode.capacity.DEADLINE")
        )
    );
    return workerMetaData;
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

  private List<SubmittedTask> getTasksFromWorkerNodes(
      List<SubmittedTask> tasksPreviouslyMarkedAsInProgress,
      List<SubmittedTaskStatus> latestStatus, TaskStatus requiredStatus) throws MasterException {

    return getWorkerPoolManager().getTasks(
        tasksPreviouslyMarkedAsInProgress.stream()
            .filter(task -> {
              Optional<SubmittedTaskStatus> currentStatus = getLatestStatusOfTheTask(latestStatus,
                  task);
              return currentStatus.isPresent() && currentStatus.get().getStatus()
                  .equals(requiredStatus);
            })
            .collect(Collectors.toList()));
  }

  private Optional<SubmittedTaskStatus> getLatestStatusOfTheTask(
      List<SubmittedTaskStatus> latestStatus,
      SubmittedTask task) {
    return latestStatus.stream()
        .filter(sts -> sts.getTaskId().equals(task.getTaskId())).findFirst();
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
