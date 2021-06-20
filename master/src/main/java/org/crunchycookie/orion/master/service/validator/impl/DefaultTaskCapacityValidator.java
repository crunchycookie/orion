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

package org.crunchycookie.orion.master.service.validator.impl;

import static org.crunchycookie.orion.master.constants.MasterConstants.ComponentID.COMPONENT_ID_TASK_VALIDATOR;
import static org.crunchycookie.orion.master.utils.MasterUtils.getLogMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.master.constants.MasterConstants.ComponentID;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.WorkerMetaData;
import org.crunchycookie.orion.master.service.validator.TaskCapacityValidator;
import org.crunchycookie.orion.master.utils.RESTUtils.ResourceParams;

public class DefaultTaskCapacityValidator implements TaskCapacityValidator {

  private static final Logger logger = LogManager.getLogger(DefaultTaskCapacityValidator.class);

  private DefaultTaskCapacityValidator() {
  }

  public enum DefaultTaskCapacityValidatorSingleton {
    INSTANCE;

    private TaskCapacityValidator taskCapacityValidator;

    DefaultTaskCapacityValidatorSingleton() {
      taskCapacityValidator = new DefaultTaskCapacityValidator();
    }

    public TaskCapacityValidator get() {
      return taskCapacityValidator;
    }
  }

  public static TaskCapacityValidator getInstant() {

    return DefaultTaskCapacityValidatorSingleton.INSTANCE.get();
  }

  @Override
  public boolean validate(SubmittedTask submittedTask, WorkerMetaData workerCapacity)
      throws MasterException {

    if (logger.isDebugEnabled()) {
      logger.debug(getLogMessage(getComponentId(), submittedTask.getTaskId(), "Validating task"));
    }
    // Valid if memory limits are met.
    return Integer.parseInt(submittedTask.getResourceRequirement(ResourceParams.MEMORY)) <= Integer
        .parseInt(workerCapacity.getMaxResourceCapacities().get(ResourceParams.MEMORY));
  }

  private ComponentID getComponentId() {
    return COMPONENT_ID_TASK_VALIDATOR;
  }
}
