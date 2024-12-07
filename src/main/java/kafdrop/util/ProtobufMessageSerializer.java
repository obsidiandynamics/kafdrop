package kafdrop.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;

public class ProtobufMessageSerializer implements MessageSerializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProtobufMessageSerializer.class);
  private final String fullDescFile;
  private final String msgTypeName;

  public ProtobufMessageSerializer(String fullDescFile, String msgTypeName) {
    this.fullDescFile = fullDescFile;
    this.msgTypeName = msgTypeName;
  }

  @Override
  public byte[] serializeMessage(String value) {
    try (InputStream input = new FileInputStream(new File(fullDescFile))) {
      FileDescriptorSet set = FileDescriptorSet.parseFrom(input);

      List<FileDescriptor> fileDescriptors = new ArrayList<>();
      for (FileDescriptorProto ffdp : set.getFileList()) {
        FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(ffdp,
          fileDescriptors.toArray(new FileDescriptor[fileDescriptors.size()]));
        fileDescriptors.add(fileDescriptor);
      }

      final var descriptors =
        fileDescriptors.stream().flatMap(desc -> desc.getMessageTypes().stream()).collect(Collectors.toList());

      final var messageDescriptor = descriptors.stream().filter(desc -> msgTypeName.equals(desc.getName())).findFirst();
      if (messageDescriptor.isEmpty()) {
        final String errorMsg = String.format("Can't find specific message type: %s", msgTypeName);
        LOGGER.error(errorMsg);
        throw new SerializationException(errorMsg);
      }

      return DynamicMessage.parseFrom(messageDescriptor.get(), value.getBytes()).toByteArray();

    } catch (FileNotFoundException e) {
      final String errorMsg = String.format("Couldn't open descriptor file: %s", fullDescFile);
      LOGGER.error(errorMsg, e);
      throw new SerializationException(errorMsg);
    } catch (IOException e) {
      final String errorMsg = "Can't decode Protobuf message";
      LOGGER.error(errorMsg, e);
      throw new SerializationException(errorMsg);
    } catch (DescriptorValidationException e) {
      final String errorMsg = String.format("Can't compile proto message type: %s", msgTypeName);
      LOGGER.error(errorMsg, e);
      throw new SerializationException(errorMsg);
    }
  }

}
