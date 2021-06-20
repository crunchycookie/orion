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
import org.crunchycookie.orion.master.exception.MasterException;

public interface TaskFile {

  /**
   * Get metadata.
   *
   * @return
   */
  TaskFileMetadata getMeta();

  /**
   * Get the next byte chunk. Once read, this stream becomes unavailable.
   *
   * @return
   */
  InputStream next();

  /**
   * Check for next byte availability.
   *
   * @return
   */
  boolean hasNext();

  /**
   * Make the task file invalidated. This will close any opened file streams etc. Once invoked, this
   * object cannot be used to read the actual file, but metadata will be kept.
   *
   * @return
   */
  void invalidate() throws MasterException;
}
