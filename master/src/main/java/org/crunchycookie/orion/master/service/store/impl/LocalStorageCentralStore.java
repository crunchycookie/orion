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

package org.crunchycookie.orion.master.service.store.impl;

import static org.crunchycookie.orion.master.constants.MasterConstants.ComponentID.COMPONENT_ID_CENTRAL_STORE;
import static org.crunchycookie.orion.master.utils.MasterUtils.getLogMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.master.RESTfulEndpoint;
import org.crunchycookie.orion.master.constants.MasterConstants.ComponentID;
import org.crunchycookie.orion.master.constants.MasterConstants.ErrorCodes;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus.TaskStatus;
import org.crunchycookie.orion.master.models.file.StreamingTaskFile;
import org.crunchycookie.orion.master.models.file.TaskFile;
import org.crunchycookie.orion.master.models.file.TaskFileMetadata;
import org.crunchycookie.orion.master.service.store.CentralStore;
import org.crunchycookie.orion.master.utils.RESTUtils.ResourceParams;

public class LocalStorageCentralStore implements CentralStore {

  public static final String TASK_FOLDER_BACKUP_SUFFIX = "__backup";
  private static final Logger logger = LogManager.getLogger(LocalStorageCentralStore.class);
  private final String workspace;

  private LocalStorageCentralStore() {

    String workspaceFromConfigs = RESTfulEndpoint.configs
        .getConfig("LocalStorageCentralStore.workspace");
    workspace = workspaceFromConfigs.endsWith(File.separator) ?
        workspaceFromConfigs + "orion-workspace" + File.separator
        : workspaceFromConfigs + File.separator + "orion-workspace" + File.separator;

    // Create an isolated directory for the storage.
    File isolatedWorkspace = new File(workspace);
    try {
      if (!isolatedWorkspace.exists()) {
        FileUtils.forceMkdir(isolatedWorkspace);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed during initialization");
    }
  }

  public enum LocalStorageCentralStoreSingleton {
    INSTANCE;

    private CentralStore centralStore;

    LocalStorageCentralStoreSingleton() {
      centralStore = new LocalStorageCentralStore();
    }

    public CentralStore get() {
      return centralStore;
    }
  }

  public static CentralStore getInstant() {

    return LocalStorageCentralStoreSingleton.INSTANCE.get();
  }

  @Override
  public void store(List<SubmittedTask> submittedTasks) throws MasterException {

    for (SubmittedTask task : submittedTasks) {
      store(task);
    }
  }

  @Override
  public void store(SubmittedTask submittedTask) throws MasterException {
    
    if (logger.isDebugEnabled()) {
      logger.debug(getLogMessage(getComponentId(), submittedTask.getTaskId(), "Storing the task"));
    }
    try {
      /*
      Create task folder. Need to backup before delete if an existing one found. This is to
      support scenarios where one reads from the existing folder, do some modifications and
      store it back. At that point if we delete the existing directory initially this operation
      will fail.
      */
      String tempTaskFolderName = submittedTask.getTaskId().toString() + TASK_FOLDER_BACKUP_SUFFIX;
      File tempTaskFolder = forceCreate(tempTaskFolderName, true);

      // Create input files folder.
      File inputFilesFolder = forceCreate(
          tempTaskFolderName + File.separator + "inputs", true);
      storeFiles(submittedTask.getTaskFiles(), inputFilesFolder);

      // Create output files folder.
      File outputsFilesFolder = forceCreate(
          tempTaskFolderName + File.separator + "outputs", true);
      storeFiles(submittedTask.getOutputFiles(), outputsFilesFolder);

      // Persist metadata.
      Properties properties = new Properties();

      // Persist meta.
      properties.put("META.task-id", submittedTask.getTaskId().toString());
      properties.put("META.worker-id", getString(submittedTask.getWorkerId()));
      properties.put("META.executable-file", submittedTask.getExecutable().toString());
      properties.put("META.status", submittedTask.getStatus().getStatus().toString());

      // Persist resource limits.
      properties.put("RESOURCE.LIMITS.MEMORY",
          submittedTask.getResourceRequirement(ResourceParams.MEMORY));
      properties.put("RESOURCE.LIMITS.STORAGE",
          submittedTask.getResourceRequirement(ResourceParams.STORAGE));
      properties.put("RESOURCE.LIMITS.DEADLINE",
          submittedTask.getResourceRequirement(ResourceParams.DEADLINE));

      // Persist meta file.
      properties
          .store(new FileOutputStream(tempTaskFolder + File.separator + "meta.properties"), null);

      // Clear backup folder and copy to the original.
      String tempTaskFolderPath = tempTaskFolder.getAbsolutePath();
      File taskFolder = new File(
          tempTaskFolderPath.substring(0, tempTaskFolderPath.lastIndexOf(TASK_FOLDER_BACKUP_SUFFIX))
      );
      forceCreateFile(taskFolder);
      FileUtils.copyDirectory(tempTaskFolder, taskFolder);
      FileUtils.deleteDirectory(tempTaskFolder);
    } catch (IOException e) {
      // TODO: 2021-06-17 Add error code
      throw new MasterException("Failed to create file IOs", e);
    }
  }

  @Override
  public SubmittedTask get(UUID taskId) throws MasterException {
    
    if (logger.isDebugEnabled()) {
      logger.debug(getLogMessage(getComponentId(), taskId, "Getting the task by ID"));
    }
    try {
      // Get task folder.
      File taskFolder = getFile(taskId.toString(), true);

      // Get input files.
      File inputFilesFolder = getFile(taskFolder + File.separator + "inputs", false);
      List<TaskFile> inputs = getFiles(taskId, inputFilesFolder);

      // Get output files.
      File outputFilesFolder = getFile(taskFolder + File.separator + "outputs", false);
      List<TaskFile> outputs = getFiles(taskId, outputFilesFolder);

      // Get meta.
      Properties properties = new Properties();
      properties.load(new FileInputStream(taskFolder + File.separator + "meta.properties"));

      // Get worker id.
      String worker = getWorkerId((String) properties.get("META.worker-id"));

      // Get status.
      TaskStatus status = TaskStatus.valueOf((String) properties.get("META.status"));

      // Get executable.
      String executableFile = (String) properties.get("META.executable-file");
      TaskFileMetadata executable = TaskFileMetadata.parseString(executableFile);

      // Get resource limits.
      Map<ResourceParams, String> resourceLimits = new HashMap<>();
      resourceLimits.put(ResourceParams.MEMORY, (String) properties.get("RESOURCE.LIMITS.MEMORY"));
      resourceLimits
          .put(ResourceParams.DEADLINE, (String) properties.get("RESOURCE.LIMITS.DEADLINE"));
      resourceLimits
          .put(ResourceParams.STORAGE, (String) properties.get("RESOURCE.LIMITS.STORAGE"));

      // Build task.
      SubmittedTask submittedTask = new SubmittedTask(
          taskId,
          inputs,
          executable
      );
      submittedTask.setOutputFiles(outputs);
      submittedTask.setWorkerId(worker);
      submittedTask.setStatus(new SubmittedTaskStatus(taskId, status));
      submittedTask.setResourceRequirements(resourceLimits);

      return submittedTask;
    } catch (IOException e) {
      throw new MasterException(ErrorCodes.INTERNAL_SERVER_ERROR, "Failed to get data", e);
    }
  }

  @Override
  public List<SubmittedTask> get(List<UUID> taskIds) throws MasterException {

    List<SubmittedTask> submittedTasks = new ArrayList<>();
    for (UUID taskId : taskIds) {
      submittedTasks.add(get(taskId));
    }
    return submittedTasks;
  }

  @Override
  public List<SubmittedTask> get(TaskStatus status) throws MasterException {

    // Identify tasks for the given status.
    List<UUID> retrievingTasks = getTaskIDs(status);

    // return identified tasks.
    return get(retrievingTasks);
  }

  @Override
  public List<TaskFile> getFiles(UUID taskId, List<TaskFileMetadata> requestedFiles)
      throws MasterException {

    if (logger.isDebugEnabled()) {
      logger.debug(getLogMessage(getComponentId(), taskId, "Getting files"));
    }
    SubmittedTask submittedTask = get(taskId);
    List<TaskFile> taskFiles = new ArrayList<>();
    for (TaskFileMetadata requestedFile : requestedFiles) {
      // Search in input files.
      search(submittedTask.getTaskFiles(), requestedFile, taskFiles);
      // Search in output files.
      search(submittedTask.getOutputFiles(), requestedFile, taskFiles);
    }
    return taskFiles;
  }

  @Override
  public void remove(UUID taskId) throws MasterException {

    File taskFolder = getFile(taskId.toString(), true);
    taskFolder.delete();
  }

  @Override
  public SubmittedTaskStatus getStatus(UUID taskId) throws MasterException {

    if (logger.isDebugEnabled()) {
      logger.debug(getLogMessage(getComponentId(), taskId, "Getting status"));
    }
    return get(taskId).getStatus();
  }

  @Override
  public void setStatus(UUID taskId, SubmittedTaskStatus status) throws MasterException {

    if (logger.isDebugEnabled()) {
      logger.debug(getLogMessage(getComponentId(), taskId, "Setting the status"));
    }

    SubmittedTask submittedTask = get(taskId);
    submittedTask.setStatus(status);

    store(submittedTask);
  }

  private ComponentID getComponentId() {
    return COMPONENT_ID_CENTRAL_STORE;
  }

  private void search(List<TaskFile> files, TaskFileMetadata requestedFile,
      List<TaskFile> taskRepository) {

    files.stream()
        .filter(file -> {
          boolean isMatch = requestedFile.equals(file.getMeta());
          if (!isMatch) {
            // Invalidate file as this will not be used hereafter. Otherwise existing file can be
            // corrupted.
            invalidateFile(file);
          }
          return isMatch;
        })
        .findFirst()
        .ifPresent(taskRepository::add);
  }

  private void invalidateFile(TaskFile file) {
    try {
      file.invalidate();
    } catch (MasterException e) {
      throw new RuntimeException(e);
    }
  }

  private String getString(String id) {

    return id == null ? "" : id;
  }

  private String getWorkerId(String workerId) {
    return StringUtils.isBlank(workerId) ? null : workerId;
  }

  private List<UUID> getTaskIDs(TaskStatus status) throws MasterException {
    List<UUID> retrievingTasks = new ArrayList<>();
    try {
      File workspace = new File(this.workspace);
      for (File taskFolder : workspace.listFiles()) {
        // Validate folder is corresponding to a valid task ID.
        try {
          UUID.fromString(taskFolder.getName());
        } catch (IllegalArgumentException e) {
          // Skip the folder.
          continue;
        }

        // Load properties file.
        Properties properties = new Properties();
        properties.load(
            new FileInputStream(taskFolder.getAbsolutePath() + File.separator + "meta.properties"));

        // Get status.
        TaskStatus taskStatus = TaskStatus.valueOf((String) properties.get("META.status"));

        if (taskStatus.equals(status)) {
          retrievingTasks.add(UUID.fromString((String) properties.get("META.task-id")));
        }
      }
    } catch (IOException e) {
      throw new MasterException(ErrorCodes.INTERNAL_SERVER_ERROR, "Failed to get data", e);
    }
    return retrievingTasks;
  }

  private Optional<TaskFile> findTaskFile(List<TaskFile> files, List<TaskFile> taskFiles,
      TaskFileMetadata taskFileMetadata) {

    return files.stream().filter(f -> f.getMeta().equals(taskFileMetadata)).findFirst();
  }

  private List<TaskFile> getFiles(UUID taskId, File inputFilesFolder) throws IOException {

    List<TaskFile> inputFiles = new ArrayList<>();
    Files.walk(inputFilesFolder.toPath())
        .filter(Files::isRegularFile)
        .forEach(f -> {
          TaskFileMetadata meta = new TaskFileMetadata(
              f.getFileName().toString().split("\\.")[0],
              f.getFileName().toString().split("\\.")[1],
              taskId
          );
          try {
            TaskFile taskFile = new StreamingTaskFile(
                meta,
                new FileInputStream(f.toAbsolutePath().toString())
            );
            inputFiles.add(taskFile);
          } catch (FileNotFoundException e) {
            // ignore
          }
        });
    return inputFiles;
  }

  private void storeFiles(List<TaskFile> taskFiles, File inputFilesFolder) throws IOException {

    for (TaskFile taskFile : taskFiles) {
      TaskFileMetadata meta = taskFile.getMeta();

      Vector<InputStream> inputStreams = new Vector<>();
      while (taskFile.hasNext()) {
        inputStreams.add(taskFile.next());
      }
      SequenceInputStream combinedStream = new SequenceInputStream(inputStreams.elements());

      FileUtils.copyInputStreamToFile(combinedStream, new File(inputFilesFolder
          + File.separator + meta.getFileName() + "." + meta.getFileType()));
    }
  }

  private File forceCreate(String path, Boolean isWorkspacePrefixed)
      throws IOException {

    File file = getFile(path, isWorkspacePrefixed);
    forceCreateFile(file);
    return file;
  }

  private void forceCreateFile(File file) throws IOException {
    if (file.exists()) {
      FileUtils.deleteDirectory(file);
    }
    FileUtils.forceMkdir(file);
  }

  private File getFile(String path, Boolean isWorkspacePrefixed) {
    if (isWorkspacePrefixed) {
      return new File(workspace + path);
    }
    return new File(path);
  }
}
