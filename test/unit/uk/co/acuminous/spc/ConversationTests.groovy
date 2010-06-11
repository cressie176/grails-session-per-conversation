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

import grails.test.GrailsUnitTestCase
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*
import org.gmock.WithGMock
import org.hibernate.Session
import org.hibernate.Transaction
import org.hibernate.SessionFactory
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.orm.hibernate3.SessionHolder
import org.hibernate.FlushMode
import org.hibernate.impl.SessionImpl


@WithGMock
class ConversationTests extends GrailsUnitTestCase {

    SessionHolder sessionHolder    

    void testThatICanInitialiseANewConversation() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session defaultSession = mock(Session)
        stubTransactionSynchronizationManager(mockSessionFactory, defaultSession)

        Session underlying = mock(Session)
        mockSessionFactory.openSession().returns(underlying)

        Session mockConversationalSession = mock(ConversationalSession, constructor(underlying))
        mockConversationalSession.flushMode.set(FlushMode.MANUAL)
        mockConversationalSession.transaction.returns(null)
        mockConversationalSession.beginTransaction()
        
        Conversation conversation = getNewConversation(mockSessionFactory)
        play {
            conversation.init()
        }

        assertThat conversation.depth, is(equalTo(1))
        assertThat conversation.state, is(equalTo(ConversationState.ACTIVE))
        assertThat conversation.conversationalSession, is(sameInstance(mockConversationalSession))
        assertThat conversation.defaultSession, is(sameInstance(defaultSession))
        assertThat sessionHolder.session, is(sameInstance(mockConversationalSession))
    }

    void testThatICanReinitialiseAPreviousConversation() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session defaultSession = mock(Session)
        stubTransactionSynchronizationManager(mockSessionFactory, defaultSession)

        Session mockConversationalSession = mock(ConversationalSession)
        mockConversationalSession.isOpen().returns(true)

        Transaction mockTransaction = mock(Transaction)
        mockConversationalSession.transaction.returns(mockTransaction)
        mockTransaction.isActive().returns(false)
        mockConversationalSession.beginTransaction()

        Conversation conversation = getPreviousConversation(mockSessionFactory, mockConversationalSession)
        play {
            conversation.init()
        }

        assertThat conversation.depth, is(equalTo(1))
        assertThat conversation.state, is(equalTo(ConversationState.ACTIVE))
        assertThat conversation.conversationalSession, is(mockConversationalSession)
        assertThat conversation.defaultSession, is(sameInstance(defaultSession))        
        assertThat sessionHolder.session, is(sameInstance(mockConversationalSession))
    }

    void testThatICanReinitialiseANestedConversation() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(Session)

        Conversation conversation = getNestedConversation(mockSessionFactory, mockConversationalSession)
        play {
            conversation.init()
        }

        assertThat conversation.depth, is(equalTo(3))
        assertThat conversation.state, is(equalTo(ConversationState.ACTIVE))
        assertThat conversation.conversationalSession, is(mockConversationalSession)
    }

    void testThatICanShelveAConversationOnClose() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)

        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)
        Session defaultSession = mock(Session)

        Transaction mockTransaction = mock(Transaction)
        mockConversationalSession.transaction.returns(mockTransaction).times(2)
        mockTransaction.isActive().returns(true)
        mockTransaction.commit()
               
        mockConversationalSession.isConnected().returns(true)
        mockConversationalSession.disconnect()        

        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)

        play {
            conversation.close()
        }

        assertThat conversation.state, is(equalTo(ConversationState.SHELVED))
        assertThat sessionHolder.session, is(sameInstance(defaultSession))
    }

    void testThatICanCommitAConversationOnClose() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)
        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)

        Transaction mockTransaction = mock(Transaction)
        mockConversationalSession.flush()
        mockConversationalSession.transaction.returns(mockTransaction).times(2)

        mockTransaction.isActive().returns(true)        
        mockTransaction.commit()

        mockConversationalSession.isOpen().returns(true)        
        mockConversationalSession.close()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)
        conversation.state = ConversationState.PENDING_COMMIT
        conversation.attributes.foo = 'bar'

        play {
            conversation.close()
        }

        assertThat conversation.state, is(equalTo(ConversationState.COMMITTED))
        assertThat conversation.conversationalSession, is(nullValue())
        assertThat conversation.attributes.isEmpty, is(true)
        assertThat sessionHolder.session, is(sameInstance(defaultSession))        
    }

    void testThatICanCancelAConversationOnClose() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)
        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)
        
        Transaction mockTransaction = mock(Transaction)    
        mockConversationalSession.transaction.returns(mockTransaction).times(2)

        mockTransaction.isActive().returns(true)
        mockTransaction.rollback()

        mockConversationalSession.isOpen().returns(true)
        mockConversationalSession.close()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)
        conversation.state = ConversationState.PENDING_CANCEL

        play {
            conversation.close()
        }

        assertThat conversation.state, is(equalTo(ConversationState.CANCELLED))
        assertThat conversation.conversationalSession, is(nullValue())
        assertThat sessionHolder.session, is(sameInstance(defaultSession))        
    }

    void testThatIDisconnectOnException() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)

        mockConversationalSession.flush().raises(new RuntimeException())
        mockConversationalSession.isConnected().returns(true)
        mockConversationalSession.disconnect()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)
        conversation.state = ConversationState.PENDING_COMMIT

        play {
            shouldFail(RuntimeException) {
                conversation.close()
            }
        }
    }


    void testThatIRevertToDefaultSessionOnException() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)
        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)        

        mockConversationalSession.flush().raises(new RuntimeException())
        mockConversationalSession.isConnected().returns(true)
        mockConversationalSession.disconnect()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)
        conversation.state = ConversationState.PENDING_COMMIT

        play {
            shouldFail(RuntimeException) {
                conversation.close()
            }
        }
        assertThat sessionHolder.session, is(sameInstance(defaultSession))        
    }

    void testThatClosingANestedConversationOnlyDecrementsTheDepth() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)

        Conversation conversation = getNestedConversation(mockSessionFactory, mockConversationalSession)
        conversation.state = ConversationState.PENDING_COMMIT

        play {
            conversation.close()
        }

        assertThat conversation.state, is(equalTo(ConversationState.PENDING_COMMIT))
        assertThat conversation.depth, is(equalTo(1))
    }

    private void stubTransactionSynchronizationManager(SessionFactory mockSessionFactory, Session mockSession) {
        sessionHolder = new SessionHolder(mockSession)           
        mock(TransactionSynchronizationManager).static.getResource(any(SessionFactory)).returns(sessionHolder).stub()
    }

    private Conversation getNewConversation(SessionFactory sessionFactory) {
        return new ConversationBuilder()
            .sessionFactory(sessionFactory)
            .build()
    }

    private Conversation getCurrentConversation(SessionFactory sessionFactory, Session conversationalSession, Session defaultSession) {
        return new ConversationBuilder()
            .state(ConversationState.ACTIVE)
            .sessionFactory(sessionFactory)
            .conversationalSession(conversationalSession)
            .defaultSession(defaultSession)
            .depth(1)
            .build()
    }

    private Conversation getPreviousConversation(SessionFactory sessionFactory, Session conversationalSession) {
        return new ConversationBuilder()
            .state(ConversationState.ACTIVE)
            .sessionFactory(sessionFactory)        
            .conversationalSession(conversationalSession)
            .build()
    }

    private Conversation getNestedConversation(SessionFactory sessionFactory, Session conversationalSession) {
        return new ConversationBuilder()
            .state(ConversationState.ACTIVE)
            .sessionFactory(sessionFactory)
            .conversationalSession(conversationalSession)
            .depth(2)
            .build()
    }
}
