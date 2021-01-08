package kafdrop.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@ApiModel("Create topic model")
public final class CreateTopicVO {
  @ApiParam("Topic name")
  String name;

  @ApiParam("Number of partitions")
  int partitionsNumber;

  @ApiParam("Replication factor")
  int replicationFactor;
}
