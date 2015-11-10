package com.homeadvisor.kafdrop.controller;

import com.homeadvisor.kafdrop.service.KafkaMonitor;
import com.homeadvisor.kafdrop.service.TopicNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/topic")
public class TopicController
{
   @Autowired
   private KafkaMonitor kafkaMonitor;

   @RequestMapping("/{name:.+}")
   public String topicDetails(@PathVariable("name") String topicName, Model model)
   {
      model.addAttribute("topic", kafkaMonitor.getTopic(topicName)
         .orElseThrow(() -> new TopicNotFoundException(topicName)));
      model.addAttribute("consumers", kafkaMonitor.getConsumers(topicName));

      return "topic-detail";
   }

}
