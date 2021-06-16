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

package org.crunchycookie.orion.master.service.validator;

import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.WorkerMetaData;

/**
 * This class represents a validator capable of determining whether a task is capable to be executed
 * with the current worker pool.
 */
public interface TaskCapacityValidator {

  /**
   * Analyze and provide whether the worker is capable of executing the submitted task. This method
   * throws a {@link org.crunchycookie.orion.master.exception.MasterClientException} if worker pool
   * does not have enough resources to execute the task.
   *
   * @param submittedTask
   * @param workerCapacity
   * @return
   */
  boolean validate(SubmittedTask submittedTask, WorkerMetaData workerCapacity)
      throws MasterException;
}
