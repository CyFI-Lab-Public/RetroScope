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
package android.webkit.cts;

import libcore.io.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

/**
 * Simple http test server for testing webkit client functionality.
 */
public class CtsTestServer {
    private static final String TAG = "CtsTestServer";

    public static final String FAVICON_PATH = "/favicon.ico";
    public static final String USERAGENT_PATH = "/useragent.html";

    public static final String TEST_DOWNLOAD_PATH = "/download.html";
    private static final String DOWNLOAD_ID_PARAMETER = "downloadId";
    private static final String NUM_BYTES_PARAMETER = "numBytes";

    public static final String ASSET_PREFIX = "/assets/";
    public static final String RAW_PREFIX = "raw/";
    public static final String FAVICON_ASSET_PATH = ASSET_PREFIX + "webkit/favicon.png";
    public static final String APPCACHE_PATH = "/appcache.html";
    public static final String APPCACHE_MANIFEST_PATH = "/appcache.manifest";
    public static final String REDIRECT_PREFIX = "/redirect";
    public static final String DELAY_PREFIX = "/delayed";
    public static final String BINARY_PREFIX = "/binary";
    public static final String COOKIE_PREFIX = "/cookie";
    public static final String AUTH_PREFIX = "/auth";
    public static final String SHUTDOWN_PREFIX = "/shutdown";
    public static final String NOLENGTH_POSTFIX = "nolength";
    public static final int DELAY_MILLIS = 2000;

    public static final String AUTH_REALM = "Android CTS";
    public static final String AUTH_USER = "cts";
    public static final String AUTH_PASS = "secret";
    // base64 encoded credentials "cts:secret" used for basic authentication
    public static final String AUTH_CREDENTIALS = "Basic Y3RzOnNlY3JldA==";

    public static final String MESSAGE_401 = "401 unauthorized";
    public static final String MESSAGE_403 = "403 forbidden";
    public static final String MESSAGE_404 = "404 not found";

    private static Hashtable<Integer, String> sReasons;

    private ServerThread mServerThread;
    private String mServerUri;
    private AssetManager mAssets;
    private Context mContext;
    private Resources mResources;
    private boolean mSsl;
    private MimeTypeMap mMap;
    private Vector<String> mQueries;
    private ArrayList<HttpEntity> mRequestEntities;
    private final Map<String, HttpRequest> mLastRequestMap = new HashMap<String, HttpRequest>();
    private long mDocValidity;
    private long mDocAge;

    /**
     * Create and start a local HTTP server instance.
     * @param context The application context to use for fetching assets.
     * @throws IOException
     */
    public CtsTestServer(Context context) throws Exception {
        this(context, false);
    }

    public static String getReasonString(int status) {
        if (sReasons == null) {
            sReasons = new Hashtable<Integer, String>();
            sReasons.put(HttpStatus.SC_UNAUTHORIZED, "Unauthorized");
            sReasons.put(HttpStatus.SC_NOT_FOUND, "Not Found");
            sReasons.put(HttpStatus.SC_FORBIDDEN, "Forbidden");
            sReasons.put(HttpStatus.SC_MOVED_TEMPORARILY, "Moved Temporarily");
        }
        return sReasons.get(status);
    }

    /**
     * Create and start a local HTTP server instance.
     * @param context The application context to use for fetching assets.
     * @param ssl True if the server should be using secure sockets.
     * @throws Exception
     */
    public CtsTestServer(Context context, boolean ssl) throws Exception {
        mContext = context;
        mAssets = mContext.getAssets();
        mResources = mContext.getResources();
        mSsl = ssl;
        mRequestEntities = new ArrayList<HttpEntity>();
        mMap = MimeTypeMap.getSingleton();
        mQueries = new Vector<String>();
        mServerThread = new ServerThread(this, mSsl);
        if (mSsl) {
            mServerUri = "https://localhost:" + mServerThread.mSocket.getLocalPort();
        } else {
            mServerUri = "http://localhost:" + mServerThread.mSocket.getLocalPort();
        }
        mServerThread.start();
    }

