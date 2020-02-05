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
  private String descFileName;

  private static final Logger LOG = LoggerFactory.getLogger(ProtobufMessageDeserializer.class);

  public ProtobufMessageDeserializer(String topic, String fullDescFile, String msgTypeName) {
    this.topic = topic;
    this.fullDescFile = fullDescFile;
    Path path = Paths.get(fullDescFile);
    descFileName = path.getFileName().toString();
    this.msgTypeName = msgTypeName;
  }

  @Override
  public String deserializeMessage(ByteBuffer buffer) {

    try (InputStream input = new FileInputStream(new File(fullDescFile))) {
      // LOG.info("decoding message: " +
      // Base64.getEncoder().encodeToString(buffer.array()));

      FileDescriptorSet set = FileDescriptorSet.parseFrom(input);
      String protoFileName = descFileName.replace(".desc", ".proto");

      Predicate<FileDescriptorProto> byName = desc -> protoFileName.equals(desc.getName());
      var results = set.getFileList().stream().filter(byName).collect(Collectors.toList());
      if (CollectionUtils.isEmpty(results)) {
        final String errorMsg = "Can't find descriptor in provided descriptor file: " + protoFileName;
        LOG.error(errorMsg);
        throw new DeserializationException(errorMsg);
      }
      List<FileDescriptor> descs = new ArrayList<>();

      for (FileDescriptorProto ffdp : set.getFileList()) {
        FileDescriptor fd = Descriptors.FileDescriptor.buildFrom(ffdp,
            (FileDescriptor[]) descs.toArray(new FileDescriptor[descs.size()]));
        descs.add(fd);
      }

      Predicate<FileDescriptor> fdByName = desc -> protoFileName.equals(desc.getName());
      var fd = descs.stream().filter(fdByName).collect(Collectors.toList()).get(0);

      Predicate<Descriptor> byMsgTypeName = desc -> msgTypeName.equals(desc.getName());

      var msgTypes = fd.getMessageTypes().stream().filter(byMsgTypeName).collect(Collectors.toList());
      if (CollectionUtils.isEmpty(msgTypes)) {
        final String errorMsg = "Can't find specific message type: " + msgTypeName;
        LOG.error(errorMsg);
        throw new DeserializationException(errorMsg);
      }
      Descriptor messageType = msgTypes.get(0);

      DynamicMessage dMsg = DynamicMessage.parseFrom(messageType, CodedInputStream.newInstance(buffer));

      JsonFormat.TypeRegistry typeRegistry = JsonFormat.TypeRegistry.newBuilder()
        .add(descs.stream().flatMap(desc -> desc.getMessageTypes().stream()).collect(Collectors.toList()))
        .build();
      Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry);

      return printer.print(dMsg).replaceAll("\n", ""); // must remove line break so it defaults to collapse mode
    } catch (FileNotFoundException e) {
      final String errorMsg = "Couldn't open descriptor file: " + fullDescFile;
      LOG.error(errorMsg, e);
      throw new DeserializationException(errorMsg);
    } catch (IOException e) {
      final String errorMsg = "Can't decode Protobuf message";
      LOG.error(errorMsg, e);
      throw new DeserializationException(errorMsg);
    } catch (DescriptorValidationException e) {
      final String errorMsg = "Can't compile proto message type" + msgTypeName;
      LOG.error(errorMsg, e);
      throw new DeserializationException(errorMsg);
    }
  }

}
