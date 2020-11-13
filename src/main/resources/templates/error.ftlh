<#import "/spring.ftl" as spring />
<#import "lib/template.ftlh" as template>
<@template.header "${error.error}"/>
<h2>${error.error}</h2>
<div id="errorHeader">
A ${error.status} error has occurred: <span id="errorMessage">${error.message}</span>
</div>
<#if error.trace??>
    <div id="stackTraceTitle">Stack trace</div>
    <div id="stackTrace">
    ${error.trace}
    </div>
<#else>
</#if>
<@template.footer/>
