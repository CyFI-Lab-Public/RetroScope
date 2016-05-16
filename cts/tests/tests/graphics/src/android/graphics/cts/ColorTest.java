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
package android.graphics.cts;

import android.graphics.Color;
import android.test.AndroidTestCase;

public class ColorTest extends AndroidTestCase {

    public void testAlpha(){
        assertEquals(0xff, Color.alpha(Color.RED));
        assertEquals(0xff, Color.alpha(Color.YELLOW));
        new Color();
    }

    public void testArgb(){
        assertEquals(Color.RED, Color.argb(0xff, 0xff, 0x00, 0x00));
        assertEquals(Color.YELLOW, Color.argb(0xff, 0xff, 0xff, 0x00));
    }

    public void testBlue(){
        assertEquals(0x00, Color.blue(Color.RED));
        assertEquals(0x00, Color.blue(Color.YELLOW));
    }

    public void testGreen(){
        assertEquals(0x00, Color.green(Color.RED));
        assertEquals(0xff, Color.green(Color.GREEN));
    }

    public void testHSVToColor1(){
        //abnormal case: hsv length less than 3
        try{
            float[] hsv = new float[2];
            Color.HSVToColor(hsv);
            fail("shouldn't come to here");
        }catch(RuntimeException e){
            //expected
        }

        float[] hsv = new float[3];
        Color.colorToHSV(Color.RED, hsv);
        assertEquals(Color.RED, Color.HSVToColor(hsv));
    }

    public void testHSVToColor2(){
        //abnormal case: hsv length less than 3
        try{
            float[] hsv = new float[2];
            Color.HSVToColor(hsv);
            fail("shouldn't come to here");
        }catch(RuntimeException e){
            //expected
        }

        float[] hsv = new float[3];
        Color.colorToHSV(Color.RED, hsv);
        assertEquals(Color.RED, Color.HSVToColor(0xff, hsv));
    }

    public void testParseColor(){
        //abnormal case: colorString starts with '#' but length is neither 7 nor 9
        try{
            Color.parseColor("#ff00ff0");
            fail("should come to here");
        }catch(IllegalArgumentException e){
            //expected
        }

        assertEquals(Color.RED, Color.parseColor("#ff0000"));
        assertEquals(Color.RED, Color.parseColor("#ffff0000"));

        //abnormal case: colorString doesn't start with '#' and is unknown color
        try{
            Color.parseColor("hello");
            fail("should come to here");
        }catch(IllegalArgumentException e){
            //expected
        }

        assertEquals(Color.BLACK, Color.parseColor("black"));
        assertEquals(Color.DKGRAY, Color.parseColor("darkgray"));
        assertEquals(Color.GRAY, Color.parseColor("gray"));
        assertEquals(Color.LTGRAY, Color.parseColor("lightgray"));
        assertEquals(Color.WHITE, Color.parseColor("white"));
        assertEquals(Color.RED, Color.parseColor("red"));
        assertEquals(Color.GREEN, Color.parseColor("green"));
        assertEquals(Color.BLUE, Color.parseColor("blue"));
        assertEquals(Color.YELLOW, Color.parseColor("yellow"));
        assertEquals(Color.CYAN, Color.parseColor("cyan"));
        assertEquals(Color.MAGENTA, Color.parseColor("magenta"));
    }

    public void testRed(){
        assertEquals(0xff, Color.red(Color.RED));
        assertEquals(0xff, Color.red(Color.YELLOW));
    }

    public void testRgb(){
        assertEquals(Color.RED, Color.rgb(0xff, 0x00, 0x00));
        assertEquals(Color.YELLOW, Color.rgb(0xff, 0xff, 0x00));
    }

    public void testRGBToHSV(){
        //abnormal case: hsv length less than 3
        try{
            float[] hsv = new float[2];
            Color.RGBToHSV(0xff, 0x00, 0x00, hsv);
            fail("shouldn't come to here");
        }catch(RuntimeException e){
            //expected
        }

        float[] hsv = new float[3];
        Color.RGBToHSV(0xff, 0x00, 0x00, hsv);
        assertEquals(Color.RED, Color.HSVToColor(hsv));
    }
}
