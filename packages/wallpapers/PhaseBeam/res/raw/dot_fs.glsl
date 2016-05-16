varying float pointSize;

void main()  {
    gl_FragColor = texture2D(UNI_Tex0, gl_PointCoord);
    gl_FragColor.a = pointSize;
}