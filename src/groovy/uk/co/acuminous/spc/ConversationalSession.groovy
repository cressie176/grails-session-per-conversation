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

import org.hibernate.event.EventSource
import org.hibernate.classic.Session
import org.hibernate.jdbc.JDBCContext.Context
import org.hibernate.impl.SessionImpl

class ConversationalSession {
    @Delegate EventSource eventSource
    @Delegate Session session
    @Delegate Context context

    public ConversationalSession(SessionImpl sessionImpl) {
        eventSource = session = context = sessionImpl
    }

    /*
     * Grails makes entities readonly when they fail validation so that invalid data isn't flushed on session close.
     * The entity remains readonly until validation is successful or is reloaded from a different session in a
     * subsequent http request (simplified)
     *
     * Making an entity readonly has the side effect of quietly suppressing all subsequent saves made with validation off,
     * for it's lifetime within the same session, which is something your application may need to do if it supports
     * persistence draft versions of the entity.
     *
     * This is sensible in a request-per-request model, where the readonly flag will be reset between requests,
     * but not for session-per-conversation, where the flag will last for the duration of the conversation
     */
    void setReadOnly(Object entity, boolean value) {
        // suppress
    }
}
