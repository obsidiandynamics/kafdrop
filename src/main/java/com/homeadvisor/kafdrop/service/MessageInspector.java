package com.homeadvisor.kafdrop.service;

import com.homeadvisor.kafdrop.model.MessageVO;
import com.homeadvisor.kafdrop.model.TopicPartitionVO;
import com.homeadvisor.kafdrop.model.TopicVO;
import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.Message;
import kafka.message.MessageAndOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class MessageInspector
{
   private final Logger LOG = LoggerFactory.getLogger(getClass());

   @Autowired
   private KafkaMonitor kafkaMonitor;

   public List<MessageVO> getMessages(String topicName, int partitionId, int offset, int count)
   {
      final TopicVO topic = kafkaMonitor.getTopic(topicName).orElseThrow(TopicNotFoundException::new);
      final TopicPartitionVO partition = topic.getPartition(partitionId).orElseThrow(PartitionNotFoundException::new);

      final SimpleConsumer consumer = kafkaMonitor.getSimpleConsumer(partition.getLeader().getId());
      final FetchRequestBuilder fetchRequestBuilder = new FetchRequestBuilder()
         .clientId("KafDrop")
         .maxWait(5000) // todo: make configurable
         .minBytes(1);

      List<MessageVO> messages = new ArrayList<>(count);
      long currentOffset = offset;
      while (messages.size() < count)
      {
         final FetchRequest fetchRequest =
            fetchRequestBuilder
               .addFetch(topicName, partitionId, currentOffset, 1024 * 1024)
               .build();

         final FetchResponse fetchResponse = consumer.fetch(fetchRequest);
         final ByteBufferMessageSet messageSet = fetchResponse.messageSet(topicName, partitionId);
         if (messageSet.validBytes() <= 0) break;


         int oldSize = messages.size();
         StreamSupport.stream(messageSet.spliterator(), false)
            .limit(count - messages.size())
            .map(MessageAndOffset::message)
            .map(this::createMessage)
            .forEach(messages::add);
         currentOffset += messages.size() - oldSize;
      }
      return messages;
   }

   private MessageVO createMessage(Message message)
   {
      MessageVO vo = new MessageVO();
      if (message.hasKey())
      {
         vo.setKey(readString(message.key()));
      }
      if (!message.isNull())
      {
         vo.setMessage(readString(message.payload()));
      }

      vo.setValid(message.isValid());
      vo.setCompressionCodec(message.compressionCodec().name());
      vo.setChecksum(message.checksum());
      vo.setComputedChecksum(message.computeChecksum());

      return vo;
   }

   private String readString(ByteBuffer buffer)
   {
      try
      {
         return new String(readBytes(buffer), "UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
         return "<unsupported encoding>";
      }
   }

   private byte[] readBytes(ByteBuffer buffer)
   {
      return readBytes(buffer, 0, buffer.limit());
   }

   private byte[] readBytes(ByteBuffer buffer, int offset, int size)
   {
      byte[] dest = new byte[size];
      if (buffer.hasArray())
      {
         System.arraycopy(buffer.array(), buffer.arrayOffset() + offset, dest, 0, size);
      }
      else
      {
         buffer.mark();
         buffer.get(dest);
         buffer.reset();
      }
      return dest;
   }

}
