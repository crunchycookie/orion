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

package org.crunchycookie.orion.master.service.scheduler;

import java.util.Optional;
import java.util.UUID;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.service.prioratizer.PriorityQueue;

/**
 * This class represents the task scheduler component. It handles task priority, task scheduling,
 * and persisting data for scheduled tasks.
 */
public interface TaskScheduler {

  /**
   * Validates whether a worker can execute this task, get the priority value for the task, insert
   * into the priority queue based on that, and finally persist task files in the central store.
   *
   * @param submittedTask
   * @return
   */
  SubmittedTaskStatus schedule(SubmittedTask submittedTask) throws MasterException;

  /**
   * Provides next scheduled task.
   *
   * @return
   */
  Optional<UUID> next() throws MasterException;

  /**
   * Provides whether a scheduled task exists.
   *
   * @return
   */
  boolean hasNext() throws MasterException;

  PriorityQueue getQueue();
}
