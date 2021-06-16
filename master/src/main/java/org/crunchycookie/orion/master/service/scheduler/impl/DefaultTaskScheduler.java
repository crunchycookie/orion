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

import static org.crunchycookie.orion.master.utils.MasterUtils.getCentralStore;
import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskCapacityValidator;
import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskPrioratizer;
import static org.crunchycookie.orion.master.utils.MasterUtils.getWorkerPoolManager;

import java.util.UUID;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.Priority;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus.TaskStatus;
import org.crunchycookie.orion.master.service.prioratizer.PriorityQueue;
import org.crunchycookie.orion.master.service.prioratizer.impl.DefaultPriorityQueue;
import org.crunchycookie.orion.master.service.scheduler.TaskScheduler;

public class DefaultTaskScheduler implements TaskScheduler {

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

  @Override
  public UUID next() throws MasterException {

    return priorityQueue.next();
  }

  @Override
  public SubmittedTaskStatus schedule(SubmittedTask submittedTask) throws MasterException {

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

  private void insertToTheQueue(SubmittedTask submittedTask, Priority priority) {

    priorityQueue.insert(submittedTask.getTaskId(), priority);
  }
}
