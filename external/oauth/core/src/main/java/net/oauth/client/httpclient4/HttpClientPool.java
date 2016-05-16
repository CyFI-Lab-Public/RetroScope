/*
 * Copyright 2008 Sean Sullivan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.oauth.client.httpclient4;

import java.net.URL;

/**
 * 
 * A source of Apache HttpClient 4 objects.
 * 
 * This class relies on <a href="http://hc.apache.org">Apache HttpClient</a>
 * version 4.
 * 
 * @author Sean Sullivan
 * @hide
 */
public interface HttpClientPool {

    /** Get the appropriate HttpClient for sending a request to the given URL. */
    public org.apache.http.client.HttpClient getHttpClient(URL server);

}
