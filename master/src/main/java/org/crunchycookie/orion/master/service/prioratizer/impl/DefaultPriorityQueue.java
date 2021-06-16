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

package org.crunchycookie.orion.master.service.prioratizer.impl;

import static org.crunchycookie.orion.master.constants.MasterConstants.ErrorCodes.INTERNAL_SERVER_ERROR;

import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.Priority;
import org.crunchycookie.orion.master.service.prioratizer.PriorityQueue;

public class DefaultPriorityQueue implements PriorityQueue {

  private final PriorityBlockingQueue<PrioratizedTask> priorityBlockingQueue;

  public DefaultPriorityQueue() {
    int MAX_NUMBER_OF_TASKS = 100;
    priorityBlockingQueue = new PriorityBlockingQueue<>(
        MAX_NUMBER_OF_TASKS,
        Comparator.comparing(PrioratizedTask::getPriority)
    );
  }

  private class PrioratizedTask {

    private final UUID taskId;
    private final Priority priority;

    public PrioratizedTask(UUID taskId, Priority priority) {
      this.taskId = taskId;
      this.priority = priority;
    }

    public Priority getPriority() {
      return priority;
    }

    public UUID getTaskId() {
      return taskId;
    }
  }

  @Override
  public void insert(UUID taskId, Priority priority) {
    this.priorityBlockingQueue.add(new PrioratizedTask(taskId, priority));
  }

  @Override
  public UUID next() throws MasterException {
    try {
      return this.priorityBlockingQueue.take().getTaskId();
    } catch (InterruptedException e) {
      throw new MasterException(INTERNAL_SERVER_ERROR,
          "Obtaining an element from the priority queue was interrupted");
    }
  }
}
