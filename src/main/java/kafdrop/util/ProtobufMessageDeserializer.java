package kafdrop.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;

public class ProtobufMessageDeserializer implements MessageDeserializer {

  private String topic;
  private String fullDescFile;
  private String msgTypeName;
  private Boolean isDelimited;

  private static final Logger LOG = LoggerFactory.getLogger(ProtobufMessageDeserializer.class);

  public ProtobufMessageDeserializer(String topic, String fullDescFile, String msgTypeName, Boolean isDelimited) {
    this.topic = topic;
    this.fullDescFile = fullDescFile;
    this.msgTypeName = msgTypeName;
    this.isDelimited = isDelimited;
  }

  @Override
  public String deserializeMessage(ByteBuffer buffer) {

    try (InputStream input = new FileInputStream(new File(fullDescFile))) {
      FileDescriptorSet set = FileDescriptorSet.parseFrom(input);

      List<FileDescriptor> descs = new ArrayList<>();
      for (FileDescriptorProto ffdp : set.getFileList()) {
        FileDescriptor fd = Descriptors.FileDescriptor.buildFrom(ffdp,
            (FileDescriptor[]) descs.toArray(new FileDescriptor[descs.size()]));
        descs.add(fd);
      }

      final var descriptors = descs.stream().flatMap(desc -> desc.getMessageTypes().stream()).collect(Collectors.toList());

      final var messageDescriptor = descriptors.stream().filter(desc -> msgTypeName.equals(desc.getName())).findFirst();
      if (messageDescriptor.isEmpty()) {
        final String errorMsg = "Can't find specific message type: " + msgTypeName;
        LOG.error(errorMsg);
        throw new DeserializationException(errorMsg);
      }

      final var inputStream = CodedInputStream.newInstance(buffer);
      if (isDelimited) {
        inputStream.readRawVarint32();
      }
      DynamicMessage message = DynamicMessage.parseFrom(messageDescriptor.get(), inputStream);

      JsonFormat.TypeRegistry typeRegistry = JsonFormat.TypeRegistry.newBuilder().add(descriptors).build();
      Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry);

      return printer.print(message).replaceAll("\n", ""); // must remove line break so it defaults to collapse mode
    } catch (FileNotFoundException e) {
      final String errorMsg = "Couldn't open descriptor file: " + fullDescFile;
      LOG.error(errorMsg, e);
      throw new DeserializationException(errorMsg);
    } catch (IOException e) {
      final String errorMsg = "Can't decode Protobuf message";
      LOG.error(errorMsg, e);
      throw new DeserializationException(errorMsg);
    } catch (DescriptorValidationException e) {
      final String errorMsg = "Can't compile proto message type: " + msgTypeName;
      LOG.error(errorMsg, e);
      throw new DeserializationException(errorMsg);
    }
  }

}
