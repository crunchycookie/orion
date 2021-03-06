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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.master.exception.MasterClientException;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.manager.TaskManager;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.file.IteratingTaskFile;
import org.crunchycookie.orion.master.models.file.TaskFile;
import org.crunchycookie.orion.master.models.file.TaskFileMetadata;
import org.crunchycookie.orion.master.rest.api.SubmitApiDelegate;
import org.crunchycookie.orion.master.rest.model.SubmittedTask;
import org.crunchycookie.orion.master.utils.MasterUtils;
import org.crunchycookie.orion.master.utils.RESTUtils;
import org.crunchycookie.orion.master.utils.RESTUtils.ResourceParams;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SubmitAPIImpl implements SubmitApiDelegate {

  private static final Logger logger = LogManager.getLogger(SubmitAPIImpl.class);

  @Override
  public ResponseEntity<SubmittedTask> submitTask(String executableShellScript,
      String outputFiles, String resourceRequirements, List<MultipartFile> filename) {

    try {
      // Build parameters.
      UUID taskId = UUID.randomUUID();
      org.crunchycookie.orion.master.models.SubmittedTask submittedTask = getSubmittedTask(
          executableShellScript,
          filename,
          taskId
      );
      populateResourceRequirements(submittedTask, resourceRequirements);
      populateOutputFiles(outputFiles, taskId, submittedTask);

      // Submit the task.
      TaskManager taskManager = MasterUtils.getTaskManager();
      SubmittedTaskStatus taskStatus = taskManager.submit(submittedTask);

      // Return the response.
      return switch (taskStatus.getStatus()) {
        case SUCCESS -> ResponseEntity.status(HttpStatus.ACCEPTED).body(new SubmittedTask()
            .taskId(taskId)
            .status(
                org.crunchycookie.orion.master.rest.model.SubmittedTaskStatus.SUCCESSFUL
            ));
        case IN_PROGRESS, PENDING -> ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(new SubmittedTask()
                .taskId(taskId)
                .status(
                    org.crunchycookie.orion.master.rest.model.SubmittedTaskStatus.INPROGRESS
                ));
        case FAILED -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SubmittedTask()
            .taskId(taskId)
            .status(
                org.crunchycookie.orion.master.rest.model.SubmittedTaskStatus.FAILED
            ));
      };
    } catch (Throwable t) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  private void populateOutputFiles(String outputFiles, UUID taskId,
      org.crunchycookie.orion.master.models.SubmittedTask submittedTask) {

    List<TaskFile> outputTaskFiles = Arrays.stream(outputFiles.split(","))
        .map(s -> {
          TaskFileMetadata meta = new TaskFileMetadata(
              s.split("\\.")[0],
              s.split("\\.")[1],
              taskId
          );
          return new IteratingTaskFile(
              meta,
              null
          );
        })
        .collect(Collectors.toList());
    submittedTask.setOutputFiles(outputTaskFiles);
  }

  private void populateResourceRequirements(
      org.crunchycookie.orion.master.models.SubmittedTask submittedTask,
      String requirements) throws MasterException {

    List<String> resourceRequirements = Arrays.stream(requirements.split(","))
        .collect(Collectors.toList());
    for (String resourceRequirement : resourceRequirements) {
      String[] resourceRequirementSplitted = resourceRequirement.split("=");
      ResourceParams requirement;
      try {
        requirement = ResourceParams.valueOf(resourceRequirementSplitted[0]);
      } catch (IllegalArgumentException e) {
        throw new MasterClientException("Provided resource requirement type is invalid");
      }
      submittedTask.setResourceRequirement(requirement, resourceRequirementSplitted[1]);
    }
  }

  private org.crunchycookie.orion.master.models.SubmittedTask getSubmittedTask(
      String executableShellScript, List<MultipartFile> filename, UUID taskId) throws IOException {

    return new org.crunchycookie.orion.master.models.SubmittedTask(
        taskId,
        RESTUtils.getTaskFiles(taskId, filename),
        new TaskFileMetadata(
            executableShellScript.split("\\.")[0],
            executableShellScript.split("\\.")[1],
            taskId
        )
    );
  }
}
