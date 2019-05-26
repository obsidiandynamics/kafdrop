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

import com.homeadvisor.kafdrop.model.*;
import com.homeadvisor.kafdrop.service.*;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/consumer")
public class ConsumerController {
  @Autowired
  private KafkaMonitor kafkaMonitor;

  @RequestMapping("/{groupId:.+}")
  public String consumerDetail(@PathVariable("groupId") String groupId, Model model) {
    model.addAttribute("consumer", kafkaMonitor.getConsumer(groupId)
        .orElseThrow(() -> new ConsumerNotFoundException(groupId)));
    return "consumer-detail";
  }

  @ApiOperation(value = "getConsumer", notes = "Get topic and partition details for a consumer group")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Success", response = ConsumerVO.class),
      @ApiResponse(code = 404, message = "Invalid consumer group")
  })
  @RequestMapping(path = "/{groupId:.+}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
  public @ResponseBody
  ConsumerVO getConsumer(@PathVariable("groupId") String groupId) throws Exception {
    final ConsumerVO consumer = kafkaMonitor.getConsumer(groupId)
        .orElseThrow(() -> new ConsumerNotFoundException(groupId));

    return consumer;
  }

}
