/*
 * Copyright 2017 HomeAdvisor, Inc.
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

import com.homeadvisor.kafdrop.model.BrokerVO;
import com.homeadvisor.kafdrop.service.BrokerNotFoundException;
import com.homeadvisor.kafdrop.service.KafkaMonitor;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

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

   @ApiOperation(value = "getBroker", notes = "Get details for a specific Kafka broker")
   @ApiResponses(value = {
         @ApiResponse(code = 200, message = "Success", response = BrokerVO.class),
         @ApiResponse(code = 404, message = "Invalid Broker ID")
   })
   @RequestMapping(path = "/broker/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
   public @ResponseBody BrokerVO brokerDetailsJson(@PathVariable("id") int brokerId)
   {
      return kafkaMonitor.getBroker(brokerId).orElseThrow(() -> new BrokerNotFoundException(String.valueOf(brokerId)));
   }

   @ApiOperation(value = "getAllBrokers", notes = "Get details for all known Kafka brokers")
   @ApiResponses(value = {
         @ApiResponse(code = 200, message = "Success", response = BrokerVO.class)
   })
   @RequestMapping(path = "/broker", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
   public @ResponseBody List<BrokerVO> brokerDetailsJson()
   {
      return kafkaMonitor.getBrokers();
   }
}
