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

import grails.test.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

import org.gmock.WithGMock
import uk.co.acuminous.spc.test.ConversationalControllerTestUtils

@WithGMock
class UserControllerTests extends ControllerUnitTestCase {

    ConversationManager mockConversationManager

    void setUp() {
        super.setUp()
        mockConversationManager = mock(ConversationManager)
        ConversationalControllerTestUtils.makeConversational(controller, mockConversationManager)
    }

    void testThatICanShowTheListOfUsers() {
        User user1 = new User()
        User user2 = new User()
        mockDomain User, [user1, user2]

        controller.index()

        assertThat renderArgs.view, is(equalTo('index'))
        assertThat renderArgs.model.users, is(equalTo([user1, user2]))
    }


    void testThatICanShowATab() {
        User user = setUpUser()

        controller.params.id = user.id         
        controller.params.tab = 'account'        
        controller.showTab()

        assertThat renderArgs.template?.toString(), is(equalTo('accountTab'))
        assertThat renderArgs.model.user, is(equalTo(user))
    }

    void testThatISaveWhenSwitchingTabs() {
        User user = setUpUser()
        String expectedUsername = 'chuck'
        mock(user).save([validate:false]).returns(user)

        controller.params.id = user.id
        controller.params.username = expectedUsername        
        play {
            controller.onSwitchTab()
        }

        assertThat user.username, is(equalTo(expectedUsername))
        assertThat controller.response.contentAsString, is(equalTo('OK'))        
    }

    void testThatICanCreate() {
        User mockUser = mock(User, constructor())
        mockUser.save([validate: false])
        mockUser.id.returns(123L)

        controller.params.conversationId = 'abc'
        play {
            controller.create()
        }

        assertThat forwardArgs.action, is(equalTo('edit'))
        assertThat forwardArgs.params.id, is(equalTo(123L))
    }

    void testThatICanEdit() {
        User user = setUpUser()

        controller.params.id = user.id         
        controller.edit()

        assertThat renderArgs.view, is(equalTo('edit'))
        assertThat renderArgs.model.user, is(equalTo(user))      
    }
    
    void testThatICanDelete() {
        User user1 = new User(id: 'a', username: 'one')
        User user2 = new User(id: 'b', username: 'two')
        User user3 = new User(id: 'c', username: 'three')
        mockDomain User, [user1, user2, user3]

        controller.params.id = user2.id
        controller.delete()

        assertThat User.count(), is(equalTo(2))
        assertThat renderArgs.template, is(equalTo('userList'))
        assertThat renderArgs.model.users, is(equalTo([user1, user3]))
    }

    void testThatICanSaveChangesToAUser() {
        User user = setUpUser()
        String expectedUsername = 'chuck'
        mock(user).save([validate:false]).returns(user)

        mockConversationManager.commit('abc')
                
        controller.params.id = user.id        
        controller.params.username = expectedUsername
        controller.params.conversationId = 'abc'
        play {
            controller.save()
        }

        assertThat user.username, is(equalTo(expectedUsername))
    }

    void testThatSaveRendersTheCurrentTab() {
        User user = setUpUser()

        controller.params.id = user.id
        controller.params.tab = 'account'
        controller.save()

        assertThat forwardArgs.action, is(equalTo('showTab'))
    }

    void testThatCancelRedirectsToTheUserListPage() {
        String expectedUrl = '/foo'
        mockTagLib('createLink', [controller: 'user', action: 'index'], expectedUrl)

        mockConversationManager.cancel('abc')
        controller.params.conversationId = 'abc'
        play {
            controller.cancel()
        }
        assertThat renderArgs.view, is(equalTo('/common/redirect'))
        assertThat renderArgs.model.url, is(equalTo(expectedUrl))
    }

    void testThatSubmitSavesChangesToTheUser() {
        User user = setUpUser()
        String expectedUsername = 'chuck'
        mock(user).save().returns(user)
        
        mockTagLib('createLink', [controller: 'user', action: 'index'], '/foo')

        mockConversationManager.commit('abc')        

        controller.params.id = user.id
        controller.params.username = expectedUsername
        controller.params.conversationId = 'abc'
        play {
            controller.submit()
        }

        assertThat user.username, is(equalTo(expectedUsername))
    }

    void testThatSubmitRedirectsToTheUserListPage() {
        User user = setUpUser()
        mock(user).save().returns(user)
        
        String expectedUrl = '/foo'
        mockTagLib('createLink', [controller: 'user', action: 'index'], expectedUrl)

        mockConversationManager.commit('abc')                

        controller.params.id = user.id
        controller.params.conversationId = 'abc'        
        play {
            controller.submit()
        }
        assertThat renderArgs.view, is(equalTo('/common/redirect'))
        assertThat renderArgs.model.url, is(equalTo(expectedUrl))
    }

    void testThatSubmitReportsValidationErrors() {
        User user = setUpUser()

        mock(user).save().returns(false)

        controller.params.id = user.id
        play {
            controller.submit()
        }

        assertThat forwardArgs.action, is(equalTo('showTab'))
    }


    private User setUpUser(Long id = 123L) {
        User user = new User(id: id, firstName: 'Steve', lastName: 'Cresswell', username: 'scresswell', location: 'Ipswich', email: 'scresswell@acuminous.co.uk', password: 'secret')
        mockDomain User, [user]
        return user
    }

    private void mockTagLib(String methodName, def args, def returnValue = null) {
        if (returnValue != null) {
            mock(controller)."$methodName"(args).returns(returnValue)
        } else {
            mock(controller)."$methodName"(args)
        }
        mock(controller).g.returns(controller)
    }
}
