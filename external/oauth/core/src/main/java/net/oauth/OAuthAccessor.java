/*
 * Copyright 2007 Netflix, Inc.
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

package net.oauth;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.oauth.http.HttpMessage;

/**
 * Properties of one User of an OAuthConsumer. Properties may be added freely,
 * e.g. to support extensions.
 * 
 * @author John Kristian
 * @hide
 */
public class OAuthAccessor implements Serializable {

    private static final long serialVersionUID = 5590788443138352999L;

    public final OAuthConsumer consumer;
    public String requestToken;
    public String accessToken;
    public String tokenSecret;

    public OAuthAccessor(OAuthConsumer consumer) {
        this.consumer = consumer;
        this.requestToken = null;
        this.accessToken = null;
        this.tokenSecret = null;
    }

    private final Map<String, Object> properties = new HashMap<String, Object>();

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    /**
     * Construct a request message containing the given parameters but no body.
     * Don't send the message, merely construct it. The caller will ordinarily
     * send it, for example by calling OAuthClient.invoke.
     * 
     * @param method
     *            the HTTP request method. If this is null, use the default
     *            method; that is getProperty("httpMethod") or (if that's null)
     *            consumer.getProperty("httpMethod") or (if that's null)
     *            OAuthMessage.GET.
     */
    public OAuthMessage newRequestMessage(String method, String url,
            Collection<? extends Map.Entry> parameters)
    throws OAuthException, IOException, URISyntaxException {
        if (method == null) {
            method = (String) this.getProperty("httpMethod");
            if (method == null) {
                method = (String) this.consumer.getProperty("httpMethod");
                if (method == null) {
                    method = OAuthMessage.GET;
                }
            }
        }
        OAuthMessage message = new OAuthMessage(method, url, parameters);
        message.addRequiredParameters(this);
        Object accepted = consumer.getProperty(OAuthConsumer.ACCEPT_ENCODING);
        if (accepted != null) {
            message.getHeaders().add(new OAuth.Parameter(HttpMessage.ACCEPT_ENCODING, accepted.toString()));
        }
        return message;
    }

}
