eglInitialize check major 1 check minor 1
eglGetConfigs check configs config_size
eglChooseConfig check configs config_size check num_config 1 sentinel attrib_list EGL_NONE
eglGetConfigAttrib check value 1
//STUB function: //eglCreateWindowSurface sentinel attrib_list EGL_NONE
eglCreatePbufferSurface sentinel attrib_list EGL_NONE
//unsupported: eglCreatePixmapSurface sentinel attrib_list EGL_NONE
eglCreatePixmapSurface unsupported
eglCopyBuffers unsupported
eglQuerySurface check value 1
eglCreatePbufferFromClientBuffer sentinel attrib_list EGL_NONE
eglCreateContext sentinel attrib_list EGL_NONE
eglQueryContext check value 1
