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

import io.swagger.annotations.*;
import kafdrop.config.*;
import kafdrop.config.CuratorConfiguration.*;
import kafdrop.model.*;
import kafdrop.service.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.*;

@Controller
public final class ClusterController {
  private final KafkaConfiguration kafkaConfiguration;

  private final KafkaMonitor kafkaMonitor;

  private final CuratorConfiguration.ZookeeperProperties zookeeperProperties;

  public ClusterController(KafkaConfiguration kafkaConfiguration, KafkaMonitor kafkaMonitor, ZookeeperProperties zookeeperProperties) {
    this.kafkaConfiguration = kafkaConfiguration;
    this.kafkaMonitor = kafkaMonitor;
    this.zookeeperProperties = zookeeperProperties;
  }

  @RequestMapping("/")
  public String clusterInfo(Model model,
                            @RequestParam(value = "filter", required = false) String filter) {
    model.addAttribute("bootstrapServers", kafkaConfiguration.getBrokerConnect());

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
  public @ResponseBody ClusterInfoVO getCluster() {
    final ClusterInfoVO vo = new ClusterInfoVO();
    vo.zookeeper = zookeeperProperties;
    vo.brokers = kafkaMonitor.getBrokers();
    vo.topics = kafkaMonitor.getTopics();
    vo.summary = kafkaMonitor.getClusterSummary(vo.topics);
    return vo;
  }

  @ExceptionHandler(BrokerNotFoundException.class)
  public String brokerNotFound(Model model) {
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
  public static final class ClusterInfoVO {
    CuratorConfiguration.ZookeeperProperties zookeeper;
    ClusterSummaryVO summary;
    List<BrokerVO> brokers;
    List<TopicVO> topics;
  }
}
