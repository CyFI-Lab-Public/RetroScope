precision mediump float;

varying vec2 varTex0;
varying vec2 varTex1;
varying vec2 varTex2;
varying vec2 varTex3;
varying vec2 varTex4;

void main() {
    lowp vec4 tex =  texture2D(UNI_Tex0, varTex0);
    lowp vec4 col = mix(UNI_clearColor, tex.rgba, tex.a);

    tex = texture2D(UNI_Tex1, varTex1);
    col = mix(col, tex.rgba, tex.a);

    tex = texture2D(UNI_Tex2, varTex2);
    col = mix(col, tex.rgba, tex.a);

    tex = texture2D(UNI_Tex3, varTex3);
    col = mix(col, tex.rgba, tex.a);

    tex = texture2D(UNI_Tex4, varTex4);
    col = mix(col, tex.rgba, tex.a);
    gl_FragColor = col;
}

