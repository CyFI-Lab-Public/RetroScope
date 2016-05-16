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

#pragma version(1)
#pragma rs java_package_name(com.android.ex.carousel);
#pragma rs set_reflect_license()

#include "rs_graphics.rsh"

typedef struct __attribute__((aligned(4))) Card {
    // *** Update initCard if you add/remove fields here.
    rs_allocation texture; // basic card texture
    rs_allocation detailTexture; // screen-aligned detail texture
    float2 detailTextureOffset; // offset to add, in screen coordinates
    float2 detailLineOffset; // offset to add to detail line, in screen coordinates
    float2 detailTexturePosition[2]; // screen coordinates of detail texture, computed at draw time
    rs_mesh geometry;
    rs_matrix4x4 matrix; // custom transform for this card/geometry
    int textureState;  // whether or not the primary card texture is loaded.
    int detailTextureState; // whether or not the detail for the card is loaded.
    int geometryState; // whether or not geometry is loaded
    int cardVisible; // not bool because of packing bug?
    int detailVisible; // not bool because of packing bug?
    int shouldPrefetch; // not bool because of packing bug?
    int64_t textureTimeStamp; // time when this texture was last updated, in ms
    int64_t detailTextureTimeStamp; // time when this texture was last updated, in ms
    int64_t geometryTimeStamp; // time when the card itself was last updated, in ms
} Card_t;

typedef struct Ray_s {
    float3 position;
    float3 direction;
} Ray;

typedef struct Plane_s {
    float3 point;
    float3 normal;
    float constant;
} Plane;

typedef struct Cylinder_s {
    float3 center; // center of a y-axis-aligned infinite cylinder
    float radius;
} Cylinder;

typedef struct PerspectiveCamera_s {
    float3 from;
    float3 at;
    float3 up;
    float  fov;
    float  aspect;
    float  near;
    float  far;
} PerspectiveCamera;

typedef struct ProgramStore_s {
    rs_program_store programStore;
} ProgramStore_t;

typedef struct FragmentShaderConstants_s {
    float fadeAmount;
    float overallAlpha;
} FragmentShaderConstants;

// Request states. Used for loading 3D object properties from the Java client.
// Typical properties: texture, geometry and matrices.
enum {
    STATE_INVALID = 0, // item hasn't been loaded
    STATE_LOADING, // we've requested an item but are waiting for it to load
    STATE_STALE, // we have an old item, but should request an update
    STATE_UPDATING, // we've requested an update, and will display the old one in the meantime
    STATE_LOADED // item was delivered
};

// Interpolation modes ** THIS LIST MUST MATCH THOSE IN CarouselView.java ***
enum {
    INTERPOLATION_LINEAR = 0,
    INTERPOLATION_DECELERATE_QUADRATIC = 1,
    INTERPOLATION_ACCELERATE_DECELERATE_CUBIC = 2,
};

// Detail texture alignments ** THIS LIST MUST MATCH THOSE IN CarouselView.java ***
enum {
    /** Detail is centered vertically with respect to the card **/
    CENTER_VERTICAL = 1,
    /** Detail is aligned with the top edge of the carousel view **/
    VIEW_TOP = 1 << 1,
    /** Detail is aligned with the bottom edge of the carousel view (not yet implemented) **/
    VIEW_BOTTOM = 1 << 2,
    /** Detail is positioned above the card (not yet implemented) **/
    ABOVE = 1 << 3,
    /** Detail is positioned below the card **/
    BELOW = 1 << 4,
    /** Mask that selects those bits that control vertical alignment **/
    VERTICAL_ALIGNMENT_MASK = 0xff,

    /**
     * Detail is centered horizontally with respect to either the top or bottom
     * extent of the card, depending on whether the detail is above or below the card.
     */
    CENTER_HORIZONTAL = 1 << 8,
    /**
     * Detail is aligned with the left edge of either the top or the bottom of
     * the card, depending on whether the detail is above or below the card.
     */
    LEFT = 1 << 9,
    /**
     * Detail is aligned with the right edge of either the top or the bottom of
     * the card, depending on whether the detail is above or below the card.
     * (not yet implemented)
     */
    RIGHT = 1 << 10,
    /** Mask that selects those bits that control horizontal alignment **/
    HORIZONTAL_ALIGNMENT_MASK = 0xff00,
};

// Client messages *** THIS LIST MUST MATCH THOSE IN CarouselRS.java. ***
static const int CMD_CARD_SELECTED = 100;
static const int CMD_DETAIL_SELECTED = 105;
static const int CMD_CARD_LONGPRESS = 110;
static const int CMD_REQUEST_TEXTURE = 200;
static const int CMD_INVALIDATE_TEXTURE = 210;
static const int CMD_REQUEST_GEOMETRY = 300;
static const int CMD_INVALIDATE_GEOMETRY = 310;
static const int CMD_ANIMATION_STARTED = 400;
static const int CMD_ANIMATION_FINISHED = 500;
static const int CMD_REQUEST_DETAIL_TEXTURE = 600;
static const int CMD_INVALIDATE_DETAIL_TEXTURE = 610;
static const int CMD_PING = 1000;

// Drag model *** THIS LIST MUST MATCH THOSE IN CarouselRS.java. ***
static const int DRAG_MODEL_SCREEN_DELTA = 0; // Drag relative to x coordinate of motion vector
static const int DRAG_MODEL_PLANE = 1; // Drag relative to projected point on plane of carousel
static const int DRAG_MODEL_CYLINDER_INSIDE = 2; // Drag relative to point on inside of cylinder
static const int DRAG_MODEL_CYLINDER_OUTSIDE = 3; // Drag relative to point on outside of cylinder

// Constants
static const int ANIMATION_DELAY_TIME = 125; // hold off scale animation until this time
static const int ANIMATION_SCALE_UP_TIME = 200; // Time it takes to animate selected card, in ms
static const int ANIMATION_SCALE_DOWN_TIME = 200; // Time it takes to animate selected card, in ms
static const float3 SELECTED_SCALE_FACTOR = { 0.1f, 0.1f, 0.1f }; // increase by this %
static const int VELOCITY_HISTORY_MAX = 10; // # recent velocity samples used to calculate average
static const int VISIBLE_SLOT_PADDING = 2;  // # slots to draw on either side of visible slots

// Constants affecting tilt overscroll.  Some of these should be parameters.
static const int TILT_SLOT_NUMBER = 5;
static const float TILT_MIN_ANGLE = M_PI / 315.0f;
static const float TILT_MAX_BIAS = M_PI / 8.0f;
static const float TILT_MAX_ANGLE = M_PI / 8.0f;
static const float MAX_DELTA_BIAS = 0.008f;

// Debug flags
const bool debugCamera = false; // dumps ray/camera coordinate stuff
const bool debugSelection = false; // logs selection events
const bool debugTextureLoading = false; // for debugging texture load/unload
const bool debugGeometryLoading = false; // for debugging geometry load/unload
const bool debugDetails = false; // for debugging detail texture geometry
const bool debugRendering = false; // flashes display when the frame changes
const bool debugRays = false; // shows visual depiction of hit tests, See renderWithRays().

// Exported variables. These will be reflected to Java set_* variables.
Card_t *cards; // array of cards to draw
float startAngle; // position of initial card, in radians
int slotCount; // number of positions where a card can be
int cardCount; // number of cards in stack
int programStoresCardCount; // number of program fragment stores
int visibleSlotCount; // number of visible slots (for culling)
int visibleDetailCount; // number of visible detail textures to show
int prefetchCardCount; // how many cards to keep in memory
int detailTextureAlignment; // How to align detail texture with respect to card
bool drawRuler; // whether to draw a ruler from the card to the detail texture
float radius; // carousel radius. Cards will be centered on a circle with this radius
float cardRotation; // rotation of card in XY plane relative to Z=1
bool cardsFaceTangent; // whether cards are rotated to face along a tangent to the circle
float swaySensitivity; // how much to rotate cards in relation to the rotation velocity
float frictionCoeff; // how much to slow down the carousel over time
float dragFactor; // a scale factor for how sensitive the carousel is to user dragging
int fadeInDuration; // amount of time (in ms) for smoothly switching out textures
int cardCreationFadeDuration; // amount of time (in ms) to fade while initially showing a card
float rezInCardCount; // this controls how rapidly distant card textures will be rez-ed in
float detailFadeRate; // rate at which details fade as they move into the distance
float4 backgroundColor;
int rowCount;  // number of rows of cards in a given slot, default 1
float rowSpacing;  // spacing between rows of cards
bool firstCardTop; // set true for first card on top row when multiple rows used
float overscrollSlots; // amount of allowed overscroll (in slots)

