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

import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskLimits;

import org.crunchycookie.orion.master.manager.TaskManager;
import org.crunchycookie.orion.master.rest.model.TaskLimits;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RESTUtils {

  public static ResponseEntity<TaskLimits> getInternalServerErrorResponse() {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }
}
