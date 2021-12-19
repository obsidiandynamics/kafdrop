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

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import kafdrop.util.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import kafdrop.config.MessageFormatConfiguration.MessageFormatProperties;
import kafdrop.config.ProtobufDescriptorConfiguration.ProtobufDescriptorProperties;
import kafdrop.config.SchemaRegistryConfiguration.SchemaRegistryProperties;
import kafdrop.model.MessageVO;
import kafdrop.model.TopicPartitionVO;
import kafdrop.model.TopicVO;
import kafdrop.service.KafkaMonitor;
import kafdrop.service.MessageInspector;
import kafdrop.service.TopicNotFoundException;

@Controller
public final class MessageController {
  private final KafkaMonitor kafkaMonitor;

  private final MessageInspector messageInspector;

  private final MessageFormatProperties messageFormatProperties;
  private final MessageFormatProperties keyFormatProperties;

  private final SchemaRegistryProperties schemaRegistryProperties;

  private final ProtobufDescriptorProperties protobufProperties;

  public MessageController(KafkaMonitor kafkaMonitor, MessageInspector messageInspector, MessageFormatProperties messageFormatProperties, MessageFormatProperties keyFormatProperties, SchemaRegistryProperties schemaRegistryProperties, ProtobufDescriptorProperties protobufProperties) {
    this.kafkaMonitor = kafkaMonitor;
    this.messageInspector = messageInspector;
    this.messageFormatProperties = messageFormatProperties;
    this.keyFormatProperties = keyFormatProperties;
    this.schemaRegistryProperties = schemaRegistryProperties;
    this.protobufProperties = protobufProperties; 
  }

  /**
   * Human friendly view of reading all topic messages sorted by timestamp.
   * @param topicName Name of topic
   * @param model
   * @return View for seeing all messages in a topic sorted by timestamp.
   */
  @GetMapping("/topic/{name:.+}/allmessages")
  public String viewAllMessages(@PathVariable("name") String topicName,
                                Model model, @RequestParam(name = "count", required = false) Integer count) {
    final int size = (count != null? count : 100);
    final MessageFormat defaultFormat = messageFormatProperties.getFormat();
    final MessageFormat defaultKeyFormat = keyFormatProperties.getFormat();
    final TopicVO topic = kafkaMonitor.getTopic(topicName)
        .orElseThrow(() -> new TopicNotFoundException(topicName));

    model.addAttribute("topic", topic);
    model.addAttribute("defaultFormat", defaultFormat);
    model.addAttribute("messageFormats", MessageFormat.values());
    model.addAttribute("keyFormats", KeyFormat.values());
    model.addAttribute("descFiles", protobufProperties.getDescFilesList());

    final var deserializers = new Deserializers(
          getDeserializer(topicName, defaultKeyFormat, "", ""),
          getDeserializer(topicName, defaultFormat, "", ""));

    final List<MessageVO> messages = messageInspector.getMessages(topicName, size, deserializers);

    for (TopicPartitionVO partition : topic.getPartitions()) {
      messages.addAll(messageInspector.getMessages(topicName,
          partition.getId(),
          partition.getFirstOffset(),
          size,
          deserializers));
    }

    messages.sort(Comparator.comparing(MessageVO::getTimestamp));
    model.addAttribute("messages", messages);

    return "topic-messages";
  }

  /**
   * Human friendly view of reading messages.
   * @param topicName Name of topic
   * @param messageForm Message form for submitting requests to view messages.
   * @param errors
   * @param model
   * @return View for seeing messages in a partition.
   */
  @GetMapping("/topic/{name:.+}/messages")
  public String viewMessageForm(@PathVariable("name") String topicName,
                                @Valid @ModelAttribute("messageForm") PartitionOffsetInfo messageForm,
                                BindingResult errors,
                                Model model) {
    final MessageFormat defaultFormat = messageFormatProperties.getFormat();
    final MessageFormat defaultKeyFormat = keyFormatProperties.getFormat();

    if (messageForm.isEmpty()) {
      final PartitionOffsetInfo defaultForm = new PartitionOffsetInfo();

      defaultForm.setCount(100l);
      defaultForm.setOffset(0l);
      defaultForm.setPartition(0);
      defaultForm.setFormat(defaultFormat);
      defaultForm.setKeyFormat(defaultFormat);

      model.addAttribute("messageForm", defaultForm);
    }

    final TopicVO topic = kafkaMonitor.getTopic(topicName)
        .orElseThrow(() -> new TopicNotFoundException(topicName));
    model.addAttribute("topic", topic);

    model.addAttribute("defaultFormat", defaultFormat);
    model.addAttribute("messageFormats", MessageFormat.values());
    model.addAttribute("defaultKeyFormat", defaultKeyFormat);
    model.addAttribute("keyFormats",KeyFormat.values());
    model.addAttribute("descFiles", protobufProperties.getDescFilesList());

    if (!messageForm.isEmpty() && !errors.hasErrors()) {

      final var deserializers = new Deserializers(
          getDeserializer(topicName, messageForm.getKeyFormat(), messageForm.getDescFile(),messageForm.getMsgTypeName()),
          getDeserializer(topicName, messageForm.getFormat(), messageForm.getDescFile(), messageForm.getMsgTypeName())
      );

      model.addAttribute("messages",
                         messageInspector.getMessages(topicName,
                                                      messageForm.getPartition(),
                                                      messageForm.getOffset(),
                                                      messageForm.getCount().intValue(),
                                                      deserializers));

    }

    return "message-inspector";
  }


