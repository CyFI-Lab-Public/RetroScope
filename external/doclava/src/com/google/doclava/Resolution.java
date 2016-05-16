/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.doclava;

/**
 * Resolution stores information about a Java type
 * that needs to be resolved at a later time.
 * It is a plain-old-data (POD) type.
 *
 * <p>Resolutions contain a Variable and a Value, both of which are set in the Resolution constructor.
 * Public accessors {@link Resolution#getVariable()} and {@link Resolution#getValue()} exist to
 * manipulate this data in read-only form.
 *
 * <p>Variables refer to the piece of data within a Java type that needs to be updated
 * (such as superclass, interfaceImplemented, etc) that we could not resolve.
 *
 * <p>Values are the value to which the variable contained within this {@link Resolution} refers.
 * For instance, when AlertDialog extends Dialog, we may not know what Dialog is).
 * In this scenario, the AlertDialog class would have a {@link Resolution} that
 * contains "superclass" as its variable and "Dialog" as its value.
 */
public class Resolution {
    private String mVariable;
    private String mValue;
    private InfoBuilder mBuilder;

    /**
     * Creates a new resolution with variable and value.
     * @param variable The piece of data within a Java type that needs to be updated
     * that we could not resolve.
     * @param value The value to which the variable contained within this {@link Resolution} refers.
     * @param builder The InfoBuilder that is building the file in which the Resolution exists.
     */
    public Resolution(String variable, String value, InfoBuilder builder) {
        mVariable = variable;
        mValue = value;
        mBuilder = builder;
    }

    /**
     * @return The piece of data within a Java type that needs to be updated
     * that we could not resolve.
     */
    public String getVariable() {
        return mVariable;
    }

    /**
     * @return The value to which the variable contained within this {@link Resolution} refers.
     */
    public String getValue() {
        return mValue;
    }

    /**
     * @return The InfoBuilder that built the file in which the Resolution exists.
     */
    public InfoBuilder getInfoBuilder() {
        return mBuilder;
    }

    @Override
    public String toString() {
        return mVariable + ": " +  mValue;
    }
}
