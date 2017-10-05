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

package com.homeadvisor.kafdrop.controller;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;

public abstract class BaseController
{
   @Value("${spring.profiles.active:}")
   private String springProfile = "";

   public void init(Model model)
   {
      try
      {
         if (model != null && StringUtils.isNotEmpty(springProfile))
         {
            model.addAttribute("profileTag", springProfile);
         }
      }
      catch (Exception ex)
      {
         // SILENCE ALL
      }
   }
}
