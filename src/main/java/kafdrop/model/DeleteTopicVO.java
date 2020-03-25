package kafdrop.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.Data;

@Data
@ApiModel("Delete topic model")
public final class DeleteTopicVO {
    @ApiParam("Topic name")
    String name;
    public DeleteTopicVO(String topicName){
        this.name = topicName;
    }
}
