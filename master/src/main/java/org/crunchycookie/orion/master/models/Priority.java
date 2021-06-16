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

package org.crunchycookie.orion.master.models;

import org.crunchycookie.orion.master.constants.MasterConstants.ErrorCodes;
import org.crunchycookie.orion.master.exception.MasterException;

/**
 * This class represent a priority value. Priority is rated between 0-100 in the ascending order.
 */
public class Priority implements Comparable<Priority> {

  private Integer priority = 0;

  public Priority(Integer priority) throws MasterException {
    if (priority < 0 || priority > 100) {
      throw new MasterException(ErrorCodes.INTERNAL_SERVER_ERROR,
          "Priority value must resides between 0 and 100");
    }
    this.priority = priority;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  @Override
  public int compareTo(Priority priority2) {
    return priority.compareTo(priority2.getPriority());
  }
}
