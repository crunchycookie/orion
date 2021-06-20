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

package org.crunchycookie.orion.master.service.scheduler.impl;

import static org.crunchycookie.orion.master.constants.MasterConstants.ComponentID.COMPONENT_ID_TASK_SCHEDULER;
import static org.crunchycookie.orion.master.utils.MasterUtils.getCentralStore;
import static org.crunchycookie.orion.master.utils.MasterUtils.getLogMessage;
import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskCapacityValidator;
import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskPrioratizer;
import static org.crunchycookie.orion.master.utils.MasterUtils.getWorkerPoolManager;

import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.master.constants.MasterConstants.ComponentID;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.Priority;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus.TaskStatus;
import org.crunchycookie.orion.master.service.prioratizer.PriorityQueue;
import org.crunchycookie.orion.master.service.prioratizer.impl.DefaultPriorityQueue;
import org.crunchycookie.orion.master.service.scheduler.TaskScheduler;

public class DefaultTaskScheduler implements TaskScheduler {

  private static final Logger logger = LogManager.getLogger(DefaultTaskScheduler.class);

  private final PriorityQueue priorityQueue;

  private DefaultTaskScheduler() {
    priorityQueue = new DefaultPriorityQueue();
  }

  public enum DefaultTaskSchedulerSingleton {
    INSTANCE;
    private final TaskScheduler taskScheduler = new DefaultTaskScheduler();

    public TaskScheduler get() {
      return taskScheduler;
    }
  }

  public static TaskScheduler getInstant() {

    return DefaultTaskSchedulerSingleton.INSTANCE.get();
  }

  @Override
  public Optional<UUID> next() throws MasterException {

    if (logger.isDebugEnabled()) {
      logger.debug(getLogMessage(getComponentId(), null, "Getting next task"));
    }
    return priorityQueue.next();
  }

  @Override
  public boolean hasNext() throws MasterException {
    if (logger.isDebugEnabled()) {
      logger.debug(getLogMessage(getComponentId(), null, "Checking next task"));
    }
    return priorityQueue.hasNext();
  }

  @Override
  public SubmittedTaskStatus schedule(SubmittedTask submittedTask) throws MasterException {

    if (logger.isDebugEnabled()) {
      logger.debug(getLogMessage(getComponentId(), submittedTask.getTaskId(), "Scheduling task"));
    }
    // Validate submitted job against the worker's capacity.
    getTaskCapacityValidator().validate(
        submittedTask,
        getWorkerPoolManager().getWorkerInformation()
    );

    // Obtain the task priority.
    Priority priority = getTaskPrioratizer().getPriority(submittedTask);

    // Put task in the queue.
    insertToTheQueue(submittedTask, priority);

    // Persist task files in the central store.
    getCentralStore().store(submittedTask);

    // Since there are no errors, the task is successfully set for execution.
    return new SubmittedTaskStatus(submittedTask.getTaskId(), TaskStatus.IN_PROGRESS);
  }

  private ComponentID getComponentId() {
    return COMPONENT_ID_TASK_SCHEDULER;
  }

  private void insertToTheQueue(SubmittedTask submittedTask, Priority priority) {

    if (logger.isDebugEnabled()) {
      logger.debug(getLogMessage(getComponentId(), submittedTask.getTaskId(),
          "Inserting task into the queue"));
    }
    priorityQueue.insert(submittedTask.getTaskId(), priority);
  }
}
