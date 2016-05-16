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

/**
 *  Utility to report benchmarking result via HTTP POST
 *  to CTS.
 * @param msg message to add to the report
 * @param score resulting score
 * @param isFinal true if this is the last / final score
 */
function CtsReport(msg, score, isFinal)
{
    req = new XMLHttpRequest();
    req.open("POST", "cts_report.html?final=" + (isFinal ? "1" : "0") +
             "&score=" + score + "&message=" + msg, false);
    req.send(null)
}

