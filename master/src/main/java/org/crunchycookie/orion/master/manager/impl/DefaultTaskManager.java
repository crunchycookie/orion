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

import static org.crunchycookie.orion.master.constants.MasterConstants.ComponentID.COMPONENT_ID_TASK_MANAGER;
import static org.crunchycookie.orion.master.utils.MasterUtils.getCentralStore;
import static org.crunchycookie.orion.master.utils.MasterUtils.getLogMessage;
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
import org.crunchycookie.orion.master.constants.MasterConstants.ComponentID;
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

  private static final Logger logger = LogManager.getLogger(DefaultTaskManager.class);

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
  @Scheduled(fixedDelay = 5000, initialDelay = 1000)
  public void sync() throws MasterException {

    Instant syncStart = Instant.now();

    // Obtain all in-progress tasks from the central store.
    List<SubmittedTask> tasksMarkedAsInProgress = getCentralStore().get(TaskStatus.IN_PROGRESS);

    // Get the current status of those tasks from worker pool.
    List<SubmittedTaskStatus> latestStatus = getWorkerPoolManager().getStatus(
        tasksMarkedAsInProgress);

    // Filter success tasks and obtain those filtered tasks from the worker pool. This include
    // latest input and output files since the process is now completed in the worker.
    List<SubmittedTask> successTasks = getFilteredTasksFromWorkerPool(tasksMarkedAsInProgress,
        latestStatus, TaskStatus.SUCCESS);

    /*
     Store success tasks in the central store. This will replace the existing entry thus now
     includes output files and updated status.
     */
    getCentralStore().store(successTasks);

    // Filter failed tasks among the in-progress list obtain from the central store and update their
    // status as failed. Then re-schedule those failed tasks.
    filterAndRescheduleFailedTasks(tasksMarkedAsInProgress, latestStatus);

    // Obtain next scheduled task and ask task distributor to distribute it.
    if (getTaskScheduler().hasNext()) {
      getTaskDistributor().distribute(getCentralStore().get(
          getTaskScheduler().next().get()
      ));
    }

    Instant syncCompletion = Instant.now();
    if (MasterUtils.isDebugEnabled(logger)) {
      logger.info("Synced in " + ChronoUnit.MILLIS.between(syncStart, syncCompletion) / 1000);
    }
  }

  @Override
  public WorkerMetaData getTaskLimitations() throws MasterException {

    logger.info(getLogMessage(getComponentId(), null, "Querying task limitations"));
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

    logger.info(getLogMessage(getComponentId(), taskId, "Task Submitted."));
    return getTaskScheduler().schedule(submittedTask);
  }

  @Override
  public SubmittedTaskStatus getTaskStatus(UUID uniqueTaskId) throws MasterException {

    logger.info(getLogMessage(getComponentId(), uniqueTaskId, "Querying task status"));
    return getCentralStore().getStatus(uniqueTaskId);
  }

  @Override
  public List<TaskFile> getFiles(UUID uniqueTaskId, List<TaskFileMetadata> fileInformation)
      throws MasterException {

    logger.info(getLogMessage(getComponentId(), uniqueTaskId, "Obtaining files"));
    return getCentralStore().getFiles(uniqueTaskId, fileInformation);
  }

  private ComponentID getComponentId() {
    return COMPONENT_ID_TASK_MANAGER;
  }

  private void filterAndRescheduleFailedTasks(List<SubmittedTask> tasksMarkedAsInProgress,
      List<SubmittedTaskStatus> latestStatus) throws MasterException {

    // Filter failed tasks.
    List<SubmittedTask> failedTasks = getFilteredTasks(tasksMarkedAsInProgress, latestStatus,
        TaskStatus.FAILED);

    // Set status as failed.
    failedTasks.forEach(failedTask -> failedTask.setStatus(new SubmittedTaskStatus(
        failedTask.getTaskId(),
        TaskStatus.FAILED
    )));

    // Re-schedule failed tasks.
    for (SubmittedTask failedTask : failedTasks) {
      this.submit(failedTask);
    }
  }

  private List<SubmittedTask> getFilteredTasksFromWorkerPool(
      List<SubmittedTask> tasksMarkedAsInProgress,
      List<SubmittedTaskStatus> latestStatus, TaskStatus requiredStatus) throws MasterException {

    // Filter successful status, and obtain their corresponding tasks.
    List<SubmittedTask> successfulTasks = getFilteredTasks(tasksMarkedAsInProgress, latestStatus,
        requiredStatus);

    // Retrieve them from the worker pool.
    List<SubmittedTask> successTasks = getTasksFromWorkerPool(successfulTasks);
    return successTasks;
  }

  private List<SubmittedTask> getFilteredTasks(List<SubmittedTask> tasksMarkedAsInProgress,
      List<SubmittedTaskStatus> latestStatus, TaskStatus requiredStatus) {

    // Filter required status.
    List<SubmittedTaskStatus> filteredStatus = latestStatus.stream()
        .filter(status -> status.getStatus().equals(requiredStatus))
        .collect(Collectors.toList());

    // Match and obtain corresponding tasks for them.
    List<SubmittedTask> filteredTasks = tasksMarkedAsInProgress.stream()
        .filter(t -> filteredStatus.stream().anyMatch(
            status -> status.getTaskId().equals(t.getTaskId())
        )).collect(Collectors.toList());

    return filteredTasks;
  }

  private List<SubmittedTask> getTasksFromWorkerPool(List<SubmittedTask> tasks)
      throws MasterException {

    return getWorkerPoolManager().getTasks(tasks);
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
