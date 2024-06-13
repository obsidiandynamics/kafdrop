package kafdrop.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@ApiModel("Update topic retention model")
public final class UpdateTopicRetentionVO {
  @ApiParam("Retention period in ms")
  int retentionTime;
}
