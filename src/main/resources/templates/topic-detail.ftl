<#--
 Copyright 2016 Kafdrop contributors.

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

<h2>Topic: ${topic.name}</h2>

<div id="action-bar" class="container pl-0">
  <a id="topic-messages" class="btn btn-outline-light" href="<@spring.url '/topic/${topic.name}/messages'/>"><i class="fa fa-eye"></i> View Messages</a>
</div>
<br/>
<div class="container-fluid pl-0">
    <div class="row">
        <div id="topic-overview" class="col-md-8">
            <h3>Overview</h3>

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
            <h3>Configuration</h3>

            <#if topic.config?size == 0>
                <div>No topic-specific configuration</div>
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
            <h3>Partition Detail</h3>
            <table id="partition-detail-table" class="table table-bordered table-sm small">
                <thead>
                <tr>
                    <th>Partition</th>
                    <th>First<br>Offset</th>
                    <th>Last<br>Offset</th>
                    <th>Size</th>
                    <th>Leader<br>Node</th>
                    <th>Replica<br>Nodes</th>
                    <th>In-sync<br>Replica<br>Nodes</th>
                    <th>Offline<br>Replica<br>Nodes</th>
                    <th>Preferred<br>Leader</th>
                    <th>Under-replicated</th>
                </tr>
                </thead>
                <tbody>
                <#list topic.partitions as p>
                    <tr>
                        <td><a href="<@spring.url '/topic/${topic.name}/messages?partition=${p.id}&offset=${p.firstOffset}&count=100'/>">${p.id}</a></td>
                        <td>${p.firstOffset}</td>
                        <td>${p.size}</td>
                        <td>${p.size - p.firstOffset}</td>
                        <td <#if !(p.leader)??>class="warning"</#if>>${(p.leader.id)!"none"}</td>
                        <td><#list p.replicas as r>${r.id}<#if r_has_next>,</#if></#list></td>
                        <td><#list p.inSyncReplicas as r>${r.id}<#if r_has_next>,</#if></#list></td>
                        <td><#list p.offlineReplicas as r>${r.id}<#if r_has_next>,</#if></#list></td>
                        <td <#if !p.leaderPreferred>class="warning"</#if>><@template.yn p.leaderPreferred/></td>
                        <td <#if p.underReplicated>class="warning"</#if>><@template.yn p.underReplicated/></td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </div>

        <div id="consumers" class="col-md-4">
            <h3>Consumers</h3>
            <table id="consumers-table" class="table table-bordered table-sm small">
                <thead>
                <tr>
                    <th>Group ID</th>
                    <th>Combined Lag</th>
                </tr>
                </thead>
                <tbody>
                <#list consumers![] as c>
                    <tr>
                        <td><a href="<@spring.url '/consumer/${c.groupId}'/>">${c.groupId}</a></td>
                        <td>${c.getTopic(topic.name).lag}</td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </div>
    </div>
</div>
<@template.footer/>
