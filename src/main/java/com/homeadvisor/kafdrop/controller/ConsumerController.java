package com.homeadvisor.kafdrop.controller;

import com.homeadvisor.kafdrop.service.ConsumerNotFoundException;
import com.homeadvisor.kafdrop.service.KafkaMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/consumer")
public class ConsumerController
{
   @Autowired
   private KafkaMonitor kafkaMonitor;

   @RequestMapping("/{groupId:.+}")
   public String consumerDetail(@PathVariable("groupId") String groupId, Model model)
   {
      model.addAttribute("consumer", kafkaMonitor.getConsumer(groupId)
         .orElseThrow(() -> new ConsumerNotFoundException(groupId)));
      return "consumer-detail";
   }

}
