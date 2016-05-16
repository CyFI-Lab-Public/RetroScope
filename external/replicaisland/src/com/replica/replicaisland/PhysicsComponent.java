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

/**
 * Component that adds physics to its parent game object.  This component implements force
 * calculation based on mass, impulses, friction, and collisions.
 */
public class PhysicsComponent extends GameComponent {

    private float mMass;
    private float mBounciness; // 1.0 = super bouncy, 0.0 = zero bounce
    private float mInertia;
    private float mStaticFrictionCoeffecient;
    private float mDynamicFrictionCoeffecient;

    private static final float DEFAULT_MASS = 1.0f;
    private static final float DEFAULT_BOUNCINESS = 0.1f;
    private static final float DEFAULT_INERTIA = 0.01f;
    private static final float DEFAULT_STATIC_FRICTION_COEFFECIENT = 0.05f;
    private static final float DEFAULT_DYNAMIC_FRICTION_COEFFECIENT = 0.02f;
    
    PhysicsComponent() {
        super();
        reset();
        setPhase(ComponentPhases.POST_PHYSICS.ordinal());
    }
    
    @Override
    public void reset() {
        // TODO: no reason to call accessors here locally.
        setMass(DEFAULT_MASS);
        setBounciness(DEFAULT_BOUNCINESS);
        setInertia(DEFAULT_INERTIA);
        setStaticFrictionCoeffecient(DEFAULT_STATIC_FRICTION_COEFFECIENT);
        setDynamicFrictionCoeffecient(DEFAULT_DYNAMIC_FRICTION_COEFFECIENT);
    }

    @Override
    public void update(float timeDelta, BaseObject parent) {
        GameObject parentObject = (GameObject) parent;

        // we look to user data so that other code can provide impulses
        Vector2 impulseVector = parentObject.getImpulse();

        final Vector2 currentVelocity = parentObject.getVelocity();
        
        final Vector2 surfaceNormal = parentObject.getBackgroundCollisionNormal();
        if (surfaceNormal.length2() > 0.0f) {
            resolveCollision(currentVelocity, impulseVector, surfaceNormal, impulseVector);
        }

        VectorPool vectorPool = sSystemRegistry.vectorPool;

        // if our speed is below inertia, we need to overcome inertia before we can move.

        boolean physicsCausesMovement = true;

        final float inertiaSquared = getInertia() * getInertia();

        Vector2 newVelocity = vectorPool.allocate(currentVelocity);
        newVelocity.add(impulseVector);

        if (newVelocity.length2() < inertiaSquared) {
            physicsCausesMovement = false;
        }

        final boolean touchingFloor = parentObject.touchingGround();

        GravityComponent gravity = parentObject.findByClass(GravityComponent.class);

        if (touchingFloor && currentVelocity.y <= 0.0f && Math.abs(newVelocity.x) > 0.0f
                        && gravity != null) {
            final Vector2 gravityVector = gravity.getGravity();

            // if we were moving last frame, we'll use dynamic friction. Else
            // static.
            float frictionCoeffecient = Math.abs(currentVelocity.x) > 0.0f ? 
                        getDynamicFrictionCoeffecient() : getStaticFrictionCoeffecient();
            frictionCoeffecient *= timeDelta;

            // Friction = cofN, where cof = friction coefficient and N = force
            // perpendicular to the ground.
            final float maxFriction = Math.abs(gravityVector.y) * getMass()
                    * frictionCoeffecient;

            if (maxFriction > Math.abs(newVelocity.x)) {
                newVelocity.x = (0.0f);
            } else {
                newVelocity.x = (newVelocity.x
                        - (maxFriction * Utils.sign(newVelocity.x)));
            }
        }

        if (Math.abs(newVelocity.x) < 0.01f) {
            newVelocity.x = (0.0f);
        }

        if (Math.abs(newVelocity.y) < 0.01f) {
            newVelocity.y = (0.0f);
        }

        // physics-based movements means constant acceleration, always. set the target to the
        // velocity.
        if (physicsCausesMovement) {
            parentObject.setVelocity(newVelocity);
            parentObject.setTargetVelocity(newVelocity);
            parentObject.setAcceleration(Vector2.ZERO);
            parentObject.setImpulse(Vector2.ZERO);
        }

        vectorPool.release(newVelocity);
    }

