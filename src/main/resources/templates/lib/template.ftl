<#macro header title>
<!DOCTYPE html>
<html>
<head>
    <title>Kafdrop: ${title}</title>
    <link type="text/css" rel="stylesheet" href="/css/baseless.min.css"/>
    <link type="text/css" rel="stylesheet" href="/css/global.css"/>

    <script src="/js/jquery.min.js"></script>
    <script src="/js/global.js"></script>

    <#nested>
</head>
<body>
   <#include "/includes/header.ftl">
<div class="container">
</#macro>

<#macro footer>
</div>
   <#nested>
</body>
</html>
</#macro>

<#macro toggleLink target startVisible=true anchor='#'>
[<a href="<#if !anchor?starts_with('#')>#</#if>${anchor}" class="toggle-link" data-toggle-target="${target}"><#if startVisible>-<#else>+</#if></a>]
</#macro>