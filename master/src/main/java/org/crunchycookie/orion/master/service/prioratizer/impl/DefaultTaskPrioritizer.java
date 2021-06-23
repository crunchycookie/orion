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

package org.crunchycookie.orion.master.service.prioratizer.impl;

import static org.crunchycookie.orion.master.constants.MasterConstants.ComponentID.COMPONENT_ID_TASK_PRIORATIZER;

import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.master.constants.MasterConstants.ComponentID;
import org.crunchycookie.orion.master.exception.MasterClientException;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.Priority;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.service.prioratizer.TaskPrioritizer;
import org.crunchycookie.orion.master.utils.RESTUtils.ResourceParams;

/**
 * This class implements earliest deadline first algorithm.
 */
public class DefaultTaskPrioritizer implements TaskPrioritizer {

  private static final Logger logger = LogManager.getLogger(DefaultTaskPrioritizer.class);

  private DefaultTaskPrioritizer() {
  }

  public enum DefaultTaskPrioritizerSingleton {
    INSTANCE;

    private TaskPrioritizer taskPrioritizer;

    DefaultTaskPrioritizerSingleton() {
      taskPrioritizer = new DefaultTaskPrioritizer();
    }

    public TaskPrioritizer get() {
      return taskPrioritizer;
    }
  }

  public static TaskPrioritizer getInstant() {

    return DefaultTaskPrioritizerSingleton.INSTANCE.get();
  }

  @Override
  public Priority getPriority(SubmittedTask submittedTask) throws MasterException {

    // Deadline is in UTC.
    Instant deadline = Instant.parse(submittedTask.getResourceRequirement(ResourceParams.DEADLINE));

    Instant now = Instant.now();

    if (deadline.isBefore(now)) {
      // TODO: 2021-06-19 Handle expired tasks. Catch this exception at the top level and update the status to failed or expired in the central storage.
      throw new MasterClientException("Task is expired");
    }

    // Lowest value means highest priority.
    return new Priority(Long.valueOf((
        deadline.toEpochMilli() - Instant.parse("2020-01-01T20:34:11Z").toEpochMilli()
    )).doubleValue());
  }

  private ComponentID getComponentId() {
    return COMPONENT_ID_TASK_PRIORATIZER;
  }
}
