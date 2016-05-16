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

package android.webkit.cts;

import android.cts.util.PollingCheck;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.print.PrintDocumentAdapter;
import android.test.InstrumentationTestCase;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebView.PictureListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import junit.framework.Assert;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.Map;

/**
 * Many tests need to run WebView code in the UI thread. This class
 * wraps a WebView so that calls are ensured to arrive on the UI thread.
 *
 * All methods may be run on either the UI thread or test thread.
 */
public class WebViewOnUiThread {
    /**
     * The maximum time, in milliseconds (10 seconds) to wait for a load
     * to be triggered.
     */
    private static final long LOAD_TIMEOUT = 10000;

    /**
     * Set to true after onPageFinished is called.
     */
    private boolean mLoaded;

    /**
     * Set to true after onNewPicture is called. Reset when onPageStarted
     * is called.
     */
    private boolean mNewPicture;

    /**
     * The progress, in percentage, of the page load. Valid values are between
     * 0 and 100.
     */
    private int mProgress;

    /**
     * The test that this class is being used in. Used for runTestOnUiThread.
     */
    private InstrumentationTestCase mTest;

    /**
     * The WebView that calls will be made on.
     */
    private WebView mWebView;

    /**
     * Initializes the webView with a WebViewClient, WebChromeClient,
     * and PictureListener to prepare for loadUrlAndWaitForCompletion.
     *
     * A new WebViewOnUiThread should be called during setUp so as to
     * reinitialize between calls.
     *
     * @param test The test in which this is being run.
     * @param webView The webView that the methods should call.
     * @see loadUrlAndWaitForCompletion
     */
    public WebViewOnUiThread(InstrumentationTestCase test, WebView webView) {
        mTest = test;
        mWebView = webView;
        final WebViewClient webViewClient = new WaitForLoadedClient(this);
        final WebChromeClient webChromeClient = new WaitForProgressClient(this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.setWebViewClient(webViewClient);
                mWebView.setWebChromeClient(webChromeClient);
                mWebView.setPictureListener(new WaitForNewPicture());
            }
        });
    }

    /**
     * Called after a test is complete and the WebView should be disengaged from
     * the tests.
     */
    public void cleanUp() {
        clearHistory();
        clearCache(true);
        setPictureListener(null);
        setWebChromeClient(null);
        setWebViewClient(null);
    }

    /**
     * Called from WaitForNewPicture, this is used to indicate that
     * the page has been drawn.
     */
    synchronized public void onNewPicture() {
        mNewPicture = true;
        this.notifyAll();
    }

    /**
     * Called from WaitForLoadedClient, this is used to clear the picture
     * draw state so that draws before the URL begins loading don't count.
     */
    synchronized public void onPageStarted() {
        mNewPicture = false; // Earlier paints won't count.
    }

    /**
     * Called from WaitForLoadedClient, this is used to indicate that
     * the page is loaded, but not drawn yet.
     */
    synchronized public void onPageFinished() {
        mLoaded = true;
        this.notifyAll();
    }

    /**
     * Called from the WebChrome client, this sets the current progress
     * for a page.
     * @param progress The progress made so far between 0 and 100.
     */
    synchronized public void onProgressChanged(int progress) {
        mProgress = progress;
        this.notifyAll();
    }

    public void setWebViewClient(final WebViewClient webViewClient) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.setWebViewClient(webViewClient);
            }
        });
    }

    public void setWebChromeClient(final WebChromeClient webChromeClient) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.setWebChromeClient(webChromeClient);
            }
        });
    }

    public void setPictureListener(final PictureListener pictureListener) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.setPictureListener(pictureListener);
            }
        });
    }

    public void setNetworkAvailable(final boolean available) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.setNetworkAvailable(available);
            }
        });
    }

    public void setDownloadListener(final DownloadListener listener) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.setDownloadListener(listener);
            }
        });
    }

    public void setBackgroundColor(final int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.setBackgroundColor(color);
            }
        });
    }

    public void clearCache(final boolean includeDiskFiles) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.clearCache(includeDiskFiles);
            }
        });
    }

    public void clearHistory() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.clearHistory();
            }
        });
    }

    public void requestFocus() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.requestFocus();
            }
        });
    }

    public boolean zoomIn() {
        return getValue(new ValueGetter<Boolean>() {
            @Override
            public Boolean capture() {
                return mWebView.zoomIn();
            }
        });
    }

    public boolean zoomOut() {
        return getValue(new ValueGetter<Boolean>() {
            @Override
            public Boolean capture() {
                return mWebView.zoomOut();
            }
        });
    }

    public void setFindListener(final WebView.FindListener listener) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.setFindListener(listener);
            }
        });
    }

    public void removeJavascriptInterface(final String interfaceName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.removeJavascriptInterface(interfaceName);
            }
        });
    }

    public void addJavascriptInterface(final Object object, final String name) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.addJavascriptInterface(object, name);
            }
        });
    }

    public void flingScroll(final int vx, final int vy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.flingScroll(vx, vy);
            }
        });
    }

    public void requestFocusNodeHref(final Message hrefMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.requestFocusNodeHref(hrefMsg);
            }
        });
    }

    public void requestImageRef(final Message msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.requestImageRef(msg);
            }
        });
    }

    public void setInitialScale(final int scaleInPercent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.setInitialScale(scaleInPercent);
            }
        });
    }

    public void clearSslPreferences() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.clearSslPreferences();
            }
        });
    }

    public void resumeTimers() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.resumeTimers();
            }
        });
    }

    public void findNext(final boolean forward) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.findNext(forward);
            }
        });
    }

    public void clearMatches() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.clearMatches();
            }
        });
    }

    /**
     * Calls loadUrl on the WebView and then waits onPageFinished,
     * onNewPicture and onProgressChange to reach 100.
     * Test fails if the load timeout elapses.
     * @param url The URL to load.
     */
    public void loadUrlAndWaitForCompletion(final String url) {
        callAndWait(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(url);
            }
        });
    }

    /**
     * Calls loadUrl on the WebView and then waits onPageFinished,
     * onNewPicture and onProgressChange to reach 100.
     * Test fails if the load timeout elapses.
     * @param url The URL to load.
     */
    public void loadUrlAndWaitForCompletion(final String url,
            final Map<String, String> extraHeaders) {
        callAndWait(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(url, extraHeaders);
            }
        });
    }

    public void loadUrl(final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(url);
            }
        });
    }

    public void stopLoading() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.stopLoading();
            }
        });
    }

    public void loadDataAndWaitForCompletion(final String data,
            final String mimeType, final String encoding) {
        callAndWait(new Runnable() {
            @Override
            public void run() {
                mWebView.loadData(data, mimeType, encoding);
            }
        });
    }

    public void loadDataWithBaseURLAndWaitForCompletion(final String baseUrl,
            final String data, final String mimeType, final String encoding,
            final String historyUrl) {
        callAndWait(new Runnable() {
            @Override
            public void run() {
                mWebView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding,
                        historyUrl);
            }
        });
    }

    /**
     * Reloads a page and waits for it to complete reloading. Use reload
     * if it is a form resubmission and the onFormResubmission responds
     * by telling WebView not to resubmit it.
     */
    public void reloadAndWaitForCompletion() {
        callAndWait(new Runnable() {
            @Override
            public void run() {
                mWebView.reload();
            }
        });
    }

    /**
     * Reload the previous URL. Use reloadAndWaitForCompletion unless
     * it is a form resubmission and the onFormResubmission responds
     * by telling WebView not to resubmit it.
     */
    public void reload() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.reload();
            }
        });
    }

    /**
     * Use this only when JavaScript causes a page load to wait for the
     * page load to complete. Otherwise use loadUrlAndWaitForCompletion or
     * similar functions.
     */
    public void waitForLoadCompletion() {
        waitForCriteria(LOAD_TIMEOUT,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        return isLoaded();
                    }
                });
        clearLoad();
    }

    private void waitForCriteria(long timeout, Callable<Boolean> doneCriteria) {
        if (isUiThread()) {
            waitOnUiThread(timeout, doneCriteria);
        } else {
            waitOnTestThread(timeout, doneCriteria);
        }
    }

    public String getTitle() {
        return getValue(new ValueGetter<String>() {
            @Override
            public String capture() {
                return mWebView.getTitle();
            }
        });
    }

    public WebSettings getSettings() {
        return getValue(new ValueGetter<WebSettings>() {
            @Override
            public WebSettings capture() {
                return mWebView.getSettings();
            }
        });
    }

    public WebBackForwardList copyBackForwardList() {
        return getValue(new ValueGetter<WebBackForwardList>() {
            @Override
            public WebBackForwardList capture() {
                return mWebView.copyBackForwardList();
            }
        });
    }

    public Bitmap getFavicon() {
        return getValue(new ValueGetter<Bitmap>() {
            @Override
            public Bitmap capture() {
                return mWebView.getFavicon();
            }
        });
    }

    public String getUrl() {
        return getValue(new ValueGetter<String>() {
            @Override
            public String capture() {
                return mWebView.getUrl();
            }
        });
    }

    public int getProgress() {
        return getValue(new ValueGetter<Integer>() {
            @Override
            public Integer capture() {
                return mWebView.getProgress();
            }
        });
    }

    public int getHeight() {
        return getValue(new ValueGetter<Integer>() {
            @Override
            public Integer capture() {
                return mWebView.getHeight();
            }
        });
    }

    public int getContentHeight() {
        return getValue(new ValueGetter<Integer>() {
            @Override
            public Integer capture() {
                return mWebView.getContentHeight();
            }
        });
    }

    public boolean savePicture(final Bundle b, final File dest) {
        return getValue(new ValueGetter<Boolean>() {
            @Override
            public Boolean capture() {
                return mWebView.savePicture(b, dest);
            }
        });
    }

    public boolean pageUp(final boolean top) {
        return getValue(new ValueGetter<Boolean>() {
            @Override
            public Boolean capture() {
                return mWebView.pageUp(top);
            }
        });
    }

    public boolean pageDown(final boolean bottom) {
        return getValue(new ValueGetter<Boolean>() {
            @Override
            public Boolean capture() {
                return mWebView.pageDown(bottom);
            }
        });
    }

    public int[] getLocationOnScreen() {
        final int[] location = new int[2];
        return getValue(new ValueGetter<int[]>() {
            @Override
            public int[] capture() {
                mWebView.getLocationOnScreen(location);
                return location;
            }
        });
    }

    public float getScale() {
        return getValue(new ValueGetter<Float>() {
            @Override
            public Float capture() {
                return mWebView.getScale();
            }
        });
    }

    public boolean requestFocus(final int direction,
            final Rect previouslyFocusedRect) {
        return getValue(new ValueGetter<Boolean>() {
            @Override
            public Boolean capture() {
                return mWebView.requestFocus(direction, previouslyFocusedRect);
            }
        });
    }

    public HitTestResult getHitTestResult() {
        return getValue(new ValueGetter<HitTestResult>() {
            @Override
            public HitTestResult capture() {
                return mWebView.getHitTestResult();
            }
        });
    }

    public int getScrollX() {
        return getValue(new ValueGetter<Integer>() {
            @Override
            public Integer capture() {
                return mWebView.getScrollX();
            }
        });
    }

    public int getScrollY() {
        return getValue(new ValueGetter<Integer>() {
            @Override
            public Integer capture() {
                return mWebView.getScrollY();
            }
        });
    }

    public final DisplayMetrics getDisplayMetrics() {
        return getValue(new ValueGetter<DisplayMetrics>() {
            @Override
            public DisplayMetrics capture() {
                return mWebView.getContext().getResources().getDisplayMetrics();
            }
        });
    }

    public boolean requestChildRectangleOnScreen(final View child,
            final Rect rect,
            final boolean immediate) {
        return getValue(new ValueGetter<Boolean>() {
            @Override
            public Boolean capture() {
                return mWebView.requestChildRectangleOnScreen(child, rect,
                        immediate);
            }
        });
    }

    public int findAll(final String find) {
        return getValue(new ValueGetter<Integer>() {
            @Override
            public Integer capture() {
                return mWebView.findAll(find);
            }
        });
    }

    public Picture capturePicture() {
        return getValue(new ValueGetter<Picture>() {
            @Override
            public Picture capture() {
                return mWebView.capturePicture();
            }
        });
    }

    public void evaluateJavascript(final String script, final ValueCallback<String> result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.evaluateJavascript(script, result);
            }
        });
    }

    public PrintDocumentAdapter createPrintDocumentAdapter() {
        return getValue(new ValueGetter<PrintDocumentAdapter>() {
            @Override
            public PrintDocumentAdapter capture() {
                return mWebView.createPrintDocumentAdapter();
            }
        });
    }

    public void setLayoutHeightToMatchParent() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewParent parent = mWebView.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).getLayoutParams().height =
                        ViewGroup.LayoutParams.MATCH_PARENT;
                }
                mWebView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                mWebView.requestLayout();
            }
        });
    }

    /**
     * Helper for running code on the UI thread where an exception is
     * a test failure. If this is already the UI thread then it runs
     * the code immediately.
     *
     * @see runTestOnUiThread
     * @param r The code to run in the UI thread
     */
    public void runOnUiThread(Runnable r) {
        try {
            if (isUiThread()) {
                r.run();
            } else {
                mTest.runTestOnUiThread(r);
            }
        } catch (Throwable t) {
            Assert.fail("Unexpected error while running on UI thread: "
                    + t.getMessage());
        }
    }

    /**
     * Accessor for underlying WebView.
     * @return The WebView being wrapped by this class.
     */
    public WebView getWebView() {
        return mWebView;
    }

    private <T> T getValue(ValueGetter<T> getter) {
        runOnUiThread(getter);
        return getter.getValue();
    }

    private abstract class ValueGetter<T> implements Runnable {
        private T mValue;

        @Override
        public void run() {
            mValue = capture();
        }

        protected abstract T capture();

        public T getValue() {
           return mValue;
        }
    }

    /**
     * Returns true if the current thread is the UI thread based on the
     * Looper.
     */
    private static boolean isUiThread() {
        return (Looper.myLooper() == Looper.getMainLooper());
    }

    /**
     * @return Whether or not the load has finished.
     */
    private synchronized boolean isLoaded() {
        return mLoaded && mNewPicture && mProgress == 100;
    }

    /**
     * Makes a WebView call, waits for completion and then resets the
     * load state in preparation for the next load call.
     * @param call The call to make on the UI thread prior to waiting.
     */
    private void callAndWait(Runnable call) {
        Assert.assertTrue("WebViewOnUiThread.load*AndWaitForCompletion calls "
                + "may not be mixed with load* calls directly on WebView "
                + "without calling waitForLoadCompletion after the load",
                !isLoaded());
        clearLoad(); // clear any extraneous signals from a previous load.
        runOnUiThread(call);
        waitForLoadCompletion();
    }

    /**
     * Called whenever a load has been completed so that a subsequent call to
     * waitForLoadCompletion doesn't return immediately.
     */
    synchronized private void clearLoad() {
        mLoaded = false;
        mNewPicture = false;
        mProgress = 0;
    }

    /**
     * Uses a polling mechanism, while pumping messages to check when the
     * criteria is met.
     */
    private void waitOnUiThread(long timeout, final Callable<Boolean> doneCriteria) {
        new PollingCheck(timeout) {
            @Override
            protected boolean check() {
                pumpMessages();
                try {
                    return doneCriteria.call();
                } catch (Exception e) {
                    Assert.fail("Unexpected error while checking the criteria: "
                            + e.getMessage());
                    return true;
                }
            }
        }.run();
    }

    /**
     * Uses a wait/notify to check when the criteria is met.
     */
    private synchronized void waitOnTestThread(long timeout, Callable<Boolean> doneCriteria) {
        try {
            long waitEnd = SystemClock.uptimeMillis() + timeout;
            long timeRemaining = timeout;
            while (!doneCriteria.call() && timeRemaining > 0) {
                this.wait(timeRemaining);
                timeRemaining = waitEnd - SystemClock.uptimeMillis();
            }
            Assert.assertTrue("Action failed to complete before timeout", doneCriteria.call());
        } catch (InterruptedException e) {
            // We'll just drop out of the loop and fail
        } catch (Exception e) {
            Assert.fail("Unexpected error while checking the criteria: "
                    + e.getMessage());
        }
    }

    /**
     * Pumps all currently-queued messages in the UI thread and then exits.
     * This is useful to force processing while running tests in the UI thread.
     */
    private void pumpMessages() {
        class ExitLoopException extends RuntimeException {
        }

        // Force loop to exit when processing this. Loop.quit() doesn't
        // work because this is the main Loop.
        mWebView.getHandler().post(new Runnable() {
            @Override
            public void run() {
                throw new ExitLoopException(); // exit loop!
            }
        });
        try {
            // Pump messages until our message gets through.
            Looper.loop();
        } catch (ExitLoopException e) {
        }
    }

    /**
     * A WebChromeClient used to capture the onProgressChanged for use
     * in waitFor functions. If a test must override the WebChromeClient,
     * it can derive from this class or call onProgressChanged
     * directly.
     */
    public static class WaitForProgressClient extends WebChromeClient {
        private WebViewOnUiThread mOnUiThread;

        public WaitForProgressClient(WebViewOnUiThread onUiThread) {
            mOnUiThread = onUiThread;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            mOnUiThread.onProgressChanged(newProgress);
        }
    }

    /**
     * A WebViewClient that captures the onPageFinished for use in
     * waitFor functions. Using initializeWebView sets the WaitForLoadedClient
     * into the WebView. If a test needs to set a specific WebViewClient and
     * needs the waitForCompletion capability then it should derive from
     * WaitForLoadedClient or call WebViewOnUiThread.onPageFinished.
     */
    public static class WaitForLoadedClient extends WebViewClient {
        private WebViewOnUiThread mOnUiThread;

        public WaitForLoadedClient(WebViewOnUiThread onUiThread) {
            mOnUiThread = onUiThread;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mOnUiThread.onPageFinished();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mOnUiThread.onPageStarted();
        }
    }

    /**
     * A PictureListener that captures the onNewPicture for use in
     * waitForLoadCompletion. Using initializeWebView sets the PictureListener
     * into the WebView. If a test needs to set a specific PictureListener and
     * needs the waitForCompletion capability then it should call
     * WebViewOnUiThread.onNewPicture.
     */
    private class WaitForNewPicture implements PictureListener {
        @Override
        public void onNewPicture(WebView view, Picture picture) {
            WebViewOnUiThread.this.onNewPicture();
        }
    }
}
