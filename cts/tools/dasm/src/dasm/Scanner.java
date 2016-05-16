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

package dasm;


import dasm.tokens.number_token;
import dasm.tokens.relative_number_token;

import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;

import java_cup.runtime.int_token;
import java_cup.runtime.str_token;
import java_cup.runtime.token;

/**
 * Tokenizer
 */


class Scanner {
    /**
     * Chars buffer with autoexpanding.
     */
    class CharBuffer {
        private int buffer_size = 256;
        private char[] buffer = new char[buffer_size];
        private int cur_pos = 0;

        private void expand() {
            char tmp[] = new char[buffer_size * 2];
            System.arraycopy(buffer, 0, tmp, 0, buffer_size);
            buffer_size *= 2;
            buffer = tmp;
        }

        void add(char c) {
            buffer[cur_pos] = c;
            cur_pos++;
            if (cur_pos == buffer_size) expand();
        }

        int size() {
            return cur_pos;
        }

        char charAt(int idx) {
            return buffer[idx];
        }

        public String toString() {
            return new String(buffer, 0, cur_pos);
        }

        void reset() {
            cur_pos = 0;
        }
    }


    protected Reader inputReader;

    /**
     * next character in input stream
     */
    protected int nextChar;

    protected CharBuffer charBuf = new CharBuffer();

    /**
     * Whitespace characters
     */
    protected static final String WHITESPACE = " \n\t\r";

    /**
     * Separator characters
     */
    protected static final String SEPARATORS = WHITESPACE + ":=" + ",";

    /**
     * error reporting
     */
    public int line_num, token_line_num, char_num;
    public StringBuffer line;

    /**
     * Holds new variables defined by .set directive
     */
    public Hashtable dict = new Hashtable();

    public Scanner(Reader i) throws IOException, DasmError {
        inputReader = i;
        line_num = 1;
        char_num = 0;
        line = new StringBuffer();
        nextChar = 0;
        skipEmptyLines();
        if (nextChar == -1) throw new DasmError("empty source file");
    }

    /**
     * Checks if a character code is a whitespace character
     */
    protected static boolean isWhitespace(int c) {
        return (WHITESPACE.indexOf(c) != -1);
    }

    /**
     * Checks if a character code is a separator character
     */
    protected static boolean isSeparator(int c) {
        return (c == -1 || SEPARATORS.indexOf(c) != -1);
    }

    /**
     * Gets next char from input
     */
    protected void readNextChar() throws IOException {
        nextChar = inputReader.read();
        switch (nextChar) {
        case -1: // EOF
            if (char_num == 0) {
                char_num = -1;
                break;
            }
            nextChar = '\n';
            // fall thru
        case '\n':
            line_num++;
            char_num = 0;
            break;
        default:
            line.append((char) nextChar);
            char_num++;
            return;
        }
        line.setLength(0);
    }

    /**
     * Skips empty lines in input stream
     */
    private void skipEmptyLines() throws IOException {
        for (;;) {
            if (nextChar != ';') {
                do {
                    readNextChar();
                } while (isWhitespace(nextChar));
                if (nextChar != ';') return;
            }
            do {
                readNextChar();
                if (nextChar == -1) return;
            } while (nextChar != '\n');
        }
    }

