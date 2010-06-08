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

import org.hibernate.Session
import org.hibernate.SessionFactory

class ConversationBuilder {

    Map fields =[:]


    def propertyMissing(String name) {
        return fields[name]
    }

    def methodMissing(String name, args) {
        fields[name] = args[0]
        return this
    }

    Conversation build() {
        Conversation conversation = new Conversation()
        conversation.depth = depth ?: 0
        conversation.state = state
        conversation.conversationalSession = conversationalSession
        conversation.defaultSession = defaultSession
        conversation.sessionFactory = sessionFactory
        return conversation
    }
}
