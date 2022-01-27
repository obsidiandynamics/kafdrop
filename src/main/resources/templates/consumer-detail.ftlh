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
<@template.header "Consumer: ${consumer.groupId}"/>

<#setting number_format="0">

<h2>Kafka Consumer: ${consumer.groupId}</h2>

<div class="container-fluid pl-0">
    <div id="overview">
        <h3>Overview</h3>
        <table class="table table-bordered overview">
            <tbody>
            <tr>
                <td>Topics</td>
                <td>${consumer.topics?size}</td>
            </tr>
            </tbody>
        </table>
    </div>

    <div id="topics">
        <#list consumer.topics as consumerTopic>
            <#assign tableId='topic-${consumerTopic_index}-table'>
            <h3><@template.toggleLink target="#${tableId}" anchor='${tableId}' /> Topic: <a
                        href="<@spring.url '/topic/${consumerTopic.topic}'/>">${consumerTopic.topic}</a></h3>
            <div id="${tableId}">
                <p>
                <table class="table table-bordered table-sm">
                    <thead>
                    <tr>
                        <th>Partition</th>
                        <th>First Offset</th>
                        <th>Last Offset</th>
                        <th>Consumer Offset</th>
                        <th>Lag</th>
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
                        </tr>
                    </#list>
                    <tr>
                        <td colspan="4"><b>Combined lag</b></td>
                        <td><b>${consumerTopic.lag}</b></td>
                    </tr>
                    </tbody>
                </table>
                </p>
            </div>
        </#list>
    </div>
</div>

<@template.footer/>