    /**
     * Reads unicode char (\\uXXXX)
     */
    private char readUnicodeChar() throws IOException, DasmError {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            readNextChar();
            if (nextChar == -1) return 0;

            int tmp = Character.digit((char) nextChar, 16);
            if (tmp == -1)
                throw new DasmError("Invalid '\\u' escape sequence");
            result = (result << 4) | tmp;
        }
        return (char) result;
    }

    private char nameEscape() throws IOException, DasmError {
        readNextChar();
        if (nextChar != 'u')
            throw new DasmError("Only '\\u' escape sequence allowed in names");
        char chval = readUnicodeChar();
        if (nextChar == -1)
            throw new DasmError("Left over '\\u' escape sequence");
        return chval;
    }

    /**
     * Read and recognize next token
     */
    public token next_token() throws IOException, DasmError {
        token_line_num = line_num;

        for (;;)
            switch (nextChar) {
            case ';': // a comment
            case '\n':
                // return single SEP token (skip multiple newlines
                // interspersed with whitespace or comments)
                skipEmptyLines();
                token_line_num = line_num;
                return new token(sym.SEP);

            case ' ':
            case '\t':
            case '\r':
            case ',': // whitespace
                readNextChar();
                break;

            case -1: // EOF token
                char_num = -1;
                return new token(sym.EOF);

            case '=': // EQUALS token
                readNextChar();
                return new token(sym.EQ);

            case ':': // COLON token
                readNextChar();
                return new token(sym.COLON);

            case '-':
            case '+':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '.': // a number
            {
                return readNumber();
            }

            case '"': // quoted string
            {
                return readQuotedString();
            }

            case '{': // list of registers
            {
                return readRegList();
            }

            case '\'': // quotation for overloading reserved words
                return readQuotedReservedWord();

            default: {
                // read up until a separatorcharacter
                boolean only_name = false;

                charBuf.reset();
                do {
                    char chval = (char) nextChar;
                    if (nextChar == '\\') {
                        chval = nameEscape();
                        only_name = true;
                    }
                    charBuf.add(chval);
                    readNextChar();
                } while (!isSeparator(nextChar));

                String str = charBuf.toString();

                if (!only_name) {
                    token tok;

                    // keyword or directive?
                    if ((tok = ReservedWords.get(str)) != null) return tok;

                    // VM instruction?
                    if (DopInfo.contains(str))
                        return new str_token(sym.Insn, str);

                    if (str.charAt(0) == '$') {
                        String s = str.substring(1);
                        Object v;
                        int n = 10;
                        boolean neg = false;
                        switch (s.charAt(0)) {
                        default:
                            break;

                        case '-':
                            neg = true;
                        case '+':
                            s = s.substring(1);
                            if (s.startsWith("0x")) {
                                n = 16;
                                s = s.substring(2);
                            }
                            try {
                                n = Integer.parseInt(s, n);
                            } catch (NumberFormatException e) {
                                throw new DasmError(
                                        "Bad relative offset number");
                            }
                            if (neg) n = -n;
                            return new relative_number_token(sym.Relative, n);
                        }
                        // Do variable substitution
                        if ((v = dict.get(s)) != null) return (token) v;
                    } // not begin from '$'
                } // !only_name
                // Unrecognized string token (e.g. a classname)
                return new str_token(sym.Word, str);
            }
            }
    }

    /**
     * Reads "-quoted string
     */
    protected token readQuotedString() throws IOException, DasmError {
        boolean f = false;
        charBuf.reset();
        for (;;) {
            if (f)
                f = false;
            else
                readNextChar();

            if (nextChar == '"') {
                readNextChar(); // skip closing quote
                return new str_token(sym.Str, charBuf.toString());
            }

            if (nextChar == -1) throw new DasmError("Unterminated string");

            char chval = (char) nextChar;

            if (chval == '\\') {
                readNextChar();
                switch (nextChar) {
                case -1:
                    f = true;
                    continue;
                case 'n':
                    chval = '\n';
                    break;
                case 'r':
                    chval = '\r';
                    break;
                case 't':
                    chval = '\t';
                    break;
                case 'f':
                    chval = '\f';
                    break;
                case 'b':
                    chval = '\b';
                    break;
                case '"':
                    chval = '"';
                    break;
                case '\'':
                    chval = '\'';
                    break;
                case '\\':
                    chval = '\\';
                    break;

                case 'u':
                    chval = readUnicodeChar();
                    if (nextChar == -1) {
                        f = true;
                        continue;
                    }
                    break;

                // octals
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7': {
                    int res = nextChar & 7;
                    readNextChar();
                    if (nextChar < '0' || nextChar > '7')
                        f = true;
                    else {
                        res = res * 8 + (nextChar & 7);
                        readNextChar();
                        if (nextChar < '0' || nextChar > '7')
                            f = true;
                        else {
                            int val = res * 8 + (nextChar & 7);
                            if (val >= 0x100)
                                f = true;
                            else
                                res = val;
                        }
                    }
                    chval = (char) res;
                }
                    break;

                default:
                    throw new DasmError("Incorrect backslash escape sequence");
                }
            }
            charBuf.add(chval);
        }
    }

    /**
     * Reads list of registers ({v1, v2, v3} or {v1..v3})
     */
    protected token readRegList() throws IOException, DasmError {
        charBuf.reset();
        for (;;) {
            readNextChar();

            if (nextChar == '}') {
                readNextChar(); // skip closing quote
                return new str_token(sym.Word, charBuf.toString());
            }

            if (nextChar == -1)
                throw new DasmError("Unterminated list of registers");


            charBuf.add((char) nextChar);
        }
    }

    /**
     * Reads number
     */
    protected token readNumber() throws IOException, DasmError {
        charBuf.reset();

        do {
            charBuf.add((char) nextChar);
            readNextChar();
        } while (!isSeparator(nextChar));

        String str = charBuf.toString();
        token tok;

        // directive?
        if ((tok = ReservedWords.get(str)) != null) return tok;

        Number num;
        try {
            num = Utils.stringToNumber(str);
        } catch (NumberFormatException e) {
            if (charBuf.charAt(0) != '.') // directive?
                throw new DasmError("Bad number format");
            throw new DasmError("Unknown directive or bad number format");
        }

        if (num instanceof Integer) {
            return new int_token(sym.Int, num.intValue());
        }

        return new number_token(sym.Num, num);
    }

    /**
     * Reads ''-quoted overloaded reserved words
     */
    protected token readQuotedReservedWord() throws IOException, DasmError {
        charBuf.reset();
        for (;;) {
            readNextChar();
            if (isSeparator(nextChar))
                throw new DasmError("Unterminated ''-enclosed name");
            if (nextChar == '\'') {
                if (charBuf.size() == 0)
                    throw new DasmError("Empty ''-enclosed name");
                readNextChar(); // skip close quote
                if (!isSeparator(nextChar))
                    throw new DasmError(
                            "Missed separator after ''-enclosed name");
                return new str_token(sym.Word, charBuf.toString());
            }
            char chval = (char) nextChar;
            if (nextChar == '\\') chval = nameEscape();
            charBuf.add(chval);
        }
    }
};
