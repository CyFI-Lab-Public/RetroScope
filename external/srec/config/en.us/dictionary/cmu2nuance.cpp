/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


//
// This converts the data found at http://www.speech.cs.cmu.edu/cgi-bin/cmudict
// into the *.ok format used by Nuance.
// We use the file c0.6, which corresponds to (v. 0.6).
//
// to run: make cmu2nuance && ./cmu2nuance <c0.6 >c0.6.ok
//
// TODO: look at generation of 'L', ')', and ','
//

#include <stdio.h>
#include <string.h>
#include <ctype.h>


static const char* xlate(const char* phone, const char* cmu, const char* nuance) {
  int ncmu = strlen(cmu);
  if (strncmp(phone, cmu, ncmu) || !isspace(phone[ncmu])) return NULL;
  fputs(nuance, stdout);
  return phone + strlen(cmu);
}


int main(int argc, const char* argv[]) {
  char line[200];

  fputs("#LANG=EN-US\n", stdout);

  for (int lineno = 1; NULL != fgets(line, sizeof(line), stdin); lineno++)
  {
    if (line[0] == '#') continue;
    if (line[0] == 0) continue;
    if (!isalnum(line[0])) {
      fprintf(stderr, "warning: ignoring line %d - %s", lineno, line);
      continue;
    }

    const char* p = line;

    // parse name, echoing in lower case and skipping (2) suffix
    while (!isspace(*p)) {
      if (*p == 0) {
        fprintf(stderr, "can't read name at line %d\n", lineno);
        break;
      }
      if (p[0] == '(' && isdigit(p[1]) && p[2] == ')' && isspace(p[3])) {
        p += 3;
        break;
      }
      fputc(tolower(*p), stdout);
      p++;
    }
    fputc(' ', stdout);

    // loop over whitespace delimited phonemes
    while (1) {
      // skip leading whitespace
      while (isspace(*p)) p++;
      if (*p == 0) break;

      const char* next = 0;
      if (
        (next=xlate(p, "AA1 R", ")r")) ||   // odd     AA D
        (next=xlate(p, "AA0", "o")) ||   // odd     AA D
        (next=xlate(p, "AA1", "o")) ||   // odd     AA D
        (next=xlate(p, "AA2", "o")) ||   // odd     AA D

        (next=xlate(p, "AE0", "a")) ||   // at      AE T
        (next=xlate(p, "AE1", "a")) ||   // at      AE T
        (next=xlate(p, "AE2", "a")) ||   // at      AE T

//        (next=xlate(p, "AH0 L", "L")) || // drops accuracy by 1%
        (next=xlate(p, "AH0 N", "~")) ||   // hut     HH AH T - from jean
        (next=xlate(p, "AH0 M", "}")) ||   // hut     HH AH T - from jean
        (next=xlate(p, "AH0", "@")) ||   // hut     HH AH T - from jean
        (next=xlate(p, "AH1", "u")) ||   // hut     HH AH T
        (next=xlate(p, "AH2", "u")) ||   // hut     HH AH T

        (next=xlate(p, "AO0", "{")) ||   // ought   AO T
        (next=xlate(p, "AO1", "{")) ||   // ought   AO T
        (next=xlate(p, "AO2", "{")) ||   // ought   AO T

        (next=xlate(p, "AW0", "?")) ||   // cow     K AW
        (next=xlate(p, "AW1", "?")) ||   // cow     K AW
        (next=xlate(p, "AW2", "?")) ||   // cow     K AW

        (next=xlate(p, "AY0", "I")) ||   // hide    HH AY D
        (next=xlate(p, "AY1", "I")) ||   // hide    HH AY D
        (next=xlate(p, "AY2", "I")) ||   // hide    HH AY D

        (next=xlate(p, "B"  , "b")) ||   // be      B IY
        (next=xlate(p, "CH" , "C")) ||   // cheese  CH IY Z
        (next=xlate(p, "D"  , "d")) ||   // dee     D IY
        (next=xlate(p, "DH" , "D")) ||   // thee    DH IY

        (next=xlate(p, "EH1 R", ",r")) ||   // Ed      EH D
        (next=xlate(p, "EH0", "c")) ||   // Ed      EH D - from jean
        (next=xlate(p, "EH1", "e")) ||   // Ed      EH D
        (next=xlate(p, "EH2", "e")) ||   // Ed      EH D

        (next=xlate(p, "ER0", "P")) ||   // hurt    HH ER T
        (next=xlate(p, "ER1", "V")) ||   // hurt    HH ER T
        (next=xlate(p, "ER2", "V")) ||   // hurt    HH ER T

        (next=xlate(p, "EY0", "A")) ||   // ate     EY T
        (next=xlate(p, "EY1", "A")) ||   // ate     EY T
        (next=xlate(p, "EY2", "A")) ||   // ate     EY T

        (next=xlate(p, "F"  , "f")) ||   // fee     F IY
        (next=xlate(p, "G"  , "g")) ||   // green   G R IY N
        (next=xlate(p, "HH" , "h")) ||   // he      HH IY

        (next=xlate(p, "IH0", "6")) ||   // it      IH T
        (next=xlate(p, "IH1", "i")) ||   // it      IH T
        (next=xlate(p, "IH2", "i")) ||   // it      IH T

        (next=xlate(p, "IY0", "/")) ||   // eat     IY T - from jean
        (next=xlate(p, "IY1", "E")) ||   // eat     IY T
        (next=xlate(p, "IY2", "E")) ||   // eat     IY T

        (next=xlate(p, "JH" , "j")) ||   // gee     JH IY
        (next=xlate(p, "K"  , "k")) ||   // key     K IY
        (next=xlate(p, "L"  , "l")) ||   // lee     L IY
        (next=xlate(p, "M"  , "m")) ||   // me      M IY
        (next=xlate(p, "N"  , "n")) ||   // knee    N IY
        (next=xlate(p, "NG" , "N")) ||   // ping    P IH NG

        (next=xlate(p, "OW0", "]")) ||   // oat     OW T
        (next=xlate(p, "OW1", "O")) ||   // oat     OW T
        (next=xlate(p, "OW2", "O")) ||   // oat     OW T

        (next=xlate(p, "OY0", "<")) ||   // toy     T OY
        (next=xlate(p, "OY1", "<")) ||   // toy     T OY
        (next=xlate(p, "OY2", "<")) ||   // toy     T OY

        (next=xlate(p, "P"  , "p")) ||   // pee     P IY
        (next=xlate(p, "R"  , "r")) ||   // read    R IY D
        (next=xlate(p, "S"  , "s")) ||   // sea     S IY
        (next=xlate(p, "SH" , "S")) ||   // she     SH IY
        (next=xlate(p, "T"  , "t")) ||   // tea     T IY
        (next=xlate(p, "TH" , "T")) ||   // theta   TH EY T AH

        (next=xlate(p, "UH0", "q")) ||   // hood    HH UH D
        (next=xlate(p, "UH1", "q")) ||   // hood    HH UH D
        (next=xlate(p, "UH2", "q")) ||   // hood    HH UH D

        (next=xlate(p, "UW0", "U")) ||   // two     T UW
        (next=xlate(p, "UW1", "U")) ||   // two     T UW
        (next=xlate(p, "UW2", "U")) ||   // two     T UW

        (next=xlate(p, "V"  , "v")) ||   // vee     V IY
        (next=xlate(p, "W"  , "w")) ||   // we      W IY
        (next=xlate(p, "Y"  , "y")) ||   // yield   Y IY L D
        (next=xlate(p, "Z"  , "z")) ||   // zee     Z IY
        (next=xlate(p, "ZH" , "Z")) ||   // seizure S IY ZH ER
        0) {
        p = next;
      }
      else {
        fprintf(stderr, "can't pronounce line %d: %s", lineno, p);
        break;
      }

    }

    fputc('\n', stdout);

  }
}
