/****************************************************************************
 ****************************************************************************
 ***
 ***   This header was automatically generated from a Linux kernel header
 ***   of the same name, to make information necessary for userspace to
 ***   call into the kernel available to libc.  It contains only constants,
 ***   structures, and macros generated from the original header, and thus,
 ***   contains no copyrightable information.
 ***
 ***   To edit the content of this header, modify the corresponding
 ***   source file (e.g. under external/kernel-headers/original/) then
 ***   run bionic/libc/kernel/tools/update_all.py
 ***
 ***   Any manual change here will be lost the next time this script will
 ***   be run. You've been warned!
 ***
 ****************************************************************************
 ****************************************************************************/
#ifndef _MSM_VIDC_H_
#define _MSM_VIDC_H_
struct msm_vidc_interlace_payload {
 unsigned int format;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct msm_vidc_framerate_payload {
 unsigned int frame_rate;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct msm_vidc_ts_payload {
 unsigned int timestamp_lo;
 unsigned int timestamp_hi;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct msm_vidc_concealmb_payload {
 unsigned int num_mbs;
};
struct msm_vidc_recoverysei_payload {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int flags;
};
struct msm_vidc_aspect_ratio_payload {
 unsigned int size;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int version;
 unsigned int port_index;
 unsigned int aspect_width;
 unsigned int aspect_height;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct msm_vidc_mpeg2_seqdisp_payload {
 unsigned int video_format;
 bool color_descp;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int color_primaries;
 unsigned int transfer_char;
 unsigned int matrix_coeffs;
 unsigned int disp_width;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int disp_height;
};
struct msm_vidc_panscan_window {
 unsigned int panscan_height_offset;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int panscan_width_offset;
 unsigned int panscan_window_width;
 unsigned int panscan_window_height;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct msm_vidc_panscan_window_payload {
 unsigned int num_panscan_windows;
 struct msm_vidc_panscan_window wnd[1];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum msm_vidc_extradata_type {
 EXTRADATA_NONE = 0x00000000,
 EXTRADATA_MB_QUANTIZATION = 0x00000001,
 EXTRADATA_INTERLACE_VIDEO = 0x00000002,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 EXTRADATA_VC1_FRAMEDISP = 0x00000003,
 EXTRADATA_VC1_SEQDISP = 0x00000004,
 EXTRADATA_TIMESTAMP = 0x00000005,
 EXTRADATA_S3D_FRAME_PACKING = 0x00000006,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 EXTRADATA_FRAME_RATE = 0x00000007,
 EXTRADATA_PANSCAN_WINDOW = 0x00000008,
 EXTRADATA_RECOVERY_POINT_SEI = 0x00000009,
 EXTRADATA_MPEG2_SEQDISP = 0x0000000D,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 EXTRADATA_MULTISLICE_INFO = 0x7F100000,
 EXTRADATA_NUM_CONCEALED_MB = 0x7F100001,
 EXTRADATA_INDEX = 0x7F100002,
 EXTRADATA_ASPECT_RATIO = 0x7F100003,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 EXTRADATA_METADATA_FILLER = 0x7FE00002,
};
enum msm_vidc_interlace_type {
 INTERLACE_FRAME_PROGRESSIVE = 0x01,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 INTERLACE_INTERLEAVE_FRAME_TOPFIELDFIRST = 0x02,
 INTERLACE_INTERLEAVE_FRAME_BOTTOMFIELDFIRST = 0x04,
 INTERLACE_FRAME_TOPFIELDFIRST = 0x08,
 INTERLACE_FRAME_BOTTOMFIELDFIRST = 0x10,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum msm_vidc_recovery_sei {
 FRAME_RECONSTRUCTION_INCORRECT = 0x0,
 FRAME_RECONSTRUCTION_CORRECT = 0x01,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FRAME_RECONSTRUCTION_APPROXIMATELY_CORRECT = 0x02,
};
#endif
