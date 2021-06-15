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

import java.util.List;
import java.util.Optional;
import org.crunchycookie.orion.worker.WorkerOuterClass.Task;

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
   * Create a group for the submitted sub tasks and assign workers.
   *
   * @param subTasks Set of sub tasks.
   * @return Unique id for the created group of sub-tasks.
   */
  Optional<String> distribute(List<Task> subTasks);
}
