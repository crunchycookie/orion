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

import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskStatus;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.crunchycookie.orion.master.RESTfulEndpoint;
import org.crunchycookie.orion.master.config.worker.WorkerNodeDiscoveryInfo;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.WorkerMetaData;
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

  public static WorkerPoolManager getInstant() {

    return DefaultWorkerPoolManagerSingleton.INSTANCE.get();
  }

  @Override
  public void init() throws MasterException {

    discoverAndRegisterWorkerNodes();
  }

  @Override
  public Optional<WorkerNode> getFreeWorker() {

    return getRegisteredNodes().stream().filter(n -> n.getStatus() == WorkerNodeStatus.IDLE)
        .findFirst();
  }

  @Override
  public Optional<WorkerNode> getWorker(UUID id) {

    return getRegisteredNodes().stream().filter(n -> n.getId().equals(id)).findFirst();
  }

  @Override
  public List<SubmittedTaskStatus> getStatus(List<SubmittedTask> submittedTasks) {

    return submittedTasks.stream()
        .map(st -> {
          Optional<WorkerNode> executionNode = getExecutionNode(st);
          if (executionNode.isPresent()) {
            return new SubmittedTaskStatus(
                st.getTaskId(),
                getTaskStatus(executionNode.get().getStatus())
            );
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public List<SubmittedTask> getTasks(List<UUID> taskIds) throws MasterException {

    // Get the worker nodes executing the requested tasks.
    List<WorkerNode> executionNodes = getExecutionNodes(taskIds);

    // Tasks can only be obtained if the task is not under the execution currently.
    return obtainNonExecutingTasks(executionNodes);
  }

  @Override
  public WorkerMetaData getWorkerInformation() {

    return RESTfulEndpoint.configs.getWorkerMetaData();
  }

  protected List<WorkerNode> getRegisteredNodes() {

    return registeredNodes;
  }

  protected void registerNode(WorkerNode node) {

    registeredNodes.add(node);
  }

  private List<SubmittedTask> obtainNonExecutingTasks(List<WorkerNode> executionNodes)
      throws MasterException {

    List<SubmittedTask> tasks = new ArrayList<>();
    for (WorkerNode node : executionNodes) {
      if (!node.getStatus().equals(WorkerNodeStatus.EXECUTING)) {
        tasks.add(node.obtain());
      }
    }
    return tasks;
  }

  private List<WorkerNode> getExecutionNodes(List<UUID> taskIds) {

    List<WorkerNode> executionNodes = getRegisteredNodes().stream()
        .filter(n -> taskIds.contains(n.getTaskId().get()))
        .collect(Collectors.toList());
    return executionNodes;
  }

  private Optional<WorkerNode> getExecutionNode(SubmittedTask st) {

    Optional<WorkerNode> matchingNode = getRegisteredNodes().stream()
        .filter(n -> n.getTaskId().equals(st.getTaskId()))
        .findFirst();
    return matchingNode;
  }

  private void discoverAndRegisterWorkerNodes() throws MasterException {

    registeredWorkerNodesInfo = RESTfulEndpoint.configs.getWorkerNodes();
    for (WorkerNodeDiscoveryInfo nodeInfo : registeredWorkerNodesInfo) {
      WorkerNode workerNode;
      try {
        Class<?> c = Class.forName(nodeInfo.getType());
        Constructor<?> cons = c.getConstructor(String.class, String.class);
        workerNode = (WorkerNode) cons.newInstance(nodeInfo.getHost(), nodeInfo.getPort());
      } catch (Exception e) {
        throw new MasterException("Failed to initialize the worker node");
      }
      registerNode(workerNode);
    }
  }
}
