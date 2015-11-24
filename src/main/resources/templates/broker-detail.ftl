<#import "lib/template.ftl" as template>
<@template.header "Broker: ${broker.id?string}">
<style type="text/css">
    .bs-table.overview td { white-space: nowrap; }
</style>
</@template.header>

<#setting number_format="0">

<h1>Broker Id: ${broker.id}</h1>

<div id="topic-overview">
    <h2>Broker Overview</h2>

    <table class="bs-table default overview">
        <tbody>
        <tr>
            <td>Host</td>
            <td>${broker.host}:${broker.port}</td>
        </tr>
        <tr>
            <td>Start Time</td>
            <td>${broker.timestamp?string["yyyy-MM-dd HH:mm:ss.SSSZ"]}</td>
        </tr>
        <tr>
            <td># of Topics</td>
            <td>${topics?size}</td>
        </tr>

        <#assign partitionCount=0>
        <#list topics as t>
            <#assign partitionCount=partitionCount+(t.getLeaderPartitions(broker.id)?size)>
        </#list>
        <tr>
            <td># of Partitions</td>
            <td>${partitionCount}</td>
        </tr>
        </tbody>
    </table>
</div>

<div>
    <h2>Topic Detail</h2>

    <table class="bs-table default">
        <thead>
        <tr>
            <th>Topic</th>
            <th>Total Partitions</th>
            <th>Broker Partitions</th>
            <th>Partition Ids</th>
        </tr>
        </thead>
        <tbody>
        <#list topics as t>
              <tr>
                  <td><a href="/topic/${t.name}">${t.name}</a></td>
                  <td>${t.partitions?size}</td>
                  <td>${t.getLeaderPartitions(broker.id)?size}</td>
                  <td><#list t.getLeaderPartitions(broker.id) as p>${p.id}<#sep>,</#list></td>
              </tr>
        </#list>
        </tbody>
    </table>
</div>

<@template.footer/>