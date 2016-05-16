varying float alpha;

void main()  {
    lowp vec4 texColor;
    texColor = texture2D(UNI_Tex0, gl_PointCoord);
    texColor.a = texColor.a*alpha;
    gl_FragColor = texColor;
}