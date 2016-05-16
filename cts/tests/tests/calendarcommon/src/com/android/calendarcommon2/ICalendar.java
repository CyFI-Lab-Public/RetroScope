/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendarcommon2;

import android.util.Log;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

/**
 * Stub version of the ICalendar class, containing the subclasses for
 * Component and Property, for use in the test.
 */
public class ICalendar {

    private static final String TAG = "Sync";

    /**
     * A component within an iCalendar (VEVENT, VTODO, VJOURNAL, VFEEBUSY,
     * VTIMEZONE, VALARM).
     */
    public static class Component {
        private final String mName;
        private final LinkedHashMap<String, ArrayList<Property>> mPropsMap =
                new LinkedHashMap<String, ArrayList<Property>>();

        /**
         * Creates a new component with the provided name.
         * @param name The name of the component.
         */
        public Component(String name, Component parent) {
            mName = name;
        }

        /**
         * Adds a Property to this component.
         * @param prop
         */
        public void addProperty(Property prop) {
            String name= prop.getName();
            ArrayList<Property> props = mPropsMap.get(name);
            if (props == null) {
                props = new ArrayList<Property>();
                mPropsMap.put(name, props);
            }
            props.add(prop);
        }

        /**
         * Returns a list of properties with the specified name.  Returns null
         * if there are no such properties.
         * @param name The name of the property that should be returned.
         * @return A list of properties with the requested name.
         */
        public List<Property> getProperties(String name) {
            return mPropsMap.get(name);
        }
    }

    /**
     * A property within an iCalendar component (e.g., DTSTART, DTEND, etc.,
     * within a VEVENT).
     */
    public static class Property {
        private final String mName;

        /**
         * Creates a new property with the provided name.
         * @param name The name of the property.
         */
        public Property(String name) {
            mName = name;
        }

        /**
         * Returns the name of the property.
         * @return The name of the property.
         */
        public String getName() {
            return mName;
        }
    }

    private ICalendar() {
    }
}
