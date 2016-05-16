/*
 * Copyright (C) 2008 The Android Open Source Project
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

#include <utils/Errors.h>
#include <binder/Parcel.h>
#include <gui/ISurfaceComposerClient.h>
#include <gui/IGraphicBufferProducer.h>
#include <private/gui/LayerState.h>

namespace android {

status_t layer_state_t::write(Parcel& output) const
{
    output.writeStrongBinder(surface);
    output.writeInt32(what);
    output.writeFloat(x);
    output.writeFloat(y);
    output.writeInt32(z);
    output.writeInt32(w);
    output.writeInt32(h);
    output.writeInt32(layerStack);
    output.writeFloat(alpha);
    output.writeInt32(flags);
    output.writeInt32(mask);
    *reinterpret_cast<layer_state_t::matrix22_t *>(
            output.writeInplace(sizeof(layer_state_t::matrix22_t))) = matrix;
    output.write(crop);
    output.write(transparentRegion);
    return NO_ERROR;
}

status_t layer_state_t::read(const Parcel& input)
{
    surface = input.readStrongBinder();
    what = input.readInt32();
    x = input.readFloat();
    y = input.readFloat();
    z = input.readInt32();
    w = input.readInt32();
    h = input.readInt32();
    layerStack = input.readInt32();
    alpha = input.readFloat();
    flags = input.readInt32();
    mask = input.readInt32();
    matrix = *reinterpret_cast<layer_state_t::matrix22_t const *>(
            input.readInplace(sizeof(layer_state_t::matrix22_t)));
    input.read(crop);
    input.read(transparentRegion);
    return NO_ERROR;
}

status_t ComposerState::write(Parcel& output) const {
    output.writeStrongBinder(client->asBinder());
    return state.write(output);
}

status_t ComposerState::read(const Parcel& input) {
    client = interface_cast<ISurfaceComposerClient>(input.readStrongBinder());
    return state.read(input);
}


status_t DisplayState::write(Parcel& output) const {
    output.writeStrongBinder(token);
    output.writeStrongBinder(surface->asBinder());
    output.writeInt32(what);
    output.writeInt32(layerStack);
    output.writeInt32(orientation);
    output.write(viewport);
    output.write(frame);
    return NO_ERROR;
}

status_t DisplayState::read(const Parcel& input) {
    token = input.readStrongBinder();
    surface = interface_cast<IGraphicBufferProducer>(input.readStrongBinder());
    what = input.readInt32();
    layerStack = input.readInt32();
    orientation = input.readInt32();
    input.read(viewport);
    input.read(frame);
    return NO_ERROR;
}


}; // namespace android
