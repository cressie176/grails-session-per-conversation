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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import org.hibernate.Session

import static uk.co.acuminous.spc.ConversationState.*
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.hibernate.FlushMode
import org.apache.log4j.Logger
import org.hibernate.SessionFactory

class Conversation {

    static Logger log = Logger.getLogger(Conversation)

    String id
    ConversationalSession conversationalSession
    Session defaultSession
    ConversationState state
    ConcurrentMap attributes = new ConcurrentHashMap()
    SessionFactory sessionFactory    


    public Conversation init() {
        if (state != ACTIVE) {
            log.debug("$token Initialising conversation")
            retainDefaultSession()
            initConversationalSession()            
            state = ACTIVE
        } else {
            log.debug("$token already initialised")            
        }
        return this
    }

    public void close() {
        if (state == ACTIVE) {
            log.debug("$token Closing conversation")
            try {
                suspend()
            } finally {
                disconnectSession()
                restoreDefaultSession()
            }
        }
    }

    public boolean canBeResumed() {
        return state in [ACTIVE, SUSPENDED]
    }

    protected void save() {
        log.debug("$token Saving conversation")
        assert state == ACTIVE
        flushSession()
        commitTransaction()
        closeSession()
        initConversationalSession()
    }

    protected void cancel() {
        log.debug("$token Cancelling conversation")
        if (state == CANCELLED) {
            log.warn("$token Conversation has already been cancelled")
            return
        }

        assert state == ACTIVE        
        try {            
            state = CANCELLED            
            rollbackTransaction()
            clearSession() // Mostly unnecessary but just possible flush mode was set to AUTO
        } finally {
            closeSession()
            restoreDefaultSession()
        }
    }

    protected void end() {
        log.debug("$token Committing conversation")

        if (state == ENDED) {
            log.warn("$token Conversation has already ended")
            return
        }

        assert state == ACTIVE
        try {
            state = ENDED
            flushSession()
            commitTransaction()
        } finally {
            closeSession()
            restoreDefaultSession()
        }
    }

    protected void suspend() {
        log.debug("$token Suspending conversation")
        commitTransaction() // Trust that nothing has been flushed      
        state = SUSPENDED
    }

    private void retainDefaultSession() {
        log.debug("$token Retaining default session")
        SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
        defaultSession = sessionHolder.session
    }

    private void restoreDefaultSession() {
        log.debug("$token Restoring default session")
        TransactionSynchronizationManager.getResource(sessionFactory).addSession(defaultSession)
    }        

    private void initConversationalSession() {
		log.debug("$token Initialising conversational session")
        openSession()
        beginTransaction()
    }

	private void openSession() {
		log.debug("$token Opening hibernate session")
		if (!conversationalSession?.isOpen()) {
			conversationalSession = openConversationalSession()
			conversationalSession.flushMode = FlushMode.MANUAL
		} else {
			log.debug("$token Hibernate session was already open")
		}
        TransactionSynchronizationManager.getResource(sessionFactory).addSession(conversationalSession)        
	}

	private void flushSession() {
		log.debug("$token Flushing hibernate session")
        conversationalSession.flushMode = FlushMode.AUTO        
		conversationalSession.flush()
	}
    
	private void clearSession() {
		log.debug("$token Clearing hibernate session")
		conversationalSession.clear()
	}

	private void closeSession() {
		log.debug("$token Closing hibernate session")
        if (conversationalSession?.isOpen()) {
            conversationalSession.close()
            conversationalSession = null
            attributes.clear()
        } else {
			log.warn("$token Hibernate session was already closed")            
        }
	}

    private void disconnectSession() {
        log.debug("$token Disconnecting session")
        if (conversationalSession?.isConnected()) {
            conversationalSession.disconnect()
        } else {
            log.debug("$token Session was already disconnected")
        }
    }    

    private void beginTransaction() {
        log.debug("$token Beginning hibernate transaction")
        if (!conversationalSession.transaction?.isActive()) {
            conversationalSession.beginTransaction()
        } else {
            log.warn("$token Hibernate transaction was already active")
        }
    }

	private void rollbackTransaction() {
		log.debug("$token Rolling back Hibernate transaction")
        if (conversationalSession?.transaction?.isActive()) {
            conversationalSession.transaction.rollback()
        } else {
        	log.warn("$token No active transaction")
		}
	}

    private void commitTransaction() {
        log.debug("$token Committing Hibernate transaction")
        if (conversationalSession?.transaction?.isActive()) {
            conversationalSession.transaction.commit()
        } else {
        	log.warn("$token No active transaction")
        }
    }

	private String getToken() {
		return "[$id/${conversationalSession?.hashCode()}]"
	}

    private ConversationalSession openConversationalSession() {
        return new ConversationalSession(sessionFactory.openSession()) 
    }
}