int dragModel = DRAG_MODEL_SCREEN_DELTA;
int fillDirection; // the order in which to lay out cards: +1 for CCW (default), -1 for CW
ProgramStore_t *programStoresCard;
rs_program_store programStoreBackground;
rs_program_store programStoreDetail;
rs_program_fragment singleTextureFragmentProgram;
rs_program_fragment singleTextureBlendingFragmentProgram;
rs_program_fragment multiTextureFragmentProgram;
rs_program_fragment multiTextureBlendingFragmentProgram;
rs_program_vertex vertexProgram;
rs_program_raster rasterProgram;
rs_allocation defaultTexture; // shown when no other texture is assigned
rs_allocation loadingTexture; // progress texture (shown when app is fetching the texture)
rs_allocation backgroundTexture; // drawn behind everything, if set
rs_allocation detailLineTexture; // used to draw detail line (as a quad, of course)
rs_allocation detailLoadingTexture; // used when detail texture is loading
rs_mesh defaultGeometry; // shown when no geometry is loaded
rs_mesh loadingGeometry; // shown when geometry is loading
rs_matrix4x4 defaultCardMatrix;
rs_matrix4x4 projectionMatrix;
rs_matrix4x4 modelviewMatrix;
FragmentShaderConstants* shaderConstants;
rs_sampler linearClamp;

// Local variables
static float bias; // rotation bias, in radians. Used for animation and dragging.
static float overscrollBias; // Track overscroll bias separately for tilt effect.
static bool updateCamera;    // force a recompute of projection and lookat matrices
static const float FLT_MAX = 1.0e37;
static int animatedSelection = -1;
static int currentFirstCard = -1;
static int64_t touchTime = -1; // time of first touch (see doStart())
static int64_t releaseTime = 0L; // when touch was released
static float touchBias = 0.0f; // bias on first touch
static float2 touchPosition; // position of first touch, as defined by last call to doStart(x,y)
static float velocity = 0.0f;  // angular velocity in radians/s
static bool isOverScrolling = false; // whether we're in the overscroll animation
static bool isAutoScrolling = false; // whether we're in the autoscroll animation
static bool isDragging = false; // true while the user is dragging the carousel
static float selectionRadius = 50.0f; // movement greater than this will result in no selection
static bool enableSelection = false; // enabled until the user drags outside of selectionRadius
static float tiltAngle = 0.0f;

// Default plane of the carousel. Used for angular motion estimation in view.
static Plane carouselPlane = {
       { 0.0f, 0.0f, 0.0f }, // point
       { 0.0f, 1.0f, 0.0f }, // normal
       0.0f // plane constant (= -dot(P, N))
};

static Cylinder carouselCylinder = {
        {0.0f, 0.0f, 0.0f }, // center
        1.0f // radius - update with carousel radius.
};

// Because allocations can't have 0 dimensions, we have to track whether or not
// cards and program stores are valid separately.
// TODO: Remove this dependency once allocations can have a zero dimension.
static bool cardAllocationValid = false;
static bool programStoresAllocationValid = false;

// Default geometry when card.geometry is not set.
static const float3 cardVertices[4] = {
        { -1.0, -1.0, 0.0 },
        { 1.0, -1.0, 0.0 },
        { 1.0, 1.0, 0.0 },
        {-1.0, 1.0, 0.0 }
};

// Default camera
static PerspectiveCamera camera = {
        {2,2,2}, // from
        {0,0,0}, // at
        {0,1,0}, // up
        25.0f,   // field of view
        1.0f,    // aspect
        0.1f,    // near
        100.0f   // far
};

// Forward references
static int intersectGeometry(Ray* ray, float *bestTime);
static int intersectDetailTexture(float x, float y, float2 *tapCoordinates);
static bool __attribute__((overloadable))
        makeRayForPixelAt(Ray* ray, PerspectiveCamera* cam, float x, float y);
static bool __attribute__((overloadable))
        makeRayForPixelAt(Ray* ray, rs_matrix4x4* model, rs_matrix4x4* proj, float x, float y);
static float deltaTimeInSeconds(int64_t current);
static bool rayPlaneIntersect(Ray* ray, Plane* plane, float* tout);
static bool rayCylinderIntersect(Ray* ray, Cylinder* cylinder, float* tout);
static void stopAutoscroll();
static bool tiltOverscroll();

void init() {
    // initializers currently have a problem when the variables are exported, so initialize
    // globals here.
    if (debugTextureLoading) rsDebug("Renderscript: init()", 0);
    startAngle = 0.0f;
    slotCount = 10;
    visibleSlotCount = 1;
    visibleDetailCount = 3;
    bias = 0.0f;
    overscrollBias = 0.0f;
    tiltAngle = 0.0f;
    radius = carouselCylinder.radius = 1.0f;
    cardRotation = 0.0f;
    cardsFaceTangent = false;
    updateCamera = true;
    backgroundColor = (float4) { 0.0f, 0.0f, 0.0f, 1.0f };
    cardAllocationValid = false;
    programStoresAllocationValid = false;
    cardCount = 0;
    rowCount = 1;
    rowSpacing = 0.0f;
    firstCardTop = false;
    fadeInDuration = 250;
    rezInCardCount = 0.0f; // alpha will ramp to 1.0f over this many cards (0.0f means disabled)
    detailFadeRate = 0.5f; // fade details over this many slot positions.
    rsMatrixLoadIdentity(&defaultCardMatrix);
}

static void updateAllocationVars()
{
    // Cards
    rs_allocation cardAlloc;
    cardAlloc = rsGetAllocation(cards);
    cardCount = (cardAllocationValid && rsIsObject(cardAlloc)) ? rsAllocationGetDimX(cardAlloc) : 0;

    // Program stores
    rs_allocation psAlloc;
    psAlloc = rsGetAllocation(programStoresCard);
    programStoresCardCount = (programStoresAllocationValid && rsIsObject(psAlloc) ?
        rsAllocationGetDimX(psAlloc) : 0);
}

void setRadius(float rad)
{
    radius = carouselCylinder.radius = rad;
}

static void initCard(Card_t* card)
{
    // Object refs are always initilized cleared.
    static const float2 zero = {0.0f, 0.0f};
    card->detailTextureOffset = zero;
    card->detailLineOffset = zero;
    rsMatrixLoad(&card->matrix, &defaultCardMatrix);
    card->textureState = STATE_INVALID;
    card->detailTextureState = STATE_INVALID;
    card->geometryState = STATE_INVALID;
    card->cardVisible = false;
    card->detailVisible = false;
    card->shouldPrefetch = false;
    card->textureTimeStamp = 0;
    card->detailTextureTimeStamp = 0;
    card->geometryTimeStamp = rsUptimeMillis();
}

void createCards(int start, int total)
{
    if (!cardAllocationValid) {
        // If the allocation is invalid, it contains a single place-holder
        // card that has not yet been initialized (see CarouselRS.createCards).
        // Here we ensure that it is initialized when growing the total.
        start = 0;
    }
    for (int k = start; k < total; k++) {
        initCard(cards + k);
    }

    // Since allocations can't have 0-size, we track validity ourselves based on the call to
    // this method.
    cardAllocationValid = total > 0;

    updateAllocationVars();
}

// Computes an alpha value for a card using elapsed time and constant fadeInDuration
static float getAnimatedAlpha(int64_t startTime, int64_t currentTime, int64_t duration)
{
    double timeElapsed = (double) (currentTime - startTime); // in ms
    double alpha = duration > 0 ? (double) timeElapsed / duration : 1.0;
    return min(1.0f, (float) alpha);
}

// Returns total angle for given number of slots
static float wedgeAngle(float slots)
{
    return slots * 2.0f * M_PI / slotCount;
}

// Return angle of slot in position p.
static float slotPosition(int p)
{
    return startAngle + wedgeAngle(p) * fillDirection;
}

// Return angle for card in position p.
static float cardPosition(int p)
{
    return bias + slotPosition(p / rowCount);
}

// Return the lowest possible bias value, based on the fill direction
static float minimumBias()
{
    const int totalSlots = (cardCount + rowCount - 1) / rowCount;
    return (fillDirection > 0) ?
        -max(0.0f, wedgeAngle(totalSlots - visibleDetailCount)) :
        wedgeAngle(0.0f);
}

// Return the highest possible bias value, based on the fill direction
static float maximumBias()
{
    const int totalSlots = (cardCount + rowCount - 1) / rowCount;
    return (fillDirection > 0) ?
        wedgeAngle(0.0f) :
        max(0.0f, wedgeAngle(totalSlots - visibleDetailCount));
}


// convert from carousel rotation angle (in card slot units) to radians.
static float carouselRotationAngleToRadians(float carouselRotationAngle)
{
    return -wedgeAngle(carouselRotationAngle);
}

// convert from radians to carousel rotation angle (in card slot units).
static float radiansToCarouselRotationAngle(float angle)
{
    return -angle * slotCount / ( 2.0f * M_PI );
}

// Set basic camera properties:
//    from - position of the camera in x,y,z
//    at - target we're looking at - used to compute view direction
//    up - a normalized vector indicating up (typically { 0, 1, 0})
//
// NOTE: the view direction and up vector cannot be parallel/antiparallel with each other
void lookAt(float fromX, float fromY, float fromZ,
        float atX, float atY, float atZ,
        float upX, float upY, float upZ)
{
    camera.from.x = fromX;
    camera.from.y = fromY;
    camera.from.z = fromZ;
    camera.at.x = atX;
    camera.at.y = atY;
    camera.at.z = atZ;
    camera.up.x = upX;
    camera.up.y = upY;
    camera.up.z = upZ;
    updateCamera = true;
}

