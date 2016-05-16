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

public final class LevelTree {
	public static class LevelGroup {
		public ArrayList<Level> levels = new ArrayList<Level>();
	}
	
    public static class Level {
    	public int resource;
        public DialogEntry dialogResources;
        public String name;
        public String timeStamp;
        public boolean completed;
        public boolean inThePast;
        public boolean diaryCollected;
        public boolean restartable;
        public boolean showWaitMessage;
        
        public Level(int level, DialogEntry dialogs, String title, String time, 
        		boolean pastEvent, boolean restartOnDeath, boolean waitMessage) {
            resource = level;
            dialogResources = dialogs;
            name = title;
            timeStamp = time;
            completed = false;
            inThePast = pastEvent;
            diaryCollected = false;
            restartable = restartOnDeath;
            showWaitMessage = waitMessage;
        }
        
    }
    
    public static class DialogEntry {
        public int diaryEntry = 0;
        public int character1Entry = 0;
        public int character2Entry = 0;
        public ArrayList<ConversationUtils.Conversation> character1Conversations;
        public ArrayList<ConversationUtils.Conversation> character2Conversations;
    }
    public final static ArrayList<LevelGroup> levels = new ArrayList<LevelGroup>();
    private static boolean mLoaded = false;
    private static int mLoadedResource = 0;
    
    public static final Level get(int row, int index) {
    	return levels.get(row).levels.get(index);
    }
    
    public static final boolean isLoaded(int resource) {
    	return mLoaded && mLoadedResource == resource;
    }
    
    public static final void loadLevelTree(int resource, Context context) {
        if (levels.size() > 0 && mLoadedResource == resource) {
        	// already loaded
        	return;
        }
        
    	XmlResourceParser parser = context.getResources().getXml(resource);
        
        levels.clear();
        
        LevelGroup currentGroup = null;
        Level currentLevel = null;
        DialogEntry currentDialog = null;
        
        try { 
            int eventType = parser.getEventType(); 
            while (eventType != XmlPullParser.END_DOCUMENT) { 
                if(eventType == XmlPullParser.START_TAG) { 
                	if (parser.getName().equals("group")) {
                		currentGroup = new LevelGroup();
                		levels.add(currentGroup);
                		currentLevel = null;
                		currentDialog = null;
                	}
                	
                    if (parser.getName().equals("level") && currentGroup != null) { 
                    	int levelResource = 0;
                    	String titleString = null;
                    	String timeStamp = null;
                    	boolean inThePast = false;
                    	boolean restartable = true;
                    	boolean showWaitMessage = false;
                        for(int i=0; i < parser.getAttributeCount(); i++) { 
                    		if (parser.getAttributeName(i).equals("past")) {
                    			if (parser.getAttributeValue(i).equals("true")) {
                    				inThePast = true;
                    			}
                    		} else if (parser.getAttributeName(i).equals("restartable")) {
                    			if (parser.getAttributeValue(i).equals("false")) {
                    				restartable = false;
                    			}
                    		} else if (parser.getAttributeName(i).equals("waitmessage")) {
                    			if (parser.getAttributeValue(i).equals("true")) {
                    				showWaitMessage = true;
                    			}
                    		} else {
                                final int value = parser.getAttributeResourceValue(i, -1);
                                if (value != -1) {
                                    if (parser.getAttributeName(i).equals("resource")) {
                                        levelResource = value;
                                    }
                                    if (parser.getAttributeName(i).equals("title")) {
                                        titleString = context.getString(value);
                                    } else if (parser.getAttributeName(i).equals("time")) {
                                        timeStamp = context.getString(value);
                                    }
                                }
                    		}
                        	
                        } 
                        currentDialog = null;
                        currentLevel = new Level(levelResource, null, titleString, timeStamp, inThePast, restartable, showWaitMessage);
                        currentGroup.levels.add(currentLevel);
                    } 
                    
                    if (parser.getName().equals("dialog") && currentLevel != null) { 
                    	currentDialog = new DialogEntry();
                    	currentLevel.dialogResources = currentDialog;
                    }
                    
                    if (parser.getName().equals("diary") && currentDialog != null) { 
                    	
                    	for(int i=0; i < parser.getAttributeCount(); i++) { 
                            final int value = parser.getAttributeResourceValue(i, -1);
                            if (value != -1) {
                                if (parser.getAttributeName(i).equals("resource")) {
                                	currentDialog.diaryEntry = value;
                                }
                               
                            }
                    	} 
                    }
                    
                    if (parser.getName().equals("character1") && currentDialog != null) { 
                    	for(int i=0; i < parser.getAttributeCount(); i++) { 
                            final int value = parser.getAttributeResourceValue(i, -1);
                            if (value != -1) {
                                if (parser.getAttributeName(i).equals("resource")) {
                                	currentDialog.character1Entry = value;
                                }
                               
                            }
                    	} 
                    }
                    
                    if (parser.getName().equals("character2") && currentDialog != null) { 
                    	
                    	for(int i=0; i < parser.getAttributeCount(); i++) { 
                            final int value = parser.getAttributeResourceValue(i, -1);
                            if (value != -1) {
                                if (parser.getAttributeName(i).equals("resource")) {
                                	currentDialog.character2Entry = value;
                                }
                               
                            }
                    	}
                    }
                    
                } 
                eventType = parser.next(); 
            } 
        } catch(Exception e) { 
                DebugLog.e("LevelTree", e.getStackTrace().toString()); 
        } finally { 
            parser.close(); 
        } 
        mLoaded = true;
        mLoadedResource = resource;
    }
    
