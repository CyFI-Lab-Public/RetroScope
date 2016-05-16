varying vec4 varColor;

void main() {
    float dist = ATTRIB_position.y * UNI_scaleSize;
    float angle = ATTRIB_position.x;

    float x = dist * sin(angle);
    float y = dist * cos(angle) * 0.8;
    float p = dist * 7.5;
    float s = cos(p);
    float t = sin(p);
    vec4 pos;
    pos.x = t*x + s*y;
    pos.y = s*x - t*y;
    pos.z = ATTRIB_position.z;
    pos.w = 1.0;

    pos.y = pos.y * 0.5;
    gl_Position = UNI_MVP * pos;
    varColor = vec4(1.0, 1.0, 1.0, 0.5);
    float pointSize = 1.0;

    gl_PointSize = pointSize;
}