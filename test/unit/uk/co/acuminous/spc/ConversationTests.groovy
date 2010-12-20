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
        mockTransaction.isActive().returns(false)
        mockConversationalSession.transaction.returns(mockTransaction)
        mockConversationalSession.beginTransaction()

        Conversation conversation = getPreviousConversation(mockSessionFactory, mockConversationalSession)
        play {
            conversation.init()
        }

        assertThat conversation.state, is(equalTo(ConversationState.ACTIVE))
        assertThat conversation.conversationalSession, is(mockConversationalSession)
        assertThat conversation.defaultSession, is(sameInstance(defaultSession))        
        assertThat sessionHolder.session, is(sameInstance(mockConversationalSession))
    }


    void testThatITolerateReInitialisationOfAnActiveConversation() {

        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)

        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)
        Session defaultSession = mock(Session)        

        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)
        play {
            conversation.init()
        }
    }

    void testThatICanSuspendAConversationOnClose() {
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

        assertThat conversation.state, is(equalTo(ConversationState.SUSPENDED))
        assertThat sessionHolder.session, is(sameInstance(defaultSession))
    }

    void testThatICanCheckWhetherAConversationCanBeResumed() {
        assertThat new Conversation(state: ConversationState.ACTIVE).canBeResumed(), is(true)
        assertThat new Conversation(state: ConversationState.SUSPENDED).canBeResumed(), is(true)
        assertThat new Conversation(state: ConversationState.ENDED).canBeResumed(), is(false)
        assertThat new Conversation(state: ConversationState.CANCELLED).canBeResumed(), is(false)        
    }

    void testThatICanSaveAConversation() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session firstConversationalSession = mock(ConversationalSession)
        stubTransactionSynchronizationManager(mockSessionFactory, firstConversationalSession)

        Transaction firstTransaction = mock(Transaction)
        firstConversationalSession.flushMode.set(FlushMode.AUTO)
        firstConversationalSession.flush()
        firstConversationalSession.transaction.returns(firstTransaction).times(2)

        firstTransaction.isActive().returns(true)
        firstTransaction.commit()

        firstConversationalSession.isOpen().returns(true)
        firstConversationalSession.close()

        SessionImpl mockSession = mock(SessionImpl)
        mockSessionFactory.openSession().returns(mockSession)
        ConversationalSession secondConversationalSession = mock(ConversationalSession, constructor(mockSession))
        secondConversationalSession.flushMode.set(FlushMode.MANUAL)

        Transaction secondTransaction = mock(Transaction)
        secondTransaction.isActive().returns(false)

        secondConversationalSession.transaction.returns(secondTransaction)
        secondConversationalSession.beginTransaction()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, firstConversationalSession, defaultSession)

        play {
            conversation.save()
        }

        assertThat conversation.state, is(equalTo(ConversationState.ACTIVE))
        assertThat conversation.conversationalSession, is(secondConversationalSession)
        assertThat sessionHolder.session, is(sameInstance(secondConversationalSession))
    }

    void testThatICanEndAConversation() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)
        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)

        Transaction mockTransaction = mock(Transaction)
        mockConversationalSession.flushMode.set(FlushMode.AUTO)        
        mockConversationalSession.flush()
        mockConversationalSession.transaction.returns(mockTransaction).times(2)

        mockTransaction.isActive().returns(true)        
        mockTransaction.commit()

        mockConversationalSession.isOpen().returns(true)
        mockConversationalSession.close()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)
        conversation.attributes.foo = 'bar'

        play {
            conversation.end()
        }

        assertThat conversation.state, is(equalTo(ConversationState.ENDED))
        assertThat conversation.conversationalSession, is(nullValue())
        assertThat conversation.attributes.isEmpty(), is(true)
        assertThat sessionHolder.session, is(sameInstance(defaultSession))        
    }

    void testThatICanCancelAConversation() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)
        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)
        
        Transaction mockTransaction = mock(Transaction)    
        mockConversationalSession.transaction.returns(mockTransaction).times(2)

        mockTransaction.isActive().returns(true)
        mockTransaction.rollback()

        mockConversationalSession.clear()        

        mockConversationalSession.isOpen().returns(true)
        mockConversationalSession.close()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)

        play {
            conversation.cancel()
        }

        assertThat conversation.state, is(equalTo(ConversationState.CANCELLED))
        assertThat conversation.conversationalSession, is(nullValue())
        assertThat sessionHolder.session, is(sameInstance(defaultSession))        
    }

    void testThatClosingAnActiveConversationDisconnectsOnException() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)
        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)

        Transaction mockTransaction = mock(Transaction)
        mockConversationalSession.transaction.returns(mockTransaction).times(2)
        mockTransaction.isActive().returns(true)
        mockTransaction.commit().raises(new FakeException())                

        mockConversationalSession.isConnected().returns(true)
        mockConversationalSession.disconnect()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)
        conversation.state = ConversationState.ACTIVE

        play {
            shouldFail(FakeException) {
                conversation.close()
            }
        }
    }

    void testThatClosingAnActiveConversationRestoresTheDefaultSessionOnException() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)
        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)        

        Transaction mockTransaction = mock(Transaction)
        mockConversationalSession.transaction.returns(mockTransaction).times(2)
        mockTransaction.isActive().returns(true)
        mockTransaction.commit().raises(new FakeException())        

        mockConversationalSession.isConnected().returns(true)
        mockConversationalSession.disconnect()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)
        conversation.state = ConversationState.ACTIVE

        play {
            shouldFail(FakeException) {
                conversation.close()
            }
        }
        assertThat sessionHolder.session, is(sameInstance(defaultSession))        
    }

    void testThatEndingAConversationDisconnectsOnException() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)
        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)

        Transaction mockTransaction = mock(Transaction)
        mockConversationalSession.transaction.returns(mockTransaction)
        mockTransaction.isActive().raises(new FakeException())

        mockConversationalSession.isOpen().returns(true)
        mockConversationalSession.close()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)

        play {
            shouldFail(FakeException) {
                conversation.cancel()
            }
        }
        assertThat sessionHolder.session, is(sameInstance(defaultSession))        
    }


    void testThatEndingAConversationRestoresTheDefaultSessionOnException() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)
        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)

        Transaction mockTransaction = mock(Transaction)
        mockConversationalSession.transaction.returns(mockTransaction)
        mockTransaction.isActive().raises(new FakeException())

        mockConversationalSession.isOpen().returns(true)
        mockConversationalSession.close()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)

        play {
            shouldFail(FakeException) {
                conversation.cancel()
            }
        }
    }

    void testThatCancellingAConversationDisconnectsOnException() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)
        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)

        mockConversationalSession.flushMode.set(FlushMode.AUTO).raises(new FakeException())

        mockConversationalSession.isOpen().returns(true)
        mockConversationalSession.close()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)

        play {
            shouldFail(FakeException) {
                conversation.end()
            }
        }
        assertThat sessionHolder.session, is(sameInstance(defaultSession))
    }


    void testThatCancellingAConversationRestoresTheDefaultSessionOnException() {
        SessionFactory mockSessionFactory = mock(SessionFactory)
        Session mockConversationalSession = mock(ConversationalSession)
        stubTransactionSynchronizationManager(mockSessionFactory, mockConversationalSession)

        mockConversationalSession.flushMode.set(FlushMode.AUTO).raises(new FakeException())

        mockConversationalSession.isOpen().returns(true)
        mockConversationalSession.close()

        Session defaultSession = mock(Session)
        Conversation conversation = getCurrentConversation(mockSessionFactory, mockConversationalSession, defaultSession)

        play {
            shouldFail(FakeException) {
                conversation.end()
            }
        }
    }
    
    void testThatICannotCancelAnEndedConversation() {
        shouldFail AssertionError, {
            new Conversation(state: ConversationState.ENDED).cancel()
        }
    }
        
    void testThatICannotCancelASuspendedConversation() {
        shouldFail AssertionError, {
            new Conversation(state: ConversationState.SUSPENDED).cancel()
        }
    }

    void testThatITolerateCancellingACancelledConversation() {
        Conversation conversation = new Conversation(state: ConversationState.CANCELLED)
        play {
            conversation.cancel()
        }
    }

    void testThatICannotEndACancelledConversation() {
        shouldFail AssertionError, {
            new Conversation(state: ConversationState.CANCELLED).end()
        }
    }    

    void testThatICannotEndASuspendedConversation() {
        shouldFail AssertionError, {
            new Conversation(state: ConversationState.SUSPENDED).end()
        }
    }

    void testThatITolerateEndingAnEndedConversation() {
        Conversation conversation = new Conversation(state: ConversationState.ENDED)
        play {
            conversation.end()
        }
    }

    void testThatICannotSaveACancelledConversation() {
        shouldFail AssertionError, {
            new Conversation(state: ConversationState.CANCELLED).save()
        }
    }

    void testThatICannotSaveAnEndedConversation() {
        shouldFail AssertionError, {
            new Conversation(state: ConversationState.ENDED).save()
        }
    }

    void testThatICannotSaveASuspendedConversation() {
        shouldFail AssertionError, {
            new Conversation(state: ConversationState.ENDED).save()
        }
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
            .state(ConversationState.SUSPENDED)
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
