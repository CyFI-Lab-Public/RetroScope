/*
 * Copyright 2008 Netflix, Inc.
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

package net.oauth.http;

import java.io.IOException;
import net.oauth.OAuthMessage;

/**
 * @hide
 */
public interface HttpClient
{
    /**
     * Send an HTTP request and return the response.
     * <p>
     * Don't follow redirects. If a redirect response is received, simply return
     * it (with a statusCode and LOCATION header).
     */
    HttpResponseMessage execute(HttpMessage request) throws IOException;

    static final String GET = OAuthMessage.GET;
    static final String POST = OAuthMessage.POST;
    static final String PUT = OAuthMessage.PUT;
    static final String DELETE = OAuthMessage.DELETE;

}