// Load a projection matrix for the given parameters.  This is equivalent to gluPerspective()
static void loadPerspectiveMatrix(rs_matrix4x4* matrix, float fovy, float aspect, float near, float far)
{
    rsMatrixLoadIdentity(matrix);
    float top = near * tan((float) (fovy * M_PI / 360.0f));
    float bottom = -top;
    float left = bottom * aspect;
    float right = top * aspect;
    rsMatrixLoadFrustum(matrix, left, right, bottom, top, near, far);
}

// Construct a matrix based on eye point, center and up direction. Based on the
// man page for gluLookat(). Up must be normalized.
static void loadLookatMatrix(rs_matrix4x4* matrix, float3 eye, float3 center, float3 up)
{
    float3 f = normalize(center - eye);
    float3 s = normalize(cross(f, up));
    float3 u = cross(s, f);
    float m[16];
    m[0] = s.x;
    m[4] = s.y;
    m[8] = s.z;
    m[12] = 0.0f;
    m[1] = u.x;
    m[5] = u.y;
    m[9] = u.z;
    m[13] = 0.0f;
    m[2] = -f.x;
    m[6] = -f.y;
    m[10] = -f.z;
    m[14] = 0.0f;
    m[3] = m[7] = m[11] = 0.0f;
    m[15] = 1.0f;
    rsMatrixLoad(matrix, m);
    rsMatrixTranslate(matrix, -eye.x, -eye.y, -eye.z);
}

/*
 * Returns true if a state represents a texture that is loaded enough to draw
 */
static bool textureEverLoaded(int state) {
    return (state == STATE_LOADED) || (state == STATE_STALE) || (state == STATE_UPDATING);
}

void setTexture(int n, rs_allocation texture)
{
    if (n < 0 || n >= cardCount) return;
    cards[n].texture = texture;
    if (cards[n].textureState != STATE_STALE &&
        cards[n].textureState != STATE_UPDATING) {
        cards[n].textureTimeStamp = rsUptimeMillis();
    }
    cards[n].textureState = (texture.p != 0) ? STATE_LOADED : STATE_INVALID;
}

void setDetailTexture(int n, float offx, float offy, float loffx, float loffy, rs_allocation texture)
{
    if (n < 0 || n >= cardCount) return;
    cards[n].detailTexture = texture;
    if (cards[n].detailTextureState != STATE_STALE &&
        cards[n].detailTextureState != STATE_UPDATING) {
        cards[n].detailTextureTimeStamp = rsUptimeMillis();
    }
    cards[n].detailTextureOffset.x = offx;
    cards[n].detailTextureOffset.y = offy;
    cards[n].detailLineOffset.x = loffx;
    cards[n].detailLineOffset.y = loffy;
    cards[n].detailTextureState = (texture.p != 0) ? STATE_LOADED : STATE_INVALID;
}

void invalidateTexture(int n, bool eraseCurrent)
{
    if (n < 0 || n >= cardCount) return;
    if (eraseCurrent) {
        cards[n].textureState = STATE_INVALID;
        rsClearObject(&cards[n].texture);
    } else {
        cards[n].textureState =
            textureEverLoaded(cards[n].textureState) ? STATE_STALE : STATE_INVALID;
    }
}

void invalidateDetailTexture(int n, bool eraseCurrent)
{
    if (n < 0 || n >= cardCount) return;
    if (eraseCurrent) {
        cards[n].detailTextureState = STATE_INVALID;
        rsClearObject(&cards[n].detailTexture);
    } else {
        cards[n].detailTextureState =
            textureEverLoaded(cards[n].detailTextureState) ? STATE_STALE : STATE_INVALID;
    }
}

void setGeometry(int n, rs_mesh geometry)
{
    if (n < 0 || n >= cardCount) return;
    cards[n].geometry = geometry;
    if (cards[n].geometry.p != 0)
        cards[n].geometryState = STATE_LOADED;
    else
        cards[n].geometryState = STATE_INVALID;
    cards[n].geometryTimeStamp = rsUptimeMillis();
}

void setMatrix(int n, rs_matrix4x4 matrix) {
    if (n < 0 || n >= cardCount) return;
    cards[n].matrix = matrix;
}

void setProgramStoresCard(int n, rs_program_store programStore)
{
    programStoresCard[n].programStore = programStore;
    programStoresAllocationValid = true;
}

void setCarouselRotationAngle(float carouselRotationAngle) {
    bias = carouselRotationAngleToRadians(carouselRotationAngle);
}

// Gets animated scale value for current selected card.
// If card is currently being animated, returns true,  otherwise returns false.
static bool getAnimatedScaleForSelected(float3* scale)
{
    static const float3 one = { 1.0f, 1.0f, 1.0f };
    static float fraction = 0.0f;
    bool stillAnimating = false;
    if (isDragging) {
        // "scale up" animation
        int64_t dt = rsUptimeMillis() - touchTime - ANIMATION_DELAY_TIME;
        if (dt > 0L && enableSelection) {
            float s = (float) dt / ANIMATION_SCALE_UP_TIME;
            s = min(s, 1.0f);
            fraction = max(s, fraction);
        }
        stillAnimating = dt < ANIMATION_SCALE_UP_TIME;
    } else {
        // "scale down" animation
        int64_t dt = rsUptimeMillis() - releaseTime;
        if (dt < ANIMATION_SCALE_DOWN_TIME) {
            float s = 1.0f - ((float) dt / ANIMATION_SCALE_DOWN_TIME);
            fraction = min(s, fraction);
            stillAnimating = true;
        } else {
            fraction = 0.0f;
        }
    }
    *scale = one + fraction * SELECTED_SCALE_FACTOR;
    return stillAnimating; // still animating;
}

// The Verhulst logistic function: http://en.wikipedia.org/wiki/Logistic_function
//    P(t) = 1 / (1 + e^(-t))
// Parameter t: Any real number
// Returns: A float in the range (0,1), with P(0.5)=0
static float logistic(float t) {
    return 1.f / (1.f + exp(-t));
}

static float getSwayAngleForVelocity(float v, bool enableSway)
{
    float sway = 0.0f;

    if (enableSway) {
        const float range = M_PI * 2./3.; // How far we can deviate from center, peak-to-peak
        sway = range * (logistic(-v * swaySensitivity) - 0.5f);
    }

    return sway;
}

static float getCardTiltAngle(int i) {
    i /= rowCount;
    int totalSlots = (cardCount + rowCount - 1) / rowCount;
    float tiltSlotNumber = TILT_SLOT_NUMBER;
    float deltaTilt = tiltAngle / tiltSlotNumber;
    float cardTiltAngle = 0;
    if (tiltAngle > 0 && i < tiltSlotNumber) {
        // Overscroll for the front cards.
        cardTiltAngle = deltaTilt * (tiltSlotNumber - i);
    } else if (tiltAngle < 0 && i > (totalSlots - tiltSlotNumber)) {
        cardTiltAngle = deltaTilt * (i - totalSlots + tiltSlotNumber + 1);
    }
    return cardTiltAngle;
}

// Returns the vertical offset for a card in its slot,
// depending on the number of rows configured.
static float getVerticalOffsetForCard(int i) {
   if (rowCount == 1) {
       // fast path
       return 0;
   }
   const float cardHeight = (cardVertices[3].y - cardVertices[0].y) *
      rsMatrixGet(&defaultCardMatrix, 1, 1);
   const float totalHeight = rowCount * (cardHeight + rowSpacing) - rowSpacing;
   if (firstCardTop)
      i = rowCount - (i % rowCount) - 1;
   else
      i = i % rowCount;
   const float rowOffset = i * (cardHeight + rowSpacing);
   return (cardHeight - totalHeight) / 2 + rowOffset;
}

/*
 * Composes a matrix for the given card.
 * matrix: The output matrix.
 * i: The card we're getting the matrix for.
 * enableSway: Whether to enable swaying. (We want it on for cards, and off for detail textures.)
 * enableCardMatrix: Whether to also consider the user-specified card matrix
 *
 * returns true if an animation is being applied to the given card
 */
static bool getMatrixForCard(rs_matrix4x4* matrix, int i, bool enableSway, bool enableCardMatrix)
{
    float theta = cardPosition(i);
    float swayAngle = getSwayAngleForVelocity(velocity, enableSway);
    rsMatrixRotate(matrix, degrees(theta), 0, 1, 0);
    rsMatrixTranslate(matrix, radius, getVerticalOffsetForCard(i), 0);
    float tiltAngle = getCardTiltAngle(i);
    float rotation = cardRotation + swayAngle + tiltAngle;
    if (!cardsFaceTangent) {
      rotation -= theta;
    }
    rsMatrixRotate(matrix, degrees(rotation), 0, 1, 0);
    bool stillAnimating = false;
    if (i == animatedSelection) {
        float3 scale;
        stillAnimating = getAnimatedScaleForSelected(&scale);
        rsMatrixScale(matrix, scale.x, scale.y, scale.z);
    }
    // TODO(jshuma): Instead of ignoring this matrix for the detail texture, use card bounding box
    if (enableCardMatrix) {
        rsMatrixLoadMultiply(matrix, matrix, &cards[i].matrix);
    }
    return stillAnimating;
}

/*
 * Draws the requested mesh, with the appropriate program store in effect.
 */
