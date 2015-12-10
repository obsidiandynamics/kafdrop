package com.homeadvisor.kafdrop.controller;

import com.homeadvisor.kafdrop.model.TopicVO;
import com.homeadvisor.kafdrop.service.KafkaMonitor;
import com.homeadvisor.kafdrop.service.MessageInspector;
import com.homeadvisor.kafdrop.service.TopicNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Controller
public class MessageController
{
   @Autowired
   private KafkaMonitor kafkaMonitor;

   @Autowired
   private MessageInspector messageInspector;

   @RequestMapping(method = RequestMethod.GET, value = "/topic/{name:.+}/messages")
   public String viewMessageForm(@PathVariable("name") String topicName,
                                 @Valid @ModelAttribute("messageForm") MessageInspectorForm messageForm,
                                 BindingResult errors,
                                 Model model)
   {
      if (messageForm.isEmpty())
      {
         final MessageInspectorForm defaultForm = new MessageInspectorForm();
         defaultForm.setCount(1);
         model.addAttribute("messageForm", defaultForm);
      }

      final TopicVO topic = kafkaMonitor.getTopic(topicName)
         .orElseThrow(() -> new TopicNotFoundException(topicName));
      model.addAttribute("topic", topic);

      if (!messageForm.isEmpty() && !errors.hasErrors())
      {
         model.addAttribute("messages",
                            messageInspector.getMessages(topicName,
                                                         messageForm.getPartition(),
                                                         messageForm.getOffset(),
                                                         messageForm.getCount()));
      }

      return "message-inspector";
   }

   public static class MessageInspectorForm
   {
      @NotNull
      @Min(0)
      private Integer partition;

      @NotNull
      @Min(0)
      private Integer offset;

      @NotNull
      @Min(1)
      @Max(100)
      private Integer count;

      public boolean isEmpty()
      {
         return partition == null && offset == null && (count == null || count == 1);
      }

      public Integer getPartition()
      {
         return partition;
      }

      public void setPartition(Integer partition)
      {
         this.partition = partition;
      }

      public Integer getOffset()
      {
         return offset;
      }

      public void setOffset(Integer offset)
      {
         this.offset = offset;
      }

      public Integer getCount()
      {
         return count;
      }

      public void setCount(Integer count)
      {
         this.count = count;
      }
   }
}
