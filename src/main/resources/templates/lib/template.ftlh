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
<#macro header title>
    <!DOCTYPE html>
<html>
<head>
    <title>Kafdrop: ${title}</title>
    <link type="text/css" rel="stylesheet" href="<@spring.url '/css/bootstrap.min.css'/>"/>
    <link type="text/css" rel="stylesheet" href="<@spring.url '/css/font-awesome.min.css'/>"/>
    <link type="text/css" rel="stylesheet" href="<@spring.url '/css/global.css'/>"/>

    <script src="<@spring.url '/js/jquery-3.5.1.slim.min.js'/>"></script>
    <script src="<@spring.url '/js/bootstrap.bundle.min.js'/>"></script>
    <script src="<@spring.url '/js/global.js'/>"></script>
    <script async defer src="<@spring.url '/js/github-buttons.js'/>"></script>

    <#nested>
</head>
<body>
<#include "../includes/header.ftlh">
<div class="container">
    </#macro>

    <#macro footer>
</div>
<#include "../includes/footer.ftlh">
<#nested>
</body>
</html>
</#macro>

<#macro toggleLink target startVisible=true anchor='#'>
    <a href="<#if !anchor?starts_with('#')>#</#if>${anchor}" class="toggle-link" data-toggle-target="${target}"><i
                class="fa <#if startVisible>fa-chevron-circle-down<#else>fa-chevron-circle-right</#if>"></i></a>
</#macro>

<#macro yn value>
    <#if value>Yes<#else>No</#if>
</#macro>
