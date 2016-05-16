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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import net.oauth.client.ExcerptInputStream;
import net.oauth.http.HttpMessage;
import net.oauth.http.HttpResponseMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

/**
 * Utility methods for an OAuth client based on the <a
 * href="http://hc.apache.org">Apache HttpClient</a>.
 * 
 * @author Sean Sullivan
 * @hide
 */
public class HttpClient4 implements net.oauth.http.HttpClient {

    public HttpClient4() {
        this(SHARED_CLIENT);
    }

    public HttpClient4(HttpClientPool clientPool) {
        this.clientPool = clientPool;
    }

    private final HttpClientPool clientPool;

    public HttpResponseMessage execute(HttpMessage request) throws IOException {
        final String method = request.method;
        final String url = request.url.toExternalForm();
        final InputStream body = request.getBody();
        final boolean isDelete = DELETE.equalsIgnoreCase(method);
        final boolean isPost = POST.equalsIgnoreCase(method);
        final boolean isPut = PUT.equalsIgnoreCase(method);
        byte[] excerpt = null;
        HttpRequestBase httpRequest;
        if (isPost || isPut) {
            HttpEntityEnclosingRequestBase entityEnclosingMethod =
                isPost ? new HttpPost(url) : new HttpPut(url);
            if (body != null) {
                ExcerptInputStream e = new ExcerptInputStream(body);
                excerpt = e.getExcerpt();
                String length = request.removeHeaders(HttpMessage.CONTENT_LENGTH);
                entityEnclosingMethod.setEntity(new InputStreamEntity(e,
                        (length == null) ? -1 : Long.parseLong(length)));
            }
            httpRequest = entityEnclosingMethod;
        } else if (isDelete) {
            httpRequest = new HttpDelete(url);
        } else {
            httpRequest = new HttpGet(url);
        }
        for (Map.Entry<String, String> header : request.headers) {
            httpRequest.addHeader(header.getKey(), header.getValue());
        }
        HttpClient client = clientPool.getHttpClient(new URL(httpRequest.getURI().toString()));
        client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
        HttpResponse httpResponse = client.execute(httpRequest);
        return new HttpMethodResponse(httpRequest, httpResponse, excerpt, request.getContentCharset());
    }

    private static final HttpClientPool SHARED_CLIENT = new SingleClient();
    
    /**
     * A pool that simply shares a single HttpClient. An HttpClient owns a pool
     * of TCP connections. So, callers that share an HttpClient will share
     * connections. Sharing improves performance (by avoiding the overhead of
     * creating connections) and uses fewer resources in the client and its
     * servers.
     */
    private static class SingleClient implements HttpClientPool
    {
        SingleClient()
        {
            HttpClient client = new DefaultHttpClient();
            ClientConnectionManager mgr = client.getConnectionManager();
            if (!(mgr instanceof ThreadSafeClientConnManager)) {
                HttpParams params = client.getParams();
                client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,
                        mgr.getSchemeRegistry()), params);
            }
            this.client = client;
        }

        private final HttpClient client;

        public HttpClient getHttpClient(URL server)
        {
            return client;
        }
    }

}
