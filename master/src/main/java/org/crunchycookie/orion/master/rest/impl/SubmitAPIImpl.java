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

package org.crunchycookie.orion.master.rest.impl;

import java.util.List;
import java.util.UUID;
import org.crunchycookie.orion.master.rest.api.SubmitApiDelegate;
import org.crunchycookie.orion.master.rest.model.SubmittedTask;
import org.crunchycookie.orion.master.rest.model.SubmittedTaskStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SubmitAPIImpl implements SubmitApiDelegate {

  @Override
  public ResponseEntity<SubmittedTask> submitTask(String executableShellScript,
      List<MultipartFile> filename) {
    SubmittedTask submittedTask = new SubmittedTask();
    return ResponseEntity
        .ok(submittedTask.taskId(UUID.randomUUID()).status(SubmittedTaskStatus.INPROGRESS));
  }
}
