package uk.co.acuminous.spc.support

import grails.test.GrailsUnitTestCase
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

import uk.co.acuminous.spc.Conversational
import uk.co.acuminous.spc.Propagation

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

    void testThatICanGetTheSpecifiedAnnotation() {
        AnnotationTracker tracker = new AnnotationTracker([Conversational])
        tracker.recordAnnotationsOnClosures(Sample)
        
        Conversational annotation =  tracker.getAnnotation(Conversational, Sample, 'someClosure')
        assertThat annotation.propagation(), is(Propagation.REQUIRED)
    }
    
    void testThatICanGetAnExplicitlySpecifiedAnnotation() {
        AnnotationTracker tracker = new AnnotationTracker([Conversational])
        tracker.recordAnnotationsOnClosures(Sample)

        Conversational annotation =  tracker.getAnnotation(Conversational, Sample, 'mandatoryClosure')
        assertThat annotation.propagation(), is(Propagation.MANDATORY)
    }
}

class Sample {
    @Conversational
    def someClosure = {}
    def someOtherClosure = {}
    @Conversational(propagation=Propagation.MANDATORY)
    def mandatoryClosure = {} 
}

class AnotherSample {
    def someClosure = {}
}
