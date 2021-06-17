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

package org.crunchycookie.orion.master.service.worker;

import java.util.Optional;
import java.util.UUID;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;

/**
 * This class represents a worker node and it takes care of the essential actions which needs to be
 * done with a worker node.
 */
public interface WorkerNode {

  /**
   * Deploy a task to the worker node and start execution.
   *
   * @param submittedTask
   */
  void dispatch(SubmittedTask submittedTask) throws MasterException;

  /**
   * Obtain the submitted task from the worker node. In addition to the dispatched files, this will
   * include all the output files created during task execution. An excemption is thrown if there is
   * no task submitted.
   *
   * @throws MasterException
   */
  SubmittedTask obtain() throws MasterException;

  /**
   * Get current status of the node.
   *
   * @return
   */
  WorkerNodeStatus getStatus();

  /**
   * Get the unique id of the node.
   *
   * @return
   */
  UUID getId();

  /**
   * Get the unique ID of the submitted task. returns empty if the node isn't running a task.
   *
   * @return
   */
  Optional<UUID> getTaskId();

  /**
   * Status about the node.
   */
  public enum WorkerNodeStatus {
    IDLE,
    EXECUTING,
    COMPLETED,
    DEAD
  }
}
