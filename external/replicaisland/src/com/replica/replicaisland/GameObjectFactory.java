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

import java.util.Comparator;

import com.replica.replicaisland.AnimationComponent.PlayerAnimations;
import com.replica.replicaisland.CollisionParameters.HitType;
import com.replica.replicaisland.EnemyAnimationComponent.EnemyAnimations;
import com.replica.replicaisland.GameObject.ActionType;
import com.replica.replicaisland.GameObject.Team;
import com.replica.replicaisland.GenericAnimationComponent.Animation;

/** A class for generating game objects at runtime.
 * This should really be replaced with something that is data-driven, but it is hard to do data
 * parsing quickly at runtime.  For the moment this class is full of large functions that just
 * patch pointers between objects, but in the future those functions should either be 
 * a) generated from data at compile time, or b) described by data at runtime.
 */
public class GameObjectFactory extends BaseObject {
    private final static int MAX_GAME_OBJECTS = 384;
    private final static ComponentPoolComparator sComponentPoolComparator = new ComponentPoolComparator();
    private FixedSizeArray<FixedSizeArray<BaseObject>> mStaticData;
    private FixedSizeArray<GameComponentPool> mComponentPools;
    private GameComponentPool mPoolSearchDummy;
    private GameObjectPool mGameObjectPool;
    
    private float mTightActivationRadius;
    private float mNormalActivationRadius;
    private float mWideActivationRadius;
    private float mAlwaysActive;
    
    private final static String sRedButtonChannel = "RED BUTTON";
    private final static String sBlueButtonChannel = "BLUE BUTTON";
    private final static String sGreenButtonChannel = "GREEN BUTTON";
    private final static String sSurprisedNPCChannel = "SURPRISED";
    
    
    // A list of game objects that can be spawned at runtime.  Note that the indicies of these
    // objects must match the order of the object tileset in the level editor in order for the 
    // level content to make sense.
    public enum GameObjectType {
        INVALID(-1),
        
        PLAYER (0),
        
        // Collectables
        COIN (1),
        RUBY (2),
        DIARY (3),
        
        // Characters
        WANDA (10),
        KYLE (11),
        KYLE_DEAD (12),
        ANDOU_DEAD (13),
        KABOCHA (26),
        ROKUDOU_TERMINAL (27),
        KABOCHA_TERMINAL (28),
        EVIL_KABOCHA (29),
        ROKUDOU (30),
        
        // AI
        BROBOT (16),
        SNAILBOMB (17),
        SHADOWSLIME (18),
        MUDMAN (19),
        SKELETON (20),
        KARAGUIN (21),
        PINK_NAMAZU (22),
        TURRET (23),
        TURRET_LEFT (24),
        BAT (6),
        STING(7),
        ONION(8),
        
        // Objects
        DOOR_RED (32),
        DOOR_BLUE (33),
        DOOR_GREEN (34),
        BUTTON_RED (35),
        BUTTON_BLUE (36),
        BUTTON_GREEN (37),
        CANNON (38),
        BROBOT_SPAWNER (39),
        BROBOT_SPAWNER_LEFT (40),
        BREAKABLE_BLOCK(41),
        THE_SOURCE(42),
        HINT_SIGN(43),
        
        // Effects
        DUST(48),
        EXPLOSION_SMALL(49),
        EXPLOSION_LARGE(50),
        EXPLOSION_GIANT(51),
        
        
        // Special Spawnable
        DOOR_RED_NONBLOCKING (52),
        DOOR_BLUE_NONBLOCKING (53),
        DOOR_GREEN_NONBLOCKING (54),
        
        GHOST_NPC(55),
        
        CAMERA_BIAS(56),
        
        FRAMERATE_WATCHER(57),
        INFINITE_SPAWNER(58),
        CRUSHER_ANDOU(59),
        
        
        // Projectiles
        ENERGY_BALL(68),
        CANNON_BALL(65),
        TURRET_BULLET(66),
        BROBOT_BULLET(67),
        BREAKABLE_BLOCK_PIECE(68),
        BREAKABLE_BLOCK_PIECE_SPAWNER(69),
        WANDA_SHOT(70),
        
        // Special Objects -- Not spawnable normally
        SMOKE_BIG(-1),
        SMOKE_SMALL(-1),
        
        CRUSH_FLASH(-1),
        FLASH(-1),
        
        PLAYER_JETS(-1),
        PLAYER_SPARKS(-1),
        PLAYER_GLOW(-1),
        ENEMY_SPARKS(-1),
        GHOST(-1),
        SMOKE_POOF(-1),
        GEM_EFFECT(-1),
        GEM_EFFECT_SPAWNER(-1),
        
        
        // End
        OBJECT_COUNT(-1);
        
        private final int mIndex;
        GameObjectType(int index) {
            this.mIndex = index;
        }
        
        public int index() {
            return mIndex;
        }
        
        // TODO: Is there any better way to do this?
        public static GameObjectType indexToType(int index) {
            final GameObjectType[] valuesArray = values();
            GameObjectType foundType = INVALID;
            for (int x = 0; x < valuesArray.length; x++) {
                GameObjectType type = valuesArray[x];
                if (type.mIndex == index) {
                    foundType = type;
                    break;
                }
            }
            return foundType;
        }
        
    }
    
    public GameObjectFactory() {
        super();
        
        mGameObjectPool = new GameObjectPool(MAX_GAME_OBJECTS);
        
        final int objectTypeCount = GameObjectType.OBJECT_COUNT.ordinal();
        mStaticData = new FixedSizeArray<FixedSizeArray<BaseObject>>(objectTypeCount);
        
        for (int x = 0; x < objectTypeCount; x++) {
            mStaticData.add(null);
        }
        
        final ContextParameters context = sSystemRegistry.contextParameters;
        final float halfHeight2 = (context.gameHeight * 0.5f) * (context.gameHeight * 0.5f);
        final float halfWidth2 = (context.gameWidth * 0.5f) * (context.gameWidth * 0.5f);
        final float screenSizeRadius = (float)Math.sqrt(halfHeight2 + halfWidth2);
        mTightActivationRadius = screenSizeRadius + 128.0f;
        mNormalActivationRadius = screenSizeRadius * 1.25f;
        mWideActivationRadius = screenSizeRadius * 2.0f;
        mAlwaysActive = -1.0f;
       
        // TODO: I wish there was a way to do this automatically, but the ClassLoader doesn't seem
        // to provide access to the currently loaded class list.  There's some discussion of walking
        // the actual class file objects and using forName() to instantiate them, but that sounds
        // really heavy-weight.  For now I'll rely on (sucky) manual enumeration.
        class ComponentClass {
            public Class<?> type;
            public int poolSize;
            public ComponentClass(Class<?> classType, int size) {
                type = classType;
                poolSize = size;
            }
        }
        ComponentClass[] componentTypes = {
                new ComponentClass(AnimationComponent.class, 1),
                new ComponentClass(AttackAtDistanceComponent.class, 16),
                new ComponentClass(BackgroundCollisionComponent.class, 192),
                new ComponentClass(ButtonAnimationComponent.class, 32),
                new ComponentClass(CameraBiasComponent.class, 8),
                new ComponentClass(ChangeComponentsComponent.class, 256),
                new ComponentClass(CrusherAndouComponent.class, 1),
                new ComponentClass(DoorAnimationComponent.class, 256),  //!
                new ComponentClass(DynamicCollisionComponent.class, 256),
                new ComponentClass(EnemyAnimationComponent.class, 256),
                new ComponentClass(FadeDrawableComponent.class, 32),
                new ComponentClass(FixedAnimationComponent.class, 8),
                new ComponentClass(FrameRateWatcherComponent.class, 1),
                new ComponentClass(GenericAnimationComponent.class, 32),
                new ComponentClass(GhostComponent.class, 256),
                new ComponentClass(GravityComponent.class, 128),
                new ComponentClass(HitPlayerComponent.class, 256),
                new ComponentClass(HitReactionComponent.class, 256),
                new ComponentClass(InventoryComponent.class, 128),
                new ComponentClass(LauncherComponent.class, 16),
                new ComponentClass(LaunchProjectileComponent.class, 128),
                new ComponentClass(LifetimeComponent.class, 384),
                new ComponentClass(MotionBlurComponent.class, 1),
                new ComponentClass(MovementComponent.class, 128),
                new ComponentClass(NPCAnimationComponent.class, 8),
                new ComponentClass(NPCComponent.class, 8),
                new ComponentClass(OrbitalMagnetComponent.class, 1),
                new ComponentClass(PatrolComponent.class, 256),
                new ComponentClass(PhysicsComponent.class, 8),
                new ComponentClass(PlayerComponent.class, 1),
                new ComponentClass(PlaySingleSoundComponent.class, 128),
                new ComponentClass(PopOutComponent.class, 32),
                new ComponentClass(RenderComponent.class, 384),
                new ComponentClass(ScrollerComponent.class, 8),
                new ComponentClass(SelectDialogComponent.class, 8),
                new ComponentClass(SimpleCollisionComponent.class, 32),
                new ComponentClass(SimplePhysicsComponent.class, 256),
                new ComponentClass(SleeperComponent.class, 32),
                new ComponentClass(SolidSurfaceComponent.class, 16),
                new ComponentClass(SpriteComponent.class, 384),
                new ComponentClass(TheSourceComponent.class, 1),
                
        };
        
        mComponentPools = new FixedSizeArray<GameComponentPool>(componentTypes.length, sComponentPoolComparator);
        for (int x = 0; x < componentTypes.length; x++) {
            ComponentClass component = componentTypes[x];
            mComponentPools.add(new GameComponentPool(component.type, component.poolSize));
        }
        mComponentPools.sort(true);
        
        mPoolSearchDummy = new GameComponentPool(Object.class, 1);
        
    }
    
    @Override
    public void reset() {
        
    }
    
    protected GameComponentPool getComponentPool(Class<?> componentType) {
        GameComponentPool pool = null;
        mPoolSearchDummy.objectClass = componentType;
        final int index = mComponentPools.find(mPoolSearchDummy, false);
        if (index != -1) {
            pool = mComponentPools.get(index);
        }
        return pool;
    }
    
    protected GameComponent allocateComponent(Class<?> componentType) {
        GameComponentPool pool = getComponentPool(componentType);
        assert pool != null;
        GameComponent component = null;
        if (pool != null) {
            component = pool.allocate();
        }
        return component;
    }
    
    protected void releaseComponent(GameComponent component) {
        GameComponentPool pool = getComponentPool(component.getClass());
        assert pool != null;
        if (pool != null) {
            component.reset();
            component.shared = false;
            pool.release(component);
        }
    }
    
    protected boolean componentAvailable(Class<?> componentType, int count) {
    	boolean canAllocate = false;
        GameComponentPool pool = getComponentPool(componentType);
        assert pool != null;
        if (pool != null) {
        	canAllocate = pool.getAllocatedCount() + count < pool.getSize();
        }
        return canAllocate;
    }
    
