//--------------------------------------------------------------------------
// Parsing of GPS info from exif header.
//
// Matthias Wandel,  Dec 1999 - Dec 2002
//--------------------------------------------------------------------------
#include "jhead.h"

#include <string.h>
#include <utils/Log.h>


#define TAG_GPS_LAT_REF    1
#define TAG_GPS_LAT        2
#define TAG_GPS_LONG_REF   3
#define TAG_GPS_LONG       4
#define TAG_GPS_ALT_REF    5
#define TAG_GPS_ALT        6
#define TAG_GPS_TIMESTAMP  7
#define TAG_GPS_PROCESSING_METHOD 27
#define TAG_GPS_DATESTAMP  29

static TagTable_t GpsTags[]= {
    { 0x00, "GPSVersionID", FMT_BYTE, 4},
    { 0x01, "GPSLatitudeRef", FMT_STRING, 2},
    { 0x02, "GPSLatitude", FMT_URATIONAL, 3},
    { 0x03, "GPSLongitudeRef", FMT_STRING, 2},
    { 0x04, "GPSLongitude", FMT_URATIONAL, 3},
    { 0x05, "GPSAltitudeRef", FMT_BYTE, 1},
    { 0x06, "GPSAltitude", FMT_URATIONAL, 1},
    { 0x07, "GPSTimeStamp", FMT_SRATIONAL, 3},
    { 0x08, "GPSSatellites", FMT_STRING, -1},
    { 0x09, "GPSStatus", FMT_STRING, 2},
    { 0x0A, "GPSMeasureMode", FMT_STRING, 2},
    { 0x0B, "GPSDOP", FMT_SRATIONAL, 1},
    { 0x0C, "GPSSpeedRef", FMT_STRING, 2},
    { 0x0D, "GPSSpeed", FMT_SRATIONAL, 1},
    { 0x0E, "GPSTrackRef", FMT_STRING, 2},
    { 0x0F, "GPSTrack", FMT_SRATIONAL, 1},
    { 0x10, "GPSImgDirectionRef", FMT_STRING, -1},
    { 0x11, "GPSImgDirection", FMT_SRATIONAL, 1},
    { 0x12, "GPSMapDatum", FMT_STRING, -1},
    { 0x13, "GPSDestLatitudeRef", FMT_STRING, 2},
    { 0x14, "GPSDestLatitude", FMT_SRATIONAL, 3},
    { 0x15, "GPSDestLongitudeRef", FMT_STRING, 2},
    { 0x16, "GPSDestLongitude", FMT_SRATIONAL, 3},
    { 0x17, "GPSDestBearingRef", FMT_STRING, 1},
    { 0x18, "GPSDestBearing", FMT_SRATIONAL, 1},
    { 0x19, "GPSDestDistanceRef", FMT_STRING, 2},
    { 0x1A, "GPSDestDistance", FMT_SRATIONAL, 1},
    { 0x1B, "GPSProcessingMethod", FMT_UNDEFINED, -1},
    { 0x1C, "GPSAreaInformation", FMT_STRING, -1},
    { 0x1D, "GPSDateStamp", FMT_STRING, 11},
    { 0x1E, "GPSDifferential", FMT_SSHORT, 1},
};

#define MAX_GPS_TAG  (sizeof(GpsTags) / sizeof(TagTable_t))
#define EXIF_ASCII_PREFIX_LEN (sizeof(ExifAsciiPrefix))

// Define the line below to turn on poor man's debugging output
#undef SUPERDEBUG

#ifdef SUPERDEBUG
#define printf ALOGE
#endif


int IsGpsTag(const char* tag) {
    return strstr(tag, "GPS") == tag;
}

TagTable_t* GpsTagToTagTableEntry(unsigned short tag)
{
    unsigned int i;
    for (i = 0; i < MAX_GPS_TAG; i++) {
        if (GpsTags[i].Tag == tag) {
            printf("found tag %d", tag);
            int format = GpsTags[i].Format;
            if (format == 0) {
                printf("tag %s format not defined", GpsTags[i].Desc);
                return NULL;
            }
            return &GpsTags[i];
        }
    }
    printf("tag %d NOT FOUND", tag);
    return NULL;
}

int GpsTagToFormatType(unsigned short tag)
{
    unsigned int i;
    for (i = 0; i < MAX_GPS_TAG; i++) {
        if (GpsTags[i].Tag == tag) {
            printf("found tag %d", tag);
            int format = GpsTags[i].Format;
            if (format == 0) {
                printf("tag %s format not defined", GpsTags[i].Desc);
                return -1;
            }
            return format;
        }
    }
    printf("tag %d NOT FOUND", tag);
    return -1;
}

