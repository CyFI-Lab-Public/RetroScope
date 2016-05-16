<!--
    Copyright 2010 The Android Open Source Project

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->
<?cs # Table of contents for devices.?>
<ul id="nav">

<!-- Porting Android -->
  <li class="nav-section">
    <div class="nav-section-header">
      <a href="<?cs var:toroot ?>devices/index.html">
        <span class="en">Porting</span>
      </a>
    </div>
    <ul>
      <li><a href="<?cs var:toroot ?>devices/media.html">Media</a></li>
      <li class="nav-section">
      <div class="nav-section-header">
        <a href="<?cs var:toroot ?>devices/audio.html">
          <span class="en">Audio</span>
        </a>
      </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/audio_latency.html">Latency</a></li>
          <li><a href="<?cs var:toroot ?>devices/audio_warmup.html">Warmup</a></li>
          <li><a href="<?cs var:toroot ?>devices/audio_avoiding_pi.html">Avoiding Priority Inversion</a></li>
          <li><a href="<?cs var:toroot ?>devices/latency_design.html">Design For Reduced Latency</a></li>
          <li><a href="<?cs var:toroot ?>devices/audio_terminology.html">Terminology</a></li>
          <li><a href="<?cs var:toroot ?>devices/testing_circuit.html">Testing Circuit</a></li>
        </ul>
      </li>
      <li><a href="<?cs var:toroot ?>devices/camera.html">Camera v1</a></li>
      <li><a href="<?cs var:toroot ?>devices/drm.html">DRM</a></li>
      <li><a href="<?cs var:toroot ?>devices/graphics.html">Graphics</a></li>
      <li><a href="<?cs var:toroot ?>devices/bluetooth.html">Bluetooth</a></li>
      <!-- Find a better section for these -->
      <li class="nav-section">
        <div class="nav-section-header empty">
          <a href="<?cs var:toroot ?>devices/reference/files.html">
            <span class="en">Reference</span>
          </a>
        </div>
      </li>
    </ul>
  </li>
<!-- End Porting Android -->
  </li>


  <li class="nav-section">
    <div class="nav-section-header">
      <a href="<?cs var:toroot ?>devices/tech/index.html">
        <span class="en">Technical Information</span>
      </a>
    </div>

    <ul>
      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/tech/dalvik/index.html">
          <span class="en">Dalvik</span></a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tech/dalvik/dalvik-bytecode.html">Bytecode Format</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/dalvik/dex-format.html">.Dex Format</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/dalvik/instruction-formats.html">Instruction Formats</a></li>
        </ul>
      </li>

      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/tech/datausage/index.html">
            <span class="en">Data Usage</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/iface-overview.html">Network interface statistics overview</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/excluding-network-types.html">Excluding Network Types from Data Usage</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/tethering-data.html">Tethering Data</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/usage-cycle-resets-dates.html">Usage Cycle Reset Dates</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/kernel-overview.html">Kernel Overview</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/tags-explained.html">Data Usage Tags Explained</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/kernel-changes.html">Kernel Changes</a></li>
        </ul>
      </li>
      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/debugtune.html">
            <span class="en">Debugging and Tuning</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tuning.html">Performance Tuning</a></li>
          <li><a href="<?cs var:toroot ?>devices/native-memory.html">Native Memory Usage</a></li>
        </ul>
      </li>

      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/tech/encryption/index.html">
            <span class="en">Encryption</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tech/encryption/android_crypto_implementation.html">Encryption Technical Information</a></li>
        </ul>
      </li>
     <li>
          <a href="<?cs var:toroot ?>devices/tech/storage/index.html">
            <span class="en">External Storage</span>
          </a>
      </li>
      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/tech/input/index.html">
            <span class="en">Input</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tech/input/overview.html">Overview</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/input/key-layout-files.html">Key Layout Files</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/input/key-character-map-files.html">Key Character Map Files</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/input/input-device-configuration-files.html">Input Device Configuration Files</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/input/migration-guide.html">Migration Guide</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/input/keyboard-devices.html">Keyboard Devices</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/input/touch-devices.html">Touch Devices</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/input/dumpsys.html">Dumpsys</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/input/getevent.html">Getevent</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/input/validate-keymaps.html">Validate Keymaps</a></li>
        </ul>
      </li>

      <li>
          <a href="<?cs var:toroot ?>devices/tech/kernel.html">
            <span class="en">Kernel</span>
          </a>
      </li>

      <li>
          <a href="<?cs var:toroot ?>devices/tech/power.html">
            <span class="en">Power</span>
          </a>
      </li>
     <li class="nav-section">
          <div class="nav-section-header">
            <a href="<?cs var:toroot ?>devices/tech/security/index.html">
              <span class="en">Security</span>
            </a>
          </div>
          <ul>
            <li>
              <a href="<?cs var:toroot ?>devices/tech/security/enhancements42.html">
                <span class="en">Security Enhancements in Android 4.2</span>
              </a>
            </li>
            <li>
              <a href="<?cs var:toroot ?>devices/tech/security/enhancements43.html">
                <span class="en">Security Enhancements in Android 4.3</span>
              </a>
            </li>
            <li>
              <a href="<?cs var:toroot ?>devices/tech/security/se-linux.html">
                <span class="en">Security-Enhanced Linux</span>
              </a>
            </li>

          </ul>
      </li>

      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/index.html">
            <span class="en">Trade Federation Testing Infrastructure</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/fundamentals/index.html"
            >Start Here</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/fundamentals/machine_setup.html"
            >Machine Setup</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/fundamentals/devices.html"
            >Working with Devices</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/fundamentals/lifecycle.html"
            >Test Lifecycle</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/fundamentals/options.html"
            >Option Handling</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/full_example.html"
            >An End-to-End Example</a></li>
          <li id="tradefed-tree-list" class="nav-section">
            <div class="nav-section-header">
              <a href="<?cs var:toroot ?>reference/packages.html">
            <span class="en">Reference</span>
          </a>
        <div>
      </li>
        </ul>
      </li>

    </ul>
  </li>

</ul>
