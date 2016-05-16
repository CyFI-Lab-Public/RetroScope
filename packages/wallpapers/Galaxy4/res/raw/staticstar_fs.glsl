varying float pointSize;

void main()  {
    if (pointSize > 4.0) {
        gl_FragColor = texture2D(UNI_Tex1, gl_PointCoord);
    } else {
        gl_FragColor = texture2D(UNI_Tex0, gl_PointCoord);
    }
}