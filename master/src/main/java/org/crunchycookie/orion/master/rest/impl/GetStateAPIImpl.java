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

package org.crunchycookie.orion.master.rest.impl;

import org.crunchycookie.orion.master.rest.api.GetStateApiDelegate;
import org.crunchycookie.orion.master.rest.model.ClusterStatus;
import org.crunchycookie.orion.master.state.MasterStateManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class GetStateAPIImpl implements GetStateApiDelegate {

  @Override
  public ResponseEntity<ClusterStatus> getState() {

    MasterStateManager manager = MasterStateManager.getInstance();

    final ClusterStatus clusterStatus = new ClusterStatus();
    clusterStatus.setCentralStore(manager.getState().getCentralStorage());
    clusterStatus.setPriorityQueue(manager.getState().getQueue());
    clusterStatus.setWorkerPool(manager.getState().getWorkerPool());

    return ResponseEntity.ok(clusterStatus);
  }
}
