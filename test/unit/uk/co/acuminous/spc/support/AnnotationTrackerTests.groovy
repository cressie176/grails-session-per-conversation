package uk.co.acuminous.spc.support

import grails.test.GrailsUnitTestCase
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

import uk.co.acuminous.spc.Conversational

class AnnotationTrackerTests extends GrailsUnitTestCase {

    void testThatICanRecordAnnotatedClosures() {
        AnnotationTracker tracker = new AnnotationTracker([Conversational])
        tracker.recordAnnotationsOnClosures(Sample)
        assertThat tracker.hasAnnotation(Conversational, Sample, 'someClosure'), is(true)
        assertThat tracker.hasAnnotation(Conversational, Sample, 'someOtherClosure'), is(false)
    }

    void testThatICanDetermineWhetherAClassHasAnnotatedClosures() {
        AnnotationTracker tracker = new AnnotationTracker([Conversational])
        tracker.recordAnnotationsOnClosures(Sample)
        assertThat tracker.hasAnnotatedClosures(Sample), is(true)
    }

    void testThatICanDetermineWhetherAClassDoesNotHaveAnnotatedClosures() {
        AnnotationTracker tracker = new AnnotationTracker([Conversational])
        tracker.recordAnnotationsOnClosures(AnotherSample)
        assertThat tracker.hasAnnotatedClosures(AnotherSample), is(false)
    }
}

class Sample {
    @Conversational
    def someClosure = {}
    def someOtherClosure = {}
}

class AnotherSample {
    def someClosure = {}
}
