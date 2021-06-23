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

package org.crunchycookie.orion.master.state;

import java.util.ArrayList;
import java.util.List;
import org.crunchycookie.orion.master.rest.model.MonitorResult;

public class MasterStateManager {

  private State state = new State();

  private MasterStateManager() {
  }

  public enum MasterStateManagerSingleton {
    INSTANCE;

    private MasterStateManager manager;

    MasterStateManagerSingleton() {
      manager = new MasterStateManager();
    }

    public MasterStateManager get() {
      return manager;
    }
  }

  public class State {

    private List<MonitorResult> queue = new ArrayList<>();
    private List<MonitorResult> workerPool = new ArrayList<>();
    private List<MonitorResult> centralStorage = new ArrayList<>();

    public List<MonitorResult> getQueue() {
      return queue;
    }

    public void setQueue(List<MonitorResult> queue) {
      this.queue = queue;
    }

    public List<MonitorResult> getWorkerPool() {
      return workerPool;
    }

    public void setWorkerPool(
        List<MonitorResult> workerPool) {
      this.workerPool = workerPool;
    }

    public List<MonitorResult> getCentralStorage() {
      return centralStorage;
    }

    public void setCentralStorage(
        List<MonitorResult> centralStorage) {
      this.centralStorage = centralStorage;
    }
  }

  public static MasterStateManager getInstance() {
    return MasterStateManagerSingleton.INSTANCE.get();
  }

  public State getState() {
    return state;
  }

  public void addToQueue(List<MonitorResult> result) {
    this.getState().setQueue(result);
  }

  public void addToWorkerPool(List<MonitorResult> result) {
    this.getState().setWorkerPool(result);
  }

  public void addToCentralStorage(List<MonitorResult> result) {
    this.getState().setCentralStorage(result);
  }
}