    public void preloadEffects() {
        // These textures appear in every level, so they are long-term.
        TextureLibrary textureLibrary = sSystemRegistry.longTermTextureLibrary;
        textureLibrary.allocateTexture(R.drawable.dust01);
        textureLibrary.allocateTexture(R.drawable.dust02);
        textureLibrary.allocateTexture(R.drawable.dust03);
        textureLibrary.allocateTexture(R.drawable.dust04);
        textureLibrary.allocateTexture(R.drawable.dust05);
        
        textureLibrary.allocateTexture(R.drawable.effect_energyball01);
        textureLibrary.allocateTexture(R.drawable.effect_energyball02);
        textureLibrary.allocateTexture(R.drawable.effect_energyball03);
        textureLibrary.allocateTexture(R.drawable.effect_energyball04);
        
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small01);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small02);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small03);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small04);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small05);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small06);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_small07);
        
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big01);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big02);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big03);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big04);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big05);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big06);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big07);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big08);
        textureLibrary.allocateTexture(R.drawable.effect_explosion_big09);
        
        textureLibrary.allocateTexture(R.drawable.effect_smoke_big01);
        textureLibrary.allocateTexture(R.drawable.effect_smoke_big02);
        textureLibrary.allocateTexture(R.drawable.effect_smoke_big03);
        textureLibrary.allocateTexture(R.drawable.effect_smoke_big04);
        textureLibrary.allocateTexture(R.drawable.effect_smoke_big05);
        
        textureLibrary.allocateTexture(R.drawable.effect_smoke_small01);
        textureLibrary.allocateTexture(R.drawable.effect_smoke_small02);
        textureLibrary.allocateTexture(R.drawable.effect_smoke_small03);
        textureLibrary.allocateTexture(R.drawable.effect_smoke_small04);
        textureLibrary.allocateTexture(R.drawable.effect_smoke_small05);
     
        textureLibrary.allocateTexture(R.drawable.effect_crush_back01);
        textureLibrary.allocateTexture(R.drawable.effect_crush_back02);
        textureLibrary.allocateTexture(R.drawable.effect_crush_back03);
        textureLibrary.allocateTexture(R.drawable.effect_crush_front01);
        textureLibrary.allocateTexture(R.drawable.effect_crush_front02);
        textureLibrary.allocateTexture(R.drawable.effect_crush_front03);
        textureLibrary.allocateTexture(R.drawable.effect_crush_front04);
        textureLibrary.allocateTexture(R.drawable.effect_crush_front05);
        textureLibrary.allocateTexture(R.drawable.effect_crush_front06);
        textureLibrary.allocateTexture(R.drawable.effect_crush_front07);
    }
    
    public void destroy(GameObject object) {
        object.commitUpdates();
        final int componentCount = object.getCount();
        for (int x = 0; x < componentCount; x++) {
            GameComponent component = (GameComponent)object.get(x);
            if (!component.shared) {
                releaseComponent(component);
            }
        }
        object.removeAll();
        object.commitUpdates();
        mGameObjectPool.release(object);
    }
    
    public GameObject spawn(GameObjectType type, float x, float y, boolean horzFlip) {
        GameObject newObject = null;
        switch(type) {
            case PLAYER:
                newObject = spawnPlayer(x, y);
                break;
            case COIN:
                newObject = spawnCoin(x, y);
                break;
            case RUBY:
                newObject = spawnRuby(x, y);
                break;
            case DIARY:
                newObject = spawnDiary(x, y);
                break;
            case WANDA:
                newObject = spawnEnemyWanda(x, y, true);
                break;
            case KYLE:
                newObject = spawnEnemyKyle(x, y, true);
                break;
            case KYLE_DEAD:
                newObject = spawnEnemyKyleDead(x, y);
                break;
            case ANDOU_DEAD:
                newObject = spawnEnemyAndouDead(x, y);
                break;
            case KABOCHA:
                newObject = spawnEnemyKabocha(x, y, true);
                break;
            case ROKUDOU_TERMINAL:
                newObject = spawnRokudouTerminal(x, y);
                break;
            case KABOCHA_TERMINAL:
                newObject = spawnKabochaTerminal(x, y);
                break;
            case EVIL_KABOCHA:
                newObject = spawnEnemyEvilKabocha(x, y, true);
                break;
            case ROKUDOU:
                newObject = spawnEnemyRokudou(x, y, true);
                break;
            case BROBOT:
                newObject = spawnEnemyBrobot(x, y, horzFlip);
                break;
            case SNAILBOMB:
                newObject = spawnEnemySnailBomb(x, y, horzFlip);
                break;
            case SHADOWSLIME:
                newObject = spawnEnemyShadowSlime(x, y, horzFlip);
                break;
            case MUDMAN: 
                newObject = spawnEnemyMudman(x, y, horzFlip);
                break;
            case SKELETON:
                newObject = spawnEnemySkeleton(x, y, horzFlip);
                break;
            case KARAGUIN:
                newObject = spawnEnemyKaraguin(x, y, horzFlip);
                break;
            case PINK_NAMAZU:
                newObject = spawnEnemyPinkNamazu(x, y, horzFlip);
                break;
            case BAT:
                newObject = spawnEnemyBat(x, y, horzFlip);
                break;
            case STING:
                newObject = spawnEnemySting(x, y, horzFlip);
                break;
            case ONION:
                newObject = spawnEnemyOnion(x, y, horzFlip);
                break;
            case TURRET:
            case TURRET_LEFT:
                newObject = spawnObjectTurret(x, y, (type == GameObjectType.TURRET_LEFT));
                break;
            case DOOR_RED:
            case DOOR_RED_NONBLOCKING:
                newObject = spawnObjectDoor(x, y, GameObjectType.DOOR_RED, (type == GameObjectType.DOOR_RED));
                break;
            case DOOR_BLUE:
            case DOOR_BLUE_NONBLOCKING:
                newObject = spawnObjectDoor(x, y, GameObjectType.DOOR_BLUE, (type == GameObjectType.DOOR_BLUE));
                break;
            case DOOR_GREEN:
            case DOOR_GREEN_NONBLOCKING:
                newObject = spawnObjectDoor(x, y, GameObjectType.DOOR_GREEN, (type == GameObjectType.DOOR_GREEN));
                break;
            case BUTTON_RED:
                newObject = spawnObjectButton(x, y, GameObjectType.BUTTON_RED);
                break;
            case BUTTON_BLUE:
                newObject = spawnObjectButton(x, y, GameObjectType.BUTTON_BLUE);
                break;
            case BUTTON_GREEN:
                newObject = spawnObjectButton(x, y, GameObjectType.BUTTON_GREEN);
                break;
            case CANNON:
                newObject = spawnObjectCannon(x, y);
                break;
            case BROBOT_SPAWNER:
            case BROBOT_SPAWNER_LEFT:
                newObject = spawnObjectBrobotSpawner(x, y, (type == GameObjectType.BROBOT_SPAWNER_LEFT));
                break;
            case BREAKABLE_BLOCK:
                newObject = spawnObjectBreakableBlock(x, y);
                break;
            case THE_SOURCE:
            	newObject = spawnObjectTheSource(x, y);
            	break;
            case HINT_SIGN:
            	newObject = spawnObjectSign(x, y);
            	break;
            case DUST:
                newObject = spawnDust(x, y, horzFlip);
                break;
            case EXPLOSION_SMALL:
                newObject = spawnEffectExplosionSmall(x, y);
                break;
            case EXPLOSION_LARGE:
                newObject = spawnEffectExplosionLarge(x, y);
                break;
            case EXPLOSION_GIANT:
                newObject = spawnEffectExplosionGiant(x, y);
                break;
            case GHOST_NPC:
            	newObject = spawnGhostNPC(x, y);
            	break;
            case CAMERA_BIAS:
            	newObject = spawnCameraBias(x, y);
            	break;
            case FRAMERATE_WATCHER:
            	newObject = spawnFrameRateWatcher(x, y);
            	break;
            case INFINITE_SPAWNER:
            	newObject = spawnObjectInfiniteSpawner(x, y);
            	break;
            case CRUSHER_ANDOU:
            	newObject = spawnObjectCrusherAndou(x,y);
            	break;
            case SMOKE_BIG:
                newObject = spawnEffectSmokeBig(x, y);
                break;
            case SMOKE_SMALL:
                newObject = spawnEffectSmokeSmall(x, y);
                break;
            case CRUSH_FLASH:
                newObject = spawnEffectCrushFlash(x, y);
                break;
            case FLASH:
                newObject = spawnEffectFlash(x, y);
                break;
            
            case ENERGY_BALL:
                newObject = spawnEnergyBall(x, y, horzFlip);
                break;
            case CANNON_BALL:
                newObject = spawnCannonBall(x, y, horzFlip);
                break;
            case TURRET_BULLET:
                newObject = spawnTurretBullet(x, y, horzFlip);
                break;
            case BROBOT_BULLET:
                newObject = spawnBrobotBullet(x, y, horzFlip);
                break;
            case BREAKABLE_BLOCK_PIECE:
                newObject = spawnBreakableBlockPiece(x, y);
                break;
            case BREAKABLE_BLOCK_PIECE_SPAWNER:
                newObject = spawnBreakableBlockPieceSpawner(x, y);
                break;
            case WANDA_SHOT:
                newObject = spawnWandaShot(x, y, horzFlip);
                break;
            case SMOKE_POOF:
            	newObject = spawnSmokePoof(x, y);
            	break;
            case GEM_EFFECT:
            	newObject = spawnGemEffect(x, y);
            	break;
            case GEM_EFFECT_SPAWNER:
            	newObject = spawnGemEffectSpawner(x, y);
            	break;
        }
        
        return newObject;
    }
    
	

	public void spawnFromWorld(TiledWorld world, int tileWidth, int tileHeight) {
        // Walk the world and spawn objects based on tile indexes.
        final float worldHeight = world.getHeight() * tileHeight;
        GameObjectManager manager = sSystemRegistry.gameObjectManager;
        if (manager != null) {
            for (int y = 0; y < world.getHeight(); y++) {
                for (int x = 0; x < world.getWidth(); x++) {
                    int index = world.getTile(x, y);
                    if (index != -1) {
                        GameObjectType type = GameObjectType.indexToType(index);
                        if (type != GameObjectType.INVALID) {
                            final float worldX = x * tileWidth;
                            final float worldY = worldHeight - ((y + 1) * tileHeight);
                            GameObject object = spawn(type, worldX, worldY, false);
                            if (object != null) {
                                if (object.height < tileHeight) {
                                    // make sure small objects are vertically centered in their
                                    // tile.
                                    object.getPosition().y += (tileHeight - object.height) / 2.0f;
                                }
                                if (object.width < tileWidth) {
                                    object.getPosition().x += (tileWidth - object.width) / 2.0f;
                                } else if (object.width > tileWidth) {
                                    object.getPosition().x -= (object.width - tileWidth) / 2.0f;
                                }
                                manager.add(object);
                                if (type == GameObjectType.PLAYER) {
                                    manager.setPlayer(object);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    private FixedSizeArray<BaseObject> getStaticData(GameObjectType type) {
        return mStaticData.get(type.ordinal());
    }
    
    private void setStaticData(GameObjectType type, FixedSizeArray<BaseObject> data) {
        int index = type.ordinal();
        assert mStaticData.get(index) == null;
        
        final int staticDataCount = data.getCount();

        for (int x = 0; x < staticDataCount; x++) {
            BaseObject entry = data.get(x);
            if (entry instanceof GameComponent) {
                ((GameComponent) entry).shared = true;
            }
        }
        
        mStaticData.set(index, data);
    }
    
    private void addStaticData(GameObjectType type, GameObject object, SpriteComponent sprite) {
        FixedSizeArray<BaseObject> staticData = getStaticData(type);
        assert staticData != null;

        if (staticData != null) {
            final int staticDataCount = staticData.getCount();
            
            for (int x = 0; x < staticDataCount; x++) {
                BaseObject entry = staticData.get(x);
                if (entry instanceof GameComponent && object != null) {
                    object.add((GameComponent)entry);
                } else if (entry instanceof SpriteAnimation && sprite != null) {
                    sprite.addAnimation((SpriteAnimation)entry);
                }
            }
        }
    }
    
    public void clearStaticData() {
        final int typeCount = mStaticData.getCount();
        for (int x = 0; x < typeCount; x++) {
            FixedSizeArray<BaseObject> staticData = mStaticData.get(x);
            if (staticData != null) {
                final int count = staticData.getCount();
                for (int y = 0; y < count; y++) {
                    BaseObject entry = staticData.get(y);
                    if (entry != null) {
                        if (entry instanceof GameComponent) {
                            releaseComponent((GameComponent)entry);
                        } 
                    }
                }
                staticData.clear();
                mStaticData.set(x, null);
            }
        }
    }
    
    public void sanityCheckPools() {
        final int outstandingObjects = mGameObjectPool.getAllocatedCount();
        if (outstandingObjects != 0) {
            DebugLog.d("Sanity Check", "Outstanding game object allocations! (" 
                    + outstandingObjects + ")");
            assert false;
        }
        
        final int componentPoolCount = mComponentPools.getCount();
        for (int x = 0; x < componentPoolCount; x++) {
            final int outstandingComponents = mComponentPools.get(x).getAllocatedCount();
            
            if (outstandingComponents != 0) {
                DebugLog.d("Sanity Check", "Outstanding " 
                        + mComponentPools.get(x).objectClass.getSimpleName()
                        + " allocations! (" + outstandingComponents + ")");
                //assert false;
            }
        }
    }
    
    public GameObject spawnPlayer(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mAlwaysActive;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.PLAYER);
        
        if (staticData == null) {
            final int staticObjectCount = 13;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            PhysicsComponent physics = (PhysicsComponent)allocateComponent(PhysicsComponent.class);

            physics.setMass(9.1f);   // ~90kg w/ earth gravity
            physics.setDynamicFrictionCoeffecient(0.2f);
            physics.setStaticFrictionCoeffecient(0.01f);
            
            // Animation Data
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(16, 32, 32));
            
            FixedSizeArray<CollisionVolume> pressAndCollectVolume = 
                new FixedSizeArray<CollisionVolume>(2);
            AABoxCollisionVolume collectionVolume = new AABoxCollisionVolume(16, 0, 32, 48);
            collectionVolume.setHitType(HitType.COLLECT);
            pressAndCollectVolume.add(collectionVolume);
            AABoxCollisionVolume pressCollisionVolume = new AABoxCollisionVolume(16, 0, 32, 16);
            pressCollisionVolume.setHitType(HitType.DEPRESS);
            pressAndCollectVolume.add(pressCollisionVolume);

            SpriteAnimation idle = new SpriteAnimation(PlayerAnimations.IDLE.ordinal(), 1);
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stand), 
                    1.0f, pressAndCollectVolume, basicVulnerabilityVolume));
            
            SpriteAnimation angle = new SpriteAnimation(PlayerAnimations.MOVE.ordinal(), 1);
            angle.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_diag01), 
                            0.0416f, pressAndCollectVolume, basicVulnerabilityVolume));
            
            SpriteAnimation extremeAngle = new SpriteAnimation(
                    PlayerAnimations.MOVE_FAST.ordinal(), 1);
            extremeAngle.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_diagmore01), 
                            0.0416f, pressAndCollectVolume, basicVulnerabilityVolume));
            
            SpriteAnimation up = new SpriteAnimation(PlayerAnimations.BOOST_UP.ordinal(), 2);
            up.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_flyup02), 
                    Utils.framesToTime(24, 1), pressAndCollectVolume, basicVulnerabilityVolume));
            up.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_flyup03), 
                    Utils.framesToTime(24, 1), pressAndCollectVolume, basicVulnerabilityVolume));
            up.setLoop(true);
            
            SpriteAnimation boostAngle = new SpriteAnimation(PlayerAnimations.BOOST_MOVE.ordinal(), 2);
            boostAngle.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_diag02), 
                            Utils.framesToTime(24, 1), pressAndCollectVolume, basicVulnerabilityVolume));
            boostAngle.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_diag03), 
                            Utils.framesToTime(24, 1), pressAndCollectVolume, basicVulnerabilityVolume));
            boostAngle.setLoop(true);
            
            SpriteAnimation boostExtremeAngle = new SpriteAnimation(
                    PlayerAnimations.BOOST_MOVE_FAST.ordinal(), 2);
            boostExtremeAngle.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_diagmore02), 
                            Utils.framesToTime(24, 1), pressAndCollectVolume, basicVulnerabilityVolume));
            boostExtremeAngle.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_diagmore03), 
                            Utils.framesToTime(24, 1), pressAndCollectVolume, basicVulnerabilityVolume));
            boostExtremeAngle.setLoop(true);
            
            FixedSizeArray<CollisionVolume> stompAttackVolume = 
                new FixedSizeArray<CollisionVolume>(3);
            stompAttackVolume.add(new AABoxCollisionVolume(16, -5.0f, 32, 37, HitType.HIT));
            stompAttackVolume.add(pressCollisionVolume);
            stompAttackVolume.add(collectionVolume);
            
            SpriteAnimation stomp = new SpriteAnimation(PlayerAnimations.STOMP.ordinal(), 4);
            stomp.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp01), 
                    		Utils.framesToTime(24, 1), stompAttackVolume, null));
            stomp.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp02), 
                    		Utils.framesToTime(24, 1), stompAttackVolume, null));
            stomp.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp03), 
                    		Utils.framesToTime(24, 1), stompAttackVolume, null));
            stomp.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp04), 
                    		Utils.framesToTime(24, 1), stompAttackVolume, null));
            
            SpriteAnimation hitReactAnim = new SpriteAnimation(PlayerAnimations.HIT_REACT.ordinal(), 1);
            hitReactAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_hit), 
                            0.1f, pressAndCollectVolume, null));
            
            SpriteAnimation deathAnim = new SpriteAnimation(PlayerAnimations.DEATH.ordinal(), 16);
            AnimationFrame death1 = 
                new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_die01), 
                        Utils.framesToTime(24, 1), null, null);
            AnimationFrame death2 = 
                new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_die02), 
                        Utils.framesToTime(24, 1), null, null);
            deathAnim.addFrame(death1);
            deathAnim.addFrame(death2);
            deathAnim.addFrame(death1);
            deathAnim.addFrame(death2);
            deathAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode01), 
                            Utils.framesToTime(24, 1), null, null));
            deathAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode02), 
                            Utils.framesToTime(24, 1), null, null));
            deathAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode03), 
                            Utils.framesToTime(24, 1), null, null));
            deathAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode04), 
                            Utils.framesToTime(24, 1), null, null));
            deathAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode05), 
                            Utils.framesToTime(24, 2), null, null));
            deathAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode06), 
                            Utils.framesToTime(24, 2), null, null));
            deathAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode07), 
                            Utils.framesToTime(24, 2), null, null));
            deathAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode08), 
                            Utils.framesToTime(24, 2), null, null));
            deathAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode09), 
                            Utils.framesToTime(24, 2), null, null));
            deathAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode10), 
                            Utils.framesToTime(24, 2), null, null));
            deathAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode11), 
                            Utils.framesToTime(24, 2), null, null));
            deathAnim.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode12), 
                            Utils.framesToTime(24, 2), null, null));
            
            
            SpriteAnimation frozenAnim = new SpriteAnimation(PlayerAnimations.FROZEN.ordinal(), 1);
            // Frozen has no frames!
           
            
            // Save static data
            staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            
            
            staticData.add(idle);
            staticData.add(angle);
            staticData.add(extremeAngle);
            staticData.add(up);
            staticData.add(boostAngle);
            staticData.add(boostExtremeAngle);
            staticData.add(stomp);
            staticData.add(hitReactAnim);
            staticData.add(deathAnim);
            staticData.add(frozenAnim);
            
            setStaticData(GameObjectType.PLAYER, staticData);
        }
        
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.PLAYER);
        BackgroundCollisionComponent bgcollision 
            = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(32, 48);
        bgcollision.setOffset(16, 0);
        PlayerComponent player = (PlayerComponent)allocateComponent(PlayerComponent.class);
        AnimationComponent animation =
            (AnimationComponent)allocateComponent(AnimationComponent.class);

        animation.setPlayer(player);
        SoundSystem sound = sSystemRegistry.soundSystem;
        if (sound != null) {
            animation.setLandThump(sound.load(R.raw.thump));
            animation.setRocketSound(sound.load(R.raw.rockets));
            animation.setRubySounds(sound.load(R.raw.gem1), sound.load(R.raw.gem2), sound.load(R.raw.gem3));
            animation.setExplosionSound(sound.load(R.raw.sound_explode));
        }
        
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        animation.setSprite(sprite);
        

        
        DynamicCollisionComponent dynamicCollision 
            = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setBounceOnHit(true);
        hitReact.setPauseOnAttack(true);
        hitReact.setInvincibleTime(3.0f);
        hitReact.setSpawnOnDealHit(HitType.HIT, GameObjectType.CRUSH_FLASH, false, true);
        
        if (sound != null) {
            hitReact.setTakeHitSound(HitType.HIT, sound.load(R.raw.deep_clang));
        }
        
        dynamicCollision.setHitReactionComponent(hitReact);
        
        player.setHitReactionComponent(hitReact);
        
        InventoryComponent inventory = (InventoryComponent)allocateComponent(InventoryComponent.class);

        player.setInventory(inventory);
        animation.setInventory(inventory);
        
        ChangeComponentsComponent damageSwap = (ChangeComponentsComponent)allocateComponent(ChangeComponentsComponent.class);
        animation.setDamageSwap(damageSwap);
        
        LaunchProjectileComponent smokeGun 
            = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
        smokeGun.setDelayBetweenShots(0.25f);
        smokeGun.setObjectTypeToSpawn(GameObjectType.SMOKE_BIG);
        smokeGun.setOffsetX(32);
        smokeGun.setOffsetY(15);
        smokeGun.setVelocityX(-150.0f);
        smokeGun.setVelocityY(100.0f);
        smokeGun.setThetaError(0.1f);
        
        LaunchProjectileComponent smokeGun2 
            = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
        smokeGun2.setDelayBetweenShots(0.35f);
        smokeGun2.setObjectTypeToSpawn(GameObjectType.SMOKE_SMALL);
        smokeGun2.setOffsetX(16);
        smokeGun2.setOffsetY(15);
        smokeGun2.setVelocityX(-150.0f);
        smokeGun2.setVelocityY(150.0f);
        smokeGun2.setThetaError(0.1f); 
        
        damageSwap.addSwapInComponent(smokeGun);
        damageSwap.addSwapInComponent(smokeGun2);
        damageSwap.setPingPongBehavior(true);
        
        ChangeComponentsComponent invincibleSwap = (ChangeComponentsComponent)allocateComponent(ChangeComponentsComponent.class);
        invincibleSwap.setPingPongBehavior(true);
        player.setInvincibleSwap(invincibleSwap);

        object.life = PlayerComponent.getDifficultyConstants().getMaxPlayerLife();
        object.team = Team.PLAYER;
        
        // Very very basic DDA.  Make the game easier if we've died on this level too much.
        LevelSystem level = sSystemRegistry.levelSystem;
        if (level != null) { 
        	player.adjustDifficulty(object, level.getAttemptsCount());
        }
        
        
        object.add(player);
        object.add(inventory);
        object.add(bgcollision);
        object.add(render);
        object.add(animation);
        object.add(sprite);  
        object.add(dynamicCollision);  
        object.add(hitReact); 
        object.add(damageSwap);
        object.add(invincibleSwap);
        
        addStaticData(GameObjectType.PLAYER, object, sprite);
      
       
        
        sprite.playAnimation(PlayerAnimations.IDLE.ordinal());
       

        // Jets
        {
            FixedSizeArray<BaseObject> jetStaticData = getStaticData(GameObjectType.PLAYER_JETS);
            if (jetStaticData == null) {
                jetStaticData = new FixedSizeArray<BaseObject>(1);
                
                SpriteAnimation jetAnim = new SpriteAnimation(0, 2);
                jetAnim.addFrame(
                        new AnimationFrame(textureLibrary.allocateTexture(R.drawable.jetfire01), 
                                Utils.framesToTime(24, 1)));
                jetAnim.addFrame(
                        new AnimationFrame(textureLibrary.allocateTexture(R.drawable.jetfire02), 
                                Utils.framesToTime(24, 1)));
                jetAnim.setLoop(true);
                
                jetStaticData.add(jetAnim);
                
                setStaticData(GameObjectType.PLAYER_JETS, jetStaticData);
            }
            
            RenderComponent jetRender = (RenderComponent)allocateComponent(RenderComponent.class);
            jetRender.setPriority(SortConstants.PLAYER - 1);
            jetRender.setDrawOffset(0.0f, -16.0f);
            SpriteComponent jetSprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
            jetSprite.setSize(64, 64);
            jetSprite.setRenderComponent(jetRender);
            
            object.add(jetRender);
            object.add(jetSprite);  
            
            addStaticData(GameObjectType.PLAYER_JETS, object, jetSprite);
    
            jetSprite.playAnimation(0);
            
            animation.setJetSprite(jetSprite);
        }
        // Sparks
        {
            FixedSizeArray<BaseObject> sparksStaticData = getStaticData(GameObjectType.PLAYER_SPARKS);
            
            if (sparksStaticData == null) {
                sparksStaticData = new FixedSizeArray<BaseObject>(1);
                
                SpriteAnimation sparksAnim = new SpriteAnimation(0, 3);
                sparksAnim.addFrame(
                        new AnimationFrame(textureLibrary.allocateTexture(R.drawable.spark01), 
                                Utils.framesToTime(24, 1)));
                sparksAnim.addFrame(
                        new AnimationFrame(textureLibrary.allocateTexture(R.drawable.spark02), 
                                Utils.framesToTime(24, 1)));
                sparksAnim.addFrame(
                        new AnimationFrame(textureLibrary.allocateTexture(R.drawable.spark03), 
                                Utils.framesToTime(24, 1)));
                sparksAnim.setLoop(true);
                
                sparksStaticData.add(sparksAnim);
                
                setStaticData(GameObjectType.PLAYER_SPARKS, sparksStaticData);
            }
            
            RenderComponent sparksRender = (RenderComponent)allocateComponent(RenderComponent.class);
            sparksRender.setPriority(SortConstants.PLAYER + 1);
            SpriteComponent sparksSprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
            sparksSprite.setSize(64, 64);
            sparksSprite.setRenderComponent(sparksRender);
            
            object.add(sparksRender);
            object.add(sparksSprite);
            
            addStaticData(GameObjectType.PLAYER_SPARKS, object, sparksSprite);
    
            sparksSprite.playAnimation(0);
            
            animation.setSparksSprite(sparksSprite);
        }
        
        // Glow
        {
            FixedSizeArray<BaseObject> glowStaticData = getStaticData(GameObjectType.PLAYER_GLOW);
            if (glowStaticData == null) {
                glowStaticData = new FixedSizeArray<BaseObject>(1);
                
                FixedSizeArray<CollisionVolume> glowAttackVolume = 
                    new FixedSizeArray<CollisionVolume>(1);
                glowAttackVolume.add(new SphereCollisionVolume(40, 40, 40, HitType.HIT));
                
                SpriteAnimation glowAnim = new SpriteAnimation(0, 3);
                glowAnim.addFrame(
                        new AnimationFrame(textureLibrary.allocateTexture(R.drawable.effect_glow01), 
                                Utils.framesToTime(24, 1), glowAttackVolume, null));
                glowAnim.addFrame(
                        new AnimationFrame(textureLibrary.allocateTexture(R.drawable.effect_glow02), 
                                Utils.framesToTime(24, 1), glowAttackVolume, null));
                glowAnim.addFrame(
                        new AnimationFrame(textureLibrary.allocateTexture(R.drawable.effect_glow03), 
                                Utils.framesToTime(24, 1), glowAttackVolume, null));
                glowAnim.setLoop(true);
                
                glowStaticData.add(glowAnim);
                
                setStaticData(GameObjectType.PLAYER_GLOW, glowStaticData);
            }
            
            RenderComponent glowRender = (RenderComponent)allocateComponent(RenderComponent.class);
            glowRender.setPriority(SortConstants.PLAYER + 1);
            glowRender.setDrawOffset(0, -5.0f);
            SpriteComponent glowSprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
            glowSprite.setSize(64, 64);
            glowSprite.setRenderComponent(glowRender);
            
            DynamicCollisionComponent glowCollision 
                = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
            glowSprite.setCollisionComponent(glowCollision);
            
            FadeDrawableComponent glowFade = (FadeDrawableComponent)allocateComponent(FadeDrawableComponent.class);
            final float glowDuration = PlayerComponent.getDifficultyConstants().getGlowDuration();
            glowFade.setupFade(1.0f, 0.0f, 0.15f, 
            		FadeDrawableComponent.LOOP_TYPE_PING_PONG, 
            		FadeDrawableComponent.FADE_EASE, 
            		glowDuration - 4.0f);	// 4 seconds before the glow ends, start flashing
            glowFade.setPhaseDuration(glowDuration);
            glowFade.setRenderComponent(glowRender);
            
            // HACK
            player.setInvincibleFader(glowFade);
        
            invincibleSwap.addSwapInComponent(glowRender);
            invincibleSwap.addSwapInComponent(glowSprite);
            invincibleSwap.addSwapInComponent(glowCollision);
            invincibleSwap.addSwapInComponent(glowFade);
            
            addStaticData(GameObjectType.PLAYER_GLOW, object, glowSprite);
    
            glowSprite.playAnimation(0);
            
        }
        
        CameraSystem camera = sSystemRegistry.cameraSystem;
        if (camera != null) {
            camera.setTarget(object);
        }
            
        return object;
    }
    
    
    // Sparks are used by more than one enemy type, so the setup for them is abstracted.
    private void setupEnemySparks() {
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.ENEMY_SPARKS);
        if (staticData == null) {
            staticData = new FixedSizeArray<BaseObject>(1);
            TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;

            SpriteAnimation sparksAnim = new SpriteAnimation(0, 13);
            AnimationFrame frame1 = 
                new AnimationFrame(textureLibrary.allocateTexture(R.drawable.spark01), 
                        Utils.framesToTime(24, 1));
            AnimationFrame frame2 = 
                new AnimationFrame(textureLibrary.allocateTexture(R.drawable.spark02), 
                        Utils.framesToTime(24, 1));
            AnimationFrame frame3 = 
                new AnimationFrame(textureLibrary.allocateTexture(R.drawable.spark03), 
                        Utils.framesToTime(24, 1));
            sparksAnim.addFrame(frame1);
            sparksAnim.addFrame(frame2);
            sparksAnim.addFrame(frame3);
            sparksAnim.addFrame(frame1);
            sparksAnim.addFrame(frame2);
            sparksAnim.addFrame(frame3);
            sparksAnim.addFrame(frame1);
            sparksAnim.addFrame(frame2);
            sparksAnim.addFrame(frame3);
            sparksAnim.addFrame(frame1);
            sparksAnim.addFrame(frame2);
            sparksAnim.addFrame(frame3);
            sparksAnim.addFrame(new AnimationFrame(null, 3.0f));
            sparksAnim.setLoop(true);

            staticData.add(sparksAnim);
            setStaticData(GameObjectType.ENEMY_SPARKS, staticData);
        }
        
    }
    
    public GameObject spawnEnemyBrobot(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mNormalActivationRadius;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.BROBOT);
        if (staticData == null) {
            final int staticObjectCount = 5;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            SimplePhysicsComponent physics = (SimplePhysicsComponent)allocateComponent(SimplePhysicsComponent.class);
            physics.setBounciness(0.4f);

            
            // Animations
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(16, 32, 32));
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(2);
            basicAttackVolume.add(new SphereCollisionVolume(16, 32, 32, HitType.HIT));
            basicAttackVolume.add(new AABoxCollisionVolume(16, 0, 32, 16, HitType.DEPRESS));
            
            SpriteAnimation idle = new SpriteAnimation(EnemyAnimations.IDLE.ordinal(), 4);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_idle01), 
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_idle02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_idle03), 
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_idle02), 
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume));

            idle.setLoop(true);
            
            SpriteAnimation walk = new SpriteAnimation(EnemyAnimations.MOVE.ordinal(), 3);
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk01), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            walk.setLoop(true);
            
            staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            staticData.add(idle);
            staticData.add(walk);
            
            setStaticData(GameObjectType.BROBOT, staticData);
            
        }
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_ENEMY);
        BackgroundCollisionComponent bgcollision 
            = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(32, 48);
        bgcollision.setOffset(16, 0);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
 
        EnemyAnimationComponent animation 
            = (EnemyAnimationComponent)allocateComponent(EnemyAnimationComponent.class);
        animation.setSprite(sprite);
        
        PatrolComponent patrol = (PatrolComponent)allocateComponent(PatrolComponent.class);
        patrol.setMovementSpeed(50.0f, 1000.0f);
        
        DynamicCollisionComponent collision 
            = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact 
            = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setObjectToSpawnOnDeath(GameObjectType.EXPLOSION_GIANT);
        lifetime.setVulnerableToDeathTiles(true);
        lifetime.setIncrementEventCounter(EventRecorder.COUNTER_ROBOTS_DESTROYED);
        
        GhostComponent ghost = (GhostComponent)allocateComponent(GhostComponent.class);
        ghost.setMovementSpeed(500.0f);
        ghost.setAcceleration(1000.0f);
        ghost.setJumpImpulse(300.0f);
        ghost.setKillOnRelease(true);
        ghost.setDelayOnRelease(1.5f);
        
        SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        if (sound != null) {
        	ghost.setAmbientSound(sound.load(R.raw.sound_possession));
        }
        
        ChangeComponentsComponent ghostSwap 
            = (ChangeComponentsComponent)allocateComponent(ChangeComponentsComponent.class);
        ghostSwap.addSwapInComponent(ghost);
        ghostSwap.addSwapOutComponent(patrol);
        
        SimplePhysicsComponent ghostPhysics = (SimplePhysicsComponent)allocateComponent(SimplePhysicsComponent.class);
        ghostPhysics.setBounciness(0.0f);
        
        object.add(render);
        object.add(sprite);
        
        object.add(bgcollision);
        object.add(animation);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        object.add(lifetime);
        object.add(ghostSwap);
        
        object.team = Team.ENEMY;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
        
        addStaticData(GameObjectType.BROBOT, object, sprite);
        
        object.commitUpdates();
        
        SimplePhysicsComponent normalPhysics = object.findByClass(SimplePhysicsComponent.class);
        if (normalPhysics != null) {
            ghostSwap.addSwapOutComponent(normalPhysics);
        }
        
        ghostSwap.addSwapInComponent(ghostPhysics);
        
        sprite.playAnimation(0);
                
        // Sparks
        setupEnemySparks();
        
        RenderComponent sparksRender = (RenderComponent)allocateComponent(RenderComponent.class);
        sparksRender.setPriority(render.getPriority() + 1);
        SpriteComponent sparksSprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sparksSprite.setSize(64, 64);
        sparksSprite.setRenderComponent(sparksRender);
        
        addStaticData(GameObjectType.ENEMY_SPARKS, object, sparksSprite);

        sparksSprite.playAnimation(0);
        
        ghostSwap.addSwapInComponent(sparksSprite);
        ghostSwap.addSwapInComponent(sparksRender);
        
        
        hitReact.setPossessionComponent(ghostSwap);
                
        return object;
    }
    
    public GameObject spawnEnemySnailBomb(float positionX, float positionY, boolean flipHorizontal) {
        
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        // Make sure related textures are loaded.
        textureLibrary.allocateTexture(R.drawable.snail_bomb);
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mNormalActivationRadius;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.SNAILBOMB);
        if (staticData == null) {
            final int staticObjectCount = 6;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            GameComponent physics = allocateComponent(SimplePhysicsComponent.class);

            // Animations
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new AABoxCollisionVolume(12, 5, 42, 27, HitType.HIT));
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new AABoxCollisionVolume(12, 5, 42, 27, HitType.HIT));
            
            SpriteAnimation idle = new SpriteAnimation(EnemyAnimations.IDLE.ordinal(), 1);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_stand), 
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume));
            
            SpriteAnimation walk = new SpriteAnimation(EnemyAnimations.MOVE.ordinal(), 5);
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_stand), 
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                        textureLibrary.allocateTexture(R.drawable.snailbomb_walk01), 
                        Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_walk02), 
                    Utils.framesToTime(24, 6), basicAttackVolume, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_walk01), 
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_stand), 
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume));
            walk.setLoop(true);
            
            SpriteAnimation attack = new SpriteAnimation(EnemyAnimations.ATTACK.ordinal(), 2);
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_shoot01), 
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.snailbomb_shoot02), 
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume));
        
            staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            staticData.add(idle);
            staticData.add(walk);
            staticData.add(attack);
            
            
            setStaticData(GameObjectType.SNAILBOMB, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_ENEMY);
        BackgroundCollisionComponent bgcollision 
            = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(32, 48);
        bgcollision.setOffset(16, 5);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);

        EnemyAnimationComponent animation 
            = (EnemyAnimationComponent)allocateComponent(EnemyAnimationComponent.class);
        animation.setSprite(sprite);
        
        PatrolComponent patrol = (PatrolComponent)allocateComponent(PatrolComponent.class);
        patrol.setMovementSpeed(20.0f, 1000.0f);
        patrol.setupAttack(300, 1.0f, 4.0f, true);
        
        DynamicCollisionComponent collision 
            = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact 
            = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setVulnerableToDeathTiles(true);
        lifetime.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF);
        SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        if (sound != null) {
        	lifetime.setDeathSound(sound.load(R.raw.sound_stomp));
        }
        
        LaunchProjectileComponent gun 
            = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
        gun.setSetsPerActivation(1);
        gun.setShotsPerSet(3);
        gun.setDelayBeforeFirstSet(1.0f);
        gun.setDelayBetweenShots(0.25f);
        gun.setObjectTypeToSpawn(GameObjectType.CANNON_BALL);
        gun.setOffsetX(55);
        gun.setOffsetY(21);
        gun.setRequiredAction(GameObject.ActionType.ATTACK);
        gun.setVelocityX(100.0f);
        
        object.team = Team.ENEMY;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }

        object.add(render);
        object.add(sprite);
        object.add(bgcollision);
        object.add(animation);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        object.add(lifetime);
        object.add(gun);
        
        addStaticData(GameObjectType.SNAILBOMB, object, sprite);
        
        final SpriteAnimation attack = sprite.findAnimation(EnemyAnimations.ATTACK.ordinal());
        if (attack != null) {
            gun.setDelayBeforeFirstSet(attack.getLength());
        }
        
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnEnemyShadowSlime(float positionX, float positionY, boolean flipHorizontal) {
        
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        // Make sure related textures are loaded.
        textureLibrary.allocateTexture(R.drawable.energy_ball01);
        textureLibrary.allocateTexture(R.drawable.energy_ball02);
        textureLibrary.allocateTexture(R.drawable.energy_ball03);
        textureLibrary.allocateTexture(R.drawable.energy_ball04);
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.SHADOWSLIME);
        if (staticData == null) {
            final int staticObjectCount = 5;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
             
            PopOutComponent popOut = (PopOutComponent)allocateComponent(PopOutComponent.class);
            // edit: these guys turned out to be really annoying, so I'm changing the values
            // here to force them to always be out.
            popOut.setAppearDistance(2000);
            popOut.setHideDistance(4000);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(16, 32, 32));
            basicVulnerabilityVolume.get(0).setHitType(HitType.HIT);
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(16, 32, 32, HitType.HIT));
            
            SpriteAnimation idle = new SpriteAnimation(EnemyAnimations.IDLE.ordinal(), 2);
            AnimationFrame idle1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_idle01), 
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume); 
            AnimationFrame idle2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_idle02), 
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume); 
            idle.addFrame(idle1);
            idle.addFrame(idle2);
            idle.setLoop(true);
            
            
            SpriteAnimation appear = new SpriteAnimation(EnemyAnimations.APPEAR.ordinal(), 6);
            AnimationFrame appear1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_activate01), 
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume);
            
            AnimationFrame appear2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_activate02), 
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume);
            AnimationFrame appear3 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_activate03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume);
            AnimationFrame appear4 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_activate04), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume);
            AnimationFrame appear5 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_activate05), 
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume);
            AnimationFrame appear6 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_activate06), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume);
            
            appear.addFrame(appear1);
            appear.addFrame(appear2);
            appear.addFrame(appear3);
            appear.addFrame(appear4);
            appear.addFrame(appear5);
            appear.addFrame(appear6);
            
            SpriteAnimation hidden = new SpriteAnimation(EnemyAnimations.HIDDEN.ordinal(), 6);
            hidden.addFrame(appear6);
            hidden.addFrame(appear5);
            hidden.addFrame(appear4);
            hidden.addFrame(appear3);
            hidden.addFrame(appear2);
            hidden.addFrame(appear1);
            /*hidden.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_stand), 
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume));*/
           
            
            SpriteAnimation attack = new SpriteAnimation(EnemyAnimations.ATTACK.ordinal(), 10);
            AnimationFrame attack1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack01), 
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume);
            AnimationFrame attack2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack02), 
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume);
            AnimationFrame attack3 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack03), 
                    Utils.framesToTime(24, 2), basicAttackVolume, basicVulnerabilityVolume);
            AnimationFrame attack4 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack04), 
                    Utils.framesToTime(24, 6), basicAttackVolume, basicVulnerabilityVolume);
            AnimationFrame attackFlash = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_flash), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume);
            
            AnimationFrame attack5 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack03), 
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume);
            AnimationFrame attack6 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack02), 
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume);
            
            AnimationFrame attack7 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_shadowslime_attack04), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume);
            
            attack.addFrame(attack1);
            attack.addFrame(attack2);
            attack.addFrame(attack3);
            attack.addFrame(attack4);
            attack.addFrame(attackFlash);
            attack.addFrame(attack7);
            attack.addFrame(attackFlash);
            attack.addFrame(attack5);
            attack.addFrame(attack6);
            attack.addFrame(attack1);

            popOut.setupAttack(200, 2.0f, attack.getLength());

            
            staticData.add(popOut);
            staticData.add(idle);
            staticData.add(hidden);
            staticData.add(appear);
            staticData.add(attack);
            
            setStaticData(GameObjectType.SHADOWSLIME, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_ENEMY);
        BackgroundCollisionComponent bgcollision 
            = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(32, 48);
        bgcollision.setOffset(16, 5);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);


        sprite.playAnimation(0);
                
        EnemyAnimationComponent animation 
            = (EnemyAnimationComponent)allocateComponent(EnemyAnimationComponent.class);
        animation.setSprite(sprite);
        animation.setFacePlayer(true);
        

        DynamicCollisionComponent collision 
            = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact 
            = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF);
        SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        if (sound != null) {
        	lifetime.setDeathSound(sound.load(R.raw.sound_stomp));
        }
        
        LaunchProjectileComponent gun 
            = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
        
      
        gun.setShotsPerSet(1);
        gun.setSetsPerActivation(1);
        gun.setObjectTypeToSpawn(GameObjectType.ENERGY_BALL);
        gun.setOffsetX(44);
        gun.setOffsetY(22);
        gun.setRequiredAction(GameObject.ActionType.ATTACK);
        gun.setVelocityX(30.0f);
        
        object.team = Team.ENEMY;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
        
        // Hack.  Adjusting position lets us avoid giving this character gravity, physics, and
        // collision.
        
        object.getPosition().y -= 5;

        object.add(render);
        object.add(sprite);
        object.add(bgcollision);
        object.add(animation);
        object.add(collision);
        object.add(hitReact);
        object.add(lifetime);
        object.add(gun);
        
        addStaticData(GameObjectType.SHADOWSLIME, object, sprite);
        
        final SpriteAnimation attack = sprite.findAnimation(EnemyAnimations.ATTACK.ordinal());
        final SpriteAnimation appear = sprite.findAnimation(EnemyAnimations.APPEAR.ordinal());
        if (attack != null && appear != null) {
            gun.setDelayBeforeFirstSet(attack.getLength() / 2.0f);
        } else {
            gun.setDelayBeforeFirstSet(Utils.framesToTime(24, 12));
        }
        
        return object;
    }

    public GameObject spawnEnemyMudman(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        
    
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mNormalActivationRadius;
        object.width = 128;
        object.height = 128;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.MUDMAN);
        if (staticData == null) {
            final int staticObjectCount = 7;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            GameComponent physics = allocateComponent(SimplePhysicsComponent.class);
            
            SolidSurfaceComponent solidSurface 
                = (SolidSurfaceComponent)allocateComponent(SolidSurfaceComponent.class);
            solidSurface.inititalize(4);
            // house shape:
            // / \      1/ \2
            // | |      3| |4
            Vector2 surface1Start = new Vector2(32, 64);
            Vector2 surface1End = new Vector2(64, 96);
            Vector2 surface1Normal = new Vector2(-0.707f, 0.707f);
            surface1Normal.normalize();
            
            Vector2 surface2Start = new Vector2(64, 96);
            Vector2 surface2End = new Vector2(75, 64);
            Vector2 surface2Normal = new Vector2(0.9456f, 0.3250f);
            surface2Normal.normalize();
            
            Vector2 surface3Start = new Vector2(32, 0);
            Vector2 surface3End = new Vector2(32, 64);
            Vector2 surface3Normal = new Vector2(-1, 0);
            
            Vector2 surface4Start = new Vector2(75, 0);
            Vector2 surface4End = new Vector2(75, 64);
            Vector2 surface4Normal = new Vector2(1, 0);
            
            solidSurface.addSurface(surface1Start, surface1End, surface1Normal);
            solidSurface.addSurface(surface2Start, surface2End, surface2Normal);
            solidSurface.addSurface(surface3Start, surface3End, surface3Normal);
            solidSurface.addSurface(surface4Start, surface4End, surface4Normal);
            
            SpriteAnimation idle = new SpriteAnimation(EnemyAnimations.IDLE.ordinal(), 4);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_stand), 
                    Utils.framesToTime(24, 12), null, null));
            AnimationFrame idle1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_idle01), 
                    Utils.framesToTime(24, 2), null, null);
            AnimationFrame idle2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_idle01), 
                    Utils.framesToTime(24, 7), null, null);
            idle.addFrame(idle1);
            idle.addFrame(idle2);
            idle.addFrame(idle1);
            idle.setLoop(true);

            
            SpriteAnimation walk = new SpriteAnimation(EnemyAnimations.MOVE.ordinal(), 6);
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_walk01), 
                    Utils.framesToTime(24, 4), null, null));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_walk02), 
                    Utils.framesToTime(24, 4), null, null));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_walk03), 
                    Utils.framesToTime(24, 5), null, null));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_walk04), 
                    Utils.framesToTime(24, 4), null, null));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_walk05), 
                    Utils.framesToTime(24, 4), null, null));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_walk06), 
                    Utils.framesToTime(24, 5), null, null));
            walk.setLoop(true);
            
            FixedSizeArray<CollisionVolume> crushAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            crushAttackVolume.add(new AABoxCollisionVolume(64, 0, 64, 96, HitType.HIT));
            
            SpriteAnimation attack = new SpriteAnimation(EnemyAnimations.ATTACK.ordinal(), 8);
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_stand), 
                    Utils.framesToTime(24, 2), null, null));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack01), 
                    Utils.framesToTime(24, 2), null, null));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack02), 
                    Utils.framesToTime(24, 2), null, null));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack03), 
                    Utils.framesToTime(24, 2), null, null));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack04), 
                    Utils.framesToTime(24, 1), crushAttackVolume, null));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack05), 
                    Utils.framesToTime(24, 1), crushAttackVolume, null));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack06), 
                    Utils.framesToTime(24, 8), crushAttackVolume, null));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_mud_attack07), 
                    Utils.framesToTime(24, 5), null, null));
            
            staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            staticData.add(solidSurface);
            staticData.add(idle);
            staticData.add(walk);
            staticData.add(attack);
            
            setStaticData(GameObjectType.MUDMAN, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_ENEMY);
        
        BackgroundCollisionComponent bgcollision = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
		bgcollision.setSize(80, 90);
		bgcollision.setOffset(32, 5);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
    
        sprite.playAnimation(0);
            
        EnemyAnimationComponent animation = (EnemyAnimationComponent)allocateComponent(EnemyAnimationComponent.class);
        animation.setSprite(sprite);
        
        PatrolComponent patrol = (PatrolComponent)allocateComponent(PatrolComponent.class);
        patrol.setMovementSpeed(20.0f, 400.0f);
        
        DynamicCollisionComponent collision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);

        object.team = Team.ENEMY;
        object.life = 1;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
        
        object.add(render);
        object.add(sprite);
        object.add(bgcollision);
        object.add(animation);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        object.add(lifetime);
       
        addStaticData(GameObjectType.MUDMAN, object, sprite);
        
        final SpriteAnimation attack = sprite.findAnimation(EnemyAnimations.ATTACK.ordinal());
        if (attack != null) {
            patrol.setupAttack(70.0f, attack.getLength(), 0.0f, true);
        }
        
        sprite.playAnimation(0);
        
        return object;
    }
    
    public GameObject spawnEnemySkeleton(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mNormalActivationRadius;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.SKELETON);
        if (staticData == null) {
            final int staticObjectCount = 7;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            GameComponent physics = allocateComponent(SimplePhysicsComponent.class);

            SolidSurfaceComponent solidSurface = (SolidSurfaceComponent)allocateComponent(SolidSurfaceComponent.class);
            solidSurface.inititalize(4);
         
            Vector2 surface1Start = new Vector2(25, 0);
            Vector2 surface1End = new Vector2(25, 64);
            Vector2 surface1Normal = new Vector2(-1, 0);
            
            Vector2 surface2Start = new Vector2(40, 0);
            Vector2 surface2End = new Vector2(40, 64);
            Vector2 surface2Normal = new Vector2(1, 0);
            
            solidSurface.addSurface(surface1Start, surface1End, surface1Normal);
            solidSurface.addSurface(surface2Start, surface2End, surface2Normal);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(16, 32, 32));
            basicVulnerabilityVolume.get(0).setHitType(HitType.HIT);
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(16, 48, 32, HitType.HIT));
            
            SpriteAnimation idle = new SpriteAnimation(EnemyAnimations.IDLE.ordinal(), 1);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_stand), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            
            SpriteAnimation walk = new SpriteAnimation(EnemyAnimations.MOVE.ordinal(), 6);
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_walk01), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_walk02), 
                    Utils.framesToTime(24, 4), null, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_walk03), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_walk04), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_walk05), 
                    Utils.framesToTime(24, 4), null, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_walk03), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume));

            walk.setLoop(true);
            
            
            SpriteAnimation attack = new SpriteAnimation(EnemyAnimations.ATTACK.ordinal(), 3);
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_attack01), 
                    Utils.framesToTime(24, 5), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_attack03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_skeleton_attack04), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            
            staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            staticData.add(solidSurface);
            staticData.add(idle);
            staticData.add(walk);
            staticData.add(attack);
            
            setStaticData(GameObjectType.SKELETON, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_ENEMY);
        
        BackgroundCollisionComponent bgcollision = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
		bgcollision.setSize(32, 48);
		bgcollision.setOffset(16, 5);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        EnemyAnimationComponent animation = (EnemyAnimationComponent)allocateComponent(EnemyAnimationComponent.class);
        animation.setSprite(sprite);
        
        PatrolComponent patrol = (PatrolComponent)allocateComponent(PatrolComponent.class);
        patrol.setMovementSpeed(20.0f, 1000.0f);
        patrol.setTurnToFacePlayer(true);

        DynamicCollisionComponent collision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setVulnerableToDeathTiles(true);
        lifetime.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF);
        SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        if (sound != null) {
        	lifetime.setDeathSound(sound.load(R.raw.sound_stomp));
        }
        
        object.team = Team.ENEMY;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
                
        object.add(render);
        object.add(sprite);
        object.add(bgcollision);
        object.add(animation);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        object.add(lifetime);
        
        addStaticData(GameObjectType.SKELETON, object, sprite);
        
        final SpriteAnimation attack = sprite.findAnimation(EnemyAnimations.ATTACK.ordinal());
        if (attack != null) {
            patrol.setupAttack(75.0f, attack.getLength(), 2.0f, true);
        }
        
        sprite.playAnimation(0);

        return object;
    }
    
    
    public GameObject spawnEnemyKaraguin(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mNormalActivationRadius;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.KARAGUIN);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
           
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(8, 16, 16));
            basicVulnerabilityVolume.get(0).setHitType(HitType.HIT);
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(8, 16, 16, HitType.HIT));
            
            SpriteAnimation idle = new SpriteAnimation(0, 3);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_karaguin01), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_karaguin02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_karaguin03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            idle.setLoop(true);
           
            staticData.add(movement);
            staticData.add(idle);
            
            
            setStaticData(GameObjectType.KARAGUIN, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_ENEMY);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
         
        PatrolComponent patrol = (PatrolComponent)allocateComponent(PatrolComponent.class);
        patrol.setMovementSpeed(50.0f, 1000.0f);
        patrol.setTurnToFacePlayer(false);
        patrol.setFlying(true);

        DynamicCollisionComponent collision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF);
        SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        if (sound != null) {
        	lifetime.setDeathSound(sound.load(R.raw.sound_stomp));
        }
        
        EnemyAnimationComponent animation = (EnemyAnimationComponent)allocateComponent(EnemyAnimationComponent.class);
        animation.setSprite(sprite);
        
        object.team = Team.ENEMY;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
        // HACK.  These guys originally moved on their own, so let's keep them that way.
        object.getVelocity().x = 50.0f * object.facingDirection.x;
        object.getTargetVelocity().x = 50.0f * object.facingDirection.x;
                
        object.add(render);
        object.add(animation);
        object.add(sprite);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        object.add(lifetime);
        
        addStaticData(GameObjectType.KARAGUIN, object, sprite);
        
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnEnemyPinkNamazu(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        
    
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 128;
        object.height = 128;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.PINK_NAMAZU);
        if (staticData == null) {
            final int staticObjectCount = 7;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            GameComponent physics = allocateComponent(SimplePhysicsComponent.class);
            
            SolidSurfaceComponent solidSurface 
                = (SolidSurfaceComponent)allocateComponent(SolidSurfaceComponent.class);
            solidSurface.inititalize(5);
            // circle shape:
            //  __        __3
            // /  \      2/ \4
            // |   |     1|  |5
            /*
                0:12,6:22,52:0.98058067569092,-0.19611613513818
                0:22,52:50,75:-0.62580046626293,0.77998318983495
                0:50,75:81,75:0,1
                0:81,75:104,49:0.74038072228541,0.67218776102228
                0:104,49:104,6:-0.99997086544204,-0.00763336538505
             */
            Vector2 surface1Start = new Vector2(12, 3);
            Vector2 surface1End = new Vector2(22, 52);
            Vector2 surface1Normal = new Vector2(-0.98058067569092f, -0.19611613513818f);
            surface1Normal.normalize();
            
            Vector2 surface2Start = new Vector2(22, 52);
            Vector2 surface2End = new Vector2(50, 75);
            Vector2 surface2Normal = new Vector2(-0.62580046626293f, 0.77998318983495f);
            surface2Normal.normalize();
            
            Vector2 surface3Start = new Vector2(50, 75);
            Vector2 surface3End = new Vector2(81, 75);
            Vector2 surface3Normal = new Vector2(0, 1);
            
            Vector2 surface4Start = new Vector2(81, 75);
            Vector2 surface4End = new Vector2(104,49);
            Vector2 surface4Normal = new Vector2(0.74038072228541f, 0.67218776102228f);
            
            Vector2 surface5Start = new Vector2(104,49);
            Vector2 surface5End = new Vector2(104, 3);
            Vector2 surface5Normal = new Vector2(1.0f, 0.0f);
            
            solidSurface.addSurface(surface1Start, surface1End, surface1Normal);
            solidSurface.addSurface(surface2Start, surface2End, surface2Normal);
            solidSurface.addSurface(surface3Start, surface3End, surface3Normal);
            solidSurface.addSurface(surface4Start, surface4End, surface4Normal);
            solidSurface.addSurface(surface5Start, surface5End, surface5Normal);

            
            SpriteAnimation idle = new SpriteAnimation(GenericAnimationComponent.Animation.IDLE, 4);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_pinkdude_stand), 
                    Utils.framesToTime(24, 8), null, null));
            AnimationFrame idle1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_pinkdude_sleep01), 
                    Utils.framesToTime(24, 3), null, null);
            AnimationFrame idle2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_pinkdude_sleep02), 
                    Utils.framesToTime(24, 8), null, null);
            idle.addFrame(idle1);
            idle.addFrame(idle2);
            idle.addFrame(idle1);
            idle.setLoop(true);

            
            SpriteAnimation wake = new SpriteAnimation(GenericAnimationComponent.Animation.MOVE, 4);
            AnimationFrame wake1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_pinkdude_eyeopen), 
                    Utils.framesToTime(24, 3), null, null);
            AnimationFrame wake2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_pinkdude_stand), 
                    Utils.framesToTime(24, 3), null, null);
            wake.addFrame(wake1);
            wake.addFrame(wake2);
            wake.addFrame(wake1);
            wake.addFrame(wake2);
            
            FixedSizeArray<CollisionVolume> crushAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            crushAttackVolume.add(new AABoxCollisionVolume(32, 0, 64, 32, HitType.HIT));
            
            SpriteAnimation attack = new SpriteAnimation(GenericAnimationComponent.Animation.ATTACK, 1);
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_pinkdude_jump), 
                    Utils.framesToTime(24, 2), crushAttackVolume, null));
            
            
            staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            staticData.add(solidSurface);
            staticData.add(idle);
            staticData.add(wake);
            staticData.add(attack);
            
            setStaticData(GameObjectType.PINK_NAMAZU, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_ENEMY);
        
        BackgroundCollisionComponent bgcollision = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(100, 75);
        bgcollision.setOffset(12, 5);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
                   
        GenericAnimationComponent animation = 
            (GenericAnimationComponent)allocateComponent(GenericAnimationComponent.class);
        animation.setSprite(sprite);
        
        SleeperComponent sleeper = (SleeperComponent)allocateComponent(SleeperComponent.class);
        sleeper.setAttackImpulse(100.0f, 170.0f);
        sleeper.setSlam(0.3f, 25.0f);
        
        DynamicCollisionComponent collision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        

        object.team = Team.ENEMY;
        object.life = 1;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
        
        object.add(render);
        object.add(sprite);
        object.add(bgcollision);
        object.add(animation);
        object.add(collision);
        object.add(hitReact);
        object.add(sleeper);

       
        addStaticData(GameObjectType.PINK_NAMAZU, object, sprite);
        
        final SpriteAnimation wakeUp = sprite.findAnimation(GenericAnimationComponent.Animation.MOVE);
        if (wakeUp != null) {
            sleeper.setWakeUpDuration(wakeUp.getLength() + 1.0f);
        }
        
        sprite.playAnimation(GenericAnimationComponent.Animation.IDLE);
        
        return object;
    }
    
    public GameObject spawnEnemyBat(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mNormalActivationRadius;
        object.width = 64;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.BAT);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
           
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(16, 32, 16));
            basicVulnerabilityVolume.get(0).setHitType(HitType.HIT);
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(16, 32, 16, HitType.HIT));
            
            SpriteAnimation idle = new SpriteAnimation(0, 4);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_bat01), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_bat02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_bat03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_bat04), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            idle.setLoop(true);
           
            staticData.add(movement);
            staticData.add(idle);
            
            
            setStaticData(GameObjectType.BAT, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_ENEMY);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
         
        PatrolComponent patrol = (PatrolComponent)allocateComponent(PatrolComponent.class);
        patrol.setMovementSpeed(75.0f, 1000.0f);
        patrol.setTurnToFacePlayer(false);
        patrol.setFlying(true);

        DynamicCollisionComponent collision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF);
        SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        if (sound != null) {
        	lifetime.setDeathSound(sound.load(R.raw.sound_stomp));
        }

        EnemyAnimationComponent animation = (EnemyAnimationComponent)allocateComponent(EnemyAnimationComponent.class);
        animation.setSprite(sprite);
        
        object.team = Team.ENEMY;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
                
        // HACK.  These guys originally moved on their own, so let's keep them that way.
        object.getVelocity().x = 75.0f * object.facingDirection.x;
        object.getTargetVelocity().x = 75.0f * object.facingDirection.x;
        
        object.add(render);
        object.add(animation);
        object.add(sprite);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        object.add(lifetime);
        
        addStaticData(GameObjectType.BAT, object, sprite);
        
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnEnemySting(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mNormalActivationRadius;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.STING);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
           
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(16, 32, 16));
            basicVulnerabilityVolume.get(0).setHitType(HitType.HIT);
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(16, 32, 16, HitType.HIT));
            
            SpriteAnimation idle = new SpriteAnimation(0, 3);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_sting01), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_sting02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_sting03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
           
            idle.setLoop(true);
           
            staticData.add(movement);
            staticData.add(idle);
            
            
            setStaticData(GameObjectType.STING, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_ENEMY);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
         
        PatrolComponent patrol = (PatrolComponent)allocateComponent(PatrolComponent.class);
        patrol.setMovementSpeed(75.0f, 1000.0f);
        patrol.setTurnToFacePlayer(false);
        patrol.setFlying(true);

        DynamicCollisionComponent collision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF);
        SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        if (sound != null) {
        	lifetime.setDeathSound(sound.load(R.raw.sound_stomp));
        }
        
        EnemyAnimationComponent animation = (EnemyAnimationComponent)allocateComponent(EnemyAnimationComponent.class);
        animation.setSprite(sprite);
        
        object.team = Team.ENEMY;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
                
        // HACK.  These guys originally moved on their own, so let's keep them that way.
        object.getVelocity().x = 25.0f * object.facingDirection.x;
        object.getTargetVelocity().x = 25.0f * object.facingDirection.x;
        
        object.add(render);
        object.add(animation);
        object.add(sprite);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        object.add(lifetime);
        
        addStaticData(GameObjectType.STING, object, sprite);
        
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnEnemyOnion(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mNormalActivationRadius;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.ONION);
        if (staticData == null) {
            final int staticObjectCount = 5;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            SimplePhysicsComponent physics = (SimplePhysicsComponent)allocateComponent(SimplePhysicsComponent.class);
            physics.setBounciness(0.2f);

            
            // Animations
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(16, 32, 32));
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(16, 32, 32, HitType.HIT));
            
            SpriteAnimation idle = new SpriteAnimation(EnemyAnimations.IDLE.ordinal(), 1);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_onion01), 
                    Utils.framesToTime(24, 3), basicAttackVolume, basicVulnerabilityVolume));
           
            idle.setLoop(true);
            
            SpriteAnimation walk = new SpriteAnimation(EnemyAnimations.MOVE.ordinal(), 3);
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_onion01), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_onion02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            walk.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_onion03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, basicVulnerabilityVolume));
            walk.setLoop(true);
            
            staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            staticData.add(idle);
            staticData.add(walk);
            
            setStaticData(GameObjectType.ONION, staticData);
            
        }
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_ENEMY);
        
        BackgroundCollisionComponent bgcollision 
            = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(32, 48);
        bgcollision.setOffset(16, 5);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
 
        EnemyAnimationComponent animation 
            = (EnemyAnimationComponent)allocateComponent(EnemyAnimationComponent.class);
        animation.setSprite(sprite);
        
        PatrolComponent patrol = (PatrolComponent)allocateComponent(PatrolComponent.class);
        patrol.setMovementSpeed(50.0f, 1000.0f);
        
        DynamicCollisionComponent collision 
            = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact 
            = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setVulnerableToDeathTiles(true);
        lifetime.setObjectToSpawnOnDeath(GameObjectType.SMOKE_POOF);
        SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        if (sound != null) {
        	lifetime.setDeathSound(sound.load(R.raw.sound_stomp));
        }
        
        object.add(render);
        object.add(sprite);
        
        object.add(bgcollision);
        object.add(animation);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        object.add(lifetime);
        
        object.team = Team.ENEMY;
        
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
        
        addStaticData(GameObjectType.ONION, object, sprite);
        
        sprite.playAnimation(0);
                
        return object;
    }
    
    public GameObject spawnEnemyWanda(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        // Make sure related textures are loaded.
        textureLibrary.allocateTexture(R.drawable.energy_ball01);
        textureLibrary.allocateTexture(R.drawable.energy_ball02);
        textureLibrary.allocateTexture(R.drawable.energy_ball03);
        textureLibrary.allocateTexture(R.drawable.energy_ball04);
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mAlwaysActive;
        object.width = 64;
        object.height = 128;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.WANDA);
        if (staticData == null) {
            final int staticObjectCount = 9;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            SimplePhysicsComponent physics = (SimplePhysicsComponent)allocateComponent(SimplePhysicsComponent.class);
            physics.setBounciness(0.0f);

            
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new AABoxCollisionVolume(20, 5, 26, 80, HitType.COLLECT));
                    
            SpriteAnimation idle = new SpriteAnimation(NPCAnimationComponent.IDLE, 1);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_stand), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            
            
            
            AnimationFrame walkFrame1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_walk01), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_walk02), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame3 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_walk03), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame4 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_walk04), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame5 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_walk05), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            SpriteAnimation walk = new SpriteAnimation(NPCAnimationComponent.WALK, 8);
            walk.addFrame(walkFrame1);
            walk.addFrame(walkFrame2);
            walk.addFrame(walkFrame3);
            walk.addFrame(walkFrame4);
            walk.addFrame(walkFrame5);
            walk.addFrame(walkFrame4);
            walk.addFrame(walkFrame3);
            walk.addFrame(walkFrame2);
            walk.setLoop(true);
            
            
            AnimationFrame runFrame4 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run04), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            
            SpriteAnimation run = new SpriteAnimation(NPCAnimationComponent.RUN, 9);
            run.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run01), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            run.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run02), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            run.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run03), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            run.addFrame(runFrame4);
            run.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run05), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            run.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run06), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            run.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run07), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            run.addFrame(runFrame4);
            run.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run08), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            run.setLoop(true);
            
            SpriteAnimation jumpStart = new SpriteAnimation(NPCAnimationComponent.JUMP_START, 4);
            AnimationFrame jump1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_jump01), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            AnimationFrame jump2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_jump01), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            jumpStart.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_run04), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            jumpStart.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_crouch), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            jumpStart.addFrame(jump1);
            jumpStart.addFrame(jump2);
            
            SpriteAnimation jumpAir = new SpriteAnimation(NPCAnimationComponent.JUMP_AIR, 2);
            jumpAir.addFrame(jump1);
            jumpAir.addFrame(jump2);
            jumpAir.setLoop(true);
            
            SpriteAnimation attack = new SpriteAnimation(NPCAnimationComponent.SHOOT, 11);
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot01), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot02), 
                    Utils.framesToTime(24, 8), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot03), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot04), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot05), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot06), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot07), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot08), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot09), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot02), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_wanda_shoot01), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume));
            
            staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            staticData.add(idle);
            staticData.add(walk);
            staticData.add(run);
            staticData.add(jumpStart);
            staticData.add(jumpAir);
            staticData.add(attack);
            
            setStaticData(GameObjectType.WANDA, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.NPC);
        
        BackgroundCollisionComponent bgcollision = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(32, 82);
        bgcollision.setOffset(20, 5);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        NPCAnimationComponent animation = (NPCAnimationComponent)allocateComponent(NPCAnimationComponent.class);
        animation.setSprite(sprite);
        
        NPCComponent patrol = (NPCComponent)allocateComponent(NPCComponent.class);
      
        DynamicCollisionComponent collision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        patrol.setHitReactionComponent(hitReact);
        
        SoundSystem sound = sSystemRegistry.soundSystem;
        
        LaunchProjectileComponent gun 
            = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
        gun.setShotsPerSet(1);
        gun.setSetsPerActivation(1); 
        gun.setDelayBeforeFirstSet(Utils.framesToTime(24, 11));
        gun.setObjectTypeToSpawn(GameObjectType.WANDA_SHOT);
        gun.setOffsetX(45);
        gun.setOffsetY(42);
        gun.setRequiredAction(GameObject.ActionType.ATTACK);
        gun.setVelocityX(300.0f);
        gun.setShootSound(sound.load(R.raw.sound_poing));
        
        object.team = Team.ENEMY;
        object.life = 1;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
             
        object.add(gun);
        object.add(render);
        object.add(sprite);
        object.add(bgcollision);
        object.add(animation);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        
        addStaticData(GameObjectType.WANDA, object, sprite);
        
        
        sprite.playAnimation(0);

        return object;
    }
    
    
    public GameObject spawnEnemyKyle(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mAlwaysActive;
        object.width = 64;
        object.height = 128;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.KYLE);
        if (staticData == null) {
            final int staticObjectCount = 9;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            SimplePhysicsComponent physics = (SimplePhysicsComponent)allocateComponent(SimplePhysicsComponent.class);
            physics.setBounciness(0.0f);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new AABoxCollisionVolume(20, 5, 26, 80, HitType.COLLECT));
                    
            SpriteAnimation idle = new SpriteAnimation(NPCAnimationComponent.IDLE, 1);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_stand), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));

            AnimationFrame walkFrame1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk01), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk02), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame3 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk03), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame4 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk04), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame5 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk05), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame6 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk06), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame7 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_walk07), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            SpriteAnimation walk = new SpriteAnimation(NPCAnimationComponent.WALK, 12);
            walk.addFrame(walkFrame1);
            walk.addFrame(walkFrame2);
            walk.addFrame(walkFrame3);
            walk.addFrame(walkFrame4);
            walk.addFrame(walkFrame3);
            walk.addFrame(walkFrame2);
            walk.addFrame(walkFrame1);
            walk.addFrame(walkFrame5);
            walk.addFrame(walkFrame6);
            walk.addFrame(walkFrame7);
            walk.addFrame(walkFrame6);
            walk.addFrame(walkFrame5);
            
            walk.setLoop(true);
            
            AnimationFrame crouch1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_crouch01), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            AnimationFrame crouch2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_crouch02), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            
            SpriteAnimation runStart = new SpriteAnimation(NPCAnimationComponent.RUN_START, 2);
            runStart.addFrame(crouch1);
            runStart.addFrame(crouch2);
            
            FixedSizeArray<CollisionVolume> attackVolume = 
                new FixedSizeArray<CollisionVolume>(2);
            attackVolume.add(new AABoxCollisionVolume(32, 32, 50, 32, HitType.HIT));
            attackVolume.add(new AABoxCollisionVolume(32, 32, 50, 32, HitType.COLLECT));
            
            SpriteAnimation run = new SpriteAnimation(NPCAnimationComponent.RUN, 2);
            run.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_dash01), 
                    Utils.framesToTime(24, 1), attackVolume, basicVulnerabilityVolume));
            run.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_dash02), 
                    Utils.framesToTime(24, 1), attackVolume, basicVulnerabilityVolume));
            run.setLoop(true);
            
            SpriteAnimation jumpStart = new SpriteAnimation(NPCAnimationComponent.JUMP_START, 2);
            jumpStart.addFrame(crouch1);
            jumpStart.addFrame(crouch2);
            
            SpriteAnimation jumpAir = new SpriteAnimation(NPCAnimationComponent.JUMP_AIR, 2);
            AnimationFrame jump1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_jump01), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            AnimationFrame jump2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kyle_jump01), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            jumpAir.addFrame(jump1);
            jumpAir.addFrame(jump2);
            jumpAir.setLoop(true);
            
            
            
            staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            staticData.add(idle);
            staticData.add(walk);
            staticData.add(runStart);
            staticData.add(run);
            staticData.add(jumpStart);
            staticData.add(jumpAir);
            
            setStaticData(GameObjectType.KYLE, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.NPC);

        BackgroundCollisionComponent bgcollision = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(32, 90);
        bgcollision.setOffset(20, 5); 
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        NPCAnimationComponent animation = (NPCAnimationComponent)allocateComponent(NPCAnimationComponent.class);
        animation.setSprite(sprite);
        animation.setStopAtWalls(false); // Kyle can run through walls
        
        NPCComponent patrol = (NPCComponent)allocateComponent(NPCComponent.class);
        patrol.setSpeeds(350.0f, 50.0f, 400.0f, -10.0f, 400.0f);
        patrol.setGameEvent(GameFlowEvent.EVENT_SHOW_ANIMATION, AnimationPlayerActivity.KYLE_DEATH, false);

        DynamicCollisionComponent collision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        patrol.setHitReactionComponent(hitReact); 
        
        MotionBlurComponent motionBlur = (MotionBlurComponent)allocateComponent(MotionBlurComponent.class);
        motionBlur.setTarget(render);
        
        LauncherComponent launcher = (LauncherComponent)allocateComponent(LauncherComponent.class);
        launcher.setup((float)(Math.PI * 0.45f), 1000.0f, 0.0f, 0.0f, false);
        launcher.setLaunchEffect(GameObjectType.FLASH, 70.0f, 50.0f);
        hitReact.setLauncherComponent(launcher, HitType.HIT);
        
        object.team = Team.NONE;
        object.life = 1;

        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
                
        object.add(render);
        object.add(sprite);
        object.add(bgcollision);
        object.add(animation);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        object.add(motionBlur);
        object.add(launcher);
        
        addStaticData(GameObjectType.KYLE, object, sprite);
        
        
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnEnemyKyleDead(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 128;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.KYLE_DEAD);
        if (staticData == null) {
            final int staticObjectCount = 1;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
                  
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new AABoxCollisionVolume(32, 5, 64, 32, HitType.COLLECT));
            
            SpriteAnimation idle = new SpriteAnimation(0, 1);
            AnimationFrame frame1 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.enemy_kyle_dead), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            
            idle.addFrame(frame1);
           
            idle.setLoop(true);
            
            staticData.add(idle);
            
            setStaticData(GameObjectType.KYLE_DEAD, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_OBJECT);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);

        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        dynamicCollision.setHitReactionComponent(hitReact);
        hitReact.setSpawnGameEventOnHit(HitType.COLLECT, GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2, 0);
        
        SelectDialogComponent dialogSelect = (SelectDialogComponent)allocateComponent(SelectDialogComponent.class);
        dialogSelect.setHitReact(hitReact);
        
        // Since this object doesn't have gravity or background collision, adjust down to simulate the position
        // at which a bounding volume would rest.
        
        object.getPosition().y -= 5.0f;
        
        object.add(dialogSelect);
        object.add(render);
        object.add(sprite);
        object.add(dynamicCollision);
        object.add(hitReact);
        
        addStaticData(GameObjectType.KYLE_DEAD, object, sprite);
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnEnemyAndouDead(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.ANDOU_DEAD);
        if (staticData == null) {
            final int staticObjectCount = 1;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
                  
            SpriteAnimation idle = new SpriteAnimation(0, 1);
            AnimationFrame frame1 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_explode12), 
                    Utils.framesToTime(24, 1), null, null);
            
            idle.addFrame(frame1);
           
            idle.setLoop(true);
            
            staticData.add(idle);
            
            setStaticData(GameObjectType.ANDOU_DEAD, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_OBJECT);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);

        LaunchProjectileComponent smokeGun 
	        = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
	    smokeGun.setDelayBetweenShots(0.25f);
	    smokeGun.setObjectTypeToSpawn(GameObjectType.SMOKE_BIG);
	    smokeGun.setOffsetX(32);
	    smokeGun.setOffsetY(15);
	    smokeGun.setVelocityX(-150.0f);
	    smokeGun.setVelocityY(100.0f);
	    smokeGun.setThetaError(0.1f);
	    
	    LaunchProjectileComponent smokeGun2 
	        = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
	    smokeGun2.setDelayBetweenShots(0.35f);
	    smokeGun2.setObjectTypeToSpawn(GameObjectType.SMOKE_SMALL);
	    smokeGun2.setOffsetX(16);
	    smokeGun2.setOffsetY(15);
	    smokeGun2.setVelocityX(-150.0f);
	    smokeGun2.setVelocityY(150.0f);
	    smokeGun2.setThetaError(0.1f); 
             
        object.add(render);
        object.add(sprite);
        object.add(smokeGun);
        object.add(smokeGun2);
        
        object.facingDirection.x = -1.0f;
        
        addStaticData(GameObjectType.ANDOU_DEAD, object, sprite);
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnEnemyKabocha(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mAlwaysActive;
        object.width = 64;
        object.height = 128;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.KABOCHA);
        if (staticData == null) {
            final int staticObjectCount = 5;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            SimplePhysicsComponent physics = (SimplePhysicsComponent)allocateComponent(SimplePhysicsComponent.class);
            physics.setBounciness(0.0f);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new AABoxCollisionVolume(20, 5, 26, 80, HitType.COLLECT));
                    
            SpriteAnimation idle = new SpriteAnimation(NPCAnimationComponent.IDLE, 1);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_stand), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));

            AnimationFrame walkFrame1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_walk01), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_walk02), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame3 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_walk03), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame4 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_walk04), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame5 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_walk05), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame6 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_walk06), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            
            SpriteAnimation walk = new SpriteAnimation(NPCAnimationComponent.WALK, 6);
            walk.addFrame(walkFrame1);
            walk.addFrame(walkFrame2);
            walk.addFrame(walkFrame3);
            walk.addFrame(walkFrame4);
            walk.addFrame(walkFrame5);
            walk.addFrame(walkFrame6);
  
            
            walk.setLoop(true);
            
            staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            staticData.add(idle);
            staticData.add(walk);
            
            setStaticData(GameObjectType.KABOCHA, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.NPC);

        BackgroundCollisionComponent bgcollision = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(38, 82);
        bgcollision.setOffset(16, 5);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        NPCAnimationComponent animation = (NPCAnimationComponent)allocateComponent(NPCAnimationComponent.class);
        animation.setSprite(sprite);
        
        NPCComponent patrol = (NPCComponent)allocateComponent(NPCComponent.class);
        
        DynamicCollisionComponent collision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        patrol.setHitReactionComponent(hitReact);
        
        object.team = Team.ENEMY;
        object.life = 1;

        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
                
        object.add(render);
        object.add(sprite);
        object.add(bgcollision);
        object.add(animation);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        
        addStaticData(GameObjectType.KABOCHA, object, sprite);
        
        
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnRokudouTerminal(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.ROKUDOU_TERMINAL);
        if (staticData == null) {
            final int staticObjectCount = 1;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new AABoxCollisionVolume(0, 0, 64, 64));
            basicVulnerabilityVolume.get(0).setHitType(HitType.COLLECT);
            
           
            AnimationFrame frame1 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal01), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            AnimationFrame frame2 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal02), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            AnimationFrame frame3 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal03), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            AnimationFrame frame4 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal01), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame frame5 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal02), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame frame6 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal01), 
                    1.0f, null, basicVulnerabilityVolume);
            
            SpriteAnimation idle = new SpriteAnimation(0, 12);
            idle.addFrame(frame1);
            idle.addFrame(frame5);
            idle.addFrame(frame4);
            idle.addFrame(frame3);
            idle.addFrame(frame2);
            idle.addFrame(frame6);
            idle.addFrame(frame6);
            idle.addFrame(frame3);
            idle.addFrame(frame2);
            idle.addFrame(frame1);
            idle.addFrame(frame2);
            idle.addFrame(frame6);

            idle.setLoop(true);
            
           
            staticData.add(idle);
            
            setStaticData(GameObjectType.ROKUDOU_TERMINAL, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_OBJECT);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);

        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setSpawnGameEventOnHit(HitType.COLLECT, GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2, 0);

        SelectDialogComponent dialogSelect = (SelectDialogComponent)allocateComponent(SelectDialogComponent.class);
        dialogSelect.setHitReact(hitReact);
        
        dynamicCollision.setHitReactionComponent(hitReact);
        
        object.add(dialogSelect);
        object.add(render);
        object.add(sprite);
        object.add(dynamicCollision);
        object.add(hitReact);
        
        addStaticData(GameObjectType.ROKUDOU_TERMINAL, object, sprite);
        sprite.playAnimation(0);

        return object;
    }
    
    
    public GameObject spawnKabochaTerminal(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.KABOCHA_TERMINAL);
        if (staticData == null) {
            final int staticObjectCount = 1;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new AABoxCollisionVolume(0, 0, 64, 64));
            basicVulnerabilityVolume.get(0).setHitType(HitType.COLLECT);
            
           
            AnimationFrame frame1 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal_kabocha01), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            AnimationFrame frame2 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal_kabocha02), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            AnimationFrame frame3 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal_kabocha03), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            AnimationFrame frame4 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal_kabocha01), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame frame5 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal_kabocha02), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame frame6 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_terminal_kabocha01), 
                    1.0f, null, basicVulnerabilityVolume);
            
            SpriteAnimation idle = new SpriteAnimation(0, 12);
            idle.addFrame(frame1);
            idle.addFrame(frame5);
            idle.addFrame(frame4);
            idle.addFrame(frame3);
            idle.addFrame(frame2);
            idle.addFrame(frame6);
            idle.addFrame(frame6);
            idle.addFrame(frame3);
            idle.addFrame(frame2);
            idle.addFrame(frame1);
            idle.addFrame(frame2);
            idle.addFrame(frame6);

            idle.setLoop(true);
            
           
            staticData.add(idle);
            
            setStaticData(GameObjectType.KABOCHA_TERMINAL, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_OBJECT);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);

        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setSpawnGameEventOnHit(HitType.COLLECT, GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2, 0);

        SelectDialogComponent dialogSelect = (SelectDialogComponent)allocateComponent(SelectDialogComponent.class);
        dialogSelect.setHitReact(hitReact);
       
        dynamicCollision.setHitReactionComponent(hitReact);
        
        object.add(dialogSelect);
        object.add(render);
        object.add(sprite);
        object.add(dynamicCollision);
        object.add(hitReact);
        
        addStaticData(GameObjectType.KABOCHA_TERMINAL, object, sprite);
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnEnemyEvilKabocha(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mNormalActivationRadius;
        object.width = 128;
        object.height = 128;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.EVIL_KABOCHA);
        if (staticData == null) {
            final int staticObjectCount = 8;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            SimplePhysicsComponent physics = (SimplePhysicsComponent)allocateComponent(SimplePhysicsComponent.class);
            physics.setBounciness(0.0f);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new AABoxCollisionVolume(52, 5, 26, 80, HitType.HIT));
                    
            SpriteAnimation idle = new SpriteAnimation(NPCAnimationComponent.IDLE, 1);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_stand), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));

            AnimationFrame walkFrame1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_walk01), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_walk02), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame3 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_walk03), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame4 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_walk04), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame5 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_walk05), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            AnimationFrame walkFrame6 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_walk06), 
                    Utils.framesToTime(24, 3), null, basicVulnerabilityVolume);
            
            SpriteAnimation walk = new SpriteAnimation(NPCAnimationComponent.WALK, 6);
            walk.addFrame(walkFrame1);
            walk.addFrame(walkFrame2);
            walk.addFrame(walkFrame3);
            walk.addFrame(walkFrame4);
            walk.addFrame(walkFrame5);
            walk.addFrame(walkFrame6);
  
            walk.setLoop(true);
            
            
            SpriteAnimation surprised = new SpriteAnimation(NPCAnimationComponent.SURPRISED, 1);
            surprised.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_surprised), 
                    4.0f, null, null));
            
            
            SpriteAnimation hit = new SpriteAnimation(NPCAnimationComponent.TAKE_HIT, 2);
            hit.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_hit01), 
                    Utils.framesToTime(24, 1), null, null));
            hit.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_hit02), 
                    Utils.framesToTime(24, 10), null, null));
            
            SpriteAnimation die = new SpriteAnimation(NPCAnimationComponent.DEATH, 5);
            die.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_die01), 
                    Utils.framesToTime(24, 6), null, null));
            die.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_stand), 
                    Utils.framesToTime(24, 2), null, null));
            die.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_die02), 
                    Utils.framesToTime(24, 2), null, null));
            die.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_die03), 
                    Utils.framesToTime(24, 2), null, null));
            die.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_kabocha_evil_die04), 
                    Utils.framesToTime(24, 6), null, null));
            
            staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            staticData.add(idle);
            staticData.add(walk);
            staticData.add(surprised);
            staticData.add(hit);
            staticData.add(die);
            
            setStaticData(GameObjectType.EVIL_KABOCHA, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.NPC);

        BackgroundCollisionComponent bgcollision = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(38, 82);
        bgcollision.setOffset(45, 5);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        NPCAnimationComponent animation = (NPCAnimationComponent)allocateComponent(NPCAnimationComponent.class);
        animation.setSprite(sprite);
        
        ChannelSystem.Channel surpriseChannel = null;
        ChannelSystem channelSystem = BaseObject.sSystemRegistry.channelSystem;
        surpriseChannel = channelSystem.registerChannel(sSurprisedNPCChannel);
        animation.setChannel(surpriseChannel);
        animation.setChannelTrigger(NPCAnimationComponent.SURPRISED);
        
        NPCComponent patrol = (NPCComponent)allocateComponent(NPCComponent.class);
        patrol.setSpeeds(50.0f, 50.0f, 0.0f, -10.0f, 200.0f);
        patrol.setReactToHits(true);
        patrol.setGameEvent(GameFlowEvent.EVENT_SHOW_ANIMATION, AnimationPlayerActivity.ROKUDOU_ENDING, true);

        DynamicCollisionComponent collision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        SoundSystem sound = sSystemRegistry.soundSystem;
        if (sound != null) {
        	hitReact.setTakeHitSound(HitType.HIT, sound.load(R.raw.sound_kabocha_hit));
        }
        
        patrol.setHitReactionComponent(hitReact);
        
        object.team = Team.ENEMY;
        object.life = 3;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
                
        object.add(render);
        object.add(sprite);
        object.add(bgcollision);
        object.add(animation);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        
        addStaticData(GameObjectType.EVIL_KABOCHA, object, sprite);
        
        
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnEnemyRokudou(float positionX, float positionY, boolean flipHorizontal) {
    	TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;

    	// Make sure related textures are loaded.
        textureLibrary.allocateTexture(R.drawable.energy_ball01);
        textureLibrary.allocateTexture(R.drawable.energy_ball02);
        textureLibrary.allocateTexture(R.drawable.energy_ball03);
        textureLibrary.allocateTexture(R.drawable.energy_ball04);
        
        textureLibrary.allocateTexture(R.drawable.effect_bullet01);
        textureLibrary.allocateTexture(R.drawable.effect_bullet02);
        
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mNormalActivationRadius;
        object.width = 128;
        object.height = 128;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.ROKUDOU);
        if (staticData == null) {
            final int staticObjectCount = 8;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            SimplePhysicsComponent physics = (SimplePhysicsComponent)allocateComponent(SimplePhysicsComponent.class);
            physics.setBounciness(0.0f);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new AABoxCollisionVolume(45, 23, 42, 75, HitType.HIT));
                    
            SpriteAnimation idle = new SpriteAnimation(NPCAnimationComponent.IDLE, 1);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_stand), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            
            SpriteAnimation fly = new SpriteAnimation(NPCAnimationComponent.WALK, 2);
            fly.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_fly01), 
                    1.0f, null, basicVulnerabilityVolume));
            fly.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_fly02), 
                    1.0f, null, basicVulnerabilityVolume));
            fly.setLoop(true);
            
            SpriteAnimation shoot = new SpriteAnimation(NPCAnimationComponent.SHOOT, 2);
            shoot.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_shoot01), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            shoot.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_shoot02), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            shoot.setLoop(true);
            
            
            SpriteAnimation surprised = new SpriteAnimation(NPCAnimationComponent.SURPRISED, 1);
            surprised.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_surprise), 
                    4.0f, null, null));
            
            
            SpriteAnimation hit = new SpriteAnimation(NPCAnimationComponent.TAKE_HIT, 7);
            AnimationFrame hitFrame1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_hit01), 
                    Utils.framesToTime(24, 2), null, null);
            AnimationFrame hitFrame2 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_hit02), 
                    Utils.framesToTime(24, 1), null, null);
            AnimationFrame hitFrame3 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_hit03), 
                    Utils.framesToTime(24, 1), null, null);
            
            hit.addFrame(hitFrame1);
            hit.addFrame(hitFrame2);
            hit.addFrame(hitFrame3);
            hit.addFrame(hitFrame2);
            hit.addFrame(hitFrame3);
            hit.addFrame(hitFrame2);
            hit.addFrame(hitFrame3);
            
            SpriteAnimation die = new SpriteAnimation(NPCAnimationComponent.DEATH, 5);
            die.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_stand), 
                    Utils.framesToTime(24, 6), null, null));
            die.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_die01), 
                    Utils.framesToTime(24, 2), null, null));
            die.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_die02), 
                    Utils.framesToTime(24, 4), null, null));
            die.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_die03), 
                    Utils.framesToTime(24, 6), null, null));
            die.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_rokudou_fight_die04), 
                    Utils.framesToTime(24, 6), null, null));
            
            staticData.add(movement);
            staticData.add(physics);
            staticData.add(idle);
            staticData.add(fly);
            staticData.add(surprised);
            staticData.add(hit);
            staticData.add(die);
            staticData.add(shoot);
            
            setStaticData(GameObjectType.ROKUDOU, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.NPC);

        BackgroundCollisionComponent bgcollision = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(45, 75);
        bgcollision.setOffset(45, 23);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
       
        
        NPCAnimationComponent animation = (NPCAnimationComponent)allocateComponent(NPCAnimationComponent.class);
        animation.setSprite(sprite);
        animation.setFlying(true);
        
        ChannelSystem.Channel surpriseChannel = null;
        ChannelSystem channelSystem = BaseObject.sSystemRegistry.channelSystem;
        surpriseChannel = channelSystem.registerChannel(sSurprisedNPCChannel);
        animation.setChannel(surpriseChannel);
        animation.setChannelTrigger(NPCAnimationComponent.SURPRISED);
        
        NPCComponent patrol = (NPCComponent)allocateComponent(NPCComponent.class);
        patrol.setSpeeds(500.0f, 100.0f, 100.0f, -100.0f, 400.0f);
        patrol.setFlying(true);
        patrol.setReactToHits(true);
        patrol.setGameEvent(GameFlowEvent.EVENT_SHOW_ANIMATION, AnimationPlayerActivity.KABOCHA_ENDING, true);
        patrol.setPauseOnAttack(false);
        
        DynamicCollisionComponent collision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        SoundSystem sound = sSystemRegistry.soundSystem;
        if (sound != null) {
        	hitReact.setTakeHitSound(HitType.HIT, sound.load(R.raw.sound_rokudou_hit));
        }
        
        patrol.setHitReactionComponent(hitReact);
        
        ChangeComponentsComponent deathSwap = (ChangeComponentsComponent)allocateComponent(ChangeComponentsComponent.class);
        deathSwap.addSwapInComponent(allocateComponent(GravityComponent.class));
        deathSwap.setSwapAction(ActionType.DEATH);
                
        LaunchProjectileComponent gun 
	        = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
	    gun.setShotsPerSet(1);
	    gun.setSetsPerActivation(-1); 
	    gun.setDelayBetweenSets(1.5f);
	    gun.setObjectTypeToSpawn(GameObjectType.ENERGY_BALL);
	    gun.setOffsetX(75);
	    gun.setOffsetY(42);
	    gun.setRequiredAction(GameObject.ActionType.ATTACK);
	    gun.setVelocityX(300.0f);
	    gun.setVelocityY(-300.0f);
        gun.setShootSound(sound.load(R.raw.sound_poing));

	    
	    LaunchProjectileComponent gun2
        = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
	    gun2.setShotsPerSet(5);
	    gun2.setDelayBetweenShots(0.1f);
	    gun2.setSetsPerActivation(-1); 
	    gun2.setDelayBetweenSets(2.5f);
	    gun2.setObjectTypeToSpawn(GameObjectType.TURRET_BULLET);
	    gun2.setOffsetX(75);
	    gun2.setOffsetY(42);
	    gun2.setRequiredAction(GameObject.ActionType.ATTACK);
	    gun2.setVelocityX(300.0f);
	    gun2.setVelocityY(-300.0f);
        gun.setShootSound(sound.load(R.raw.sound_gun));

        
        object.team = Team.ENEMY;  
        object.life = 3;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
        
        // HACK! Since there's no gravity and this is a big character, align him to the floor
        // manually.
        object.getPosition().y -= 23;
                
        object.add(render);
        object.add(sprite);
        object.add(bgcollision);
        object.add(animation);
        object.add(patrol);
        object.add(collision);
        object.add(hitReact);
        object.add(deathSwap);
        object.add(gun);
        object.add(gun2);

        addStaticData(GameObjectType.ROKUDOU, object, sprite);
        
        
        sprite.playAnimation(0);

        return object;
    }
    
    
    public GameObject spawnPlayerGhost(float positionX, float positionY, GameObject player, float lifeTime) {  
        TextureLibrary textureLibrary = sSystemRegistry.longTermTextureLibrary;
        
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mAlwaysActive;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.GHOST);
        if (staticData == null) {
            final int staticObjectCount = 4;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            //GravityComponent gravity = (GravityComponent)allocateComponent(GravityComponent.class);
            //gravity.setGravityMultiplier(0.1f);
            
            GameComponent movement = allocateComponent(MovementComponent.class);
            SimplePhysicsComponent physics = (SimplePhysicsComponent)allocateComponent(SimplePhysicsComponent.class);
            physics.setBounciness(0.6f);

            GhostComponent ghost = (GhostComponent)allocateComponent(GhostComponent.class);
            ghost.setMovementSpeed(2000.0f);
            ghost.setAcceleration(700.0f);	//300
            ghost.setUseOrientationSensor(true);
            ghost.setKillOnRelease(true);
            
            SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
            if (sound != null) {
            	ghost.setAmbientSound(sound.load(R.raw.sound_possession));
            }
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(32, 32, 32, HitType.POSSESS));

            SpriteAnimation idle = new SpriteAnimation(0, 4);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_energyball01), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_energyball02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_energyball03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_energyball04), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.setLoop(true);
            
            //staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            staticData.add(ghost);
            staticData.add(idle);
            
            setStaticData(GameObjectType.GHOST, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.PROJECTILE);

        BackgroundCollisionComponent bgcollision = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
		bgcollision.setSize(64, 64);
		bgcollision.setOffset(0, 0);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
 
        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setDieOnAttack(true);
        
        dynamicCollision.setHitReactionComponent(hitReact);
        LifetimeComponent life = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        // when the ghost dies it either releases itself or passes control to another object, so we
        // don't want control to return to the player.
        
        life.setReleaseGhostOnDeath(false); 
        
        
        object.life = 1;

        object.add(bgcollision);
        object.add(render);
        object.add(sprite);  
        object.add(dynamicCollision);  
        object.add(hitReact); 
        object.add(life);
        
        addStaticData(GameObjectType.GHOST, object, sprite);
        
        object.commitUpdates();
        
        GhostComponent ghost = object.findByClass(GhostComponent.class);
        if (ghost != null) {
            ghost.setLifeTime(lifeTime);
        }
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnEnergyBall(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.ENERGY_BALL);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(16, 16, 16, HitType.HIT));

            SpriteAnimation idle = new SpriteAnimation(0, 4);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball01), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball04), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.setLoop(true);
            
            staticData.add(movement);
            staticData.add(idle);
            
            setStaticData(GameObjectType.ENERGY_BALL, staticData);

        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.PROJECTILE);

        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setTimeUntilDeath(5.0f);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setDieOnAttack(true);
        
        dynamicCollision.setHitReactionComponent(hitReact);

        object.life = 1;
        object.team = Team.ENEMY;
        object.destroyOnDeactivation = true;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
        
        object.add(lifetime);
        object.add(render);
        object.add(sprite);
        object.add(dynamicCollision);
        object.add(hitReact);
        
        addStaticData(GameObjectType.ENERGY_BALL, object, sprite);
        
        sprite.playAnimation(0);
        
        return object;
    }
    
    public GameObject spawnWandaShot(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.WANDA_SHOT);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(16, 16, 16, HitType.HIT));

            SpriteAnimation idle = new SpriteAnimation(0, 4);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball01), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.energy_ball04), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.setLoop(true);
            
            staticData.add(movement);
            staticData.add(idle);
            
            setStaticData(GameObjectType.WANDA_SHOT, staticData);

        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.PROJECTILE);

        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setTimeUntilDeath(5.0f);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        //hitReact.setDieOnAttack(true);
        
        dynamicCollision.setHitReactionComponent(hitReact);

        object.life = 1;
        object.team = Team.NONE;
        object.destroyOnDeactivation = true;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
        
        object.add(lifetime);
        object.add(render);
        object.add(sprite);
        object.add(dynamicCollision);
        object.add(hitReact);
        
        addStaticData(GameObjectType.WANDA_SHOT, object, sprite);
        
        sprite.playAnimation(0);
        
        return object;
    }
    
    public GameObject spawnCannonBall(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.CANNON_BALL);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(8, 16, 16, HitType.HIT));

            SpriteAnimation idle = new SpriteAnimation(0, 1);
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.snail_bomb), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));

            staticData.add(movement);
            staticData.add(idle);
            
            setStaticData(GameObjectType.CANNON_BALL, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.PROJECTILE);

        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setTimeUntilDeath(3.0f);
        lifetime.setDieOnHitBackground(true);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
 
        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setDieOnAttack(true);
        
        dynamicCollision.setHitReactionComponent(hitReact);

        SimpleCollisionComponent collision = (SimpleCollisionComponent)allocateComponent(SimpleCollisionComponent.class);
        
        
        object.life = 1;
        object.team = Team.ENEMY;
        object.destroyOnDeactivation = true;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
        
        object.add(lifetime);
        object.add(render);
        object.add(sprite);
        object.add(dynamicCollision);
        object.add(hitReact);
        object.add(collision);

        addStaticData(GameObjectType.CANNON_BALL, object, sprite);
        
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnTurretBullet(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 16;
        object.height = 16;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.TURRET_BULLET);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(8, 8, 8, HitType.HIT));

            SpriteAnimation idle = new SpriteAnimation(0, 2);
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.effect_bullet01), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.effect_bullet02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.setLoop(true);

            staticData.add(movement);
            staticData.add(idle);
            
            setStaticData(GameObjectType.TURRET_BULLET, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.PROJECTILE);

        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setTimeUntilDeath(3.0f);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
 
        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setDieOnAttack(true);
        
        dynamicCollision.setHitReactionComponent(hitReact);

        
        object.life = 1;
        object.team = Team.ENEMY;
        object.destroyOnDeactivation = true;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
        
        object.add(lifetime);
        object.add(render);
        object.add(sprite);
        object.add(dynamicCollision);
        object.add(hitReact);

        addStaticData(GameObjectType.TURRET_BULLET, object, sprite);
        
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnBrobotBullet(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.BROBOT_BULLET);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            SpriteAnimation idle = new SpriteAnimation(0, 3);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk01), 
                    Utils.framesToTime(24, 1), null, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk02), 
                    Utils.framesToTime(24, 1), null, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk03), 
                    Utils.framesToTime(24, 1), null, null));
            idle.setLoop(true);

            staticData.add(movement);
            staticData.add(idle);
            
            setStaticData(GameObjectType.BROBOT_BULLET, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.PROJECTILE);

        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setTimeUntilDeath(3.0f);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
             
        object.life = 1;
        object.team = Team.ENEMY;
        object.destroyOnDeactivation = true;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }
        
        object.add(lifetime);
        object.add(render);
        object.add(sprite);
       

        addStaticData(GameObjectType.BROBOT_BULLET, object, sprite);
        
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnCoin(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 16;
        object.height = 16;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.COIN);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = null; /*new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(8, 8, 8));
            basicVulnerabilityVolume.get(0).setHitType(HitType.COLLECT);*/
            
            SpriteAnimation idle = new SpriteAnimation(0, 5);
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_coin01), 
                    Utils.framesToTime(24, 30), null, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_coin02), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_coin03), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_coin04), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_coin05), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            idle.setLoop(true);
            
            InventoryComponent.UpdateRecord addCoin = new InventoryComponent.UpdateRecord();
            addCoin.coinCount = 1;
            
            staticData.add(addCoin);
            staticData.add(idle);
            
            setStaticData(GameObjectType.COIN, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_OBJECT);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);

        //DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        //sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setDieWhenCollected(true);
        hitReact.setInvincible(true);
        
        HitPlayerComponent hitPlayer = (HitPlayerComponent)allocateComponent(HitPlayerComponent.class);
        hitPlayer.setup(32, hitReact, HitType.COLLECT, false);
        
        SoundSystem sound = sSystemRegistry.soundSystem;
        if (sound != null) {
            hitReact.setTakeHitSound(HitType.COLLECT, sound.load(R.raw.ding));
        }
        
        // TODO: this is pretty dumb.  The static data binding needs to be made generic.
        final int staticDataSize = staticData.getCount();
        for (int x = 0; x < staticDataSize; x++) {
            final BaseObject entry = staticData.get(x);
            if (entry instanceof InventoryComponent.UpdateRecord) {
                hitReact.setInventoryUpdate((InventoryComponent.UpdateRecord)entry);
                break;
            }
        }
        
        //dynamicCollision.setHitReactionComponent(hitReact);

        LifetimeComponent life = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        life.setIncrementEventCounter(EventRecorder.COUNTER_PEARLS_COLLECTED);
        
        object.life = 1;
            
        object.add(render);
        object.add(sprite);
        //object.add(dynamicCollision);
        object.add(hitPlayer);
        object.add(hitReact);
        object.add(life);
        
        addStaticData(GameObjectType.COIN, object, sprite);
        sprite.playAnimation(0);
        
        EventRecorder recorder = sSystemRegistry.eventRecorder;
        recorder.incrementEventCounter(EventRecorder.COUNTER_PEARLS_TOTAL);

        return object;
    }
    
    public GameObject spawnRuby(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.RUBY);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(16, 16, 16));
            basicVulnerabilityVolume.get(0).setHitType(HitType.COLLECT);
            
            SpriteAnimation idle = new SpriteAnimation(0, 5);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_ruby01), 
                    2.0f, null, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_ruby02), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_ruby03), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_ruby04), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_ruby05), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            idle.setLoop(true);
            
            InventoryComponent.UpdateRecord addRuby = new InventoryComponent.UpdateRecord();
            addRuby.rubyCount = 1;
            
            staticData.add(addRuby);
            
            staticData.add(idle);
            setStaticData(GameObjectType.RUBY, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_OBJECT);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);

        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setDieWhenCollected(true);
        hitReact.setInvincible(true);
        // TODO: this is pretty dumb.  The static data binding needs to be made generic.
        final int staticDataSize = staticData.getCount();
        for (int x = 0; x < staticDataSize; x++) {
            final BaseObject entry = staticData.get(x);
            if (entry instanceof InventoryComponent.UpdateRecord) {
                hitReact.setInventoryUpdate((InventoryComponent.UpdateRecord)entry);
                break;
            }
        }
        
        dynamicCollision.setHitReactionComponent(hitReact);

        LifetimeComponent life = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        life.setObjectToSpawnOnDeath(GameObjectType.GEM_EFFECT_SPAWNER);
        
        object.life = 1;
            
        object.add(render);
        object.add(sprite);
        object.add(dynamicCollision);
        object.add(hitReact);
        object.add(life);
        
        addStaticData(GameObjectType.RUBY, object, sprite);
        
        sprite.playAnimation(0);
        
        return object;
    }
    
    public GameObject spawnDiary(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        
        LevelSystem level = sSystemRegistry.levelSystem;
        if (level != null) {
        	final LevelTree.Level currentLevel = level.getCurrentLevel();
        	if (currentLevel != null && currentLevel.diaryCollected) {
        		return null;
        	}
        }
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.DIARY);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(16, 16, 16));
            basicVulnerabilityVolume.get(0).setHitType(HitType.COLLECT);
            
            SpriteAnimation idle = new SpriteAnimation(0, 8);
            AnimationFrame frame1 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary01), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            AnimationFrame frame2 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary02), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume);
            
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary01), 
                    1.0f, null, basicVulnerabilityVolume));
            idle.addFrame(frame2);
            idle.addFrame(frame1);
            idle.addFrame(frame2);
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary03), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary04), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary05), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_diary06), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            
            idle.setLoop(true);
            
            InventoryComponent.UpdateRecord addDiary = new InventoryComponent.UpdateRecord();
            addDiary.diaryCount = 1;
            
            staticData.add(addDiary);
            
            staticData.add(idle);
            
            setStaticData(GameObjectType.DIARY, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_OBJECT);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);

        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setDieWhenCollected(true);
        hitReact.setInvincible(true);
        hitReact.setSpawnGameEventOnHit(CollisionParameters.HitType.COLLECT, 
                GameFlowEvent.EVENT_SHOW_DIARY, 0);
        // TODO: this is pretty dumb.  The static data binding needs to be made generic.
        final int staticDataSize = staticData.getCount();
        for (int x = 0; x < staticDataSize; x++) {
            final BaseObject entry = staticData.get(x);
            if (entry instanceof InventoryComponent.UpdateRecord) {
                hitReact.setInventoryUpdate((InventoryComponent.UpdateRecord)entry);
                break;
            }
        }
        
        dynamicCollision.setHitReactionComponent(hitReact);

        LifetimeComponent life = (LifetimeComponent)allocateComponent(LifetimeComponent.class);

        object.life = 1;
            
        object.add(render);
        object.add(sprite);
        object.add(dynamicCollision);
        object.add(hitReact);
        object.add(life);
        
        addStaticData(GameObjectType.DIARY, object, sprite);
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnObjectDoor(float positionX, float positionY, GameObjectType type, boolean solid) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 32;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(type);
        if (staticData == null) {
            final int staticObjectCount = 5;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            final int red_frames[] = { 
                    R.drawable.object_door_red01, 
                    R.drawable.object_door_red02, 
                    R.drawable.object_door_red03, 
                    R.drawable.object_door_red04, 
            };
            
            final int blue_frames[] = { 
                    R.drawable.object_door_blue01, 
                    R.drawable.object_door_blue02, 
                    R.drawable.object_door_blue03, 
                    R.drawable.object_door_blue04, 
            };
            
            final int green_frames[] = { 
                    R.drawable.object_door_green01, 
                    R.drawable.object_door_green02, 
                    R.drawable.object_door_green03, 
                    R.drawable.object_door_green04, 
            };
            
            int frames[] = red_frames;
            
            if (type == GameObjectType.DOOR_GREEN) {
                frames = green_frames;
            } else if (type == GameObjectType.DOOR_BLUE) {
                frames = blue_frames;
            }
            
            FixedSizeArray<CollisionVolume> vulnerabilityVolume = null;
             
            AnimationFrame frame1 = new AnimationFrame(textureLibrary.allocateTexture(frames[0]), 
                    Utils.framesToTime(24, 1), null, vulnerabilityVolume);
            AnimationFrame frame2 = new AnimationFrame(textureLibrary.allocateTexture(frames[1]), 
                    Utils.framesToTime(24, 2));
            AnimationFrame frame3 = new AnimationFrame(textureLibrary.allocateTexture(frames[2]), 
                    Utils.framesToTime(24, 2));
            AnimationFrame frame4 = new AnimationFrame(textureLibrary.allocateTexture(frames[3]), 
                    Utils.framesToTime(24, 1));
            
            // one frame of closing is deadly
            
            FixedSizeArray<CollisionVolume> attackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            attackVolume.add(new AABoxCollisionVolume(12, 8, 8, 56));
            attackVolume.get(0).setHitType(HitType.DEATH);
            
            
            
            AnimationFrame closeFrame2 = new AnimationFrame(textureLibrary.allocateTexture(frames[1]), 
                    Utils.framesToTime(24, 2), attackVolume, vulnerabilityVolume);
            
            SpriteAnimation idle_closed = new SpriteAnimation(DoorAnimationComponent.Animation.CLOSED, 1);
            idle_closed.addFrame(frame1);
                      
            SpriteAnimation idle_open = new SpriteAnimation(DoorAnimationComponent.Animation.OPEN, 1);
            idle_open.addFrame(frame4);
            
            SpriteAnimation open = new SpriteAnimation(DoorAnimationComponent.Animation.OPENING, 2);
            open.addFrame(frame2);
            open.addFrame(frame3);
            
            SpriteAnimation close = new SpriteAnimation(DoorAnimationComponent.Animation.CLOSING, 2);
            close.addFrame(frame3);
            close.addFrame(closeFrame2);
            
            SolidSurfaceComponent solidSurface 
                = (SolidSurfaceComponent)allocateComponent(SolidSurfaceComponent.class);
            solidSurface.inititalize(4);
            // box shape:
            // ___       ___1
            // | |      2| |3
            // ---       ---4
            Vector2 surface1Start = new Vector2(0, object.height);
            Vector2 surface1End = new Vector2(object.width, object.height);
            Vector2 surface1Normal = new Vector2(0.0f, -1.0f);
            surface1Normal.normalize();
            
            Vector2 surface2Start = new Vector2(0, object.height);
            Vector2 surface2End = new Vector2(0, 0);
            Vector2 surface2Normal = new Vector2(-1.0f, 0.0f);
            surface2Normal.normalize();
            
            Vector2 surface3Start = new Vector2(object.width, object.height);
            Vector2 surface3End = new Vector2(object.width, 0);
            Vector2 surface3Normal = new Vector2(1.0f, 0);
            
            Vector2 surface4Start = new Vector2(0, 0);
            Vector2 surface4End = new Vector2(object.width, 0);
            Vector2 surface4Normal = new Vector2(0, 1.0f);
            
            solidSurface.addSurface(surface1Start, surface1End, surface1Normal);
            solidSurface.addSurface(surface2Start, surface2End, surface2Normal);
            solidSurface.addSurface(surface3Start, surface3End, surface3Normal);
            solidSurface.addSurface(surface4Start, surface4End, surface4Normal);
                
            staticData.add(idle_open);
            staticData.add(idle_closed);
            staticData.add(open);
            staticData.add(close);
            staticData.add(solidSurface);
            setStaticData(type, staticData);
        }
        
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.FOREGROUND_OBJECT);
       
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        DoorAnimationComponent doorAnim = (DoorAnimationComponent)allocateComponent(DoorAnimationComponent.class);
        doorAnim.setSprite(sprite);
        
        SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        if (sound != null) {
        	doorAnim.setSounds(sound.load(R.raw.sound_open), sound.load(R.raw.sound_close));
        }
        
        ChannelSystem.Channel doorChannel = null;
        ChannelSystem channelSystem = BaseObject.sSystemRegistry.channelSystem;
        switch (type) {
            case DOOR_RED:
                doorChannel = channelSystem.registerChannel(sRedButtonChannel);
                break;
            case DOOR_BLUE:
                doorChannel = channelSystem.registerChannel(sBlueButtonChannel);
                break;
            case DOOR_GREEN:
                doorChannel = channelSystem.registerChannel(sGreenButtonChannel);
                break;
        }
        doorAnim.setChannel(doorChannel);
             
        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact 
            = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        dynamicCollision.setHitReactionComponent(hitReact);
       
        
         
        object.add(render);
        object.add(sprite);
        object.add(doorAnim);   
        object.add(dynamicCollision);
        object.add(hitReact);
        addStaticData(type, object, sprite);
        
        object.commitUpdates();

        SolidSurfaceComponent solidSurface = object.findByClass(SolidSurfaceComponent.class);
        if (solid) {
            doorAnim.setSolidSurface(solidSurface);
        } else {
            object.remove(solidSurface);
            object.commitUpdates();
        }
        
              
        sprite.playAnimation(0);
        
        return object;
    }
    
    public GameObject spawnObjectButton(float positionX, float positionY, GameObjectType type) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(type);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            final int red_frames[] = { 
                    R.drawable.object_button_red, 
                    R.drawable.object_button_pressed_red, 
            };
            
            final int blue_frames[] = { 
                    R.drawable.object_button_blue, 
                    R.drawable.object_button_pressed_blue, 
            };
            
            final int green_frames[] = { 
                    R.drawable.object_button_green, 
                    R.drawable.object_button_pressed_green, 
            };
            
            int frames[] = red_frames;
            
            if (type == GameObjectType.BUTTON_GREEN) {
                frames = green_frames;
            } else if (type == GameObjectType.BUTTON_BLUE) {
                frames = blue_frames;
            }
            
            FixedSizeArray<CollisionVolume> vulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            vulnerabilityVolume.add(new AABoxCollisionVolume(0, 0, 32, 16));
            vulnerabilityVolume.get(0).setHitType(HitType.DEPRESS);
            
            AnimationFrame frame1 = new AnimationFrame(textureLibrary.allocateTexture(frames[0]), 
                    Utils.framesToTime(24, 1), null, vulnerabilityVolume);
            AnimationFrame frame2 = new AnimationFrame(textureLibrary.allocateTexture(frames[1]), 
                    Utils.framesToTime(24, 1), null, vulnerabilityVolume);
           
            SpriteAnimation idle = new SpriteAnimation(ButtonAnimationComponent.Animation.UP, 1);
            idle.addFrame(frame1);

            SpriteAnimation pressed = new SpriteAnimation(ButtonAnimationComponent.Animation.DOWN, 1);
            pressed.addFrame(frame2);

            staticData.add(idle);
            staticData.add(pressed);
            
            setStaticData(type, staticData);
        }
        
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_OBJECT);
       
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        ButtonAnimationComponent button = (ButtonAnimationComponent)allocateComponent(ButtonAnimationComponent.class);
        button.setSprite(sprite);
        
        SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        if (sound != null) {
        	button.setDepressSound(sound.load(R.raw.sound_button));
        }
        
        ChannelSystem.Channel buttonChannel = null;
        ChannelSystem channelSystem = BaseObject.sSystemRegistry.channelSystem;
        switch (type) {
            case BUTTON_RED:
                buttonChannel = channelSystem.registerChannel(sRedButtonChannel);
                break;
            case BUTTON_BLUE:
                buttonChannel = channelSystem.registerChannel(sBlueButtonChannel);
                break;
            case BUTTON_GREEN:
                buttonChannel = channelSystem.registerChannel(sGreenButtonChannel);
                break;
        }
        button.setChannel(buttonChannel);
        
        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setInvincible(false);
        
        
        
        dynamicCollision.setHitReactionComponent(hitReact);
        
        object.team = Team.NONE;
        
        object.add(render);
        object.add(sprite);
        object.add(button);
        object.add(dynamicCollision);
        object.add(hitReact);
        
        addStaticData(type, object, sprite);
        
        sprite.playAnimation(0);
        
        return object;
    }
    
    public GameObject spawnObjectCannon(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 64;
        object.height = 128;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.CANNON);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            FixedSizeArray<CollisionVolume> attackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            attackVolume.add(new AABoxCollisionVolume(16, 16, 32, 80));
            attackVolume.get(0).setHitType(HitType.LAUNCH);
            
            AnimationFrame frame1 = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_cannon), 
                    1.0f, attackVolume, null);
           
            SpriteAnimation idle = new SpriteAnimation(GenericAnimationComponent.Animation.IDLE, 1);
            idle.addFrame(frame1);
            
            AnimationFrame frame1NoAttack = new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_cannon), 
                    1.0f, null, null);
            
            SpriteAnimation shoot = new SpriteAnimation(GenericAnimationComponent.Animation.ATTACK, 1);
            shoot.addFrame(frame1NoAttack);

            staticData.add(idle);
            staticData.add(shoot);
            
            setStaticData(GameObjectType.CANNON, staticData);
        }
        
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.FOREGROUND_OBJECT);
       
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        LauncherComponent launcher = (LauncherComponent)allocateComponent(LauncherComponent.class);
        launcher.setLaunchEffect(GameObjectType.SMOKE_POOF, 32.0f, 85.0f);
        
        SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        if (sound != null) {
        	launcher.setLaunchSound(sound.load(R.raw.sound_cannon));
        }
        
        
        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setInvincible(false);
        hitReact.setLauncherComponent(launcher, HitType.LAUNCH);
        
        dynamicCollision.setHitReactionComponent(hitReact);
        
        GenericAnimationComponent anim = (GenericAnimationComponent)allocateComponent(GenericAnimationComponent.class);
        anim.setSprite(sprite);
        
        object.team = Team.NONE;
        
        object.add(render);
        object.add(sprite);
        object.add(dynamicCollision);
        object.add(hitReact);
        object.add(launcher);
        object.add(anim);
        
        addStaticData(GameObjectType.CANNON, object, sprite);
        
        sprite.playAnimation(0);
        
        return object;
    }
    
    public GameObject spawnObjectBrobotSpawner(float positionX, float positionY, boolean flipHorizontal) {
        
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;

        // This is pretty heavy-handed.
        // TODO: figure out a general solution for objects that depend on other objects.
        textureLibrary.allocateTexture(R.drawable.enemy_brobot_idle01);
        textureLibrary.allocateTexture(R.drawable.enemy_brobot_idle02);
        textureLibrary.allocateTexture(R.drawable.enemy_brobot_idle03);
        textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk01);
        textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk02);
        textureLibrary.allocateTexture(R.drawable.enemy_brobot_walk03);
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.BROBOT_SPAWNER);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
         
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(32, 32, 32));
            basicVulnerabilityVolume.get(0).setHitType(HitType.POSSESS);
            
            SpriteAnimation idle = new SpriteAnimation(0, 1);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_brobot_machine), 
                    1.0f, null, basicVulnerabilityVolume));
            
            SolidSurfaceComponent solidSurface 
                = (SolidSurfaceComponent)allocateComponent(SolidSurfaceComponent.class);
            solidSurface.inititalize(3);
            /*
                0:2,0:8,59:-0.99532399996093,0.09659262446878
                0:8,59:61,33:0.44551558813576,0.89527418187282
                0:61,33:61,-1:1,0

             */
            // trapezoid shape:
            // |\        |\2
            // | |      1| |3
           
            Vector2 surface1Start = new Vector2(0, 0);
            Vector2 surface1End = new Vector2(8.0f, 59.0f);
            Vector2 surface1Normal = new Vector2(-0.9953f, 0.0965f);
            surface1Normal.normalize();
            
            Vector2 surface2Start = new Vector2(8.0f, 59.0f);
            Vector2 surface2End = new Vector2(61.0f, 33.0f);
            Vector2 surface2Normal = new Vector2(0.445515f, 0.89527f);
            surface2Normal.normalize();
            
            Vector2 surface3Start = new Vector2(61.0f, 33.0f);
            Vector2 surface3End = new Vector2(61.0f, 0.0f);
            Vector2 surface3Normal = new Vector2(1.0f, 0.0f);
            
            solidSurface.addSurface(surface1Start, surface1End, surface1Normal);
            solidSurface.addSurface(surface2Start, surface2End, surface2Normal);
            solidSurface.addSurface(surface3Start, surface3End, surface3Normal);
        
            staticData.add(solidSurface);
            staticData.add(idle);
            
            
            setStaticData(GameObjectType.BROBOT_SPAWNER, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_OBJECT);

        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        DynamicCollisionComponent collision 
            = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact 
            = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
       
        LaunchProjectileComponent gun 
            = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
        gun.setDelayBeforeFirstSet(3.0f);
        gun.setObjectTypeToSpawn(GameObjectType.BROBOT);
        gun.setOffsetX(36);
        gun.setOffsetY(50);
        gun.setVelocityX(100.0f);
        gun.setVelocityY(300.0f);
        gun.enableProjectileTracking(1);
        
        
        object.team = Team.ENEMY;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        } else {
            object.facingDirection.x = 1.0f;
        }

        object.add(render);
        object.add(sprite);
        object.add(gun);
        object.add(collision);
        object.add(hitReact);
        
        
        addStaticData(GameObjectType.BROBOT_SPAWNER, object, sprite);
        
        object.commitUpdates();
        
   
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnObjectInfiniteSpawner(float positionX, float positionY) {
    	GameObject object = spawnObjectBrobotSpawner(positionX, positionY, false);
    	object.facingDirection.y = -1; //vertical flip
    	LaunchProjectileComponent gun = object.findByClass(LaunchProjectileComponent.class);
    	if (gun != null) {
    		gun.disableProjectileTracking();
    		gun.setDelayBetweenShots(0.15f);
    		gun.setSetsPerActivation(1);
    		gun.setShotsPerSet(60);
    	}
    	
    	return object;
    }
    
    public GameObject spawnObjectCrusherAndou(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mAlwaysActive;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.CRUSHER_ANDOU);
        
        if (staticData == null) {
            final int staticObjectCount = 5;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            PhysicsComponent physics = (PhysicsComponent)allocateComponent(PhysicsComponent.class);

            physics.setMass(9.1f);   // ~90kg w/ earth gravity
            physics.setDynamicFrictionCoeffecient(0.2f);
            physics.setStaticFrictionCoeffecient(0.01f);
            
            // Animation Data
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(16, 32, 32));

            SpriteAnimation idle = new SpriteAnimation(Animation.IDLE, 1);
            idle.addFrame(new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stand), 
                    1.0f, null, basicVulnerabilityVolume));
            
          
            FixedSizeArray<CollisionVolume> stompAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            stompAttackVolume.add(new AABoxCollisionVolume(16, -5.0f, 32, 37, HitType.HIT));
            
            
            SpriteAnimation stomp = new SpriteAnimation(Animation.ATTACK, 4);
            stomp.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp01), 
                    		Utils.framesToTime(24, 1), stompAttackVolume, null));
            stomp.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp02), 
                    		Utils.framesToTime(24, 1), stompAttackVolume, null));
            stomp.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp03), 
                    		Utils.framesToTime(24, 1), stompAttackVolume, null));
            stomp.addFrame(
                    new AnimationFrame(textureLibrary.allocateTexture(R.drawable.andou_stomp04), 
                    		Utils.framesToTime(24, 1), stompAttackVolume, null));
            
           
            
            // Save static data
            staticData.add(gravity);
            staticData.add(movement);
            staticData.add(physics);
            
            
            staticData.add(idle);
            staticData.add(stomp);
            
            
            setStaticData(GameObjectType.CRUSHER_ANDOU, staticData);
        }
        
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.PLAYER);
        BackgroundCollisionComponent bgcollision 
            = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(32, 48);
        bgcollision.setOffset(16, 0);
        
        GenericAnimationComponent animation = (GenericAnimationComponent)allocateComponent(GenericAnimationComponent.class);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        animation.setSprite(sprite);

        
        DynamicCollisionComponent dynamicCollision 
            = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        hitReact.setBounceOnHit(true);
        hitReact.setPauseOnAttack(true);
        hitReact.setInvincibleTime(3.0f);
        hitReact.setSpawnOnDealHit(HitType.HIT, GameObjectType.CRUSH_FLASH, false, true);
        
      
        dynamicCollision.setHitReactionComponent(hitReact);
        
       

        object.life = 1;
        object.team = Team.PLAYER;
       
        object.add(animation);
        object.add(bgcollision);
        object.add(render);
        object.add(sprite);  
        object.add(dynamicCollision);  
        object.add(hitReact); 
         
        addStaticData(GameObjectType.CRUSHER_ANDOU, object, sprite);
        
        sprite.playAnimation(Animation.IDLE);
        
        object.commitUpdates();
        
        ChangeComponentsComponent swap = (ChangeComponentsComponent)allocateComponent(ChangeComponentsComponent.class);
        
        final int count = object.getCount();
        for (int x = 0; x < count; x++) {
        	swap.addSwapInComponent((GameComponent)object.get(x));
        }
        
        object.removeAll();
        
        CrusherAndouComponent crusher = (CrusherAndouComponent)allocateComponent(CrusherAndouComponent.class);
        
        crusher.setSwap(swap);
        
        object.add(swap);
        object.add(crusher);
        
        object.commitUpdates();
        
        return object;
    }
    
    public GameObject spawnObjectBreakableBlock(float positionX, float positionY) {
        
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;

        // Preload block piece texture.
        textureLibrary.allocateTexture(R.drawable.object_debris_piece);
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.BREAKABLE_BLOCK);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
         
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new AABoxCollisionVolume(7, 0, 32 - 7, 42, HitType.HIT));
            
            SpriteAnimation idle = new SpriteAnimation(0, 1);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_debris_block), 
                    1.0f, null, basicVulnerabilityVolume));
            
            SolidSurfaceComponent solidSurface 
                = (SolidSurfaceComponent)allocateComponent(SolidSurfaceComponent.class);
            solidSurface.inititalize(4);
            
            // box shape:
            // ___       ___2
            // | |      1| |3
            // ---       ---4
           
            Vector2 surface1Start = new Vector2(0.0f, 0.0f);
            Vector2 surface1End = new Vector2(0.0f, 32.0f);
            Vector2 surface1Normal = new Vector2(-1.0f, 0.0f);
            surface1Normal.normalize();
            
            Vector2 surface2Start = new Vector2(0.0f, 32.0f);
            Vector2 surface2End = new Vector2(32.0f, 32.0f);
            Vector2 surface2Normal = new Vector2(0.0f, 1.0f);
            surface2Normal.normalize();
            
            Vector2 surface3Start = new Vector2(32.0f, 32.0f);
            Vector2 surface3End = new Vector2(32.0f, 0.0f);
            Vector2 surface3Normal = new Vector2(1.0f, 0.0f);
            
            Vector2 surface4Start = new Vector2(32.0f, 0.0f);
            Vector2 surface4End = new Vector2(0.0f, 0.0f);
            Vector2 surface4Normal = new Vector2(0.0f, -1.0f);
            
            solidSurface.addSurface(surface1Start, surface1End, surface1Normal);
            solidSurface.addSurface(surface2Start, surface2End, surface2Normal);
            solidSurface.addSurface(surface3Start, surface3End, surface3Normal);
            solidSurface.addSurface(surface4Start, surface4End, surface4Normal);

            staticData.add(solidSurface);
            staticData.add(idle);
            
            
            setStaticData(GameObjectType.BREAKABLE_BLOCK, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_OBJECT);

        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        DynamicCollisionComponent collision 
            = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact 
            = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
       
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setObjectToSpawnOnDeath(GameObjectType.BREAKABLE_BLOCK_PIECE_SPAWNER);
        SoundSystem sound = BaseObject.sSystemRegistry.soundSystem;
        if (sound != null) {
        	lifetime.setDeathSound(sound.load(R.raw.sound_break_block));
        }
        
        object.life = 1;
        object.team = Team.ENEMY;
      
        object.add(render);
        object.add(sprite);
        object.add(collision);
        object.add(hitReact);
        object.add(lifetime);
        
        
        addStaticData(GameObjectType.BREAKABLE_BLOCK, object, sprite);
          
        sprite.playAnimation(0);

        return object;
    }

	public GameObject spawnObjectTheSource(float positionX, float positionY) {
	    
	    final TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
	    
	    GameObject object = mGameObjectPool.allocate();
	    object.activationRadius = mAlwaysActive;
	    object.width = 512;
	    object.height = 512;
        object.getPosition().set(positionX, positionY);
	    
	    RenderComponent layer1Render = (RenderComponent)allocateComponent(RenderComponent.class);
	    layer1Render.setPriority(SortConstants.THE_SOURCE_START);
	    FadeDrawableComponent layer1Fade = (FadeDrawableComponent)allocateComponent(FadeDrawableComponent.class);
	    layer1Fade.setRenderComponent(layer1Render);
	    layer1Fade.setTexture(textureLibrary.allocateTexture(R.drawable.enemy_source_spikes));
	    layer1Fade.setupFade(1.0f, 0.2f, 1.9f, FadeDrawableComponent.LOOP_TYPE_PING_PONG, FadeDrawableComponent.FADE_EASE, 0.0f);

	    RenderComponent layer2Render = (RenderComponent)allocateComponent(RenderComponent.class);
	    layer2Render.setPriority(SortConstants.THE_SOURCE_START + 1);
	    FadeDrawableComponent layer2Fade = (FadeDrawableComponent)allocateComponent(FadeDrawableComponent.class);
	    layer2Fade.setRenderComponent(layer2Render);
	    layer2Fade.setTexture(textureLibrary.allocateTexture(R.drawable.enemy_source_body));
	    layer2Fade.setupFade(1.0f, 0.8f, 5.0f, FadeDrawableComponent.LOOP_TYPE_PING_PONG, FadeDrawableComponent.FADE_EASE, 0.0f);
	    
	    RenderComponent layer3Render = (RenderComponent)allocateComponent(RenderComponent.class);
	    layer3Render.setPriority(SortConstants.THE_SOURCE_START + 2);
	    FadeDrawableComponent layer3Fade = (FadeDrawableComponent)allocateComponent(FadeDrawableComponent.class);
	    layer3Fade.setRenderComponent(layer3Render);
	    layer3Fade.setTexture(textureLibrary.allocateTexture(R.drawable.enemy_source_black));
	    layer3Fade.setupFade(0.0f, 1.0f, 6.0f, FadeDrawableComponent.LOOP_TYPE_PING_PONG, FadeDrawableComponent.FADE_LINEAR, 0.0f);
	  
	    RenderComponent layer4Render = (RenderComponent)allocateComponent(RenderComponent.class);
	    layer4Render.setPriority(SortConstants.THE_SOURCE_START + 3);
	    FadeDrawableComponent layer4Fade = (FadeDrawableComponent)allocateComponent(FadeDrawableComponent.class);
	    layer4Fade.setRenderComponent(layer4Render);
	    layer4Fade.setTexture(textureLibrary.allocateTexture(R.drawable.enemy_source_spots));
	    layer4Fade.setupFade(0.0f, 1.0f, 2.3f, FadeDrawableComponent.LOOP_TYPE_PING_PONG, FadeDrawableComponent.FADE_EASE, 0.0f);
	    
	    RenderComponent layer5Render = (RenderComponent)allocateComponent(RenderComponent.class);
	    layer5Render.setPriority(SortConstants.THE_SOURCE_START + 4);
	    FadeDrawableComponent layer5Fade = (FadeDrawableComponent)allocateComponent(FadeDrawableComponent.class);
	    layer5Fade.setRenderComponent(layer5Render);
	    layer5Fade.setTexture(textureLibrary.allocateTexture(R.drawable.enemy_source_core));
	    layer5Fade.setupFade(0.2f, 1.0f, 1.2f, FadeDrawableComponent.LOOP_TYPE_PING_PONG, FadeDrawableComponent.FADE_EASE, 0.0f);
	    
	    
	    OrbitalMagnetComponent orbit = (OrbitalMagnetComponent)allocateComponent(OrbitalMagnetComponent.class);
	    orbit.setup(320.0f, 220.0f);
	    
	    DynamicCollisionComponent collision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
	    FixedSizeArray<CollisionVolume> vulnerabilityVolume = 
            new FixedSizeArray<CollisionVolume>(1);
	    vulnerabilityVolume.add(new SphereCollisionVolume(256, 256, 256, HitType.HIT));
	    FixedSizeArray<CollisionVolume> attackVolume = 
            new FixedSizeArray<CollisionVolume>(1);
	    attackVolume.add(new SphereCollisionVolume(256, 256, 256, HitType.HIT));
	    collision.setCollisionVolumes(attackVolume, vulnerabilityVolume);
        
	    HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        hitReact.setInvincibleTime(TheSourceComponent.SHAKE_TIME);
        
        TheSourceComponent theSource = (TheSourceComponent)allocateComponent(TheSourceComponent.class);
        ChannelSystem.Channel surpriseChannel = null;
        ChannelSystem channelSystem = BaseObject.sSystemRegistry.channelSystem;
        surpriseChannel = channelSystem.registerChannel(sSurprisedNPCChannel);
        theSource.setChannel(surpriseChannel);
        theSource.setGameEvent(GameFlowEvent.EVENT_SHOW_ANIMATION, AnimationPlayerActivity.WANDA_ENDING);

        
	    object.life = 3;
	    object.team = Team.PLAYER;
	  
	    object.add(layer1Render);
	    object.add(layer2Render);
	    object.add(layer3Render);
	    object.add(layer4Render);
	    object.add(layer5Render);
	    
	    object.add(layer1Fade);
	    object.add(layer2Fade);
	    object.add(layer3Fade);
	    object.add(layer4Fade);
	    object.add(layer5Fade);
	    
	    object.add(orbit);
	    object.add(collision);
	    object.add(hitReact);
	    object.add(theSource);	      
	
	    return object;
	}
	
	public GameObject spawnObjectSign(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.HINT_SIGN);
        if (staticData == null) {
            final int staticObjectCount = 1;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
                  
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new AABoxCollisionVolume(8, 0, 24, 32, HitType.COLLECT));
            
            SpriteAnimation idle = new SpriteAnimation(0, 1);
            AnimationFrame frame1 = new AnimationFrame(textureLibrary.allocateTexture(R.drawable.object_sign), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume);
            
            idle.addFrame(frame1);
           
            idle.setLoop(true);
            
            staticData.add(idle);
            
            setStaticData(GameObjectType.HINT_SIGN, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_OBJECT);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);

        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        HitReactionComponent hitReact = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        dynamicCollision.setHitReactionComponent(hitReact);
        hitReact.setSpawnGameEventOnHit(HitType.COLLECT, GameFlowEvent.EVENT_SHOW_DIALOG_CHARACTER2, 0);
        
        SelectDialogComponent dialogSelect = (SelectDialogComponent)allocateComponent(SelectDialogComponent.class);
        dialogSelect.setHitReact(hitReact);
     
        object.add(dialogSelect);
        object.add(render);
        object.add(sprite);
        object.add(dynamicCollision);
        object.add(hitReact);
        
        addStaticData(GameObjectType.HINT_SIGN, object, sprite);
        sprite.playAnimation(0);

        return object;
    }

    public GameObject spawnObjectTurret(float positionX, float positionY, boolean flipHorizontal) {
        
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;

        // Make sure related textures are loaded.
        textureLibrary.allocateTexture(R.drawable.effect_bullet01);
        textureLibrary.allocateTexture(R.drawable.effect_bullet02);

        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.TURRET);
        if (staticData == null) {
            final int staticObjectCount = 3;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
         
            // Animations
            FixedSizeArray<CollisionVolume> basicVulnerabilityVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicVulnerabilityVolume.add(new SphereCollisionVolume(32, 32, 32));
            basicVulnerabilityVolume.get(0).setHitType(HitType.POSSESS);
             
            SpriteAnimation idle = new SpriteAnimation(EnemyAnimations.IDLE.ordinal(), 2);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_gunturret01), 
                    1.0f, null, basicVulnerabilityVolume));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_gunturret_idle), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            idle.setLoop(true);
            
            SpriteAnimation attack = new SpriteAnimation(EnemyAnimations.ATTACK.ordinal(), 4);
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_gunturret02), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_gunturret01), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_gunturret03), 
                    Utils.framesToTime(24, 2), null, basicVulnerabilityVolume));
            attack.addFrame(new AnimationFrame(
                    textureLibrary.allocateTexture(R.drawable.object_gunturret01), 
                    Utils.framesToTime(24, 1), null, basicVulnerabilityVolume));
            attack.setLoop(true);
            
            GhostComponent ghost = (GhostComponent)allocateComponent(GhostComponent.class);
            ghost.setTargetAction(ActionType.IDLE);
            ghost.changeActionOnButton(ActionType.ATTACK);

            staticData.add(idle);
            staticData.add(attack);
            staticData.add(ghost);
            
            
            setStaticData(GameObjectType.TURRET, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.GENERAL_OBJECT);

        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
        sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
  
        GenericAnimationComponent animation
            = (GenericAnimationComponent)allocateComponent(GenericAnimationComponent.class);
        animation.setSprite(sprite);
        
        AttackAtDistanceComponent attack = (AttackAtDistanceComponent)
            allocateComponent(AttackAtDistanceComponent.class);
        attack.setupAttack(300, 0.0f, 1.0f, true);

        
        DynamicCollisionComponent collision 
            = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(collision);
        
        HitReactionComponent hitReact 
            = (HitReactionComponent)allocateComponent(HitReactionComponent.class);
        collision.setHitReactionComponent(hitReact);
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setObjectToSpawnOnDeath(GameObjectType.EXPLOSION_LARGE);

        SoundSystem sound = sSystemRegistry.soundSystem;
        
        LaunchProjectileComponent gun 
            = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
        gun.setShotsPerSet(1);
        gun.setDelayBetweenShots(0.0f);
        gun.setDelayBetweenSets(0.3f);
        gun.setObjectTypeToSpawn(GameObjectType.TURRET_BULLET);
        gun.setOffsetX(54);
        gun.setOffsetY(13);
        gun.setRequiredAction(GameObject.ActionType.ATTACK);
        gun.setVelocityX(300.0f);
        gun.setVelocityY(-300.0f);
        gun.setShootSound(sound.load(R.raw.sound_gun));
        
        // Components for possession
        
        ChangeComponentsComponent componentSwap = (ChangeComponentsComponent)allocateComponent(ChangeComponentsComponent.class);
        componentSwap.addSwapOutComponent(attack);
        componentSwap.setPingPongBehavior(true);
        
        hitReact.setPossessionComponent(componentSwap);
        
        object.team = Team.ENEMY;
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        } else {
            object.facingDirection.x = 1.0f;
        }

        object.add(render);
        object.add(sprite);
        object.add(animation);
        object.add(attack);
        object.add(collision);
        object.add(hitReact);
        object.add(lifetime);
        object.add(gun);
        object.add(componentSwap);
        
        addStaticData(GameObjectType.TURRET, object, sprite);
        
        object.commitUpdates();
        
        GhostComponent possessedGhost = object.findByClass(GhostComponent.class);
        if (possessedGhost != null) {
            object.remove(possessedGhost);   // Not supposed to be added yet.
            componentSwap.addSwapInComponent(possessedGhost);
        }
        
        sprite.playAnimation(0);

        return object;
    }

    public GameObject spawnDust(float positionX, float positionY, boolean flipHorizontal) {
        TextureLibrary textureLibrary = sSystemRegistry.longTermTextureLibrary;

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.DUST);
        if (staticData == null) {
            final int staticObjectCount = 1;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            SpriteAnimation idle = new SpriteAnimation(0, 5);
            idle.addFrame(new AnimationFrame(textureLibrary.getTextureByResource(R.drawable.dust01), 
                    Utils.framesToTime(24, 1)));
            idle.addFrame(new AnimationFrame(textureLibrary.getTextureByResource(R.drawable.dust02), 
                    Utils.framesToTime(24, 1)));
            idle.addFrame(new AnimationFrame(textureLibrary.getTextureByResource(R.drawable.dust03), 
                    Utils.framesToTime(24, 1)));
            idle.addFrame(new AnimationFrame(textureLibrary.getTextureByResource(R.drawable.dust04), 
                    Utils.framesToTime(24, 1)));
            idle.addFrame(new AnimationFrame(textureLibrary.getTextureByResource(R.drawable.dust05), 
                    Utils.framesToTime(24, 1)));
            
            staticData.add(idle);
            setStaticData(GameObjectType.DUST, staticData);
        }
        
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.EFFECT);

        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setTimeUntilDeath(0.30f);
    
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        
        
        if (flipHorizontal) {
            object.facingDirection.x = -1.0f;
        }        
        object.destroyOnDeactivation = true;
        
        object.add(lifetime);
        object.add(render);
        object.add(sprite);
           
        addStaticData(GameObjectType.DUST, object, sprite);
        
        sprite.playAnimation(0);
        
        return object;
    }
    
    public GameObject spawnEffectExplosionSmall(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.longTermTextureLibrary;

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mAlwaysActive;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.EXPLOSION_SMALL);
        if (staticData == null) {
            final int staticObjectCount = 1;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(16, 16, 16, HitType.HIT));

            SpriteAnimation idle = new SpriteAnimation(0, 7);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small01), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small04), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small05), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small06), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small07), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            
            staticData.add(idle);
            setStaticData(GameObjectType.EXPLOSION_SMALL, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.EFFECT);

        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
     

        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        
        DynamicCollisionComponent dynamicCollision = (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
                      
        object.add(dynamicCollision);
        object.add(lifetime);
        object.add(render);
        object.add(sprite);
          
        addStaticData(GameObjectType.EXPLOSION_SMALL, object, sprite);
        
        final SpriteAnimation idle = sprite.findAnimation(0);
        if (idle != null) {
            lifetime.setTimeUntilDeath(idle.getLength());
        }
        
        sprite.playAnimation(0);

        return object;
    }
    
    public GameObject spawnEffectExplosionLarge(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.longTermTextureLibrary;

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mAlwaysActive;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.EXPLOSION_LARGE);
        if (staticData == null) {
            final int staticObjectCount = 1;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = 
                new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(32, 32, 32, HitType.HIT));

            SpriteAnimation idle = new SpriteAnimation(0, 9);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big01), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big04), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big05), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big06), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big07), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big08), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big09), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            
            
            staticData.add(idle);            
            setStaticData(GameObjectType.EXPLOSION_LARGE, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.EFFECT);
        
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);
        

        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
         
        DynamicCollisionComponent dynamicCollision = 
            (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        PlaySingleSoundComponent soundEffect = (PlaySingleSoundComponent)allocateComponent(PlaySingleSoundComponent.class);
        soundEffect.setSound(sSystemRegistry.soundSystem.load(R.raw.quick_explosion));
        
        
        object.add(soundEffect);            
        object.add(dynamicCollision);
        object.add(lifetime);
        object.add(render);
        object.add(sprite);
        
        addStaticData(GameObjectType.EXPLOSION_LARGE, object, sprite);
        
        final SpriteAnimation idle = sprite.findAnimation(0);
        if (idle != null) {
            lifetime.setTimeUntilDeath(idle.getLength());
        }
        
        sprite.playAnimation(0);
        
        return object;
    }
    
    public GameObject spawnEffectExplosionGiant(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.longTermTextureLibrary;

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mAlwaysActive;
        object.width = 64;
        object.height = 64;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.EXPLOSION_GIANT);
        if (staticData == null) {
            final int staticObjectCount = 4;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            FixedSizeArray<CollisionVolume> basicAttackVolume = new FixedSizeArray<CollisionVolume>(1);
            basicAttackVolume.add(new SphereCollisionVolume(64, 32, 32, HitType.HIT));

            SpriteAnimation idle = new SpriteAnimation(0, 9);
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big01), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big02), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big03), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big04), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big05), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big06), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big07), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big08), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));
            idle.addFrame(new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_big09), 
                    Utils.framesToTime(24, 1), basicAttackVolume, null));

            
            AnimationFrame smallFrame1 = new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small01), 
                    Utils.framesToTime(24, 1), null, null);
            AnimationFrame smallFrame2 = new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small02), 
                    Utils.framesToTime(24, 1), null, null);
            AnimationFrame smallFrame3 = new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small03), 
                    Utils.framesToTime(24, 1), null, null);
            AnimationFrame smallFrame4 = new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small04), 
                    Utils.framesToTime(24, 1), null, null);
            AnimationFrame smallFrame5 = new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small05), 
                    Utils.framesToTime(24, 1), null, null);
            AnimationFrame smallFrame6 = new AnimationFrame(
                    textureLibrary.getTextureByResource(R.drawable.effect_explosion_small06), 
                    Utils.framesToTime(24, 1), null, null);
            AnimationFrame smallFrame7 = new AnimationFrame
            (textureLibrary.getTextureByResource(R.drawable.effect_explosion_small07), 
                    Utils.framesToTime(24, 1), null, null);

            SpriteAnimation smallBlast1 = new SpriteAnimation(0, 7);
            smallBlast1.addFrame(smallFrame1);
            smallBlast1.addFrame(smallFrame2);
            smallBlast1.addFrame(smallFrame3);
            smallBlast1.addFrame(smallFrame4);
            smallBlast1.addFrame(smallFrame5);
            smallBlast1.addFrame(smallFrame6);
            smallBlast1.addFrame(smallFrame7);
            
            SpriteAnimation smallBlast2 = new SpriteAnimation(0, 8);
            smallBlast2.addFrame(new AnimationFrame(null, Utils.framesToTime(24, 4), null, null));
            smallBlast2.addFrame(smallFrame1);
            smallBlast2.addFrame(smallFrame2);
            smallBlast2.addFrame(smallFrame3);
            smallBlast2.addFrame(smallFrame4);
            smallBlast2.addFrame(smallFrame5);
            smallBlast2.addFrame(smallFrame6);
            smallBlast2.addFrame(smallFrame7);
            
            SpriteAnimation smallBlast3 = new SpriteAnimation(0, 8);
            smallBlast3.addFrame(new AnimationFrame(null, Utils.framesToTime(24, 8), null, null));
            smallBlast3.addFrame(smallFrame1);
            smallBlast3.addFrame(smallFrame2);
            smallBlast3.addFrame(smallFrame3);
            smallBlast3.addFrame(smallFrame4);
            smallBlast3.addFrame(smallFrame5);
            smallBlast3.addFrame(smallFrame6);
            smallBlast3.addFrame(smallFrame7);
            
           
            staticData.add(idle);
            staticData.add(smallBlast1);
            staticData.add(smallBlast2);
            staticData.add(smallBlast3);
            
            setStaticData(GameObjectType.EXPLOSION_GIANT, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.EFFECT);
        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		sprite.setSize((int)object.width, (int)object.height);
        sprite.setRenderComponent(render);

        // Hack.  Use static data differently for this object so we can share three animations
        // amongst three separate sprites.
    
        final SpriteAnimation idle = (SpriteAnimation)staticData.get(0);
        final SpriteAnimation smallBlast1 = (SpriteAnimation)staticData.get(1);
        final SpriteAnimation smallBlast2 = (SpriteAnimation)staticData.get(2);
        final SpriteAnimation smallBlast3 = (SpriteAnimation)staticData.get(3);
        
        sprite.addAnimation(idle);
        sprite.playAnimation(0);

        RenderComponent blast1Render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.EFFECT);
        SpriteComponent blast1Sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		blast1Sprite.setSize(32, 32);
        blast1Sprite.setRenderComponent(blast1Render);
        blast1Render.setDrawOffset(40, 50);
        blast1Sprite.addAnimation(smallBlast1);
        blast1Sprite.playAnimation(0);
        
        RenderComponent blast2Render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.EFFECT);
        SpriteComponent blast2Sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		blast2Sprite.setSize(32, 32);
        blast2Sprite.setRenderComponent(blast2Render);
        blast2Render.setDrawOffset(-10, 0);
        blast2Sprite.addAnimation(smallBlast2);
        blast2Sprite.playAnimation(0);
        
        RenderComponent blast3Render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.EFFECT);
        SpriteComponent blast3Sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
		blast3Sprite.setSize(32, 32);
        blast3Sprite.setRenderComponent(blast3Render);
        blast3Render.setDrawOffset(0, 32);
        blast3Sprite.addAnimation(smallBlast3);
        blast3Sprite.playAnimation(0);

        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setTimeUntilDeath(Math.max(
                Math.max(
                        Math.max(idle.getLength(), smallBlast1.getLength()), 
                        smallBlast2.getLength()), 
                smallBlast3.getLength()));
        
        DynamicCollisionComponent dynamicCollision = 
            (DynamicCollisionComponent)allocateComponent(DynamicCollisionComponent.class);
        sprite.setCollisionComponent(dynamicCollision);
        
        PlaySingleSoundComponent soundEffect = (PlaySingleSoundComponent)allocateComponent(PlaySingleSoundComponent.class);
        soundEffect.setSound(sSystemRegistry.soundSystem.load(R.raw.quick_explosion));
        
        
              
        object.team = Team.PLAYER;  // Maybe this should be an argument to this function.
        
        object.add(dynamicCollision);
        object.add(lifetime);
        object.add(render);
        object.add(sprite);
        object.add(soundEffect);

        
        object.add(blast1Render);
        object.add(blast1Sprite);
        
        object.add(blast2Render);
        object.add(blast2Sprite);
        
        object.add(blast3Render);
        object.add(blast3Sprite);
           
        
        return object;
    }
    
    
    public GameObject spawnGhostNPC(float positionX, float positionY) {
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mAlwaysActive;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.GHOST_NPC);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            staticData.add(gravity);
            staticData.add(movement);
            
            
            setStaticData(GameObjectType.GHOST_NPC, staticData);
        }
        
        NPCComponent patrol = (NPCComponent)allocateComponent(NPCComponent.class);
        LifetimeComponent life = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        
        object.team = Team.NONE;  
        object.life = 1;
        
        object.add(patrol);
        object.add(life);
        
        addStaticData(GameObjectType.GHOST_NPC, object, null);
        return object;
    }
    
    private GameObject spawnCameraBias(float positionX, float positionY) {
    	GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.CAMERA_BIAS);
        if (staticData == null) {
            final int staticObjectCount = 1;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent bias = allocateComponent(CameraBiasComponent.class);
            
            staticData.add(bias);
            
            setStaticData(GameObjectType.CAMERA_BIAS, staticData);
        }
        
        addStaticData(GameObjectType.CAMERA_BIAS, object, null);
        return object;
	}
    
    public GameObject spawnEffectSmokeBig(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.longTermTextureLibrary;

        GameObject object = null;
        // This is just an effect, so we can live without it if our pools are exhausted.
        if (componentAvailable(RenderComponent.class, 1)) { 
        	object = mGameObjectPool.allocate();
	        
	        object.getPosition().set(positionX, positionY);
	        object.activationRadius = mTightActivationRadius;
	        object.width = 32;
	        object.height = 32;
	        
	        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.SMOKE_BIG);
	        if (staticData == null) {
	            final int staticObjectCount = 6;
	            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
	            
	            GameComponent movement = allocateComponent(MovementComponent.class);
	            
	             
	            AnimationFrame frame2 = new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_big02), 
	                    Utils.framesToTime(24, 1), null, null);
	            
	            AnimationFrame frame3 = new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_big03), 
	                    Utils.framesToTime(24, 1), null, null);
	            
	            AnimationFrame frame4 = new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_big04), 
	                    Utils.framesToTime(24, 1), null, null);
	            
	            AnimationFrame frame5 = new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_big05), 
	                    Utils.framesToTime(24, 1), null, null);
	            
	            SpriteAnimation idle = new SpriteAnimation(0, 5);
	            idle.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_big01), 
	                    Utils.framesToTime(24, 10), null, null));
	            idle.addFrame(frame2);
	            idle.addFrame(frame3);
	            idle.addFrame(frame4);
	            idle.addFrame(frame5);
	            
	            SpriteAnimation idle2 = new SpriteAnimation(1, 5);
	            idle2.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_big01), 
	                    Utils.framesToTime(24, 13), null, null));
	            idle2.addFrame(frame2);
	            idle2.addFrame(frame3);
	            idle2.addFrame(frame4);
	            idle2.addFrame(frame5);
	            
	            SpriteAnimation idle3 = new SpriteAnimation(2, 5);
	            idle3.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_big01), 
	                    Utils.framesToTime(24, 8), null, null));
	            idle3.addFrame(frame2);
	            idle3.addFrame(frame3);
	            idle3.addFrame(frame4);
	            idle3.addFrame(frame5);
	
	            SpriteAnimation idle4 = new SpriteAnimation(3, 5);
	            idle4.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_big01), 
	                    Utils.framesToTime(24, 5), null, null));
	            idle4.addFrame(frame2);
	            idle4.addFrame(frame3);
	            idle4.addFrame(frame4);
	            idle4.addFrame(frame5);
	            
	            SpriteAnimation idle5 = new SpriteAnimation(4, 5);
	            idle5.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_big01), 
	                    Utils.framesToTime(24, 15), null, null));
	            idle5.addFrame(frame2);
	            idle5.addFrame(frame3);
	            idle5.addFrame(frame4);
	            idle5.addFrame(frame5);
	            
	            staticData.add(idle);
	            staticData.add(idle2);
	            staticData.add(idle3);
	            staticData.add(idle4);
	            staticData.add(idle5);
	            staticData.add(movement);
	            setStaticData(GameObjectType.SMOKE_BIG, staticData);
	        }
	        
	        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
	        render.setPriority(SortConstants.EFFECT);
	
	        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
	        sprite.setSize((int)object.width, (int)object.height);
	        sprite.setRenderComponent(render);
	     
	        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
	        lifetime.setDieWhenInvisible(true);
	                      
	        object.destroyOnDeactivation = true;
	        
	        object.add(lifetime);
	        object.add(render);
	        object.add(sprite);
	          
	        addStaticData(GameObjectType.SMOKE_BIG, object, sprite);
	        
	        final int animIndex = (int)(Math.random() * sprite.getAnimationCount());
	        final SpriteAnimation idle = sprite.findAnimation(animIndex);
	        if (idle != null) {
	            lifetime.setTimeUntilDeath(idle.getLength());
	            sprite.playAnimation(animIndex);
	        }
	        
        
        }
        return object;
    }
    
    public GameObject spawnEffectSmokeSmall(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.longTermTextureLibrary;

        GameObject object = null;
        // This is just an effect, so we can live without it if our pools are exhausted.
        if (componentAvailable(RenderComponent.class, 1)) {
	        object = mGameObjectPool.allocate();
	        object.getPosition().set(positionX, positionY);
	        object.activationRadius = mAlwaysActive;
	        object.width = 16;
	        object.height = 16;
	        
	        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.SMOKE_SMALL);
	        if (staticData == null) {
	            final int staticObjectCount = 2;
	            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
	            
	            GameComponent movement = allocateComponent(MovementComponent.class);
	
	            SpriteAnimation idle = new SpriteAnimation(0, 5);
	            idle.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_small01), 
	                    Utils.framesToTime(24, 10), null, null));
	            idle.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_small02), 
	                    Utils.framesToTime(24, 1), null, null));
	            idle.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_small03), 
	                    Utils.framesToTime(24, 1), null, null));
	            idle.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_small04), 
	                    Utils.framesToTime(24, 1), null, null));
	            idle.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_smoke_small05), 
	                    Utils.framesToTime(24, 1), null, null));
	            
	            staticData.add(idle);
	            staticData.add(movement);
	            setStaticData(GameObjectType.SMOKE_SMALL, staticData);
	        }
	        
	        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
	        render.setPriority(SortConstants.EFFECT);
	
	        SpriteComponent sprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
	        sprite.setSize((int)object.width, (int)object.height);
	        sprite.setRenderComponent(render);
	     
	        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
	        lifetime.setDieWhenInvisible(true);
	                      
	        object.destroyOnDeactivation = true;
	        
	        object.add(lifetime);
	        object.add(render);
	        object.add(sprite);
	          
	        addStaticData(GameObjectType.SMOKE_SMALL, object, sprite);
	        
	        final SpriteAnimation idle = sprite.findAnimation(0);
	        if (idle != null) {
	            lifetime.setTimeUntilDeath(idle.getLength());
	        }
	        
	        sprite.playAnimation(0);
        }

        return object;
    }
    
    public GameObject spawnEffectCrushFlash(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.longTermTextureLibrary;

        GameObject object = null;
        // This is just an effect, so we can live without it if our pools are exhausted.
        if (componentAvailable(RenderComponent.class, 1)) {
	        object = mGameObjectPool.allocate();
	        object.getPosition().set(positionX, positionY);
	        object.activationRadius = mAlwaysActive;
	        object.width = 64;
	        object.height = 64;
	        
	        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.CRUSH_FLASH);
	        if (staticData == null) {
	            final int staticObjectCount = 2;
	            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
	            
	            SpriteAnimation back = new SpriteAnimation(0, 3);
	            back.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_back01), 
	                    Utils.framesToTime(24, 1), null, null));
	            back.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_back02), 
	                    Utils.framesToTime(24, 1), null, null));
	            back.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_back03), 
	                    Utils.framesToTime(24, 1), null, null));
	            
	            SpriteAnimation front = new SpriteAnimation(1, 7);
	            front.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_front01), 
	                    Utils.framesToTime(24, 1), null, null));
	            front.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_front02), 
	                    Utils.framesToTime(24, 1), null, null));
	            front.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_front03), 
	                    Utils.framesToTime(24, 1), null, null));
	            front.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_front04), 
	                    Utils.framesToTime(24, 1), null, null));
	            front.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_front05), 
	                    Utils.framesToTime(24, 1), null, null));
	            front.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_front06), 
	                    Utils.framesToTime(24, 1), null, null));
	            front.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_front07), 
	                    Utils.framesToTime(24, 1), null, null));
	           
	            
	            staticData.add(back);
	            staticData.add(front);
	            setStaticData(GameObjectType.CRUSH_FLASH, staticData);
	        }
	        
	        
	        RenderComponent backRender = (RenderComponent)allocateComponent(RenderComponent.class);
	        backRender.setPriority(SortConstants.EFFECT);
	        
	        SpriteComponent backSprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
	        backSprite.setSize((int)object.width, (int)object.height);
	        backSprite.setRenderComponent(backRender);
	        
	        RenderComponent foreRender = (RenderComponent)allocateComponent(RenderComponent.class);
	        foreRender.setPriority(SortConstants.FOREGROUND_EFFECT);
	        
	        SpriteComponent foreSprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
	        foreSprite.setSize((int)object.width, (int)object.height);
	        foreSprite.setRenderComponent(foreRender);
	
	        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
	
	    
	        object.add(lifetime);
	        object.add(backRender);
	        object.add(foreRender);
	        object.add(foreSprite);
	        object.add(backSprite);
	          
	        addStaticData(GameObjectType.CRUSH_FLASH, object, backSprite);
	        addStaticData(GameObjectType.CRUSH_FLASH, null, foreSprite);
	
	        
	        final SpriteAnimation idle = foreSprite.findAnimation(1);
	        if (idle != null) {
	            lifetime.setTimeUntilDeath(idle.getLength());
	        }
	        
	        backSprite.playAnimation(0);
	        foreSprite.playAnimation(1);
        }
        
        return object;
    }
    
    public GameObject spawnEffectFlash(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.longTermTextureLibrary;
        GameObject object = null;
        // This is just an effect, so we can live without it if our pools are exhausted.
        if (componentAvailable(RenderComponent.class, 1)) {
	        object = mGameObjectPool.allocate();
	        object.getPosition().set(positionX, positionY);
	        object.activationRadius = mAlwaysActive;
	        object.width = 64;
	        object.height = 64;
	        
	        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.FLASH);
	        if (staticData == null) {
	            final int staticObjectCount = 1;
	            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
	            
	            SpriteAnimation back = new SpriteAnimation(0, 3);
	            back.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_back01), 
	                    Utils.framesToTime(24, 1), null, null));
	            back.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_back02), 
	                    Utils.framesToTime(24, 1), null, null));
	            back.addFrame(new AnimationFrame(
	                    textureLibrary.getTextureByResource(R.drawable.effect_crush_back03), 
	                    Utils.framesToTime(24, 1), null, null));
	            
	         
	            staticData.add(back);
	            setStaticData(GameObjectType.FLASH, staticData);
	        }
	        
	        
	        RenderComponent backRender = (RenderComponent)allocateComponent(RenderComponent.class);
	        backRender.setPriority(SortConstants.EFFECT);
	        
	        SpriteComponent backSprite = (SpriteComponent)allocateComponent(SpriteComponent.class);
	        backSprite.setSize((int)object.width, (int)object.height);
	        backSprite.setRenderComponent(backRender);
	        
	   
	
	        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
	
	    
	        object.add(lifetime);
	        object.add(backRender);
	        object.add(backSprite);
	          
	        addStaticData(GameObjectType.FLASH, object, backSprite);
	
	        
	        final SpriteAnimation idle = backSprite.findAnimation(0);
	        if (idle != null) {
	            lifetime.setTimeUntilDeath(idle.getLength());
	        }
	        
	        backSprite.playAnimation(0);
        }
        
        return object;
    }
    

    public GameObject spawnFrameRateWatcher(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;
        ContextParameters params = sSystemRegistry.contextParameters;
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(250, 0);	// HACK!
        object.activationRadius = mAlwaysActive;
        object.width = params.gameWidth;
        object.height = params.gameHeight;
        
        DrawableBitmap indicator = new DrawableBitmap(
                textureLibrary.allocateTexture(R.drawable.framerate_warning), 
                (int)object.width, 
                (int)object.height);
        
        indicator.setCrop(0, 8, 8, 8); // hack!  this shouldn't be hard-coded.
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.OVERLAY);
        render.setCameraRelative(false);
        
        FrameRateWatcherComponent watcher = (FrameRateWatcherComponent)allocateComponent(FrameRateWatcherComponent.class);
        watcher.setup(render, indicator);
        
        object.add(render);
        object.add(watcher);
        
          
        return object;
    }
    
    public GameObject spawnBreakableBlockPiece(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 16;
        object.height = 16;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.BREAKABLE_BLOCK_PIECE);
        if (staticData == null) {
            final int staticObjectCount = 4;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent gravity = allocateComponent(GravityComponent.class);
            GameComponent movement = allocateComponent(MovementComponent.class);
            
            SimplePhysicsComponent physics = (SimplePhysicsComponent)allocateComponent(SimplePhysicsComponent.class);
            physics.setBounciness(0.3f);
            
            DrawableBitmap piece = new DrawableBitmap(
                    textureLibrary.getTextureByResource(R.drawable.object_debris_piece), 
                    (int)object.width, 
                    (int)object.height);
            
            
            RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
            render.setPriority(SortConstants.GENERAL_OBJECT);
            render.setDrawable(piece);
            
            staticData.add(render);
            staticData.add(movement);
            staticData.add(gravity);
            staticData.add(physics);
            setStaticData(GameObjectType.BREAKABLE_BLOCK_PIECE, staticData);
        }
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setTimeUntilDeath(3.0f);
        
        BackgroundCollisionComponent bgcollision = (BackgroundCollisionComponent)allocateComponent(BackgroundCollisionComponent.class);
        bgcollision.setSize(12, 12);
        bgcollision.setOffset(2, 2); 
        
        
        object.destroyOnDeactivation = true;
        
        object.add(lifetime);
        object.add(bgcollision);
        
        addStaticData(GameObjectType.BREAKABLE_BLOCK_PIECE, object, null);
     
        return object;
    }
    
    public GameObject spawnBreakableBlockPieceSpawner(float positionX, float positionY) {
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 1;
        object.height = 1;
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setTimeUntilDeath(0.5f);
        
        LaunchProjectileComponent pieceSpawner 
            = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
        pieceSpawner.setObjectTypeToSpawn(GameObjectType.BREAKABLE_BLOCK_PIECE);
        pieceSpawner.setDelayBeforeFirstSet(0.0f);
        pieceSpawner.setSetsPerActivation(1);
        pieceSpawner.setShotsPerSet(3);
        pieceSpawner.setDelayBetweenShots(0.0f);
        pieceSpawner.setOffsetX(16);
        pieceSpawner.setOffsetY(16);
        pieceSpawner.setVelocityX(600.0f);
        pieceSpawner.setVelocityY(-1000.0f);
        pieceSpawner.setThetaError(1.0f);
        
        object.life = 1;    
        object.destroyOnDeactivation = true;
        
        object.add(lifetime);
        object.add(pieceSpawner);
        
        return object;
    }
    
    public GameObject spawnSmokePoof(float positionX, float positionY) {
        
    	GameObject object = null;
        // This is just an effect, so we can live without it if our pools are exhausted.
        if (componentAvailable(LaunchProjectileComponent.class, 2)) {
	        object = mGameObjectPool.allocate();
	        object.getPosition().set(positionX, positionY);
	        object.activationRadius = mTightActivationRadius;
	        object.width = 1;
	        object.height = 1;
	        
	        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
	        lifetime.setTimeUntilDeath(0.5f);
	        
	        LaunchProjectileComponent smokeGun 
		        = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
	        smokeGun.setSetsPerActivation(1);
	        smokeGun.setShotsPerSet(3);
		    smokeGun.setDelayBetweenShots(0.0f);
		    smokeGun.setObjectTypeToSpawn(GameObjectType.SMOKE_BIG);
		    smokeGun.setVelocityX(200.0f);
		    smokeGun.setVelocityY(200.0f);
		    smokeGun.setOffsetX(16);
		    smokeGun.setOffsetY(16);
		    smokeGun.setThetaError(1.0f);
		    
		    LaunchProjectileComponent smokeGun2 
		        = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
		    smokeGun2.setSetsPerActivation(1);
		    smokeGun2.setShotsPerSet(3);
		    smokeGun2.setDelayBetweenShots(0.0f);
		    smokeGun2.setObjectTypeToSpawn(GameObjectType.SMOKE_SMALL);
		    smokeGun2.setVelocityX(200.0f);
		    smokeGun2.setVelocityY(200.0f);
		    smokeGun2.setThetaError(1.0f); 
		    smokeGun2.setOffsetX(16);
		    smokeGun2.setOffsetY(16);
	        
	        object.life = 1;    
	        object.destroyOnDeactivation = true;
	        
	        object.add(lifetime);
	        object.add(smokeGun);
	        object.add(smokeGun2);
        }
        return object;
    }
    
    public GameObject spawnGemEffect(float positionX, float positionY) {
        TextureLibrary textureLibrary = sSystemRegistry.shortTermTextureLibrary;

        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 32;
        object.height = 32;
        
        FixedSizeArray<BaseObject> staticData = getStaticData(GameObjectType.GEM_EFFECT);
        if (staticData == null) {
            final int staticObjectCount = 2;
            staticData = new FixedSizeArray<BaseObject>(staticObjectCount);
            
            GameComponent movement = allocateComponent(MovementComponent.class);
                
            staticData.add(movement);
            
            setStaticData(GameObjectType.GEM_EFFECT, staticData);
        }
        
        RenderComponent render = (RenderComponent)allocateComponent(RenderComponent.class);
        render.setPriority(SortConstants.EFFECT);

        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setTimeUntilDeath(0.5f);
        
        FadeDrawableComponent fadeOut = (FadeDrawableComponent)allocateComponent(FadeDrawableComponent.class);
        fadeOut.setupFade(1.0f, 0.0f, 0.5f, FadeDrawableComponent.LOOP_TYPE_NONE, FadeDrawableComponent.FADE_LINEAR, 0.0f);
        fadeOut.setTexture(textureLibrary.allocateTexture(R.drawable.object_ruby01));
        fadeOut.setRenderComponent(render);
        
        object.destroyOnDeactivation = true;
        
        object.add(lifetime);
        object.add(fadeOut);
        object.add(render);
        
        addStaticData(GameObjectType.GEM_EFFECT, object, null);       
     
        return object;
    }
    
    public GameObject spawnGemEffectSpawner(float positionX, float positionY) {
        
        GameObject object = mGameObjectPool.allocate();
        object.getPosition().set(positionX, positionY);
        object.activationRadius = mTightActivationRadius;
        object.width = 1;
        object.height = 1;
        
        LifetimeComponent lifetime = (LifetimeComponent)allocateComponent(LifetimeComponent.class);
        lifetime.setTimeUntilDeath(0.5f);
        
        final int gems = 6;
        final float angleIncrement = (float)(2.0f * Math.PI) / gems;
        for (int x = 0; x < gems; x++) {
	        LaunchProjectileComponent gemGun 
		        = (LaunchProjectileComponent)allocateComponent(LaunchProjectileComponent.class);
	        gemGun.setSetsPerActivation(1);
	        gemGun.setShotsPerSet(1);
	        gemGun.setDelayBetweenShots(0.0f);
	        gemGun.setObjectTypeToSpawn(GameObjectType.GEM_EFFECT);
	        gemGun.setVelocityX((float)Math.sin(angleIncrement * x) * 150.0f);
	        gemGun.setVelocityY((float)Math.cos(angleIncrement * x) * 150.0f);
	        gemGun.setOffsetX(16);
	        gemGun.setOffsetY(16);
		    
	        object.add(gemGun);
        }
        
	   
        
        object.life = 1;    
        object.destroyOnDeactivation = true;
        
        object.add(lifetime);
        

        return object;
    }
    
    /** Comparator for game objects objects. */
    private final static class ComponentPoolComparator implements Comparator<GameComponentPool> {
        public int compare(final GameComponentPool object1, final GameComponentPool object2) {
            int result = 0;
            if (object1 == null && object2 != null) {
                result = 1;
            } else if (object1 != null && object2 == null) {
                result = -1;
            } else if (object1 != null && object2 != null) {
                result = object1.objectClass.hashCode() - object2.objectClass.hashCode();
            }
            return result;
        }
    }
    
    public class GameObjectPool extends TObjectPool<GameObject> {

        public GameObjectPool() {
            super();
        }
        
        public GameObjectPool(int size) {
            super(size);
        }
        
        @Override
        protected void fill() {
            for (int x = 0; x < getSize(); x++) {
                getAvailable().add(new GameObject());
            }
        }

        @Override
        public void release(Object entry) {
            ((GameObject)entry).reset();
            super.release(entry);
        }

    }
}

