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

import java.io.InputStream;
import java.io.OutputStream;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileMetaData;

/**
 * This represent a file download response received from the {@link Worker}.
 */
public class FileDownloadResponse {

  private FileMetaData metaData;
  private OutputStream fileStream;

  public FileDownloadResponse(FileMetaData metaData, OutputStream fileStream) {
    this.metaData = metaData;
    this.fileStream = fileStream;
  }

  public FileMetaData getMetaData() {
    return metaData;
  }

  public void setMetaData(FileMetaData metaData) {
    this.metaData = metaData;
  }

  public OutputStream getFileStream() {
    return fileStream;
  }

  public void setFileStream(OutputStream fileStream) {
    this.fileStream = fileStream;
  }
}
