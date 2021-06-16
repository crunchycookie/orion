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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.crunchycookie.orion.master.constants.MasterConstants.ErrorCodes;
import org.crunchycookie.orion.master.exception.MasterClientException;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.manager.TaskManager;
import org.crunchycookie.orion.master.manager.impl.DefaultTaskManager.DefaultTaskManagerSingleton;
import org.crunchycookie.orion.master.models.WorkerMetaData;
import org.crunchycookie.orion.master.rest.model.Property;
import org.crunchycookie.orion.master.rest.model.TaskLimits;
import org.crunchycookie.orion.master.service.manager.WorkerPoolManager;
import org.crunchycookie.orion.master.service.prioratizer.TaskPrioritizer;
import org.crunchycookie.orion.master.service.scheduler.TaskScheduler;
import org.crunchycookie.orion.master.service.scheduler.impl.DefaultTaskScheduler.DefaultTaskSchedulerSingleton;
import org.crunchycookie.orion.master.service.store.CentralStore;
import org.crunchycookie.orion.master.service.validator.TaskCapacityValidator;
import org.crunchycookie.orion.master.utils.RESTUtils.ResourceParams;

public class MasterUtils {

  public static TaskLimits getTaskLimits(WorkerMetaData workerMetaData) {

    List<Property> properties = new ArrayList<>();
    for (Map.Entry<ResourceParams, String> entry : workerMetaData.getMaxResourceCapacities()
        .entrySet()) {
      Property property = new Property();
      property.setKey(entry.getKey().toString());
      property.setValue(entry.getValue());
      properties.add(property);
    }
    return new TaskLimits().limits(properties);
  }

  public static TaskManager getTaskManager() throws MasterException {

    // Need to insert a pluggable mechanism. Until then, the default is hardcoded.
    Optional<TaskManager> taskManager = Optional.of(DefaultTaskManagerSingleton.INSTANCE.get());

    if (taskManager.isEmpty()) {
      throw new MasterException(ErrorCodes.INTERNAL_SERVER_ERROR, "Failed to obtain task manager");
    }
    return taskManager.get();
  }

  public static TaskScheduler getTaskScheduler() throws MasterException {

    // Need to insert a pluggable mechanism. Until then, the default is hardcoded.
    Optional<TaskScheduler> TaskScheduler = Optional
        .of(DefaultTaskSchedulerSingleton.INSTANCE.get());

    if (TaskScheduler.isEmpty()) {
      throw new MasterException(ErrorCodes.INTERNAL_SERVER_ERROR,
          "Failed to obtain task scheduler");
    }
    return TaskScheduler.get();
  }

  public static WorkerPoolManager getWorkerPoolManager() throws MasterException {

    // Need to insert a pluggable mechanism. Until then, the default is hardcoded.
    Optional<WorkerPoolManager> workerPoolManager = Optional
        .empty();

    if (workerPoolManager.isEmpty()) {
      throw new MasterException(ErrorCodes.INTERNAL_SERVER_ERROR,
          "Failed to obtain worker pool manager");
    }
    return workerPoolManager.get();
  }

  public static TaskCapacityValidator getTaskCapacityValidator() throws MasterException {

    // Need to insert a pluggable mechanism. Until then, the default is hardcoded.
    Optional<TaskCapacityValidator> taskCapacityValidator = Optional.empty();

    if (taskCapacityValidator.isEmpty()) {
      throw new MasterException(ErrorCodes.INTERNAL_SERVER_ERROR,
          "Failed to obtain task capacity validator");
    }
    return taskCapacityValidator.get();
  }

  public static TaskPrioritizer getTaskPrioratizer() throws MasterException {

    // Need to insert a pluggable mechanism. Until then, the default is hardcoded.
    Optional<TaskPrioritizer> taskPrioritizer = Optional.empty();

    if (taskPrioritizer.isEmpty()) {
      throw new MasterException(ErrorCodes.INTERNAL_SERVER_ERROR,
          "Failed to obtain the task prioratizer");
    }
    return taskPrioritizer.get();
  }

  public static CentralStore getCentralStore() throws MasterException {

    // Need to insert a pluggable mechanism. Until then, the default is hardcoded.
    Optional<CentralStore> centralStore = Optional.empty();

    if (centralStore.isEmpty()) {
      throw new MasterException(ErrorCodes.INTERNAL_SERVER_ERROR,
          "Failed to obtain the task prioratizer");
    }
    return centralStore.get();
  }

  public static void handleClientExceptionScenario(String msg) throws MasterClientException {
    throw new MasterClientException(msg);
  }
}
