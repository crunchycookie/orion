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

package org.crunchycookie.orion.master.utils;

import java.math.BigDecimal;
import java.util.Optional;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.manager.TaskManager;
import org.crunchycookie.orion.master.models.WorkerMetaData;
import org.crunchycookie.orion.master.rest.model.Property;
import org.crunchycookie.orion.master.rest.model.TaskLimits;

public class MasterUtils {

  public static TaskLimits getTaskLimits(WorkerMetaData workerMetaData) {

    return new TaskLimits()
        .memory(BigDecimal.valueOf(workerMetaData.getMemoryUpperLimitInGB()))
        .addAdditionalPropertiesItem(
            new Property()
                .key("storage")
                .value(Long.toString(workerMetaData.getStorageUpperLimitInGB()))
        );
  }

  public static TaskManager getTaskManager() throws MasterException {

    Optional<TaskManager> taskManager = Optional.empty();
    if (taskManager.isEmpty()) {
      throw new MasterException("Failed to obtain task manager");
    }
    return taskManager.get();
  }
}
