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

import com.homeadvisor.kafdrop.model.ConsumerVO;
import com.homeadvisor.kafdrop.model.TopicVO;
import com.homeadvisor.kafdrop.service.ConsumerNotFoundException;
import com.homeadvisor.kafdrop.service.KafkaMonitor;
import com.homeadvisor.kafdrop.service.TopicNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;

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

   @ApiOperation(value = "getTopic", notes = "Get partition and consumer details for a topic")
   @ApiResponses(value = {
         @ApiResponse(code = 200, message = "Success", response = TopicVO.class),
         @ApiResponse(code = 404, message = "Invalid topic name or consumer group")
   })
   @RequestMapping(path = "/{name:.+}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
   public @ResponseBody TopicVO getTopic(@PathVariable("name") String topicName) throws Exception
   {
      final TopicVO topic = kafkaMonitor.getTopic(topicName)
            .orElseThrow(() -> new TopicNotFoundException(topicName));

      return topic;
   }

   @ApiOperation(value = "getTopicAndConsumer", notes = "Get partition details for a topic and consumer group")
   @ApiResponses(value = {
         @ApiResponse(code = 200, message = "Success", response = ConsumerVO.class),
         @ApiResponse(code = 404, message = "Invalid topic name or consumer group")
   })
   @RequestMapping(path = "/{name:.+}/{groupId:.+}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
   public @ResponseBody
   ConsumerVO getTopicAndConsumer(
         @PathVariable("name") String topicName,
         @PathVariable("groupId") String groupId)
         throws Exception
   {
      final TopicVO topic = kafkaMonitor.getTopic(topicName)
            .orElseThrow(() -> new TopicNotFoundException(topicName));

      final ConsumerVO consumer = kafkaMonitor.getConsumerByTopic(groupId, topic)
            .orElseThrow(() -> new ConsumerNotFoundException(topicName));

      return consumer;
   }

   @ApiOperation(value = "getAllTopics", notes = "Get list of all topics")
   @ApiResponses(value = {
         @ApiResponse(code = 200, message = "Success", response = String.class, responseContainer = "List")
   })
   @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
   public @ResponseBody List<String> getAllTopics() throws Exception
   {
      return kafkaMonitor.getTopics().stream().map(TopicVO::getName).collect(Collectors.toList());
   }
}
