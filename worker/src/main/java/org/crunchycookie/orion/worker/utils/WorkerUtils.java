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

package org.crunchycookie.orion.worker.utils;

import org.crunchycookie.orion.worker.WorkerOuterClass.Status;
import org.crunchycookie.orion.worker.store.TaskExecutionManager;
import org.crunchycookie.orion.worker.store.constants.TaskExecutionManagerConstants.OperationStatus;
import org.crunchycookie.orion.worker.store.impl.PrimaryStorageBasedTaskExecutionManager;

public class WorkerUtils {

  /**
   * Get the execution manager instance. Currently, the singleton instance of {@link
   * PrimaryStorageBasedTaskExecutionManager} is returned.
   *
   * @return {@link TaskExecutionManager} instance.
   */
  public static TaskExecutionManager getTaskExecutionManager() {
    /*
    This method chose the execution manager. Currently, the hardcoded one is returned. However, in
    a scenario where containers can be executed, this method involves in selecting the correct
    container and getting the execution manager for that container.
     */
    return PrimaryStorageBasedTaskExecutionManager.getInstance();
  }

  /**
   * Convert operation status to response status.
   *
   * @param status {@link OperationStatus}
   * @return {@link Status} for the response.
   */
  public static Status getResponseStatus(OperationStatus status) {
    return switch (status) {
      case SUCCESSFULLY_STARTED -> Status.SUCCESS;
      case REJECTED_PROCESS_ALREADY_EXISTS -> Status.IN_PROGRESS;
      case IDLE -> Status.NOT_EXECUTING; // Either task is done executing or never executed at all.
      case BUSY -> Status.IN_PROGRESS;
      default -> Status.FAILED;
    };
  }
}
