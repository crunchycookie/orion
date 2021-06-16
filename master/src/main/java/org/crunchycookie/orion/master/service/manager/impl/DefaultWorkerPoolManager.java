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

package org.crunchycookie.orion.master.service.manager.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.WorkerMetaData;
import org.crunchycookie.orion.master.service.manager.WorkerPoolManager;
import org.crunchycookie.orion.master.service.worker.WorkerNode;

public class DefaultWorkerPoolManager implements WorkerPoolManager {

  @Override
  public Optional<WorkerNode> getFreeWorker() {
    return Optional.empty();
  }

  @Override
  public Optional<WorkerNode> getWorker(String id) {
    return Optional.empty();
  }

  @Override
  public List<SubmittedTaskStatus> getStatus(List<SubmittedTask> submittedTasks) {
    return null;
  }

  @Override
  public List<SubmittedTask> getTasks(List<UUID> taskIds) {
    return null;
  }

  @Override
  public WorkerMetaData getWorkerInformation() {
    return null;
  }
}
