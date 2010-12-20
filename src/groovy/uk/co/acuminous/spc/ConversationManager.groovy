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

import static uk.co.acuminous.spc.ConversationState.*
import org.hibernate.SessionFactory
import org.apache.log4j.Logger

class ConversationManager {

    static Logger log = Logger.getLogger(ConversationManager)

	Map<String, Conversation> conversations = [:]
    SessionFactory sessionFactory

    public synchronized Conversation start() {
        log.trace('Starting conversation')
        Conversation conversation = new Conversation(id: UUID.randomUUID().toString(), sessionFactory: sessionFactory)
        conversations[conversation.id] = conversation        
        return conversation.init()
    }    

    public synchronized Conversation resume(String id) {
        log.trace("Resuming conversation ${id}")

        Conversation conversation = conversations[id]
        if (conversation?.canBeResumed()) {
            return conversation.init()
        } else if (conversation == null) {
            throw new ConversationNotFoundException("Cannot resume conversation $id because it does not exist")
        } else {
            throw new ConversationException("Cannot resume conversation $id with state ${conversation.state}")
        }
    }

    public synchronized Conversation startOrResume(String id) {
        return conversations[id] != null ? resume(id) : start()
    }
    
    public synchronized Conversation resumeIfPossible(String id) {
        Conversation conversation = conversations[id]
        if (conversation?.canBeResumed()) {
            return conversation.init()
        } else {
            return null
        }
    }

    public synchronized Conversation getConversation(String id) {
        return conversations[id]
    }

    public synchronized save(String id) {
        getConversation(id).save()
    }    

    public synchronized end(String id) {
        getConversation(id).end()
    }

    public synchronized cancel(String id) {
        getConversation(id).cancel()
    }

    public synchronized void close(String id) {
        if (conversations.containsKey(id)) {
            Conversation conversation = conversations[id]
            conversation.close()
            if (conversation.state in [ENDED, CANCELLED]) {
                conversations.remove(id)
            }
        }
    }
}
