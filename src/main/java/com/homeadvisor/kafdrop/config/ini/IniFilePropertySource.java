package com.homeadvisor.kafdrop.config.ini;

import com.google.common.collect.Maps;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class IniFilePropertySource extends MapPropertySource
{
   public IniFilePropertySource(String name, IniFileProperties source, String[] activeProfiles)
   {
      super(name, loadPropertiesForIniFile(source, activeProfiles));
   }

   private static Map<String, Object> loadPropertiesForIniFile(IniFileProperties iniProperties,
                                                               String[] activeProfiles)
   {
      final Map<String, Object> properties = Maps.newLinkedHashMap();
      properties.putAll(iniProperties.getDefaultProperties());

      if (activeProfiles != null && activeProfiles.length > 0)
      {
         for (String profile : activeProfiles)
         {
            final Map<String, String> sectionProperties = iniProperties.getSectionProperties(profile);
            if (sectionProperties != null)
            {
               properties.putAll(sectionProperties);
            }
         }
      }
      return properties;
   }
}
