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
        mockConversation.init().returns(mockConversation)

        play {
            assertThat conversationManager.start(), is(sameInstance(mockConversation))
        }
        assertThat conversationManager.conversations['abc'], is(sameInstance(mockConversation))
    }
    
    void testThatICanResumeAConversation() {
        Conversation mockConversation = mock(Conversation)
        mockConversation.canBeResumed().returns(true)
        mockConversation.init().returns(mockConversation)

        conversationManager.conversations['abc'] = mockConversation
        play {
            assertThat conversationManager.resume('abc'), is(sameInstance(mockConversation))
        }
    }
    
    void testThatAttemptingToResumeAnUnresumableConversationFails() {
        Conversation mockConversation = mock(Conversation)
        mockConversation.state.returns(ConversationState.ENDED).stub()
        mockConversation.canBeResumed().returns(false)

        conversationManager.conversations['abc'] = mockConversation
        play {
            shouldFail ConversationException, {
                conversationManager.resume('abc')
            }
        }
    }

    void testThatICannotResumeANonExistingConversation() {
        shouldFail ConversationNotFoundException, {
            conversationManager.resume('abc')
        }
    }

    void testThatStartOrResumeWillResumeAnExistingConversation() {
        conversationManager.conversations['abc'] = new Conversation()

        mock(conversationManager).resume('abc')
        play {
            conversationManager.startOrResume('abc')
        }
    }
    
    void testThatStartOrResumeWillStartAConversationIfNoIdIsSpecified() {
        mock(conversationManager).start()
        play {
            conversationManager.startOrResume(null)
        }
    }

    void testThatStartOrResumeWillStartAConversationIfNoConversationIsFound() {
        mock(conversationManager).start()
        play {
            conversationManager.startOrResume('abc')
        }
    }

    void testThatResumeIfPossibleInitialisesAResumableConversation() {
        Conversation mockConversation = mock(Conversation)
        conversationManager.conversations['abc'] = mockConversation

        mockConversation.canBeResumed().returns(true)
        mockConversation.init().returns(mockConversation)
        
        play {
            assertThat conversationManager.resumeIfPossible('abc'), is(sameInstance(mockConversation))
        }
    }

    void testThatResumeIfPossibleReturnsNullIfTheConversationCannotBeResumed() {
        Conversation mockConversation = mock(Conversation)
        conversationManager.conversations['abc'] = mockConversation
        mockConversation.canBeResumed().returns(false)

        play {
            assertThat conversationManager.resumeIfPossible('abc'), is(nullValue())
        }        
    }

    void testThatResumeIfPossibleReturnsNullIfNoConversationIdIsSpecified() {
        play {
            assertThat conversationManager.resumeIfPossible(null), is(nullValue())
        }
    }

    void testThatResumeIfPossibleReturnsNullIfTheConversationIsNotFound() {
        play {
            assertThat conversationManager.resumeIfPossible('abc'), is(nullValue())
        }
    }

    void testThatICanCloseAConversation() {
        Conversation mockConversation = mock(Conversation)
        conversationManager.conversations['abc'] = mockConversation
        mockConversation.close()
        mockConversation.state.returns(ConversationState.SUSPENDED).stub()

        play {
            conversationManager.close('abc')
        }

        assertThat conversationManager.conversations['abc'], is(equalTo(mockConversation))
    }

    void testThatIRemoveEndedConversationsConversation() {
        Conversation mockConversation = mock(Conversation)
        conversationManager.conversations['abc'] = mockConversation
        mockConversation.close()
        mockConversation.state.returns(ConversationState.ENDED)

        play {
            conversationManager.close('abc')
        }

        assertThat conversationManager.conversations.containsKey('abc'), is(false)
    }


    void testThatIRemoveCancelledConversations() {
        Conversation mockConversation = mock(Conversation)
        conversationManager.conversations['abc'] = mockConversation
        mockConversation.close()
        mockConversation.state.returns(ConversationState.CANCELLED).stub()

        play {
            conversationManager.close('abc')
        }

        assertThat conversationManager.conversations.containsKey('abc'), is(false)
    }

    void testThatCloseToleratesMissingConversations() {
        conversationManager.close('abc')        
    }

    void testThatICanGetAConversation() {
        Conversation conversation = new Conversation()
        conversationManager.conversations['abc'] = conversation

        assertThat conversationManager.getConversation('abc'), is(sameInstance(conversation))
    }
}
