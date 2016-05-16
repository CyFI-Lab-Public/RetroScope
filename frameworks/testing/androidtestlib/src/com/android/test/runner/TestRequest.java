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
package com.android.test.runner;

import org.junit.runner.Request;
import org.junit.runner.notification.Failure;

import java.util.List;

/**
 * A data structure for holding a {@link Request} and the {@link Failure}s that occurred during its
 * creation.
 */
public class TestRequest {
     private final List<Failure> mFailures;
     private final Request mRequest;

     public TestRequest(List<Failure> requestBuildFailures, Request request) {
         mRequest = request;
         mFailures = requestBuildFailures;
     }

     public List<Failure> getFailures() {
         return mFailures;
     }

     public Request getRequest() {
         return mRequest;
     }
}