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

import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import org.hibernate.SessionFactory

class ConversationManager {
	Map<String, Conversation> conversations = [:]
    SessionFactory sessionFactory

    public synchronized Conversation start() {
        Conversation conversation = create(UUID.randomUUID().toString())
        conversation.init()
        return conversation
    }    

    public synchronized Conversation resume(String id) {
        Conversation conversation = conversations[id] ?: create(id)
        conversation.init()
        return conversation
    }

    private Conversation create(String id) {
        Conversation conversation = new Conversation(id: id, sessionFactory: sessionFactory)
        conversations[conversation.id] = conversation
        return conversation
    }

    public synchronized Conversation getConversation(String id) {
        return conversations[id]
    }

    public synchronized commit(String id) {
        setConversationState(id, ConversationState.PENDING_COMMIT)
    }

    public synchronized cancel(String id) {
        setConversationState(id, ConversationState.PENDING_CANCEL)
    }

    private void setConversationState(String id, ConversationState state) {
        Conversation conversation = conversations[id]
        if (conversation.state != ConversationState.ACTIVE && conversation.state != state) {
            throw new IllegalArgumentException('Conversation has already been set to cancel or commit')
        }
        conversation.state = state
    }

    public synchronized void close(String id) {
        if (conversations.containsKey(id)) {
            Conversation conversation = conversations[id]
            conversation.close()
            if (conversation.state == ConversationState.COMMITTED || conversation.state == ConversationState.CANCELLED) {
                conversations.remove(id)
            }
        }
    }
}
