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

package org.crunchycookie.orion.master;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.crunchycookie.orion.master.rest.impl.TaskLimitsAPIImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class RESTfulEndpointTest {

  private static final String URL_PREFIX = "http://localhost:";
  private static final String URL_PATH_PREFIX = "/orion/v0.1/";
  private static final String URL_PATH_API_TASK_LIMITS = URL_PATH_PREFIX + "task-limits";

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private TaskLimitsAPIImpl taskLimitsAPIImpl;

  @BeforeAll
  public static void setup() {
    RESTfulEndpoint.initConfigs("orion-master.properties");
  }

  @Test
  public void testTaskLimits() throws Exception {

    // Call API and obtain response.
    String taskLimitsAPI = URL_PREFIX + port + URL_PATH_API_TASK_LIMITS;
    String responseBody = this.restTemplate.getForObject(
        taskLimitsAPI,
        String.class
    );

    // Assert response.
    JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
    responseJson.getAsJsonArray("limits").iterator().forEachRemaining(jsonElement ->
        Assertions.assertTrue(List.of("MEMORY", "STORAGE")
            .contains(jsonElement.getAsJsonObject().get("key").getAsString())
        )
    );
  }
}
