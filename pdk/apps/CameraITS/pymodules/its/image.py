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

import matplotlib
matplotlib.use('Agg')

import its.error
import pylab
import sys
import Image
import numpy
import math
import unittest

DEFAULT_YUV_TO_RGB_CCM = numpy.matrix([
                                [1.000,  1.402,  0.000],
                                [1.000, -0.714, -0.344],
                                [1.000,  0.000,  1.772]])

DEFAULT_YUV_OFFSETS = numpy.array([0, 128, 128])

DEFAULT_GAMMA_LUT = numpy.array(
        [math.floor(65535 * math.pow(i/65535.0, 1/2.2) + 0.5) for i in xrange(65536)])

DEFAULT_INVGAMMA_LUT = numpy.array(
        [math.floor(65535 * math.pow(i/65535.0, 2.2) + 0.5) for i in xrange(65536)])

MAX_LUT_SIZE = 65536

def load_yuv420_to_rgb_image(yuv_fname,
                             w, h,
                             ccm_yuv_to_rgb=DEFAULT_YUV_TO_RGB_CCM,
                             yuv_off=DEFAULT_YUV_OFFSETS):
    """Load a YUV420 image file, and return as an RGB image.

    Args:
        yuv_fname: The path of the YUV420 file.
        w: The width of the image.
        h: The height of the image.
        ccm_yuv_to_rgb: (Optional) the 3x3 CCM to convert from YUV to RGB.
        yuv_off: (Optional) offsets to subtract from each of Y,U,V values.

    Returns:
        RGB float-3 image array, with pixel values in [0.0, 1.0].
    """
    with open(yuv_fname, "rb") as f:
        y = numpy.fromfile(f, numpy.uint8, w*h, "")
        v = numpy.fromfile(f, numpy.uint8, w*h/4, "")
        u = numpy.fromfile(f, numpy.uint8, w*h/4, "")
        y = numpy.subtract(y, yuv_off[0])
        u = numpy.subtract(u, yuv_off[1]).view(numpy.int8)
        v = numpy.subtract(v, yuv_off[2]).view(numpy.int8)
        u = u.reshape(h/2, w/2).repeat(2, axis=1).repeat(2, axis=0)
        v = v.reshape(h/2, w/2).repeat(2, axis=1).repeat(2, axis=0)
        yuv = numpy.dstack([y, u.reshape(w*h), v.reshape(w*h)])
        flt = numpy.empty([h, w, 3], dtype=numpy.float32)
        flt.reshape(w*h*3)[:] = yuv.reshape(h*w*3)[:]
        flt = numpy.dot(flt.reshape(w*h,3), ccm_yuv_to_rgb.T).clip(0, 255)
        rgb = numpy.empty([h, w, 3], dtype=numpy.uint8)
        rgb.reshape(w*h*3)[:] = flt.reshape(w*h*3)[:]
        return rgb.astype(numpy.float32) / 255.0

def load_yuv420_to_yuv_planes(yuv_fname, w, h):
    """Load a YUV420 image file, and return separate Y, U, and V plane images.

    Args:
        yuv_fname: The path of the YUV420 file.
        w: The width of the image.
        h: The height of the image.

    Returns:
        Separate Y, U, and V images as float-1 Numpy arrays, pixels in [0,1].
        Note that pixel (0,0,0) is not black, since U,V pixels are centered at
        0.5, and also that the Y and U,V plane images returned are different
        sizes (due to chroma subsampling in the YUV420 format).
    """
    with open(yuv_fname, "rb") as f:
        y = numpy.fromfile(f, numpy.uint8, w*h, "")
        v = numpy.fromfile(f, numpy.uint8, w*h/4, "")
        u = numpy.fromfile(f, numpy.uint8, w*h/4, "")
        return ((y.astype(numpy.float32) / 255.0).reshape(h, w, 1),
                (u.astype(numpy.float32) / 255.0).reshape(h/2, w/2, 1),
                (v.astype(numpy.float32) / 255.0).reshape(h/2, w/2, 1))

def apply_lut_to_image(img, lut):
    """Applies a LUT to every pixel in a float image array.

    Internally converts to a 16b integer image, since the LUT can work with up
    to 16b->16b mappings (i.e. values in the range [0,65535]). The lut can also
    have fewer than 65536 entries, however it must be sized as a power of 2
    (and for smaller luts, the scale must match the bitdepth).

    For a 16b lut of 65536 entries, the operation performed is:

        lut[r * 65535] / 65535 -> r'
        lut[g * 65535] / 65535 -> g'
        lut[b * 65535] / 65535 -> b'

    For a 10b lut of 1024 entries, the operation becomes:

        lut[r * 1023] / 1023 -> r'
        lut[g * 1023] / 1023 -> g'
        lut[b * 1023] / 1023 -> b'

    Args:
        img: Numpy float image array, with pixel values in [0,1].
        lut: Numpy table encoding a LUT, mapping 16b integer values.

    Returns:
        Float image array after applying LUT to each pixel.
    """
    n = len(lut)
    if n <= 0 or n > MAX_LUT_SIZE or (n & (n - 1)) != 0:
        raise its.error.Error('Invalid arg LUT size: %d' % (n))
    m = float(n-1)
    return (lut[(img * m).astype(numpy.uint16)] / m).astype(numpy.float32)

