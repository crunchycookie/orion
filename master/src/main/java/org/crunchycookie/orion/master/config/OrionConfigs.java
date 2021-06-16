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

package org.crunchycookie.orion.master.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.crunchycookie.orion.master.config.worker.WorkerNodeDiscoveryInfo;
import org.crunchycookie.orion.master.constants.MasterConstants.ErrorCodes;
import org.crunchycookie.orion.master.exception.MasterException;

public class OrionConfigs {

  private String configFilePath;
  private List<WorkerNodeDiscoveryInfo> workerNodes = new ArrayList<>();
  private Properties configs;

  public OrionConfigs(String configFilePath) {

    this.configFilePath = configFilePath;

    Properties orionConfigs = new Properties();
    try {
      orionConfigs.load(this.getClass().getClassLoader().getResourceAsStream(this.configFilePath));
    } catch (IOException e) {
      throw new RuntimeException("Failed to load configs");
    }

    // Get worker nodes.
    for (int i = 0; ; i++) {
      if (!orionConfigs.containsKey("WorkerNode\\." + i + "\\.host")) {
        break;
      }
      WorkerNodeDiscoveryInfo nodeInfo = new WorkerNodeDiscoveryInfo();
      nodeInfo.setHost(orionConfigs.getProperty("WorkerNode\\." + i + "\\.host"));
      nodeInfo.setType(orionConfigs.getProperty("WorkerNode\\." + i + "\\.port"));
      nodeInfo.setType(orionConfigs.getProperty("WorkerNode\\." + i + "\\.type"));
      this.workerNodes.add(nodeInfo);
    }

    this.configs = orionConfigs;
  }

  public List<WorkerNodeDiscoveryInfo> getWorkerNodes() {
    return workerNodes;
  }

  public String getConfig(String key) {

    return this.configs.getProperty(key);
  }
}
