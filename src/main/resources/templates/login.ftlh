<#import "/spring.ftl" as spring />
<#import "lib/template.ftlh" as template>
<@template.header "Enter Login Information"/>

<div class="container ">
    <br/><br/><br/>
    <div class="row ">
        <div class="col-md"><span></span></div>
        <div class="col-md"><span></span></div>
        <div class="col-md">
            <form method="post" action="<@spring.url '/login'/>">
                <h3 class="form-signin-heading">Please sign in</h3><br/>
                <p>
                    <label for="username">User Name:</label><br/>
                    <input type="text" id="username" name="username" placeholder="Username" required>
                </p>
                <p>
                    <label for="password">Password:</label><br/>
                    <input type="password" id="password" name="password" placeholder="Password" required>
                </p>
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <button type="submit">Sign in</button>
            </form>
        </div>
        <div class="col-md"><span></span></div>
        <div class="col-md"><span></span></div>
    </div>
</div>

<@template.footer/>
