/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.quicksearchbox.util;

import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple HTTP client API.
 */
public class JavaNetHttpHelper implements HttpHelper {
    private static final String TAG = "QSB.JavaNetHttpHelper";
    private static final boolean DBG = false;

    private static final int BUFFER_SIZE = 1024 * 4;
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String DEFAULT_CHARSET = "UTF-8";

    private int mConnectTimeout;
    private int mReadTimeout;
    private final String mUserAgent;
    private final HttpHelper.UrlRewriter mRewriter;

    /**
     * Creates a new HTTP helper.
     *
     * @param rewriter URI rewriter
     * @param userAgent User agent string, e.g. "MyApp/1.0".
     */
    public JavaNetHttpHelper(UrlRewriter rewriter, String userAgent) {
        mUserAgent = userAgent + " (" + Build.DEVICE + " " + Build.ID + ")";
        mRewriter = rewriter;
    }

    /**
     * Executes a GET request and returns the response content.
     *
     * @param request Request.
     * @return The response content. This is the empty string if the response
     *         contained no content.
     * @throws IOException If an IO error occurs.
     * @throws HttpException If the response has a status code other than 200.
     */
    public String get(GetRequest request) throws IOException, HttpException {
        return get(request.getUrl(), request.getHeaders());
    }

    /**
     * Executes a GET request and returns the response content.
     *
     * @param url Request URI.
     * @param requestHeaders Request headers.
     * @return The response content. This is the empty string if the response
     *         contained no content.
     * @throws IOException If an IO error occurs.
     * @throws HttpException If the response has a status code other than 200.
     */
    public String get(String url, Map<String,String> requestHeaders)
            throws IOException, HttpException {
        HttpURLConnection c = null;
        try {
            c = createConnection(url, requestHeaders);
            c.setRequestMethod("GET");
            c.connect();
            return getResponseFrom(c);
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    @Override
    public String post(PostRequest request) throws IOException, HttpException {
        return post(request.getUrl(), request.getHeaders(), request.getContent());
    }

    public String post(String url, Map<String,String> requestHeaders, String content)
            throws IOException, HttpException {
        HttpURLConnection c = null;
        try {
            if (requestHeaders == null) {
                requestHeaders = new HashMap<String, String>();
            }
            requestHeaders.put("Content-Length",
                    Integer.toString(content == null ? 0 : content.length()));
            c = createConnection(url, requestHeaders);
            c.setDoOutput(content != null);
            c.setRequestMethod("POST");
            c.connect();
            if (content != null) {
                OutputStreamWriter writer = new OutputStreamWriter(c.getOutputStream());
                writer.write(content);
                writer.close();
            }
            return getResponseFrom(c);
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    private HttpURLConnection createConnection(String url, Map<String, String> headers)
            throws IOException, HttpException {
        URL u = new URL(mRewriter.rewrite(url));
        if (DBG) Log.d(TAG, "URL=" + url + " rewritten='" + u + "'");
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        if (headers != null) {
            for (Map.Entry<String,String> e : headers.entrySet()) {
                String name = e.getKey();
                String value = e.getValue();
                if (DBG) Log.d(TAG, "  " + name + ": " + value);
                c.addRequestProperty(name, value);
            }
        }
        c.addRequestProperty(USER_AGENT_HEADER, mUserAgent);
        if (mConnectTimeout != 0) {
            c.setConnectTimeout(mConnectTimeout);
        }
        if (mReadTimeout != 0) {
            c.setReadTimeout(mReadTimeout);
        }
        return c;
    }

    private String getResponseFrom(HttpURLConnection c) throws IOException, HttpException {
        if (c.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new HttpException(c.getResponseCode(), c.getResponseMessage());
        }
        if (DBG) {
            Log.d(TAG, "Content-Type: " + c.getContentType() + " (assuming " +
                    DEFAULT_CHARSET + ")");
        }
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(c.getInputStream(), DEFAULT_CHARSET));
        StringBuilder string = new StringBuilder();
        char[] chars = new char[BUFFER_SIZE];
        int bytes;
        while ((bytes = reader.read(chars)) != -1) {
            string.append(chars, 0, bytes);
        }
        return string.toString();
    }

    public void setConnectTimeout(int timeoutMillis) {
        mConnectTimeout = timeoutMillis;
    }

    public void setReadTimeout(int timeoutMillis) {
        mReadTimeout = timeoutMillis;
    }

    /**
     * A Url rewriter that does nothing, i.e., returns the
     * url that is passed to it.
     */
    public static class PassThroughRewriter implements UrlRewriter {
        @Override
        public String rewrite(String url) {
            return url;
        }
    }
}
