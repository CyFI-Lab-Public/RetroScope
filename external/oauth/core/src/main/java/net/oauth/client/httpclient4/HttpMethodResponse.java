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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.oauth.OAuth;
import net.oauth.client.ExcerptInputStream;
import net.oauth.http.HttpMessage;
import net.oauth.http.HttpResponseMessage;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

/**
 * An HttpResponse, encapsulated as an OAuthMessage.
 * 
 * This class relies on <a href="http://hc.apache.org">Apache HttpClient</a>
 * version 4.
 * 
 * @author Sean Sullivan
 * @hide
 */
public class HttpMethodResponse extends HttpResponseMessage
{

    /**
     * Construct an OAuthMessage from the HTTP response, including parameters
     * from OAuth WWW-Authenticate headers and the body. The header parameters
     * come first, followed by the ones from the response body.
     */
    public HttpMethodResponse(HttpRequestBase request, HttpResponse response, byte[] requestBody,
            String requestEncoding) throws IOException
    {
        super(request.getMethod(), new URL(request.getURI().toString()));
        this.httpRequest = request;
        this.httpResponse = response;
        this.requestBody = requestBody;
        this.requestEncoding = requestEncoding;
        this.headers.addAll(getHeaders());
    }

    private final HttpRequestBase httpRequest;
    private final HttpResponse httpResponse;
    private final byte[] requestBody;
    private final String requestEncoding;

    @Override
    public int getStatusCode()
    {
        return httpResponse.getStatusLine().getStatusCode();
    }

    @Override
    public InputStream openBody() throws IOException
    {
        return httpResponse.getEntity().getContent();
    }

    private List<Map.Entry<String, String>> getHeaders()
    {
        List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();
        Header[] allHeaders = httpResponse.getAllHeaders();
        if (allHeaders != null) {
            for (Header header : allHeaders) {
                headers.add(new OAuth.Parameter(header.getName(), header.getValue()));
            }
        }
        return headers;
    }

    /** Return a complete description of the HTTP exchange. */
    @Override
    public void dump(Map<String, Object> into) throws IOException
    {
        super.dump(into);
        {
            StringBuilder request = new StringBuilder(httpRequest.getMethod());
            request.append(" ").append(httpRequest.getURI().getPath());
            String query = httpRequest.getURI().getQuery();
            if (query != null && query.length() > 0) {
                request.append("?").append(query);
            }
            request.append(EOL);
            for (Header header : httpRequest.getAllHeaders()) {
                request.append(header.getName()).append(": ").append(header.getValue()).append(EOL);
            }
            if (httpRequest instanceof HttpEntityEnclosingRequest) {
                HttpEntityEnclosingRequest r = (HttpEntityEnclosingRequest) httpRequest;
                long contentLength = r.getEntity().getContentLength();
                if (contentLength >= 0) {
                    request.append("Content-Length: ").append(contentLength).append(EOL);
                }
            }
            request.append(EOL);
            if (requestBody != null) {
                request.append(new String(requestBody, requestEncoding));
            }
            into.put(REQUEST, request.toString());
        }
        {
            StringBuilder response = new StringBuilder();
            String value = httpResponse.getStatusLine().toString();
            response.append(value).append(EOL);
            for (Header header : httpResponse.getAllHeaders()) {
                String name = header.getName();
                value = header.getValue();
                response.append(name).append(": ").append(value).append(EOL);
            }
            response.append(EOL);
            if (body != null) {
                response.append(new String(((ExcerptInputStream) body).getExcerpt(),
                        getContentCharset()));
            }
            into.put(HttpMessage.RESPONSE, response.toString());
        }
    }
}
