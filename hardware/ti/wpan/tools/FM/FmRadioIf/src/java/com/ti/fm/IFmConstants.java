/*
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ti.fm;

public interface IFmConstants {

    /*
    * By setting this flag true, FM APIS can be used in blocking mode. else FM
    * APIS will be non blocking
    */
    public static final boolean MAKE_FM_APIS_BLOCKING = false;

    /*
    * By setting this flag true, FM RDS data(PS and RDS Text) can be sent to
    * the application as bytearray. else FM RDS data(PS and RDS Text) will be
    * sent to the application as String
    */

    public static final boolean FM_SEND_RDS_IN_BYTEARRAY = false;

    public static final int FM_SEEK_IN_PROGRESS = 0xFF;

    public static final int FM_BAND_EUROPE_US = 0;

    public static final int FM_BAND_JAPAN = 1;

    /* Europe / US band limits */
    public static final int FM_FIRST_FREQ_US_EUROPE_KHZ = 87500;

    public static final int FM_LAST_FREQ_US_EUROPE_KHZ = 108000;

    /* Japan band limits */
    public static final int FM_FIRST_FREQ_JAPAN_KHZ = 76000;

    public static final int FM_LAST_FREQ_JAPAN_KHZ = 90000;

    public static final int DEF_VOL = 1;

    public static final int FM_MAX_VOLUME = 16383; //32767,//65535;

    /* volume states */

    public static final boolean VOL_REQ_STATE_IDLE = true;

    public static final boolean VOL_REQ_STATE_PENDING = false;

    public static final int FM_CHANNEL_SPACE = 2;

    public static final int FM_NOT_MUTE = 1;

    public static final int FM_RF_DEP_MUTE_OFF = 0;

    public static final int FM_RSSI_THRESHHOLD = 7;

    /* Mute constants */

    public static final int FM_MUTE = 0;

    public static final int FM_UNMUTE = 1;

    public static final int FM_ATT = 2;

    /* Fm Radio State */
    public static final int STATE_ENABLED = 0;

    public static final int STATE_DISABLED = 1;

    public static final int STATE_ENABLING = 2;

    public static final int STATE_DISABLING = 3;

    public static final int STATE_PAUSE = 4;

    public static final int STATE_RESUME = 5;

    public static final int STATE_DEFAULT = 6;

    // public static final int FM_SUCCESS = 0 ;
    /* FM Error Returns */
    public static final int FM_FAILED = 0xFFF;

    public static final int FM_UNDEFINED_FREQ = 0xFFFFFFFF;

    public static final int FM_COMPLETE_SCAN_IS_NOT_IN_PROGRESS = 115;

    public static final int FM_COMPLETE_SCAN_STOPPED = 116;

    /* Recovery Params */
    // must be a long time to account for turning off stale btipsd + turning on
    // new one
    public static final long FM_RADIO_ON_TIMEOUT_MSEC = 20 * 1000;

    public static final long FM_RADIO_OFF_TIMEOUT_MSEC = 10 * 1000;

    /***********************************************************************************************
    * Look up tables for RDS data conversion
    ***********************************************************************************************/
    public static char[][] lookUpTable_G0 = {
          /* 0 */{
                '\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007',
                '\u0008', '\u0009', '\u0000', '\u000b', '\u000c', '\u0000', '\u000e', '\u000f'
          },
          /* 1 */{
                '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016', '\u0017',
                '\u0018', '\u0019', '\u001a', '\u001b', '\u001c', '\u001d', '\u001e', '\u001f'
          },
          /* 2 */{
                '\u0020', '\u0021', '\u0022', '\u0023', '\u00A4', '\u0025', '\u0026', '\u0000',
                '\u0028', '\u0029', '\u002a', '\u002b', '\u002c', '\u002d', '\u002e', '\u2044'
          },
          /* 3 */{
                '\u0030', '\u0031', '\u0032', '\u0033', '\u0034', '\u0035', '\u0036', '\u0037',
                '\u0038', '\u0039', '\u003a', '\u003b', '\u003c', '\u003d', '\u003e', '\u003f'
          },
          /* 4 */{
                '\u0040', '\u0041', '\u0042', '\u0043', '\u0044', '\u0045', '\u0046', '\u0047',
                '\u0048', '\u0049', '\u004a', '\u004b', '\u004c', '\u004d', '\u004e', '\u004f'
          },
          /* 5 */{
                '\u0050', '\u0051', '\u0052', '\u0053', '\u0054', '\u0055', '\u0056', '\u0057',
                '\u0058', '\u0059', '\u005a', '\u005b', '\u0000', '\u005d', '\u005e', '\u005f'
          },
          /* 6 */{
                '\u0060', '\u0061', '\u0062', '\u0063', '\u0064', '\u0065', '\u0066', '\u0067',
                '\u0068', '\u0069', '\u006a', '\u006b', '\u006c', '\u006d', '\u006e', '\u006f'
          },
          /* 7 */{
                '\u0070', '\u0071', '\u0072', '\u0073', '\u0074', '\u0075', '\u0076', '\u0077',
                '\u0078', '\u0079', '\u007a', '\u007b', '\u007c', '\u007d', '\u0020', '\u0020'
          },
          /* 8 */{
                '\u00E1', '\u00E0', '\u00E9', '\u00E8', '\u00ED', '\u00EC', '\u00F3', '\u00F2',
                '\u00FA', '\u00F9', '\u00D1', '\u00C7', '\u015E', '\u03B2', '\u03AF', '\u0132'
          },
          /* 9 */{
                '\u00E2', '\u00E4', '\u00EA', '\u00EB', '\u00EE', '\u00EF', '\u00F4', '\u00F6',
                '\u00FB', '\u00FC', '\u00F1', '\u00E7', '\u015F', '\u0020', '\u00B9', '\u0133'
          },
          /* a */{
                '\u00AA', '\u03B1', '\u00A9', '\u2030', '\u011E', '\u0115', '\u0148', '\u0151',
                '\u03C0', '\u0020', '\u00A3', '\u0024', '\u2190', '\u2191', '\u2192', '\u2193'
          },
          /* b */{
                '\u00b0', '\u00B9', '\u00B2', '\u00B3', '\u00B1', '\u0130', '\u0144', '\u0171',
                '\u00B5', '\u00BF', '\u00F7', '\u00B0', '\u00BC', '\u00BD', '\u00BE', '\u00A7'
          },
          /* c */{
                '\u00C1', '\u00C0', '\u00C9', '\u00C8', '\u00CD', '\u00CC', '\u00D3', '\u00D2',
                '\u00DA', '\u00D9', '\u0158', '\u010C', '\u0160', '\u017D', '\u0110', '\u013F'
          },
          /* d */{
                '\u00C2', '\u00C4', '\u00CA', '\u00CB', '\u00CE', '\u00CF', '\u00D4', '\u00D6',
                '\u00DB', '\u00DC', '\u0159', '\u010D', '\u0161', '\u017E', '\u0111', '\u0140'
          },
          /* e */{
                '\u00C3', '\u00C5', '\u01FC', '\u0152', '\u0177', '\u00DD', '\u00D5', '\u00D8',
                '\u00FE', '\u014A', '\u0154', '\u0106', '\u015A', '\u0179', '\u0166', '\u00F0'
          },
          /* f */{
                '\u00E3', '\u00E5', '\u01FD', '\u0153', '\u0175', '\u00FD', '\u00F5', '\u00F8',
                '\u00DE', '\u014B', '\u0155', '\u0107', '\u015B', '\u017A', '\u0167', '\u0020'
          }
    };

    public static char[][] lookUpTable_G1 = {
          /* 0 */{
                '\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007',
                '\u0008', '\u0009', '\u0000', '\u000b', '\u000c', '\u0000', '\u000e', '\u000f'
          },
          /* 1 */{
                '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016', '\u0017',
                '\u0018', '\u0019', '\u001a', '\u001b', '\u001c', '\u001d', '\u001e', '\u001f'
          },
          /* 2 */{
                '\u0020', '\u0021', '\u0022', '\u0023', '\u00A4', '\u0025', '\u0026', '\u0000',
                '\u0028', '\u0029', '\u002a', '\u002b', '\u002c', '\u002d', '\u002e', '\u2044'
          },
          /* 3 */{
                '\u0030', '\u0031', '\u0032', '\u0033', '\u0034', '\u0035', '\u0036', '\u0037',
                '\u0038', '\u0039', '\u003a', '\u003b', '\u003c', '\u003d', '\u003e', '\u003f'
          },
          /* 4 */{
                '\u0040', '\u0041', '\u0042', '\u0043', '\u0044', '\u0045', '\u0046', '\u0047',
                '\u0048', '\u0049', '\u004a', '\u004b', '\u004c', '\u004d', '\u004e', '\u004f'
          },
          /* 5 */{
                '\u0050', '\u0051', '\u0052', '\u0053', '\u0054', '\u0055', '\u0056', '\u0057',
                '\u0058', '\u0059', '\u005a', '\u005b', '\u0000', '\u005d', '\u005e', '\u005f'
          },
          /* 6 */{
                '\u0060', '\u0061', '\u0062', '\u0063', '\u0064', '\u0065', '\u0066', '\u0067',
                '\u0068', '\u0069', '\u006a', '\u006b', '\u006c', '\u006d', '\u006e', '\u006f'
          },
          /* 7 */{
                '\u0070', '\u0071', '\u0072', '\u0073', '\u0074', '\u0075', '\u0076', '\u0077',
                '\u0078', '\u0079', '\u007a', '\u007b', '\u007c', '\u007d', '\u0020', '\u0020'
          },
          /* 8 */{
                '\u00E1', '\u00E0', '\u00E9', '\u00E8', '\u00ED', '\u00EC', '\u00F3', '\u00F2',
                '\u00FA', '\u00F9', '\u00D1', '\u00C7', '\u015E', '\u03B2', '\u03AF', '\u0132'
          },
          /* 9 */{
                '\u00E2', '\u00E4', '\u00EA', '\u00EB', '\u00EE', '\u00EF', '\u00D4', '\u00D6',
                '\u00FB', '\u00FC', '\u00F1', '\u00E7', '\u015F', '\u0020', '\u00B9', '\u0133'
          },
          /* a */{
                '\u00AA', '\u00a1', '\u00A9', '\u2030', '\u01CE', '\u0115', '\u0148', '\u0151',
                '\u0165', '\u0020', '\u00A3', '\u0024', '\u2190', '\u2191', '\u2192', '\u2193'
          },
          /* b */{
                '\u00BA', '\u00B9', '\u00B2', '\u00B3', '\u00B1', '\u0130', '\u0144', '\u0171',
                '\u0163', '\u00BF', '\u00F7', '\u00B0', '\u00BC', '\u00BD', '\u00BE', '\u00A7'
          },
          /* c */{
                '\u0404', '\u042F', '\u0020', '\u0427', '\u0414', '\u042D', '\u0444', '\u0403',
                '\u00c8', '\u0418', '\u0436', '\u045C', '\u041B', '\u045B', '\u0452', '\u044B'
          },
          /* d */{
                '\u00FD', '\u0459', '\u00d2', '\u0448', '\u0446', '\u042E', '\u0449', '\u040A',
                '\u040F', '\u0419', '\u0417', '\u010D', '\u0161', '\u017E', '\u00de', '\u00df'
          },
          /* e */{
                '\u03A0', '\u03B1', '\u00e2', '\u00e3', '\u03B4', '\u03B5', '\u03C6', '\u03B3',
                '\u00e8', '\u03B9', '\u2140', '\u03F0', '\u03BB', '\u03BC', '\u03B3', '\u03C9'
          },
          /* f */{
                '\u03C0', '\u03A9', '\u037B', '\u00f3', '\u03C4', '\u03BE', '\u0398', '\u0393',
                '\u039E', '\u03C5', '\u03B6', '\u03DB', '\u039B', '\u03A8', '\u0394', '\u0020'
          }
    };

    public static char[][] lookUpTable_G2 = {
          /* 0 */{
                '\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007',
                '\u0008', '\u0009', '\u0000', '\u000b', '\u000c', '\u0000', '\u000e', '\u000f'
          },
          /* 1 */{
                '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016', '\u0017',
                '\u0018', '\u0019', '\u001a', '\u001b', '\u001c', '\u001d', '\u001e', '\u001f'
          },
          /* 2 */{
                '\u0020', '\u0021', '\u0022', '\u0023', '\u00A4', '\u0025', '\u0026', '\u0000',
                '\u0028', '\u0029', '\u002a', '\u002b', '\u002c', '\u002d', '\u002e', '\u2044'
          },
          /* 3 */{
                '\u0030', '\u0031', '\u0032', '\u0033', '\u0034', '\u0035', '\u0036', '\u0037',
                '\u0038', '\u0039', '\u003a', '\u003b', '\u003c', '\u003d', '\u003e', '\u003f'
          },
          /* 4 */{
                '\u0040', '\u0041', '\u0042', '\u0043', '\u0044', '\u0045', '\u0046', '\u0047',
                '\u0048', '\u0049', '\u004a', '\u004b', '\u004c', '\u004d', '\u004e', '\u004f'
          },
          /* 5 */{
                '\u0050', '\u0051', '\u0052', '\u0053', '\u0054', '\u0055', '\u0056', '\u0057',
                '\u0058', '\u0059', '\u005a', '\u005b', '\u0000', '\u005d', '\u005e', '\u005f'
          },
          /* 6 */{
                '\u0060', '\u0061', '\u0062', '\u0063', '\u0064', '\u0065', '\u0066', '\u0067',
                '\u0068', '\u0069', '\u006a', '\u006b', '\u006c', '\u006d', '\u006e', '\u006f'
          },
          /* 7 */{
                '\u0070', '\u0071', '\u0072', '\u0073', '\u0074', '\u0075', '\u0076', '\u0077',
                '\u0078', '\u0079', '\u007a', '\u007b', '\u007c', '\u007d', '\u0020', '\u0020'
          },
          /* 8 */{
                '\uEE92', '\uEE98', '\u0629', '\uEE9C', '\uFEA0', '\uFEA4', '\ufea8', '\u062f',
                '\u0630', '\ufe8d', '\ufe83', '\ufeb3', '\ufeb7', '\ufebb', '\ufebf', '\ufec1'
          },
          /* 9 */{
                '\ufec5', '\ufecb', '\ufecf', '\ufed3', '\ufed7', '\ufedb', '\ufedd', '\ufee3',
                '\ufee7', '\ufeeb', '\ufeed', '\ufef3', '\u2190', '\u2191', '\u2192', '\u2193'
          },
          /* a */{
                '\u05d0', '\u05d1', '\u05d2', '\u05d3', '\u05d4', '\u05d5', '\u05d6', '\u05d7',
                '\u05d8', '\u05d9', '\u05db', '\u05da', '\u05dc', '\u05de', '\u05dd', '\u05e0'
          },
          /* b */{
                '\u05df', '\u05e1', '\u05e2', '\u05e4', '\u05e3', '\u05e6', '\u05e5', '\u05e7',
                '\u05e8', '\u05e9', '\u05ea', '\u00B0', '\u00BC', '\u00BD', '\u00BE', '\u00A7'
          },
          /* c */{
                '\u0404', '\u042F', '\u0020', '\u0427', '\u0414', '\u042D', '\u0444', '\u0403',
                '\u00c8', '\u0418', '\u0436', '\u045C', '\u041B', '\u045B', '\u0452', '\u044B'
          },
          /* d */{
                '\u00FD', '\u0459', '\u00d2', '\u0448', '\u0446', '\u042E', '\u0449', '\u040A',
                '\u040F', '\u0419', '\u0417', '\u010D', '\u0161', '\u017E', '\u00de', '\u00df'
          },
          /* e */{
                '\u03A0', '\u00C5', '\u01FC', '\u0152', '\u0177', '\u00DD', '\u00D5', '\u00D8',
                '\u00FE', '\u014A', '\u03A3', '\u0106', '\u03BB', '\u03BC', '\u03B3', '\u03C9'
          },
          /* f */{
                '\u03C0', '\u03A9', '\u037B', '\u00f3', '\u03C4', '\u03BE', '\u0398', '\u0393',
                '\u039E', '\u03C5', '\u03B6', '\u03DB', '\u039B', '\u03A8', '\u0394', '\u0020'
          }
    };

    /********* FM TX *************/

    public static final int FM_AF_CODE_NO_AF_AVAILABLE = 224;

    public static final int FM_RDS_PTY_CODE_NO_PROGRAM_UNDEFINED = 0;

    public static final int FM_RDS_PTY_CODE_MAX_VALUE = 31;

    public static final int FM_RDS_SCROLL_SPEED_DEFUALT = 3;
}
