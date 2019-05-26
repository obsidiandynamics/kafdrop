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
<@template.header "Topic: ${topic.name}">
    <style type="text/css">
        #action-bar {
            margin-top: 17px;
        }

        th {
            word-break: break-all;
        }
    </style>
</@template.header>

<#setting number_format="0">

<h1>Topic: ${topic.name}</h1>

<div id="action-bar" class="container">
    <a class="btn btn-default" href="/topic/${topic.name}/messages"><i class="fa fa-eye"></i> View Messages</a>
</div>

<div class="container-fluid">
    <div class="row">

        <div id="topic-overview" class="col-md-8">
            <h2>Overview</h2>

            <table class="table table-bordered">
                <tbody>
                <tr>
                    <td># of partitions</td>
                    <td>${topic.partitions?size}</td>
                </tr>
                <tr>
                    <td>Preferred replicas</td>
                    <td <#if topic.preferredReplicaPercent lt 1.0>class="warning"</#if>>${topic.preferredReplicaPercent?string.percent}</td>
                </tr>
                <tr>
                    <td>Under-replicated partitions</td>
                    <td <#if topic.underReplicatedPartitions?size gt 0>class="warning"</#if>>${topic.underReplicatedPartitions?size}</td>
                </tr>
                <tr>
                    <td>Total size</td>
                    <td>${topic.totalSize}</td>
                </tr>
                <tr>
                    <td>Total available messages</td>
                    <td>${topic.availableSize}</td>
                </tr>
                </tbody>
            </table>
        </div>


        <div id="topic-config" class="col-md-4">
            <h2>Configuration</h2>

            <#if topic.config?size == 0>
                <div>No topic specific configuration</div>
            <#else>
                <table class="table table-bordered">
                    <tbody>
                    <#list topic.config?keys as c>
                        <tr>
                            <td>${c}</td>
                            <td>${topic.config[c]}</td>
                        </tr>
                    </#list>
                    </tbody>
                </table>
            </#if>
        </div>

    </div>

    <div class="row">
        <div id="partition-detail" class="col-md-8">
            <h2>Partition Detail</h2>
            <table id="partition-detail-table" class="table table-bordered table-condensed small">
                <thead>
                <tr>
                    <th>Partition</th>
                    <th>First Offset</th>
                    <th>Last Offset</th>
                    <th>Size</th>
                    <th>Leader</th>
                    <th>Replicas</th>
                    <th>In-sync Replicas</th>
                    <th>Preferred Leader</th>
                    <th>Under-replicated</th>
                </tr>
                </thead>
                <tbody>
                <#list topic.partitions as p>
                    <tr>
                        <td>${p.id}</td>
                        <td>${p.firstOffset}</td>
                        <td>${p.size}</td>
                        <td>${p.size - p.firstOffset}</td>
                        <td <#if !(p.leader)??>class="warning"</#if>>${(p.leader.id)!"none"}</td>
                        <td><#list p.replicas as r>${r.id}<#if r_has_next>,</#if></#list></td>
                        <td><#list p.inSyncReplicas as r>${r.id}<#if r_has_next>,</#if></#list></td>
                        <td <#if !p.leaderPreferred>class="warning"</#if>><@template.yn p.leaderPreferred/></td>
                        <td <#if p.underReplicated>class="warning"</#if>><@template.yn p.underReplicated/></td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </div>

        <div id="consumers" class="col-md-4">
            <h2>Consumers</h2>
            <table id="consumers-table" class="table table-bordered table-condensed small">
                <thead>
                <tr>
                    <th>Group ID</th>
                    <th>Lag</th>
                    <th>Active Instances</th>
                </tr>
                </thead>
                <tbody>
                <#list consumers![] as c>
                    <tr>
                        <td><a href="/consumer/${c.groupId}">${c.groupId}</a></td>
                        <td>${c.getTopic(topic.name).lag}</td>
                        <td>
                            <ul class="list-unstyled">
                                <#list c.getActiveInstancesForTopic(topic.name) as i>
                                    <li>${i.id}</li>
                                </#list>
                            </ul>
                        </td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </div>
    </div>
</div>
<@template.footer/>
