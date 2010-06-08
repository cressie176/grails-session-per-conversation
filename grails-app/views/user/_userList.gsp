<g:form name="userList" action="create">
    <table id="userList" cellpadding="0" cellspacing="0">
        <thead>
            <tr>
                <th>id</th>
                <th>username</th>
                <th>&nbsp;</th>                
            </tr>
        </thead>
        <tbody>
            <g:each var="user" in="${users}" status="status">
                <tr class="${status % 2 == 0 ? 'even' : 'odd'}">
                    <td class="id"><g:link action="edit" id="$user.id">${status + 1}</g:link></td>
                    <td class="username">${user.username?.encodeAsHTML()}</td>
                    <td class="delete"><g:submitToRemote action="delete" id="$user.id" value="X" update="indexPage"/></td>
                </tr>
            </g:each>
        </tbody>
        <tbody>
            <tr class="buttons">
                <td colspan="3">
                    <g:submitButton name="create" value="create" />
                </td>
            </tr>        
        </tbody>
    </table>
</g:form>