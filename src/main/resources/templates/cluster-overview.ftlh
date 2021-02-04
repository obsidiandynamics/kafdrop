<#--
 Copyright 2021 Kafdrop contributors.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<#import "/spring.ftl" as spring />
<#import "lib/template.ftlh" as template>
<@template.header "Broker List"/>

<script src="<@spring.url '/js/powerFilter.js'/>"></script>


<#setting number_format="0">
<div>
    <div id="kafdropVersion">${buildProperties.getVersion()} [${buildProperties.getTime()}]</div>

    <h2>Kafka Cluster Overview</h2>
    <div id="cluster-overview">
        <table class="table table-bordered">
            <tbody>
            <tr>
                <td><i class="fa fa-server"></i>&nbsp;&nbsp;Bootstrap servers</td>
                <td>${bootstrapServers}</td>
            </tr>
            <tr>
                <td><i class="fa fa-database"></i>&nbsp;&nbsp;Total topics</td>
                <td>${clusterSummary.topicCount}</td>
            </tr>
            <tr>
                <td><i class="fa fa-pie-chart"></i>&nbsp;&nbsp;Total partitions</td>
                <td>${clusterSummary.partitionCount}</td>
            </tr>
            <tr>
                <td><i class="fa fa-trophy"></i>&nbsp;&nbsp;Total preferred partition leader</td>
                <td <#if clusterSummary.preferredReplicaPercent lt 1.0>class="warning"</#if>>${clusterSummary.preferredReplicaPercent?string.percent}</td>
            </tr>
            <tr>
                <td><i class="fa fa-heartbeat"></i>&nbsp;&nbsp;Total under-replicated partitions</td>
                <td <#if clusterSummary.underReplicatedCount gt 0>class="warning"</#if>>${clusterSummary.underReplicatedCount}</td>
            </tr>
            </tbody>
        </table>
    </div>

    <div id="brokers">
        <h3><i class="fa fa-server"></i> Brokers</h3>
        <table class="table table-bordered">
            <thead>
            <tr>
                <th><i class="fa fa-tag"></i>&nbsp;&nbsp;ID</th>
                <th><i class="fa fa-laptop"></i>&nbsp;&nbsp;Host</th>
                <th><i class="fa fa-plug"></i>&nbsp;&nbsp;Port</th>
                <th><i class="fa fa-server"></i>&nbsp;&nbsp;Rack</th>
                <th><i class="fa fa-trophy"></i>&nbsp;&nbsp;Controller</th>
                <th>
                    <i class="fa fa-pie-chart"></i>&nbsp;&nbsp;Number of partitions (% of total)
                    <a title="# of partitions this broker is the leader for"
                       data-toggle="tooltip" data-placement="top" href="#">
                        <i class="fa fa-question-circle"></i>
                    </a>
                </th>
            </tr>
            </thead>
            <tbody>
            <#if brokers?size == 0>
                <tr>
                    <td class="danger text-danger" colspan="8"><i class="fa fa-warning"></i> No brokers available</td>
                </tr>
            <#elseif missingBrokerIds?size gt 0>
                <tr>
                    <td class="danger text-danger" colspan="8"><i class="fa fa-warning"></i> Missing
                        brokers: <#list missingBrokerIds as b>${b}<#if b_has_next>, </#if></#list></td>
                </tr>
            </#if>
            <#list brokers as b>
                <tr>
                    <td><a href="<@spring.url '/broker/${b.id}'/>"><i class="fa fa-info-circle fa-lg"></i> ${b.id}</a></td>
                    <td>${b.host?if_exists}</td>
                    <td>${b.port?string}</td>
                    <td><#if b.rack??>${b.rack}<#else>-</#if></td>
                    <td><@template.yn b.controller/></td>
                    <td>${(clusterSummary.getBrokerLeaderPartitionCount(b.id))!0}
                        (${(clusterSummary.getBrokerLeaderPartitionRatio(b.id))?string.percent})
                    </td>
                </tr>
            </#list>
            </tbody>
        </table>
    </div>

    <div id="topics">
        <h3><i class="fa fa-database"></i> Topics&nbsp;&nbsp;&nbsp;<a href="<@spring.url '/acl'/>"><i class="fa fa-lock"></i> ACLs</a></h3>
        <table class="table table-bordered">
            <thead>
            <tr>
                <th>
                    Name

                    <span style="font-weight:normal;">
                            &nbsp;<INPUT id='filter' size=25 NAME='searchRow' title='Just type to filter the rows'>&nbsp;
                        <span id="rowCount"></span>
                    </span>
                </th>
                <th>
                    Partitions
                    <a title="Number of partitions in the topic"
                       data-toggle="tooltip" data-placement="top" href="#"
                    ><i class="fa fa-question-circle"></i></a>
                </th>
                <th>
                    % Preferred
                    <a title="Percent of partitions where the preferred broker has been assigned leadership"
                       data-toggle="tooltip" data-placement="top" href="#"
                    ><i class="fa fa-question-circle"></i></a>
                </th>
                <th>
                    # Under-replicated
                    <a title="Number of partition replicas that are not in sync with the primary partition"
                       data-toggle="tooltip" data-placement="top" href="#"
                    ><i class="fa fa-question-circle"></i></a>
                </th>
                <th>Custom Config</th>
            </tr>
            </thead>
            <tbody>
            <#if topics?size == 0>
                <tr>
                    <td colspan="5">No topics available</td>
                </tr>
            </#if>
            <#list topics as t>
                <tr class="dataRow">
                    <td><a href="<@spring.url '/topic/${t.name}'/>">${t.name}</a></td>
                    <td>${t.partitions?size}</td>
                    <td <#if t.preferredReplicaPercent lt 1.0>class="warning"</#if>>${t.preferredReplicaPercent?string.percent}</td>
                    <td <#if t.underReplicatedPartitions?size gt 0>class="warning"</#if>>${t.underReplicatedPartitions?size}</td>
                    <td><@template.yn t.config?size gt 0/></td>
                </tr>
            </#list>
            </tbody>
        </table>
        <#if topicCreateEnabled>
            <a class="btn btn-outline-light" href="<@spring.url '/topic/create'/>">
                <i class="fa fa-plus"></i> New
            </a>
        </#if>
    </div>
</div>

<@template.footer/>

<script>
    $(document).ready(function () {
        $('#filter').focus();

        <#if filter??>
        $('#filter').val('${filter}');
        </#if>
        $('[data-toggle="tooltip"]').tooltip()
    });
</script>
