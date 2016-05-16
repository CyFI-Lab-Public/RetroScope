:: This script must not rely on any external tools or PATH values.
@echo OFF

if "%SETUP_ENV_LIBYUV_TOOLS%"=="done" goto :EOF
set SETUP_ENV_LIBYUV_TOOLS=done

:: TODO(fbarchard): add files\win32 to for psnr tool
