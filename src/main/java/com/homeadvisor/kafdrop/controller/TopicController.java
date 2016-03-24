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

import com.homeadvisor.kafdrop.model.TopicVO;
import com.homeadvisor.kafdrop.service.KafkaMonitor;
import com.homeadvisor.kafdrop.service.TopicNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/topic")
public class TopicController
{
   @Autowired
   private KafkaMonitor kafkaMonitor;

   @RequestMapping("/{name:.+}")
   public String topicDetails(@PathVariable("name") String topicName, Model model)
   {
      final TopicVO topic = kafkaMonitor.getTopic(topicName)
         .orElseThrow(() -> new TopicNotFoundException(topicName));
      model.addAttribute("topic", topic);
      model.addAttribute("consumers", kafkaMonitor.getConsumers(topic));

      return "topic-detail";
   }

}