    /**
     * Terminate the http server.
     */
    public void shutdown() {
        try {
            // Avoid a deadlock between two threads where one is trying to call
            // close() and the other one is calling accept() by sending a GET
            // request for shutdown and having the server's one thread
            // sequentially call accept() and close().
            URL url = new URL(mServerUri + SHUTDOWN_PREFIX);
            URLConnection connection = openConnection(url);
            connection.connect();

            // Read the input from the stream to send the request.
            InputStream is = connection.getInputStream();
            is.close();

            // Block until the server thread is done shutting down.
            mServerThread.join();

        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (KeyManagementException e) {
            throw new IllegalStateException(e);
        }
    }

    private URLConnection openConnection(URL url)
            throws IOException, NoSuchAlgorithmException, KeyManagementException {
        if (mSsl) {
            // Install hostname verifiers and trust managers that don't do
            // anything in order to get around the client not trusting
            // the test server due to a lack of certificates.

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setHostnameVerifier(new CtsHostnameVerifier());

            SSLContext context = SSLContext.getInstance("TLS");
            CtsTrustManager trustManager = new CtsTrustManager();
            context.init(null, new CtsTrustManager[] {trustManager}, null);
            connection.setSSLSocketFactory(context.getSocketFactory());

            return connection;
        } else {
            return url.openConnection();
        }
    }

    /**
     * {@link X509TrustManager} that trusts everybody. This is used so that
     * the client calling {@link CtsTestServer#shutdown()} can issue a request
     * for shutdown by blindly trusting the {@link CtsTestServer}'s
     * credentials.
     */
    private static class CtsTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // Trust the CtSTestServer...
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // Trust the CtSTestServer...
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    /**
     * {@link HostnameVerifier} that verifies everybody. This permits
     * the client to trust the web server and call
     * {@link CtsTestServer#shutdown()}.
     */
    private static class CtsHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /**
     * Return the URI that points to the server root.
     */
    public String getBaseUri() {
        return mServerUri;
    }

    /**
     * Return the absolute URL that refers to the given asset.
     * @param path The path of the asset. See {@link AssetManager#open(String)}
     */
    public String getAssetUrl(String path) {
        StringBuilder sb = new StringBuilder(getBaseUri());
        sb.append(ASSET_PREFIX);
        sb.append(path);
        return sb.toString();
    }

    /**
     * Return an artificially delayed absolute URL that refers to the given asset. This can be
     * used to emulate a slow HTTP server or connection.
     * @param path The path of the asset. See {@link AssetManager#open(String)}
     */
    public String getDelayedAssetUrl(String path) {
        StringBuilder sb = new StringBuilder(getBaseUri());
        sb.append(DELAY_PREFIX);
        sb.append(ASSET_PREFIX);
        sb.append(path);
        return sb.toString();
    }

    /**
     * Return an absolute URL that refers to the given asset and is protected by
     * HTTP authentication.
     * @param path The path of the asset. See {@link AssetManager#open(String)}
     */
    public String getAuthAssetUrl(String path) {
        StringBuilder sb = new StringBuilder(getBaseUri());
        sb.append(AUTH_PREFIX);
        sb.append(ASSET_PREFIX);
        sb.append(path);
        return sb.toString();
    }


    /**
     * Return an absolute URL that indirectly refers to the given asset.
     * When a client fetches this URL, the server will respond with a temporary redirect (302)
     * referring to the absolute URL of the given asset.
     * @param path The path of the asset. See {@link AssetManager#open(String)}
     */
    public String getRedirectingAssetUrl(String path) {
        return getRedirectingAssetUrl(path, 1);
    }

    /**
     * Return an absolute URL that indirectly refers to the given asset.
     * When a client fetches this URL, the server will respond with a temporary redirect (302)
     * referring to the absolute URL of the given asset.
     * @param path The path of the asset. See {@link AssetManager#open(String)}
     * @param numRedirects The number of redirects required to reach the given asset.
     */
    public String getRedirectingAssetUrl(String path, int numRedirects) {
        StringBuilder sb = new StringBuilder(getBaseUri());
        for (int i = 0; i < numRedirects; i++) {
            sb.append(REDIRECT_PREFIX);
        }
        sb.append(ASSET_PREFIX);
        sb.append(path);
        return sb.toString();
    }

    public String getBinaryUrl(String mimeType, int contentLength) {
        StringBuilder sb = new StringBuilder(getBaseUri());
        sb.append(BINARY_PREFIX);
        sb.append("?type=");
        sb.append(mimeType);
        sb.append("&length=");
        sb.append(contentLength);
        return sb.toString();
    }

    public String getCookieUrl(String path) {
        StringBuilder sb = new StringBuilder(getBaseUri());
        sb.append(COOKIE_PREFIX);
        sb.append("/");
        sb.append(path);
        return sb.toString();
    }

    public String getUserAgentUrl() {
        StringBuilder sb = new StringBuilder(getBaseUri());
        sb.append(USERAGENT_PATH);
        return sb.toString();
    }

    public String getAppCacheUrl() {
        StringBuilder sb = new StringBuilder(getBaseUri());
        sb.append(APPCACHE_PATH);
        return sb.toString();
    }

    /**
     * @param downloadId used to differentiate the files created for each test
     * @param numBytes of the content that the CTS server should send back
     * @return url to get the file from
     */
    public String getTestDownloadUrl(String downloadId, int numBytes) {
        return Uri.parse(getBaseUri())
                .buildUpon()
                .path(TEST_DOWNLOAD_PATH)
                .appendQueryParameter(DOWNLOAD_ID_PARAMETER, downloadId)
                .appendQueryParameter(NUM_BYTES_PARAMETER, Integer.toString(numBytes))
                .build()
                .toString();
    }

    /**
     * Returns true if the resource identified by url has been requested since
     * the server was started or the last call to resetRequestState().
     *
     * @param url The relative url to check whether it has been requested.
     */
    public synchronized boolean wasResourceRequested(String url) {
        Iterator<String> it = mQueries.iterator();
        while (it.hasNext()) {
            String request = it.next();
            if (request.endsWith(url)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all received request entities since the last reset.
     */
    public synchronized ArrayList<HttpEntity> getRequestEntities() {
        return mRequestEntities;
    }

    public synchronized int getRequestCount() {
        return mQueries.size();
    }

    /**
     * Set the validity of any future responses in milliseconds. If this is set to a non-zero
     * value, the server will include a "Expires" header.
     * @param timeMillis The time, in milliseconds, for which any future response will be valid.
     */
    public synchronized void setDocumentValidity(long timeMillis) {
        mDocValidity = timeMillis;
    }

    /**
     * Set the age of documents served. If this is set to a non-zero value, the server will include
     * a "Last-Modified" header calculated from the value.
     * @param timeMillis The age, in milliseconds, of any document served in the future.
     */
    public synchronized void setDocumentAge(long timeMillis) {
        mDocAge = timeMillis;
    }

    /**
     * Resets the saved requests and request counts.
     */
    public synchronized void resetRequestState() {

        mQueries.clear();
        mRequestEntities = new ArrayList<HttpEntity>();
    }

    /**
     * Returns the last HttpRequest at this path. Can return null if it is never requested.
     */
    public synchronized HttpRequest getLastRequest(String requestPath) {
        String relativeUrl = getRelativeUrl(requestPath);
        if (!mLastRequestMap.containsKey(relativeUrl))
            return null;
        return mLastRequestMap.get(relativeUrl);
    }
    /**
     * Hook for adding stuffs for HTTP POST. Default implementation does nothing.
     * @return null to use the default response mechanism of sending the requested uri as it is.
     *         Otherwise, the whole response should be handled inside onPost.
     */
    protected HttpResponse onPost(HttpRequest request) throws Exception {
        return null;
    }

    /**
     * Return the relative URL that refers to the given asset.
     * @param path The path of the asset. See {@link AssetManager#open(String)}
     */
    private String getRelativeUrl(String path) {
        StringBuilder sb = new StringBuilder(ASSET_PREFIX);
        sb.append(path);
        return sb.toString();
    }

    /**
     * Generate a response to the given request.
     * @throws InterruptedException
     * @throws IOException
     */
    private HttpResponse getResponse(HttpRequest request) throws Exception {
        RequestLine requestLine = request.getRequestLine();
        HttpResponse response = null;
        String uriString = requestLine.getUri();
        Log.i(TAG, requestLine.getMethod() + ": " + uriString);

        synchronized (this) {
            mQueries.add(uriString);
            mLastRequestMap.put(uriString, request);
            if (request instanceof HttpEntityEnclosingRequest) {
                mRequestEntities.add(((HttpEntityEnclosingRequest)request).getEntity());
            }
        }

        if (requestLine.getMethod().equals("POST")) {
            HttpResponse responseOnPost = onPost(request);
            if (responseOnPost != null) {
                return responseOnPost;
            }
        }

        URI uri = URI.create(uriString);
        String path = uri.getPath();
        String query = uri.getQuery();
        if (path.equals(FAVICON_PATH)) {
            path = FAVICON_ASSET_PATH;
        }
        if (path.startsWith(DELAY_PREFIX)) {
            try {
                Thread.sleep(DELAY_MILLIS);
            } catch (InterruptedException ignored) {
                // ignore
            }
            path = path.substring(DELAY_PREFIX.length());
        }
        if (path.startsWith(AUTH_PREFIX)) {
            // authentication required
            Header[] auth = request.getHeaders("Authorization");
            if ((auth.length > 0 && auth[0].getValue().equals(AUTH_CREDENTIALS))
                // This is a hack to make sure that loads to this url's will always
                // ask for authentication. This is what the test expects.
                 && !path.endsWith("embedded_image.html")) {
                // fall through and serve content
                path = path.substring(AUTH_PREFIX.length());
            } else {
                // request authorization
                response = createResponse(HttpStatus.SC_UNAUTHORIZED);
                response.addHeader("WWW-Authenticate", "Basic realm=\"" + AUTH_REALM + "\"");
            }
        }
        if (path.startsWith(BINARY_PREFIX)) {
            List <NameValuePair> args = URLEncodedUtils.parse(uri, "UTF-8");
            int length = 0;
            String mimeType = null;
            try {
                for (NameValuePair pair : args) {
                    String name = pair.getName();
                    if (name.equals("type")) {
                        mimeType = pair.getValue();
                    } else if (name.equals("length")) {
                        length = Integer.parseInt(pair.getValue());
                    }
                }
                if (length > 0 && mimeType != null) {
                    ByteArrayEntity entity = new ByteArrayEntity(new byte[length]);
                    entity.setContentType(mimeType);
                    response = createResponse(HttpStatus.SC_OK);
                    response.setEntity(entity);
                    response.addHeader("Content-Disposition", "attachment; filename=test.bin");
                } else {
                    // fall through, return 404 at the end
                }
            } catch (Exception e) {
                // fall through, return 404 at the end
                Log.w(TAG, e);
            }
        } else if (path.startsWith(ASSET_PREFIX)) {
            path = path.substring(ASSET_PREFIX.length());
            // request for an asset file
            try {
                InputStream in;
                if (path.startsWith(RAW_PREFIX)) {
                  String resourceName = path.substring(RAW_PREFIX.length());
                  int id = mResources.getIdentifier(resourceName, "raw", mContext.getPackageName());
                  if (id == 0) {
                    Log.w(TAG, "Can't find raw resource " + resourceName);
                    throw new IOException();
                  }
                  in = mResources.openRawResource(id);
                } else {
                  in = mAssets.open(path);
                }
                response = createResponse(HttpStatus.SC_OK);
                InputStreamEntity entity = new InputStreamEntity(in, in.available());
                String mimeType =
                    mMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path));
                if (mimeType == null) {
                    mimeType = "text/html";
                }
                entity.setContentType(mimeType);
                response.setEntity(entity);
                if (query == null || !query.contains(NOLENGTH_POSTFIX)) {
                    response.setHeader("Content-Length", "" + entity.getContentLength());
                }
            } catch (IOException e) {
                response = null;
                // fall through, return 404 at the end
            }
        } else if (path.startsWith(REDIRECT_PREFIX)) {
            response = createResponse(HttpStatus.SC_MOVED_TEMPORARILY);
            String location = getBaseUri() + path.substring(REDIRECT_PREFIX.length());
            Log.i(TAG, "Redirecting to: " + location);
            response.addHeader("Location", location);
        } else if (path.startsWith(COOKIE_PREFIX)) {
            /*
             * Return a page with a title containing a list of all incoming cookies,
             * separated by '|' characters. If a numeric 'count' value is passed in a cookie,
             * return a cookie with the value incremented by 1. Otherwise, return a cookie
             * setting 'count' to 0.
             */
            response = createResponse(HttpStatus.SC_OK);
            Header[] cookies = request.getHeaders("Cookie");
            Pattern p = Pattern.compile("count=(\\d+)");
            StringBuilder cookieString = new StringBuilder(100);
            cookieString.append(cookies.length);
            int count = 0;
            for (Header cookie : cookies) {
                cookieString.append("|");
                String value = cookie.getValue();
                cookieString.append(value);
                Matcher m = p.matcher(value);
                if (m.find()) {
                    count = Integer.parseInt(m.group(1)) + 1;
                }
            }

            response.addHeader("Set-Cookie", "count=" + count + "; path=" + COOKIE_PREFIX);
            response.setEntity(createEntity("<html><head><title>" + cookieString +
                    "</title></head><body>" + cookieString + "</body></html>"));
        } else if (path.equals(USERAGENT_PATH)) {
            response = createResponse(HttpStatus.SC_OK);
            Header agentHeader = request.getFirstHeader("User-Agent");
            String agent = "";
            if (agentHeader != null) {
                agent = agentHeader.getValue();
            }
            response.setEntity(createEntity("<html><head><title>" + agent + "</title></head>" +
                    "<body>" + agent + "</body></html>"));
        } else if (path.equals(TEST_DOWNLOAD_PATH)) {
            response = createTestDownloadResponse(Uri.parse(uriString));
        } else if (path.equals(SHUTDOWN_PREFIX)) {
            response = createResponse(HttpStatus.SC_OK);
            // We cannot close the socket here, because we need to respond.
            // Status must be set to OK, or else the test will fail due to
            // a RunTimeException.
        } else if (path.equals(APPCACHE_PATH)) {
            response = createResponse(HttpStatus.SC_OK);
            response.setEntity(createEntity("<!DOCTYPE HTML>" +
                    "<html manifest=\"appcache.manifest\">" +
                    "  <head>" +
                    "    <title>Waiting</title>" +
                    "    <script>" +
                    "      function updateTitle(x) { document.title = x; }" +
                    "      window.applicationCache.onnoupdate = " +
                    "          function() { updateTitle(\"onnoupdate Callback\"); };" +
                    "      window.applicationCache.oncached = " +
                    "          function() { updateTitle(\"oncached Callback\"); };" +
                    "      window.applicationCache.onupdateready = " +
                    "          function() { updateTitle(\"onupdateready Callback\"); };" +
                    "      window.applicationCache.onobsolete = " +
                    "          function() { updateTitle(\"onobsolete Callback\"); };" +
                    "      window.applicationCache.onerror = " +
                    "          function() { updateTitle(\"onerror Callback\"); };" +
                    "    </script>" +
                    "  </head>" +
                    "  <body onload=\"updateTitle('Loaded');\">AppCache test</body>" +
                    "</html>"));
        } else if (path.equals(APPCACHE_MANIFEST_PATH)) {
            response = createResponse(HttpStatus.SC_OK);
            try {
                StringEntity entity = new StringEntity("CACHE MANIFEST");
                // This entity property is not used when constructing the response, (See
                // AbstractMessageWriter.write(), which is called by
                // AbstractHttpServerConnection.sendResponseHeader()) so we have to set this header
                // manually.
                // TODO: Should we do this for all responses from this server?
                entity.setContentType("text/cache-manifest");
                response.setEntity(entity);
                response.setHeader("Content-Type", "text/cache-manifest");
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, "Unexpected UnsupportedEncodingException");
            }
        }
        if (response == null) {
            response = createResponse(HttpStatus.SC_NOT_FOUND);
        }
        StatusLine sl = response.getStatusLine();
        Log.i(TAG, sl.getStatusCode() + "(" + sl.getReasonPhrase() + ")");
        setDateHeaders(response);
        return response;
    }

    private void setDateHeaders(HttpResponse response) {
        long time = System.currentTimeMillis();
        synchronized (this) {
            if (mDocValidity != 0) {
                String expires = DateUtils.formatDate(new Date(time + mDocValidity),
                        DateUtils.PATTERN_RFC1123);
                response.addHeader("Expires", expires);
            }
            if (mDocAge != 0) {
                String modified = DateUtils.formatDate(new Date(time - mDocAge),
                        DateUtils.PATTERN_RFC1123);
                response.addHeader("Last-Modified", modified);
            }
        }
        response.addHeader("Date", DateUtils.formatDate(new Date(), DateUtils.PATTERN_RFC1123));
    }

    /**
     * Create an empty response with the given status.
     */
    private static HttpResponse createResponse(int status) {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_0, status, null);

        // Fill in error reason. Avoid use of the ReasonPhraseCatalog, which is Locale-dependent.
        String reason = getReasonString(status);
        if (reason != null) {
            StringBuffer buf = new StringBuffer("<html><head><title>");
            buf.append(reason);
            buf.append("</title></head><body>");
            buf.append(reason);
            buf.append("</body></html>");
            response.setEntity(createEntity(buf.toString()));
        }
        return response;
    }

