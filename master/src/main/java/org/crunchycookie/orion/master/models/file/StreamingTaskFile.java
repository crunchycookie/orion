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

package org.crunchycookie.orion.master.models.file;

import java.io.InputStream;
import java.util.PriorityQueue;
import org.crunchycookie.orion.master.models.TaskFileMetadata;

public class StreamingTaskFile implements TaskFile {

  private final PriorityQueue<InputStream> queue = new PriorityQueue<>();
  TaskFileMetadata meta;

  public StreamingTaskFile(TaskFileMetadata meta, InputStream fileStream) {
    this.meta = meta;
    queue.add(fileStream);
  }

  @Override
  public TaskFileMetadata getMeta() {
    return meta;
  }

  @Override
  public InputStream next() {
    return queue.remove();
  }

  @Override
  public boolean hasNext() {
    return false;
  }
}
