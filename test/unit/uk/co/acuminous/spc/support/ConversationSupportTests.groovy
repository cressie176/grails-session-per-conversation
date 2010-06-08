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
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

import org.gmock.WithGMock
import uk.co.acuminous.spc.Conversational

@WithGMock
class ConversationSupportTests extends GrailsUnitTestCase {

    ConversationSupport conversationSupport
    AnnotationTracker mockAnnotationTracker

    void setUp() {
        super.setUp()
        mockAnnotationTracker = mock(AnnotationTracker)
        conversationSupport = new ConversationSupport()
        conversationSupport.annotationTracker = mockAnnotationTracker
    }

    void testThatICanCheckWhetherAClassRequiresConversations() {
        mockAnnotationTracker.recordAnnotationsOnClosures(Sample)
        mockAnnotationTracker.hasAnnotatedClosures(Sample).returns(true)
        play {
            assertThat conversationSupport.requiresConversations(Sample), is(true)
        }
    }

    void testThatICanCheckWhetherAClassDoesNotRequireConversations() {
        mockAnnotationTracker.recordAnnotationsOnClosures(Sample)
        mockAnnotationTracker.hasAnnotatedClosures(Sample).returns(false)
        play {
            assertThat conversationSupport.requiresConversations(Sample), is(false)
        }
    }

    void testThatICanCheckWhetherAClosureIsConversational() {
        mockAnnotationTracker.hasAnnotation(Conversational, Sample, 'someClosure').returns(true)
        play {                                       
            assertThat conversationSupport.isConversational(Sample, 'someClosure'), is(true)
        }
    }

    void testThatICanCheckWhetherAClosureIsNotConversational() {
        mockAnnotationTracker.hasAnnotation(Conversational, Sample, 'someClosure').returns(false)
        play {
            assertThat conversationSupport.isConversational(Sample, 'someClosure'), is(false)
        }
    }
}