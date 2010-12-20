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

import grails.test.GrailsUnitTestCase

import org.gmock.WithGMock

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import javax.servlet.http.HttpServletRequest

import uk.co.acuminous.spc.UserController
import uk.co.acuminous.spc.ConversationManager
import uk.co.acuminous.spc.Conversation
import uk.co.acuminous.spc.ConversationManagerFactory
import static uk.co.acuminous.spc.Propagation.*
import uk.co.acuminous.spc.Propagation
import uk.co.acuminous.spc.ConversationException

@WithGMock
class ControllerSupportTests extends GrailsUnitTestCase {

    ControllerSupport controllerSupport
    ConversationManager mockConversationManager

    void setUp() {
        mockConversationManager = mock(ConversationManager)

        controllerSupport = new ControllerSupport()
        controllerSupport.conversationManagerFactory =
            [getConversationManager: { return mockConversationManager }] as ConversationManagerFactory
    }

    void testThatBeforeHandlesNoController() {
        HttpServletRequest mockRequest = mock(HttpServletRequest)
        GrailsParameterMap mockParams = mock(GrailsParameterMap)

        play {
            controllerSupport.before(null, null, mockRequest, mockParams)
        }
    }

    void testThatAfterHandlesNoController() {
        HttpServletRequest mockRequest = mock(HttpServletRequest)

        play {
            controllerSupport.after(null, null, mockRequest)
        }
    }

    void testThatBeforeRaisesAnErrorIfRequiresNewButAConversationExists() {
        conversation REQUIRES_NEW

        HttpServletRequest mockRequest = mock(HttpServletRequest)
        GrailsParameterMap mockParams = mock(GrailsParameterMap)

        mockRequest.requestURI.returns('/foo')
        mockParams.getAt('conversationId').returns('ABC')

        play {
            shouldFail ConversationException, {
                controllerSupport.before('userController', 'create', mockRequest, mockParams)
            }
        }        
    }

    void testThatBeforeStartsARequiresNewConversationIfNoIdExists() {
        conversation REQUIRES_NEW

        Conversation conversation = new Conversation(id: 'ABC')
        HttpServletRequest mockRequest = mock(HttpServletRequest)
        GrailsParameterMap mockParams = mock(GrailsParameterMap)

        mockRequest.requestURI.returns('/foo')
        mockRequest.conversation.set(conversation)
        mockParams.getAt('conversationId').returns(null)
        mockParams.putAt('conversationId', conversation.id)

        mockConversationManager.start().returns(conversation)

        play {
            controllerSupport.before('userController', 'create', mockRequest, mockParams)
        }
    }

    void testThatBeforeStartsARequiredConversationIfNoIdExists() {
        conversation REQUIRED

        Conversation conversation = new Conversation(id: 'ABC')
        HttpServletRequest mockRequest = mock(HttpServletRequest)
        GrailsParameterMap mockParams = mock(GrailsParameterMap)

        mockRequest.requestURI.returns('/foo')
        mockRequest.conversation.set(conversation)
        mockParams.getAt('conversationId').returns(null)
        mockParams.putAt('conversationId', conversation.id)

        mockConversationManager.startOrResume(null).returns(conversation)

        play {
            controllerSupport.before('userController', 'create', mockRequest, mockParams)
        }
    }

    void testThatBeforeResumesARequiredConversationIfIdExists() {
        conversation REQUIRED

        Conversation conversation = new Conversation(id: 'XXX')
        HttpServletRequest mockRequest = mock(HttpServletRequest)
        GrailsParameterMap mockParams = mock(GrailsParameterMap)

        mockRequest.requestURI.returns('/foo')
        mockRequest.conversation.set(conversation)
        mockParams.getAt('conversationId').returns('ABC')
        mockParams.putAt('conversationId', conversation.id)

        mockConversationManager.startOrResume('ABC').returns(conversation)

        play {
            controllerSupport.before('userController', 'create', mockRequest, mockParams)
        }
    }

    void testThatBeforeDoesNothingIfTheActionIsNotConversational() {

        mock(controllerSupport).getControllerClass('userController').returns(UserController)
        mock(controllerSupport).isConversational(UserController, 'create').returns(false)

        HttpServletRequest mockRequest = mock(HttpServletRequest)
        GrailsParameterMap mockParams = mock(GrailsParameterMap)

        play {
            controllerSupport.before('userController', 'create', mockRequest, mockParams)
        }
    }

