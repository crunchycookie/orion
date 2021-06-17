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
import org.crunchycookie.orion.master.RESTfulEndpoint;
import org.crunchycookie.orion.master.constants.MasterConstants.ErrorCodes;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.master.models.SubmittedTask;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus;
import org.crunchycookie.orion.master.models.SubmittedTaskStatus.TaskStatus;
import org.crunchycookie.orion.master.models.file.TaskFileMetadata;
import org.crunchycookie.orion.master.models.file.StreamingTaskFile;
import org.crunchycookie.orion.master.models.file.TaskFile;
import org.crunchycookie.orion.master.service.store.CentralStore;
import org.crunchycookie.orion.master.utils.RESTUtils.ResourceParams;

public class LocalStorageCentralStore implements CentralStore {

  private final String workspace;

  public LocalStorageCentralStore() {

    String workspaceFromConfigs = RESTfulEndpoint.configs
        .getConfig("LocalStorageCentralStore.workspace");
    workspace = workspaceFromConfigs.endsWith(File.separator) ? workspaceFromConfigs
        : workspaceFromConfigs + File.separator;
  }

  @Override
  public void store(List<SubmittedTask> submittedTasks) throws MasterException {

    for (SubmittedTask task : submittedTasks) {
      store(task);
    }
  }

  @Override
  public void store(SubmittedTask submittedTask) throws MasterException {

    try {
      // Create task folder.
      File taskFolderPath = forceCreate(submittedTask.getTaskId().toString());

      // Create input files folder.
      File inputFilesFolder = forceCreate(
          taskFolderPath.getAbsolutePath() + File.separator + "inputs");
      storeFiles(submittedTask.getTaskFiles(), inputFilesFolder);

      // Create output files folder.
      File outputsFilesFolder = forceCreate(
          taskFolderPath.getAbsolutePath() + File.separator + "outputs");
      storeFiles(submittedTask.getOutputFiles(), outputsFilesFolder);

      // Persist metadata.
      Properties properties = new Properties();

      // Persist meta.
      properties.put("META.task-id", submittedTask.getTaskId().toString());
      properties.put("META.worker-id", submittedTask.getWorkerId().toString());
      properties.put("META.executable-file", submittedTask.getExecutable().toString());
      properties.put("META.status", submittedTask.getStatus().getStatus().toString());

      // Persist resource limits.
      properties.put("RESOURCE.LIMITS.MEMORY", submittedTask.getResourceRequirement(ResourceParams.MEMORY));
      properties.put("RESOURCE.LIMITS.STORAGE", submittedTask.getResourceRequirement(ResourceParams.STORAGE));

      // Persist meta file.
      properties
          .store(new FileOutputStream(taskFolderPath + File.separator + "meta.properties"), null);
    } catch (IOException e) {
      // TODO: 2021-06-17 Add error code
      throw new MasterException("Failed to create file IOs", e);
    }
  }

  @Override
  public SubmittedTask get(UUID taskId) throws MasterException {

    try {
      // Get task folder.
      File taskFolder = getFile(taskId.toString());

      // Get input files.
      File inputFilesFolder = getFile(taskFolder + File.separator + "inputs");
      List<TaskFile> inputs = getInputFiles(taskId, inputFilesFolder);

      // Get output files.
      File outputFilesFolder = getFile(taskFolder + File.separator + "outputs");
      List<TaskFile> outputs = getInputFiles(taskId, outputFilesFolder);

      // Get meta.
      Properties properties = new Properties();
      properties.load(new FileInputStream(taskFolder + File.separator + "meta.properties"));

      // Get worker id.
      UUID worker = UUID.fromString((String) properties.get("META.worker-id"));

      // Get status.
      TaskStatus status = TaskStatus.valueOf((String) properties.get("META.status"));

      // Get executable.
      String executableFile = (String) properties.get("META.executable-file");
      TaskFileMetadata executable = new TaskFileMetadata(
          executableFile.split("-")[1],
          executableFile.split("-")[2],
          UUID.fromString(executableFile.split("-")[0])
      );

      // Get resource limits.
      Map<ResourceParams, String> resourceLimits = new HashMap<>();
      resourceLimits.put(ResourceParams.MEMORY, (String) properties.get("RESOURCE.LIMITS.MEMORY"));
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
  public List<TaskFile> getFiles(UUID taskId, List<TaskFileMetadata> files)
      throws MasterException {

    SubmittedTask submittedTask = get(taskId);
    List<TaskFile> taskFiles = new ArrayList<>();
    for (TaskFileMetadata taskFileMetadata : files) {
      findTaskFile(submittedTask.getTaskFiles(), taskFiles, taskFileMetadata).ifPresentOrElse(
          taskFiles::add,
          () -> findTaskFile(submittedTask.getOutputFiles(), taskFiles, taskFileMetadata)
      );
    }
    return taskFiles;
  }

  @Override
  public void remove(UUID taskId) throws MasterException {

    File taskFolder = getFile(taskId.toString());
    taskFolder.delete();
  }

  @Override
  public SubmittedTaskStatus getStatus(UUID taskId) throws MasterException {

    return get(taskId).getStatus();
  }

  @Override
  public void setStatus(UUID taskId, SubmittedTaskStatus status) throws MasterException {

    SubmittedTask submittedTask = get(taskId);
    submittedTask.setStatus(status);

    store(submittedTask);
  }

  private List<UUID> getTaskIDs(TaskStatus status) throws MasterException {
    List<UUID> retrievingTasks = new ArrayList<>();
    try {
      File workspace = new File(this.workspace);
      for (File taskFolder : workspace.listFiles()) {
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

  private List<TaskFile> getInputFiles(UUID taskId, File inputFilesFolder) throws IOException {

    List<TaskFile> inputFiles = new ArrayList<>();
    Files.walk(inputFilesFolder.toPath())
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

      Vector<InputStream> inputStreams = new Vector<InputStream>();
      while (taskFile.hasNext()) {
        inputStreams.add(taskFile.next());
      }
      SequenceInputStream combinedStream = new SequenceInputStream(inputStreams.elements());

      FileUtils.copyInputStreamToFile(combinedStream, new File(inputFilesFolder
          + File.separator + meta.getFileName() + "." + meta.getFileType()));
    }
  }

  private File forceCreate(String path) throws IOException {

    File file = getFile(path);
    if (file.exists()) {
      file.delete();
    }
    FileUtils.forceMkdir(file);
    return file;
  }

  private File getFile(String path) {
    return new File(workspace + path);
  }
}
