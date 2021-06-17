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

import static org.crunchycookie.orion.master.utils.MasterUtils.getTaskManager;

import java.util.List;
import java.util.UUID;
import org.crunchycookie.orion.master.exception.MasterClientException;
import org.crunchycookie.orion.master.models.TaskFileStream;
import org.crunchycookie.orion.master.models.TaskFileMetadata;
import org.crunchycookie.orion.master.rest.api.TasksApiDelegate;
import org.crunchycookie.orion.master.rest.model.SubmittedTaskStatus;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class TasksAPIImpl implements TasksApiDelegate {

  @Override
  public ResponseEntity<Resource> downloadFiles(UUID taskId, String filename) {

    try {
      TaskFileMetadata taskFileMetadata = new TaskFileMetadata(
          filename.split("\\.")[0],
          filename.split("\\.")[1],
          taskId
      );
      List<TaskFileStream> taskFileStreams = getTaskManager().getFiles(taskId, List.of(taskFileMetadata));
      return ResponseEntity.ok(new InputStreamResource(taskFileStreams.get(0).getFileDataStream()));
    } catch (Throwable t) {
      if (t instanceof MasterClientException) {
        return switch (((MasterClientException) t).getErrorCode()) {
          case ERROR_FILE_DOWNLOAD_STILL_IN_PROGRESS, ERROR_FILE_DOWNLOAD_FAILED -> ResponseEntity
              .badRequest().build();
          default -> getInternalServerErrorResponse();
        };
      }
      return getInternalServerErrorResponse();
    }
  }

  private ResponseEntity<Resource> getInternalServerErrorResponse() {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }

  @Override
  public ResponseEntity<SubmittedTaskStatus> monitorFiles(UUID taskId) {

    try {
      org.crunchycookie.orion.master.models.SubmittedTaskStatus status = getTaskManager()
          .getTaskStatus(taskId);
      SubmittedTaskStatus responseStatus = switch (status.getStatus()) {
        case IN_PROGRESS -> SubmittedTaskStatus.INPROGRESS;
        case SUCCESS -> SubmittedTaskStatus.SUCCESSFUL;
        case FAILED -> SubmittedTaskStatus.FAILED;
      };
      return ResponseEntity.ok(responseStatus);
    } catch (Throwable t) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