    protected void resolveCollision(Vector2 velocity, Vector2 impulse, Vector2 opposingNormal,
                    Vector2 outputImpulse) {
        VectorPool vectorPool = sSystemRegistry.vectorPool;

        outputImpulse.set(impulse);

        Vector2 collisionNormal = vectorPool.allocate(opposingNormal);

        collisionNormal.normalize();

        Vector2 relativeVelocity = vectorPool.allocate(velocity);
        relativeVelocity.add(impulse);

        final float dotRelativeAndNormal = relativeVelocity.dot(collisionNormal);

        // make sure the motion of the entity requires resolution
        if (dotRelativeAndNormal < 0.0f) {
            final float coefficientOfRestitution = getBounciness(); // 0 = perfectly inelastic,
                                                                    // 1 = perfectly elastic

            // calculate an impulse to apply to the entity
            float j = (-(1 + coefficientOfRestitution) * dotRelativeAndNormal);

            j /= ((collisionNormal.dot(collisionNormal)) * (1 / getMass()));

            Vector2 entity1Adjust = vectorPool.allocate(collisionNormal);

            entity1Adjust.set(collisionNormal);
            entity1Adjust.multiply(j);
            entity1Adjust.divide(getMass());
            entity1Adjust.add(impulse);
            outputImpulse.set(entity1Adjust);
            vectorPool.release(entity1Adjust);

        }

        vectorPool.release(collisionNormal);
        vectorPool.release(relativeVelocity);
    }

    protected void resolveCollision(Vector2 velocity, Vector2 impulse, Vector2 opposingNormal,
                    float otherMass, Vector2 otherVelocity, Vector2 otherImpulse,
                    float otherBounciness, Vector2 outputImpulse) {
        VectorPool vectorPool = sSystemRegistry.vectorPool;

        Vector2 collisionNormal = vectorPool.allocate(opposingNormal);
        collisionNormal.normalize();

        Vector2 entity1Velocity = vectorPool.allocate(velocity);
        entity1Velocity.add(impulse);

        Vector2 entity2Velocity = vectorPool.allocate(otherVelocity);
        entity2Velocity.add(otherImpulse);

        Vector2 relativeVelocity = vectorPool.allocate(entity1Velocity);
        relativeVelocity.subtract(entity2Velocity);

        final float dotRelativeAndNormal = relativeVelocity.dot(collisionNormal);

        // make sure the entities' motion requires resolution
        if (dotRelativeAndNormal < 0.0f) {
            final float bounciness = Math.min(getBounciness() + otherBounciness, 1.0f);
            final float coefficientOfRestitution = bounciness;  // 0 = perfectly inelastic,
                                                                // 1 = perfectly elastic
            
            // calculate an impulse to apply to both entities
            float j = (-(1 + coefficientOfRestitution) * dotRelativeAndNormal);

            j /= ((collisionNormal.dot(collisionNormal)) * (1 / getMass() + 1 / otherMass));

            Vector2 entity1Adjust = vectorPool.allocate(collisionNormal);
            entity1Adjust.multiply(j);
            entity1Adjust.divide(getMass());
            entity1Adjust.add(impulse);

            outputImpulse.set(entity1Adjust);

            // TODO: Deal impulses both ways.
            /*
             * Vector3 entity2Adjust = (collisionNormal j); 
             * entity2Adjust[0] /= otherMass;
             * entity2Adjust[1] /= otherMass; 
             * entity2Adjust[2] /= otherMass;
             * 
             * const Vector3 newEntity2Impulse = otherImpulse + entity2Adjust;
             */

            vectorPool.release(entity1Adjust);
        }

        vectorPool.release(collisionNormal);
        vectorPool.release(entity1Velocity);
        vectorPool.release(entity2Velocity);
        vectorPool.release(relativeVelocity);
    }

    public float getMass() {
        return mMass;
    }

    public void setMass(float mass) {
        mMass = mass;
    }

    public float getBounciness() {
        return mBounciness;
    }

    public void setBounciness(float bounciness) {
        mBounciness = bounciness;
    }

    public float getInertia() {
        return mInertia;
    }

    public void setInertia(float inertia) {
        mInertia = inertia;
    }

    public float getStaticFrictionCoeffecient() {
        return mStaticFrictionCoeffecient;
    }

    public void setStaticFrictionCoeffecient(float staticFrictionCoeffecient) {
        mStaticFrictionCoeffecient = staticFrictionCoeffecient;
    }

    public float getDynamicFrictionCoeffecient() {
        return mDynamicFrictionCoeffecient;
    }

    public void setDynamicFrictionCoeffecient(float dynamicFrictionCoeffecient) {
        mDynamicFrictionCoeffecient = dynamicFrictionCoeffecient;
    }

}
