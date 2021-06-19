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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileMetaData;
import org.crunchycookie.orion.worker.exception.WorkerServerException;
import org.crunchycookie.orion.worker.store.TaskExecutionManager;
import org.crunchycookie.orion.worker.store.constants.TaskExecutionManagerConstants.OperationStatus;

/**
 * This {@link TaskExecutionManager} stores files on the nodes's primary storage by creating a
 * folder in the node's temporary files location.
 * <p>
 * References for each job process gets executed in the node is kept in-memory of this instance.
 */
public class PrimaryStorageBasedTaskExecutionManager implements TaskExecutionManager {

  private static final String TASKS_FOLDER = "tasks";
  private static final String FILE_TYPE_SEPARATOR = ".";

  private static volatile PrimaryStorageBasedTaskExecutionManager store = null;
  private static Monitor monitor = new Monitor();

  private final Map<String, Process> tasksLedger;

  private PrimaryStorageBasedTaskExecutionManager() {
    tasksLedger = new HashMap<>();
  }

  public static PrimaryStorageBasedTaskExecutionManager getInstance() {
    handleStoreInitialization();
    return store;
  }

  @Override
  public Optional<OutputStream> store(FileMetaData metaData) throws WorkerServerException {

    try {
      /*
      Files are stored in, <JAR-location>/tasks/<task-id>/<file-name>.
       */
      String storeWorkspaceFolder = getWorkspaceFolder();

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
      OutputStream outputStream = new FileOutputStream(file);

      return Optional.of(outputStream);
    } catch (IOException | URISyntaxException e) {
      throw new WorkerServerException("Unable to create the directories", e);
    }
  }

  private String getWorkspaceFolder() throws URISyntaxException {

    String locationOfTheCodeSource = PrimaryStorageBasedTaskExecutionManager.class
        .getProtectionDomain()
        .getCodeSource().getLocation().toURI().getPath();
    if (locationOfTheCodeSource.endsWith(File.separator)) {
      // Remove ending file separator.
      locationOfTheCodeSource = locationOfTheCodeSource
          .substring(0, locationOfTheCodeSource.length() - 1);
    }
    // Remove jar name.
    if (locationOfTheCodeSource.endsWith(".jar")) {
      locationOfTheCodeSource = locationOfTheCodeSource
          .substring(0, locationOfTheCodeSource.lastIndexOf(File.separator));
    }
    return locationOfTheCodeSource;
  }

  @Override
  public Pair<FileMetaData, InputStream> get(FileMetaData file) throws WorkerServerException {

    try {
      String filePath = getFilePath(file);
      return new ImmutablePair<>(file, new FileInputStream(filePath));
    } catch (IOException | URISyntaxException e) {
      throw new WorkerServerException("Unable to read the file", e);
    }
  }

  @Override
  public OperationStatus execute(FileMetaData executableFile) throws WorkerServerException {

    // Currently, the execution is only supported in Unix nodes.
    if (getTaskFromLedger(executableFile) != null && getTaskFromLedger(executableFile).isAlive()) {
      return OperationStatus.REJECTED_PROCESS_ALREADY_EXISTS;
    }
    try {
      // Apply execution permissions.
      applyPermisionCommand(getFilePath(executableFile), "chmod a+x");
      // Apply file IO permissions.
      applyPermisionCommand(getFileDirectory(executableFile), "chmod -R a+rwx");
      // Execute the script
      Process executableProcess = executeScript(executableFile);
      // Store process reference in ledger.
      putTaskInLedger(executableFile, executableProcess);
      return OperationStatus.SUCCESSFULLY_STARTED;
    } catch (URISyntaxException e) {
      throw new WorkerServerException("Unable to read the file", e);
    } catch (IOException e) {
      throw new WorkerServerException("Error while obtaining runtime process for the execution",
          e);
    } catch (InterruptedException e) {
      throw new WorkerServerException("Interrupted while obtaining runtime process for the "
          + "execution", e);
    }
  }

  @Override
  public OperationStatus getStatus(FileMetaData executableFile)
      throws WorkerServerException {

    Process executableTask = getTaskFromLedger(executableFile);
    if (executableTask == null || !executableTask.isAlive()) {
      return OperationStatus.IDLE;
    }
    return OperationStatus.BUSY;
  }

  @Override
  public OperationStatus remove(FileMetaData metaData) throws WorkerServerException {

    try {
      File taskFolder = new File(getFileDirectory(metaData));
      if (taskFolder.exists()) {
        FileUtils.forceDelete(taskFolder);
      }
      return OperationStatus.SUCCESS;
    } catch (URISyntaxException e) {
      throw new WorkerServerException("Error while getting directory path", e);
    } catch (IOException e) {
      throw new WorkerServerException("Unable to remove the file", e);
    }
  }

  @Override
  public String getName() {
    return "PrimaryStorageWorkerStore";
  }

  private Process executeScript(FileMetaData executableFile)
      throws URISyntaxException, IOException {

    String[] command = new String[]{"bash", getFilePath(executableFile)};
    ProcessBuilder pb = new ProcessBuilder(command)
        .directory(new File(getFileDirectory(executableFile)))
        .redirectOutput(new File(getFileDirectory(executableFile) + "/log.txt"))
        .redirectError(new File(getFileDirectory(executableFile) + "/error-log.txt"));
    Process executableProcess = pb.start();
    return executableProcess;
  }

  private void applyPermisionCommand(String path, String command)
      throws URISyntaxException, IOException, InterruptedException {
    String grantPermissionCommand = command + " " + path;
    Process permissionGrantingProcess = Runtime.getRuntime().exec(grantPermissionCommand);
    permissionGrantingProcess.waitFor();
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
    PrimaryStorageBasedTaskExecutionManager initializedStore = store;
    if (initializedStore == null) {
      monitor.enter();
      try {
        store = new PrimaryStorageBasedTaskExecutionManager();
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
    String storeWorkspaceFolder = getWorkspaceFolder();

    // Build file path.
    String filePath = storeWorkspaceFolder
        + (storeWorkspaceFolder.endsWith(File.separator) ? "" : File.separator)
        + TASKS_FOLDER
        + File.separator + file.getTaskId();
    return filePath;
  }

  private String getFileName(FileMetaData file) {
    return file.getName() + FILE_TYPE_SEPARATOR + file.getType();
  }

  private String getUniqueTaskId(FileMetaData executableFile) {
    return executableFile.getTaskId();
  }
}
