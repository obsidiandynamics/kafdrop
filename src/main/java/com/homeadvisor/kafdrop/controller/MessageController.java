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

import com.fasterxml.jackson.annotation.*;
import com.homeadvisor.kafdrop.config.*;
import com.homeadvisor.kafdrop.model.*;
import com.homeadvisor.kafdrop.service.*;
import com.homeadvisor.kafdrop.util.*;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.validation.*;
import org.springframework.web.bind.annotation.*;

import javax.validation.*;
import javax.validation.constraints.*;
import java.util.*;

@Controller
public class MessageController {
  @Autowired
  private KafkaMonitor kafkaMonitor;

  @Autowired
  private MessageInspector messageInspector;

  @Autowired
  private MessageFormatConfiguration.MessageFormatProperties messageFormatProperties;

  @Autowired
  private SchemaRegistryConfiguration.SchemaRegistryProperties schemaRegistryProperties;

  /**
   * Human friendly view of reading messages.
   * @param topicName Name of topic
   * @param messageForm Message form for submitting requests to view messages.
   * @param errors
   * @param model
   * @return View for seeing messages in a partition.
   */
  @RequestMapping(method = RequestMethod.GET, value = "/topic/{name:.+}/messages")
  public String viewMessageForm(@PathVariable("name") String topicName,
                                @Valid @ModelAttribute("messageForm") PartitionOffsetInfo messageForm,
                                BindingResult errors,
                                Model model) {
    final MessageFormat defaultFormat = messageFormatProperties.getFormat();

    if (messageForm.isEmpty()) {
      final PartitionOffsetInfo defaultForm = new PartitionOffsetInfo();

      defaultForm.setCount(100l);
      defaultForm.setOffset(0l);
      defaultForm.setPartition(0);
      defaultForm.setFormat(defaultFormat);

      model.addAttribute("messageForm", defaultForm);
    }

    final TopicVO topic = kafkaMonitor.getTopic(topicName)
        .orElseThrow(() -> new TopicNotFoundException(topicName));
    model.addAttribute("topic", topic);

    model.addAttribute("defaultFormat", defaultFormat);
    model.addAttribute("messageFormats", MessageFormat.values());

    if (!messageForm.isEmpty() && !errors.hasErrors()) {
      final MessageDeserializer deserializer = getDeserializer(
          topicName, messageForm.getFormat());

      model.addAttribute("messages",
                         messageInspector.getMessages(topicName,
                                                      messageForm.getPartition(),
                                                      messageForm.getOffset(),
                                                      messageForm.getCount()));

    }

    return "message-inspector";
  }

  /**
   * Return a JSON list of all partition offset info for the given topic. If specific partition
   * and offset parameters are given, then this returns actual kafka messages from that partition
   * (if the offsets are valid; if invalid offsets are passed then the message list is empty).
   * @param topicName Name of topic.
   * @return Offset or message data.
   */
  @ApiOperation(value = "getPartitionOrMessages", notes = "Get offset or message data for a topic. Without query params returns all partitions with offset data. With query params, returns actual messages (if valid offsets are provided).")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Success", response = List.class),
      @ApiResponse(code = 404, message = "Invalid topic name")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/topic/{name:.+}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody
  List<Object> getPartitionOrMessages(
      @PathVariable("name") String topicName,
      @RequestParam(name = "partition", required = false) Integer partition,
      @RequestParam(name = "offset", required = false) Long offset,
      @RequestParam(name = "count", required = false) Long count
  ) {
    if (partition == null || offset == null || count == null) {
      final TopicVO topic = kafkaMonitor.getTopic(topicName)
          .orElseThrow(() -> new TopicNotFoundException(topicName));

      List<Object> partitionList = new ArrayList<>();
      topic.getPartitions().forEach(vo -> partitionList.add(new PartitionOffsetInfo(vo.getId(), vo.getFirstOffset(), vo.getSize())));

      return partitionList;
    } else {
      List<Object> messages = new ArrayList<>();
      List<MessageVO> vos = messageInspector.getMessages(
          topicName,
          partition,
          offset,
          count);

      if (vos != null) {
        messages.addAll(vos);
      }

      return messages;
    }
  }

  private MessageDeserializer getDeserializer(String topicName, MessageFormat format) {
    final MessageDeserializer deserializer;

    if (format == MessageFormat.AVRO) {
      final String schemaRegistryUrl = schemaRegistryProperties.getConnect();
      deserializer = new AvroMessageDeserializer(topicName, schemaRegistryUrl);
    } else {
      deserializer = new DefaultMessageDeserializer();
    }

    return deserializer;
  }

  /**
   * Encapsulates offset data for a single partition.
   */
  public static class PartitionOffsetInfo {
    @NotNull
    @Min(0)
    private Integer partition;

    /**
     * Need to clean this up. We're re-using this form for the JSON message API
     * and it's a bit confusing to have the Java variable and JSON field named
     * differently.
     */
    @NotNull
    @Min(0)
    @JsonProperty("firstOffset")
    private Long offset;

    /**
     * Need to clean this up. We're re-using this form for the JSON message API
     * and it's a bit confusing to have the Java variable and JSON field named
     * differently.
     */
    @NotNull
    @Min(1)
    @Max(100)
    @JsonProperty("lastOffset")
    private Long count;

    private MessageFormat format;

    public PartitionOffsetInfo(int partition, long offset, long count, MessageFormat format) {
      this.partition = partition;
      this.offset = offset;
      this.count = count;
      this.format = format;
    }

    public PartitionOffsetInfo(int partition, long offset, long count) {
      this(partition, offset, count, MessageFormat.DEFAULT);
    }

    public PartitionOffsetInfo() {

    }

    @JsonIgnore
    public boolean isEmpty() {
      return partition == null && offset == null && (count == null || count == 1);
    }

    public Integer getPartition() {
      return partition;
    }

    public void setPartition(Integer partition) {
      this.partition = partition;
    }

    public Long getOffset() {
      return offset;
    }

    public void setOffset(Long offset) {
      this.offset = offset;
    }

    public Long getCount() {
      return count;
    }

    public void setCount(Long count) {
      this.count = count;
    }

    public MessageFormat getFormat() {
      return format;
    }

    public void setFormat(MessageFormat format) {
      this.format = format;
    }
  }
}
