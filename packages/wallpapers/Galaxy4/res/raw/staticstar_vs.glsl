varying float pointSize;

void main() {
    vec4 pos;
    pos.x = ATTRIB_position.x;
    pos.y = ATTRIB_position.y;
    pos.z = ATTRIB_position.z;
    pos.w = 1.0;

    gl_Position = UNI_MVP * pos;
    pointSize = ATTRIB_pointSize;
    gl_PointSize = pointSize*UNI_scaleSize;
}