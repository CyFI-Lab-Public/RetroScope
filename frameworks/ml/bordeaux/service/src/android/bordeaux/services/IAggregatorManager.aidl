/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.bordeaux.services;

import android.bordeaux.services.StringString;

interface IAggregatorManager {
    List<StringString> getData(in String dataName);

    // TODO: remove the following interfaces in production
    // they are only used for demo purpose
    List<String> getLocationClusters();
    List<String> getTimeOfDayValues();
    List<String> getDayOfWeekValues();
    // use "" to disable the fake location
    boolean setFakeLocation(in String cluster);
    // use "" to disable the fake time
    boolean setFakeTimeOfDay(in String time_of_day);
    // use "" to disable the fake day
    boolean setFakeDayOfWeek(in String day_of_week);
    // return whether the service is in fake mode
    // it's in fake mode, if any of the location, time and day is fake
    boolean getFakeMode();
}
