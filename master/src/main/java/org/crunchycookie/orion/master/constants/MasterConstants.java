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

package org.crunchycookie.orion.master.constants;

public class MasterConstants {

  public static String ERROR_CODE_PREFIX = "OME";
  public static String ERROR_CODE_SEPARATOR = "-";

  /**
   * Format of the code = OME(ORION Master Error) + number
   */
  public enum ErrorCodes {

    INTERNAL_SERVER_ERROR("00000", "Unexpected error occurred"),
    ERROR_FILE_DOWNLOAD_STILL_IN_PROGRESS("00001", "Task is still in-progress"),
    ERROR_FILE_DOWNLOAD_FAILED("00002", "Task is still in-progress"),
    ERROR_COM_WORKER_METHOD_INVOCATION_FAILED("00003", "Communication with a worker node failed");

    private String code;
    private String message;

    ErrorCodes(String code, String message) {
      this.code = ERROR_CODE_PREFIX + ERROR_CODE_SEPARATOR + code;
      this.message = message;
    }

    public String getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }
  }

  public enum ComponentID {

    COMPONENT_ID_REST_ENDPOINT("REST_ENDPOINT"),
    COMPONENT_ID_TASK_MANAGER("TASK_MANAGER"),
    COMPONENT_ID_TASK_PRIORATIZER("TASK_PRIORATIZER"),
    COMPONENT_ID_TASK_SCHEDULER("TASK_SCHEDULER"),
    COMPONENT_ID_TASK_VALIDATOR("TASK_VALIDATOR"),
    COMPONENT_ID_TASK_DISTRIBUTOR("TASK_DISTRIBUTOR"),
    COMPONENT_ID_CENTRAL_STORE("CENTRAL_STORE"),
    COMPONENT_ID_WORKER_POOL_MANAGER("WORKER_POOL_MANAGER"),
    COMPONENT_ID_WORKER_NODE("WORKER_NODE");

    private String name;

    ComponentID(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class Logging {

    public static String LOGGING_FORMAT = "Component: %s | Task: %s | Msg: %s";
    public static String TASK_ID_NOT_APPLICABLE = "TASK_ID_NOT_APPLICABLE";
  }
}
