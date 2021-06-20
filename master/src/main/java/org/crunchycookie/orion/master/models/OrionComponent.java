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

package org.crunchycookie.orion.master.models;

import java.util.HashMap;
import java.util.Map;
import org.crunchycookie.orion.master.constants.MasterConstants.ComponentID;

/**
 * Each major orion component should extend this class. It supports effective monitoring via
 * logging.
 */
public class OrionComponent {

  public static Map<ComponentID, ComponentState> status = new HashMap<>();
  private ComponentID id;

  public class ComponentState {

    private Map<String, String> parameters;
    private ComponentID componentID;

    public ComponentState(
        ComponentID componentID) {
      this.componentID = componentID;
    }

    public Map<String, String> getParameters() {
      return parameters;
    }

    public ComponentState setParameters(Map<String, String> parameters) {
      this.parameters = parameters;
      return this;
    }

    public ComponentID getComponentID() {
      return componentID;
    }
  }

  public static Map<ComponentID, ComponentState> getState() {
    return status;
  }

  public static ComponentState getState(ComponentID componentID) {
    return status.get(componentID);
  }

  /**
   * Publish component's stats. This is helpful for monitoring.
   */
  public void publishState(Map<String, String> parameters) {
    status.put(
        id,
        status.get(id).setParameters(parameters)
    );
  }

  /**
   * Register as an orion component
   */
  void register(ComponentID componentID) {
    this.id = componentID;
    status.put(componentID, new ComponentState(componentID));
  }
}
