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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.Priority;
import org.crunchycookie.orion.master.service.prioratizer.PriorityQueue;

public class DefaultPriorityQueue implements PriorityQueue {

  private static final Logger logger = LogManager.getLogger(DefaultPriorityQueue.class);


  private final java.util.PriorityQueue<PrioratizedTask> priorityQueue;

  public DefaultPriorityQueue() {
    int MAX_NUMBER_OF_TASKS = 100;
    priorityQueue = new java.util.PriorityQueue<>(
        MAX_NUMBER_OF_TASKS,
        Comparator.comparing(PrioratizedTask::getPriority).reversed()
        // Lowest value means highest priority
    );
  }

  public class PrioratizedTask {

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
    this.priorityQueue.add(new PrioratizedTask(taskId, priority));
  }

  @Override
  public Optional<UUID> next() throws MasterException {
    try {
      return Optional.of(this.priorityQueue.remove().getTaskId());
    } catch (NoSuchElementException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean hasNext() throws MasterException {
    return !this.priorityQueue.isEmpty();
  }

  @Override
  public List<PrioratizedTask> getState() {
    return new ArrayList<>(Arrays.asList(priorityQueue.toArray(new PrioratizedTask[]{})));
  }
}
