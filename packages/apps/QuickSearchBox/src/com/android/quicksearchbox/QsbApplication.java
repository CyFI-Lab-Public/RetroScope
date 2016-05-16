/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.quicksearchbox;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.view.ContextThemeWrapper;

import com.android.quicksearchbox.google.GoogleSource;
import com.android.quicksearchbox.google.GoogleSuggestClient;
import com.android.quicksearchbox.google.SearchBaseUrlHelper;
import com.android.quicksearchbox.ui.DefaultSuggestionViewFactory;
import com.android.quicksearchbox.ui.SuggestionViewFactory;
import com.android.quicksearchbox.util.Factory;
import com.android.quicksearchbox.util.HttpHelper;
import com.android.quicksearchbox.util.JavaNetHttpHelper;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.PerNameExecutor;
import com.android.quicksearchbox.util.PriorityThreadFactory;
import com.android.quicksearchbox.util.SingleThreadNamedTaskExecutor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class QsbApplication {
    private final Context mContext;

    private int mVersionCode;
    private Handler mUiThreadHandler;
    private Config mConfig;
    private SearchSettings mSettings;
    private NamedTaskExecutor mSourceTaskExecutor;
    private ThreadFactory mQueryThreadFactory;
    private SuggestionsProvider mSuggestionsProvider;
    private SuggestionViewFactory mSuggestionViewFactory;
    private GoogleSource mGoogleSource;
    private VoiceSearch mVoiceSearch;
    private Logger mLogger;
    private SuggestionFormatter mSuggestionFormatter;
    private TextAppearanceFactory mTextAppearanceFactory;
    private NamedTaskExecutor mIconLoaderExecutor;
    private HttpHelper mHttpHelper;
    private SearchBaseUrlHelper mSearchBaseUrlHelper;

    public QsbApplication(Context context) {
        // the application context does not use the theme from the <application> tag
        mContext = new ContextThemeWrapper(context, R.style.Theme_QuickSearchBox);
    }

    public static boolean isFroyoOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean isHoneycombOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static QsbApplication get(Context context) {
        return ((QsbApplicationWrapper) context.getApplicationContext()).getApp();
    }

    protected Context getContext() {
        return mContext;
    }

    public int getVersionCode() {
        if (mVersionCode == 0) {
            try {
                PackageManager pm = getContext().getPackageManager();
                PackageInfo pkgInfo = pm.getPackageInfo(getContext().getPackageName(), 0);
                mVersionCode = pkgInfo.versionCode;
            } catch (PackageManager.NameNotFoundException ex) {
                // The current package should always exist, how else could we
                // run code from it?
                throw new RuntimeException(ex);
            }
        }
        return mVersionCode;
    }

    protected void checkThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Accessed Application object from thread "
                    + Thread.currentThread().getName());
        }
    }

    protected void close() {
        checkThread();
        if (mConfig != null) {
            mConfig.close();
            mConfig = null;
        }
        if (mSuggestionsProvider != null) {
            mSuggestionsProvider.close();
            mSuggestionsProvider = null;
        }
    }

    public synchronized Handler getMainThreadHandler() {
        if (mUiThreadHandler == null) {
            mUiThreadHandler = new Handler(Looper.getMainLooper());
        }
        return mUiThreadHandler;
    }

    public void runOnUiThread(Runnable action) {
        getMainThreadHandler().post(action);
    }

    public synchronized NamedTaskExecutor getIconLoaderExecutor() {
        if (mIconLoaderExecutor == null) {
            mIconLoaderExecutor = createIconLoaderExecutor();
        }
        return mIconLoaderExecutor;
    }

    protected NamedTaskExecutor createIconLoaderExecutor() {
        ThreadFactory iconThreadFactory = new PriorityThreadFactory(
                    Process.THREAD_PRIORITY_BACKGROUND);
        return new PerNameExecutor(SingleThreadNamedTaskExecutor.factory(iconThreadFactory));
    }

    /**
     * Indicates that construction of the QSB UI is now complete.
     */
    public void onStartupComplete() {
    }

    /**
     * Gets the QSB configuration object.
     * May be called from any thread.
     */
    public synchronized Config getConfig() {
        if (mConfig == null) {
            mConfig = createConfig();
        }
        return mConfig;
    }

    protected Config createConfig() {
        return new Config(getContext());
    }

    public synchronized SearchSettings getSettings() {
        if (mSettings == null) {
            mSettings = createSettings();
            mSettings.upgradeSettingsIfNeeded();
        }
        return mSettings;
    }

    protected SearchSettings createSettings() {
        return new SearchSettingsImpl(getContext(), getConfig());
    }

    protected Factory<Executor> createExecutorFactory(final int numThreads) {
        final ThreadFactory threadFactory = getQueryThreadFactory();
        return new Factory<Executor>() {
            @Override
            public Executor create() {
                return Executors.newFixedThreadPool(numThreads, threadFactory);
            }
        };
    }

    /**
    /**
     * Gets the source task executor.
     * May only be called from the main thread.
     */
    public NamedTaskExecutor getSourceTaskExecutor() {
        checkThread();
        if (mSourceTaskExecutor == null) {
            mSourceTaskExecutor = createSourceTaskExecutor();
        }
        return mSourceTaskExecutor;
    }

    protected NamedTaskExecutor createSourceTaskExecutor() {
        ThreadFactory queryThreadFactory = getQueryThreadFactory();
        return new PerNameExecutor(SingleThreadNamedTaskExecutor.factory(queryThreadFactory));
    }

    /**
     * Gets the query thread factory.
     * May only be called from the main thread.
     */
    protected ThreadFactory getQueryThreadFactory() {
        checkThread();
        if (mQueryThreadFactory == null) {
            mQueryThreadFactory = createQueryThreadFactory();
        }
        return mQueryThreadFactory;
    }

    protected ThreadFactory createQueryThreadFactory() {
        String nameFormat = "QSB #%d";
        int priority = getConfig().getQueryThreadPriority();
        return new ThreadFactoryBuilder()
                .setNameFormat(nameFormat)
                .setThreadFactory(new PriorityThreadFactory(priority))
                .build();
    }

    /**
     * Gets the suggestion provider.
     *
     * May only be called from the main thread.
     */
    protected SuggestionsProvider getSuggestionsProvider() {
        checkThread();
        if (mSuggestionsProvider == null) {
            mSuggestionsProvider = createSuggestionsProvider();
        }
        return mSuggestionsProvider;
    }

    protected SuggestionsProvider createSuggestionsProvider() {
        return new SuggestionsProviderImpl(getConfig(),
              getSourceTaskExecutor(),
              getMainThreadHandler(),
              getLogger());
    }

    /**
     * Gets the default suggestion view factory.
     * May only be called from the main thread.
     */
    public SuggestionViewFactory getSuggestionViewFactory() {
        checkThread();
        if (mSuggestionViewFactory == null) {
            mSuggestionViewFactory = createSuggestionViewFactory();
        }
        return mSuggestionViewFactory;
    }

    protected SuggestionViewFactory createSuggestionViewFactory() {
        return new DefaultSuggestionViewFactory(getContext());
    }

    /**
     * Gets the Google source.
     * May only be called from the main thread.
     */
    public GoogleSource getGoogleSource() {
        checkThread();
        if (mGoogleSource == null) {
            mGoogleSource = createGoogleSource();
        }
        return mGoogleSource;
    }

    protected GoogleSource createGoogleSource() {
        return new GoogleSuggestClient(getContext(), getMainThreadHandler(),
                getIconLoaderExecutor(), getConfig());
    }

    /**
     * Gets Voice Search utilities.
     */
    public VoiceSearch getVoiceSearch() {
        checkThread();
        if (mVoiceSearch == null) {
            mVoiceSearch = createVoiceSearch();
        }
        return mVoiceSearch;
    }

    protected VoiceSearch createVoiceSearch() {
        return new VoiceSearch(getContext());
    }

    /**
     * Gets the event logger.
     * May only be called from the main thread.
     */
    public Logger getLogger() {
        checkThread();
        if (mLogger == null) {
            mLogger = createLogger();
        }
        return mLogger;
    }

    protected Logger createLogger() {
        return new EventLogLogger(getContext(), getConfig());
    }

    public SuggestionFormatter getSuggestionFormatter() {
        if (mSuggestionFormatter == null) {
            mSuggestionFormatter = createSuggestionFormatter();
        }
        return mSuggestionFormatter;
    }

    protected SuggestionFormatter createSuggestionFormatter() {
        return new LevenshteinSuggestionFormatter(getTextAppearanceFactory());
    }

    public TextAppearanceFactory getTextAppearanceFactory() {
        if (mTextAppearanceFactory == null) {
            mTextAppearanceFactory = createTextAppearanceFactory();
        }
        return mTextAppearanceFactory;
    }

    protected TextAppearanceFactory createTextAppearanceFactory() {
        return new TextAppearanceFactory(getContext());
    }

    public synchronized HttpHelper getHttpHelper() {
        if (mHttpHelper == null) {
            mHttpHelper = createHttpHelper();
        }
        return mHttpHelper;
    }

    protected HttpHelper createHttpHelper() {
        return new JavaNetHttpHelper(
                new JavaNetHttpHelper.PassThroughRewriter(),
                getConfig().getUserAgent());
    }

    public synchronized SearchBaseUrlHelper getSearchBaseUrlHelper() {
        if (mSearchBaseUrlHelper == null) {
            mSearchBaseUrlHelper = createSearchBaseUrlHelper();
        }

        return mSearchBaseUrlHelper;
    }

    protected SearchBaseUrlHelper createSearchBaseUrlHelper() {
        // This cast to "SearchSettingsImpl" is somewhat ugly.
        return new SearchBaseUrlHelper(getContext(), getHttpHelper(),
                getSettings(), ((SearchSettingsImpl)getSettings()).getSearchPreferences());
    }

    public Help getHelp() {
        // No point caching this, it's super cheap.
        return new Help(getContext(), getConfig());
    }
}
