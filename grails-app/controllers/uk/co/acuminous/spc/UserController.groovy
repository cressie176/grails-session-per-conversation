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

class UserController {

    def index = {
        render(view: 'index', model:[users: User.list()])
    }

    @Conversational
    def create = {
        User user = new User()
        user.save(validate:false)

        forward(action: 'edit', params: [id: user.id])
    }

    @Conversational
    def edit = {
        render(view: 'edit', model:[user: User.get(params.id)])
    }

    def delete = {
        User.get(params.id).delete(flush:true)
        render(template: 'userList', model: [users: User.list()])
    }

    @Conversational
    def showTab = {
        render(template: "${params.tab}Tab", model:[user: User.get(params.id)])
    }

    @Conversational
    def onSwitchTab = {
        User user = User.get(params.id)
        user.properties = params
        assert user.save(validate:false), user.errors
        render("OK")
    }    

    @Conversational
    def save = {
        User user = User.get(params.id)
        user.properties = params
        assert user.save(validate:false), user.errors


        commitConversation()
        forward(action: 'showTab')
    }

    @Conversational
    def cancel = {
        cancelConversation()
        String url = g.createLink([controller: 'user', action: 'index'])
        render(view: '/common/redirect', model: [url: url])
    }

    @Conversational
    def submit = {
        User user = User.get(params.id)
        user.properties = params
        if (user.save(flush:true)) {
            commitConversation()            
            String url = g.createLink([controller: 'user', action: 'index'])
            render(view: '/common/redirect', model: [url: url])
        } else {
            forward(action: 'showTab')
        }
    }

    @Conversational
    def attributeTest = {
        if (params.store) {
            conversation.foo = params.store
            render "OK"
        } else {
            render(conversation.foo)            
        }
    }
}