    /**
     * Create a string entity for the given content.
     */
    private static StringEntity createEntity(String content) {
        try {
            StringEntity entity = new StringEntity(content);
            entity.setContentType("text/html");
            return entity;
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, e);
        }
        return null;
    }

    private static HttpResponse createTestDownloadResponse(Uri uri) throws IOException {
        String downloadId = uri.getQueryParameter(DOWNLOAD_ID_PARAMETER);
        int numBytes = uri.getQueryParameter(NUM_BYTES_PARAMETER) != null
                ? Integer.parseInt(uri.getQueryParameter(NUM_BYTES_PARAMETER))
                : 0;
        HttpResponse response = createResponse(HttpStatus.SC_OK);
        response.setHeader("Content-Length", Integer.toString(numBytes));
        response.setEntity(createFileEntity(downloadId, numBytes));
        return response;
    }

    private static FileEntity createFileEntity(String downloadId, int numBytes) throws IOException {
        String storageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equalsIgnoreCase(storageState)) {
            File storageDir = Environment.getExternalStorageDirectory();
            File file = new File(storageDir, downloadId + ".bin");
            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
            byte data[] = new byte[1024];
            for (int i = 0; i < data.length; i++) {
                data[i] = 1;
            }
            try {
                for (int i = 0; i < numBytes / data.length; i++) {
                    stream.write(data);
                }
                stream.write(data, 0, numBytes % data.length);
                stream.flush();
            } finally {
                stream.close();
            }
            return new FileEntity(file, "application/octet-stream");
        } else {
            throw new IllegalStateException("External storage must be mounted for this test!");
        }
    }

    protected DefaultHttpServerConnection createHttpServerConnection() {
        return new DefaultHttpServerConnection();
    }

    private static class ServerThread extends Thread {
        private CtsTestServer mServer;
        private ServerSocket mSocket;
        private boolean mIsSsl;
        private boolean mIsCancelled;
        private SSLContext mSslContext;
        private ExecutorService mExecutorService = Executors.newFixedThreadPool(20);

        /**
         * Defines the keystore contents for the server, BKS version. Holds just a
         * single self-generated key. The subject name is "Test Server".
         */
        private static final String SERVER_KEYS_BKS =
            "AAAAAQAAABQDkebzoP1XwqyWKRCJEpn/t8dqIQAABDkEAAVteWtleQAAARpYl20nAAAAAQAFWC41" +
            "MDkAAAJNMIICSTCCAbKgAwIBAgIESEfU1jANBgkqhkiG9w0BAQUFADBpMQswCQYDVQQGEwJVUzET" +
            "MBEGA1UECBMKQ2FsaWZvcm5pYTEMMAoGA1UEBxMDTVRWMQ8wDQYDVQQKEwZHb29nbGUxEDAOBgNV" +
            "BAsTB0FuZHJvaWQxFDASBgNVBAMTC1Rlc3QgU2VydmVyMB4XDTA4MDYwNTExNTgxNFoXDTA4MDkw" +
            "MzExNTgxNFowaTELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExDDAKBgNVBAcTA01U" +
            "VjEPMA0GA1UEChMGR29vZ2xlMRAwDgYDVQQLEwdBbmRyb2lkMRQwEgYDVQQDEwtUZXN0IFNlcnZl" +
            "cjCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA0LIdKaIr9/vsTq8BZlA3R+NFWRaH4lGsTAQy" +
            "DPMF9ZqEDOaL6DJuu0colSBBBQ85hQTPa9m9nyJoN3pEi1hgamqOvQIWcXBk+SOpUGRZZFXwniJV" +
            "zDKU5nE9MYgn2B9AoiH3CSuMz6HRqgVaqtppIe1jhukMc/kHVJvlKRNy9XMCAwEAATANBgkqhkiG" +
            "9w0BAQUFAAOBgQC7yBmJ9O/eWDGtSH9BH0R3dh2NdST3W9hNZ8hIa8U8klhNHbUCSSktZmZkvbPU" +
            "hse5LI3dh6RyNDuqDrbYwcqzKbFJaq/jX9kCoeb3vgbQElMRX8D2ID1vRjxwlALFISrtaN4VpWzV" +
            "yeoHPW4xldeZmoVtjn8zXNzQhLuBqX2MmAAAAqwAAAAUvkUScfw9yCSmALruURNmtBai7kQAAAZx" +
            "4Jmijxs/l8EBaleaUru6EOPioWkUAEVWCxjM/TxbGHOi2VMsQWqRr/DZ3wsDmtQgw3QTrUK666sR" +
            "MBnbqdnyCyvM1J2V1xxLXPUeRBmR2CXorYGF9Dye7NkgVdfA+9g9L/0Au6Ugn+2Cj5leoIgkgApN" +
            "vuEcZegFlNOUPVEs3SlBgUF1BY6OBM0UBHTPwGGxFBBcetcuMRbUnu65vyDG0pslT59qpaR0TMVs" +
            "P+tcheEzhyjbfM32/vwhnL9dBEgM8qMt0sqF6itNOQU/F4WGkK2Cm2v4CYEyKYw325fEhzTXosck" +
            "MhbqmcyLab8EPceWF3dweoUT76+jEZx8lV2dapR+CmczQI43tV9btsd1xiBbBHAKvymm9Ep9bPzM" +
            "J0MQi+OtURL9Lxke/70/MRueqbPeUlOaGvANTmXQD2OnW7PISwJ9lpeLfTG0LcqkoqkbtLKQLYHI" +
            "rQfV5j0j+wmvmpMxzjN3uvNajLa4zQ8l0Eok9SFaRr2RL0gN8Q2JegfOL4pUiHPsh64WWya2NB7f" +
            "V+1s65eA5ospXYsShRjo046QhGTmymwXXzdzuxu8IlnTEont6P4+J+GsWk6cldGbl20hctuUKzyx" +
            "OptjEPOKejV60iDCYGmHbCWAzQ8h5MILV82IclzNViZmzAapeeCnexhpXhWTs+xDEYSKEiG/camt" +
            "bhmZc3BcyVJrW23PktSfpBQ6D8ZxoMfF0L7V2GQMaUg+3r7ucrx82kpqotjv0xHghNIm95aBr1Qw" +
            "1gaEjsC/0wGmmBDg1dTDH+F1p9TInzr3EFuYD0YiQ7YlAHq3cPuyGoLXJ5dXYuSBfhDXJSeddUkl" +
            "k1ufZyOOcskeInQge7jzaRfmKg3U94r+spMEvb0AzDQVOKvjjo1ivxMSgFRZaDb/4qw=";

        private String PASSWORD = "android";

        /**
         * Loads a keystore from a base64-encoded String. Returns the KeyManager[]
         * for the result.
         */
        private KeyManager[] getKeyManagers() throws Exception {
            byte[] bytes = Base64.decode(SERVER_KEYS_BKS.getBytes());
            InputStream inputStream = new ByteArrayInputStream(bytes);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(inputStream, PASSWORD.toCharArray());
            inputStream.close();

            String algorithm = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(algorithm);
            keyManagerFactory.init(keyStore, PASSWORD.toCharArray());

            return keyManagerFactory.getKeyManagers();
        }


        public ServerThread(CtsTestServer server, boolean ssl) throws Exception {
            super("ServerThread");
            mServer = server;
            mIsSsl = ssl;
            int retry = 3;
            while (true) {
                try {
                    if (mIsSsl) {
                        mSslContext = SSLContext.getInstance("TLS");
                        mSslContext.init(getKeyManagers(), null, null);
                        mSocket = mSslContext.getServerSocketFactory().createServerSocket(0);
                    } else {
                        mSocket = new ServerSocket(0);
                    }
                    return;
                } catch (IOException e) {
                    Log.w(TAG, e);
                    if (--retry == 0) {
                        throw e;
                    }
                    // sleep in case server socket is still being closed
                    Thread.sleep(1000);
                }
            }
        }

        public void run() {
            while (!mIsCancelled) {
                try {
                    Socket socket = mSocket.accept();

                    DefaultHttpServerConnection conn = mServer.createHttpServerConnection();
                    HttpParams params = new BasicHttpParams();
                    params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_0);
                    conn.bind(socket, params);

                    // Determine whether we need to shutdown early before
                    // parsing the response since conn.close() will crash
                    // for SSL requests due to UnsupportedOperationException.
                    HttpRequest request = conn.receiveRequestHeader();
                    if (isShutdownRequest(request)) {
                        mIsCancelled = true;
                    }
                    if (request instanceof HttpEntityEnclosingRequest) {
                        conn.receiveRequestEntity( (HttpEntityEnclosingRequest) request);
                    }

                    mExecutorService.submit(new HandleResponseTask(conn, request));
                } catch (IOException e) {
                    // normal during shutdown, ignore
                    Log.w(TAG, e);
                } catch (HttpException e) {
                    Log.w(TAG, e);
                } catch (UnsupportedOperationException e) {
                    // DefaultHttpServerConnection's close() throws an
                    // UnsupportedOperationException.
                    Log.w(TAG, e);
                }
            }
            try {
                mExecutorService.shutdown();
                mExecutorService.awaitTermination(1L, TimeUnit.MINUTES);
                mSocket.close();
            } catch (IOException ignored) {
                // safe to ignore
            } catch (InterruptedException e) {
                Log.e(TAG, "Shutting down threads", e);
            }
        }

        private static boolean isShutdownRequest(HttpRequest request) {
            RequestLine requestLine = request.getRequestLine();
            String uriString = requestLine.getUri();
            URI uri = URI.create(uriString);
            String path = uri.getPath();
            return path.equals(SHUTDOWN_PREFIX);
        }

        private class HandleResponseTask implements Callable<Void> {

            private DefaultHttpServerConnection mConnection;

            private HttpRequest mRequest;

            public HandleResponseTask(DefaultHttpServerConnection connection,
                    HttpRequest request) {
                this.mConnection = connection;
                this.mRequest = request;
            }

            @Override
            public Void call() throws Exception {
                HttpResponse response = mServer.getResponse(mRequest);
                mConnection.sendResponseHeader(response);
                mConnection.sendResponseEntity(response);
                mConnection.close();
                return null;
            }
        }
    }
}
