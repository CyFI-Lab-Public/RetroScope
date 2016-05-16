/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.cts.tradefed.result;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/** MultipartForm builds a multipart form and submits it. */
class MultipartForm {

    private static final String FORM_DATA_BOUNDARY = "C75I55u3R3p0r73r";

    private final String mServerUrl;

    private final Map<String, String> mFormValues = new HashMap<String, String>();

    private String mName;
    private String mFileName;
    private byte[] mData;

    public MultipartForm(String serverUrl) {
        mServerUrl = serverUrl;
    }

    public MultipartForm addFormValue(String name, String value) {
        mFormValues.put(name, value);
        return this;
    }

    public MultipartForm addFormFile(String name, String fileName, byte[] data) {
        mName = name;
        mFileName = fileName;
        mData = data;
        return this;
    }

    public void submit() throws IOException {
        String redirectUrl = submitForm(mServerUrl);
        if (redirectUrl != null) {
            submitForm(redirectUrl);
        }
    }

    /**
     * @param serverUrl to post the data to
     * @return a url if the server redirected to another url
     * @throws IOException
     */
    private String submitForm(String serverUrl) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(serverUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + FORM_DATA_BOUNDARY);

            byte[] body = getContentBody();
            connection.setRequestProperty("Content-Length", Integer.toString(body.length));

            OutputStream output = connection.getOutputStream();
            try {
                output.write(body);
            } finally {
                output.close();
            }

            // Open the stream to get a response. Otherwise request will be cancelled.
            InputStream input = connection.getInputStream();
            input.close();

            if (connection.getResponseCode() == 302) {
                return connection.getHeaderField("Location");
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    private byte[] getContentBody() throws IOException {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(byteOutput));
        writer.println();

        for (Map.Entry<String, String> formValue : mFormValues.entrySet()) {
            writeFormField(writer, formValue.getKey(), formValue.getValue());
        }

        if (mData != null) {
            writeFormFileHeader(writer, mName, mFileName);
            writer.flush(); // Must flush here before writing to the byte stream!
            byteOutput.write(mData);
            writer.println();
        }
        writer.append("--").append(FORM_DATA_BOUNDARY).println("--");
        writer.flush();
        writer.close();
        return byteOutput.toByteArray();
    }

    private void writeFormField(PrintWriter writer, String name, String value) {
        writer.append("--").println(FORM_DATA_BOUNDARY);
        writer.append("Content-Disposition: form-data; name=\"").append(name).println("\"");
        writer.println();
        writer.println(value);
    }

    private void writeFormFileHeader(PrintWriter writer, String name, String fileName) {
        writer.append("--").println(FORM_DATA_BOUNDARY);
        writer.append("Content-Disposition: form-data; name=\"").append(name);
        writer.append("\"; filename=\"").append(fileName).println("\"");
        writer.println("Content-Type: application/x-gzip");
        writer.println("Content-Transfer-Encoding: binary");
        writer.println();
    }
}
