#pragma version(1)

#pragma rs java_package_name(com.android.galaxy4)
#include "rs_graphics.rsh"
#pragma stateVertex(parent);
#pragma stateStore(parent);

typedef struct __attribute__((packed, aligned(4))) Particle {
    float3 position;
    float pointSize;
} Particle_t;

typedef struct VpConsts {
    rs_matrix4x4 MVP;
    float scaleSize;
} VpConsts_t;
VpConsts_t *vpConstants;

Particle_t *spaceClouds;
Particle_t *bgStars;
Particle_t *staticStars;

rs_mesh spaceCloudsMesh;
rs_mesh bgStarsMesh;
rs_mesh staticStarsMesh;

rs_program_vertex vertSpaceClouds;
rs_program_vertex vertBgStars;
rs_program_vertex vertStaticStars;
rs_program_fragment fragSpaceClouds;
rs_program_fragment fragBgStars;
rs_program_fragment fragStaticStars;
rs_program_vertex vertBg;
rs_program_fragment fragBg;

rs_allocation textureSpaceCloud;
rs_allocation textureStaticStar;
rs_allocation textureStaticStar2;
rs_allocation textureBg;

float densityDPI;

static int gGalaxyRadius = 300;
static float screenWidth;
static float screenHeight;

static int numBgStars;
static int numClouds;
static int numStaticStars;

#define PI 3.1415f
#define TWO_PI 6.283f

/**
 * Helper function to generate the stars.
 */
static float randomGauss() {
    float x1;
    float x2;
    float w = 2.f;

    while (w >= 1.0f) {
        x1 = rsRand(2.0f) - 1.0f;
        x2 = rsRand(2.0f) - 1.0f;
        w = x1 * x1 + x2 * x2;
    }

    w = sqrt(-2.0f * log(w) / w);
    return x1 * w;
}

static float mapf(float minStart, float minStop, float maxStart, float maxStop, float value) {
    return maxStart + (maxStart - maxStop) * ((value - minStart) / (minStop - minStart));
}

void positionParticles() {
    screenWidth = rsgGetWidth();
    screenHeight = rsgGetHeight();

    float wRatio = 1.0f;
    float hRatio = 1.0f;

    if (screenWidth > screenHeight) {
        wRatio = screenWidth/screenHeight;
        screenHeight = screenWidth;
    } else {
        hRatio = screenHeight/screenWidth;
        screenWidth = screenHeight;
    }

    float scale = gGalaxyRadius / (screenWidth * 0.5f);

    // clouds
    Particle_t* particle = spaceClouds;
    numClouds = rsAllocationGetDimX(rsGetAllocation(spaceClouds));
    for (int i=0; i<numClouds; i++) {
        float d = fabs(randomGauss()) * gGalaxyRadius * 0.5f + rsRand(64.0f);
        d = mapf(-4.0f, gGalaxyRadius + 4.0f, 0.0f, scale, d);
        float id = d / gGalaxyRadius;
        float z = randomGauss() * 0.4f * (1.0f - id);
        if (d > gGalaxyRadius * 0.15f) {
            z *= 0.6f * (1.0f - id);
        } else {
            z *= 0.72f;
        }
        particle->position.x = rsRand(TWO_PI);
        particle->position.y = d;
        particle->pointSize = 1.0f;
        particle->position.z = z/5.0f;
        particle++;
    }

    // bg stars
    numBgStars = rsAllocationGetDimX(rsGetAllocation(bgStars));
    particle = bgStars;
    for (int i=0; i<numBgStars; i++) {
        float d = fabs(randomGauss()) * gGalaxyRadius * 0.5f + rsRand(64.0f);
        d = mapf(-4.0f, gGalaxyRadius + 4.0f, 0.0f, scale, d);
        float id = d / gGalaxyRadius;
        float z = randomGauss() * 0.4f * (1.0f - id);
        if (d > gGalaxyRadius * 0.15f) {
            z *= 0.6f * (1.0f - id);
        } else {
            z *= 0.72f;
        }
        particle->position.x = rsRand(TWO_PI);
        particle->position.y = d;
        particle->pointSize = 1.0f;
        particle->position.z = z/5.0f;
        particle++;
    }

    // static stars
    numStaticStars = rsAllocationGetDimX(rsGetAllocation(staticStars));
    particle = staticStars;
    for (int i=0; i<numStaticStars; i++) {
        particle->position.x = rsRand(-wRatio, wRatio);
        particle->position.y = rsRand(-hRatio, hRatio);
        particle->pointSize = rsRand(1.0f, 10.0f);
        particle++;
    }
}

int root() {
    float xpos = 0.0f;
    float ypos = 0.0f;
    xpos = -(screenWidth-rsgGetWidth())/2.0f;
    ypos = -(screenHeight-rsgGetHeight())/2.0f;

    rsgClearColor(0.0f, 0.f, 0.f, 0.5f);

    // bg
    rsgBindProgramVertex(vertBg);
    rsgBindProgramFragment(fragBg);
    rsgBindTexture(fragBg, 0, textureBg);
    rsgDrawRect(xpos, ypos, screenWidth+xpos, screenHeight+ypos, 0.0f);

    // space cloud
    rsgBindProgramVertex(vertSpaceClouds);
    Particle_t *particle = spaceClouds;
    for (int i=0; i<numClouds; i++) {
        particle->position.x -= .065;
        particle++;
    }
    rsgBindProgramFragment(fragSpaceClouds);
    rsgBindTexture(fragSpaceClouds, 0, textureSpaceCloud);
    rsgDrawMesh(spaceCloudsMesh);

    // bg stars
    rsgBindProgramVertex(vertBgStars);
    particle = bgStars;
    for (int i=0; i<numBgStars; i++) {
        particle->position.x -= .007;
        particle++;
    }
    rsgBindProgramFragment(fragBgStars);
    rsgDrawMesh(bgStarsMesh);

    // static stars
    rsgBindTexture(fragStaticStars, 0, textureStaticStar);
    rsgBindTexture(fragStaticStars, 1, textureStaticStar2);
    rsgBindProgramVertex(vertStaticStars);
    rsgBindProgramFragment(fragStaticStars);
    rsgDrawMesh(staticStarsMesh);

    return 40;
}