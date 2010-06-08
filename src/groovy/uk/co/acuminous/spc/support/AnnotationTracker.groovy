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

class AnnotationTracker {

    Map<String, Map<Class, List<Class>>> classMap = [:]
    List<Class> supportedAnnotations

    AnnotationTracker(List supportedAnnotations) {
        this.supportedAnnotations = supportedAnnotations
    }

    void recordAnnotationsOnClosures(Class target) {
        Map<String, List<Class>> annotatedClosures = findAnnotatedClosures(target)
        if (annotatedClosures) {            
            classMap[target] = annotatedClosures
        }
    }

    public boolean hasAnnotatedClosures(Class clazz) {
        return classMap.containsKey(clazz)
    }

    public boolean hasAnnotation(Class annotation, Class target, String closureName) {
        Map<String, List<Class>> closureAnnotations = classMap[target] ?: [:]
        List<Class> annotations = closureAnnotations[closureName]
        return annotations?.contains(annotation)
    }

    private Map<String, List<Class>> findAnnotatedClosures(Class clazz) {
        def map = [:]
        clazz.declaredFields.each { Field field ->
            List fieldAnnotations = []
            supportedAnnotations.each { Class annotationClass ->
                if (field.isAnnotationPresent(annotationClass)) {
                    fieldAnnotations << annotationClass
                }
            }
            if (fieldAnnotations) {
                map[field.name] = fieldAnnotations
            }
        }

        return map
    }
}
