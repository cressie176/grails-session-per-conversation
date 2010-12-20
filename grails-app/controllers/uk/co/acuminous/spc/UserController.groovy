/*
    This file is part of the grails session-per-conversation plugin.

    session-per-conversation is free software: you can redistribute it and/or modify
    it under the terms of the Lesser GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    session-per-conversation is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    Lesser GNU General Public License for more details.

    You should have received a copy of the Lesser GNU General Public License
    along with AppStatus.  If not, see <http://www.gnu.org/licenses/>.
*/

package uk.co.acuminous.spc

import static uk.co.acuminous.spc.Propagation.*

class UserController {

    @Conversational(propagation=NEVER)    
    def index = {
        render(view: 'index', model:[users: User.list()])
    }

    @Conversational(propagation=REQUIRES_NEW)
    def create = {
        User user = new User()
        user.save(validate:false)
        forward(action: 'edit', params: [id: user.id, conversationId: conversation.id])
    }

    @Conversational
    def edit = {
        render(view: 'edit', model:[user: User.get(params.id)])        
    }

    @Conversational(propagation=NEVER)        
    def delete = {
        User.get(params.id).delete(flush:true)
        render(template: 'userList', model: [users: User.list()])
    }

    @Conversational(propagation=MANDATORY)
    def showTab = {
        render(template: "${params.tab}Tab", model:[user: User.get(params.id)])
    }

    @Conversational(propagation=MANDATORY)
    def onSwitchTab = {
        updateUser()
        render("OK")
    }    

    @Conversational(propagation=MANDATORY)
    def save = {
        updateUser()
        saveConversation()
        forward(action: 'showTab', params: [tab: params.tab, id: params.id, conversationId: conversation.id])
    }

    @Conversational(propagation=MANDATORY)
    def cancel = {
        cancelConversation()
        redirectToIndexPage()
    }

    @Conversational(propagation=MANDATORY)
    def submit = {
        if (updateUser(true)) {
            endConversation()
            redirectToIndexPage()
        } else {
            forward(action: 'showTab', params: [tab: params.tab, id: params.id, conversationId: conversation.id])
        }
    }

    private User updateUser(Boolean shouldValidate = false) {
        User user = User.get(params.id)
        user.properties = params
        return user.save(validate:shouldValidate)
    }

    private void redirectToIndexPage() {
        String url = g.createLink([controller: 'user', action: 'index'])
        render(view: '/common/redirect', model: [url: url])
    }

    @Conversational()
    def attributeTest = {
        if (params.store) {
            conversationScope.foo = params.store
            render "<div id='conversationId'>${conversation.id}</div>"
        } else {
            render(conversationScope.foo)            
        }
    }
}
