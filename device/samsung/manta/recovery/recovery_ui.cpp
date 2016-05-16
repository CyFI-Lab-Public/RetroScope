/*
 * Copyright (C) 2012 The Android Open Source Project
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

#include <linux/input.h>
#include <sys/stat.h>
#include <errno.h>
#include <string.h>

#include "common.h"
#include "device.h"
#include "screen_ui.h"

const char* HEADERS[] = { "Volume up/down to move highlight;",
                          "power button to select.",
                          "",
                          NULL };

const char* ITEMS[] = { "reboot system now",
                        "apply update from ADB",
                        "wipe data/factory reset",
                        "wipe cache partition",
                        NULL };

class MantaUI : public ScreenRecoveryUI {
  public:
    MantaUI() :
        consecutive_power_keys(0) {
    }

    virtual KeyAction CheckKey(int key) {
        if (IsKeyPressed(KEY_POWER) && key == KEY_VOLUMEUP) {
            return TOGGLE;
        }
        if (key == KEY_POWER) {
            ++consecutive_power_keys;
            if (consecutive_power_keys >= 7) {
                return REBOOT;
            }
        } else {
            consecutive_power_keys = 0;
        }
        return ENQUEUE;
    }

  private:
    int consecutive_power_keys;
};


class MantaDevice : public Device {
  public:
    MantaDevice() :
        ui(new MantaUI) {
    }

    RecoveryUI* GetUI() { return ui; }

    int HandleMenuKey(int key_code, int visible) {
        if (visible) {
            switch (key_code) {
              case KEY_DOWN:
              case KEY_VOLUMEDOWN:
                return kHighlightDown;

              case KEY_UP:
              case KEY_VOLUMEUP:
                return kHighlightUp;

              case KEY_POWER:
                return kInvokeItem;
            }
        }

        return kNoAction;
    }

    BuiltinAction InvokeMenuItem(int menu_position) {
        switch (menu_position) {
          case 0: return REBOOT;
          case 1: return APPLY_ADB_SIDELOAD;
          case 2: return WIPE_DATA;
          case 3: return WIPE_CACHE;
          default: return NO_ACTION;
        }
    }

    const char* const* GetMenuHeaders() { return HEADERS; }
    const char* const* GetMenuItems() { return ITEMS; }

  private:
    RecoveryUI* ui;
};

Device* make_device() {
    return new MantaDevice;
}
