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

@WithGMock
class ControllerConversationSupportTests extends GrailsUnitTestCase {

    ControllerConversationSupport controllerSupport
    ConversationManager mockConversationManager

    void setUp() {
        mockConversationManager = mock(ConversationManager)

        controllerSupport = new ControllerConversationSupport()
        controllerSupport.conversationManagerFactory =
            [getConversationManager: { return mockConversationManager }] as ConversationManagerFactory
    }

    void testThatBeforeStartsAConversationIfNoIdExists() {

        mock(controllerSupport).getControllerClass('userController').returns(UserController)
        mock(controllerSupport).isConversational(UserController, 'create').returns(true)

        Conversation conversation = new Conversation(id: 'ABC')
        HttpServletRequest mockRequest = mock(HttpServletRequest)
        GrailsParameterMap mockParams = mock(GrailsParameterMap)

        mockRequest.uri.returns('/foo')
        mockRequest.conversation.set(conversation)
        mockParams.getAt('conversationId').returns(null)
        mockParams.putAt('conversationId', conversation.id)

        mockConversationManager.start().returns(conversation)

        play {
            controllerSupport.before('userController', 'create', mockRequest, mockParams)
        }
    }

    void testThatBeforeResumesAConversationIfAnIdExists() {

        mock(controllerSupport).getControllerClass('userController').returns(UserController)
        mock(controllerSupport).isConversational(UserController, 'create').returns(true)

        Conversation conversation = new Conversation(id: 'XXX')
        HttpServletRequest mockRequest = mock(HttpServletRequest)
        GrailsParameterMap mockParams = mock(GrailsParameterMap)

        mockRequest.uri.returns('/foo')
        mockRequest.conversation.set(conversation)
        mockParams.getAt('conversationId').returns('ABC')
        mockParams.putAt('conversationId', conversation.id)

        mockConversationManager.resume('ABC').returns(conversation)

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

    void testThatAfterClosesAConversation() {

        mock(controllerSupport).getControllerClass('userController').returns(UserController)
        mock(controllerSupport).isConversational(UserController, 'create').returns(true)

        Conversation conversation = new Conversation(id: 'ABC')

        HttpServletRequest mockRequest = mock(HttpServletRequest)
        mockRequest.uri.returns('/foo')
        mockRequest.conversation.returns(conversation)


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
}