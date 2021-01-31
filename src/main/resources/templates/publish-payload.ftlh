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
<#import "lib/template.ftlh" as template>
<@template.header "Broker:local host">
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

<h2>Publish payload</h2>

<div id="topic-overview">
    <div id="create-form">
        <form action="<@spring.url '/publish'/>" method="POST">
            <table class="table table-bordered" style="width: 40%; margin-top: 20px">
                <tbody>
                <tr>
                    <td>Topic Name</td>
                    <#--<td align="center"><input type="text" name="name" required></td>-->
                    <td align="center">
                        <select  name="name" class="custom-select col-md-3" id="inputGroupSelect06">
                            <#list topicNames! as topic>
                                <option value="${topic}">${topic}</option>
                            </#list>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td>Payload</td>
                    <td align="center">
                        <textarea  name="payload" rows="6" cols="120" required>{}
                        </textarea>
                    </td>
                </tr>
                </tbody>
            </table>

            <button class="btn btn-success" type="submit">
                <i class="fa fa-plus"></i> Publish
            </button>
            <br>
            <br>
        </form>
    </div>
    <#if errorMessage??>
        <p>Error in publishing on topic ${topicName}: ${errorMessage}</p>
    <#elseif published??>
        <p>Successfully published on topic  <a href="<@spring.url '/topic/${topicName}'/>">${topicName}</a> </p>
    </#if>
</div>

<#--<div>
    <h3>Topic Detail</h3>

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
                <td><a href="<@spring.url '/topic/${t.name}'/>">${t.name}</a></td>
                <td>${t.partitions?size}</td>
                <td>${t.getLeaderPartitions(broker.id)?size}</td>
                <td class="leader-partitions"><#list t.getLeaderPartitions(broker.id) as p>${p.id}<#sep>,</#list></td>
            </tr>
        </#list>
        </tbody>
    </table>
</div>-->

<@template.footer/>
