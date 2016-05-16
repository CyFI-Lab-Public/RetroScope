#ifndef DALVIK_ZOMBIE_PATCH_VT_MANUAL_H_
#define DALVIK_ZOMBIE_PATCH_VT_MANUAL_H_

#define ZMB_EXTERNAL

#include <zombie/MemMap.h>
#include <SkiaShader.h>

static void zmbPatchVTShader(SkiaShader * shader)
{
  if (zmbIsSafeAddr(shader) && zmbIsSafeAddr(*((void **)shader))){
    SkiaShader::Type shaderType = shader->mType;
    SkiaShader * newShader = NULL;
    switch (shaderType)
    {
       case SkiaShader::kNone: 		newShader = NULL; break;
       case SkiaShader::kBitmap:	newShader = new SkiaBitmapShader(); break;
       case SkiaShader::kLinearGradient:	newShader = new SkiaLinearGradientShader(); break;
       case SkiaShader::kCircularGradient: 	newShader = new SkiaCircularGradientShader(); break;
       case SkiaShader::kSweepGradient: 	newShader = new SkiaSweepGradientShader(); break;
       case SkiaShader::kCompose: 	newShader = new SkiaComposeShader(); break;
    }
    void * newVT = *((void **)newShader);
    *((void **)shader) = newVT;
 }
}

static void zmbPatchVTColorFilter(SkiaColorFilter * filter)
{
  if (zmbIsSafeAddr(filter) && zmbIsSafeAddr(*((void **)filter))){
    SkiaColorFilter::Type filterType = filter->mType;
    SkiaColorFilter * newFilter = NULL;
    switch (filterType)
    {
       case SkiaColorFilter::kNone:	newFilter = NULL; break;
       case SkiaColorFilter::kColorMatrix:	newFilter = new SkiaColorMatrixFilter(); break;
       case SkiaColorFilter::kLighting:		newFilter = new SkiaLightingFilter(); break;
       case SkiaColorFilter::kBlend: 	newFilter = new SkiaBlendFilter(); break;
    }
    void * newVT = *((void **)newFilter);
    *((void **)filter) = newVT;
    //delete newFilter;
  }
}

#endif // DALVIK_ZOMBIE_PATCH_VT_MANUAL_H_
