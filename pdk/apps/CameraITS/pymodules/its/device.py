# Copyright 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import its.error
import os
import os.path
import sys
import re
import json
import tempfile
import time
import unittest
import subprocess

class ItsSession(object):
    """Controls a device over adb to run ITS scripts.

    The script importing this module (on the host machine) prepares JSON
    objects encoding CaptureRequests, specifying sets of parameters to use
    when capturing an image using the Camera2 APIs. This class encapsualtes
    sending the requests to the device, monitoring the device's progress, and
    copying the resultant captures back to the host machine when done.

    The device must have ItsService.apk installed.

    The "adb logcat" command is used to receive messages from the service
    running on the device.

    Attributes:
        proc: The handle to the process in which "adb logcat" is invoked.
        logcat: The stdout stream from the logcat process.
    """

    # TODO: Handle multiple connected devices.
    # The adb program is used for communication with the device. Need to handle
    # the case of multiple devices connected. Currently, uses the "-d" param
    # to adb, which causes it to fail if there is more than one device.
    ADB = "adb -d"

    # Set to True to take a pre-shot before capture and throw it away (for
    # debug purposes).
    CAPTURE_THROWAWAY_SHOTS = False

    DEVICE_FOLDER_ROOT = '/sdcard/its'
    DEVICE_FOLDER_CAPTURE = 'captures'
    INTENT_CAPTURE = 'com.android.camera2.its.CAPTURE'
    INTENT_3A = 'com.android.camera2.its.3A'
    INTENT_GETPROPS = 'com.android.camera2.its.GETPROPS'
    TAG = 'CAMERA-ITS-PY'

    MSG_RECV = "RECV"
    MSG_SIZE = "SIZE"
    MSG_FILE = "FILE"
    MSG_CAPT = "CAPT"
    MSG_DONE = "DONE"
    MSG_FAIL = "FAIL"
    MSG_AF   = "3A-F"
    MSG_AE   = "3A-E"
    MSG_AWB  = "3A-W"
    MSGS = [MSG_RECV, MSG_SIZE, MSG_FILE, MSG_CAPT, MSG_DONE,
            MSG_FAIL, MSG_AE,   MSG_AF,   MSG_AWB]

    def __init__(self):
        self.proc = None
        reboot_device_on_argv()
        self.__open_logcat()

    def __del__(self):
        self.__kill_logcat()

    def __enter__(self):
        return self

    def __exit__(self, type, value, traceback):
        return False

    def __open_logcat(self):
        """Opens the "adb logcat" stream.

        Internal function, called by this class's constructor.

        Gets the adb logcat stream that is intended for parsing by this python
        script. Flushes it first to clear out existing messages.

        Populates the proc and logcat members of this class.
        """
        _run('%s logcat -c' % (self.ADB))
        self.proc = subprocess.Popen(
                self.ADB.split() + ["logcat", "-s", "'%s:v'" % (self.TAG)],
                stdout=subprocess.PIPE)
        self.logcat = self.proc.stdout

    def __get_next_msg(self):
        """Gets the next message from the logcat stream.

        Reads from the logcat stdout stream. Blocks until a new line is ready,
        but exits in the event of a keyboard interrupt (to allow the script to
        be Ctrl-C killed).

        If the special message "FAIL" is received, kills the script; the test
        shouldn't continue running if something went wrong. The user can then
        manually inspect the device to see what the problem is, for example by
        looking at logcat themself.

        Returns:
            The next string from the logcat stdout stream.
        """
        while True:
            # Get the next logcat line.
            line = self.logcat.readline().strip()
            # Get the message, which is the string following the "###" code.
            idx = line.find('### ')
            if idx >= 0:
                msg = line[idx+4:]
                if self.__unpack_msg(msg)[0] == self.MSG_FAIL:
                    raise its.error.Error('FAIL device msg received')
                return msg

    def __kill_logcat(self):
        """Kill the logcat process.

        Internal function called by this class's destructor.
        """
        if self.proc:
            self.proc.kill()

    def __send_intent(self, intent_string, intent_params=None):
        """Send an intent to the device.

        Takes a Python object object specifying the operation to be performed
        on the device, converts it to JSON, sends it to the device over adb,
        then sends an intent to ItsService.apk running on the device with
        the path to that JSON file (including starting the service).

        Args:
            intent_string: The string corresponding to the intent to send (3A
                or capture).
            intent_params: A Python dictionary object containing the operations
                to perform; for a capture intent, the dict. contains either
                captureRequest or captureRequestList key, and for a 3A intent,
                the dictionary contains a 3A params key.
        """
        _run('%s shell mkdir -p "%s"' % (
             self.ADB, self.DEVICE_FOLDER_ROOT))
        intent_args = ""
        if intent_params:
            with tempfile.NamedTemporaryFile(
                    mode="w", suffix=".json", delete=False) as f:
                tmpfname = f.name
                f.write(json.dumps(intent_params))
            _run('%s push %s %s' % (
                 self.ADB, tmpfname, self.DEVICE_FOLDER_ROOT))
            os.remove(tmpfname)
            intent_args = ' -d "file://%s/%s"' % (
                      self.DEVICE_FOLDER_ROOT, os.path.basename(tmpfname))
        # TODO: Figure out why "--user 0" is needed, and fix the problem
        _run(('%s shell am startservice --user 0 -t text/plain '
              '-a %s%s') % (self.ADB, intent_string, intent_args))

    def __start_capture(self, request):
        self.__send_intent(self.INTENT_CAPTURE, request)

    def __start_3a(self, params):
        self.__send_intent(self.INTENT_3A, params)

    def __start_getprops(self):
        self.__send_intent(self.INTENT_GETPROPS)

    def __unpack_msg(self, msg):
        """Process a string containing a coded message from the device.

        The logcat messages intended to be parsed by this script are of the
        following form:
            RECV                    - Indicates capture command was received
            SIZE <WIDTH> <HEIGHT>   - The width,height of the captured image
            FILE <PATH>             - The path on the device of the captured image
            CAPT <I> of <N>         - Indicates capt cmd #I out of #N was issued
            DONE                    - Indicates the capture sequence completed
            FAIL                    - Indicates an error occurred

        Args:
            msg: The string message from the device.

        Returns:
            Tuple containing the message type (a string) and the message
            payload (a list).
        """
        a = msg.split()
        if a[0] not in self.MSGS:
            raise its.error.Error('Invalid device message: %s' % (msg))
        return a[0], a[1:]

    def __wait_for_camera_properties(self):
        """Block until the requested camera properties object is available.

        Monitors messages from the service on the device (via logcat), looking
        for special coded messages that indicate the status of the request.

        Returns:
            The remote path (on the device) where the camera properties JSON
            file is stored.
        """
        fname = None
        msg = self.__get_next_msg()
        if self.__unpack_msg(msg)[0] != self.MSG_RECV:
            raise its.error.Error('Device msg not RECV: %s' % (msg))
        while True:
            msg = self.__get_next_msg()
            msgtype, msgparams = self.__unpack_msg(msg)
            if msgtype == self.MSG_FILE:
                fname = msgparams[0]
            elif msgtype == self.MSG_DONE:
                return fname

    def __wait_for_capture_done_single(self):
        """Block until a single capture is done.

        Monitors messages from the service on the device (via logcat), looking
        for special coded messages that indicate the status of the captures.

        Returns:
            The remote path (on the device) where the image file was stored,
            along with the image's width and height.
        """
        fname = None
        w = None
        h = None
        msg = self.__get_next_msg()
        if self.__unpack_msg(msg)[0] != self.MSG_RECV:
            raise its.error.Error('Device msg not RECV: %s' % (msg))
        while True:
            msg = self.__get_next_msg()
            msgtype, msgparams = self.__unpack_msg(msg)
            if msgtype == self.MSG_SIZE:
                w = int(msgparams[0])
                h = int(msgparams[1])
            elif msgtype == self.MSG_FILE:
                fname = msgparams[0]
            elif msgtype == self.MSG_DONE:
                return fname, w, h

    def __wait_for_capture_done_burst(self, num_req):
        """Block until a burst of captures is done.

        Monitors messages from the service on the device (via logcat), looking
        for special coded messages that indicate the status of the captures.

        Args:
            num_req: The number of captures to wait for.

        Returns:
            The remote paths (on the device) where the image files were stored,
            along with their width and height.
        """
        fnames = []
        w = None
        h = None
        msg = self.__get_next_msg()
        if self.__unpack_msg(msg)[0] != self.MSG_RECV:
            raise its.error.Error('Device msg not RECV: %s' % (msg))
        while True:
            msg = self.__get_next_msg()
            msgtype, msgparams = self.__unpack_msg(msg)
            if msgtype == self.MSG_SIZE:
                w = int(msgparams[0])
                h = int(msgparams[1])
            elif msgtype == self.MSG_FILE:
                fnames.append(msgparams[0])
            elif msgtype == self.MSG_DONE:
                if len(fnames) != num_req or not w or not h:
                    raise its.error.Error('Missing FILE or SIZE device msg')
                return fnames, w, h

    def __get_json_path(self, image_fname):
        """Get the path of the JSON metadata file associated with an image.

        Args:
            image_fname: Path of the image file (local or remote).

        Returns:
            The path of the associated JSON metadata file, which has the same
            basename but different extension.
        """
        base, ext = os.path.splitext(image_fname)
        return base + ".json"

    def __copy_captured_files(self, remote_fnames):
        """Copy captured data from device back to host machine over adb.

        Copy captured images and associated metadata from the device to the
        host machine. The image and metadata files have the same basename, but
        different file extensions; the captured image is .yuv/.jpg/.raw, and
        the captured metadata is .json.

        File names are unique, as each has the timestamp of the capture in it.

        Deletes the files from the device after they have been transferred off.

        Args:
            remote_fnames: List of paths of the captured image files on the
                remote device.

        Returns:
            List of paths of captured image files on the local host machine
            (which is just in the current directory).
        """
        local_fnames = []
        for fname in remote_fnames:
            _run('%s pull %s .' % (self.ADB, fname))
            _run('%s pull %s .' % (
                       self.ADB, self.__get_json_path(fname)))
            local_fnames.append(os.path.basename(fname))
        _run('%s shell rm -rf %s/*' % (self.ADB, self.DEVICE_FOLDER_ROOT))
        return local_fnames

    def __parse_captured_json(self, local_fnames):
        """Parse the JSON objects that are returned alongside captured images.

        Args:
            local_fnames: List of paths of captured image on the local machine.

        Returns:
            List of Python objects obtained from loading the argument files
            and converting from the JSON object form to native Python.
        """
        return [json.load(open(self.__get_json_path(f))) for f in local_fnames]

    def get_camera_properties(self):
        """Get the camera properties object for the device.

        Returns:
            The Python dictionary object for the CameraProperties object.
        """
        self.__start_getprops()
        remote_fname = self.__wait_for_camera_properties()
        _run('%s pull %s .' % (self.ADB, remote_fname))
        local_fname = os.path.basename(remote_fname)
        return self.__parse_captured_json([local_fname])[0]['cameraProperties']

    def do_3a(self, region_ae, region_awb, region_af,
              do_ae=True, do_awb=True, do_af=True):
        """Perform a 3A operation on the device.

        Triggers some or all of AE, AWB, and AF, and returns once they have
        converged. Uses the vendor 3A that is implemented inside the HAL.

        Throws an assertion if 3A fails to converge.

        Args:
            region_ae: Normalized rect. (x,y,w,h) specifying the AE region.
            region_awb: Normalized rect. (x,y,w,h) specifying the AWB region.
            region_af: Normalized rect. (x,y,w,h) specifying the AF region.

        Returns:
            Five values:
            * AE sensitivity; None if do_ae is False
            * AE exposure time; None if do_ae is False
            * AWB gains (list); None if do_awb is False
            * AWB transform (list); None if do_awb is false
            * AF focus position; None if do_af is false
        """
        params = {"regions" : {"ae": region_ae,
                               "awb": region_awb,
                               "af": region_af },
                  "triggers": {"ae": do_ae,
                               "af": do_af } }
        print "Running vendor 3A on device"
        self.__start_3a(params)
        ae_sens = None
        ae_exp = None
        awb_gains = None
        awb_transform = None
        af_dist = None
        while True:
            msg = self.__get_next_msg()
            msgtype, msgparams = self.__unpack_msg(msg)
            if msgtype == self.MSG_AE:
                ae_sens = int(msgparams[0])
                ae_exp = int(msgparams[1])
            elif msgtype == self.MSG_AWB:
                awb_gains = [float(x) for x in msgparams[:4]]
                awb_transform = [float(x) for x in msgparams[4:]]
            elif msgtype == self.MSG_AF:
                af_dist = float(msgparams[0]) if msgparams[0] != "null" else 0
            elif msgtype == self.MSG_DONE:
                if (do_ae and ae_sens == None or do_awb and awb_gains == None
                                              or do_af and af_dist == None):
                    raise its.error.Error('3A failed to converge')
                return ae_sens, ae_exp, awb_gains, awb_transform, af_dist

    def do_capture(self, cap_request, out_surface=None, out_fname_prefix=None):
        """Issue capture request(s), and read back the image(s) and metadata.

        The main top-level function for capturing one or more images using the
        device. Captures a single image if cap_request is a single object, and
        captures a burst if it is a list of objects.

        The out_surface field can specify the width, height, and format of
        the captured image. The format may be "yuv" or "jpeg". The default is
        a YUV420 frame ("yuv") corresponding to a full sensor frame.

        Example of a single capture request:

            {
                "android.sensor.exposureTime": 100*1000*1000,
                "android.sensor.sensitivity": 100
            }

        Example of a list of capture requests:

            [
                {
                    "android.sensor.exposureTime": 100*1000*1000,
                    "android.sensor.sensitivity": 100
                },
                {
                    "android.sensor.exposureTime": 100*1000*1000,
                    "android.sensor.sensitivity": 200
                }
            ]

        Example of an output surface specification:

            {
                "width": 640,
                "height": 480,
                "format": "yuv"
            }

        Args:
            cap_request: The Python dict/list specifying the capture(s), which
                will be converted to JSON and sent to the device.
            out_fname_prefix: (Optionally) the file name prefix to use for the
                captured files. If this arg is present, then the captured files
                will be renamed appropriately.

        Returns:
            Four values:
            * The path or list of paths of the captured images (depending on
              whether the request was for a single or burst capture). The paths
              are on the host machine. The captured metadata file(s) have the
              same file names as their corresponding images, with a ".json"
              extension.
            * The width and height of the captured image(s). For a burst, all
              are the same size.
            * The Python dictionary or list of dictionaries (in the case of a
              burst capture) containing the returned capture result objects.
        """
        if not isinstance(cap_request, list):
            request = {"captureRequest" : cap_request}
            if out_surface is not None:
                request["outputSurface"] = out_surface
            if self.CAPTURE_THROWAWAY_SHOTS:
                print "Capturing throw-away image"
                self.__start_capture(request)
                self.__wait_for_capture_done_single()
            print "Capturing image"
            self.__start_capture(request)
            remote_fname, w, h = self.__wait_for_capture_done_single()
            local_fname = self.__copy_captured_files([remote_fname])[0]
            out_metadata_obj = self.__parse_captured_json([local_fname])[0]
            if out_fname_prefix:
                _, image_ext = os.path.splitext(local_fname)
                os.rename(local_fname, out_fname_prefix + image_ext)
                os.rename(self.__get_json_path(local_fname),
                          out_fname_prefix + ".json")
                local_fname = out_fname_prefix + image_ext
            return local_fname, w, h, out_metadata_obj["captureResult"]
        else:
            request = {"captureRequestList" : cap_request}
            if out_surface is not None:
                request["outputSurface"] = out_surface
            n = len(request['captureRequestList'])
            print "Capture burst of %d images" % (n)
            self.__start_capture(request)
            remote_fnames, w, h = self.__wait_for_capture_done_burst(n)
            local_fnames = self.__copy_captured_files(remote_fnames)
            out_metadata_objs = self.__parse_captured_json(local_fnames)
            for i in range(len(out_metadata_objs)):
                out_metadata_objs[i] = out_metadata_objs[i]["captureResult"]
            if out_fname_prefix is not None:
                for i in range(len(local_fnames)):
                    _, image_ext = os.path.splitext(local_fnames[i])
                    os.rename(local_fnames[i],
                              "%s-%04d%s" % (out_fname_prefix, i, image_ext))
                    os.rename(self.__get_json_path(local_fnames[i]),
                              "%s-%04d.json" % (out_fname_prefix, i))
                    local_fnames[i] = out_fname_prefix + image_ext
            return local_fnames, w, h, out_metadata_objs

