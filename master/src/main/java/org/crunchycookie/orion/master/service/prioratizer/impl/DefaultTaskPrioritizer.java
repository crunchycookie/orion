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

import java.time.Instant;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.Priority;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.service.prioratizer.TaskPrioritizer;
import org.crunchycookie.orion.master.utils.RESTUtils.ResourceParams;

/**
 * This class implements earliest deadline first algorithm.
 */
public class DefaultTaskPrioritizer implements TaskPrioritizer {

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

    // Convert deadline to millis and derive priority value.
    double now = Instant.now().toEpochMilli();
    double priorityValue = ((Instant.MAX.toEpochMilli() - deadline.toEpochMilli()) * 100.0)
        / (Instant.MAX.toEpochMilli() - now);
    return new Priority(priorityValue);
  }
}
