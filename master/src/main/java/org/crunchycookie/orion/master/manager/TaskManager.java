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

package org.crunchycookie.orion.master.manager;

import java.util.List;
import org.crunchycookie.orion.master.models.ClientTask;
import org.crunchycookie.orion.master.models.ClientTaskStatus;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.TaskFile;
import org.crunchycookie.orion.master.models.TaskFileMetadata;
import org.crunchycookie.orion.master.models.WorkerMetaData;

/**
 * This represents the task manager which is responsible of the followings.
 * <p/>
 * 1. Provide resource limitation of a worker element.
 * <p/>
 * 2. Accept tasks from clients, and hand them to the scheduler.
 * <p/>
 * 3. Provide information about the task status to clients.
 * <p/>
 * 4. Upon completion, provide processed files to clients.
 */
public interface TaskManager {

  /**
   * Provide resource limitation of a worker element.
   *
   * @return Resource limits.
   */
  WorkerMetaData getTaskLimitations();

  /**
   * Accept tasks from clients, and hand them to the scheduler.
   *
   * @param clientTask Information about the tasks, including files.
   * @return Status of the submitted task. This contains the unique ID for the submitted task.
   */
  ClientTaskStatus submit(SubmittedTask submittedTask);

  /**
   * Provide information about the task status to clients.
   *
   * @param uniqueTaskId task Id.
   * @return Status of the submitted task.
   */
  ClientTaskStatus getTaskStatus(String uniqueTaskId);

  /**
   * Upon completion, provide processed files to clients.
   *
   * @param uniqueTaskId Task ID.
   * @param fileInformation Metadata of the files requesting.
   * @return Requested files.
   */
  List<TaskFile> getFiles(String uniqueTaskId, List<TaskFileMetadata> fileInformation);
}