def _run(cmd):
    """Replacement for os.system, with hiding of stdout+stderr messages.
    """
    with open(os.devnull, 'wb') as devnull:
        subprocess.check_call(
                cmd.split(), stdout=devnull, stderr=subprocess.STDOUT)

def reboot_device(sleep_duration=30):
    """Function to reboot a device and block until it is ready.

    Can be used at the start of a test to get the device into a known good
    state. Will disconnect any other adb sessions, so this function is not
    a part of the ItsSession class (which encapsulates a session with a
    device.)

    Args:
        sleep_duration: (Optional) the length of time to sleep (seconds) after
            the device comes online before returning; this gives the device
            time to finish booting.
    """
    print "Rebooting device"
    _run("%s reboot" % (ItsSession.ADB));
    _run("%s wait-for-device" % (ItsSession.ADB))
    time.sleep(sleep_duration)
    print "Reboot complete"

def reboot_device_on_argv():
    """Examine sys.argv, and reboot if the "reboot" arg is present.

    If the script command line contains either:

        reboot
        reboot=30

    then the device will be rebooted, and if the optional numeric arg is
    present, then that will be the sleep duration passed to the reboot
    call.

    Returns:
        Boolean, indicating whether the device was rebooted.
    """
    for s in sys.argv[1:]:
        if s[:6] == "reboot":
            if len(s) > 7 and s[6] == "=":
                duration = int(s[7:])
                reboot_device(duration)
            elif len(s) == 6:
                reboot_device()
            return True
    return False

class __UnitTest(unittest.TestCase):
    """Run a suite of unit tests on this module.
    """

    # TODO: Add some unit tests.
    None

if __name__ == '__main__':
    unittest.main()

