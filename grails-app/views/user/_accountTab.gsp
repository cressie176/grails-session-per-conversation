<g:render template="/common/errors" model="[bean: user]" />
<g:form name="accountForm" action="onSwitchTab" id="${user.id}">
    <g:hiddenField name="tab" value="account" />
    <g:hiddenField name="conversationId" value="${conversation.id}" />
    <div><label for="username">username</label><g:textField name="username" value="${fieldValue(bean:user,field:'username')}"/></div>
    <div><label for="email">email</label><g:textField name="email" value="${fieldValue(bean:user,field:'email')}"/></div>
    <div><label for="password">password</label><g:passwordField name="password" value="${fieldValue(bean:user,field:'password')}"/></div>    
    <g:render template="buttons" model="[id: user.id, tabContainer: 'Account_Tab']" />
</g:form>