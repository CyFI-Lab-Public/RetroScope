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

public class EventRecorder extends BaseObject {
	public final static int COUNTER_ROBOTS_DESTROYED = 0;
	public final static int COUNTER_PEARLS_COLLECTED = 1;
	public final static int COUNTER_PEARLS_TOTAL = 2;
	
	private Vector2 mLastDeathPosition = new Vector2();
	private int mLastEnding = -1;
	private int mRobotsDestroyed = 0;
	private int mPearlsCollected = 0;
	private int mPearlsTotal = 0;
	
	@Override
	public void reset() {
		mRobotsDestroyed = 0;
		mPearlsCollected = 0;
		mPearlsTotal = 0;
	}
	
	synchronized void setLastDeathPosition(Vector2 position) {
		mLastDeathPosition.set(position);
	}
	
	synchronized Vector2 getLastDeathPosition() {
		return mLastDeathPosition;
	}
	
	synchronized void setLastEnding(int ending) {
		mLastEnding = ending;
	}
	
	synchronized int getLastEnding() {
		return mLastEnding;
	}
	
	synchronized void incrementEventCounter(int event) {
		if (event == COUNTER_ROBOTS_DESTROYED) {
			mRobotsDestroyed++;
		} else if (event == COUNTER_PEARLS_COLLECTED) {
			mPearlsCollected++;
		} else if (event == COUNTER_PEARLS_TOTAL) {
			mPearlsTotal++;
		}
	}
	
	synchronized int getRobotsDestroyed() {
		return mRobotsDestroyed;
	}
	
	synchronized int getPearlsCollected() {
		return mPearlsCollected;
	}

	synchronized int getPearlsTotal() {
		return mPearlsTotal;
	}
}
