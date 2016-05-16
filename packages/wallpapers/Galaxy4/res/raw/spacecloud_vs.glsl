varying float pointSize;

void main() {
    float dist = ATTRIB_position.y / 4.0;
    float angle = ATTRIB_position.x;

    float x = dist * sin(angle) * 0.8;
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
    pointSize = 170.0-(pos.y*pos.y*1.2*1.2 + pos.x*pos.x)*400.0;
    if (pointSize<=30.0) {
        pointSize = 30.0;
    }
    gl_PointSize = pointSize;
}