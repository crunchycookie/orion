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

import java.util.List;
import java.util.Map;
import org.crunchycookie.orion.worker.WorkerOuterClass.Task;

/**
 * This class represents a task submitted from a client.
 */
public class ClientTask {

  private List<Task> subTasks;
  private String deadline;
  private Map<String, String> additionalAttributes;

  public ClientTask(List<Task> subTasks, String deadline,
      Map<String, String> additionalAttributes) {
    this.subTasks = subTasks;
    this.deadline = deadline;
    this.additionalAttributes = additionalAttributes;
  }

  public List<Task> getSubTasks() {
    return subTasks;
  }

  public void setSubTasks(List<Task> subTasks) {
    this.subTasks = subTasks;
  }

  public String getDeadline() {
    return deadline;
  }

  public void setDeadline(String deadline) {
    this.deadline = deadline;
  }

  public Map<String, String> getAdditionalAttributes() {
    return additionalAttributes;
  }

  public void setAdditionalAttributes(Map<String, String> additionalAttributes) {
    this.additionalAttributes = additionalAttributes;
  }
}
