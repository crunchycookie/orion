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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crunchycookie.orion.worker.service.WorkerService;

public class GRPCEndpoint {

  private static final Logger LOG = LogManager.getLogger(GRPCEndpoint.class);

  private final int port;
  private final Server server;

  public GRPCEndpoint(int port) {

    this(ServerBuilder.forPort(port), port);
  }

  public GRPCEndpoint(ServerBuilder<?> serverBuilder, int port) {

    this.port = port;
    server = serverBuilder.addService(new WorkerService()).build();
  }

  /**
   * Start serving requests.
   */
  public void start() throws IOException {
    server.start();
    LOG.info("Worker Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      // Use stderr here since the logger may have been reset by its JVM shutdown hook.
      LOG.error("*** shutting down Worker server since JVM is shutting down");
      try {
        GRPCEndpoint.this.stop();
      } catch (InterruptedException e) {
        e.printStackTrace(System.err);
      }
      LOG.error("*** Worker server shut down");
    }));
  }

  public void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }
}
