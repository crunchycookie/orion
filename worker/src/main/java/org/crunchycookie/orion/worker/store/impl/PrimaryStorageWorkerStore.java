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

package org.crunchycookie.orion.worker.store.impl;

import com.google.common.util.concurrent.Monitor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileMetaData;
import org.crunchycookie.orion.worker.exception.WorkerRuntimeException;
import org.crunchycookie.orion.worker.store.WorkerStore;
import org.crunchycookie.orion.worker.store.constants.StoreConstants.OperationStatus;

/**
 * This {@link WorkerStore} stores files on the nodes's primary storage by creating a folder in the
 * node's temporary files location.
 *
 * References for each job process gets executed in the node is kept in-memory of this instance.
 */
public class PrimaryStorageWorkerStore implements WorkerStore {

  private static final String TASKS_FOLDER = "tasks";
  private static final String FILE_TYPE_SEPARATOR = ".";

  private static volatile PrimaryStorageWorkerStore store = null;
  private static Monitor monitor = new Monitor();

  private final Map<String, Process> tasksLedger;

  private PrimaryStorageWorkerStore() {
    tasksLedger = new HashMap<>();
  }

  public static PrimaryStorageWorkerStore getInstance() {
    handleStoreInitialization();
    return store;
  }

  @Override
  public Optional<FileOutputStream> store(FileMetaData metaData) throws WorkerRuntimeException {

    try {
      /*
      Files are stored in, <JAR-location>/tasks/<task-id>/<file-name>.
       */
      String storeWorkspaceFolder = PrimaryStorageWorkerStore.class.getProtectionDomain()
          .getCodeSource().getLocation().toURI().getPath();

      // Create tasks folder.
      String tasksFolderPath = createFolder(storeWorkspaceFolder, TASKS_FOLDER);

      // Create the folder corresponding to the task id.
      String taskFolderPath = createFolder(tasksFolderPath, metaData.getTaskId());

      // Delete file if exists.
      File file = new File(taskFolderPath + File.separator + metaData.getName()
          + FILE_TYPE_SEPARATOR + metaData.getType());
      if (file.exists()) {
        FileUtils.delete(file);
      }

      // Open stream to write the file.
      FileOutputStream fileOutputStream = new FileOutputStream(file);

      return Optional.of(fileOutputStream);
    } catch (IOException | URISyntaxException e) {
      throw new WorkerRuntimeException("Unable to create the directories", e);
    }
  }

  @Override
  public Pair<FileMetaData, FileInputStream> get(FileMetaData file) throws WorkerRuntimeException {

    try {
      String filePath = getFilePath(file);
      return new ImmutablePair<>(file, new FileInputStream(filePath));
    } catch (IOException | URISyntaxException e) {
      throw new WorkerRuntimeException("Unable to read the file", e);
    }
  }

  @Override
  public Enum<OperationStatus> execute(FileMetaData executableFile) throws WorkerRuntimeException {

    if (getTaskFromLedger(executableFile).isAlive()) {
      return OperationStatus.REJECTED_PROCESS_ALREADY_EXISTS;
    }
    try {
      String workingDirectoryPath = getFileDirectory(executableFile);
      ProcessBuilder executableProcessBuilder = new ProcessBuilder(getFileName(executableFile));
      executableProcessBuilder.directory(new File(workingDirectoryPath));

      Process executableProcess = executableProcessBuilder.start();
      putTaskInLedger(executableFile, executableProcess);

      return OperationStatus.SUCCESSFULLY_STARTED;
    } catch (URISyntaxException e) {
      throw new WorkerRuntimeException("Unable to read the file", e);
    } catch (IOException e) {
      throw new WorkerRuntimeException("Error while obtaining runtime process for the execution",
          e);
    }
  }

  @Override
  public Enum<OperationStatus> getStatus(FileMetaData executableFile)
      throws WorkerRuntimeException {
    return null;
  }

  @Override
  public Enum<OperationStatus> remove(FileMetaData metaData) {
    return null;
  }

  @Override
  public String getName() {
    return "PrimaryStorageWorkerStore";
  }

  private Process putTaskInLedger(FileMetaData executableFile, Process executableProcess) {
    return tasksLedger.put(getUniqueTaskId(executableFile), executableProcess);
  }

  private Process getTaskFromLedger(FileMetaData executableFile) {
    return tasksLedger.get(getUniqueTaskId(executableFile));
  }

  private String createFolder(String parent, String folder) throws IOException {
    String tasksFolderPath = parent + (parent.endsWith(File.separator) ? "" : File.separator)
        + folder;
    File tasksFolder = new File(tasksFolderPath);
    if (!tasksFolder.exists()) {
      FileUtils.forceMkdir(tasksFolder);
    }
    return tasksFolderPath;
  }

  private static void handleStoreInitialization() {
    // This ensures we access volatile store only once, thus improving the performance.
    PrimaryStorageWorkerStore initializedStore = store;
    if (initializedStore == null) {
      monitor.enter();
      try {
        store = new PrimaryStorageWorkerStore();
      } finally {
        monitor.leave();
      }
    }
  }

  private String getFilePath(FileMetaData file) throws URISyntaxException {
    String fileDirectoryPath = getFileDirectory(file);
    return fileDirectoryPath + File.separator + getFileName(file);
  }

  private String getFileDirectory(FileMetaData file) throws URISyntaxException {
    /*
      Files are stored in, <JAR-location>/tasks/<task-id>/<file-name>.
    */
    String storeWorkspaceFolder = PrimaryStorageWorkerStore.class.getProtectionDomain()
        .getCodeSource().getLocation().toURI().getPath();

    // Build file path.
    String filePath = storeWorkspaceFolder
        + File.separator + TASKS_FOLDER
        + File.separator + file.getTaskId();
    return filePath;
  }

  private String getFileName(FileMetaData file) {
    return file.getName() + FILE_TYPE_SEPARATOR + file.getName();
  }

  private String getUniqueTaskId(FileMetaData executableFile) {
    return executableFile.getTaskId() + "-" + executableFile.getName() + "-" + executableFile
        .getType();
  }
}
