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

package org.crunchycookie.orion.master.service.distributor;

import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;

/**
 * This class represents the task distributor. It's responsibilities are,
 * <p/>
 * 1. Accept a set of sub-tasks derived from a submitted task from a client, assign workers to them,
 * and manage each of such sub-tasks set treating as a single task entity. Management includes
 * monitoring any worker failure and assigning a new worker, etc. // TODO: 2021-06-14 Implement
 * worker fault-tolerance
 */
public interface TaskDistributor {

  /**
   * Assign and dispatch the submitted task to an available worker, and update central store with
   * the status. If no free worker is available, then ask the {@link org.crunchycookie.orion.master.service.scheduler.TaskScheduler}
   * to re-schedule the task.
   *
   * @param submittedTask task.
   * @return Unique id for the created group of sub-tasks.
   */
  void distribute(SubmittedTask submittedTask) throws MasterException;
}
