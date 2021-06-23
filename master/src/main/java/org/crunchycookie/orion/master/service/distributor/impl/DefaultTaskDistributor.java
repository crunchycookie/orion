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

package org.crunchycookie.orion.master.service.distributor.impl;

import static org.crunchycookie.orion.master.constants.MasterConstants.ComponentID.COMPONENT_ID_TASK_DISTRIBUTOR;
import static org.crunchycookie.orion.master.utils.MasterUtils.getCentralStore;
import static org.crunchycookie.orion.master.utils.MasterUtils.getLogMessage;
import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskScheduler;
import static org.crunchycookie.orion.master.utils.MasterUtils.getWorkerPoolManager;

import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.master.constants.MasterConstants.ComponentID;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus.TaskStatus;
import org.crunchycookie.orion.master.service.distributor.TaskDistributor;
import org.crunchycookie.orion.master.service.worker.WorkerNode;

public class DefaultTaskDistributor implements TaskDistributor {

  private static final Logger logger = LogManager.getLogger(DefaultTaskDistributor.class);

  private DefaultTaskDistributor() {
  }

  public enum DefaultTaskDistributorSingleton {
    INSTANCE;

    private TaskDistributor taskDistributor;

    DefaultTaskDistributorSingleton() {
      taskDistributor = new DefaultTaskDistributor();
    }

    public TaskDistributor get() {
      return taskDistributor;
    }
  }

  public static TaskDistributor getInstant() {

    return DefaultTaskDistributorSingleton.INSTANCE.get();
  }

  @Override
  public void distribute(SubmittedTask submittedTask) throws MasterException {

    if (logger.isDebugEnabled()) {
      logger.debug(getLogMessage(getComponentId(), submittedTask.getTaskId(),
          "Distributing the task"));
    }

    // Check for an available free worker.
    Optional<WorkerNode> availableWorker = getWorkerPoolManager().getFreeWorker();

    if (availableWorker.isEmpty()) {
      // Re schedule the task.
      getTaskScheduler().schedule(submittedTask);
      return;
    }

    // Dispatch the task to the worker.
    boolean dispatchedStatus = dispatchTask(availableWorker.get(), submittedTask);

    if (!dispatchedStatus) {
      // Re schedule the task because the available worker failed to execute this task.
      getTaskScheduler().schedule(submittedTask);
      return;
    }

    // Update the store.
    SubmittedTask existingTask = getCentralStore().get(submittedTask.getTaskId());
    existingTask.setStatus(new SubmittedTaskStatus(
        submittedTask.getTaskId(),
        TaskStatus.IN_PROGRESS
    ));
    existingTask.setWorkerId(submittedTask.getWorkerId());
    getCentralStore().store(existingTask);
  }

  private ComponentID getComponentId() {
    return COMPONENT_ID_TASK_DISTRIBUTOR;
  }

  private boolean dispatchTask(WorkerNode worker, SubmittedTask submittedTask) {

    try {
      worker.dispatch(submittedTask);
      submittedTask.setWorkerId(worker.getId());
      return true;
    } catch (MasterException e) {
      // TODO: 2021-06-21 need to re-schedule or else task will be faltly identified.
      e.printStackTrace();
    }
    return false;
  }
}
