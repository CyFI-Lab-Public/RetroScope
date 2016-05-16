#pragma version(1)

#pragma rs java_package_name(com.android.noisefield)

#include "rs_graphics.rsh"
#pragma stateVertex(parent);
#pragma stateStore(parent);


rs_allocation textureDot;
rs_allocation textureVignette;

rs_program_vertex vertBg;
rs_program_fragment fragBg;

rs_program_vertex vertDots;
rs_program_fragment fragDots;

rs_program_store storeAlpha;
rs_program_store storeAdd;

typedef struct VpConsts {
    rs_matrix4x4 MVP;
    float scaleSize;
} VpConsts_t;
VpConsts_t *vpConstants;

typedef struct Particle {
    float3 position;
    float speed;
    float wander;
    float alphaStart;
    float alpha;
    int life;
    int death;
} Particle_t;
Particle_t *dotParticles;


typedef struct VertexColor_s {
    float3 position;
    float4 color;
    float offsetX;

} VertexColor;
VertexColor* vertexColors;

rs_mesh dotMesh;
rs_mesh gBackgroundMesh;

float densityDPI;
bool touchDown = false;

#define B 0x100
#define BM 0xff
#define N 0x1000

static int p[B + B + 2];
static float g3[B + B + 2][3];
static float g2[B + B + 2][2];
static float g1[B + B + 2];

// used for motion easing from touch to non-touch state
static float touchInfluence = 0;

static float touchX = 0;
static float touchY = 0;

static float noise_sCurve(float t) {
    return t * t * (3.0f - 2.0f * t);
}

static void normalizef2(float v[]) {
    float s = (float)sqrt(v[0] * v[0] + v[1] * v[1]);
    v[0] = v[0] / s;
    v[1] = v[1] / s;
}

static void normalizef3(float v[]) {
    float s = (float)sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    v[0] = v[0] / s;
    v[1] = v[1] / s;
    v[2] = v[2] / s;
}

void init() {
    int i, j, k;
    for (i = 0; i < B; i++) {
        p[i] = i;

        g1[i] = (float)(rsRand(B * 2) - B) / B;

        for (j = 0; j < 2; j++)
            g2[i][j] = (float)(rsRand(B * 2) - B) / B;
        normalizef2(g2[i]);

        for (j = 0; j < 3; j++)
            g3[i][j] = (float)(rsRand(B * 2) - B) / B;
        normalizef3(g3[i]);
    }

    for (i = B-1; i >= 0; i--) {
        k = p[i];
        p[i] = p[j = rsRand(B)];
        p[j] = k;
    }

    for (i = 0; i < B + 2; i++) {
        p[B + i] = p[i];
        g1[B + i] = g1[i];
        for (j = 0; j < 2; j++)
            g2[B + i][j] = g2[i][j];
        for (j = 0; j < 3; j++)
            g3[B + i][j] = g3[i][j];
    }
}

static float noisef2(float x, float y) {
    int bx0, bx1, by0, by1, b00, b10, b01, b11;
    float rx0, rx1, ry0, ry1, sx, sy, a, b, t, u, v;
    float *q;
    int i, j;

    t = x + N;
    bx0 = ((int)t) & BM;
    bx1 = (bx0+1) & BM;
    rx0 = t - (int)t;
    rx1 = rx0 - 1.0f;

    t = y + N;
    by0 = ((int)t) & BM;
    by1 = (by0+1) & BM;
    ry0 = t - (int)t;
    ry1 = ry0 - 1.0f;

    i = p[bx0];
    j = p[bx1];

    b00 = p[i + by0];
    b10 = p[j + by0];
    b01 = p[i + by1];
    b11 = p[j + by1];

    sx = noise_sCurve(rx0);
    sy = noise_sCurve(ry0);

    q = g2[b00]; u = rx0 * q[0] + ry0 * q[1];
    q = g2[b10]; v = rx1 * q[0] + ry0 * q[1];
    a = mix(u, v, sx);

    q = g2[b01]; u = rx0 * q[0] + ry1 * q[1];
    q = g2[b11]; v = rx1 * q[0] + ry1 * q[1];
    b = mix(u, v, sx);

    return 1.5f * mix(a, b, sy);
}

