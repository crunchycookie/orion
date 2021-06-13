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

/**
 * This class represent the store where worker related data is stored.
 */
package org.crunchycookie.orion.worker.store;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileMetaData;
import org.crunchycookie.orion.worker.exception.WorkerRuntimeException;
import org.crunchycookie.orion.worker.exception.WorkerServerException;
import org.crunchycookie.orion.worker.store.constants.TaskExecutionManagerConstants.OperationStatus;

/**
 * This class represents the Execution Manager. Within an actual physical node, there can be
 * multiple agents capable of executing tasks such as containers, etc. Each of such element need to
 * have an execution manager where they are managed by the worker service.
 */
public interface TaskExecutionManager {

  /**
   * Store a file in a task. This method creates a {@link FileOutputStream} based on the provide
   * {@link FileMetaData} details.
   *
   * @param file {@link FileMetaData} object representing file details.
   * @return The {@link FileOutputStream} where caller needs to write the file. Stream should be
   * properly closed by the caller.
   */
  Optional<OutputStream> store(FileMetaData file) throws WorkerServerException;

  /**
   * Given a file description, this API returns the stream to read it if available.
   *
   * @param file Metadata description.
   * @return The stream for the requested file and the file description.
   */
  Pair<FileMetaData, InputStream> get(FileMetaData file) throws WorkerServerException;

  /**
   * Execute given command in the workspace where the provided file resides. This is a non-blocking
   * method. In order to monitor the status of the process, use {@link #getStatus} method.
   *
   * @param executableFile Executable file description.
   * @return Status of the execution process.
   * @throws WorkerRuntimeException
   */
  OperationStatus execute(FileMetaData executableFile) throws WorkerServerException;

  /**
   * Obtain current status of the task associated to the provided executable file.
   *
   * @param executableFile Executable file description.
   * @return Current status.
   * @throws WorkerRuntimeException
   */
  OperationStatus getStatus(FileMetaData executableFile) throws WorkerServerException;

  /**
   * Remove a file object.
   *
   * @param metaData {@link FileMetaData} object representing file details.
   * @return Status of the action.
   */
  OperationStatus remove(FileMetaData metaData) throws WorkerServerException;

  /**
   * Name of this worker storage.
   *
   * @return Name.
   */
  String getName();
}
