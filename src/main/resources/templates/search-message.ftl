<#ftl output_format="HTML">
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
<@template.header "Topic: ${topic.name}: Search messages">
    <style type="text/css">
        h2 {
            margin-bottom: 16px;
        }

        #searchMessageFormPanel {
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
	    }
	    else
	    {
	        $('#protobufDescriptor').show();
	        $('#protobufMsgType').show();
	        $('#descFile').prop('required',true);
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


<h2>Search messages in: <a href="<@spring.url '/topic/${topic.name}'/>">${topic.name}</a></h2>

<#assign selectedFormat=searchMessageForm.format!defaultFormat>
<#assign selectedKeyFormat=searchMessageForm.keyFormat!defaultFormat>

<div id="searchMessageFormPanel" class="card">
    <form method="GET" action="<@spring.url '/topic/${topic.name}/search-messages'/>" id="searchMessageForm" class="form-inline card-body">
        <div class="form-row">
        <@spring.bind path="searchMessageForm.searchText"/>
        <div class="form-group ${spring.status.error?string("has-error", "")}">
            <label class="control-label" for="searchText">Search text</label>
            <@spring.formInput path="searchMessageForm.searchText" attributes='class="form-control" size="10"'/>
            <#if spring.status.error>
                <span class="text-danger"><i class="fa fa-times-circle"></i><@spring.showErrors "<br/>"/></span>
            </#if>
        </div>
        &nbsp;&nbsp;
        <div class="form-group">
            <label class="control-label" for="keyFormat">Key format</label>
            <select class="form-control" id="keyFormat" name="keyFormat">
                <#list keyFormats as kf>
                    <option value="${kf}" <#if kf == selectedKeyFormat>selected="selected"</#if>>${kf}</option>
                </#list>
            </select>
        </div>
        &nbsp;&nbsp;
        <div class="form-group">
            <label class="control-label" for="format">Message format</label>
            <select class="form-control" id="format" name="format">
                <#list messageFormats as f>
                    <option value="${f}" <#if f == selectedFormat>selected="selected"</#if>>${f}</option>
                </#list>
            </select>
        </div>
        &nbsp;&nbsp;
        <div class="form-group" id="protobufDescriptor">
        <#if descFiles?size != 0>
          <label class="control-label" for="descFile">Protobuf descriptor</label>
	        <select class="form-control" id="descFile" name="descFile">
	          <#list descFiles as f>
	           <option value="${f}">${f}</option>
	          </#list>
	        </select>
        <#else>
          <span class="errorMessage">No available descriptor, please check.</span>
        </#if>
        </div>
        &nbsp;&nbsp;
        <div class="form-group" id="protobufMsgType">
            <#if descFiles?size != 0>
              <label class="control-label" for="msgTypeName">Protobuf message type name</label>
              <@spring.formInput path="searchMessageForm.msgTypeName" attributes='class="form-control"'/>
            </#if>
        </div>
        &nbsp;&nbsp;
        </div>
        <button id="searchMessagesBtn" class="btn btn-success" type="submit"><i class="fa fa-search"></i> Search Messages</button>
    </form>
</div>

<@spring.bind path="searchMessageForm.*"/>
<div id="message-display" class="container">
    <#if messages?? && messages?size gt 0>
        <div class="card text-right bg-transparent border-0">
            <div class="card-body">
                <span class="badge badge-primary">${messages?size}</span> messages found.
            </div>
        </div>
        <#list messages as msg>
            <div class="message-detail">
                <span class="badge badge-light">Item:</span> ${msg?index + 1} &nbsp;
                <span class="badge badge-light">Partition:</span> ${msg.partition} &nbsp;
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
    <#elseif !(spring.status.error) && !(searchMessageForm.empty)>
        No messages found containing ${(searchMessageForm.searchText)!"No search text"}
    </#if>
</div>

<@template.footer/>