varying lowp vec4 color;
varying lowp float factor1;
varying lowp float factor2;

void main()
{
    gl_Position = UNI_modelViewProj * vec4(ATTRIB_position.xyz, 1.0);
    factor2 = (UNI_farPlane - abs(gl_Position.z)) / UNI_farPlane;
    gl_PointSize = factor2 * UNI_maxPointSize;
    color = ATTRIB_color;
    color.a = color.a * factor2;
    factor2 = abs((factor2 * 2.0) - 1.0);
    factor1 = (1.0 - factor2) * 0.2;
}
