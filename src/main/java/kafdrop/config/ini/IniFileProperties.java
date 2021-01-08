/*
 * Copyright 2017 Kafdrop contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package kafdrop.config.ini;

import com.google.common.collect.*;

import java.util.*;

public final class IniFileProperties {
  private Map<String, Map<String, String>> sectionProperties = Maps.newLinkedHashMap();
  private Map<String, String> defaultProperties = Maps.newLinkedHashMap();

  public Map<String, String> getDefaultProperties() {
    return defaultProperties;
  }

  public Collection<String> getSectionNames() {
    return Collections.unmodifiableCollection(sectionProperties.keySet());
  }

  public Map<String, String> getSectionProperties(String section) {
    final var sectionProperties = doGetSectionProperties(section);
    return sectionProperties != null ? sectionProperties : Collections.unmodifiableMap(sectionProperties);
  }

  public void addSectionProperty(String section, String name, String value) {
    var properties = doGetSectionProperties(section);
    if (properties == null) {
      properties = Maps.newLinkedHashMap();
      sectionProperties.put(section, properties);
    }

    properties.put(name, value);
  }

  private Map<String, String> doGetSectionProperties(String section) {
    if (section == null) {
      return getDefaultProperties();
    } else {
      return sectionProperties.get(section);
    }
  }
}
