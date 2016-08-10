/*
 * Copyright 2016 HomeAdvisor, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.homeadvisor.kafdrop.controller;

import com.homeadvisor.kafdrop.config.CuratorConfiguration;
import com.homeadvisor.kafdrop.service.BrokerNotFoundException;
import com.homeadvisor.kafdrop.service.KafkaMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collection;
import java.util.Collections;

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

   @ExceptionHandler(BrokerNotFoundException.class)
   private String brokerNotFound(Model model)
   {
      model.addAttribute("zookeeper", zookeeperProperties);
      model.addAttribute("brokers", Collections.emptyList());
      model.addAttribute("topics", Collections.emptyList());
      return "cluster-overview";

   }
}