  /**
   * Returns the selected nessagr format based on the
   * form submission
   * @param format String representation of format name
   * @return
   */
  private MessageFormat getSelectedMessageFormat(String format) {
    if ("AVRO".equalsIgnoreCase(format)) {
      return MessageFormat.AVRO;
    } else if ("PROTOBUF".equalsIgnoreCase(format)) {
      return MessageFormat.PROTOBUF;
    } else if ("MSGPACK".equalsIgnoreCase(format)){
      return MessageFormat.MSGPACK;
    } else {
      return MessageFormat.DEFAULT;
    }
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
  @GetMapping(value = "/topic/{name:.+}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody
  List<Object> getPartitionOrMessages(
      @PathVariable("name") String topicName,
      @RequestParam(name = "partition", required = false) Integer partition,
      @RequestParam(name = "offset", required = false) Long offset,
      @RequestParam(name = "count", required = false) Integer count,
      @RequestParam(name = "format", required = false) String format,
      @RequestParam(name = "keyFormat", required = false) String keyFormat,
      @RequestParam(name = "descFile", required = false) String descFile,
      @RequestParam(name = "msgTypeName", required = false) String msgTypeName
  ) {
    if (partition == null || offset == null || count == null) {
      final TopicVO topic = kafkaMonitor.getTopic(topicName)
          .orElseThrow(() -> new TopicNotFoundException(topicName));

      List<Object> partitionList = new ArrayList<>();
      topic.getPartitions().forEach(vo -> partitionList.add(new PartitionOffsetInfo(vo.getId(), vo.getFirstOffset(), vo.getSize())));

      return partitionList;
    } else {

      final var deserializers = new Deserializers(
              getDeserializer(topicName, getSelectedMessageFormat(keyFormat), descFile, msgTypeName),
              getDeserializer(topicName, getSelectedMessageFormat(format), descFile, msgTypeName));

      List<Object> messages = new ArrayList<>();
      List<MessageVO> vos = messageInspector.getMessages(
          topicName,
          partition,
          offset,
          count,
          deserializers);

      if (vos != null) {
        messages.addAll(vos);
      }

      return messages;
    }
  }

  private MessageDeserializer getDeserializer(String topicName, MessageFormat format, String descFile, String msgTypeName) {
    final MessageDeserializer deserializer;

    if (format == MessageFormat.AVRO) {
      final var schemaRegistryUrl = schemaRegistryProperties.getConnect();
      final var schemaRegistryAuth = schemaRegistryProperties.getAuth();

      deserializer = new AvroMessageDeserializer(topicName, schemaRegistryUrl, schemaRegistryAuth);
    } else if (format == MessageFormat.PROTOBUF && null != descFile) {
      // filter the input file name

      final var descFileName = descFile.replace(".desc", "")
          .replace(".", "")
          .replace("/", "");
      final var fullDescFile = protobufProperties.getDirectory() + File.separator + descFileName + ".desc";
      deserializer = new ProtobufMessageDeserializer(fullDescFile, msgTypeName);
    } else if (format == MessageFormat.PROTOBUF) {
      final var schemaRegistryUrl = schemaRegistryProperties.getConnect();
      final var schemaRegistryAuth = schemaRegistryProperties.getAuth();

      deserializer = new ProtobufSchemaRegistryMessageDeserializer(topicName, schemaRegistryUrl, schemaRegistryAuth);
    } else if (format == MessageFormat.MSGPACK) {
      deserializer = new MsgPackMessageDeserializer();
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

    private MessageFormat keyFormat;
    
    private String descFile;
    
    private String msgTypeName;

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

    public MessageFormat getKeyFormat() {
      return keyFormat;
    }

    public void setKeyFormat(MessageFormat keyFormat) { this.keyFormat = keyFormat; }

    public MessageFormat getFormat() {
      return format;
    }

    public void setFormat(MessageFormat format) {
      this.format = format;
    }

    public String getDescFile() {
      return descFile;
    }

    public void setDescFile(String descFile) {
      this.descFile = descFile;
    }

    public String getMsgTypeName() {
      return msgTypeName;
    }

    public void setMsgTypeName(String msgTypeName) {
      this.msgTypeName = msgTypeName;
    }

  }
}
