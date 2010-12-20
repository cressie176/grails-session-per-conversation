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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*
import org.springframework.validation.FieldError;


class UserTests extends GrailsUnitTestCase {

    void testFieldsCannotBeBlank() {
        mockDomain(User)
        User user = new User()
        user.validate()

        assertFieldError(user, 'username', 'nullable')
        assertFieldError(user, 'email', 'nullable')
        assertFieldError(user, 'password', 'nullable')        
    }

    void assertFieldError(User user, String fieldName, String errorCode) {
        assertThat user.hasErrors(), is(true)

        FieldError error = user.errors.getFieldError(fieldName)
        assertThat error, is(not(nullValue()))
        assertThat error.codes as List, hasItem("${errorCode}.${fieldName}".toString())
    }
}
