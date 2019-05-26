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

import com.homeadvisor.kafdrop.config.*;
import com.homeadvisor.kafdrop.model.*;
import com.homeadvisor.kafdrop.service.*;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.*;

@Controller
public class ClusterController {
  @Autowired
  private KafkaMonitor kafkaMonitor;

  @Autowired
  private CuratorConfiguration.ZookeeperProperties zookeeperProperties;

  @RequestMapping("/")
  public String clusterInfo(Model model,
                            @RequestParam(value = "filter", required = false) String filter) {
    model.addAttribute("zookeeper", zookeeperProperties);

    final List<BrokerVO> brokers = kafkaMonitor.getBrokers();
    final List<TopicVO> topics = kafkaMonitor.getTopics();
    final ClusterSummaryVO clusterSummary = kafkaMonitor.getClusterSummary(topics);

    final List<Integer> missingBrokerIds = clusterSummary.getExpectedBrokerIds().stream()
        .filter(brokerId -> brokers.stream().noneMatch(b -> b.getId() == brokerId))
        .collect(Collectors.toList());

    model.addAttribute("brokers", brokers);
    model.addAttribute("missingBrokerIds", missingBrokerIds);
    model.addAttribute("topics", topics);
    model.addAttribute("clusterSummary", clusterSummary);

    if (filter != null) {
      model.addAttribute("filter", filter);
    }

    return "cluster-overview";
  }

  @ApiOperation(value = "getCluster", notes = "Get high level broker, topic, and partition data for the Kafka cluster")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Success", response = ClusterInfoVO.class)
  })
  @RequestMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
  public @ResponseBody
  ClusterInfoVO getCluster() throws Exception {
    ClusterInfoVO vo = new ClusterInfoVO();

    vo.zookeeper = zookeeperProperties;
    vo.brokers = kafkaMonitor.getBrokers();
    vo.topics = kafkaMonitor.getTopics();
    vo.summary = kafkaMonitor.getClusterSummary(vo.topics);

    return vo;
  }

  @ExceptionHandler(BrokerNotFoundException.class)
  private String brokerNotFound(Model model) {
    model.addAttribute("zookeeper", zookeeperProperties);
    model.addAttribute("brokers", Collections.emptyList());
    model.addAttribute("topics", Collections.emptyList());
    return "cluster-overview";

  }

  @ResponseStatus(HttpStatus.OK)
  @RequestMapping("/health_check")
  public void healthCheck() {
  }

  /**
   * Simple DTO to encapsulate the cluster state: ZK properties, broker list,
   * and topic list.
   */
  public static class ClusterInfoVO {
    public CuratorConfiguration.ZookeeperProperties zookeeper;
    public ClusterSummaryVO summary;
    public List<BrokerVO> brokers;
    public List<TopicVO> topics;
  }
}
