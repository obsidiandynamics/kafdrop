package kafdrop.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@ApiModel("publish payload model")
public final class PublishPayloadVO {
  @ApiParam("Topic name")
  String name;

  @ApiParam("Payload")
  String payload;

}
