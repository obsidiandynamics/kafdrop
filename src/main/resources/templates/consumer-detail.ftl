<#import "lib/template.ftl" as template>
<@template.header "Consumer: ${consumer.groupId}"/>

<#setting number_format="0">

<h1>Kafka Consumer: ${consumer.groupId}</h1>

<div id="overview">
    <h2>Overview</h2>
    <table class="bs-table default overview">
        <tbody>
        <tr>
            <td>Active Instances</td>
            <td>${consumer.activeInstances?size}</td>
        </tr>
        <tr>
            <td>Topics</td>
            <td>${consumer.topics?size} (${consumer.activeTopicCount} active)</td>
        </tr>
        </tbody>
    </table>
</div>

<div id="topics">
    <#list consumer.topics as consumerTopic>

    <h2>Topic: <a href="/topic/${consumerTopic.topic}">${consumerTopic.topic}</a></h2>
        <table class="bs-table default">
            <thead>
            <tr>
                <th>Partition</th>
                <th>Size</th>
                <th>Offset</th>
                <th>Lag</th>
                <th>Owner</th>
            </tr>
            </thead>
            <tbody>
            <#list consumerTopic.partitions as p>
               <tr>
                   <td>${p.partitionId}</td>
                   <td>${p.size}</td>
                   <td>${p.offset}</td>
                   <td>${p.lag}</td>
                   <td>${p.owner!''}</td>
               </tr>
            </#list>
            </tbody>
        </table>
    </#list>

</div>

<@template.footer/>