    public final static void loadAllDialog(Context context) {
    	final int levelGroupCount = levels.size();
    	for (int x = 0; x < levelGroupCount; x++) {
    		final ArrayList<Level> row = levels.get(x).levels;
    		final int levelCount = row.size();
    		for (int y = 0; y < levelCount; y++) {
    			final Level level = row.get(y);
    			if (level != null && level.dialogResources != null) {
    				DialogEntry dialog = level.dialogResources;
    				if (dialog.character1Entry != 0) {
    					dialog.character1Conversations = ConversationUtils.loadDialog(dialog.character1Entry, context);
    				}
    				
    				if (dialog.character2Entry != 0) {
    					dialog.character2Conversations = ConversationUtils.loadDialog(dialog.character2Entry, context);
    				}
    			}
    		}
    		
    	}
    }

	public final static void updateCompletedState(int levelRow, int completedLevels) {
		final int rowCount = levels.size();
		for (int x = 0; x < rowCount; x++) {
			final LevelGroup group = levels.get(x);
			final int levelCount = group.levels.size();
			for (int y = 0; y < levelCount; y++) {
				final Level level = group.levels.get(y);
				if (x < levelRow) {
					level.completed = true;
				} else if (x == levelRow) {
					if ((completedLevels & (1 << y)) != 0) {
						level.completed = true;
					}
				} else {
					level.completed = false;
				}
			}
		}
		
	}

	public final static int packCompletedLevels(int levelRow) {
		int completed = 0;
		final LevelGroup group = levels.get(levelRow);
		final int levelCount = group.levels.size();
		for (int y = 0; y < levelCount; y++) {
			final Level level = group.levels.get(y);
			if (level.completed) {
				completed |= 1 << y;
			}
		}
		return completed;
	}

	public static boolean levelIsValid(int row, int index) {
		boolean valid = false;
		if (row >= 0 && row < levels.size()) {
			final LevelGroup group = levels.get(row);
			if (index >=0 && index < group.levels.size()) {
				valid = true;
			}
		}
		
		return valid;
	}
	
	public static boolean rowIsValid(int row) {
		boolean valid = false;
		if (row >= 0 && row < levels.size()) {
			valid = true;
		}
		
		return valid;
	}
    
}