def apply_matrix_to_image(img, mat):
    """Multiplies a 3x3 matrix with each float-3 image pixel.

    Each pixel is considered a column vector, and is left-multiplied by
    the given matrix:

        [     ]   r    r'
        [ mat ] * g -> g'
        [     ]   b    b'

    Args:
        img: Numpy float image array, with pixel values in [0,1].
        mat: Numpy 3x3 matrix.

    Returns:
        The numpy float-3 image array resulting from the matrix mult.
    """
    h = img.shape[0]
    w = img.shape[1]
    img2 = numpy.empty([h, w, 3], dtype=numpy.float32)
    img2.reshape(w*h*3)[:] = (numpy.dot(img.reshape(h*w, 3), mat.T)
                             ).reshape(w*h*3)[:]
    return img2

def get_image_patch(img, xnorm, ynorm, wnorm, hnorm):
    """Get a patch (tile) of an image.

    Args:
        img: Numpy float image array, with pixel values in [0,1].
        xnorm,ynorm,wnorm,hnorm: Normalized (in [0,1]) coords for the tile.

    Returns:
        Float image array of the patch.
    """
    hfull = img.shape[0]
    wfull = img.shape[1]
    xtile = math.ceil(xnorm * wfull)
    ytile = math.ceil(ynorm * hfull)
    wtile = math.floor(wnorm * wfull)
    htile = math.floor(hnorm * hfull)
    return img[ytile:ytile+htile,xtile:xtile+wtile,:].copy()

def compute_image_means(img):
    """Calculate the mean of each color channel in the image.

    Args:
        img: Numpy float image array, with pixel values in [0,1].

    Returns:
        A list of mean values, one per color channel in the image.
    """
    means = []
    chans = img.shape[2]
    for i in xrange(chans):
        means.append(numpy.mean(img[:,:,i], dtype=numpy.float64))
    return means

def compute_image_variances(img):
    """Calculate the variance of each color channel in the image.

    Args:
        img: Numpy float image array, with pixel values in [0,1].

    Returns:
        A list of mean values, one per color channel in the image.
    """
    variances = []
    chans = img.shape[2]
    for i in xrange(chans):
        variances.append(numpy.var(img[:,:,i], dtype=numpy.float64))
    return variances

def write_image(img, fname, apply_gamma=False):
    """Save a float-3 numpy array image to a file.

    Supported formats: PNG, JPEG, and others; see PIL docs for more.

    Image can be 3-channel, which is interpreted as RGB, or can be 1-channel,
    which is greyscale.

    Can optionally specify that the image should be gamma-encoded prior to
    writing it out; this should be done if the image contains linear pixel
    values, to make the image look "normal".

    Args:
        img: Numpy image array data.
        fname: Path of file to save to; the extension specifies the format.
        apply_gamma: (Optional) apply gamma to the image prior to writing it.
    """
    if apply_gamma:
        img = apply_lut_to_image(img, DEFAULT_GAMMA_LUT)
    (h, w, chans) = img.shape
    if chans == 3:
        Image.fromarray((img * 255.0).astype(numpy.uint8), "RGB").save(fname)
    elif chans == 1:
        img3 = (img * 255.0).astype(numpy.uint8).repeat(3).reshape(h,w,3)
        Image.fromarray(img3, "RGB").save(fname)
    else:
        raise its.error.Error('Unsupported image type')

class __UnitTest(unittest.TestCase):
    """Run a suite of unit tests on this module.
    """

    # TODO: Add more unit tests.

    def test_apply_matrix_to_image(self):
        """Unit test for apply_matrix_to_image.

        Test by using a canned set of values on a 1x1 pixel image.

            [ 1 2 3 ]   [ 0.1 ]   [ 1.4 ]
            [ 4 5 6 ] * [ 0.2 ] = [ 3.2 ]
            [ 7 8 9 ]   [ 0.3 ]   [ 5.0 ]
               mat         x         y
        """
        mat = numpy.array([[1,2,3],[4,5,6],[7,8,9]])
        x = numpy.array([0.1,0.2,0.3]).reshape(1,1,3)
        y = apply_matrix_to_image(x, mat).reshape(3).tolist()
        y_ref = [1.4,3.2,5.0]
        passed = all([math.fabs(y[i] - y_ref[i]) < 0.001 for i in xrange(3)])
        self.assertTrue(passed)

    def test_apply_lut_to_image(self):
        """ Unit test for apply_lut_to_image.

        Test by using a canned set of values on a 1x1 pixel image. The LUT will
        simply double the value of the index:

            lut[x] = 2*x
        """
        lut = numpy.array([2*i for i in xrange(65536)])
        x = numpy.array([0.1,0.2,0.3]).reshape(1,1,3)
        y = apply_lut_to_image(x, lut).reshape(3).tolist()
        y_ref = [0.2,0.4,0.6]
        passed = all([math.fabs(y[i] - y_ref[i]) < 0.001 for i in xrange(3)])
        self.assertTrue(passed)

if __name__ == '__main__':
    unittest.main()

