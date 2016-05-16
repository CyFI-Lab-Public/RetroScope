/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.hardware.cts.helpers;

import android.content.Context;

import android.hardware.Sensor;

import java.security.InvalidParameterException;

/**
 * A 'property' bag of sensor information used for testing purposes.
 */
public class SensorTestInformation {
    private SensorTestInformation() {}

    public static int getAxisCount(int sensorType) {
        switch(sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return 3;
            case Sensor.TYPE_MAGNETIC_FIELD:
                return 3;
//            case Sensor.TYPE_ORIENTATION:
//                return "Orientation";
            case Sensor.TYPE_GYROSCOPE:
                return 3;
//            case Sensor.TYPE_LIGHT:
//                return "Light";
//            case Sensor.TYPE_PRESSURE:
//                return "Pressure";
//            case Sensor.TYPE_TEMPERATURE:
//                return "Temperature";
//            case Sensor.TYPE_PROXIMITY:
//                return "Proximity";
            case Sensor.TYPE_GRAVITY:
                return 3;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return 3;
//            case Sensor.TYPE_ROTATION_VECTOR:
//                return "Rotation Vector";
//            case Sensor.TYPE_RELATIVE_HUMIDITY:
//                return "Relative Humidity";
//            case Sensor.TYPE_AMBIENT_TEMPERATURE:
//                return "Ambient Temperature";
//            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
//                return "Magnetic Field Uncalibrated";
//            case Sensor.TYPE_GAME_ROTATION_VECTOR:
//                return "Game Rotation Vector";
//            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
//                return "Gyroscope Uncalibrated";
//            case Sensor.TYPE_SIGNIFICANT_MOTION:
//                return "Significant Motion";
//            case Sensor.TYPE_STEP_DETECTOR:
//                return "Step Detector";
//            case Sensor.TYPE_STEP_COUNTER:
//                return "Step Counter";
//            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
//                return "Geomagnetic Rotation Vector";
            default:
                throw new InvalidParameterException(
                        String.format("Invalid sensorType:%d. Unable to find axis count.", sensorType));
        }
    }

    public static String getSensorName(int sensorType) {
        String name;
        switch(sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                name = "Accelerometer";
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                name = "Magnetic Field";
                break;
            case Sensor.TYPE_ORIENTATION:
                name = "Orientation";
                break;
            case Sensor.TYPE_GYROSCOPE:
                name = "Gyroscope";
                break;
            case Sensor.TYPE_LIGHT:
                name = "Light";
                break;
            case Sensor.TYPE_PRESSURE:
                name = "Pressure";
                break;
            case Sensor.TYPE_TEMPERATURE:
                name = "Temperature";
                break;
            case Sensor.TYPE_PROXIMITY:
                name = "Proximity";
                break;
            case Sensor.TYPE_GRAVITY:
                name = "Gravity";
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                name = "Linear Acceleration";
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                name = "Rotation Vector";
                break;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                name = "Relative Humidity";
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                name = "Ambient Temperature";
                break;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                name = "Magnetic Field Uncalibrated";
                break;
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                name = "Game Rotation Vector";
                break;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                name = "Gyroscope Uncalibrated";
                break;
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                name = "Significant Motion";
                break;
            case Sensor.TYPE_STEP_DETECTOR:
                name = "Step Detector";
                break;
            case Sensor.TYPE_STEP_COUNTER:
                name = "Step Counter";
                break;
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                name = "Geomagnetic Rotation Vector";
                break;
            default:
                name = "<Unknown>";
        }
        return String.format("%s (%d)", name, sensorType);
    }

    public static int getMaxSamplingRateInUs(Context context, int sensorType) {
        Sensor sensor = SensorCtsHelper.getSensor(context, sensorType);
        return sensor.getMinDelay();
    }
}
