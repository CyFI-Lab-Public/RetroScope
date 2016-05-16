varying lowp vec4 color;
varying lowp float factor1;
varying lowp float factor2;

void main()
{
    lowp vec4 texColor = texture2D(UNI_Tex0, gl_PointCoord);
    gl_FragColor.a = color.a * (texColor.r * factor1 + texColor.g * factor2);
    gl_FragColor.rgb = color.rgb;
}
