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

import org.apache.commons.lang.StringUtils

class User {

    String id

    // Depending on the hibernate dialect (MySql, HSQLDB) the default behaviour for generating ids
    // may result in a flush. (Oracle uses a sequence which is OK) 
    static mapping = {
        id generator: 'uuid'
        table name: 'spc_user'
    }

    // Account
    String username
    String email
    String password

    // Profile
    String firstName
    String lastName    
    String location
    String bio

    static constraints = {
        // nullable:true makes db fields nullable which is necessary for non validating saves
        username(nullable:true, validator: { String value, User user -> notNullable(value) })
        email(nullable:true, validator: { String value, User user -> notNullable(value) })
        password(nullable:true, validator: { String value, User user -> notNullable(value) })
        firstName(nullable:true, validator: { String value, User user -> notNullable(value) })
        lastName(nullable:true, validator: { String value, User user -> notNullable(value) })
        location(nullable:true, validator: { String value, User user -> notNullable(value) })
        bio(nullable:true)
    }

    static def notNullable(String value) {
        return StringUtils.isBlank(value) ? ['nullable'] : null
    }

    public String toString() {
        return "User[id=${id},version=${version},username=${username}]"
    }
}
