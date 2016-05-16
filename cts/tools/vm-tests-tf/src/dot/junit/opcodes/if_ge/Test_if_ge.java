package dot.junit.opcodes.if_ge;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.if_ge.d.T_if_ge_1;
import dot.junit.opcodes.if_ge.d.T_if_ge_3;

public class Test_if_ge extends DxTestCase {
    
    /**
     * @title Case: 5 < 6
     */
    public void testN1() {
        T_if_ge_1 t = new T_if_ge_1();
        assertEquals(1234, t.run(5, 6));
    }

    /**
     * @title Case: 0x0f0e0d0c = 0x0f0e0d0c
     */
    public void testN2() {
        T_if_ge_1 t = new T_if_ge_1();
        assertEquals(1, t.run(0x0f0e0d0c, 0x0f0e0d0c));
    }

    /**
     * @title Case: 5 > -5
     */
    public void testN3() {
        T_if_ge_1 t = new T_if_ge_1();
        assertEquals(1, t.run(5, -5));
    }

    /**
     * @title Arguments = Integer.MAX_VALUE, Integer.MAX_VALUE
     */
    public void testB1() {
        T_if_ge_1 t = new T_if_ge_1();
        assertEquals(1, t.run(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    /**
     * @title Arguments = Integer.MIN_VALUE, Integer.MAX_VALUE
     */
    public void testB2() {
        T_if_ge_1 t = new T_if_ge_1();
        assertEquals(1234, t.run(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }
    
    /**
     * @title Arguments = Integer.MAX_VALUE, Integer.MIN_VALUE
     */
    public void testB3() {
        T_if_ge_1 t = new T_if_ge_1();
        assertEquals(1, t.run(Integer.MAX_VALUE, Integer.MIN_VALUE));
    }

    /**
     * @title Arguments = 0, Integer.MIN_VALUE
     */
    public void testB4() {
        T_if_ge_1 t = new T_if_ge_1();
        assertEquals(1, t.run(0, Integer.MIN_VALUE));
    }

    /**
     * @title Arguments = 0, 0
     */
    public void testB5() {
        T_if_ge_1 t = new T_if_ge_1();
        assertEquals(1, t.run(0, 0));
    }
    
    /**
     * @constraint A23 
     * @title  number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.if_ge.d.T_if_ge_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    


    /**
     * @constraint B1 
     * @title  types of arguments - int, double
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.if_ge.d.T_if_ge_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title  types of arguments - long, int
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.if_ge.d.T_if_ge_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title  types of arguments - int, reference
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.if_ge.d.T_if_ge_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Types of arguments - int, float. The verifier checks that ints
     * and floats are not used interchangeably.
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.if_ge.d.T_if_ge_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

     /**
     * @constraint A6 
     * @title  branch target shall be inside the method
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.if_ge.d.T_if_ge_9");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A6 
     * @title  branch target shall not be "inside" instruction
     */
    public void testVFE7() {
        try {
            Class.forName("dot.junit.opcodes.if_ge.d.T_if_ge_10");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a 
     * @title  branch target shall 0
     */
    public void testVFE8() {
        try {
            Class.forName("dot.junit.opcodes.if_ge.d.T_if_ge_11");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
