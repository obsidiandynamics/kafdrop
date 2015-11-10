<#import "lib/template.ftl" as template>
<@template.header "Topic: ${topic.name}"/>

<#setting number_format="0">

<h1>Kafka Topic: ${topic.name}</h1>

<div id="topic-overview">
    <h2>Topic Overview</h2>

    <table class="bs-table default overview">
        <tbody>
        <tr>
            <td># of Partitions</td>
            <td>${topic.partitions?size}</td>
        </tr>
        <tr>
            <td>Preferred Replicas</td>
            <td>${topic.preferredReplicaPercent?string.percent}</td>
        </tr>
        </tbody>
    </table>
</div>

<div>
    <h2>Topic Configuration</h2>

    <#if topic.config?size == 0>
    <div>No topic specific configuration</div>
    <#else>
    <table class="bs-table default">
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

<div id="partition-detail">
    <h2>Partition Detail</h2>
    <table class="bs-table default">
        <thead>
        <tr>
            <th>Partition</th>
            <th>Leader</th>
            <th>Replicas</th>
            <th>In Sync Replicas</th>
            <th>Preferred Leader?</th>
            <th>Under Replicated?</th>
        </tr>
        </thead>
        <tbody>
        <#list topic.partitions as p>
        <tr>
            <td>${p.id}</td>
            <td>${p.leader.id}</td>
            <td><#list p.replicas as r>${r.id}<#if r_has_next>,</#if></#list></td>
            <td><#list p.inSyncReplicas as r>${r.id}<#if r_has_next>,</#if></#list></td>
            <td <#if !p.leaderPreferred>class="warn"</#if>><#if p.leaderPreferred>Yes<#else>No</#if></td>
            <td></td>
        </tr>
        </#list>
        </tbody>
    </table>
</div>

<div id="consumers">
    <h2>Consumers</h2>
    <table class="bs-table default">
        <thead>
        <tr>
            <th>Group Id</th>
            <th>Lag</th>
            <th>Active Instances</th>
        </tr>
        </thead>
        <tbody>
        <#list consumers as c>
            <tr>
                <td><a href="/consumer/${c.groupId}">${c.groupId}</a></td>
                <td>${c.getTopic(topic.name).lag}</td>
                <td>
                    <ul>
                       <#list c.activeInstances as i>
                           <li>${i}</li>
                       </#list>
                    </ul>
                </td>
            </tr>
        </#list>
        </tbody>
    </table>
</div>

<@template.footer/>