package kafdrop.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public final class CreateMessageVO {

  private int topicPartition;

  private String key;

  private String value;

  private String topic;
}
