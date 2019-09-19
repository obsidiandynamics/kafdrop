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
import org.springframework.core.env.*;

import java.util.*;

public class IniFilePropertySource extends MapPropertySource {
  public IniFilePropertySource(String name, IniFileProperties source, String[] activeProfiles) {
    super(name, loadPropertiesForIniFile(source, activeProfiles));
  }

  private static Map<String, Object> loadPropertiesForIniFile(IniFileProperties iniProperties,
                                                              String[] activeProfiles) {
    final Map<String, Object> properties = Maps.newLinkedHashMap();
    properties.putAll(iniProperties.getDefaultProperties());

    if (activeProfiles != null && activeProfiles.length > 0) {
      for (String profile : activeProfiles) {
        final Map<String, String> sectionProperties = iniProperties.getSectionProperties(profile);
        if (sectionProperties != null) {
          properties.putAll(sectionProperties);
        }
      }
    }
    return properties;
  }
}
