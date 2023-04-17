<#ftl output_format="HTML">
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
<@template.header "Topic: ${topic.name}: Messages">
    <style type="text/css">
        h2 {
            margin-bottom: 16px;
        }

        #messageFormPanel, #messageNavigationPanel {
            margin-top: 16px;
        }

        #partitionSizes {
            margin-left: 16px;
        }

        .badge {
            margin-right: 5px;
        }

        .toggle-msg {
            float: left;
        }
    </style>

    <script src="<@spring.url '/js/message-inspector.js'/>"></script>
    
    <script type="text/javascript">
	$( document ).ready(updateProtobufPanel);
	function updateProtobufPanel()   
	{   	   
	   if ($("#format").val() != "PROTOBUF")
	    {
	        $('#protobufDescriptor').hide();
	        $('#protobufMsgType').hide();
            $('#protobufIsAnyProto').hide();
	    }
	    else
	    {
	        $('#protobufDescriptor').show();
	        $('#protobufMsgType').show();
	        $('#descFile').prop('required',true);
            $('#protobufIsAnyProto').show();
	    }
	}  
	$(document).ready(function(){
	    $("#format").change(function() {
	    	updateProtobufPanel();
	    });
	});
	</script>
</@template.header>
<#setting number_format="0">


<h2>Topic Messages: <a href="<@spring.url '/topic/${topic.name}'/>">${topic.name}</a></h2>

<#assign selectedPartition=messageForm.partition!0?number>
<#assign selectedFormat=messageForm.format!defaultFormat>
<#assign selectedKeyFormat=messageForm.keyFormat!defaultKeyFormat>
<#assign selectedIsAnyProtoOpt=messageForm.isAnyProto>

<div id="partitionSizes">
    <#assign curPartition=topic.getPartition(selectedPartition).get()>
    <span class="badge badge-light">First Offset:</span> <span id="firstOffset">${curPartition.firstOffset}</span>&nbsp;
    <span class="badge badge-light">Last Offset:</span> <span id="lastOffset">${curPartition.size}</span>&nbsp;
    <span class="badge badge-light">Size:</span> <span
            id="partitionSize">${curPartition.size - curPartition.firstOffset}</span>
</div>

<div id="messageFormPanel" class="card">
    <form method="GET" action="<@spring.url '/topic/${topic.name}/messages'/>" id="messageForm" class="form-inline card-body">
        <div class="form-group">
            <label for="partition">Partition</label>
            <select class="form-control" id="partition" name="partition">
                <#list topic.partitions as p>
                    <option value="${p.id}" data-first-offset="${p.firstOffset}" data-last-offset="${p.size}"
                            <#if p.id == selectedPartition>selected="selected"</#if>>${p.id}</option>
                </#list>
            </select>
        </div>
        &nbsp;&nbsp;
        <@spring.bind path="messageForm.offset"/>
        <div class="form-group ${spring.status.error?string("has-error", "")}">
            <label class="control-label" for="offset">Offset</label>
            <@spring.formInput path="messageForm.offset" attributes='class="form-control" size="10"'/>
            <#if spring.status.error>
                <span class="text-danger"><i class="fa fa-times-circle"></i><@spring.showErrors "<br/>"/></span>
            </#if>
        </div>
        &nbsp;&nbsp;
        <@spring.bind path="messageForm.count"/>
        <div class="form-group ${spring.status.error?string("has-error", "")}">
            <label class=control-label" for="count"># messages</label>
            <@spring.formInput path="messageForm.count" attributes='class="form-control ${spring.status.error?string("has-error", "")}" size="10"'/>
            <#if spring.status.error>
                <span class="text-danger"><i class="fa fa-times-circle"></i><@spring.showErrors "<br/>"/></span>
            </#if>
        </div>
        &nbsp;&nbsp;
        <div class="form-group">
            <label for="format">Key format</label>
            <select class="form-control" id="keyFormat" name="keyFormat">
                <#list keyFormats as kf>
                    <option value="${kf}" <#if kf == selectedKeyFormat>selected="selected"</#if>>${kf}</option>
                </#list>
            </select>
        </div>
        &nbsp;&nbsp;
        <div class="form-group">
            <label for="format">Message format</label>
            <select class="form-control" id="format" name="format">
                <#list messageFormats as f>
                    <option value="${f}" <#if f == selectedFormat>selected="selected"</#if>>${f}</option>
                </#list>
            </select>
        </div>
        &nbsp;&nbsp;
        <div class="form-group" id="protobufDescriptor">
        <#if descFiles?size != 0>
          <label for="format">Protobuf descriptor</label>
	        <select class="form-control" id="descFile" name="descFile">
	          <#list descFiles as f>
	           <option value="${f}" <#if f == defaultDescFile>selected="selected"</#if>>${f}</option>
	          </#list>
	        </select>
        <#else>
          <span class="errorMessage">No available descriptor, please check.</span>
        </#if>
        </div>
        &nbsp;&nbsp;
        <div class="form-group" id="protobufMsgType">
            <#if descFiles?size != 0>
              <label class=control-label" for="format">Protobuf message type name</label>
              <@spring.formInput path="messageForm.msgTypeName" attributes='class="form-control"'/>
            </#if>
        </div>
        &nbsp;&nbsp;
        <div class="form-group" id="protobufIsAnyProto">
        	<label class=control-label" for="format">Is Any proto?</label>
            <select class="form-control" id="isAnyProto" name="isAnyProto">
                 <#list isAnyProtoOpts as opt>
                    <option value="${opt?c}" <#if opt == selectedIsAnyProtoOpt>selected="selected"</#if>>${opt?string('Yes', 'No')}</option>
                </#list>
            </select>
        </div>
        &nbsp;&nbsp;
        <button id="viewMessagesBtn" class="btn btn-success" type="submit" ><i class="fa fa-search"></i> View Messages</button>
    </form>
