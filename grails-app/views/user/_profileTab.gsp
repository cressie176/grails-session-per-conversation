<g:render template="/common/errors" model="[bean: user]" />
<g:form name="profileForm" action="onSwitchTab" id="${user.id}">
    <g:hiddenField name="tab" value="profile" />
    <g:hiddenField name="conversationId" value="${conversation.id}" />    
    <div><label for="firstName">first name</label><g:textField name="firstName" value="${fieldValue(bean:user,field:'firstName')}"/></div>
    <div><label for="lastName">last name</label><g:textField name="lastName" value="${fieldValue(bean:user,field:'lastName')}"/></div>
    <div><label for="location">location</label><g:textField name="location" value="${fieldValue(bean:user,field:'location')}"/></div>
    <div><label for="bio">bio</label><g:textField name="bio" value="${fieldValue(bean:user,field:'bio')}"/></div>
    <g:render template="buttons" model="[id: user.id, tabContainer: 'Profile_Tab']" />
</g:form>