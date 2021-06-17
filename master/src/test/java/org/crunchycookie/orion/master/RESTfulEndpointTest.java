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

import java.util.List;
import java.util.stream.Collectors;
import org.crunchycookie.orion.master.config.OrionConfigs;
import org.crunchycookie.orion.master.rest.impl.TaskLimitsAPIImpl;
import org.crunchycookie.orion.master.rest.model.Property;
import org.crunchycookie.orion.master.service.worker.WorkerNode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RESTfulEndpointTest {

  private static OrionConfigs configs;

  @Mock
  WorkerNode workerNode;

  @InjectMocks
  TaskLimitsAPIImpl taskLimitsAPIImpl;

  @BeforeClass
  public static void setup() {
    RESTfulEndpoint.initConfigs("orion-master.properties");
  }

  @Test
  public void testGetTaskLimitations() {

    List<Property> limits = taskLimitsAPIImpl.getTaskLimitations().getBody().getLimits();
    Assert.assertEquals(RESTfulEndpoint.configs.getConfig("WorkerNode.capacity.MEMORY"),
        limits.stream()
            .filter(p -> p.getKey().equals("MEMORY"))
            .map(Property::getValue)
            .collect(Collectors.toList())
            .get(0));
    Assert.assertEquals(RESTfulEndpoint.configs.getConfig("WorkerNode.capacity.STORAGE"),
        limits.stream()
            .filter(p -> p.getKey().equals("STORAGE"))
            .map(Property::getValue)
            .collect(Collectors.toList())
            .get(0));
  }
}
