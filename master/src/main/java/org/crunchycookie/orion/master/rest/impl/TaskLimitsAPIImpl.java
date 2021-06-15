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

import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskLimits;
import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskManager;
import static org.crunchycookie.orion.master.utils.RESTUtils.getInternalServerErrorResponse;

import org.crunchycookie.orion.master.models.WorkerMetaData;
import org.crunchycookie.orion.master.rest.api.TaskLimitsApiDelegate;
import org.crunchycookie.orion.master.rest.model.TaskLimits;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class TaskLimitsAPIImpl implements TaskLimitsApiDelegate {

  @Override
  public ResponseEntity<TaskLimits> getTaskLimitations() {
    try {
      WorkerMetaData workerMetaData = getTaskManager().getTaskLimitations();
      if (workerMetaData == null) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok(getTaskLimits(workerMetaData));
    } catch (Throwable e) {
      return getInternalServerErrorResponse();
    }
  }
}
