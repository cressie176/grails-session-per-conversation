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

package uk.co.acuminous.spc.test

import uk.co.acuminous.spc.ConversationManager
import uk.co.acuminous.spc.ConversationManagerFactory
import uk.co.acuminous.spc.support.ControllerConversationSupport

class ConversationalControllerTestUtils {

    static void makeConversational(def controller, ConversationManager conversationManager) {

        ControllerConversationSupport spcSupport = new ControllerConversationSupport()

        spcSupport.conversationManagerFactory =
            [getConversationManager: { return conversationManager }] as ConversationManagerFactory

        spcSupport.makeConversational(controller.class)
        
    }

}
