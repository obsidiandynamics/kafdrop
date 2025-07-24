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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import kafdrop.config.MessageFormatConfiguration.MessageFormatProperties;
import kafdrop.config.ProtobufDescriptorConfiguration.ProtobufDescriptorProperties;
import kafdrop.config.SchemaRegistryConfiguration.SchemaRegistryProperties;
import kafdrop.form.SearchMessageForm;
import kafdrop.form.SearchMessageFormForJson;
import kafdrop.model.CreateMessageVO;
import kafdrop.model.MessageVO;
import kafdrop.model.SearchResultsVO;
import kafdrop.model.TopicPartitionVO;
import kafdrop.model.TopicVO;
import kafdrop.service.KafkaMonitor;
import kafdrop.service.MessageInspector;
import kafdrop.service.TopicNotFoundException;
import kafdrop.util.AvroMessageDeserializer;
import kafdrop.util.AvroMessageSerializer;
import kafdrop.util.DefaultMessageDeserializer;
import kafdrop.util.DefaultMessageSerializer;
import kafdrop.util.Deserializers;
import kafdrop.util.IntMessageDeserializer;
import kafdrop.util.KeyFormat;
import kafdrop.util.MessageDeserializer;
import kafdrop.util.MessageFormat;
import kafdrop.util.MessageSerializer;
import kafdrop.util.MsgPackMessageDeserializer;
import kafdrop.util.MsgPackMessageSerializer;
import kafdrop.util.ProtobufMessageDeserializer;
import kafdrop.util.ProtobufMessageSerializer;
import kafdrop.util.ProtobufSchemaRegistryMessageDeserializer;
import kafdrop.util.Serializers;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Tag(name = "message-controller", description = "Message Controller")
@Controller
public final class MessageController {
  private final KafkaMonitor kafkaMonitor;

  private final MessageInspector messageInspector;

  private final MessageFormatProperties messageFormatProperties;

  private final SchemaRegistryProperties schemaRegistryProperties;

  private final ProtobufDescriptorProperties protobufProperties;

  private final boolean sendEnabled;

  public MessageController(KafkaMonitor kafkaMonitor, MessageInspector messageInspector,
                           MessageFormatProperties messageFormatProperties,
                           SchemaRegistryProperties schemaRegistryProperties,
                           ProtobufDescriptorProperties protobufProperties,
                           @Value("${message.sendEnabled:false}") boolean sendEnabled) {
    this.kafkaMonitor = kafkaMonitor;
    this.messageInspector = messageInspector;
    this.messageFormatProperties = messageFormatProperties;
    this.schemaRegistryProperties = schemaRegistryProperties;
    this.protobufProperties = protobufProperties;
    this.sendEnabled = sendEnabled;
  }

  /**
   * Human friendly view of reading all topic messages sorted by timestamp.
   *
   * @param topicName Name of topic
   * @param model
   * @return View for seeing all messages in a topic sorted by timestamp.
   */
  @GetMapping("/topic/{name:.+}/allmessages")
  public String viewAllMessages(@PathVariable("name") String topicName,
                                Model model, @RequestParam(name = "count", required = false) Integer count) {
    final int size = (count != null ? count : 100);
    final MessageFormat defaultFormat = messageFormatProperties.getFormat();
    final MessageFormat defaultKeyFormat = messageFormatProperties.getKeyFormat();
    final TopicVO topic = kafkaMonitor.getTopic(topicName)
      .orElseThrow(() -> new TopicNotFoundException(topicName));

    model.addAttribute("topic", topic);
    model.addAttribute("defaultFormat", defaultFormat);
    model.addAttribute("defaultKeyFormat", defaultKeyFormat);
    model.addAttribute("messageFormats", MessageFormat.values());
    model.addAttribute("keyFormats", KeyFormat.values());
    model.addAttribute("descFiles", protobufProperties.getDescFilesList());

    model.addAttribute("messages", getMessages(topicName, defaultKeyFormat, defaultFormat, topic, size));

    return "topic-messages";
  }

