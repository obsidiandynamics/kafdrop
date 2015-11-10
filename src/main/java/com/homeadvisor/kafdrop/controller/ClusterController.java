package com.homeadvisor.kafdrop.controller;

import com.homeadvisor.kafdrop.config.CuratorConfiguration;
import com.homeadvisor.kafdrop.service.KafkaMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ClusterController
{
   @Autowired
   private KafkaMonitor kafkaMonitor;

   @Autowired
   private CuratorConfiguration.ZookeeperProperties zookeeperProperties;

   @RequestMapping("/")
   public String allBrokers(Model model)
   {
      model.addAttribute("zookeeper", zookeeperProperties);
      model.addAttribute("brokers", kafkaMonitor.getBrokers());
      model.addAttribute("topics", kafkaMonitor.getTopics());
      return "cluster-overview";
   }
}
