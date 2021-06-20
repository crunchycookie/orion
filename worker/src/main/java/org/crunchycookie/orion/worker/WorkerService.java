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

package org.crunchycookie.orion.worker;

import java.io.IOException;

public class WorkerService {

  public static void main(String[] args) {

    logMessage("Starting the worker service...");
    try {
      GRPCEndpoint endpoint = new GRPCEndpoint(Integer.parseInt(args[0]));
      endpoint.start();
      endpoint.blockUntilShutdown();
    } catch (IOException e) {
      logMessage("Failed to start the server");
      System.out.println("Failed to start the server");
    } catch (InterruptedException e) {
      logMessage("Error occurred while server timeout");
      System.out.println("Error occurred while waiting till service termination");
    } finally {
      logMessage("Stopping the worker service...");
    }
  }

  public static void logMessage(String msg) {
    System.out.println(msg);
  }
}
