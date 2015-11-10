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
                </tr>
                </thead>
                <tbody>
                <#list brokers as b>
                <tr>
                    <td>${b.id} (<a href="/broker/${b.id}">details</a>)</td>
                    <td>${b.host}</td>
                    <td>${b.port?string}</td>
                    <td>${b.jmxPort?string}</td>
                    <td>${b.version}</td>
                    <td>${b.timestamp?string["yyyy-MM-dd HH:mm:ss.SSSZ"]}</td>
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
                    <#--<th>Consumers</th>-->
                </tr>
                </thead>
                <tbody>
                <#list topics as t>
                <tr>
                    <td><a href="/topic/${t.name}">${t.name}</a></td>
                    <td>${t.partitions?size}</td>
                    <#--<td>${t.consumers![]?size}</td>-->
                </tr>
                </#list>
                </tbody>
            </table>
        </div>
    </div>

<@template.footer/>