static void drawMesh(rs_mesh mesh)
{
    if (programStoresCardCount == 1) {
        // Draw the entire mesh, with the only available program store
        rsgBindProgramStore(programStoresCard[0].programStore);
        rsgDrawMesh(mesh);
    } else {
        // Draw each primitive in the mesh with the corresponding program store
        for (int i=0; i<programStoresCardCount; ++i) {
            if (programStoresCard[i].programStore.p != 0) {
                rsgBindProgramStore(programStoresCard[i].programStore);
                rsgDrawMesh(mesh, i);
            }
        }
    }
}

/*
 * Draws cards around the Carousel.
 * Returns true if we're still animating any property of the cards (e.g. fades).
 */
static bool drawCards(int64_t currentTime)
{
    const float wedgeAngle = 2.0f * M_PI / slotCount;
    const float endAngle = startAngle + visibleSlotCount * wedgeAngle;
    bool stillAnimating = false;
    for (int i = cardCount-1; i >= 0; i--) {
        if (cards[i].cardVisible) {
            // If this card was recently loaded, this will be < 1.0f until the animation completes
            float animatedAlpha = getAnimatedAlpha(cards[i].textureTimeStamp, currentTime,
                fadeInDuration);
            float overallAlpha = getAnimatedAlpha(cards[i].geometryTimeStamp, currentTime,
                cardCreationFadeDuration);
            if (animatedAlpha < 1.0f || overallAlpha < 1.0f) {
                stillAnimating = true;
            }

            // Compute fade out for cards in the distance
            float positionAlpha;
            if (rezInCardCount > 0.0f) {
                positionAlpha = (endAngle - cardPosition(i)) / wedgeAngle;
                positionAlpha = min(1.0f, positionAlpha / rezInCardCount);
            } else {
                positionAlpha = 1.0f;
            }

            // Set alpha for blending between the textures
            shaderConstants->fadeAmount = min(1.0f, animatedAlpha * positionAlpha);
            shaderConstants->overallAlpha = overallAlpha;
            rsgAllocationSyncAll(rsGetAllocation(shaderConstants));

            // Bind the appropriate shader network.  If there's no alpha blend, then
            // switch to single shader for better performance.
            const int state = cards[i].textureState;
            bool loaded = textureEverLoaded(state) && rsIsObject(cards[i].texture);
            if (shaderConstants->fadeAmount == 1.0f || shaderConstants->fadeAmount < 0.01f) {
                if (overallAlpha < 1.0) {
                    rsgBindProgramFragment(singleTextureBlendingFragmentProgram);
                    rsgBindTexture(singleTextureBlendingFragmentProgram, 0,
                            (loaded && shaderConstants->fadeAmount == 1.0f) ?
                            cards[i].texture : loadingTexture);
                } else {
                    rsgBindProgramFragment(singleTextureFragmentProgram);
                    rsgBindTexture(singleTextureFragmentProgram, 0,
                            (loaded && shaderConstants->fadeAmount == 1.0f) ?
                            cards[i].texture : loadingTexture);
                }
            } else {
                if (overallAlpha < 1.0) {
                    rsgBindProgramFragment(multiTextureBlendingFragmentProgram);
                    rsgBindTexture(multiTextureBlendingFragmentProgram, 0, loadingTexture);
                    rsgBindTexture(multiTextureBlendingFragmentProgram, 1, loaded ?
                            cards[i].texture : loadingTexture);
                } else {
                    rsgBindProgramFragment(multiTextureFragmentProgram);
                    rsgBindTexture(multiTextureFragmentProgram, 0, loadingTexture);
                    rsgBindTexture(multiTextureFragmentProgram, 1, loaded ?
                            cards[i].texture : loadingTexture);
                }
            }

            // Draw geometry
            rs_matrix4x4 matrix = modelviewMatrix;
            stillAnimating |= getMatrixForCard(&matrix, i, true, true);
            rsgProgramVertexLoadModelMatrix(&matrix);
            if (cards[i].geometryState == STATE_LOADED && cards[i].geometry.p != 0) {
                drawMesh(cards[i].geometry);
            } else if (cards[i].geometryState == STATE_LOADING && loadingGeometry.p != 0) {
                drawMesh(loadingGeometry);
            } else if (defaultGeometry.p != 0) {
                drawMesh(defaultGeometry);
            } else {
                // Draw place-holder geometry
                rsgBindProgramStore(programStoresCard[0].programStore);
                rsgDrawQuad(
                    cardVertices[0].x, cardVertices[0].y, cardVertices[0].z,
                    cardVertices[1].x, cardVertices[1].y, cardVertices[1].z,
                    cardVertices[2].x, cardVertices[2].y, cardVertices[2].z,
                    cardVertices[3].x, cardVertices[3].y, cardVertices[3].z);
            }
        }
    }
    return stillAnimating;
}

/**
 * Convert projection from normalized coordinates to pixel coordinates.
 *
 * @return True on success, false on failure.
 */
static bool convertNormalizedToPixelCoordinates(float4 *screenCoord, float width, float height) {
    // This is probably cheaper than pre-multiplying with another matrix.
    if (screenCoord->w == 0.0f) {
        rsDebug("Bad transform while converting from normalized to pixel coordinates: ",
            screenCoord);
        return false;
    }
    *screenCoord *= 1.0f / screenCoord->w;
    screenCoord->x += 1.0f;
    screenCoord->y += 1.0f;
    screenCoord->z += 1.0f;
    screenCoord->x = round(screenCoord->x * 0.5f * width);
    screenCoord->y = round(screenCoord->y * 0.5f * height);
    screenCoord->z = - 0.5f * screenCoord->z;
    return true;
}

/*
 * Draws a screen-aligned card with the exact dimensions from the detail texture.
 * This is used to display information about the object being displayed.
 * Returns true if we're still animating any property of the cards (e.g. fades).
 */
