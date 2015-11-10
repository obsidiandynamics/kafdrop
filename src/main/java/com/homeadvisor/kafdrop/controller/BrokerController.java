package com.homeadvisor.kafdrop.controller;

import com.homeadvisor.kafdrop.service.BrokerNotFoundException;
import com.homeadvisor.kafdrop.service.KafkaMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class BrokerController
{
   @Autowired
   private KafkaMonitor kafkaMonitor;

   @RequestMapping("/broker/{id}")
   public String brokerDetails(@PathVariable("id") int brokerId, Model model)
   {
      model.addAttribute("broker", kafkaMonitor.getBroker(brokerId)
         .orElseThrow(() -> new BrokerNotFoundException(String.valueOf(brokerId))));
      model.addAttribute("topics", kafkaMonitor.getTopics());
      return "broker-detail";
   }
}
