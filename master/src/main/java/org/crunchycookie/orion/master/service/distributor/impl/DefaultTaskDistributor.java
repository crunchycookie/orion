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

package org.crunchycookie.orion.master.service.distributor.impl;

import static org.crunchycookie.orion.master.utils.MasterUtils.getCentralStore;
import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskScheduler;
import static org.crunchycookie.orion.master.utils.MasterUtils.getWorkerPoolManager;

import java.util.Optional;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus.TaskStatus;
import org.crunchycookie.orion.master.service.distributor.TaskDistributor;
import org.crunchycookie.orion.master.service.worker.WorkerNode;

public class DefaultTaskDistributor implements TaskDistributor {

  @Override
  public void distribute(SubmittedTask submittedTask) throws MasterException {

    // Check for an available free worker.
    Optional<WorkerNode> availableWorker = getWorkerPoolManager().getFreeWorker();

    if (availableWorker.isEmpty()) {
      // Re schedule the task.
      getTaskScheduler().schedule(submittedTask);
      return;
    }

    // Dispatch the task to the worker.
    dispatchTask(availableWorker.get(), submittedTask);

    // Update the store.
    submittedTask.setStatus(
        new SubmittedTaskStatus(
            submittedTask.getTaskId(),
            TaskStatus.IN_PROGRESS
        )
    );
    getCentralStore().store(submittedTask);
  }

  private void dispatchTask(WorkerNode worker, SubmittedTask submittedTask) {

    worker.dispatch(submittedTask);
  }
}
