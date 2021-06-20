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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import org.crunchycookie.orion.master.exception.MasterException;
import org.crunchycookie.orion.worker.WorkerOuterClass.Result;

public class IteratingTaskFile implements TaskFile {

  private final Iterator<Result> iterator;
  TaskFileMetadata meta;

  public IteratingTaskFile(TaskFileMetadata meta, Iterator<Result> iterator) {
    this.meta = meta;
    this.iterator = iterator;
  }

  @Override
  public TaskFileMetadata getMeta() {
    return meta;
  }

  @Override
  public InputStream next() {
    if (iterator == null) {
      return null;
    }
    return new ByteArrayInputStream(iterator.next().getOutputFile().getContent().toByteArray());
  }

  @Override
  public boolean hasNext() {
    return iterator != null && iterator.hasNext();
  }

  @Override
  public void invalidate() throws MasterException {

    while (iterator.hasNext()) {
      iterator.remove();
    }
  }
}
