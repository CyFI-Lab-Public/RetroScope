/*
 * Copyright (C) 2010 The Android Open Source Project
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

package vogar.util;

import java.io.PrintStream;

/**
 * A console that can erase output back to a previously marked position.
 */
public final class MarkResetConsole {

    private final PrintStream out;
    private int row;
    private final StringBuilder rowContent = new StringBuilder();

    public MarkResetConsole(PrintStream out) {
        this.out = out;
    }

    public void println(String text) {
        print(text + "\n");
    }

    public void print(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                row++;
                rowContent.delete(0, rowContent.length());
            } else {
                rowContent.append(text.charAt(i));
            }
        }

        out.print(text);
        out.flush();
    }

    public Mark mark() {
        return new Mark();
    }

    public class Mark {
        private final int markRow = row;
        private final String markRowContent = rowContent.toString();

        private Mark() {}

        public void reset() {
            /*
             * ANSI escapes
             * http://en.wikipedia.org/wiki/ANSI_escape_code
             *
             *  \u001b[K   clear the rest of the current line
             *  \u001b[nA  move the cursor up n lines
             *  \u001b[nB  move the cursor down n lines
             *  \u001b[nC  move the cursor right n lines
             *  \u001b[nD  move the cursor left n columns
             */

            for (int r = row; r > markRow; r--) {
                // clear the line, up a line
                System.out.print("\u001b[0G\u001b[K\u001b[1A");
            }

            // clear the line, reprint the line
            out.print("\u001b[0G\u001b[K");
            out.print(markRowContent);
            rowContent.delete(0, rowContent.length());
            rowContent.append(markRowContent);
            row = markRow;
        }
    }
}
