/*
 * Copyright 2016 HomeAdvisor, Inc.
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

import com.homeadvisor.kafdrop.service.ConsumerNotFoundException;
import com.homeadvisor.kafdrop.service.KafkaMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/consumer")
public class ConsumerController
{
   @Autowired
   private KafkaMonitor kafkaMonitor;

   @RequestMapping("/{groupId:.+}")
   public String consumerDetail(@PathVariable("groupId") String groupId, Model model)
   {
      model.addAttribute("consumer", kafkaMonitor.getConsumer(groupId)
         .orElseThrow(() -> new ConsumerNotFoundException(groupId)));
      return "consumer-detail";
   }

}
