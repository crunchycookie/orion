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

package org.crunchycookie.orion.master.service.manager;

import java.util.List;
import java.util.Optional;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.WorkerMetaData;
import org.crunchycookie.orion.master.service.worker.WorkerNode;

/**
 * This represents the Worker Pool Manager component. Followings are the responsibilities of this
 * manager.
 * <p/>
 * 1. Maintain a homogeneous worker pool. Here homogeneous is in the sense of resources allocated to
 * each worker. Orion assumes each worker has the same resources(CPU, RAM, etc) capacity.
 * <p/>
 * 2. Check heartbeats of Workers and update task manager for dead workers.
 * <p/>
 * 3. Handle worker registration.
 * <p/>
 * 4. Query and provide workers to the task manager from its pool of workers. // TODO: 2021-06-14
 * Implement homogenious worker pool maintanance. May be connect with another service which is able
 * to manage cluster resources. // TODO: 2021-06-14 Implement worker heartbeat check. // TODO:
 * 2021-06-14 Handle worker registration properly. May be allow dynamic registration
 */
public interface WorkerPoolManager {

  /**
   * Initialize the worker pool which includes worker node discovery and registration.
   */
  void init() throws ExceptionInInitializerError;

  /**
   * Query worker pool and provide a free worker.
   *
   * @return A free worker. Can be empty if no free workers are available.
   */
  Optional<WorkerNode> getFreeWorker();

  /**
   * Obtain current status of the submitted tasks.
   *
   * @param submittedTasks Tasks that have been submitted to worker earlier.
   * @return Latest status.
   */
  List<SubmittedTaskStatus> getStatus(List<SubmittedTask> submittedTasks);

  /**
   * Obtain requested tasks from the workers. Obtained tasks include any new file created during the
   * task execution.
   *
   * @return
   */
  List<SubmittedTask> getTasks(List<SubmittedTask> tasks) throws MasterException;

  /**
   * Provide meta information about the worker's capacity including memory and storage.
   *
   * @return Worker's meta information.
   */
  WorkerMetaData getWorkerInformation();
}
