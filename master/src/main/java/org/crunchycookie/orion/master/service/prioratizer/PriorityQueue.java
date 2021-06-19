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

package org.crunchycookie.orion.master.service.prioratizer;

import java.util.Optional;
import java.util.UUID;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.Priority;

/**
 * This class represents the queue where the priority of the task is stored until they get
 * executed.
 */
public interface PriorityQueue {

  /**
   * Insert the provided task with the given priority.
   *
   * @param taskId
   * @param priority
   */
  void insert(UUID taskId, Priority priority);

  /**
   * Obtain the next task having the highest priority.
   *
   * @return Unique Id of the task.
   */
  Optional<UUID> next() throws MasterException;

  /**
   * Check whether a next task exists.
   *
   * @return Unique Id of the task.
   */
  boolean hasNext() throws MasterException;
}