</div>

<#if messages?? && messages?size gt 0 && (curPartition.size - curPartition.firstOffset gt messageForm.count)>
    <div id="messageNavigationPanel">
        <nav>
            <ul class="pagination justify-content-center">
                <li class="page-item <#if curPartition.firstOffset gte messageForm.offset>disabled</#if>">
                    <a class="page-link btn-success" href="<@spring.url '/topic/${topic.name}/messages?partition=${curPartition.id}&offset=${curPartition.firstOffset}&count=${messageForm.count}&keyFormat=${selectedKeyFormat}&format=${selectedFormat}'/>"
                    <#if curPartition.firstOffset gte messageForm.offset>tabindex="-1"</#if>>
                        <i class="fa fa-fast-backward"></i>
                    </a>
                </li>
                <li class="page-item <#if curPartition.firstOffset gte messageForm.offset>disabled</#if>">
                    <a class="page-link btn-success" href="<@spring.url '/topic/${topic.name}/messages?partition=${curPartition.id}&offset=${[(curPartition.firstOffset),(messageForm.offset-messageForm.count)]?max}&count=${messageForm.count}&keyFormat=${selectedKeyFormat}&format=${selectedFormat}'/>"
                    <#if curPartition.firstOffset gte messageForm.offset>tabindex="-1"</#if>>
                        <i class="fa fa-step-backward"></i>
                    </a>
                </li>
               <li class="page-item <#if messageForm.offset gte curPartition.size-messageForm.count>disabled</#if>">
                    <a class="page-link btn-success" href="<@spring.url '/topic/${topic.name}/messages?partition=${curPartition.id}&offset=${[(curPartition.size-messageForm.count),(messageForm.offset+messageForm.count)]?min}&count=${messageForm.count}&keyFormat=${selectedKeyFormat}&format=${selectedFormat}'/>"
                    <#if messageForm.offset gte curPartition.size-messageForm.count>tabindex="-1"</#if>>
                        <i class="fa fa-step-forward"></i>
                    </a>
                </li>
               <li class="page-item <#if messageForm.offset gte curPartition.size-messageForm.count>disabled</#if>">
                    <a class="page-link btn-success" href="<@spring.url '/topic/${topic.name}/messages?partition=${curPartition.id}&offset=${[(curPartition.size-messageForm.count),curPartition.firstOffset]?max}&count=${messageForm.count}&keyFormat=${selectedKeyFormat}&format=${selectedFormat}'/>"
                    <#if messageForm.offset gte curPartition.size-messageForm.count>tabindex="-1"</#if>>
                        <i class="fa fa-fast-forward"></i>
                    </a>
                </li>
        </nav>
    </div>
</#if>

<@spring.bind path="messageForm.*"/>
<div id="message-display" class="container">
    <#if messages?? && messages?size gt 0>
        <#list messages as msg>
            <div class="message-detail">
                <span class="badge badge-light">Offset:</span> ${msg.offset} &nbsp;
                <span class="badge badge-light">Key:</span> ${msg.key!''} &nbsp;
                <span class="badge badge-light">Timestamp:</span> ${msg.timestamp?string('yyyy-MM-dd HH:mm:ss.SSS')}
                <span class="badge badge-light">Headers:</span> ${msg.headersFormatted}
                <div>
                    <a href="#" class="toggle-msg"><i class="fa fa-chevron-circle-right">&nbsp;</i></a>
                    <pre class="message-body">${msg.message!''}</pre>
                </div>
            </div>
        </#list>
    <#elseif !(spring.status.error) && !(messageForm.empty)>
        No messages found in partition ${(messageForm.partition)!"PARTITION_NOT_SET"} at offset ${messageForm.offset}
    </#if>
</div>

<@template.footer/>