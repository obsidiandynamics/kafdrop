/*
 * Copyright 2017 Kafdrop contributors.
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

package kafdrop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kafdrop.model.BrokerVO;
import kafdrop.service.BrokerNotFoundException;
import kafdrop.service.KafkaMonitor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Tag(name = "broker-controller", description = "Broker Controller")
@Controller
public final class BrokerController {
  private final KafkaMonitor kafkaMonitor;

  public BrokerController(KafkaMonitor kafkaMonitor) {
    this.kafkaMonitor = kafkaMonitor;
  }

  @RequestMapping("/broker/{id}")
  public String brokerDetails(@PathVariable("id") int brokerId, Model model) {
    model.addAttribute("broker", kafkaMonitor.getBroker(brokerId)
      .orElseThrow(() -> new BrokerNotFoundException("No such broker " + brokerId)));
    model.addAttribute("topics", kafkaMonitor.getTopics());
    return "broker-detail";
  }

  @Operation(summary = "getBroker", description = "Get details for a specific Kafka broker")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success"),
    @ApiResponse(responseCode = "404", description = "Invalid Broker ID")
  })
  @GetMapping(path = "/broker/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody BrokerVO brokerDetailsJson(@PathVariable("id") int brokerId) {
    return kafkaMonitor.getBroker(brokerId).orElseThrow(() ->
      new BrokerNotFoundException("No such broker " + brokerId));
  }

  @Operation(summary = "getAllBrokers", description = "Get details for all known Kafka brokers")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success")
  })
  @GetMapping(path = "/broker", produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody List<BrokerVO> brokerDetailsJson() {
    return kafkaMonitor.getBrokers();
  }
}