int GpsTagNameToValue(const char* tagName)
{
    unsigned int i;
    for (i = 0; i < MAX_GPS_TAG; i++) {
        if (strcmp(GpsTags[i].Desc, tagName) == 0) {
            printf("found GPS tag %s val %d", GpsTags[i].Desc, GpsTags[i].Tag);
            return GpsTags[i].Tag;
        }
    }
    printf("GPS tag %s NOT FOUND", tagName);
    return -1;
}


//--------------------------------------------------------------------------
// Process GPS info directory
//--------------------------------------------------------------------------
void ProcessGpsInfo(unsigned char * DirStart, int ByteCountUnused, unsigned char * OffsetBase, unsigned ExifLength)
{
    int de;
    unsigned a;
    int NumDirEntries;

    NumDirEntries = Get16u(DirStart);
    #define DIR_ENTRY_ADDR(Start, Entry) (Start+2+12*(Entry))

    if (ShowTags){
        printf("(dir has %d entries)\n",NumDirEntries);
    }

    ImageInfo.GpsInfoPresent = TRUE;
    strcpy(ImageInfo.GpsLat, "? ?");
    strcpy(ImageInfo.GpsLong, "? ?");
    ImageInfo.GpsAlt[0] = 0;

    for (de=0;de<NumDirEntries;de++){
        unsigned Tag, Format, Components;
        unsigned char * ValuePtr;
        int ComponentSize;
        unsigned ByteCount;
        unsigned char * DirEntry;
        DirEntry = DIR_ENTRY_ADDR(DirStart, de);

        if (DirEntry+12 > OffsetBase+ExifLength){
            ErrNonfatal("GPS info directory goes past end of exif",0,0);
            return;
        }

        Tag = Get16u(DirEntry);
        Format = Get16u(DirEntry+2);
        Components = Get32u(DirEntry+4);

        if ((Format-1) >= NUM_FORMATS) {
            // (-1) catches illegal zero case as unsigned underflows to positive large.
            ErrNonfatal("Illegal number format %d for tag %04x", Format, Tag);
            continue;
        }

        ComponentSize = BytesPerFormat[Format];
        ByteCount = Components * ComponentSize;

#ifdef SUPERDEBUG
    printf("GPS tag %x format %s #components %d componentsize %d bytecount %d", Tag, formatStr(Format), Components, ComponentSize,
            ByteCount);
#endif

        if (ByteCount > 4){
            unsigned OffsetVal;
            OffsetVal = Get32u(DirEntry+8);
            // If its bigger than 4 bytes, the dir entry contains an offset.
            if (OffsetVal+ByteCount > ExifLength){
                // Bogus pointer offset and / or bytecount value
                ErrNonfatal("Illegal value pointer for tag %04x", Tag,0);
                continue;
            }
            ValuePtr = OffsetBase+OffsetVal;
        }else{
            // 4 bytes or less and value is in the dir entry itself
            ValuePtr = DirEntry+8;
        }

        switch(Tag){
            char FmtString[21];
            char TempString[MAX_BUF_SIZE];
            double Values[3];

            case TAG_GPS_LAT_REF:
                ImageInfo.GpsLat[0] = ValuePtr[0];
                ImageInfo.GpsLatRef[0] = ValuePtr[0];
                ImageInfo.GpsLatRef[1] = '\0';
                break;

            case TAG_GPS_LONG_REF:
                ImageInfo.GpsLong[0] = ValuePtr[0];
                ImageInfo.GpsLongRef[0] = ValuePtr[0];
                ImageInfo.GpsLongRef[1] = '\0';
                break;

            case TAG_GPS_LAT:
            case TAG_GPS_LONG:
                if (Format != FMT_URATIONAL){
                    ErrNonfatal("Inappropriate format (%d) for GPS coordinates!", Format, 0);
                }
                strcpy(FmtString, "%0.0fd %0.0fm %0.0fs");
                for (a=0;a<3;a++){
                    int den, digits;

                    den = Get32s(ValuePtr+4+a*ComponentSize);
                    digits = 0;
                    while (den > 1 && digits <= 6){
                        den = den / 10;
                        digits += 1;
                    }
                    if (digits > 6) digits = 6;
                    FmtString[1+a*7] = (char)('2'+digits+(digits ? 1 : 0));
                    FmtString[3+a*7] = (char)('0'+digits);

                    Values[a] = ConvertAnyFormat(ValuePtr+a*ComponentSize, Format);
                }

                sprintf(TempString, FmtString, Values[0], Values[1], Values[2]);

                if (Tag == TAG_GPS_LAT){
                    strncpy(ImageInfo.GpsLat+2, TempString, 29);
                }else{
                    strncpy(ImageInfo.GpsLong+2, TempString, 29);
                }

                sprintf(TempString, "%d/%d,%d/%d,%d/%d",
                    Get32s(ValuePtr), Get32s(4+(char*)ValuePtr),
                    Get32s(8+(char*)ValuePtr), Get32s(12+(char*)ValuePtr),
                    Get32s(16+(char*)ValuePtr), Get32s(20+(char*)ValuePtr));
                if (Tag == TAG_GPS_LAT){
                    strncpy(ImageInfo.GpsLatRaw, TempString, MAX_BUF_SIZE);
                }else{
                    strncpy(ImageInfo.GpsLongRaw, TempString, MAX_BUF_SIZE);
                }
                break;

            case TAG_GPS_ALT_REF:
                ImageInfo.GpsAlt[0] = (char)(ValuePtr[0] ? '-' : ' ');
                ImageInfo.GpsAltRef = (char)ValuePtr[0];
                break;

            case TAG_GPS_ALT:
                sprintf(ImageInfo.GpsAlt + 1, "%.2fm", 
                    ConvertAnyFormat(ValuePtr, Format));
                ImageInfo.GpsAltRaw.num = Get32u(ValuePtr);
                ImageInfo.GpsAltRaw.denom = Get32u(4+(char *)ValuePtr);
                break;

            case TAG_GPS_TIMESTAMP:
                snprintf(ImageInfo.GpsTimeStamp,
                    sizeof(ImageInfo.GpsTimeStamp), "%d:%d:%d",
                    (int) ConvertAnyFormat(ValuePtr, Format),
                    (int) ConvertAnyFormat(ValuePtr + 8, Format),
                    (int) ConvertAnyFormat(ValuePtr + 16, Format)
                );
                break;

            case TAG_GPS_DATESTAMP:
                strncpy(ImageInfo.GpsDateStamp, (char*)ValuePtr, sizeof(ImageInfo.GpsDateStamp));
                break;

            case TAG_GPS_PROCESSING_METHOD:
                if (ByteCount > EXIF_ASCII_PREFIX_LEN &&
                    memcmp(ValuePtr, ExifAsciiPrefix, EXIF_ASCII_PREFIX_LEN) == 0) {
                    int length =
                        ByteCount < GPS_PROCESSING_METHOD_LEN + EXIF_ASCII_PREFIX_LEN ?
                        ByteCount - EXIF_ASCII_PREFIX_LEN : GPS_PROCESSING_METHOD_LEN;
                    memcpy(ImageInfo.GpsProcessingMethod,
                        (char*)(ValuePtr + EXIF_ASCII_PREFIX_LEN), length);
                    ImageInfo.GpsProcessingMethod[length] = 0;
                } else {
                    ALOGW("Unsupported encoding for GPSProcessingMethod");
                }
                break;
        }

        if (ShowTags){
            // Show tag value.
            if (Tag < MAX_GPS_TAG){
                printf("        %s =", GpsTags[Tag].Desc);
            }else{
                // Show unknown tag
                printf("        Illegal GPS tag %04x=", Tag);
            }

            switch(Format){
                case FMT_UNDEFINED:
                    // Undefined is typically an ascii string.

                case FMT_STRING:
                    // String arrays printed without function call (different from int arrays)
                    {
                        printf("\"");
                        for (a=0;a<ByteCount;a++){
                            int ZeroSkipped = 0;
                            if (ValuePtr[a] >= 32){
                                if (ZeroSkipped){
                                    printf("?");
                                    ZeroSkipped = 0;
                                }
                                putchar(ValuePtr[a]);
                            }else{
                                if (ValuePtr[a] == 0){
                                    ZeroSkipped = 1;
                                }
                            }
                        }
                        printf("\"\n");
                    }
                    break;

                default:
                    // Handle arrays of numbers later (will there ever be?)
                    for (a=0;;){
                        PrintFormatNumber(ValuePtr+a*ComponentSize, Format, ByteCount);
                        if (++a >= Components) break;
                        printf(", ");
                    }
                    printf("\n");
            }
        }
    }
}


