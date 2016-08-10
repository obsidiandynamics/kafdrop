<#import "lib/template.ftl" as template>
<@template.header "Topic: ${topic.name}">
  <style type="text/css">
      #action-bar { margin-top: 17px; }
      th { word-break: break-all; }
  </style>
</@template.header>

<#setting number_format="0">

<h1 class="col threecol">Topic: ${topic.name}</h1>

<div id="action-bar">
    <a class="bs-btn info" href="/topic/${topic.name}/messages"><i class="fa fa-eye"></i> View Messages</a>
</div>

<div class="row">

<div id="topic-overview" class="col fivecol">
    <h2>Overview</h2>

    <table class="bs-table default">
        <tbody>
        <tr>
            <td># of Partitions</td>
            <td>${topic.partitions?size}</td>
        </tr>
        <tr>
            <td>Preferred Replicas</td>
            <td <#if topic.preferredReplicaPercent lt 1.0>class="error"</#if>>${topic.preferredReplicaPercent?string.percent}</td>
        </tr>
        <tr>
            <td>Under Replicated Partitions</td>
            <td <#if topic.underReplicatedPartitions?size gt 0>class="error"</#if>>${topic.underReplicatedPartitions?size}</td>
        </tr>
        <tr>
            <td>Total Size</td>
            <td>${topic.totalSize}</td>
        </tr>
        <tr>
            <td>Total Available Messages</td>
            <td>${topic.availableSize}</td>
        </tr>
        </tbody>
    </table>
</div>


<div id="topic-config" class="col sevencol">
    <h2>Configuration</h2>

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

</div>

<div class="row">
<div id="partition-detail" class="col fivecol">
    <h2>Partition Detail</h2>
    <table id="partition-detail-table" class="bs-table small">
        <thead>
        <tr>
            <th>Partition</th>
            <th>First Offset</th>
            <th>Last Offset</th>
            <th>Size</th>
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
            <td>${p.firstOffset}</td>
            <td>${p.size}</td>
            <td>${p.size - p.firstOffset}</td>
            <td <#if !(p.leader)??>class="error"</#if>>${(p.leader.id)!"none"}</td>
            <td><#list p.replicas as r>${r.id}<#if r_has_next>,</#if></#list></td>
            <td><#list p.inSyncReplicas as r>${r.id}<#if r_has_next>,</#if></#list></td>
            <td <#if !p.leaderPreferred>class="error"</#if>><@template.yn p.leaderPreferred/></td>
            <td <#if p.underReplicated>class="error"</#if>><@template.yn p.underReplicated/></td>
        </tr>
        </#list>
        </tbody>
    </table>
</div>

<div id="consumers" class="col sevencol">
    <h2>Consumers</h2>
    <table id="consumers-table" class="bs-table small">
        <thead>
        <tr>
            <th>Group Id</th>
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
                    <ul class="bs-list flat">
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
<@template.footer/>