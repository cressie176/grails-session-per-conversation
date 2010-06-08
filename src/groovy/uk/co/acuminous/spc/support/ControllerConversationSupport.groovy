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

import uk.co.acuminous.spc.Conversation

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import org.apache.log4j.Logger
import javax.servlet.http.HttpServletRequest

class ControllerConversationSupport extends ConversationSupport {

    static final String SPC_TOKEN = 'conversationId'
    static final Logger log = Logger.getLogger(ControllerConversationSupport)

    void makeConversational(Class clazz) {
        makeConversational(clazz, {
            return params[spcToken]
        })
    }

    void before(String controllerName, String actionName, HttpServletRequest request, GrailsParameterMap params) {
        Class controllerClazz = getControllerClass(controllerName)
        if (isConversational(controllerClazz, actionName)) {
            log.debug("Before conversation: ${request.uri}")
            String conversationId = params[spcToken]
            Conversation conversation = joinConversation(conversationId)
            params[spcToken] = conversation.id
            request.conversation = conversation
        }
    }

    void after(String controllerName, String actionName, HttpServletRequest request) {
        Class controllerClazz = getControllerClass(controllerName)
        if (isConversational(controllerClazz, actionName)) {
            conversationManager.close(request.conversation.id)
            log.debug("After conversation: ${request.uri}")
        }
    }

    private Conversation joinConversation(String conversationId) {
        return conversationId ? conversationManager.resume(conversationId) : conversationManager.start()
    }

    private String getSpcToken() {
        return ConfigurationHolder.config?.spc?.token ?: SPC_TOKEN        
    }

    private Class getControllerClass(String controllerName) {
        return ApplicationHolder.application.getArtefactByLogicalPropertyName('Controller', controllerName).getClazz()
    }    
}
