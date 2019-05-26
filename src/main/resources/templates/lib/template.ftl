<#--
 Copyright 2016 HomeAdvisor, Inc.

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
<#macro header title>
    <!DOCTYPE html>
<html>
<head>
    <title>Kafdrop: ${title}</title>
    <link type="text/css" rel="stylesheet" href="/css/bootstrap.min.css"/>
    <link type="text/css" rel="stylesheet" href="/css/font-awesome.min.css"/>
    <link type="text/css" rel="stylesheet" href="/css/global.css"/>

    <script src="/js/jquery.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <script src="/js/global.js"></script>

    <#nested>
</head>
<body>
<#include "../includes/header.ftl">
<div class="container l-container">
    </#macro>

    <#macro footer>
</div>
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