    void testThatBeforeResumesAMandatoryConversation() {
        conversation MANDATORY

        Conversation conversation = new Conversation(id: 'ABC')
        HttpServletRequest mockRequest = mock(HttpServletRequest)
        GrailsParameterMap mockParams = mock(GrailsParameterMap)

        mockRequest.requestURI.returns('/foo')
        mockRequest.conversation.set(conversation)
        mockParams.getAt('conversationId').returns('ABC')
        mockParams.putAt('conversationId', conversation.id)

        mockConversationManager.resume('ABC').returns(conversation)

        play {
            controllerSupport.before('userController', 'create', mockRequest, mockParams)
        }
    }

    void testThatBeforeResumesASupportedConversationIfPossible() {
        conversation SUPPORTED

        Conversation conversation = new Conversation(id: 'ABC')
        HttpServletRequest mockRequest = mock(HttpServletRequest)
        GrailsParameterMap mockParams = mock(GrailsParameterMap)

        mockRequest.requestURI.returns('/foo')
        mockRequest.conversation.set(conversation)
        mockParams.getAt('conversationId').returns('ABC')
        mockParams.putAt('conversationId', conversation.id)

        mockConversationManager.resumeIfPossible('ABC').returns(conversation)

        play {
            controllerSupport.before('userController', 'create', mockRequest, mockParams)
        }
    }

    void testThatBeforeSuspendsAConversationWhenTheActionDoesNotSupportConversations() {
        conversation NOT_SUPPORTED

        HttpServletRequest mockRequest = mock(HttpServletRequest)
        GrailsParameterMap mockParams = mock(GrailsParameterMap)

        mockRequest.requestURI.returns('/foo')
        mockParams.getAt('conversationId').returns('ABC')

        mockConversationManager.close('ABC')       
        play {
            controllerSupport.before('userController', 'create', mockRequest, mockParams)
        }
    }

    void testThatAfterResumesASuspendedConversationWhenTheActionDoesNotSupportConversations() {
        conversation NOT_SUPPORTED

        Conversation conversation = new Conversation(id: 'ABC')

        HttpServletRequest mockRequest = mock(HttpServletRequest)
        mockRequest.requestURI.returns('/foo')
        mockRequest.conversation.returns(conversation).stub()

        mockConversationManager.resumeIfPossible('ABC').returns(conversation)

        play {
            controllerSupport.after('userController', 'create', mockRequest)
        }
    }

    void testThatBeforeRaisesAnErrorWhenAConversationIdIsSpecifiedButTheActionIsNeverConversational() {
        conversation NEVER

        HttpServletRequest mockRequest = mock(HttpServletRequest)
        GrailsParameterMap mockParams = mock(GrailsParameterMap)
        
        mockRequest.requestURI.returns('/foo')
        mockParams.getAt('conversationId').returns('ABC')
                
        play {
            shouldFail ConversationException, {
                controllerSupport.before('userController', 'create', mockRequest, mockParams)
            }
        }
    }

    void testThatAfterClosesAConversation() {
        conversation REQUIRED

        Conversation conversation = new Conversation(id: 'ABC')

        HttpServletRequest mockRequest = mock(HttpServletRequest)
        mockRequest.requestURI.returns('/foo')
        mockRequest.conversation.returns(conversation).stub()

        mockConversationManager.close('ABC').returns(conversation)

        play {
            controllerSupport.after('userController', 'create', mockRequest)
        }
    }

    void testThatAfterDoesNothingIfTheActionIsNotConversational() {
        mock(controllerSupport).getControllerClass('userController').returns(UserController)
        mock(controllerSupport).isConversational(UserController, 'create').returns(false)        

        HttpServletRequest mockRequest = mock(HttpServletRequest)

        play {
            controllerSupport.after('userController', 'create', mockRequest)
        }
    }


    void testThatAfterDoesNothingIfThereIsNoConversationOnTheRequest() {
        conversation REQUIRED

        HttpServletRequest mockRequest = mock(HttpServletRequest)
        mockRequest.conversation.returns(null)
        
        play {
            controllerSupport.after('userController', 'create', mockRequest)
        }
    }

    private void conversation(Propagation propagation) {
        mock(controllerSupport).getControllerClass('userController').returns(UserController)
        mock(controllerSupport).isConversational(UserController, 'create').returns(true)
        mock(controllerSupport).getPropagation(UserController, 'create').returns(propagation).stub()
    }
}