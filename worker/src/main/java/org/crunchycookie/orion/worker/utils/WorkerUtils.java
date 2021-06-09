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

package org.crunchycookie.orion.worker.utils;

import org.crunchycookie.orion.worker.store.WorkerStore;
import org.crunchycookie.orion.worker.store.impl.PrimaryStorageWorkerStore;

public class WorkerUtils {

  /**
   * Get the store instance. Currently, the singleton instance of {@link PrimaryStorageWorkerStore}
   * is returned.
   *
   * @return {@link WorkerStore} instance.
   */
  public static WorkerStore getStore() {
    /*
    This needs to be customizable such that a pluggable store can be injected. Currently, the
    default store is returned.
     */
    return PrimaryStorageWorkerStore.getInstance();
  }
}
