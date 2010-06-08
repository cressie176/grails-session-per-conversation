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
import org.hibernate.SessionFactory

@WithGMock
class ConversationManagerTests extends GrailsUnitTestCase {

    ConversationManager conversationManager
    SessionFactory sessionFactory = [:] as SessionFactory

    void setUp() {
        conversationManager = new ConversationManager(sessionFactory: sessionFactory)
    }

    void testThatICanStartANewConversation() {
        Conversation mockConversation = mock(Conversation, constructor(any(Map)))
        mockConversation.id.returns('abc')
        mockConversation.init()

        play {
            assertThat conversationManager.start(), is(sameInstance(mockConversation))
        }
        assertThat conversationManager.conversations['abc'], is(sameInstance(mockConversation))
    }

    void testThatICanResumeAConversation() {
        Conversation mockConversation = mock(Conversation)
        mockConversation.init()
        
        conversationManager.conversations['abc'] = mockConversation
        play {
            assertThat conversationManager.resume('abc'), is(sameInstance(mockConversation))
        }
    }

    void testThatResumingANonExistingOrRemovedConversationStartsANewOne() {
        Conversation mockConversation = mock(Conversation, constructor(any(Map)))
        mockConversation.id.returns('abc')
        mockConversation.init()

        play {
            assertThat conversationManager.resume('abc'), is(sameInstance(mockConversation))
        }
    }

    void testThatICanCloseAConversation() {
        Conversation mockConversation = mock(Conversation)
        conversationManager.conversations['abc'] = mockConversation
        mockConversation.close()
        mockConversation.state.returns(ConversationState.SHELVED).stub()

        play {
            conversationManager.close('abc')
        }

        assertThat conversationManager.conversations['abc'], is(equalTo(mockConversation))
    }

    void testThatIRemoveCommittedConversationsConversation() {
        Conversation mockConversation = mock(Conversation)
        conversationManager.conversations['abc'] = mockConversation
        mockConversation.close()
        mockConversation.state.returns(ConversationState.COMMITTED)

        play {
            conversationManager.close('abc')
        }

        assertThat conversationManager.conversations.containsKey('abc'), is(false)
    }


    void testThatIRemoveCancelledConversationsConversation() {
        Conversation mockConversation = mock(Conversation)
        conversationManager.conversations['abc'] = mockConversation
        mockConversation.close()
        mockConversation.state.returns(ConversationState.CANCELLED).stub()

        play {
            conversationManager.close('abc')
        }

        assertThat conversationManager.conversations.containsKey('abc'), is(false)
    }

    void testThatCloseHandlesMissingConversations() {
        conversationManager.close('abc')        
    }

    void testThatICanGetAConversation() {
        Conversation conversation = new Conversation()
        conversationManager.conversations['abc'] = conversation

        assertThat conversationManager.getConversation('abc'), is(sameInstance(conversation))
    }

    void testThatICannotCommitARolledBackTransaction() {
        Conversation conversation = new Conversation(state: ConversationState.PENDING_CANCEL)
        conversationManager.conversations['abc'] = conversation

        shouldFail {
            conversationManager.setConversationState('abc', ConversationState.PENDING_COMMIT)
        }
    }

    void testThatICannotCancelACommittedTransaction() {
        Conversation conversation = new Conversation(state: ConversationState.PENDING_COMMIT)
        conversationManager.conversations['abc'] = conversation

        shouldFail {
            conversationManager.setConversationState('abc', ConversationState.PENDING_CANCEL)
        }
    }


    void testThatSettingTheConversationStateToTheSameValueIsTollerated() {
        Conversation conversation = new Conversation(state: ConversationState.PENDING_CANCEL)
        conversationManager.conversations['abc'] = conversation
        conversationManager.setConversationState('abc', ConversationState.PENDING_CANCEL)
    }
}
