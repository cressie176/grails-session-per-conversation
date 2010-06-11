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

import org.hibernate.SessionFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import org.hibernate.Session

import static uk.co.acuminous.spc.ConversationState.*
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.hibernate.FlushMode
import org.apache.log4j.Logger

class Conversation {

    static Logger log = Logger.getLogger(Conversation)

    String id
    SessionFactory sessionFactory
    Session conversationalSession
    Session defaultSession
    Integer depth = 0
    ConversationState state
    ConcurrentMap attributes = new ConcurrentHashMap()     


    public void init() {
        depth++
        if (isFirstInitialisation()) {
            log.debug("$token Initialising conversation")
            useConversationalSession()
            state = ACTIVE
        } else {
            log.debug("$token Nesting conversation ($depth)")            
        }
    }

    public void close() {
        if (isLastClose()) {
            log.debug("$token Closing conversation")                    
            try {
                switch (state) {
                    case PENDING_COMMIT: commit()
                        break
                    case PENDING_CANCEL: cancel()
                        break
                    default: shelve()
                        break
                }
            } finally {
                disconnect()
                useDefaultSession()
            }
        } else {
            log.debug("$token Unnesting conversation ($depth)")            
        }
        depth--
    }

    private void commit() {
        log.debug("$token Committing conversation")
        flushSession()
        commitTransaction()
        closeSession()
        state = COMMITTED
    }

    private void cancel() {
        log.debug("$token Cancelling conversation")
        rollbackTransaction()
        closeSession()
        state = CANCELLED
    }

    private void shelve() {
        log.debug("$token Shelving conversation")
        commitTransaction() // No changes because we haven't flushed       
        state = SHELVED
    }

    private void disconnect() {
        log.debug("$token Disconnecting session")        
        if (conversationalSession?.isConnected()) {
            conversationalSession.disconnect()
        } else {
            log.debug("$token Session was already disconnected")
        }
    }

    private void useDefaultSession() {
        log.debug("$token Using default session")
        SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
        sessionHolder.addSession(defaultSession)
    }
        
    private void useConversationalSession() {
        log.debug("$token Using conversational session")
        SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
        defaultSession = sessionHolder.session
        initConversationalSession()
        sessionHolder.addSession(conversationalSession)
    }

    private void initConversationalSession() {
		log.debug("$token Initialising conversational session")        
        openSession()
        beginTransaction()
    }

	private void openSession() {
		log.debug("$token Opening hibernate session")
		if (!conversationalSession?.isOpen()) {
			conversationalSession = new ConversationalSession(sessionFactory.openSession())
			conversationalSession.flushMode = FlushMode.MANUAL			
		} else {
			log.debug("$token Hibernate session was already open")
		}
	}

	private void flushSession() {
		log.debug("$token Flushing hibernate session")
		conversationalSession.flush()
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

    private boolean isFirstInitialisation() {
        return depth == 1
    }

    private boolean isLastClose() {
        return depth == 1        
    }
}
