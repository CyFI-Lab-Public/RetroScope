/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package benchmarks.regression;

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

/**
 * Many of these tests are bogus in that the cost will vary wildly depending on inputs.
 * For _my_ current purposes, that's okay. But beware!
 */
public class MathBenchmark extends SimpleBenchmark {
    private final double d = 1.2;
    private final float f = 1.2f;
    private final int i = 1;
    private final long l = 1L;

    public void timeAbsD(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.abs(d);
        }
    }

    public void timeAbsF(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.abs(f);
        }
    }

    public void timeAbsI(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.abs(i);
        }
    }

    public void timeAbsL(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.abs(l);
        }
    }

    public void timeAcos(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.acos(d);
        }
    }

    public void timeAsin(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.asin(d);
        }
    }

    public void timeAtan(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.atan(d);
        }
    }

    public void timeAtan2(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.atan2(3, 4);
        }
    }

    public void timeCbrt(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.cbrt(d);
        }
    }

    public void timeCeil(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.ceil(d);
        }
    }

    public void timeCopySignD(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.copySign(d, d);
        }
    }

    public void timeCopySignF(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.copySign(f, f);
        }
    }

    public void timeCopySignD_strict(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            StrictMath.copySign(d, d);
        }
    }

    public void timeCopySignF_strict(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            StrictMath.copySign(f, f);
        }
    }

    public void timeCos(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.cos(d);
        }
    }

    public void timeCosh(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.cosh(d);
        }
    }

    public void timeExp(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.exp(d);
        }
    }

    public void timeExpm1(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.expm1(d);
        }
    }

    public void timeFloor(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.floor(d);
        }
    }

    public void timeGetExponentD(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.getExponent(d);
        }
    }

    public void timeGetExponentF(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.getExponent(f);
        }
    }

    public void timeHypot(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.hypot(d, d);
        }
    }

    public void timeIEEEremainder(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.IEEEremainder(d, d);
        }
    }

    public void timeLog(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.log(d);
        }
    }

    public void timeLog10(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.log10(d);
        }
    }

    public void timeLog1p(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.log1p(d);
        }
    }

    public void timeMaxD(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.max(d, d);
        }
    }

    public void timeMaxF(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.max(f, f);
        }
    }

    public void timeMaxI(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.max(i, i);
        }
    }

    public void timeMaxL(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.max(l, l);
        }
    }

    public void timeMinD(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.min(d, d);
        }
    }

    public void timeMinF(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.min(f, f);
        }
    }

    public void timeMinI(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.min(i, i);
        }
    }

    public void timeMinL(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.min(l, l);
        }
    }

    public void timeNextAfterD(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.nextAfter(d, d);
        }
    }

    public void timeNextAfterF(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.nextAfter(f, f);
        }
    }

    public void timeNextUpD(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.nextUp(d);
        }
    }

    public void timeNextUpF(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.nextUp(f);
        }
    }

    public void timePow(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.pow(d, d);
        }
    }

    public void timeRandom(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.random();
        }
    }

    public void timeRint(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.rint(d);
        }
    }

    public void timeRoundD(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.round(d);
        }
    }

    public void timeRoundF(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.round(f);
        }
    }

    public void timeScalbD(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.scalb(d, 5);
        }
    }

    public void timeScalbF(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.scalb(f, 5);
        }
    }

    public void timeSignumD(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.signum(d);
        }
    }

    public void timeSignumF(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.signum(f);
        }
    }

    public void timeSin(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.sin(d);
        }
    }

    public void timeSinh(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.sinh(d);
        }
    }

    public void timeSqrt(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.sqrt(d);
        }
    }

    public void timeTan(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.tan(d);
        }
    }

    public void timeTanh(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.tanh(d);
        }
    }

    public void timeToDegrees(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.toDegrees(d);
        }
    }

    public void timeToRadians(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.toRadians(d);
        }
    }

    public void timeUlpD(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.ulp(d);
        }
    }

    public void timeUlpF(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            Math.ulp(f);
        }
    }
}
