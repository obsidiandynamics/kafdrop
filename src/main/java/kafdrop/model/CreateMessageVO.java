package kafdrop.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
public final class CreateMessageVO {

  private int topicPartition;

  private String key;

  private String value;

  private String topic;

  private List<HeaderVO> headers = new ArrayList<>();
}
