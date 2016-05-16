package dot.junit.opcodes.shr_int_lit8;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.shr_int_lit8.d.T_shr_int_lit8_1;
import dot.junit.opcodes.shr_int_lit8.d.T_shr_int_lit8_2;
import dot.junit.opcodes.shr_int_lit8.d.T_shr_int_lit8_3;
import dot.junit.opcodes.shr_int_lit8.d.T_shr_int_lit8_4;
import dot.junit.opcodes.shr_int_lit8.d.T_shr_int_lit8_5;
import dot.junit.opcodes.shr_int_lit8.d.T_shr_int_lit8_6;

public class Test_shr_int_lit8 extends DxTestCase {

    /**
     * @title 15 >> 1
     */
    public void testN1() {
        T_shr_int_lit8_1 t = new T_shr_int_lit8_1();
        assertEquals(7, t.run(15));
    }

    /**
     * @title 33 >> 2
     */
    public void testN2() {
        T_shr_int_lit8_2 t = new T_shr_int_lit8_2();
        assertEquals(8, t.run(33));
    }

    /**
     * @title -15 >> 1
     */
    public void testN3() {
        T_shr_int_lit8_1 t = new T_shr_int_lit8_1();
        assertEquals(-8, t.run(-15));
    }

    /**
     * @title Arguments = 1 & -1
     */
    public void testN4() {
        T_shr_int_lit8_3 t = new T_shr_int_lit8_3();
        assertEquals(0, t.run(1));
    }

    /**
     * @title Verify that shift distance is actually in range 0 to 32.
     */
    public void testN5() {
        T_shr_int_lit8_4 t = new T_shr_int_lit8_4();
        assertEquals(16, t.run(33));
    }


    /**
     * @title Arguments = 0 & -1
     */
    public void testB1() {
        T_shr_int_lit8_3 t = new T_shr_int_lit8_3();
        assertEquals(0, t.run(0));
    }

    /**
     * @title Arguments = Integer.MAX_VALUE & 1
     */
    public void testB2() {
        T_shr_int_lit8_1 t = new T_shr_int_lit8_1();
        assertEquals(0x3FFFFFFF, t.run(Integer.MAX_VALUE));
    }

    /**
     * @title Arguments = Integer.MIN_VALUE & 1
     */
    public void testB3() {
        T_shr_int_lit8_1 t = new T_shr_int_lit8_1();
        assertEquals(0xc0000000, t.run(Integer.MIN_VALUE));
    }

    /**
     * @title Arguments = 1 & 0
     */
    public void testB4() {
        T_shr_int_lit8_5 t = new T_shr_int_lit8_5();
        assertEquals(1, t.run(1));
    }

    /**
     * @constraint A23 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.shr_int_lit8.d.T_shr_int_lit8_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    

    /**
     * @constraint B1 
     * @title types of arguments - double, int
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.shr_int_lit8.d.T_shr_int_lit8_8");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - long, int
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.shr_int_lit8.d.T_shr_int_lit8_9");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - reference, int
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.shr_int_lit8.d.T_shr_int_lit8_10");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Types of arguments - float. The verifier checks that ints
     * and floats are not used interchangeably.
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.shr_int_lit8.d.T_shr_int_lit8_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
