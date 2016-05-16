
void main()  {
    lowp vec4 texColor;
    texColor = texture2D(UNI_Tex0, gl_PointCoord);

    gl_FragColor.rgb = texColor.rgb;
    gl_FragColor.a = 0.1;
}