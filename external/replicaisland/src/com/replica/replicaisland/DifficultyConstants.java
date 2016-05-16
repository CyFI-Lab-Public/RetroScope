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

public abstract class DifficultyConstants {
	public abstract float getFuelAirRefillSpeed();
	public abstract float getFuelGroundRefillSpeed();
	public abstract int getMaxPlayerLife();
	public abstract int getCoinsPerPowerup();
	public abstract float getGlowDuration();
	public abstract int getDDAStage1Attempts();
	public abstract int getDDAStage2Attempts();
	public abstract int getDDAStage1LifeBoost();
	public abstract int getDDAStage2LifeBoost();
	public abstract float getDDAStage1FuelAirRefillSpeed();
	public abstract float getDDAStage2FuelAirRefillSpeed();
}
