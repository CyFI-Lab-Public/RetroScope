varying float pointSize;
varying float alpha;
void main() {
    vec4 pos = vec4(ATTRIB_position.xyz, 1.0);
    gl_Position = UNI_MVP * pos;

    float pointSize = 1.0 + ATTRIB_speed * UNI_scaleSize * 2500.0;
    alpha = ATTRIB_alpha;
    gl_PointSize = pointSize;
}