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
import kafdrop.model.*;
import kafdrop.service.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.*;
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

  private final BuildProperties buildProperties;

  private final boolean topicCreateEnabled;

  public ClusterController(KafkaConfiguration kafkaConfiguration, KafkaMonitor kafkaMonitor, ObjectProvider<BuildInfo> buildInfoProvider,
          @Value("${topic.createEnabled:true}") Boolean topicCreateEnabled) {
    this.kafkaConfiguration = kafkaConfiguration;
    this.kafkaMonitor = kafkaMonitor;
    this.buildProperties = buildInfoProvider.stream()
        .map(BuildInfo::getBuildProperties)
        .findAny()
        .orElseGet(ClusterController::blankBuildProperties);
    this.topicCreateEnabled = topicCreateEnabled;
  }

  private static BuildProperties blankBuildProperties() {
    final var properties = new Properties();
    properties.setProperty("version", "3.x");
    properties.setProperty("time", String.valueOf(System.currentTimeMillis()));
    return new BuildProperties(properties);
  }

  @RequestMapping("/")
  public String clusterInfo(Model model,
                            @RequestParam(value = "filter", required = false) String filter) {
    model.addAttribute("bootstrapServers", kafkaConfiguration.getBrokerConnect());
    model.addAttribute("buildProperties", buildProperties);

    final var brokers = kafkaMonitor.getBrokers();
    final var topics = kafkaMonitor.getTopics();
    final var clusterSummary = kafkaMonitor.getClusterSummary(topics);

    final var missingBrokerIds = clusterSummary.getExpectedBrokerIds().stream()
        .filter(brokerId -> brokers.stream().noneMatch(b -> b.getId() == brokerId))
        .collect(Collectors.toList());

    model.addAttribute("brokers", brokers);
    model.addAttribute("missingBrokerIds", missingBrokerIds);
    model.addAttribute("topics", topics);
    model.addAttribute("clusterSummary", clusterSummary);
    model.addAttribute("topicCreateEnabled", topicCreateEnabled);

    if (filter != null) {
      model.addAttribute("filter", filter);
    }

    return "cluster-overview";
  }

  @ApiOperation(value = "getCluster", notes = "Get high level broker, topic, and partition data for the Kafka cluster")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Success", response = ClusterInfoVO.class)
  })
  @GetMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody ClusterInfoVO getCluster() {
    final var vo = new ClusterInfoVO();
    vo.brokers = kafkaMonitor.getBrokers();
    vo.topics = kafkaMonitor.getTopics();
    vo.summary = kafkaMonitor.getClusterSummary(vo.topics);
    return vo;
  }

  @ExceptionHandler(BrokerNotFoundException.class)
  public String brokerNotFound(Model model) {
    model.addAttribute("brokers", Collections.emptyList());
    model.addAttribute("topics", Collections.emptyList());
    return "cluster-overview";
  }

  @ResponseStatus(HttpStatus.OK)
  @RequestMapping("/health_check")
  public void healthCheck() {
    // only http code shall be checked
  }

  /**
   * Simple DTO to encapsulate the cluster state.
   */
  public static final class ClusterInfoVO {
    ClusterSummaryVO summary;
    List<BrokerVO> brokers;
    List<TopicVO> topics;
    public ClusterSummaryVO getSummary() {
      return summary;
    }

    public List<BrokerVO> getBrokers() {
      return brokers;
    }

    public List<TopicVO> getTopics() {
      return topics;
    }
  }
}
