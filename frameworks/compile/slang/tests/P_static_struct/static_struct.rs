// -target-api 12
#pragma version(1)
#pragma rs java_package_name(com.example);

typedef struct Plane_s {
    float3 point;
} Plane;

static Plane carouselPlane = {
    { 0.0f, 0.0f, 0.0f }
};
