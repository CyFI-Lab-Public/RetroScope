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

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;

public final class ConversationUtils {
	private static final int MAX_CHARACTERS_PER_PAGE = 250;
	
	public static class ConversationPage {
        public int imageResource;
        public CharSequence text;
        public String title;
    }
	
	public static class Conversation {
		public ArrayList<ConversationPage> pages = new ArrayList<ConversationPage>();
		public boolean splittingComplete;
	}
	
	public final static ArrayList<Conversation> loadDialog(int resource, Context context) {
        XmlResourceParser parser = context.getResources().getXml(resource);
        
        ArrayList<Conversation> dialog = null;
        Conversation currentConversation = null;
        
        try { 
            int eventType = parser.getEventType(); 
            while (eventType != XmlPullParser.END_DOCUMENT) { 
                if (eventType == XmlPullParser.START_TAG) { 
                	if (parser.getName().equals("conversation")) {
                		if (dialog == null) {
                			dialog = new ArrayList<Conversation>();
                		}
                		currentConversation = new Conversation();
                		currentConversation.splittingComplete = false;
                		dialog.add(currentConversation);
                	} else if (parser.getName().equals("page")) { 
                        ConversationPage page = new ConversationPage();
                        for (int i=0; i < parser.getAttributeCount(); i++) { 
                                final int value = parser.getAttributeResourceValue(i, -1);
                                if (value != -1) {
                                    if (parser.getAttributeName(i).equals("image")) {
                                        page.imageResource = value;
                                    }
                                    if (parser.getAttributeName(i).equals("text")) {
                                        page.text = context.getText(value);
                                    }
                                    if (parser.getAttributeName(i).equals("title")) {
                                        page.title = context.getString(value);
                                    }
                                }
                        }
                        currentConversation.pages.add(page);
                    } 
                } 
                eventType = parser.next(); 
            } 
        } catch(Exception e) { 
                DebugLog.e("LoadDialog", e.getStackTrace().toString()); 
        } finally { 
            parser.close(); 
        } 
        
        return dialog;
    }

}
