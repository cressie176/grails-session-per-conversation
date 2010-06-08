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

import org.codehaus.groovy.grails.commons.ApplicationHolder as AH

import uk.co.acuminous.spc.ConversationManager
import uk.co.acuminous.spc.ConversationManagerFactory
import org.springframework.beans.factory.config.ServiceLocatorFactoryBean

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClass

import uk.co.acuminous.spc.support.ControllerConversationSupport

class SessionPerConversationGrailsPlugin {

	def loadAfter = ['controllers']
	def observe = ['controllers']    
    def version = "0.1"
    def grailsVersion = "1.1.1 > *"
    def dependsOn = [:]
    def pluginExcludes = [
        'grails-app/controllers/**',
        'grails-app/domain/**',
        'grails-app/i18n/**',
        'grails-app/services/**',
        'grails-app/taglib/**',
        'grails-app/utils/**',
        'grails-app/views/**',
        'lib/**',
        'scripts/**',
        'web-app/**'
    ]

    def author = "Stephen Cresswell"
    def authorEmail = "scresswell@acuminous.co.uk"
    def title = "Session Per Conversation"
    def description = '''Adds support for hibernate sessions that span multiple http requests through annotated controller actions'''
    def documentation = "http://www.acuminous.co.uk/plugins/session-per-conversation"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = { application ->
        spcManager(ConversationManager) { def bean ->
            sessionFactory = ref('sessionFactory')
            bean.scope = 'session'
        }

        spcManagerFactory(ServiceLocatorFactoryBean) {
            serviceLocatorInterface = ConversationManagerFactory
        }

        spcSupport(ControllerConversationSupport) {
            conversationManagerFactory = ref(spcManagerFactory)
        }        
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        ControllerConversationSupport conversationHelper = applicationContext.getBean('spcSupport')
        AH.application.controllerClasses.each { GrailsClass controllerClass ->
            conversationHelper.makeConversational(controllerClass.clazz)
        }
    }

    def onChange = { event ->
        ControllerConversationSupport conversationHelper = event.ctx.getBean('spcSupport')
        if (application.isArtefactOfType(ControllerArtefactHandler.TYPE, event.source)) {
			GrailsClass controllerClass = application.getControllerClass(event.source?.name)
            conversationHelper.makeConversational(controllerClass.clazz)
        }

    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
