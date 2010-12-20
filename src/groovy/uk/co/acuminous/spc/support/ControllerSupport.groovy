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

import uk.co.acuminous.spc.Propagation
import org.apache.commons.lang.StringUtils
import uk.co.acuminous.spc.ConversationException

class ControllerSupport extends ConversationSupport {

    static final String SPC_TOKEN = 'conversationId'
    static final Logger log = Logger.getLogger(ControllerSupport)

    void makeConversational(Class clazz) {
        makeConversational(clazz, {
            return params[spcToken]
        })
    }

    void before(String controllerName, String actionName, HttpServletRequest request, GrailsParameterMap params) {

        Class controllerClass = getControllerClass(controllerName)

        if (isConversational(controllerClass, actionName)) {

            log.trace("Before conversation : ${controllerClass}.${actionName} : ${request.requestURI}")

            String conversationId = params[spcToken]
            Propagation propagation = getPropagation(controllerClass, actionName)
            Closure handler = getHandler(propagation)
            Conversation conversation = handler(conversationId)

            if (conversation) {
                params[spcToken] = conversation.id
                request.conversation = conversation
            }
        }
    }

    void after(String controllerName, String actionName, HttpServletRequest request) {

        Class controllerClass = getControllerClass(controllerName)

        if (isConversational(controllerClass, actionName) && request.conversation) {

            log.trace("After conversation : ${controllerClass}.${actionName} : ${request.requestURI}")


            Propagation propagation = getPropagation(controllerClass, actionName)
            if (propagation == Propagation.NOT_SUPPORTED) {
                conversationManager.resumeIfPossible(request.conversation.id)
            } else {
                conversationManager.close(request.conversation.id)
            }
            
        }
    }

    Closure getHandler(Propagation propagation) {
        List words = propagation.name().toLowerCase().split('_')
        StringBuffer handlerName = new StringBuffer('handle')
        words.each { String word ->
            handlerName << StringUtils.capitalize(word)
        }
        log.debug("Handler: ${handlerName}")
        return this."${handlerName}"
    }

    def handleRequired = { String conversationId ->
        return conversationManager.startOrResume(conversationId)
    }

    def handleRequiresNew = { String conversationId ->
        if (conversationId == null) {
            return conversationManager.start()
        } else {
            throw new ConversationException('A new conversation is required')
        }        
    }

    def handleMandatory = { String conversationId ->
        return conversationManager.resume(conversationId)
    }

    def handleSupported = { String conversationId ->
        return conversationManager.resumeIfPossible(conversationId)        
    }

    def handleNotSupported = { String conversationId ->
        conversationManager.close(conversationId)
    }

    def handleNever = { String conversationId ->
        if (conversationId != null) {
            throw new ConversationException("This action cannot be executed within a conversation")            
        }
    }

    private String getSpcToken() {
        return ConfigurationHolder.config?.spc?.token ?: SPC_TOKEN        
    }

    private Class getControllerClass(String controllerName) {
        if (!controllerName) {
            return null
        } else {
            return ApplicationHolder.application.getArtefactByLogicalPropertyName('Controller', controllerName)?.getClazz()
        }
    }    
}
