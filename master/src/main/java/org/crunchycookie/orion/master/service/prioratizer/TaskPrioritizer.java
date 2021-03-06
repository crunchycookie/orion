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

package org.crunchycookie.orion.master.service.prioratizer;

import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.Priority;
import org.crunchycookie.orion.master.models.SubmittedTask;

/**
 * This class represents a prioritizer capable of rating tasks based on it's qualities.
 */
public interface TaskPrioritizer {

  /**
   * Analyze provided client task and predict a priority value.
   *
   * @param submittedTask Submitted task.
   * @return Predicted {@link Priority} entity.
   */
  Priority getPriority(SubmittedTask submittedTask) throws MasterException;
}
