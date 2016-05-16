package com.replica.replicaisland;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class EventReporter implements Runnable {
	public final static int EVENT_DEATH = 0;
	public final static int EVENT_BEAT_LEVEL = 1;
	public final static int EVENT_BEAT_GAME = 2;
	
	public final static String REPORT_SERVER = null; // insert your server here.
	
	private class Event {
		public String eventType;
		public float x;
		public float y;
		public float time;
		public String level;
		public int version;
		public long session;
	}
	
	private Object mLock = new Object();
	private ArrayList<Event> mEvents = new ArrayList<Event>();
	private ArrayList<Event> mProcessedEvents = new ArrayList<Event>();
	private boolean mDone = false;

	public void run() {
		while (!mDone) {
			synchronized(mLock) {
	            if (mEvents.isEmpty()) {
	                while (mEvents.isEmpty() && !mDone) {
	                    try {
	                    	mLock.wait();
	                    } catch (InterruptedException e) {
	                    }
	                }
	            }
	            
	            mProcessedEvents.addAll(mEvents);
	            mEvents.clear();
	        }
			
			final int count = mProcessedEvents.size();
			for (int x = 0; x < count; x++) {
				recordEvent(mProcessedEvents.get(x));
			}
			
			mProcessedEvents.clear();
		}
	}
	
	public void addEvent(int eventType, float x, float y, float time, String level, int version, long session) {
		Event event = new Event();
		event.x = x;
		event.y = y;
		event.time = time;
		event.level = level;
		event.version = version;
		event.session = session;
		switch (eventType) {
			case EVENT_DEATH:
				event.eventType = "death";
				break;
			case EVENT_BEAT_LEVEL:
				event.eventType = "beatLevel";
				break;
			case EVENT_BEAT_GAME:
				event.eventType = "beatGame";
				break;
		}
		synchronized(mLock) {
			mEvents.add(event);
			mLock.notifyAll();
		}
	}
	
	public void recordEvent(Event event) {
        URL serverAddress = null;
        HttpURLConnection connection = null;

        if (REPORT_SERVER != null) {
	        try {
	            serverAddress = new URL(REPORT_SERVER + "?"
	            		+ "event=" + event.eventType
	            		+ "&x=" + event.x
	            		+ "&y=" + event.y
	            		+ "&time=" + event.time
	            		+ "&level=" + URLEncoder.encode(event.level, "UTF-8")
	            		+ "&version=" + event.version
	            		+ "&session=" + event.session);
	            //set up out communications stuff
	            connection = null;
	          
	            //Set up the initial connection
	            connection = (HttpURLConnection)serverAddress.openConnection();
	               
	            connection.setRequestMethod("GET");
	            connection.setDoOutput(true);
	            connection.setReadTimeout(0);
	            
	            connection.connect();
	            final int response = connection.getResponseCode();
	            DebugLog.d("Report Event", event.eventType + "  " + response + ":" + connection.getURL().toString());
	
	         
	                      
	        } catch (Exception e) {
	           // This code can silently fail.
	        	//e.printStackTrace();
	        }
	        finally
	        {
	            //close the connection
	            connection.disconnect();
	            connection = null;
	        }
        }

    }
	
	public void stop() {
		synchronized(mLock) {
			mDone = true;
			mLock.notifyAll();
		}
	}

}
