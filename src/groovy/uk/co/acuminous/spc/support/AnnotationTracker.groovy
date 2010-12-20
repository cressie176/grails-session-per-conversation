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

import java.lang.reflect.Field
import uk.co.acuminous.spc.Conversational
import java.lang.annotation.Annotation

class AnnotationTracker {

    Map<String, Map<Class, List>> classMap = [:]
    List<Class> supportedAnnotations

    AnnotationTracker(List supportedAnnotations) {
        this.supportedAnnotations = supportedAnnotations
    }

    void recordAnnotationsOnClosures(Class target) {
        Map<String, List> annotatedClosures = findAnnotatedClosures(target)
        if (annotatedClosures) {            
            classMap[target] = annotatedClosures
        }
    }

    public boolean hasAnnotatedClosures(Class clazz) {
        return classMap.containsKey(clazz)
    }

    public boolean hasAnnotation(Class annotationClass, Class target, String closureName) {
        return getAnnotation(annotationClass, target, closureName) != null
    }

    public def getAnnotation(Class annotationClass, Class target, String closureName) {
        Map<String, List> closureAnnotations = classMap[target] ?: [:]
        List annotations = closureAnnotations[closureName]
        return annotations?.find { def annotation ->
            annotationClass.isAssignableFrom(annotation.class)
        }
    }

    private Map<String, List> findAnnotatedClosures(Class targetClass) {
        def map = [:]
        targetClass.declaredFields.each { Field field ->
            List<Annotation> fieldAnnotations = []
            supportedAnnotations.each { Class annotationClass ->
                if (field.isAnnotationPresent(annotationClass)) {
                    Annotation annotation = field.getAnnotation(annotationClass)
                    fieldAnnotations << annotation
                }
            }
            if (fieldAnnotations) {
                map[field.name] = fieldAnnotations
            }
        }

        return map
    }
}
