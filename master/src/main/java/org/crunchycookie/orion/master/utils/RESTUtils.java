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

package org.crunchycookie.orion.master.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.crunchycookie.orion.master.models.file.StreamingTaskFile;
import org.crunchycookie.orion.master.models.file.TaskFile;
import org.crunchycookie.orion.master.models.file.TaskFileMetadata;
import org.crunchycookie.orion.master.rest.model.TaskLimits;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public class RESTUtils {

  public enum ResourceParams {
    MEMORY,
    DEADLINE, // Time as a resource.
    STORAGE;
  }

  public static ResponseEntity<TaskLimits> getInternalServerErrorResponse() {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }

  public static List<TaskFile> getTaskFiles(UUID taskId, List<MultipartFile> files)
      throws IOException {

    List<TaskFile> taskFiles = new ArrayList<>();
    for (MultipartFile file : files) {
      String[] splittedFileName = file.getOriginalFilename().split("\\.");
      taskFiles.add(new StreamingTaskFile(
          new TaskFileMetadata(
              splittedFileName[0],
              splittedFileName[1],
              taskId
          ),
          file.getInputStream())
      );
    }
    return taskFiles;
  }
}
