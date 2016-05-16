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
 
 package com.replica.replicaisland;

import android.util.Log;

public final class DebugLog {
	private static boolean mLoggingEnabled = true;
	
	private DebugLog() {
		
	}
	
	public static void setDebugLogging(boolean enabled) {
		mLoggingEnabled = enabled;
	}
	
    public static int v(String tag, String msg) {
        int result = 0;
        if (mLoggingEnabled) {
        	result = Log.v(tag, msg);
        }
        return result;
    }

	public static int v(String tag, String msg, Throwable tr) {
	   int result = 0;
       if (mLoggingEnabled) {
    	   result = Log.v(tag, msg, tr);
       }
       return result;    
	}

     public static int d(String tag, String msg) {
    	 int result = 0;
         if (mLoggingEnabled) {
         	result = Log.d(tag, msg);
         }
         return result;
    }

    public static int d(String tag, String msg, Throwable tr) {
    	int result = 0;
        if (mLoggingEnabled) {
        	result = Log.d(tag, msg, tr);
        }
        return result;
    }

    public static int i(String tag, String msg) {
    	int result = 0;
        if (mLoggingEnabled) {
        	result = Log.i(tag, msg);
        }
        return result;
    }

    public static int i(String tag, String msg, Throwable tr) {
    	int result = 0;
        if (mLoggingEnabled) {
        	result = Log.i(tag, msg, tr);
        }
        return result;
    }

    public static int w(String tag, String msg) {
    	int result = 0;
        if (mLoggingEnabled) {
        	result = Log.w(tag, msg);
        }
        return result;
    }

    public static int w(String tag, String msg, Throwable tr) {
    	int result = 0;
        if (mLoggingEnabled) {
        	result = Log.w(tag, msg, tr);
        }
        return result;
    }

    public static int w(String tag, Throwable tr) {
    	int result = 0;
        if (mLoggingEnabled) {
        	result = Log.w(tag, tr);
        }
        return result;
    }

    public static int e(String tag, String msg) {
    	int result = 0;
        if (mLoggingEnabled) {
        	result = Log.e(tag, msg);
        }
        return result;
    }

    public static int e(String tag, String msg, Throwable tr) {
    	int result = 0;
        if (mLoggingEnabled) {
        	result = Log.e(tag, msg, tr);
        }
        return result;
    }
}
