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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An interface that can issue HTTP GET / POST requests
 * with timeouts.
 */
public interface HttpHelper {

    public String get(GetRequest request) throws IOException, HttpException;

    public String get(String url, Map<String,String> requestHeaders)
            throws IOException, HttpException;

    public String post(PostRequest request) throws IOException, HttpException;

    public String post(String url, Map<String,String> requestHeaders, String content)
            throws IOException, HttpException;

    public void setConnectTimeout(int timeoutMillis);

    public void setReadTimeout(int timeoutMillis);

    public static class GetRequest {
        private String mUrl;
        private Map<String,String> mHeaders;

        /**
         * Creates a new request.
         */
        public GetRequest() {
        }

        /**
         * Creates a new request.
         *
         * @param url Request URI.
         */
        public GetRequest(String url) {
            mUrl = url;
        }

        /**
         * Gets the request URI.
         */
        public String getUrl() {
            return mUrl;
        }
        /**
         * Sets the request URI.
         */
        public void setUrl(String url) {
            mUrl = url;
        }

        /**
         * Gets the request headers.
         *
         * @return The response headers. May return {@code null} if no headers are set.
         */
        public Map<String, String> getHeaders() {
            return mHeaders;
        }

        /**
         * Sets a request header.
         *
         * @param name Header name.
         * @param value Header value.
         */
        public void setHeader(String name, String value) {
            if (mHeaders == null) {
                mHeaders = new HashMap<String,String>();
            }
            mHeaders.put(name, value);
        }
    }

    public static class PostRequest extends GetRequest {

        private String mContent;

        public PostRequest() {
        }

        public PostRequest(String url) {
            super(url);
        }

        public void setContent(String content) {
            mContent = content;
        }

        public String getContent() {
            return mContent;
        }
    }

    /**
     * A HTTP exception.
     */
    public static class HttpException extends IOException {
        private final int mStatusCode;
        private final String mReasonPhrase;

        public HttpException(int statusCode, String reasonPhrase) {
            super(statusCode + " " + reasonPhrase);
            mStatusCode = statusCode;
            mReasonPhrase = reasonPhrase;
        }

        /**
         * Gets the HTTP response status code.
         */
        public int getStatusCode() {
            return mStatusCode;
        }

        /**
         * Gets the HTTP response reason phrase.
         */
        public String getReasonPhrase() {
            return mReasonPhrase;
        }
    }

    /**
     * An interface for URL rewriting.
     */
    public static interface UrlRewriter {
      public String rewrite(String url);
    }
}
