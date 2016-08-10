<#import "lib/template.ftl" as template>
<@template.header "Broker List"/>

<#setting number_format="0">
    <div>
        <h2>Kafka Cluster Info</h2>

        <div id="zookeeper">
            <h3>Zookeeper Connection</h3>
            <ul>
            <#list zookeeper.connectList as z>
               <li>${z}</li>
            </#list>
            </ul>
        </div>

        <div id="brokers">
            <h3>Brokers</h3>
            <table class="bs-table default">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Host</th>
                    <th>Port</th>
                    <th>JMX Port</th>
                    <th>Version</th>
                    <th>Start Time</th>
                    <th>Controller?</th>
                </tr>
                </thead>
                <tbody>
                <#if brokers?size == 0>
                    <tr>
                        <td class="error" colspan="7">No brokers available!</td>
                    </tr>
                </#if>
                <#list brokers as b>
                <tr>
                    <td><a href="/broker/${b.id}"><i class="fa fa-info-circle fa-lg"></i> ${b.id}</a></td>
                    <td>${b.host}</td>
                    <td>${b.port?string}</td>
                    <td>${b.jmxPort?string}</td>
                    <td>${b.version}</td>
                    <td>${b.timestamp?string["yyyy-MM-dd HH:mm:ss.SSSZ"]}</td>
                    <td><@template.yn b.controller/></td>
                </tr>
                </#list>
                </tbody>
            </table>
        </div>

        <div id="topics">
            <h3>Topics</h3>
            <table class="bs-table default">
                <thead>
                <tr>
                    <th>Name</th>
                    <th>Partitions</th>
                    <th>% Preferred</th>
                    <th># Under Replicated</th>
                    <th>Custom Config?</th>
                    <#--<th>Consumers</th>-->
                </tr>
                </thead>
                <tbody>
                <#if topics?size == 0>
                <tr>
                    <td colspan="5">No topics available</td>
                </tr>
                </#if>
                <#list topics as t>
                <tr>
                    <td><a href="/topic/${t.name}">${t.name}</a></td>
                    <td>${t.partitions?size}</td>
                    <td <#if t.preferredReplicaPercent lt 1.0>class="warn"</#if>>${t.preferredReplicaPercent?string.percent}</td>
                    <td <#if t.underReplicatedPartitions?size gt 0>class="warn"</#if>>${t.underReplicatedPartitions?size}</td>
                    <td><@template.yn t.config?size gt 0/></td>
                    <#--<td>${t.consumers![]?size}</td>-->
                </tr>
                </#list>
                </tbody>
            </table>
        </div>
    </div>

<@template.footer/>