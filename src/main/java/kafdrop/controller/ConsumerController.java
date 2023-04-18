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
import kafdrop.model.ConsumerVO;
import kafdrop.service.KafkaMonitor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Tag(name = "consumer-controller", description = "Consumer Controller")
@Controller
@RequestMapping("/consumer")
public final class ConsumerController {
  private final KafkaMonitor kafkaMonitor;

  public ConsumerController(KafkaMonitor kafkaMonitor) {
    this.kafkaMonitor = kafkaMonitor;
  }

  @RequestMapping("/{groupId:.+}")
  public String consumerDetail(@PathVariable("groupId") String groupId, Model model) throws ConsumerNotFoundException {
    final var consumer = kafkaMonitor.getConsumersByGroup(groupId).stream().findAny();

    model.addAttribute("consumer", consumer.orElseThrow(() -> new ConsumerNotFoundException(groupId)));
    return "consumer-detail";
  }

  @Operation(summary = "getConsumer", description = "Get topic and partition details for a consumer group")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success"),
    @ApiResponse(responseCode = "404", description = "Invalid consumer group")
  })
  @GetMapping(path = "/{groupId:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody ConsumerVO getConsumer(@PathVariable("groupId") String groupId)
    throws ConsumerNotFoundException {
    final var consumer = kafkaMonitor.getConsumersByGroup(groupId).stream().findAny();

    return consumer.orElseThrow(() -> new ConsumerNotFoundException(groupId));
  }
}
