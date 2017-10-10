<#import "lib/template.ftl" as template>
<#import "/spring.ftl" as spring />
<@template.header "Topic: ${topic.name}: Messages">
   <style type="text/css">
       h1 { margin-bottom: 16px; }
       #messageFormPanel { margin-top: 16px; }
       #partitionSizes { margin-left: 16px; }
       .toggle-msg { float: left;}
   </style>

  <script src="/js/message-inspector.js"></script>
</@template.header>
<#setting number_format="0">


<h1 class="col threecol">Topic Messages: <a href="/topic/${topic.name}">${topic.name}</a></h1>

<#assign selectedPartition=messageForm.partition!0?number>

<div id="partitionSizes">
    <#assign curPartition=topic.getPartition(selectedPartition).get()>
    <span class="bs-label">First Offset:</span> <span id="firstOffset">${curPartition.firstOffset}</span>
    <span class="bs-label">Last Offset:</span> <span id="lastOffset">${curPartition.size}</span>
    <span class="bs-label">Size:</span> <span id="partitionSize">${curPartition.size - curPartition.firstOffset}</span>
</div>

<div id="messageFormPanel" class="bs-panel">
<form method="GET" action="/topic/${topic.name}/messages" id="messageForm" class="bs-form panel-body">

    <div class="bs-form-group inline">
        <label for="partition">Partition</label>
        <select id="partition" name="partition">
        <#list topic.partitions as p>
            <option value="${p.id}" data-first-offset="${p.firstOffset}" data-last-offset="${p.size}" <#if p.id == selectedPartition>selected="selected"</#if>>${p.id}</option>
        </#list>
        </select>

        <label for="offset">Offset</label>
        <@spring.bind path="messageForm.offset"/>
        <@spring.formInput path="messageForm.offset" attributes='class="bs-form-elem ${spring.status.error?string("error", "")}"'/>
        <@spring.showErrors separator="\n" classOrStyle="error"/>

        <label for="count">Num Messages</label>
        <@spring.bind path="messageForm.count"/>
        <@spring.formInput path="messageForm.count" attributes='class="bs-form-elem ${spring.status.error?string("error", "")}"'/>
        <span class="error"><@spring.showErrors "<br/>"/></span>

        <button class="bs-btn primary" type="submit"><i class="fa fa-search"></i> View Messages</button>

    </div>

</form>
</div>

<@spring.bind path="messageForm.*"/>
<div id="message-display">
    <#if messages?? && messages?size gt 0>
    <#list messages as msg>
        <#assign offset=messageForm.offset + msg_index>
        <div data-offset="${offset}" class="message-detail">
            <span class="bs-label">Offset:</span> ${offset}
            <span class="bs-label">Key:</span> ${msg.key!''}
            <span class="bs-label">Checksum/Computed:</span> <span <#if !msg.valid>class="error"</#if>>${msg.checksum}/${msg.computedChecksum}</span>
            <span class="bs-label">Compression:</span> ${msg.compressionCodec}
            <div>
            <a href="#" class="toggle-msg"><i class="fa fa-chevron-circle-right">&nbsp;</i></a>
            <pre class="message-body">${msg.message!''}</pre>
            </div>
        </div>
    </#list>
    <#elseif !(spring.status.error) && !(messageForm.empty)>
        No messages found in partition ${messageForm.partition} at offset ${messageForm.offset}
    </#if>
</div>
<div class="padding"></div>

<@template.footer/>
