<#macro header title>
<!DOCTYPE html>
<html>
<head>
    <title>Kafdrop: ${title}</title>
    <link type="text/css" rel="stylesheet" href="/css/baseless.min.css"/>
    <link type="text/css" rel="stylesheet" href="/css/global.css"/>
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
