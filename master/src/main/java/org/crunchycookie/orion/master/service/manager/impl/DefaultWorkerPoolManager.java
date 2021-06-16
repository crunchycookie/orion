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

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.crunchycookie.orion.master.config.worker.WorkerNodeDiscoveryInfo;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus.TaskStatus;
import org.crunchycookie.orion.master.models.WorkerMetaData;
import org.crunchycookie.orion.master.rest.MasterRESTEndpoint;
import org.crunchycookie.orion.master.service.manager.WorkerPoolManager;
import org.crunchycookie.orion.master.service.worker.WorkerNode;
import org.crunchycookie.orion.master.service.worker.WorkerNode.WorkerNodeStatus;

public class DefaultWorkerPoolManager implements WorkerPoolManager {

  List<WorkerNodeDiscoveryInfo> registeredWorkerNodesInfo;
  List<WorkerNode> registeredNodes;

  private DefaultWorkerPoolManager() {
  }

  public enum DefaultWorkerPoolManagerSingleton {
    INSTANCE;

    private WorkerPoolManager workerPoolManager;

    DefaultWorkerPoolManagerSingleton() {
      workerPoolManager = new DefaultWorkerPoolManager();
    }

    public WorkerPoolManager get() {
      return workerPoolManager;
    }
  }

  @Override
  public void init() throws MasterException {

    discoverAndRegisterWorkerNodes();
  }

  @Override
  public Optional<WorkerNode> getFreeWorker() {

    return registeredNodes.stream().filter(n -> n.getStatus() == WorkerNodeStatus.IDLE).findFirst();
  }

  @Override
  public Optional<WorkerNode> getWorker(UUID id) {

    return registeredNodes.stream().filter(n -> n.getId().equals(id)).findFirst();
  }

  @Override
  public List<SubmittedTaskStatus> getStatus(List<SubmittedTask> submittedTasks) {

    return submittedTasks.stream()
        .map(st -> {
          Optional<WorkerNode> matchingNode = registeredNodes.stream()
              .filter(n -> n.getTaskId().equals(st.getTaskId()))
              .findFirst();
          if (matchingNode.isPresent()) {
            return new SubmittedTaskStatus(
                st.getTaskId(),
                getTaskStatus(matchingNode.get().getStatus())
            );
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public List<SubmittedTask> getTasks(List<UUID> taskIds) {
    return null;
  }

  @Override
  public WorkerMetaData getWorkerInformation() {
    return null;
  }

  private TaskStatus getTaskStatus(WorkerNodeStatus nodeStatus) {

    return switch (nodeStatus) {
      case EXECUTING -> TaskStatus.IN_PROGRESS;
      case COMPLETED -> TaskStatus.SUCCESS;
      case DEAD -> TaskStatus.FAILED;
      case IDLE -> TaskStatus.PENDING;
    };
  }

  private void discoverAndRegisterWorkerNodes() throws MasterException {

    registeredWorkerNodesInfo = MasterRESTEndpoint.configs.getWorkerNodes();
    for (WorkerNodeDiscoveryInfo nodeInfo : registeredWorkerNodesInfo) {
      WorkerNode workerNode;
      try {
        Class<?> c = Class.forName(nodeInfo.getType());
        Constructor<?> cons = c.getConstructor(String.class, String.class);
        workerNode = (WorkerNode) cons.newInstance(nodeInfo.getHost(), nodeInfo.getPort());
      } catch (Exception e) {
        throw new MasterException("Failed to initialize the worker node");
      }
      registeredNodes.add(workerNode);
    }
  }
}
