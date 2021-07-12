/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.spring.schema;

import java.util.Objects;

/**
 * The type seata target
 *
 * @author xingfudeshi@gmail.com
 */
public class SeataTarget {
    /**
     * target type
     */
    private SeataTargetType targetType;
    /**
     * the name of the target class or method
     */
    private String targetName;
    /**
     * annotation class
     */
    private Class<?> annotationClass;
    /**
     * config object of annotation class
     */
    private Object annotationConfigObject;

    public SeataTarget(SeataTargetType targetType, String targetName, Class<?> annotationClass, Object annotationConfigObject) {
        this.targetType = targetType;
        this.targetName = targetName;
        this.annotationClass = annotationClass;
        this.annotationConfigObject = annotationConfigObject;
    }

    public SeataTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(SeataTargetType targetType) {
        this.targetType = targetType;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public Class<?> getAnnotationClass() {
        return annotationClass;
    }

    public void setAnnotationClass(Class<?> annotationClass) {
        this.annotationClass = annotationClass;
    }

    public Object getAnnotationConfigObject() {
        return annotationConfigObject;
    }

    public void setAnnotationConfigObject(Object annotationConfigObject) {
        this.annotationConfigObject = annotationConfigObject;
    }

    @Override
    public String toString() {
        return "SeataTarget{" +
            "targetType=" + targetType +
            ", targetName='" + targetName + '\'' +
            ", annotationClass=" + annotationClass +
            ", annotationConfigObject=" + annotationConfigObject +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SeataTarget that = (SeataTarget) o;
        return targetType == that.targetType && Objects.equals(targetName, that.targetName) && Objects.equals(annotationClass, that.annotationClass) && Objects.equals(annotationConfigObject, that.annotationConfigObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetType, targetName, annotationClass, annotationConfigObject);
    }
}
