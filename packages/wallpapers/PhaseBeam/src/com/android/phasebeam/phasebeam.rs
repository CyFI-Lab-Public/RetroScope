#pragma version(1)

#pragma rs java_package_name(com.android.phasebeam)

#include "rs_graphics.rsh"
#pragma stateVertex(parent);
#pragma stateStore(parent);

rs_allocation textureDot;
rs_allocation textureBeam;

rs_program_vertex vertBg;
rs_program_fragment fragBg;

rs_program_vertex vertDots;
rs_program_fragment fragDots;

static int numBeamParticles;
static int numDotParticles;
static int numVertColors;

typedef struct __attribute__((packed, aligned(4))) Particle {
    float3 position;
    float offsetX;
} Particle_t;

typedef struct VpConsts {
    rs_matrix4x4 MVP;
    float scaleSize;
} VpConsts_t;
VpConsts_t *vpConstants;

typedef struct VertexColor_s {
    float3 position;
    float offsetX;
    float4 color;
} VertexColor;

VertexColor* vertexColors;
Particle_t *dotParticles;
Particle_t *beamParticles;
rs_mesh dotMesh;
rs_mesh beamMesh;
rs_mesh gBackgroundMesh;

float densityDPI;
float xOffset = 0.5;

static float screenWidth;
static float screenHeight;
static float halfScreenWidth;
static float quarterScreenWidth;
static float quarterScreenHeight;
static float halfScreenHeight;

static float newOffset = 0.5;
static float oldOffset = 0.5;

void positionParticles() {
    screenWidth = rsgGetWidth();
    screenHeight = rsgGetHeight();
    halfScreenWidth = screenWidth/2.0f;
    halfScreenHeight = screenHeight/2.0f;
    quarterScreenWidth = screenWidth/4.0f;
    quarterScreenHeight = screenHeight/4.0f;
    Particle_t* particle = dotParticles;
    numDotParticles = rsAllocationGetDimX(rsGetAllocation(dotParticles));
    numVertColors = rsAllocationGetDimX(rsGetAllocation(vertexColors));
    for(int i=0; i<numDotParticles; i++) {
        particle->position.x = rsRand(0.0f, 3.0f);
        particle->position.y = rsRand(-1.25f, 1.25f);

        float z;
        if (i < 3) {
            z = 14.0f;
        } else if(i < 7) {
            z = 25.0f;
        } else if(i < 4) {
            z = rsRand(10.f, 20.f);
        } else if(i == 10) {
            z = 24.0f;
            particle->position.x = 1.0;
        } else {
            z = rsRand(6.0f, 14.0f);
        }
        particle->position.z = z;
        particle->offsetX = 0;

        particle++;
    }

    Particle_t* beam = beamParticles;
    numBeamParticles = rsAllocationGetDimX(rsGetAllocation(beamParticles));
    for(int i=0; i<numBeamParticles; i++) {
        float z;
        if(i < 20) {
            z = rsRand(4.0f, 10.0f)/2.0f;
        } else {
            z = rsRand(4.0f, 35.0f)/2.0f;
        }
        beamParticles->position.x = rsRand(-1.25f, 1.25f);
        beamParticles->position.y = rsRand(-1.05f, 1.205f);

        beamParticles->position.z = z;
        beamParticles->offsetX = 0;
        beamParticles++;
    }
}

int root() {

    newOffset = xOffset*2;
    rsgClearColor(0.0f, 0.f, 0.f,1.0f);

    if(newOffset != oldOffset) {
        VertexColor* vert = vertexColors;
        for(int i=0; i<numVertColors; i++) {
            vert->offsetX = -xOffset/2.0;
            vert++;
        }
    }

    rsgBindProgramVertex(vertBg);
    rsgBindProgramFragment(fragBg);

    rsgDrawMesh(gBackgroundMesh);

    Particle_t* beam = beamParticles;
    Particle_t* particle = dotParticles;

    for(int i=0; i<numDotParticles; i++) {

        if(newOffset==oldOffset) {
            if(beam->position.x/beam->position.z > 0.5) {
                beam->position.x = -1.0;
            }
            if(particle->position.x/particle->position.z > 0.5) {
                particle->position.x = -1.0;
            }

            if(beam->position.y > 1.05) {
                beam->position.y = -1.05;
                beam->position.x = rsRand(-1.25f, 1.25f);
            } else {
                beam->position.y = beam->position.y + 0.000160*beam->position.z;
            }
            if(particle->position.y > 1.25) {
                particle->position.y = -1.25;
                particle->position.x = rsRand(0.0f, 3.0f);

            } else {
                particle->position.y = particle->position.y + 0.00022*particle->position.z;
            }
        }

        beam->position.x = beam->position.x + 0.0001*beam->position.z;
        beam->offsetX = newOffset;
        beam++;
        particle->offsetX = newOffset;
        particle->position.x = particle->position.x + 0.0001560*beam->position.z;
        particle++;
    }

    rsgBindProgramVertex(vertDots);
    rsgBindProgramFragment(fragDots);

    rsgBindTexture(fragDots, 0, textureBeam);
    rsgDrawMesh(beamMesh);

    rsgBindTexture(fragDots, 0, textureDot);
    rsgDrawMesh(dotMesh);

    oldOffset = newOffset;

    return 66;

}
