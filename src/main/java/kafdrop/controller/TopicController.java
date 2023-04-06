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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kafdrop.config.MessageFormatConfiguration;
import kafdrop.model.ConsumerVO;
import kafdrop.model.CreateTopicVO;
import kafdrop.model.TopicVO;
import kafdrop.service.KafkaMonitor;
import kafdrop.service.TopicNotFoundException;
import kafdrop.util.MessageFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.List;

/**
 * Handles requests for the topic page.
 */
@Tag(name = "topic-controller", description = "Topic Controller")
@Controller
@RequestMapping("/topic")
public final class TopicController {
  private final KafkaMonitor kafkaMonitor;
  private final boolean topicDeleteEnabled;
  private final boolean topicCreateEnabled;
  private final MessageFormatConfiguration.MessageFormatProperties messageFormatProperties;

  public TopicController(KafkaMonitor kafkaMonitor,
                         @Value("${topic.deleteEnabled:true}") Boolean topicDeleteEnabled,
                         @Value("${topic.createEnabled:true}") Boolean topicCreateEnabled,
                         MessageFormatConfiguration.MessageFormatProperties messageFormatProperties) {
    this.kafkaMonitor = kafkaMonitor;
    this.topicDeleteEnabled = topicDeleteEnabled;
    this.topicCreateEnabled = topicCreateEnabled;
    this.messageFormatProperties = messageFormatProperties;
  }

  @RequestMapping("/{name:.+}")
  public String topicDetails(@PathVariable("name") String topicName, Model model) {
    final MessageFormat defaultFormat = messageFormatProperties.getFormat();
    final MessageFormat defaultKeyFormat = messageFormatProperties.getKeyFormat();

    final var topic = kafkaMonitor.getTopic(topicName)
      .orElseThrow(() -> new TopicNotFoundException(topicName));
    model.addAttribute("topic", topic);
    model.addAttribute("consumers", kafkaMonitor.getConsumersByTopics(Collections.singleton(topic)));
    model.addAttribute("topicDeleteEnabled", topicDeleteEnabled);
    model.addAttribute("keyFormat", defaultKeyFormat);
    model.addAttribute("format", defaultFormat);

    return "topic-detail";
  }

  @PostMapping(value = "/{name:.+}/delete")
  public String deleteTopic(@PathVariable("name") String topicName, Model model) {
    if (!topicDeleteEnabled) {
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
   *
   * @param model
   * @return creation page
   */
  @RequestMapping("/create")
  public String createTopicPage(Model model) {
    model.addAttribute("topicCreateEnabled", topicCreateEnabled);
    model.addAttribute("brokersCount", kafkaMonitor.getBrokers().size());
    return "topic-create";
  }

  @Operation(summary = "getTopic", description = "Get details for a topic")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success"),
    @ApiResponse(responseCode = "404", description = "Invalid topic name")
  })
  @GetMapping(path = "/{name:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody TopicVO getTopic(@PathVariable("name") String topicName) {
    return kafkaMonitor.getTopic(topicName)
      .orElseThrow(() -> new TopicNotFoundException(topicName));
  }

  @Operation(summary = "getAllTopics", description = "Get list of all topics")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Success")})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody List<TopicVO> getAllTopics() {
    return kafkaMonitor.getTopics();
  }

  @Operation(summary = "getConsumers", description = "Get consumers for a topic")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success"),
    @ApiResponse(responseCode = "404", description = "Invalid topic name")
  })
  @GetMapping(path = "/{name:.+}/consumers", produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody List<ConsumerVO> getConsumers(@PathVariable("name") String topicName) {
    final var topic = kafkaMonitor.getTopic(topicName)
      .orElseThrow(() -> new TopicNotFoundException(topicName));
    return kafkaMonitor.getConsumersByTopics(Collections.singleton(topic));
  }

  /**
   * API for topic creation
   *
   * @param createTopicVO request
   */
  @Operation(summary = "createTopic", description = "Create topic")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Success")})
  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public String createTopic(CreateTopicVO createTopicVO, Model model) {
    model.addAttribute("topicCreateEnabled", topicCreateEnabled);
    model.addAttribute("topicName", createTopicVO.getName());
    if (!topicCreateEnabled) {
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
