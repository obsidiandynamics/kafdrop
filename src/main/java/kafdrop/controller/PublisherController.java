package kafdrop.controller;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import kafdrop.model.PublishPayloadVO;
import kafdrop.model.TopicVO;
import kafdrop.service.KafkaHighLevelProducer;
import kafdrop.service.KafkaMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/publish")
public class PublisherController {

    @Autowired
    KafkaMonitor kafkaMonitor;

    @RequestMapping("/ui")
    public String consumerDetail(Model model) {
        model.addAttribute("topicNames",kafkaMonitor.getTopics().stream().map(TopicVO::getName).toArray());
        return "publish-payload";
    }

    @PostMapping("{topic}")
    public void publish(@PathVariable("topic") String producerTopics, @RequestBody String message){
        kafkaMonitor.publish(producerTopics,message);
    }


    @ApiOperation(value = "publishPayload", notes = "publish payload")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = String.class)
    })
    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public String publishTopic(PublishPayloadVO publishPayloadVO, Model model) {
        try{
            model.addAttribute("topicName", publishPayloadVO.getName());
            kafkaMonitor.publish(publishPayloadVO.getName(),publishPayloadVO.getPayload());
        }catch(Exception e){
            model.addAttribute("errorMessage", e.getMessage());
        }
        model.addAttribute("published", true);
        return "publish-payload";
    }
}
