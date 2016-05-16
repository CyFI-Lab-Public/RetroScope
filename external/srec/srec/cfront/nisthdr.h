/*---------------------------------------------------------------------------*
 *  nisthdr.h  *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * 
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/

#ifndef DO_CLEANUP_20070723
#ifndef _NISTHDR_H_
#define _NISTHDR_H_




/**
 * @todo document
 */
class Nist1AHeader
{
  public:
    Nist1AHeader(void);
    ~Nist1AHeader(void);

    int read(PFile* pF);     /* returns number of chars read, errno is set
        // Bug: Reading a header does not mean that you can write it, the read
        // information does not automatically transfer to the writing data.*/

    int write(PFile* pF);    /* returns number of chars written, errno is set*/
    /*
    // The fields are not constrained to contain values from these lists,
    // but the existence of these lists makes it easy to use uniform naming.
    // Use the enum to index into the matching array of strings.
    */
    enum SampleFormatEnum {LINEAR_8BITS, LINEAR_16BITS};
    static char *mSampleFormatNames[];

    enum SexEnum
    {
      FEMALE, MALE
  };
    static char *mSexNames[];

    enum SpeakerAccentEnum
    {
      AE
  };
    static char *mSpeakerAccentNames[];

    enum MicrophoneEnum
    {
      SM10, PRIMO, TELEX_STICK, AKG_Q_400
  };
    static char *mMicrophoneNames[];
    /*
    // The procedure for adding a field to the header is:
    //   1. add a name in FieldId, make it uppercase version of header entry
    //   2. add a set function by adding declaration and definition using
    //      macro Nist1AHeaderSetDef
    //   3. add a FieldDescription in mFieldsInit in .cpp file
    // If you need a new data type (something besides d, l, f, or s) then
    // you will also have to alter the union in field description and code
    // in write() to size and print the type.  Please provide predefined
    // names with matching enums wherever possible as is done for
    // MicrophoneNames, etc.
    */
    enum FieldId
    {
      /* CAUTION: this order must match listing order in NistFields in cpp*/
      WAVE_SIZE,
      SAMPLE_RATE,
      SAMPLE_FORMAT,
      COEF_EMPHASIS,
      SPEAKER_NAME,
      SPEAKER_AGE,
      SPEAKER_SEX,
      SPEAKER_ACCENT,

      SPEAKING_MODE,
      CHANNEL_COUNT,
      SAMPLE_COUNT,
      SAMPLE_MIN,
      SAMPLE_MAX,
      SAMPLE_N_BYTES,
      SAMPLE_BYTE_FORMAT,
      RECORD_DATE,

      MICROPHONE,
      MICROPHONE_POSITION,
      RECORD_SITE,
      DOS_PROGRAM,
      DSP_PROGRAM,
      LANGUAGE,
      RECORD_TIME,
      UTTERANCE_ID,
      CHECK_SUM,
      ALL_BYTE_CHECK_SUM,
      JIN0_FRAME0,
      AVERAGE_BACKGROUND,
      AVERAGE_SPEECH,
      SIGNAL_TO_NOISE,
      PROMPT_TEXT,
      PRONUNCIATION_TEXT,
      RECORDING_MODE,
      FEP_BEGSIL,
      FEP_ENDSIL,
      FEP_XBADJ,
      END_HEAD /* needed to mark the end of this list*/
  };

    /* remove a particular field from the header (for writing only)*/
    void reset(FieldId id);

    /* empties the header (for both reading and writing)*/
    void reset(void);

    /* CAUTION: strings are not copied, only the pointer is saved*/
    void setWaveSize(long n); 
    void setSampleRate(int r);/* shouldn't it be double?*/
    void setSampleFormat(char *format);
    void setCoefEmphasis(double c);
    void setSpeakerName(char *name);
    void setSpeakerAge(char *age);/*shouln't it be int?*/
    void setSpeakerSex(char *sex);
    void setSpeakerAccent(char *accent);

    void setSpeakingMode(char *mode);
    void setChannelCount(int c);
    void setSampleCount(long c);
    void setSampleMin(int m);
    void setSampleMax(int m);
    void setSampleNBytes(int n);
    void setSampleByteFormat(char *format);
    void setRecordDate(char *date);

    void setMicrophone(char *mic);
    void setMicrophonePosition(char *micPos);
    void setRecordSite(char *site);
    void setDosProgram(char *name);
    void setDspProgram(char *name);
    void setLanguage(char *lang);
    void setRecordTime(char *time);
    void setUtteranceId(int n);
    void setCheckSum(long sum);
    void setAllByteCheckSum(long sum);
    void setJin0Frame0(int n);
    void setAverageBackground(int n);
    void setAverageSpeech(int n);
    void setSignalToNoiseRatio(int n);
    void setPromptText(char *text);
    void setPronunciationText(char *text);
    void setRecordingMode(char *text);
    void setFepBegSil(int n);
    void setFepEndSil(int n);
    void setFepXbadj(int n);

