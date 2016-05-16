package dot.junit.opcodes.shl_long;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.shl_long.d.T_shl_long_1;
import dot.junit.opcodes.shl_long.d.T_shl_long_7;

public class Test_shl_long extends DxTestCase {

    /**
     * @title Arguments = 5000000000l, 3
     */
    public void testN1() {
        T_shl_long_1 t = new T_shl_long_1();
        assertEquals(40000000000l, t.run(5000000000l, 3));
    }

    /**
     * @title Arguments = 5000000000l, 1
     */
    public void testN2() {
        T_shl_long_1 t = new T_shl_long_1();
        assertEquals(10000000000l, t.run(5000000000l, 1));
    }

    /**
     * @title Arguments = -5000000000l, 1
     */
    public void testN3() {
        T_shl_long_1 t = new T_shl_long_1();
        assertEquals(-10000000000l, t.run(-5000000000l, 1));
    }

    /**
     * @title Arguments = 1 & -1
     */
    public void testN4() {
        T_shl_long_1 t = new T_shl_long_1();
        assertEquals(0x8000000000000000l, t.run(1l, -1));
    }

    /**
     * @title Verify that shift distance is actually in range 0 to 64.
     */
    public void testN5() {
        T_shl_long_1 t = new T_shl_long_1();
        assertEquals(130l, t.run(65l, 65));
    }



    /**
     * @title Arguments = 0 & -1
     */
    public void testB1() {
        T_shl_long_1 t = new T_shl_long_1();
        assertEquals(0, t.run(0, -1));
    }

    /**
     * @title Arguments = 1 & 0
     */
    public void testB2() {
        T_shl_long_1 t = new T_shl_long_1();
        assertEquals(1, t.run(1, 0));
    }

    /**
     * @title Arguments = Long.MAX_VALUE & 1
     */
    public void testB3() {
        T_shl_long_1 t = new T_shl_long_1();
        assertEquals(0xfffffffe, t.run(Long.MAX_VALUE, 1));
    }

    /**
     * @title Arguments = Long.MIN_VALUE & 1
     */
    public void testB4() {
        T_shl_long_1 t = new T_shl_long_1();
        assertEquals(0l, t.run(Long.MIN_VALUE, 1));
    }

    /**
     * @constraint A24 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.shl_long.d.T_shl_long_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    

    /**
     * @constraint B1 
     * @title types of arguments - long & double
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.shl_long.d.T_shl_long_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - int & int
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.shl_long.d.T_shl_long_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - float & int
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.shl_long.d.T_shl_long_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title  types of arguments - reference & int
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.shl_long.d.T_shl_long_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Types of arguments - double, int. The verifier checks that longs
     * and doubles are not used interchangeably.
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.shl_long.d.T_shl_long_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