static bool drawDetails(int64_t currentTime)
{
    const float width = rsgGetWidth();
    const float height = rsgGetHeight();

    bool stillAnimating = false;

    // We'll be drawing in screen space, sampled on pixel centers
    rs_matrix4x4 projection, model;
    rsMatrixLoadOrtho(&projection, 0.0f, width, 0.0f, height, 0.0f, 1.0f);
    rsgProgramVertexLoadProjectionMatrix(&projection);
    rsMatrixLoadIdentity(&model);
    rsgProgramVertexLoadModelMatrix(&model);
    updateCamera = true; // we messed with the projection matrix. Reload on next pass...

    const float yPadding = 5.0f; // draw line this far (in pixels) away from top and geometry

    // This can be done once...
    rsgBindTexture(multiTextureFragmentProgram, 0, detailLoadingTexture);

    const float wedgeAngle = 2.0f * M_PI / slotCount;
    // Angle where details start fading from 1.0f
    const float startDetailFadeAngle = startAngle + (visibleDetailCount - 1) * wedgeAngle;
    // Angle where detail alpha is 0.0f
    const float endDetailFadeAngle = startDetailFadeAngle + detailFadeRate * wedgeAngle;

    for (int i = cardCount-1; i >= 0; --i) {
        if (cards[i].cardVisible) {
            const int state = cards[i].detailTextureState;
            const bool isLoaded = textureEverLoaded(state);
            if (isLoaded && cards[i].detailTexture.p != 0) {
                const float lineWidth = rsAllocationGetDimX(detailLineTexture);

                // Compute position in screen space of top corner or bottom corner of card
                rsMatrixLoad(&model, &modelviewMatrix);
                stillAnimating |= getMatrixForCard(&model, i, false, false);
                rs_matrix4x4 matrix;
                rsMatrixLoadMultiply(&matrix, &projectionMatrix, &model);

                int indexLeft, indexRight;
                float4 screenCoord;
                if (detailTextureAlignment & BELOW) {
                    indexLeft = 0;
                    indexRight = 1;
                } else {
                    indexLeft = 3;
                    indexRight = 2;
                }
                float4 screenCoordLeft = rsMatrixMultiply(&matrix, cardVertices[indexLeft]);
                float4 screenCoordRight = rsMatrixMultiply(&matrix, cardVertices[indexRight]);
                if (screenCoordLeft.w == 0.0f || screenCoordRight.w == 0.0f) {
                    // this shouldn't happen
                    rsDebug("Bad transform: ", screenCoord);
                    continue;
                }
                if (detailTextureAlignment & CENTER_VERTICAL) {
                    // If we're centering vertically, we'll need the other vertices too
                    if (detailTextureAlignment & BELOW) {
                        indexLeft = 3;
                        indexRight = 2;
                    } else {
                        indexLeft = 0;
                        indexRight = 1;
                    }
                    float4 otherScreenLeft = rsMatrixMultiply(&matrix, cardVertices[indexLeft]);
                    float4 otherScreenRight = rsMatrixMultiply(&matrix, cardVertices[indexRight]);
                    screenCoordRight.y = screenCoordLeft.y = (screenCoordLeft.y + screenCoordRight.y
                        + otherScreenLeft.y + otherScreenRight.y) / 4.;
                }
                (void) convertNormalizedToPixelCoordinates(&screenCoordLeft, width, height);
                (void) convertNormalizedToPixelCoordinates(&screenCoordRight, width, height);
                if (debugDetails) {
                    RS_DEBUG(screenCoordLeft);
                    RS_DEBUG(screenCoordRight);
                }
                screenCoord = screenCoordLeft;
                if (detailTextureAlignment & BELOW) {
                    screenCoord.y = min(screenCoordLeft.y, screenCoordRight.y);
                } else if (detailTextureAlignment & CENTER_VERTICAL) {
                    screenCoord.y -= round(rsAllocationGetDimY(cards[i].detailTexture) / 2.0f);
                }
                if (detailTextureAlignment & CENTER_HORIZONTAL) {
                    screenCoord.x += round((screenCoordRight.x - screenCoordLeft.x) / 2.0f -
                        rsAllocationGetDimX(cards[i].detailTexture) / 2.0f);
                }

                // Compute alpha for gradually fading in details. Applied to both line and
                // detail texture. TODO: use a separate background texture for line.
                float animatedAlpha = getAnimatedAlpha(cards[i].detailTextureTimeStamp,
                    currentTime, fadeInDuration);
                if (animatedAlpha < 1.0f) {
                    stillAnimating = true;
                }

                // Compute alpha based on position. We fade cards quickly so they cannot overlap
                float positionAlpha = ((float)endDetailFadeAngle - cardPosition(i))
                        / (endDetailFadeAngle - startDetailFadeAngle);
                positionAlpha = max(0.0f, positionAlpha);
                positionAlpha = min(1.0f, positionAlpha);

                const float blendedAlpha = min(1.0f, animatedAlpha * positionAlpha);

                if (blendedAlpha == 0.0f) {
                    cards[i].detailVisible = false;
                    continue; // nothing to draw
                } else {
                    cards[i].detailVisible = true;
                }
                if (blendedAlpha == 1.0f) {
                    rsgBindProgramFragment(singleTextureFragmentProgram);
                } else {
                    rsgBindProgramFragment(multiTextureFragmentProgram);
                }

                // Set alpha for blending between the textures
                shaderConstants->fadeAmount = blendedAlpha;
                rsgAllocationSyncAll(rsGetAllocation(shaderConstants));

                // Draw line from the card to the detail texture.
                // The line is drawn from the top or bottom left of the card
                // to either the top of the screen or the top of the detail
                // texture, depending on detailTextureAlignment.
                if (drawRuler) {
                    float rulerTop;
                    float rulerBottom;
                    if (detailTextureAlignment & BELOW) {
                        rulerTop = screenCoord.y;
                        rulerBottom = 0;
                    } else {
                        rulerTop = height;
                        rulerBottom = screenCoord.y;
                    }
                    const float halfWidth = lineWidth * 0.5f;
                    const float x0 = trunc(cards[i].detailLineOffset.x + screenCoord.x - halfWidth);
                    const float x1 = x0 + lineWidth;
                    const float y0 = rulerBottom + yPadding;
                    const float y1 = rulerTop - yPadding - cards[i].detailLineOffset.y;

                    if (blendedAlpha == 1.0f) {
                        rsgBindTexture(singleTextureFragmentProgram, 0, detailLineTexture);
                    } else {
                        rsgBindTexture(multiTextureFragmentProgram, 1, detailLineTexture);
                    }
                    rsgDrawQuad(x0, y0, screenCoord.z,  x1, y0, screenCoord.z,
                            x1, y1, screenCoord.z,  x0, y1, screenCoord.z);
                }

                // Draw the detail texture next to it using the offsets provided.
                const float textureWidth = rsAllocationGetDimX(cards[i].detailTexture);
                const float textureHeight = rsAllocationGetDimY(cards[i].detailTexture);
                const float offx = cards[i].detailTextureOffset.x;
                const float offy = -cards[i].detailTextureOffset.y;
                const float textureTop = (detailTextureAlignment & VIEW_TOP)
                        ? height : screenCoord.y;
                const float x0 = cards[i].detailLineOffset.x + screenCoord.x + offx;
                const float x1 = cards[i].detailLineOffset.x + screenCoord.x + offx + textureWidth;
                const float y0 = textureTop + offy - textureHeight - cards[i].detailLineOffset.y;
                const float y1 = textureTop + offy - cards[i].detailLineOffset.y;
                cards[i].detailTexturePosition[0].x = x0;
                cards[i].detailTexturePosition[0].y = height - y1;
                cards[i].detailTexturePosition[1].x = x1;
                cards[i].detailTexturePosition[1].y = height - y0;

                if (blendedAlpha == 1.0f) {
                    rsgBindTexture(singleTextureFragmentProgram, 0, cards[i].detailTexture);
                } else {
                    rsgBindTexture(multiTextureFragmentProgram, 1, cards[i].detailTexture);
                }
                rsgDrawQuad(x0, y0, screenCoord.z,  x1, y0, screenCoord.z,
                        x1, y1, screenCoord.z,  x0, y1, screenCoord.z);
            }
        }
    }
    return stillAnimating;
}

static void drawBackground()
{
    static bool toggle;
    if (backgroundTexture.p != 0) {
        rsgClearDepth(1.0f);
        rs_matrix4x4 projection, model;
        rsMatrixLoadOrtho(&projection, -1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f);
        rsgProgramVertexLoadProjectionMatrix(&projection);
        rsMatrixLoadIdentity(&model);
        rsgProgramVertexLoadModelMatrix(&model);
        rsgBindTexture(singleTextureFragmentProgram, 0, backgroundTexture);
        float z = -0.9999f;
        rsgDrawQuad(
            cardVertices[0].x, cardVertices[0].y, z,
            cardVertices[1].x, cardVertices[1].y, z,
            cardVertices[2].x, cardVertices[2].y, z,
            cardVertices[3].x, cardVertices[3].y, z);
        updateCamera = true; // we mucked with the matrix.
    } else {
        rsgClearDepth(1.0f);
        if (debugRendering) { // for debugging - flash the screen so we know we're still rendering
            rsgClearColor(toggle ? backgroundColor.x : 1.0f,
                        toggle ? backgroundColor.y : 0.0f,
                        toggle ? backgroundColor.z : 0.0f,
                        backgroundColor.w);
            toggle = !toggle;
        } else {
           rsgClearColor(backgroundColor.x, backgroundColor.y, backgroundColor.z,
                   backgroundColor.w);
       }
    }
}

