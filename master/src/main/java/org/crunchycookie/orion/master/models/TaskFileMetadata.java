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

import java.util.UUID;

public class TaskFileMetadata {

  private String fileName;
  private String fileType;
  private UUID taskId;

  public TaskFileMetadata(String fileName, String fileType, UUID taskId) {
    this.fileName = fileName;
    this.fileType = fileType;
    this.taskId = taskId;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public UUID getTaskId() {
    return taskId;
  }

  public void setTaskId(UUID taskId) {
    this.taskId = taskId;
  }

  @Override
  public String toString() {
    return taskId + "-" + fileName + "-" + fileType;
  }

  @Override
  public boolean equals(Object obj) {

    if (obj instanceof TaskFileMetadata) {
      return this.toString().equals(obj.toString());
    }
    return super.equals(obj);
  }
}
