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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.crunchycookie.orion.worker.WorkerOuterClass.FileMetaData;
import org.crunchycookie.orion.worker.exception.WorkerRuntimeException;
import org.crunchycookie.orion.worker.store.WorkerStore;
import org.crunchycookie.orion.worker.store.constants.StoreConstants.OperationStatus;

/**
 * This {@link WorkerStore} stores files on the nodes's primary storage by creating a folder in the
 * node's temporary files location.
 */
public class PrimaryStorageWorkerStore implements WorkerStore {

  private static final String TASKS_FOLDER = "tasks";

  private static volatile PrimaryStorageWorkerStore store = null;
  private static Monitor monitor = new Monitor();

  private PrimaryStorageWorkerStore() {
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
      createFolder(storeWorkspaceFolder, TASKS_FOLDER);

      // Create the folder corresponding to the task id.
      String tasksFolderPath = storeWorkspaceFolder + File.pathSeparator + TASKS_FOLDER;
      createFolder(tasksFolderPath, metaData.getTaskId());

      // Delete file if exists.
      File file = new File(tasksFolderPath + File.pathSeparator + metaData.getName() + "."
          + metaData.getType());
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

  private void createFolder(String parent, String folder) throws IOException {
    String tasksFolderPath = parent + File.separator + folder;
    File tasksFolder = new File(tasksFolderPath);
    if (!tasksFolder.exists()) {
      FileUtils.forceMkdir(tasksFolder);
    }
  }

  @Override
  public Enum<OperationStatus> remove(FileMetaData metaData) {
    return null;
  }

  @Override
  public String getName() {
    return "PrimaryStorageWorkerStore";
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
}
