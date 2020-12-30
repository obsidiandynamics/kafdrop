package kafdrop.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@ApiModel("Create message model")
public final class CreateMessageVO {
	
	@ApiParam("Topic partition")
	private int topicPartition;
	
	@ApiParam("Topic key")
	private String key;
	
	@ApiParam("Topic value")
	private String value;
	
	@ApiParam("Topic name")
	private String topic;
}
