/*
 * Copyright 2017 HomeAdvisor, Inc.
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

package com.homeadvisor.kafdrop.config.ini;

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class IniFileProperties
{
   private Map<String, Map<String, String>> sectionProperties = Maps.newLinkedHashMap();
   private Map<String, String> defaultProperties = Maps.newLinkedHashMap();

   public Map<String, String> getDefaultProperties()
   {
      return defaultProperties;
   }

   public Collection<String> getSectionNames()
   {
      return Collections.unmodifiableCollection(sectionProperties.keySet());
   }

   public Map<String, String> getSectionProperties(String section)
   {
      Map<String, String> sectionProperties = doGetSectionProperties(section);
      if (sectionProperties != null)
      {
         sectionProperties = Collections.unmodifiableMap(sectionProperties);
      }
      return sectionProperties;
   }

   public void addSectionProperty(String section, String name, String value)
   {
      Map<String, String> properties = doGetSectionProperties(section);
      if (section != null && properties == null)
      {
         properties = Maps.newLinkedHashMap();
         sectionProperties.put(section, properties);
      }

      properties.put(name, value);
   }

   private Map<String, String> doGetSectionProperties(String section)
   {
      Map<String, String> properties;
      if (section == null)
      {
         properties = getDefaultProperties();
      }
      else
      {
         properties = sectionProperties.get(section);
      }

      return properties;
   }
}
