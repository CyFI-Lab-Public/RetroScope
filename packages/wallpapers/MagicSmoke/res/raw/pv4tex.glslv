varying vec2 varTex0;
varying vec2 varTex1;
varying vec2 varTex2;
varying vec2 varTex3;

vec2 mul(vec4 uni, vec2 attr, float idx, vec2 offset)
{
    float invz = 0.35 + idx*0.05;
    return vec2(
        0.5 + 0.5 * invz * (uni.z * ( uni.y * (attr.x + offset.x) + uni.x * (attr.y + offset.y))) + uni.w,
        0.5 + 0.5 * invz * (uni.z * (-uni.x * (attr.x + offset.x) + uni.y * (attr.y + offset.y))));
}

void main() {
    varTex0 = mul(UNI_layer0, ATTRIB_position.xy, 1.0, UNI_panoffset);
    varTex1 = mul(UNI_layer1, ATTRIB_position.xy, 2.0, UNI_panoffset);
    varTex2 = mul(UNI_layer2, ATTRIB_position.xy, 3.0, UNI_panoffset);
    varTex3 = mul(UNI_layer3, ATTRIB_position.xy, 4.0, UNI_panoffset);
    gl_Position = ATTRIB_position;
}
