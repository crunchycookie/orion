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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.master.RESTfulEndpoint;
import org.crunchycookie.orion.master.exception.MasterClientException;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.manager.TaskManager;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus.TaskStatus;
import org.crunchycookie.orion.master.models.WorkerMetaData;
import org.crunchycookie.orion.master.rest.model.Property;
import org.crunchycookie.orion.master.rest.model.TaskLimits;
import org.crunchycookie.orion.master.service.distributor.TaskDistributor;
import org.crunchycookie.orion.master.service.manager.WorkerPoolManager;
import org.crunchycookie.orion.master.service.prioratizer.TaskPrioritizer;
import org.crunchycookie.orion.master.service.scheduler.TaskScheduler;
import org.crunchycookie.orion.master.service.store.CentralStore;
import org.crunchycookie.orion.master.service.validator.TaskCapacityValidator;
import org.crunchycookie.orion.master.service.worker.WorkerNode.WorkerNodeStatus;
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

    return getInstance("TaskManager",
        "org.crunchycookie.orion.master.manager.impl.DefaultTaskManager");
  }

  public static TaskScheduler getTaskScheduler() throws MasterException {

    return getInstance("TaskScheduler",
        "org.crunchycookie.orion.master.manager.impl.DefaultTaskScheduler");
  }

  public static WorkerPoolManager getWorkerPoolManager() throws MasterException {

    return getInstance("WorkerPoolManager",
        "org.crunchycookie.orion.master.manager.impl.DefaultWorkerPoolManager");
  }

  public static TaskCapacityValidator getTaskCapacityValidator() throws MasterException {

    return getInstance("TaskCapacityValidator",
        "org.crunchycookie.orion.master.manager.impl.DefaultTaskCapacityValidator");
  }

  public static TaskPrioritizer getTaskPrioratizer() throws MasterException {

    return getInstance("TaskPrioritizer",
        "org.crunchycookie.orion.master.manager.impl.DefaultTaskPrioritizer");
  }

  public static CentralStore getCentralStore() throws MasterException {

    return getInstance("CentralStore",
        "org.crunchycookie.orion.master.manager.impl.LocalStorageCentralStore");
  }

  public static TaskDistributor getTaskDistributor() throws MasterException {

    return getInstance("TaskDistributor",
        "org.crunchycookie.orion.master.manager.impl.DefaultTaskDistributor");
  }

  public static void handleClientExceptionScenario(String msg) throws MasterClientException {
    throw new MasterClientException(msg);
  }

  public static TaskStatus getTaskStatus(WorkerNodeStatus nodeStatus) {
    return switch (nodeStatus) {
      case EXECUTING -> TaskStatus.IN_PROGRESS;
      case COMPLETED, IDLE -> TaskStatus.SUCCESS;
      case DEAD, FAILED -> TaskStatus.FAILED;
    };
  }

  public static <T> T getInstance(String serviceName, String defaultClassName)
      throws MasterException {

    String clazzName = RESTfulEndpoint.configs.getConfig(serviceName);
    if (StringUtils.isBlank(clazzName)) {
      clazzName = defaultClassName;
    }
    try {
      Class<?> clazz = Class.forName(clazzName);
      Method method = clazz.getMethod("getInstant");
      return (T) method.invoke(null, null);
    } catch (Exception e) {
      throw new MasterException("Error while initiating");
    }
  }

  public static boolean isDebugEnabled(Logger LOG) {
    return Boolean.parseBoolean(RESTfulEndpoint.configs.getConfig("Log.debug"));
  }
}
