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

package kafdrop.controller;

import io.swagger.annotations.*;
import kafdrop.model.*;
import kafdrop.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Handles requests for the topic page.
 */
@Controller
@RequestMapping("/topic")
public final class TopicController {
  private static final Logger LOG = LoggerFactory.getLogger(TopicController.class);
  private final KafkaMonitor kafkaMonitor;

  public TopicController(KafkaMonitor kafkaMonitor) {
    this.kafkaMonitor = kafkaMonitor;
  }

  @RequestMapping("/{name:.+}")
  public String topicDetails(@PathVariable("name") String topicName, Model model) {
    final var topic = kafkaMonitor.getTopic(topicName)
        .orElseThrow(() -> new TopicNotFoundException(topicName));
    model.addAttribute("topic", topic);
    model.addAttribute("consumers", kafkaMonitor.getConsumers(Collections.singleton(topic)));
    model.addAttribute("topicDeleteEnabled", kafkaMonitor.isTopicDeleteEnabled());

    return "topic-detail";
  }

  @RequestMapping(value = "/{name:.+}/delete", method = RequestMethod.POST)
  public String deleteTopic(@PathVariable("name") String topicName, Model model) {
    if (!kafkaMonitor.isTopicDeleteEnabled()) {
      model.addAttribute("deleteErrorMessage", "Not configured to be deleted.");
      return topicDetails(topicName, model);
    }

    try {
      kafkaMonitor.deleteTopic(topicName);
      return "redirect:/";
    } catch (Exception ex) {
      model.addAttribute("deleteErrorMessage", ex.getMessage());
      return topicDetails(topicName, model);
    }
  }

  /**
   * Topic create page
   * @param model
   * @return creation page
   */
  @RequestMapping("/create")
  public String createTopicPage(Model model) {
    model.addAttribute("topicCreateEnabled", kafkaMonitor.isTopicCreateEnabled());
    model.addAttribute("brokersCount", kafkaMonitor.getBrokers().size());
    return "topic-create";
  }

  @ApiOperation(value = "getTopic", notes = "Get details for a topic")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Success", response = TopicVO.class),
      @ApiResponse(code = 404, message = "Invalid topic name")
  })
  @RequestMapping(path = "/{name:.+}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
  public @ResponseBody TopicVO getTopic(@PathVariable("name") String topicName) {
    return kafkaMonitor.getTopic(topicName)
        .orElseThrow(() -> new TopicNotFoundException(topicName));
  }

  @ApiOperation(value = "getAllTopics", notes = "Get list of all topics")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Success", response = String.class, responseContainer = "List")
  })
  @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
  public @ResponseBody List<TopicVO> getAllTopics() {
    return kafkaMonitor.getTopics();
  }

  @ApiOperation(value = "getConsumers", notes = "Get consumers for a topic")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Success", response = String.class, responseContainer = "List"),
      @ApiResponse(code = 404, message = "Invalid topic name")
  })
  @RequestMapping(path = "/{name:.+}/consumers", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
  public @ResponseBody List<ConsumerVO> getConsumers(@PathVariable("name") String topicName) {
    final var topic = kafkaMonitor.getTopic(topicName)
        .orElseThrow(() -> new TopicNotFoundException(topicName));
    return kafkaMonitor.getConsumers(Collections.singleton(topic));
  }

  /**
   * API for topic creation
   * @param createTopicVO request
   */
  @ApiOperation(value = "createTopic", notes = "Create topic")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Success", response = String.class)
  })
  @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
  public String createTopic(CreateTopicVO createTopicVO, Model model) {
    model.addAttribute("topicCreateEnabled", kafkaMonitor.isTopicCreateEnabled());
    model.addAttribute("topicName", createTopicVO.getName());
    if (!kafkaMonitor.isTopicCreateEnabled()) {
      model.addAttribute("errorMessage", "Not configured to be created.");
      return createTopicPage(model);
    }
    try {
        kafkaMonitor.createTopic(createTopicVO);
    } catch (Exception ex) {
      model.addAttribute("errorMessage", ex.getMessage());
    }
    model.addAttribute("brokersCount", kafkaMonitor.getBrokers().size());
    return "topic-create";
  }
}