static void updateCameraMatrix(float width, float height)
{
    float aspect = width / height;
    if (aspect != camera.aspect || updateCamera) {
        camera.aspect = aspect;
        loadPerspectiveMatrix(&projectionMatrix, camera.fov, camera.aspect, camera.near, camera.far);
        rsgProgramVertexLoadProjectionMatrix(&projectionMatrix);

        loadLookatMatrix(&modelviewMatrix, camera.from, camera.at, camera.up);
        rsgProgramVertexLoadModelMatrix(&modelviewMatrix);
        updateCamera = false;
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Behavior/Physics
////////////////////////////////////////////////////////////////////////////////////////////////////
static int64_t lastTime = 0L; // keep track of how much time has passed between frames
static float lastAngle = 0.0f;
static float2 lastPosition;
static bool animating = false;
static float stopVelocity = 0.1f * M_PI / 180.0f; // slower than this: carousel stops
static float selectionVelocity = 15.0f * M_PI / 180.0f; // faster than this: tap won't select
static float velocityHistory[VELOCITY_HISTORY_MAX];
static int velocityHistoryCount;
static float mass = 5.0f; // kg

static const float G = 9.80f; // gravity constant, in m/s
static const float springConstant = 0.0f;

// Computes a hit angle from the center of the carousel to a point on either a plane
// or on a cylinder. If neither is hit, returns false.
static bool hitAngle(float x, float y, float *angle)
{
    Ray ray;
    makeRayForPixelAt(&ray, &camera, x, y);
    float t = FLT_MAX;
    if (dragModel == DRAG_MODEL_PLANE && rayPlaneIntersect(&ray, &carouselPlane, &t)) {
        const float3 point = (ray.position + t*ray.direction);
        const float3 direction = point - carouselPlane.point;
        *angle = atan2(direction.x, direction.z);
        if (debugSelection) rsDebug("Plane Angle = ", degrees(*angle));
        return true;
    } else if ((dragModel == DRAG_MODEL_CYLINDER_INSIDE || dragModel == DRAG_MODEL_CYLINDER_OUTSIDE)
            && rayCylinderIntersect(&ray, &carouselCylinder, &t)) {
        const float3 point = (ray.position + t*ray.direction);
        const float3 direction = point - carouselCylinder.center;
        *angle = atan2(direction.x, direction.z);
        if (debugSelection) rsDebug("Cylinder Angle = ", degrees(*angle));
        return true;
    }
    return false;
}

static float dragFunction(float x, float y)
{
    float result;
    float angle;
    if (hitAngle(x, y, &angle)) {
        result = angle - lastAngle;
        // Handle singularity where atan2 switches between +- PI
        if (result < -M_PI) {
            result += 2.0f * M_PI;
        } else if (result > M_PI) {
            result -= 2.0f * M_PI;
        }
        lastAngle = angle;
    } else {
        // If we didn't hit anything or drag model wasn't plane or cylinder, we use screen delta
        result = dragFactor * ((x - lastPosition.x) / rsgGetWidth()) * M_PI;
    }
    return result;
}

static float deltaTimeInSeconds(int64_t current)
{
    return (lastTime > 0L) ? (float) (current - lastTime) / 1000.0f : 0.0f;
}

static int doSelection(float x, float y)
{
    Ray ray;
    if (makeRayForPixelAt(&ray, &camera, x, y)) {
        float bestTime = FLT_MAX;
        return intersectGeometry(&ray, &bestTime);
    }
    return -1;
}

static void sendAnimationStarted() {
    rsSendToClient(CMD_ANIMATION_STARTED);
}

static void sendAnimationFinished() {
    float data[1];
    data[0] = radiansToCarouselRotationAngle(bias);
    rsSendToClient(CMD_ANIMATION_FINISHED, (int*) data, sizeof(data));
}

void doStart(float x, float y, long eventTime)
{
    touchPosition = lastPosition = (float2) { x, y };
    lastAngle = hitAngle(x,y, &lastAngle) ? lastAngle : 0.0f;
    enableSelection = fabs(velocity) < selectionVelocity;
    velocity = 0.0f;
    velocityHistory[0] = 0.0f;
    velocityHistoryCount = 0;

    releaseTime = lastTime; // used to disable scale down animation - any time in the past will do
    touchTime = lastTime = eventTime;
    touchBias = bias;
    isDragging = true;
    isOverScrolling = false;
    tiltAngle = 0;
    overscrollBias = bias;

    animatedSelection = doSelection(x, y); // used to provide visual feedback on touch
    stopAutoscroll();
}

static float computeAverageVelocityFromHistory()
{
    if (velocityHistoryCount > 0) {
        const int count = min(VELOCITY_HISTORY_MAX, velocityHistoryCount);
        float vsum = 0.0f;
        for (int i = 0; i < count; i++) {
            vsum += velocityHistory[i];
        }
        return vsum / count;
    } else {
        return 0.0f;
    }
}

void doStop(float x, float y, long eventTime)
{
    updateAllocationVars();

    releaseTime = rsUptimeMillis();

    if (enableSelection) {
        int data[3];
        int selection;
        float2 point;

        if ((selection = intersectDetailTexture(x, y, &point)) != -1) {
            if (debugSelection) rsDebug("Selected detail texture on doStop():", selection);
            data[0] = selection;
            data[1] = point.x;
            data[2] = point.y;
            rsSendToClientBlocking(CMD_DETAIL_SELECTED, data, sizeof(data));
        }
        else if ((selection = doSelection(x, y))!= -1) {
            if (debugSelection) rsDebug("Selected item on doStop():", selection);
            data[0] = selection;
            rsSendToClientBlocking(CMD_CARD_SELECTED, data, sizeof(data));
        }
        animating = false;
    } else {
        velocity = computeAverageVelocityFromHistory();
        if (fabs(velocity) > stopVelocity) {
            animating = true;
        }
    }
    enableSelection = false;
    lastTime = eventTime;
    isDragging = false;
}

void doLongPress()
{
    int64_t currentTime = rsUptimeMillis();
    updateAllocationVars();
    // Selection happens for most recent position detected in doMotion()
    if (enableSelection && animatedSelection != -1) {
        if (debugSelection) rsDebug("doLongPress(), selection = ", animatedSelection);
        int data[7];
        data[0] = animatedSelection;
        data[1] = lastPosition.x;
        data[2] = lastPosition.y;
        data[3] = cards[animatedSelection].detailTexturePosition[0].x;
        data[4] = cards[animatedSelection].detailTexturePosition[0].y;
        data[5] = cards[animatedSelection].detailTexturePosition[1].x;
        data[6] = cards[animatedSelection].detailTexturePosition[1].y;
        rsSendToClientBlocking(CMD_CARD_LONGPRESS, data, sizeof(data));
        enableSelection = false;
    }
    lastTime = rsUptimeMillis();
}

void doMotion(float x, float y, long eventTime)
{
    const float highBias = maximumBias();
    const float lowBias = minimumBias();
    float deltaOmega = dragFunction(x, y);
    overscrollBias += deltaOmega;
    overscrollBias = clamp(overscrollBias, lowBias - TILT_MAX_BIAS,
            highBias + TILT_MAX_BIAS);
    bias = clamp(overscrollBias, lowBias, highBias);
    isOverScrolling = tiltOverscroll();

    const float2 delta = (float2) { x, y } - touchPosition;
    float distance = sqrt(dot(delta, delta));
    bool inside = (distance < selectionRadius);
    enableSelection &= inside;
    lastPosition = (float2) { x, y };
    float dt = deltaTimeInSeconds(eventTime);
    if (dt > 0.0f) {
        float v = deltaOmega / dt;
        velocityHistory[velocityHistoryCount % VELOCITY_HISTORY_MAX] = v;
        velocityHistoryCount++;
    }
    velocity = computeAverageVelocityFromHistory();
    lastTime = eventTime;
}

bool tiltOverscroll() {
    if (overscrollBias == bias) {
        // No overscroll required.
        return false;
    }

    // How much we deviate from the maximum bias.
    float deltaBias = overscrollBias - bias;
    // We clamped, that means we need overscroll.
    tiltAngle = (deltaBias / TILT_MAX_BIAS)
            * TILT_MAX_ANGLE * fillDirection;

    return true;
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Autoscroll Interpolation
////////////////////////////////////////////////////////////////////////////////////////////////////
static int64_t autoscrollStartTime = 0L; //tracks when we actually started interpolating
static int64_t autoscrollDuration = 0L;  //in milli seconds
static int autoscrollInterpolationMode = INTERPOLATION_LINEAR;

static float autoscrollStopAngle = 0.0f;
static float autoscrollStartAngle = 0.0f;

void setCarouselRotationAngle2(
    float endAngle,
    int   milliseconds,
    int   interpolationMode,
    float maxAnimatedArc)
{
    float actualStart = radiansToCarouselRotationAngle(bias);

    if (maxAnimatedArc > 0) {
        //snap the current position to keep end - start under maxAnimatedArc
        if (actualStart <= endAngle) {
            if (actualStart < endAngle - maxAnimatedArc) {
                actualStart = endAngle - maxAnimatedArc;
            }
        }
        else {
            if (actualStart > endAngle + maxAnimatedArc) {
                actualStart = endAngle + maxAnimatedArc;
            }
        }
    }

    animating = true;
    isAutoScrolling = true;
    autoscrollDuration = milliseconds;
    autoscrollInterpolationMode = interpolationMode;
    autoscrollStartAngle = carouselRotationAngleToRadians(actualStart);
    autoscrollStopAngle = carouselRotationAngleToRadians(endAngle);

    //Make sure the start and stop angles are in the allowed range
    const float highBias = maximumBias();
    const float lowBias  = minimumBias();
    autoscrollStartAngle = clamp(autoscrollStartAngle, lowBias, highBias);
    autoscrollStopAngle  = clamp(autoscrollStopAngle, lowBias, highBias);

    //stop other animation kinds
    isOverScrolling = false;
    velocity = 0.0f;
}

static void stopAutoscroll()
{
    isAutoScrolling = false;
    autoscrollStartTime = 0L; //reset for next time
}

// This method computes the position of all the cards by updating bias based on a
// simple interpolation model.  If the cards are still in motion, returns true.
static bool doAutoscroll(float currentTime)
{
    if (autoscrollDuration == 0L) {
        return false;
    }

    if (autoscrollStartTime == 0L) {
        autoscrollStartTime = currentTime;
    }

    const int64_t interpolationEndTime = autoscrollStartTime + autoscrollDuration;

    float timePos = (currentTime - autoscrollStartTime) / (float)autoscrollDuration;
    if (timePos > 1.0f) {
        timePos = 1.0f;
    }

    float lambda = timePos; //default to linear
    if (autoscrollInterpolationMode == INTERPOLATION_DECELERATE_QUADRATIC) {
        lambda = 1.0f - (1.0f - timePos) * (1.0f - timePos);
    }
    else if (autoscrollInterpolationMode == INTERPOLATION_ACCELERATE_DECELERATE_CUBIC) {
        lambda = timePos * timePos * (3 - 2 * timePos);
    }

    bias = lambda * autoscrollStopAngle + (1.0 - lambda) * autoscrollStartAngle;

    if (currentTime > interpolationEndTime) {
        stopAutoscroll();
        return false;
    }
    else {
        return true;
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Hit detection using ray casting.
////////////////////////////////////////////////////////////////////////////////////////////////////
static const float EPSILON = 1.0e-6f;
static const float tmin = 0.0f;

static bool
rayTriangleIntersect(Ray* ray, float3 p0, float3 p1, float3 p2, float* tout)
{
    float3 e1 = p1 - p0;
    float3 e2 = p2 - p0;
    float3 s1 = cross(ray->direction, e2);

    float div = dot(s1, e1);
    if (div == 0.0f) return false;  // ray is parallel to plane.

    float3 d = ray->position - p0;
    float invDiv = 1.0f / div;

    float u = dot(d, s1) * invDiv;
    if (u < 0.0f || u > 1.0f) return false;

    float3 s2 = cross(d, e1);
    float v = dot(ray->direction, s2) * invDiv;
    if ( v < 0.0f || (u+v) > 1.0f) return false;

    float t = dot(e2, s2) * invDiv;
    if (t < tmin || t > *tout)
        return false;
    *tout = t;
    return true;
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Computes ray/plane intersection. Returns false if no intersection found.
////////////////////////////////////////////////////////////////////////////////////////////////////
static bool
rayPlaneIntersect(Ray* ray, Plane* plane, float* tout)
{
    float denom = dot(ray->direction, plane->normal);
    if (fabs(denom) > EPSILON) {
        float t = - (plane->constant + dot(ray->position, plane->normal)) / denom;
        if (t > tmin && t < *tout) {
            *tout = t;
            return true;
        }
    }
    return false;
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Computes ray/cylindr intersection. There are 0, 1 or 2 hits.
// Returns true and sets *tout to the closest point or
// returns false if no intersection found.
////////////////////////////////////////////////////////////////////////////////////////////////////
static bool
rayCylinderIntersect(Ray* ray, Cylinder* cylinder, float* tout)
{
    const float A = ray->direction.x * ray->direction.x + ray->direction.z * ray->direction.z;
    if (A < EPSILON) return false; // ray misses

    // Compute quadratic equation coefficients
    const float B = 2.0f * (ray->direction.x * ray->position.x
            + ray->direction.z * ray->position.z);
    const float C = ray->position.x * ray->position.x
            + ray->position.z * ray->position.z
            - cylinder->radius * cylinder->radius;
    float disc = B*B - 4*A*C;

    if (disc < 0.0f) return false; // ray misses
    disc = sqrt(disc);
    const float denom = 2.0f * A;

    // Nearest point
    const float t1 = (-B - disc) / denom;
    if (dragModel == DRAG_MODEL_CYLINDER_OUTSIDE && t1 > tmin && t1 < *tout) {
        *tout = t1;
        return true;
    }

    // Far point
    const float t2 = (-B + disc) / denom;
    if (dragModel == DRAG_MODEL_CYLINDER_INSIDE && t2 > tmin && t2 < *tout) {
        *tout = t2;
        return true;
    }
    return false;
}

// Creates a ray for an Android pixel coordinate given a camera, ray and coordinates.
// Note that the Y coordinate is opposite of GL rendering coordinates.
static bool __attribute__((overloadable))
makeRayForPixelAt(Ray* ray, PerspectiveCamera* cam, float x, float y)
{
    if (debugCamera) {
        rsDebug("------ makeRay() -------", 0);
        rsDebug("Camera.from:", cam->from);
        rsDebug("Camera.at:", cam->at);
        rsDebug("Camera.dir:", normalize(cam->at - cam->from));
    }

    // Vector math.  This has the potential to be much faster.
    // TODO: pre-compute lowerLeftRay, du, dv to eliminate most of this math.
    const float u = x / rsgGetWidth();
    const float v = 1.0f - (y / rsgGetHeight());
    const float aspect = (float) rsgGetWidth() / rsgGetHeight();
    const float tanfov2 = 2.0f * tan(radians(cam->fov / 2.0f));
    float3 dir = normalize(cam->at - cam->from);
    float3 du = tanfov2 * normalize(cross(dir, cam->up));
    float3 dv = tanfov2 * normalize(cross(du, dir));
    du *= aspect;
    float3 lowerLeftRay = dir - (0.5f * du) - (0.5f * dv);
    const float3 rayPoint = cam->from;
    const float3 rayDir = normalize(lowerLeftRay + u*du + v*dv);
    if (debugCamera) {
        rsDebug("Ray direction (vector math) = ", rayDir);
    }

    ray->position =  rayPoint;
    ray->direction = rayDir;
    return true;
}

// Creates a ray for an Android pixel coordinate given a model view and projection matrix.
// Note that the Y coordinate is opposite of GL rendering coordinates.
static bool __attribute__((overloadable))
makeRayForPixelAt(Ray* ray, rs_matrix4x4* model, rs_matrix4x4* proj, float x, float y)
{
    rs_matrix4x4 pm = *model;
    rsMatrixLoadMultiply(&pm, proj, model);
    if (!rsMatrixInverse(&pm)) {
        rsDebug("ERROR: SINGULAR PM MATRIX", 0);
        return false;
    }
    const float width = rsgGetWidth();
    const float height = rsgGetHeight();
    const float winx = 2.0f * x / width - 1.0f;
    const float winy = 2.0f * y / height - 1.0f;

    float4 eye = { 0.0f, 0.0f, 0.0f, 1.0f };
    float4 at = { winx, winy, 1.0f, 1.0f };

    eye = rsMatrixMultiply(&pm, eye);
    eye *= 1.0f / eye.w;

    at = rsMatrixMultiply(&pm, at);
    at *= 1.0f / at.w;

    const float3 rayPoint = { eye.x, eye.y, eye.z };
    const float3 atPoint = { at.x, at.y, at.z };
    const float3 rayDir = normalize(atPoint - rayPoint);
    if (debugCamera) {
        rsDebug("winx: ", winx);
        rsDebug("winy: ", winy);
        rsDebug("Ray position (transformed) = ", eye);
        rsDebug("Ray direction (transformed) = ", rayDir);
    }
    ray->position =  rayPoint;
    ray->direction = rayDir;
    return true;
}

static int intersectDetailTexture(float x, float y, float2 *tapCoordinates)
{
    for (int id = 0; id < cardCount; id++) {
        if (cards[id].detailVisible) {
            const int x0 = cards[id].detailTexturePosition[0].x;
            const int y0 = cards[id].detailTexturePosition[0].y;
            const int x1 = cards[id].detailTexturePosition[1].x;
            const int y1 = cards[id].detailTexturePosition[1].y;
            if (x >= x0 && x <= x1 && y >= y0 && y <= y1) {
                float2 point = { x - x0, y - y0 };
                *tapCoordinates = point;
                return id;
            }
        }
    }
    return -1;
}

static int intersectGeometry(Ray* ray, float *bestTime)
{
    int hit = -1;
    for (int id = 0; id < cardCount; id++) {
        if (cards[id].cardVisible) {
            rs_matrix4x4 matrix;
            float3 p[4];

            // Transform card vertices to world space
            rsMatrixLoadIdentity(&matrix);
            getMatrixForCard(&matrix, id, true, true);
            for (int vertex = 0; vertex < 4; vertex++) {
                float4 tmp = rsMatrixMultiply(&matrix, cardVertices[vertex]);
                if (tmp.w != 0.0f) {
                    p[vertex].x = tmp.x;
                    p[vertex].y = tmp.y;
                    p[vertex].z = tmp.z;
                    p[vertex] *= 1.0f / tmp.w;
                } else {
                    rsDebug("Bad w coord: ", tmp);
                }
            }

            // Intersect card geometry
            if (rayTriangleIntersect(ray, p[0], p[1], p[2], bestTime)
                || rayTriangleIntersect(ray, p[2], p[3], p[0], bestTime)) {
                hit = id;
            }
        }
    }
    return hit;
}

// This method computes the position of all the cards by updating bias based on a
// simple physics model.  If the cards are still in motion, returns true.
static bool doPhysics(float dt)
{
    const float minStepTime = 1.0f / 300.0f; // ~5 steps per frame
    const int N = (dt > minStepTime) ? (1 + round(dt / minStepTime)) : 1;
    dt /= N;
    for (int i = 0; i < N; i++) {
        // Force friction - always opposes motion
        const float Ff = -frictionCoeff * velocity;

        // Restoring force to match cards with slots
        const float theta = startAngle + bias;
        const float dtheta = 2.0f * M_PI / slotCount;
        const float position = theta / dtheta;
        const float fraction = position - floor(position); // fractional position between slots
        float x;
        if (fraction > 0.5f) {
            x = - (1.0f - fraction);
        } else {
            x = fraction;
        }
        const float Fr = - springConstant * x;

        // compute velocity
        const float momentum = mass * velocity + (Ff + Fr)*dt;
        velocity = momentum / mass;
        bias += velocity * dt;
    }
    return fabs(velocity) > stopVelocity;
}

static float easeOut(float x)
{
    return x;
}

// Computes the next value for bias using the current animation (physics/overscroll/autoscrolling)
static bool updateNextPosition(int64_t currentTime)
{
    static const float biasMin = 1e-4f; // close enough if we're within this margin of result

    float dt = deltaTimeInSeconds(currentTime);

    if (dt <= 0.0f) {
        if (debugRendering) rsDebug("Time delta was <= 0", dt);
        return true;
    }

    const float firstBias = maximumBias();
    const float lastBias = minimumBias();
    bool stillAnimating = false;
    if (isOverScrolling) {
        if (tiltAngle > TILT_MIN_ANGLE) {
            tiltAngle -= dt * TILT_MAX_ANGLE;
            stillAnimating = true;
        } else if (tiltAngle < -TILT_MIN_ANGLE) {
            tiltAngle += dt * TILT_MAX_ANGLE;
            stillAnimating = true;
        } else {
           isOverScrolling = false;
           tiltAngle = false;
           velocity = 0.0f;
        }
    } else if (isAutoScrolling) {
        stillAnimating = doAutoscroll(currentTime);
    } else {
        stillAnimating = doPhysics(dt);
        isOverScrolling = tiltAngle != 0;
        if (isOverScrolling) {
            velocity = 0.0f; // prevent bouncing due to v > 0 after overscroll animation.
            stillAnimating = true;
        }
    }
    bias = clamp(bias, lastBias, firstBias);
    return stillAnimating;
}

// Cull cards based on visibility and visibleSlotCount.
// If visibleSlotCount is > 0, then only show those slots and cull the rest.
// Otherwise, it should cull based on bounds of geometry.
static void cullCards()
{
    // Calculate the first and last angles of visible slots.  We include
    // VISIBLE_SLOT_PADDING slots on either side of visibleSlotCount to allow
    // cards to slide in / out at either side, and rely on the view frustrum
    // for accurate clipping.
    const float visibleFirst = slotPosition(-VISIBLE_SLOT_PADDING);
    const float visibleLast = slotPosition(visibleSlotCount + VISIBLE_SLOT_PADDING);

    // We'll load but not draw prefetchCardCountPerSide cards
    // from either side of the visible slots.
    const int prefetchCardCountPerSide = max(prefetchCardCount / 2, VISIBLE_SLOT_PADDING);
    const float prefetchFirst = slotPosition(-prefetchCardCountPerSide);
    const float prefetchLast = slotPosition(visibleSlotCount + prefetchCardCountPerSide);
    for (int i = 0; i < cardCount; i++) {
        if (visibleSlotCount > 0) {
            // If visibleSlotCount is specified then only show cards between visibleFirst and visibleLast
            float p = cardPosition(i);
            if ((p >= prefetchFirst && p < prefetchLast)
                    || (p <= prefetchFirst && p > prefetchLast)) {
                cards[i].shouldPrefetch = true;
                cards[i].cardVisible = (p >= visibleFirst && p < visibleLast)
                        || (p <= visibleFirst && p > visibleLast);
                // cards[i].detailVisible will be set at draw time
            } else {
                cards[i].shouldPrefetch = false;
                cards[i].cardVisible = false;
                cards[i].detailVisible = false;
            }
        } else {
            // Cull the rest of the cards using bounding box of geometry.
            // TODO
            cards[i].cardVisible = true;
            // cards[i].detailVisible will be set at draw time
        }
    }
}

// Request missing texture/geometry for a single card
static void requestCardResources(int i) {
    if (debugTextureLoading) rsDebug("*** Texture stamp: ", (int)cards[i].textureTimeStamp);
    int data[1] = { i };

    // request texture from client if not loaded
    if (cards[i].textureState == STATE_INVALID) {
        if (debugTextureLoading) rsDebug("Requesting card because state is STATE_INVALID", i);
        bool enqueued = rsSendToClient(CMD_REQUEST_TEXTURE, data, sizeof(data));
        if (enqueued) {
            cards[i].textureState = STATE_LOADING;
        } else {
            if (debugTextureLoading) rsDebug("Couldn't send CMD_REQUEST_TEXTURE", i);
        }
    } else if (cards[i].textureState == STATE_STALE) {
        if (debugTextureLoading) rsDebug("Requesting card because state is STATE_STALE", i);
        bool enqueued = rsSendToClient(CMD_REQUEST_TEXTURE, data, sizeof(data));
        if (enqueued) {
            cards[i].textureState = STATE_UPDATING;
        } else {
            if (debugTextureLoading) rsDebug("Couldn't send CMD_REQUEST_TEXTURE", i);
        }
    }

    // request detail texture from client if not loaded
    if (cards[i].detailTextureState == STATE_INVALID) {
        bool enqueued = rsSendToClient(CMD_REQUEST_DETAIL_TEXTURE, data, sizeof(data));
        if (enqueued) {
            cards[i].detailTextureState = STATE_LOADING;
        } else {
            if (debugTextureLoading) rsDebug("Couldn't send CMD_REQUEST_DETAIL_TEXTURE", i);
        }
    } else if (cards[i].detailTextureState == STATE_STALE) {
        bool enqueued = rsSendToClient(CMD_REQUEST_DETAIL_TEXTURE, data, sizeof(data));
        if (enqueued) {
            cards[i].detailTextureState = STATE_UPDATING;
        } else {
            if (debugTextureLoading) rsDebug("Couldn't send CMD_REQUEST_DETAIL_TEXTURE", i);
        }
    }

    // request geometry from client if not loaded
    if (cards[i].geometryState == STATE_INVALID) {
        bool enqueued = rsSendToClient(CMD_REQUEST_GEOMETRY, data, sizeof(data));
        if (enqueued) {
            cards[i].geometryState = STATE_LOADING;
        } else {
            if (debugGeometryLoading) rsDebug("Couldn't send CMD_REQUEST_GEOMETRY", i);
        }
    }
}

// Request texture/geometry for items that have come into view
// or doesn't have a texture yet.
static void updateCardResources(int64_t currentTime)
{
    // First process any visible cards
    for (int i = cardCount-1; i >= 0; --i) {
        if (cards[i].cardVisible) {
            requestCardResources(i);
        }
    }

    // Then the rest
    for (int i = cardCount-1; i >= 0; --i) {
        if (cards[i].cardVisible) {
            // already requested above
        } else if (cards[i].shouldPrefetch) {
            requestCardResources(i);
        } else {
            // ask the host to remove the texture
            int data[1];
            if (cards[i].textureState != STATE_INVALID) {
                data[0] = i;
                bool enqueued = rsSendToClient(CMD_INVALIDATE_TEXTURE, data, sizeof(data));
                if (enqueued) {
                    cards[i].textureState = STATE_INVALID;
                    cards[i].textureTimeStamp = currentTime;
                } else {
                    if (debugTextureLoading) rsDebug("Couldn't send CMD_INVALIDATE_TEXTURE", 0);
                }
            }
            // ask the host to remove the detail texture
            if (cards[i].detailTextureState != STATE_INVALID) {
                data[0] = i;
                bool enqueued = rsSendToClient(CMD_INVALIDATE_DETAIL_TEXTURE, data, sizeof(data));
                if (enqueued) {
                    cards[i].detailTextureState = STATE_INVALID;
                    cards[i].detailTextureTimeStamp = currentTime;
                } else {
                    if (debugTextureLoading) rsDebug("Can't send CMD_INVALIDATE_DETAIL_TEXTURE", 0);
                }
            }
            // ask the host to remove the geometry
            if (cards[i].geometryState != STATE_INVALID) {
                data[0] = i;
                bool enqueued = rsSendToClient(CMD_INVALIDATE_GEOMETRY, data, sizeof(data));
                if (enqueued) {
                    cards[i].geometryState = STATE_INVALID;
                } else {
                    if (debugGeometryLoading) rsDebug("Couldn't send CMD_INVALIDATE_GEOMETRY", 0);
                }
            }
        }
    }
}

// Places dots on geometry to visually inspect that objects can be seen by rays.
// NOTE: the color of the dot is somewhat random, as it depends on texture of previously-rendered
// card.
static void renderWithRays()
{
    const float w = rsgGetWidth();
    const float h = rsgGetHeight();
    const int skip = 8;

    rsgProgramFragmentConstantColor(singleTextureFragmentProgram, 1.0f, 0.0f, 0.0f, 1.0f);
    for (int j = 0; j < (int) h; j+=skip) {
        float posY = (float) j;
        for (int i = 0; i < (int) w; i+=skip) {
            float posX = (float) i;
            Ray ray;
            if (makeRayForPixelAt(&ray, &camera, posX, posY)) {
                float bestTime = FLT_MAX;
                if (intersectGeometry(&ray, &bestTime) != -1) {
                    rsgDrawSpriteScreenspace(posX, h - posY - 1, 0.0f, 2.0f, 2.0f);
                }
            }
        }
    }
}

int root() {
    int64_t currentTime = rsUptimeMillis();

    rsgBindProgramVertex(vertexProgram);
    rsgBindProgramRaster(rasterProgram);
    rsgBindSampler(singleTextureFragmentProgram, 0, linearClamp);
    rsgBindSampler(multiTextureFragmentProgram, 0, linearClamp);
    rsgBindSampler(multiTextureFragmentProgram, 1, linearClamp);

    updateAllocationVars();

    rsgBindProgramFragment(singleTextureFragmentProgram);
    // rsgClearDepth() currently follows the value of glDepthMask(), so it's disabled when
    // the mask is disabled. We may want to change the following to always draw w/o Z for
    // the background if we can guarantee the depth buffer will get cleared and
    // there's a performance advantage.
    rsgBindProgramStore(programStoreBackground);
    drawBackground();

    updateCameraMatrix(rsgGetWidth(), rsgGetHeight());

    bool stillAnimating = (currentTime - touchTime) <= ANIMATION_SCALE_UP_TIME;

    if (!isDragging && animating) {
        stillAnimating = updateNextPosition(currentTime);
    }

    lastTime = currentTime;

    cullCards();

    updateCardResources(currentTime);

    // Draw cards opaque only if requested, and always draw detail textures with blending.
    stillAnimating |= drawCards(currentTime);
    rsgBindProgramStore(programStoreDetail);
    stillAnimating |= drawDetails(currentTime);

    if (stillAnimating != animating) {
        if (stillAnimating) {
            // we just started animating
            sendAnimationStarted();
        } else {
            // we were animating but stopped animating just now
            sendAnimationFinished();
        }
        animating = stillAnimating;
    }

    if (debugRays) {
        renderWithRays();
    }

    //rsSendToClient(CMD_PING);

    return animating ? 1 : 0;
}
