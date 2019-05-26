<#--
 Copyright 2016 HomeAdvisor, Inc.

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
<#import "lib/template.ftl" as template>
<@template.header "Consumer: ${consumer.groupId}"/>

<#setting number_format="0">

<h1>Kafka Consumer: ${consumer.groupId}</h1>

<div id="overview">
    <h2>Overview</h2>
    <table class="table table-bordered overview">
        <tbody>
        <tr>
            <td>Active Instances</td>
            <td>${consumer.activeInstances?size}</td>
        </tr>
        <tr>
            <td>Topics</td>
            <td>${consumer.topics?size}</td>
        </tr>
        <tr>
            <td>Active Topics</td>
            <td>${consumer.activeTopicCount}</td>
        </tr>
        </tbody>
    </table>
</div>

<div id="topics">
    <#list consumer.topics as consumerTopic>
        <#assign tableId='topic-${consumerTopic_index}-table'>
        <h2><@template.toggleLink target="#${tableId}" anchor='${tableId}' /> Topic: <a
                    href="/topic/${consumerTopic.topic}">${consumerTopic.topic}</a></h2>
        <div id="${tableId}">
            <p>
            <table class="table table-bordered overview">
                <tbody>
                <tr>
                    <td>Total Threads</td>
                    <td>${consumerTopic.ownerCount}</td>
                </tr>
                <tr>
                    <td>Partition Coverage</td>
                    <td>${consumerTopic.coveragePercent * 100.0}%
                        (${consumerTopic.assignedPartitionCount} of ${consumerTopic.partitions?size})
                    </td>
                </tr>
                <tr>
                    <td>Total Lag</td>
                    <td>${consumerTopic.lag}</td>
                </tr>
                <tr>
                    <td>Max Lag</td>
                    <td>${consumerTopic.maxLag}</td>
                </tr>
                </tbody>
            </table>
            </p>
            <p>
            <table class="table table-bordered table-condensed">
                <thead>
                <tr>
                    <th>Partition</th>
                    <th>First Offset</th>
                    <th>Last Offset</th>
                    <th>Offset</th>
                    <th>Lag</th>
                    <th>Owner</th>
                </tr>
                </thead>
                <tbody>
                <#list consumerTopic.partitions as p>
                    <tr>
                        <td>${p.partitionId}</td>
                        <td>${p.firstOffset}</td>
                        <td>${p.size}</td>
                        <td>${p.offset}</td>
                        <td>${p.lag}</td>
                        <td>${p.owner!''}</td>
                    </tr>
                </#list>
                </tbody>
            </table>
            </p>
        </div>
    </#list>

</div>

<@template.footer/>
