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

#include <hardware/audio_effect.h>


extern "C" {

extern const struct effect_interface_s gCTSEffectInterface;

const effect_descriptor_t gCTSEffectsDescriptor = {
        {0xf2a4bb20, 0x0c3c, 0x11e3, 0x8b07, {0x00, 0x02, 0xa5, 0xd5, 0xc5, 0x1b}}, // type
        {0xff93e360, 0x0c3c, 0x11e3, 0x8a97, {0x00, 0x02, 0xa5, 0xd5, 0xc5, 0x1b}}, // uuid
        EFFECT_CONTROL_API_VERSION,
        0,
        0,
        1,
        "CTS test Effect",
        "The Android Open Source Project",
};

struct CTSEffectsContext {
    const struct effect_interface_s *mItfe;
    effect_config_t mConfig;
};

//
//--- Effect Library Interface Implementation
//

int CTSEffectsLib_Create(const effect_uuid_t *uuid,
                         int32_t sessionId,
                         int32_t ioId,
                         effect_handle_t *pHandle) {
    if (pHandle == NULL || uuid == NULL) {
        return -EINVAL;
    }

    if (memcmp(uuid, &gCTSEffectsDescriptor.uuid, sizeof(effect_uuid_t)) != 0) {
        return -EINVAL;
    }

    CTSEffectsContext *pContext = new CTSEffectsContext;

    pContext->mItfe = &gCTSEffectInterface;

    *pHandle = (effect_handle_t)pContext;

    return 0;

}

int CTSEffectsLib_Release(effect_handle_t handle) {
    CTSEffectsContext * pContext = (CTSEffectsContext *)handle;

    if (pContext == NULL) {
        return -EINVAL;
    }
    delete pContext;

    return 0;
}

int CTSEffectsLib_GetDescriptor(const effect_uuid_t *uuid,
                                effect_descriptor_t *pDescriptor) {

    if (pDescriptor == NULL || uuid == NULL){
        return -EINVAL;
    }

    if (memcmp(uuid, &gCTSEffectsDescriptor.uuid, sizeof(effect_uuid_t)) == 0) {
        *pDescriptor = gCTSEffectsDescriptor;
        return 0;
    }

    return  -EINVAL;
} /* end CTSEffectsLib_GetDescriptor */

//
//--- Effect Control Interface Implementation
//

int CTSEffects_process(
        effect_handle_t self,audio_buffer_t *inBuffer, audio_buffer_t *outBuffer)
{
    return 0;
}   // end CTSEffects_process

int CTSEffects_command(effect_handle_t self, uint32_t cmdCode, uint32_t cmdSize,
        void *pCmdData, uint32_t *replySize, void *pReplyData) {

    CTSEffectsContext * pContext = (CTSEffectsContext *)self;

    if (pContext == NULL) {
        return -EINVAL;
    }

    switch (cmdCode) {
    case EFFECT_CMD_INIT:
        if (pReplyData == NULL || *replySize != sizeof(int)) {
            return -EINVAL;
        }
        *(int *) pReplyData = 0;
        break;
    case EFFECT_CMD_SET_CONFIG:
        if (pCmdData == NULL || cmdSize != sizeof(effect_config_t)
                || pReplyData == NULL || *replySize != sizeof(int)) {
            return -EINVAL;
        }
        memcpy(&pContext->mConfig, pCmdData, cmdSize);
        *(int *) pReplyData = 0;
        break;
    case EFFECT_CMD_GET_CONFIG:
        if (pReplyData == NULL ||
            *replySize != sizeof(effect_config_t)) {
            return -EINVAL;
        }
        memcpy(pReplyData, &pContext->mConfig, *replySize);
        break;
    case EFFECT_CMD_RESET:
        break;
    case EFFECT_CMD_ENABLE:
    case EFFECT_CMD_DISABLE:
        if (pReplyData == NULL || *replySize != sizeof(int)) {
            return -EINVAL;
        }
        *(int *)pReplyData = 0;
        break;
    case EFFECT_CMD_GET_PARAM: {
        if (pCmdData == NULL ||
            cmdSize != (int)(sizeof(effect_param_t)) ||
            pReplyData == NULL ||
            *replySize < (int)(sizeof(effect_param_t))) {
            return -EINVAL;
        }
        effect_param_t *p = (effect_param_t *)pReplyData;
        p->status = 0;
        } break;
    case EFFECT_CMD_SET_PARAM: {
        if (pCmdData == NULL ||
            cmdSize != (int)(sizeof(effect_param_t)) ||
            pReplyData == NULL || *replySize != sizeof(int32_t)) {
            return -EINVAL;
        }
        *(int32_t *)pReplyData = 0;
        } break;
    default:
        break;
    }

    return 0;
}

/* Effect Control Interface Implementation: get_descriptor */
int CTSEffects_getDescriptor(effect_handle_t   self,
                                    effect_descriptor_t *pDescriptor)
{
    CTSEffectsContext * pContext = (CTSEffectsContext *) self;

    if (pContext == NULL || pDescriptor == NULL) {
        return -EINVAL;
    }

    *pDescriptor = gCTSEffectsDescriptor;

    return 0;
}   /* end CTSEffects_getDescriptor */

// effect_handle_t interface implementation for test effect
const struct effect_interface_s gCTSEffectInterface = {
        CTSEffects_process,
        CTSEffects_command,
        CTSEffects_getDescriptor,
        NULL,
};

// This is the only symbol that needs to be exported
__attribute__ ((visibility ("default")))
audio_effect_library_t AUDIO_EFFECT_LIBRARY_INFO_SYM = {
    tag : AUDIO_EFFECT_LIBRARY_TAG,
    version : EFFECT_LIBRARY_API_VERSION,
    name : "CTS Effects Library",
    implementor : "The Android Open Source Project",
    create_effect : CTSEffectsLib_Create,
    release_effect : CTSEffectsLib_Release,
    get_descriptor : CTSEffectsLib_GetDescriptor,
};

}; // extern "C"
