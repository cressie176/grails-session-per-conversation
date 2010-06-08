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

package uk.co.acuminous.spc.support

import uk.co.acuminous.spc.Conversational
import uk.co.acuminous.spc.ConversationManagerFactory
import uk.co.acuminous.spc.ConversationManager

class ConversationSupport {

    AnnotationTracker annotationTracker = new AnnotationTracker([Conversational])
    ConversationManagerFactory conversationManagerFactory

    boolean requiresConversations(Class clazz) {
        annotationTracker.recordAnnotationsOnClosures(clazz)
        return isConversational(clazz)
    }

    boolean isConversational(Class clazz) {
        return annotationTracker.hasAnnotatedClosures(clazz)
    }

    boolean isConversational(Class clazz, String closureName) {
        return annotationTracker.hasAnnotation(Conversational, clazz, closureName)
    }

    ConversationManager getConversationManager() {
        return conversationManagerFactory.getConversationManager()
    }

    void makeConversational(Class clazz, Closure getConversationIdClosure) {

        if (requiresConversations(clazz)) {

            clazz.metaClass.getConversationId = getConversationIdClosure

            clazz.metaClass.commitConversation = { ->
                commitConversation(conversationId)
            }

            clazz.metaClass.commitConversation = { String id ->
                conversationManager.commit(id)
            }

            clazz.metaClass.cancelConversation = { ->
                cancelConversation(conversationId)
            }

            clazz.metaClass.cancelConversation = { String id ->
                conversationManager.cancel(id)
            }

            clazz.metaClass.getConversation = { ->
                return getConversation(conversationId)
            }

            clazz.metaClass.getConversation = { String id ->
                return conversationManager.getConversation(id).attributes
            }

            clazz.metaClass.getConversationManager = {
                return delegate.getConversationManager()
            }
        }
    }
}
