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
   * Status about the node.
   */
  public enum WorkerNodeStatus {
    IDLE,
    EXECUTING,
    COMPLETED,
    FAILED,
    DEAD
  }

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
   * @param submittedTask The task which was submitted to this node.
   * @return The task obtained from the worker node. This has output files from the execution, etc.
   * @throws MasterException
   */
  SubmittedTask obtain(SubmittedTask submittedTask) throws MasterException;

  /**
   * Get the current status of the submitted task in node.
   *
   * @return
   */
  WorkerNodeStatus getStatus(SubmittedTask submittedTask);

  /**
   * Get the unique id of the node.
   *
   * @return
   */
  String getId();
}
