/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef QCOM_AUDIO_PLATFORM_API_H
#define QCOM_AUDIO_PLATFORM_API_H

void *platform_init(struct audio_device *adev);
void platform_deinit(void *platform);
const char *platform_get_snd_device_name(snd_device_t snd_device);
void platform_add_backend_name(char *mixer_path, snd_device_t snd_device);
int platform_get_pcm_device_id(audio_usecase_t usecase, int device_type);
int platform_send_audio_calibration(void *platform, snd_device_t snd_device);
int platform_switch_voice_call_device_pre(void *platform);
int platform_switch_voice_call_device_post(void *platform,
                                           snd_device_t out_snd_device,
                                           snd_device_t in_snd_device);
int platform_start_voice_call(void *platform);
int platform_stop_voice_call(void *platform);
int platform_set_voice_volume(void *platform, int volume);
int platform_set_mic_mute(void *platform, bool state);
snd_device_t platform_get_output_snd_device(void *platform, audio_devices_t devices);
snd_device_t platform_get_input_snd_device(void *platform, audio_devices_t out_device);
int platform_set_hdmi_channels(void *platform, int channel_count);
int platform_edid_get_max_channels(void *platform);

/* returns the latency for a usecase in Us */
int64_t platform_render_latency(audio_usecase_t usecase);

#endif // QCOM_AUDIO_PLATFORM_API_H
