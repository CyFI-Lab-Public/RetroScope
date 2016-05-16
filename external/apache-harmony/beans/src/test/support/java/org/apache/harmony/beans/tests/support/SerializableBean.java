/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.beans.tests.support;

import java.io.Serializable;

public class SerializableBean implements Serializable {

    private static final long serialVersionUID = -753653007843975743L;

    private int value;

    private String text = null;

    private Integer iValue = null;

    private int[] intArray;

    private String[] strArray;

    public SerializableBean() {
    }

    public SerializableBean(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public Integer getIValue() {
        return iValue;
    }

    public void setIValue(Integer iValue) {
        this.iValue = iValue;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int[] getIntArray() {
        return intArray;
    }

    public void setIntArray(int[] intArray) {
        this.intArray = intArray;
    }

    public String[] getStrArray() {
        return strArray;
    }

    public void setStrArray(String[] strArray) {
        this.strArray = strArray;
    }
}