void positionParticles() {
    Particle_t* particle = dotParticles;
    int size = rsAllocationGetDimX(rsGetAllocation(dotParticles));
    for(int i=0; i<size; i++) {
        particle->position.x = rsRand(-1.0f, 1.0f);
        particle->position.y = rsRand(-1.0f, 1.0f);
        particle->speed = rsRand(0.0002f, 0.02f);
        particle->wander = rsRand(0.50f, 1.5f);
        particle->death = 0;
        particle->life = rsRand(300, 800);
        particle->alphaStart = rsRand(0.01f, 1.0f);
        particle->alpha = particle->alphaStart;
        particle++;
    }
}

void touch(float x, float y) {
    bool landscape = rsgGetWidth() > rsgGetHeight();
    float wRatio;
    float hRatio;
    if(!landscape){
        wRatio = 1.0;
        hRatio = rsgGetHeight()/rsgGetWidth();
    } else {
        hRatio = 1.0;
        wRatio = rsgGetWidth()/rsgGetHeight();
    }

    touchInfluence = 1.0;
    touchX = x/rsgGetWidth() * wRatio * 2 - wRatio;
    touchY = -(y/rsgGetHeight() * hRatio * 2 - hRatio);
}

int root() {
    rsgClearColor(0.0, 0.0, 0.0, 1.0f);
    int size = rsAllocationGetDimX(rsGetAllocation(vertexColors));
    rsgBindProgramVertex(vertDots);
    rsgBindProgramFragment(fragDots);
    rsgBindTexture(fragDots, 0, textureDot);
    rsgDrawMesh(dotMesh);

    // bg
    rsgBindProgramVertex(vertBg);
    rsgBindProgramFragment(fragBg);
    rsgDrawMesh(gBackgroundMesh);

    // dots
    Particle_t* particle = dotParticles;
    size = rsAllocationGetDimX(rsGetAllocation(dotParticles));
    float rads;
    float speed;

    for(int i=0; i<size; i++) {
        if(particle->life < 0 || particle->position.x < -1.2 ||
           particle->position.x >1.2 || particle->position.y < -1.7 ||
           particle->position.y >1.7) {
            particle->position.x = rsRand(-1.0f, 1.0f);
            particle->position.y = rsRand(-1.0f, 1.0f);
            particle->speed = rsRand(0.0002f, 0.02f);
            particle->wander = rsRand(0.50f, 1.5f);
            particle->death = 0;
            particle->life = rsRand(300, 800);
            particle->alphaStart = rsRand(0.01f, 1.0f);
            particle->alpha = particle->alphaStart;
        }

        float touchDist = sqrt(pow(touchX - particle->position.x, 2) +
                               pow(touchY - particle->position.y, 2));

        float noiseval = noisef2(particle->position.x, particle->position.y);
        if(touchDown || touchInfluence > 0.0) {
            if(touchDown){
                touchInfluence = 1.0;
            }
            rads = atan2(touchX - particle->position.x + noiseval,
                         touchY - particle->position.y + noiseval);
            if(touchDist != 0){
                speed = ( (0.25 + (noiseval * particle->speed + 0.01)) / touchDist * 0.3 );
                speed = speed * touchInfluence;
            } else {
                speed = .3;
            }
            particle->position.x += cos(rads) * speed * 0.2;
            particle->position.y += sin(rads) * speed * 0.2;
        }

        float angle = 360 * noiseval * particle->wander;
        speed = noiseval * particle->speed + 0.01;
        rads = angle * 3.14159265 / 180.0;

        particle->position.x += cos(rads) * speed * 0.33;
        particle->position.y += sin(rads) * speed * 0.33;

        particle->life--;
        particle->death++;

        float dist = sqrt(particle->position.x*particle->position.x +
                          particle->position.y*particle->position.y);
        if(dist < 0.95) {
            dist = 0;
            particle->alphaStart *= (1-dist);
        } else {
            dist = dist-0.95;
            if(particle->alphaStart < 1.0f) {
                particle->alphaStart +=0.01;
                particle->alphaStart *= (1-dist);
            }
        }

        if(particle->death < 101) {
            particle->alpha = (particle->alphaStart)*(particle->death)/100.0;
        } else if(particle->life < 101) {
            particle->alpha = particle->alpha*particle->life/100.0;
        } else {
            particle->alpha = particle->alphaStart;
        }

        particle++;
    }

    if(touchInfluence > 0) {
        touchInfluence-=0.01;
    }
    return 35;
}