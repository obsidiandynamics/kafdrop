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
<@template.header "Broker: ${broker.id?string}">
    <style type="text/css">
        .bs-table.overview td {
            white-space: nowrap;
        }

        td.leader-partitions {
            word-break: break-all;
        }

    </style>
</@template.header>

<#setting number_format="0">

<h1>Broker ID: ${broker.id}</h1>

<div id="topic-overview">
    <h2>Broker Overview</h2>

    <table class="table table-bordered overview">
        <tbody>
        <tr>
            <td><i class="fa fa-laptop"></i> Host</td>
            <td>${broker.host}:${broker.port}</td>
        </tr>
        <tr>
            <td><i class="fa fa-clock-o"></i> Start time</td>
            <td>${broker.timestamp?string["yyyy-MM-dd HH:mm:ss.SSSZ"]}</td>
        </tr>
        <tr>
            <td>Controller</td>
            <td><@template.yn broker.controller/></td>
        </tr>
        <tr>
            <td># of topics</td>
            <td>${topics?size}</td>
        </tr>

        <#assign partitionCount=0>
        <#list topics as t>
            <#assign partitionCount=partitionCount+(t.getLeaderPartitions(broker.id)?size)>
        </#list>
        <tr>
            <td># of partitions</td>
            <td>${partitionCount}</td>
        </tr>
        </tbody>
    </table>
</div>

<div>
    <h2>Topic Detail</h2>

    <table class="table table-bordered">
        <thead>
        <tr>
            <th>Topic</th>
            <th>Total Partitions</th>
            <th>Broker Partitions</th>
            <th>Partition IDs</th>
        </tr>
        </thead>
        <tbody>
        <#list topics as t>
            <tr>
                <td><a href="/topic/${t.name}">${t.name}</a></td>
                <td>${t.partitions?size}</td>
                <td>${t.getLeaderPartitions(broker.id)?size}</td>
                <td class="leader-partitions"><#list t.getLeaderPartitions(broker.id) as p>${p.id}<#sep>,</#list></td>
            </tr>
        </#list>
        </tbody>
    </table>
</div>

<@template.footer/>
