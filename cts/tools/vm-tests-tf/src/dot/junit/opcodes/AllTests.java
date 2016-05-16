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

package dot.junit.opcodes;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Listing of all the tests that are to be run.
 */
public class AllTests {

    public static void run() {
        TestRunner.main(new String[] {AllTests.class.getName()});
    }

    public static final Test suite() {
        TestSuite suite = new TestSuite("Tests for all dalvik vm opcodes");
        suite.addTestSuite(dot.junit.opcodes.add_double_2addr.Test_add_double_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.add_double.Test_add_double.class);
        suite.addTestSuite(dot.junit.opcodes.add_float_2addr.Test_add_float_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.add_float.Test_add_float.class);
        suite.addTestSuite(dot.junit.opcodes.add_int_2addr.Test_add_int_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.add_int_lit16.Test_add_int_lit16.class);
        suite.addTestSuite(dot.junit.opcodes.add_int_lit8.Test_add_int_lit8.class);
        suite.addTestSuite(dot.junit.opcodes.add_int.Test_add_int.class);
        suite.addTestSuite(dot.junit.opcodes.add_long_2addr.Test_add_long_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.add_long.Test_add_long.class);
        suite.addTestSuite(dot.junit.opcodes.aget_boolean.Test_aget_boolean.class);
        suite.addTestSuite(dot.junit.opcodes.aget_byte.Test_aget_byte.class);
        suite.addTestSuite(dot.junit.opcodes.aget_char.Test_aget_char.class);
        suite.addTestSuite(dot.junit.opcodes.aget_object.Test_aget_object.class);
        suite.addTestSuite(dot.junit.opcodes.aget_short.Test_aget_short.class);
        suite.addTestSuite(dot.junit.opcodes.aget.Test_aget.class);
        suite.addTestSuite(dot.junit.opcodes.aget_wide.Test_aget_wide.class);
        suite.addTestSuite(dot.junit.opcodes.and_int_2addr.Test_and_int_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.and_int_lit16.Test_and_int_lit16.class);
        suite.addTestSuite(dot.junit.opcodes.and_int_lit8.Test_and_int_lit8.class);
        suite.addTestSuite(dot.junit.opcodes.and_int.Test_and_int.class);
        suite.addTestSuite(dot.junit.opcodes.and_long_2addr.Test_and_long_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.and_long.Test_and_long.class);
        suite.addTestSuite(dot.junit.opcodes.aput_boolean.Test_aput_boolean.class);
        suite.addTestSuite(dot.junit.opcodes.aput_byte.Test_aput_byte.class);
        suite.addTestSuite(dot.junit.opcodes.aput_char.Test_aput_char.class);
        suite.addTestSuite(dot.junit.opcodes.aput_object.Test_aput_object.class);
        suite.addTestSuite(dot.junit.opcodes.aput_short.Test_aput_short.class);
        suite.addTestSuite(dot.junit.opcodes.aput.Test_aput.class);
        suite.addTestSuite(dot.junit.opcodes.aput_wide.Test_aput_wide.class);
        suite.addTestSuite(dot.junit.opcodes.array_length.Test_array_length.class);
        suite.addTestSuite(dot.junit.opcodes.check_cast.Test_check_cast.class);
        suite.addTestSuite(dot.junit.opcodes.cmpg_double.Test_cmpg_double.class);
        suite.addTestSuite(dot.junit.opcodes.cmpg_float.Test_cmpg_float.class);
        suite.addTestSuite(dot.junit.opcodes.cmpl_double.Test_cmpl_double.class);
        suite.addTestSuite(dot.junit.opcodes.cmpl_float.Test_cmpl_float.class);
        suite.addTestSuite(dot.junit.opcodes.cmp_long.Test_cmp_long.class);
        suite.addTestSuite(dot.junit.opcodes.const_16.Test_const_16.class);
        suite.addTestSuite(dot.junit.opcodes.const_4.Test_const_4.class);
        suite.addTestSuite(dot.junit.opcodes.const_class.Test_const_class.class);
        suite.addTestSuite(dot.junit.opcodes.const_high16.Test_const_high16.class);
        suite.addTestSuite(dot.junit.opcodes.const_string_jumbo.Test_const_string_jumbo.class);
        suite.addTestSuite(dot.junit.opcodes.const_string.Test_const_string.class);
        suite.addTestSuite(dot.junit.opcodes.const_wide_16.Test_const_wide_16.class);
        suite.addTestSuite(dot.junit.opcodes.const_wide_32.Test_const_wide_32.class);
        suite.addTestSuite(dot.junit.opcodes.const_wide_high16.Test_const_wide_high16.class);
        suite.addTestSuite(dot.junit.opcodes.const_wide.Test_const_wide.class);
        suite.addTestSuite(dot.junit.opcodes.div_double_2addr.Test_div_double_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.div_double.Test_div_double.class);
        suite.addTestSuite(dot.junit.opcodes.div_float_2addr.Test_div_float_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.div_float.Test_div_float.class);
        suite.addTestSuite(dot.junit.opcodes.div_int_2addr.Test_div_int_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.div_int_lit16.Test_div_int_lit16.class);
        suite.addTestSuite(dot.junit.opcodes.div_int_lit8.Test_div_int_lit8.class);
        suite.addTestSuite(dot.junit.opcodes.div_int.Test_div_int.class);
        suite.addTestSuite(dot.junit.opcodes.div_long_2addr.Test_div_long_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.div_long.Test_div_long.class);
        suite.addTestSuite(dot.junit.opcodes.double_to_float.Test_double_to_float.class);
        suite.addTestSuite(dot.junit.opcodes.double_to_int.Test_double_to_int.class);
        suite.addTestSuite(dot.junit.opcodes.double_to_long.Test_double_to_long.class);
        suite.addTestSuite(dot.junit.opcodes.fill_array_data.Test_fill_array_data.class);
        suite.addTestSuite(dot.junit.opcodes.filled_new_array_range.Test_filled_new_array_range.class);
        suite.addTestSuite(dot.junit.opcodes.filled_new_array.Test_filled_new_array.class);
        suite.addTestSuite(dot.junit.opcodes.float_to_double.Test_float_to_double.class);
        suite.addTestSuite(dot.junit.opcodes.float_to_int.Test_float_to_int.class);
        suite.addTestSuite(dot.junit.opcodes.float_to_long.Test_float_to_long.class);
        suite.addTestSuite(dot.junit.opcodes.goto_16.Test_goto_16.class);
        suite.addTestSuite(dot.junit.opcodes.goto_32.Test_goto_32.class);
        suite.addTestSuite(dot.junit.opcodes.if_eq.Test_if_eq.class);
        suite.addTestSuite(dot.junit.opcodes.if_eqz.Test_if_eqz.class);
        suite.addTestSuite(dot.junit.opcodes.if_ge.Test_if_ge.class);
        suite.addTestSuite(dot.junit.opcodes.if_gez.Test_if_gez.class);
        suite.addTestSuite(dot.junit.opcodes.if_gt.Test_if_gt.class);
        suite.addTestSuite(dot.junit.opcodes.if_gtz.Test_if_gtz.class);
        suite.addTestSuite(dot.junit.opcodes.if_le.Test_if_le.class);
        suite.addTestSuite(dot.junit.opcodes.if_lez.Test_if_lez.class);
        suite.addTestSuite(dot.junit.opcodes.if_lt.Test_if_lt.class);
        suite.addTestSuite(dot.junit.opcodes.if_ltz.Test_if_ltz.class);
        suite.addTestSuite(dot.junit.opcodes.if_ne.Test_if_ne.class);
        suite.addTestSuite(dot.junit.opcodes.if_nez.Test_if_nez.class);
        suite.addTestSuite(dot.junit.opcodes.iget_boolean.Test_iget_boolean.class);
        suite.addTestSuite(dot.junit.opcodes.iget_byte.Test_iget_byte.class);
        suite.addTestSuite(dot.junit.opcodes.iget_char.Test_iget_char.class);
        suite.addTestSuite(dot.junit.opcodes.iget_object.Test_iget_object.class);
        suite.addTestSuite(dot.junit.opcodes.iget_short.Test_iget_short.class);
        suite.addTestSuite(dot.junit.opcodes.iget.Test_iget.class);
        suite.addTestSuite(dot.junit.opcodes.iget_wide.Test_iget_wide.class);
        suite.addTestSuite(dot.junit.opcodes.instance_of.Test_instance_of.class);
        suite.addTestSuite(dot.junit.opcodes.int_to_byte.Test_int_to_byte.class);
        suite.addTestSuite(dot.junit.opcodes.int_to_char.Test_int_to_char.class);
        suite.addTestSuite(dot.junit.opcodes.int_to_double.Test_int_to_double.class);
        suite.addTestSuite(dot.junit.opcodes.int_to_float.Test_int_to_float.class);
        suite.addTestSuite(dot.junit.opcodes.int_to_long.Test_int_to_long.class);
        suite.addTestSuite(dot.junit.opcodes.int_to_short.Test_int_to_short.class);
        suite.addTestSuite(dot.junit.opcodes.invoke_direct_range.Test_invoke_direct_range.class);
        suite.addTestSuite(dot.junit.opcodes.invoke_direct.Test_invoke_direct.class);
        suite.addTestSuite(dot.junit.opcodes.invoke_interface_range.Test_invoke_interface_range.class);
        suite.addTestSuite(dot.junit.opcodes.invoke_interface.Test_invoke_interface.class);
        suite.addTestSuite(dot.junit.opcodes.invoke_static_range.Test_invoke_static_range.class);
        suite.addTestSuite(dot.junit.opcodes.invoke_static.Test_invoke_static.class);
        suite.addTestSuite(dot.junit.opcodes.invoke_super_range.Test_invoke_super_range.class);
        suite.addTestSuite(dot.junit.opcodes.invoke_super.Test_invoke_super.class);
        suite.addTestSuite(dot.junit.opcodes.invoke_virtual_range.Test_invoke_virtual_range.class);
        suite.addTestSuite(dot.junit.opcodes.invoke_virtual.Test_invoke_virtual.class);
        suite.addTestSuite(dot.junit.opcodes.iput_boolean.Test_iput_boolean.class);
        suite.addTestSuite(dot.junit.opcodes.iput_byte.Test_iput_byte.class);
        suite.addTestSuite(dot.junit.opcodes.iput_char.Test_iput_char.class);
        suite.addTestSuite(dot.junit.opcodes.iput_object.Test_iput_object.class);
        suite.addTestSuite(dot.junit.opcodes.iput_short.Test_iput_short.class);
        suite.addTestSuite(dot.junit.opcodes.iput.Test_iput.class);
        suite.addTestSuite(dot.junit.opcodes.iput_wide.Test_iput_wide.class);
        suite.addTestSuite(dot.junit.opcodes.long_to_double.Test_long_to_double.class);
        suite.addTestSuite(dot.junit.opcodes.long_to_float.Test_long_to_float.class);
        suite.addTestSuite(dot.junit.opcodes.long_to_int.Test_long_to_int.class);
        suite.addTestSuite(dot.junit.opcodes.monitor_enter.Test_monitor_enter.class);
        suite.addTestSuite(dot.junit.opcodes.monitor_exit.Test_monitor_exit.class);
        suite.addTestSuite(dot.junit.opcodes.move_16.Test_move_16.class);
        suite.addTestSuite(dot.junit.opcodes.move_exception.Test_move_exception.class);
        suite.addTestSuite(dot.junit.opcodes.move_from16.Test_move_from16.class);
        suite.addTestSuite(dot.junit.opcodes.move_object_16.Test_move_object_16.class);
        suite.addTestSuite(dot.junit.opcodes.move_object_from16.Test_move_object_from16.class);
        suite.addTestSuite(dot.junit.opcodes.move_object.Test_move_object.class);
        suite.addTestSuite(dot.junit.opcodes.move_result_object.Test_move_result_object.class);
        suite.addTestSuite(dot.junit.opcodes.move_result.Test_move_result.class);
        suite.addTestSuite(dot.junit.opcodes.move_result_wide.Test_move_result_wide.class);
        suite.addTestSuite(dot.junit.opcodes.move.Test_move.class);
        suite.addTestSuite(dot.junit.opcodes.move_wide_16.Test_move_wide_16.class);
        suite.addTestSuite(dot.junit.opcodes.move_wide_from16.Test_move_wide_from16.class);
        suite.addTestSuite(dot.junit.opcodes.move_wide.Test_move_wide.class);
        suite.addTestSuite(dot.junit.opcodes.mul_double_2addr.Test_mul_double_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.mul_double.Test_mul_double.class);
        suite.addTestSuite(dot.junit.opcodes.mul_float_2addr.Test_mul_float_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.mul_float.Test_mul_float.class);
        suite.addTestSuite(dot.junit.opcodes.mul_int_2addr.Test_mul_int_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.mul_int_lit16.Test_mul_int_lit16.class);
        suite.addTestSuite(dot.junit.opcodes.mul_int_lit8.Test_mul_int_lit8.class);
        suite.addTestSuite(dot.junit.opcodes.mul_int.Test_mul_int.class);
        suite.addTestSuite(dot.junit.opcodes.mul_long_2addr.Test_mul_long_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.mul_long.Test_mul_long.class);
        suite.addTestSuite(dot.junit.opcodes.neg_double.Test_neg_double.class);
        suite.addTestSuite(dot.junit.opcodes.neg_float.Test_neg_float.class);
        suite.addTestSuite(dot.junit.opcodes.neg_int.Test_neg_int.class);
        suite.addTestSuite(dot.junit.opcodes.neg_long.Test_neg_long.class);
        suite.addTestSuite(dot.junit.opcodes.new_array.Test_new_array.class);
        suite.addTestSuite(dot.junit.opcodes.new_instance.Test_new_instance.class);
        suite.addTestSuite(dot.junit.opcodes.nop.Test_nop.class);
        suite.addTestSuite(dot.junit.opcodes.not_int.Test_not_int.class);
        suite.addTestSuite(dot.junit.opcodes.not_long.Test_not_long.class);
        suite.addTestSuite(dot.junit.opcodes.opc_const.Test_opc_const.class);
        suite.addTestSuite(dot.junit.opcodes.opc_goto.Test_opc_goto.class);
        suite.addTestSuite(dot.junit.opcodes.opc_return.Test_opc_return.class);
        suite.addTestSuite(dot.junit.opcodes.opc_throw.Test_opc_throw.class);
        suite.addTestSuite(dot.junit.opcodes.or_int_2addr.Test_or_int_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.or_int_lit16.Test_or_int_lit16.class);
        suite.addTestSuite(dot.junit.opcodes.or_int_lit8.Test_or_int_lit8.class);
        suite.addTestSuite(dot.junit.opcodes.or_int.Test_or_int.class);
        suite.addTestSuite(dot.junit.opcodes.or_long_2addr.Test_or_long_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.or_long.Test_or_long.class);
        suite.addTestSuite(dot.junit.opcodes.packed_switch.Test_packed_switch.class);
        suite.addTestSuite(dot.junit.opcodes.rem_double_2addr.Test_rem_double_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.rem_double.Test_rem_double.class);
        suite.addTestSuite(dot.junit.opcodes.rem_float_2addr.Test_rem_float_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.rem_float.Test_rem_float.class);
        suite.addTestSuite(dot.junit.opcodes.rem_int_2addr.Test_rem_int_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.rem_int_lit16.Test_rem_int_lit16.class);
        suite.addTestSuite(dot.junit.opcodes.rem_int_lit8.Test_rem_int_lit8.class);
        suite.addTestSuite(dot.junit.opcodes.rem_int.Test_rem_int.class);
        suite.addTestSuite(dot.junit.opcodes.rem_long_2addr.Test_rem_long_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.rem_long.Test_rem_long.class);
        suite.addTestSuite(dot.junit.opcodes.return_object.Test_return_object.class);
        suite.addTestSuite(dot.junit.opcodes.return_void.Test_return_void.class);
        suite.addTestSuite(dot.junit.opcodes.return_wide.Test_return_wide.class);
        suite.addTestSuite(dot.junit.opcodes.rsub_int_lit8.Test_rsub_int_lit8.class);
        suite.addTestSuite(dot.junit.opcodes.rsub_int.Test_rsub_int.class);
        suite.addTestSuite(dot.junit.opcodes.sget_boolean.Test_sget_boolean.class);
        suite.addTestSuite(dot.junit.opcodes.sget_byte.Test_sget_byte.class);
        suite.addTestSuite(dot.junit.opcodes.sget_char.Test_sget_char.class);
        suite.addTestSuite(dot.junit.opcodes.sget_object.Test_sget_object.class);
        suite.addTestSuite(dot.junit.opcodes.sget_short.Test_sget_short.class);
        suite.addTestSuite(dot.junit.opcodes.sget.Test_sget.class);
        suite.addTestSuite(dot.junit.opcodes.sget_wide.Test_sget_wide.class);
        suite.addTestSuite(dot.junit.opcodes.shl_int_2addr.Test_shl_int_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.shl_int_lit8.Test_shl_int_lit8.class);
        suite.addTestSuite(dot.junit.opcodes.shl_int.Test_shl_int.class);
        suite.addTestSuite(dot.junit.opcodes.shl_long_2addr.Test_shl_long_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.shl_long.Test_shl_long.class);
        suite.addTestSuite(dot.junit.opcodes.shr_int_2addr.Test_shr_int_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.shr_int_lit8.Test_shr_int_lit8.class);
        suite.addTestSuite(dot.junit.opcodes.shr_int.Test_shr_int.class);
        suite.addTestSuite(dot.junit.opcodes.shr_long_2addr.Test_shr_long_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.shr_long.Test_shr_long.class);
        suite.addTestSuite(dot.junit.opcodes.sparse_switch.Test_sparse_switch.class);
        suite.addTestSuite(dot.junit.opcodes.sput_boolean.Test_sput_boolean.class);
        suite.addTestSuite(dot.junit.opcodes.sput_byte.Test_sput_byte.class);
        suite.addTestSuite(dot.junit.opcodes.sput_char.Test_sput_char.class);
        suite.addTestSuite(dot.junit.opcodes.sput_object.Test_sput_object.class);
        suite.addTestSuite(dot.junit.opcodes.sput_short.Test_sput_short.class);
        suite.addTestSuite(dot.junit.opcodes.sput.Test_sput.class);
        suite.addTestSuite(dot.junit.opcodes.sput_wide.Test_sput_wide.class);
        suite.addTestSuite(dot.junit.opcodes.sub_double_2addr.Test_sub_double_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.sub_double.Test_sub_double.class);
        suite.addTestSuite(dot.junit.opcodes.sub_float_2addr.Test_sub_float_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.sub_float.Test_sub_float.class);
        suite.addTestSuite(dot.junit.opcodes.sub_int_2addr.Test_sub_int_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.sub_int.Test_sub_int.class);
        suite.addTestSuite(dot.junit.opcodes.sub_long_2addr.Test_sub_long_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.sub_long.Test_sub_long.class);
        suite.addTestSuite(dot.junit.opcodes.ushr_int_2addr.Test_ushr_int_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.ushr_int_lit8.Test_ushr_int_lit8.class);
        suite.addTestSuite(dot.junit.opcodes.ushr_int.Test_ushr_int.class);
        suite.addTestSuite(dot.junit.opcodes.ushr_long_2addr.Test_ushr_long_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.ushr_long.Test_ushr_long.class);
        suite.addTestSuite(dot.junit.opcodes.xor_int_2addr.Test_xor_int_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.xor_int_lit16.Test_xor_int_lit16.class);
        suite.addTestSuite(dot.junit.opcodes.xor_int_lit8.Test_xor_int_lit8.class);
        suite.addTestSuite(dot.junit.opcodes.xor_int.Test_xor_int.class);
        suite.addTestSuite(dot.junit.opcodes.xor_long_2addr.Test_xor_long_2addr.class);
        suite.addTestSuite(dot.junit.opcodes.xor_long.Test_xor_long.class);      
        return suite;
    }
}