  protected:
    char mpCurrentTime[32];

    class FieldDescription
    {
      public:
        char *mFormat;
        char mType;
        union
        {
          int d;
          unsigned u;
          long l;
          double f;
          char *s;
        } mValue;
        BOOL mbInUse;
    };

    static FieldDescription mFieldsInit[];

    FieldDescription *mpFields;  /* each instance gets a copy of mFieldsInit*/
};

inline void
Nist1AHeader::reset(FieldId id)
{
  mpFields[id].mbInUse = FALSE;
}

#define Nist1AHeaderSetDef(fname, argType, unionKey, fieldId)\
  inline void Nist1AHeader::##fname(argType x) {\
    mpFields[fieldId].mbInUse = TRUE;\
    mpFields[fieldId].mValue.##unionKey = x;}

Nist1AHeaderSetDef(setWaveSize, long, l, WAVE_SIZE)
Nist1AHeaderSetDef(setSampleRate, int, d, SAMPLE_RATE)
Nist1AHeaderSetDef(setSampleFormat, char*, s, SAMPLE_FORMAT)
Nist1AHeaderSetDef(setCoefEmphasis, double, f, COEF_EMPHASIS)
Nist1AHeaderSetDef(setSpeakerName, char*, s, SPEAKER_NAME)
Nist1AHeaderSetDef(setSpeakerAge, char*, s, SPEAKER_AGE)
Nist1AHeaderSetDef(setSpeakerSex, char*, s, SPEAKER_SEX)
Nist1AHeaderSetDef(setSpeakerAccent, char*, s, SPEAKER_ACCENT)

Nist1AHeaderSetDef(setSpeakingMode, char*, s, SPEAKING_MODE)
Nist1AHeaderSetDef(setChannelCount, int, d, CHANNEL_COUNT)
Nist1AHeaderSetDef(setSampleCount, long, l, SAMPLE_COUNT)
Nist1AHeaderSetDef(setSampleMin, int, d, SAMPLE_MIN)
Nist1AHeaderSetDef(setSampleMax, int, d, SAMPLE_MAX)
Nist1AHeaderSetDef(setSampleNBytes, int, d, SAMPLE_N_BYTES)
Nist1AHeaderSetDef(setSampleByteFormat, char*, s, SAMPLE_BYTE_FORMAT)
Nist1AHeaderSetDef(setRecordDate, char*, s, RECORD_DATE)

Nist1AHeaderSetDef(setMicrophone, char*, s, MICROPHONE)
Nist1AHeaderSetDef(setMicrophonePosition, char*, s, MICROPHONE_POSITION)
Nist1AHeaderSetDef(setRecordSite, char*, s, RECORD_SITE)
Nist1AHeaderSetDef(setDosProgram, char*, s, DOS_PROGRAM)
Nist1AHeaderSetDef(setDspProgram, char*, s, DSP_PROGRAM)
Nist1AHeaderSetDef(setLanguage, char*, s, LANGUAGE)
Nist1AHeaderSetDef(setRecordTime, char*, s, RECORD_TIME)
Nist1AHeaderSetDef(setUtteranceId, int, d, UTTERANCE_ID)
Nist1AHeaderSetDef(setCheckSum, long, l, CHECK_SUM)
Nist1AHeaderSetDef(setAllByteCheckSum, long, l, ALL_BYTE_CHECK_SUM)
Nist1AHeaderSetDef(setJin0Frame0, int, d, JIN0_FRAME0)
Nist1AHeaderSetDef(setAverageBackground, int, d, AVERAGE_BACKGROUND)
Nist1AHeaderSetDef(setAverageSpeech, int, d, AVERAGE_SPEECH)
Nist1AHeaderSetDef(setSignalToNoiseRatio, int, d, SIGNAL_TO_NOISE)
Nist1AHeaderSetDef(setPromptText, char*, s, PROMPT_TEXT)
Nist1AHeaderSetDef(setPronunciationText, char*, s, PRONUNCIATION_TEXT)
Nist1AHeaderSetDef(setRecordingMode, char*, s, RECORDING_MODE)
Nist1AHeaderSetDef(setFepBegSil, int, d, FEP_BEGSIL)
Nist1AHeaderSetDef(setFepEndSil, int, d, FEP_ENDSIL)
Nist1AHeaderSetDef(setFepXbadj, int, d, FEP_XBADJ)

#undef Nist1AHeaderSetDef

#endif
#endif // DO_CLEANUP_20070723