  /**
   * JSON array of reading all topic messages sorted by timestamp.
   *
   * @param topicName Name of topic
   * @param count     Count of messages
   * @return JSON array for seeing all messages in a topic sorted by timestamp.
   */
  @Operation(summary = "getAllMessages", description = "Get all messages from topic")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success"),
    @ApiResponse(responseCode = "404", description = "Invalid topic name")
  })
  @GetMapping(value = "/topic/{name:.+}/allmessages", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<MessageVO> getAllMessages(@PathVariable("name") String topicName,
                                        @RequestParam(name = "count", required = false) Integer count) {
    final int size = (count != null ? count : 100);
    final MessageFormat defaultFormat = messageFormatProperties.getFormat();
    final MessageFormat defaultKeyFormat = messageFormatProperties.getKeyFormat();
    final TopicVO topic = kafkaMonitor.getTopic(topicName)
      .orElseThrow(() -> new TopicNotFoundException(topicName));

    return getMessages(topicName, defaultKeyFormat, defaultFormat, topic, size);
  }

  private @org.jetbrains.annotations.NotNull List<MessageVO> getMessages(String topicName,
                                                                         MessageFormat defaultKeyFormat,
                                                                         MessageFormat defaultFormat, TopicVO topic,
                                                                         int size) {
    final var deserializers = new Deserializers(
      getDeserializer(topicName, defaultKeyFormat, "", "", protobufProperties.getParseAnyProto()),
      getDeserializer(topicName, defaultFormat, "", "", protobufProperties.getParseAnyProto()));

    final List<MessageVO> messages = new ArrayList<>();

    for (TopicPartitionVO partition : topic.getPartitions()) {
      messages.addAll(messageInspector.getMessages(topicName,
        partition.getId(),
        partition.getFirstOffset(),
        size,
        deserializers));
    }

    messages.sort(Comparator.comparing(MessageVO::getTimestamp));
    return messages;
  }

  /**
   * Human friendly view of reading messages.
   *
   * @param topicName   Name of topic
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
    final MessageFormat defaultKeyFormat = messageFormatProperties.getKeyFormat();

    if (messageForm.isEmpty()) {
      final PartitionOffsetInfo defaultForm = new PartitionOffsetInfo();

      defaultForm.setCount(100l);
      defaultForm.setOffset(0l);
      defaultForm.setPartition(0);
      defaultForm.setFormat(defaultFormat);
      defaultForm.setKeyFormat(defaultKeyFormat);
      defaultForm.setIsAnyProto(protobufProperties.getParseAnyProto());

      model.addAttribute("messageForm", defaultForm);
    }

    final TopicVO topic = kafkaMonitor.getTopic(topicName)
      .orElseThrow(() -> new TopicNotFoundException(topicName));
    model.addAttribute("topic", topic);
    // pre-select a descriptor file for a specific topic if available
    model.addAttribute("defaultDescFile", protobufProperties.getDescFilesList().stream()
      .filter(descFile -> descFile.replace(".desc", "").equals(topicName)).findFirst().orElse(""));

    model.addAttribute("defaultFormat", defaultFormat);
    model.addAttribute("messageFormats", MessageFormat.values());
    model.addAttribute("defaultKeyFormat", defaultKeyFormat);
    model.addAttribute("keyFormats", KeyFormat.values());
    model.addAttribute("descFiles", protobufProperties.getDescFilesList());
    model.addAttribute("isAnyProtoOpts", List.of(true, false));
    model.addAttribute("sendEnabled", sendEnabled);

    if (!messageForm.isEmpty() && !errors.hasErrors()) {

      final var deserializers = new Deserializers(
        getDeserializer(topicName, messageForm.getKeyFormat(), messageForm.getDescFile(),
          messageForm.getMsgTypeName(), messageForm.getIsAnyProto()),
        getDeserializer(topicName, messageForm.getFormat(), messageForm.getDescFile(), messageForm.getMsgTypeName(),
          messageForm.getIsAnyProto())
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

  @PostMapping("/topic/{name:.+}/addmessage")
  public String addMessage(
    @PathVariable("name")
    String topicName,
    @ModelAttribute("addMessageForm") CreateMessageVO body,
    Model model) {
    try {
      if (!sendEnabled) {
        throw new IllegalStateException("Message sending is not enabled.");
      }
      final MessageFormat defaultFormat = messageFormatProperties.getFormat();
      final MessageFormat defaultKeyFormat = messageFormatProperties.getKeyFormat();

      final var serializers = new Serializers(
        getSerializer(topicName, defaultKeyFormat, "", ""),
        getSerializer(topicName, defaultFormat, "", ""));
      RecordMetadata recordMetadata = kafkaMonitor.publishMessage(body, serializers);

      final var deserializers = new Deserializers(
        getDeserializer(topicName, defaultKeyFormat, "", ""),
        getDeserializer(topicName, defaultFormat, "", "")
      );

      final PartitionOffsetInfo defaultForm = new PartitionOffsetInfo();

      defaultForm.setCount(100l);
      defaultForm.setOffset(recordMetadata.offset());
      defaultForm.setPartition(body.getTopicPartition());
      defaultForm.setFormat(defaultFormat);
      defaultForm.setKeyFormat(defaultFormat);

      model.addAttribute("messageForm", defaultForm);

      final TopicVO topic = kafkaMonitor.getTopic(topicName)
        .orElseThrow(() -> new TopicNotFoundException(topicName));

      model.addAttribute("topic", topic);

      model.addAttribute("defaultFormat", defaultFormat);
      model.addAttribute("messageFormats", MessageFormat.values());
      model.addAttribute("defaultKeyFormat", defaultKeyFormat);
      model.addAttribute("keyFormats", KeyFormat.values());
      model.addAttribute("descFiles", protobufProperties.getDescFilesList());
      model.addAttribute("messages",
        messageInspector.getMessages(topicName,
          body.getTopicPartition(),
          recordMetadata.offset(),
          100,
          deserializers));
      model.addAttribute("isAnyProtoOpts", List.of(true, false));

    } catch (Exception ex) {
      model.addAttribute("errorMessage", ex.getMessage());
    }
    return "message-inspector";
  }

  /**
   * Human friendly view of searching messages.
   *
   * @param topicName         Name of topic
   * @param searchMessageForm Message form for submitting requests to search messages.
   * @param errors
   * @param model
   * @return View for seeing messages in a partition.
   */
  @GetMapping("/topic/{name:.+}/search-messages")
  public String searchMessageForm(@PathVariable("name") String topicName,
                                  @Valid @ModelAttribute("searchMessageForm") SearchMessageForm searchMessageForm,
                                  BindingResult errors,
                                  Model model) {
    final MessageFormat defaultFormat = messageFormatProperties.getFormat();
    final MessageFormat defaultKeyFormat = messageFormatProperties.getKeyFormat();

    if (searchMessageForm.isEmpty()) {
      final SearchMessageForm defaultForm = new SearchMessageForm();

      defaultForm.setSearchText("");
      defaultForm.setFormat(defaultFormat);
      defaultForm.setKeyFormat(defaultKeyFormat);
      defaultForm.setPartition(-1);
      defaultForm.setMaximumCount(100);
      defaultForm.setStartTimestamp(new Date(0));
      model.addAttribute("searchMessageForm", defaultForm);
    }

    final TopicVO topic = kafkaMonitor.getTopic(topicName)
      .orElseThrow(() -> new TopicNotFoundException(topicName));

    model.addAttribute("topic", topic);
    model.addAttribute("defaultFormat", defaultFormat);
    model.addAttribute("messageFormats", MessageFormat.values());
    model.addAttribute("defaultKeyFormat", defaultKeyFormat);
    model.addAttribute("keyFormats", KeyFormat.values());
    model.addAttribute("partitions", topic.getPartitions().stream().map(TopicPartitionVO::getId).toList());
    model.addAttribute("descFiles", protobufProperties.getDescFilesList());

    if (!searchMessageForm.isEmpty() && !errors.hasErrors()) {
      final var searchResults = searchMessageVOs(topicName, searchMessageForm, searchMessageForm.getDescFile(),
        searchMessageForm.getMsgTypeName());

      model.addAttribute("messages", searchResults.getMessages());
      model.addAttribute("details", searchResults.getCompletionDetails());
    }

    return "search-message";
  }

  private SearchResultsVO searchMessageVOs(String topicName, SearchMessageFormForJson searchMessageForm,
                                           String descFile,
                                           String msgTypeName) {
    final var deserializers = new Deserializers(
      getDeserializer(topicName, searchMessageForm.getKeyFormat(), descFile,
        msgTypeName,
        protobufProperties.getParseAnyProto()),
      getDeserializer(topicName, searchMessageForm.getFormat(), descFile,
        msgTypeName,
        protobufProperties.getParseAnyProto())
    );

    return kafkaMonitor.searchMessages(
      topicName,
      searchMessageForm.getSearchText(),
      searchMessageForm.getPartition(),
      searchMessageForm.getMaximumCount(),
      searchMessageForm.getStartTimestamp(),
      deserializers);
  }

  /**
   * Searches for messages in a specific topic based on criteria provided in the request body.
   * This endpoint expects a POST request with a JSON payload.
   *
   * @param topicName         The name of the topic to search within.
   * @param searchMessageForm A JSON object in the request body containing the search criteria. All fields are optional.
   *                          <ul>
   *                              <li><b>searchText</b>: Text to search for in the message payload. (Default: "")</li>
   *                              <li><b>maximumCount</b>: Maximum number of messages to return. (Default: 1000)</li>
   *                              <li><b>partition</b>: Specific partition to search in. (Default: -1 for all
   *                              partitions)</li>
   *                              <li><b>format</b>: Deserialization format for the message value. (Default: DEFAULT)
   *                              </li>
   *                              <li><b>keyFormat</b>: Deserialization format for the message key. (Default:
   *                              DEFAULT)</li>
   *                              <li><b>startTimestamp</b>: Start timestamp in ISO 8601 UTC format. (Example: {@code
   *                              1970-01-01T00:00:00.000Z})</li>
   *                              <li><b>keys</b>: An array of message keys to filter by. (Example: {@code ["key1",
   *                              "key2"]})</li>
   *                          </ul>
   * @param errors            BindingResult for validation, automatically populated by Spring.
   * @return A {@link SearchResultsVO} object containing the found messages (sorted by timestamp) and search
   * completion details.
   */

  @Operation(summary = "searchMessages", description = "Search messages and return results as JSON")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success"),
    @ApiResponse(responseCode = "400", description = "Body has validation errors"),
    @ApiResponse(responseCode = "404", description = "Invalid topic name")
  })
  @PostMapping(value = "/topic/{name:.+}/search-messages",
    produces = MediaType.APPLICATION_JSON_VALUE,
    consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public SearchResultsVO searchMessages(@PathVariable("name") String topicName,
                                        @Valid @RequestBody SearchMessageFormForJson searchMessageForm,
                                        BindingResult errors) {

    if (errors.hasErrors()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errors.getAllErrors().toString());

    kafkaMonitor.getTopic(topicName)
      .orElseThrow(() -> new TopicNotFoundException(topicName));

    final var searchResultsVO = searchMessageVOs(topicName, searchMessageForm, null, null);

    if (searchMessageForm.getKeys() != null) {
      var filteredByKeyMessages = searchResultsVO.getMessages().stream()
        .filter(
          messageVO -> Arrays.asList(searchMessageForm.getKeys()).contains(messageVO.getKey()))
        .sorted(Comparator.comparing(MessageVO::getTimestamp))
        .toList();

      searchResultsVO.setMessages(filteredByKeyMessages);
    }

    return searchResultsVO;
  }

  /**
   * Returns the selected message format based on the form submission
   *
   * @param format String representation of format name
   * @return
   */
  private MessageFormat getSelectedMessageFormat(String format) {
    if ("AVRO".equalsIgnoreCase(format)) {
      return MessageFormat.AVRO;
    } else if ("PROTOBUF".equalsIgnoreCase(format)) {
      return MessageFormat.PROTOBUF;
    } else if ("MSGPACK".equalsIgnoreCase(format)) {
      return MessageFormat.MSGPACK;
    } else {
      return MessageFormat.DEFAULT;
    }
  }


  /**
   * Return a JSON list of all partition offset info for the given topic. If specific partition
   * and offset parameters are given, then this returns actual kafka messages from that partition
   * (if the offsets are valid; if invalid offsets are passed then the message list is empty).
   *
   * @param topicName Name of topic.
   * @return Offset or message data.
   */
  @Operation(summary = "getPartitionOrMessages"
    , description = "Get offset or message data for a topic. Without query params returns all partitions with offset " +
    "data. With query params, returns actual messages (if valid offsets are provided).")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success"),
    @ApiResponse(responseCode = "404", description = "Invalid topic name")
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
    @RequestParam(name = "msgTypeName", required = false) String msgTypeName,
    @RequestParam(name = "isAnyProto", required = false) Boolean isAnyProto
  ) {
    if (partition == null || offset == null || count == null) {
      final TopicVO topic = kafkaMonitor.getTopic(topicName)
        .orElseThrow(() -> new TopicNotFoundException(topicName));

      List<Object> partitionList = new ArrayList<>();
      topic.getPartitions().forEach(vo -> partitionList.add(new PartitionOffsetInfo(vo.getId(), vo.getFirstOffset(),
        vo.getSize())));

      return partitionList;
    } else {

      final var deserializers = new Deserializers(
        getDeserializer(topicName, getSelectedMessageFormat(keyFormat), descFile, msgTypeName, isAnyProto),
        getDeserializer(topicName, getSelectedMessageFormat(format), descFile, msgTypeName, isAnyProto));

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

  private MessageDeserializer getDeserializer(String topicName, MessageFormat format, String descFile,
                                              String msgTypeName) {
    return getDeserializer(topicName, format, descFile, msgTypeName, false);
  }

  private MessageDeserializer getDeserializer(String topicName, MessageFormat format, String descFile,
                                              String msgTypeName, boolean isAnyProto) {
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
      deserializer = new ProtobufMessageDeserializer(fullDescFile, msgTypeName, isAnyProto);
    } else if (format == MessageFormat.PROTOBUF) {
      final var schemaRegistryUrl = schemaRegistryProperties.getConnect();
      final var schemaRegistryAuth = schemaRegistryProperties.getAuth();

      deserializer = new ProtobufSchemaRegistryMessageDeserializer(topicName, schemaRegistryUrl, schemaRegistryAuth);
    } else if (format == MessageFormat.MSGPACK) {
      deserializer = new MsgPackMessageDeserializer();
    } else if (format == MessageFormat.INT) {
      deserializer = new IntMessageDeserializer();
    } else {
      deserializer = new DefaultMessageDeserializer();
    }

    return deserializer;
  }

  private MessageSerializer getSerializer(String topicName, MessageFormat format, String descFile, String msgTypeName) {
    final MessageSerializer serializer;

    if (format == MessageFormat.AVRO) {
      final var schemaRegistryUrl = schemaRegistryProperties.getConnect();
      final var schemaRegistryAuth = schemaRegistryProperties.getAuth();

      serializer = new AvroMessageSerializer(topicName, schemaRegistryUrl, schemaRegistryAuth);
    } else if (format == MessageFormat.PROTOBUF) {
      // filter the input file name
      final var descFileName = descFile.replace(".desc", "")
        .replaceAll("\\.", "")
        .replaceAll("/", "");
      final var fullDescFile = protobufProperties.getDirectory() + File.separator + descFileName + ".desc";
      serializer = new ProtobufMessageSerializer(fullDescFile, msgTypeName);
    } else if (format == MessageFormat.MSGPACK) {
      serializer = new MsgPackMessageSerializer();
    } else {
      serializer = new DefaultMessageSerializer();
    }

    return serializer;
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

    private Boolean isAnyProto = Boolean.FALSE;

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

    public void setKeyFormat(MessageFormat keyFormat) {
      this.keyFormat = keyFormat;
    }

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

    public Boolean getIsAnyProto() {
      return isAnyProto;
    }

    public void setIsAnyProto(Boolean isAnyProto) {
      this.isAnyProto = isAnyProto;
    }
  }
}
