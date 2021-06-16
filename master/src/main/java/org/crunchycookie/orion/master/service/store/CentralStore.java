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

package org.crunchycookie.orion.master.service.store;

import java.util.UUID;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;

/**
 * This class represents the store where submitted task files are stored until they get dispatched
 * to a worker.
 */
public interface CentralStore {

  /**
   * Store the submitted task.
   *
   * @param submittedTask
   */
  void store(SubmittedTask submittedTask) throws MasterException;

  /**
   * Get the task by its ID.
   *
   * @param taskId
   * @return
   */
  SubmittedTask get(UUID taskId) throws MasterException;

  /**
   * Remove the task.
   *
   * @param taskId
   */
  void remove(UUID taskId) throws MasterException;

  /**
   * Get the task status by its ID.
   *
   * @param taskId
   * @return
   */
  SubmittedTaskStatus getStatus(UUID taskId) throws MasterException;

  /**
   * Set the task status by its ID.
   *
   * @param taskId
   * @return
   */
  void setStatus(UUID taskId, SubmittedTaskStatus status) throws MasterException;
}
