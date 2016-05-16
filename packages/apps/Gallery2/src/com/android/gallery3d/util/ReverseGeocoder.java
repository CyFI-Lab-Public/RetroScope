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

package com.android.gallery3d.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.android.gallery3d.common.BlobCache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ReverseGeocoder {
    @SuppressWarnings("unused")
    private static final String TAG = "ReverseGeocoder";
    public static final int EARTH_RADIUS_METERS = 6378137;
    public static final int LAT_MIN = -90;
    public static final int LAT_MAX = 90;
    public static final int LON_MIN = -180;
    public static final int LON_MAX = 180;
    private static final int MAX_COUNTRY_NAME_LENGTH = 8;
    // If two points are within 20 miles of each other, use
    // "Around Palo Alto, CA" or "Around Mountain View, CA".
    // instead of directly jumping to the next level and saying
    // "California, US".
    private static final int MAX_LOCALITY_MILE_RANGE = 20;

    private static final String GEO_CACHE_FILE = "rev_geocoding";
    private static final int GEO_CACHE_MAX_ENTRIES = 1000;
    private static final int GEO_CACHE_MAX_BYTES = 500 * 1024;
    private static final int GEO_CACHE_VERSION = 0;

    public static class SetLatLong {
        // The latitude and longitude of the min latitude point.
        public double mMinLatLatitude = LAT_MAX;
        public double mMinLatLongitude;
        // The latitude and longitude of the max latitude point.
        public double mMaxLatLatitude = LAT_MIN;
        public double mMaxLatLongitude;
        // The latitude and longitude of the min longitude point.
        public double mMinLonLatitude;
        public double mMinLonLongitude = LON_MAX;
        // The latitude and longitude of the max longitude point.
        public double mMaxLonLatitude;
        public double mMaxLonLongitude = LON_MIN;
    }

    private Context mContext;
    private Geocoder mGeocoder;
    private BlobCache mGeoCache;
    private ConnectivityManager mConnectivityManager;
    private static Address sCurrentAddress; // last known address

    public ReverseGeocoder(Context context) {
        mContext = context;
        mGeocoder = new Geocoder(mContext);
        mGeoCache = CacheManager.getCache(context, GEO_CACHE_FILE,
                GEO_CACHE_MAX_ENTRIES, GEO_CACHE_MAX_BYTES,
                GEO_CACHE_VERSION);
        mConnectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public String computeAddress(SetLatLong set) {
        // The overall min and max latitudes and longitudes of the set.
        double setMinLatitude = set.mMinLatLatitude;
        double setMinLongitude = set.mMinLatLongitude;
        double setMaxLatitude = set.mMaxLatLatitude;
        double setMaxLongitude = set.mMaxLatLongitude;
        if (Math.abs(set.mMaxLatLatitude - set.mMinLatLatitude)
                < Math.abs(set.mMaxLonLongitude - set.mMinLonLongitude)) {
            setMinLatitude = set.mMinLonLatitude;
            setMinLongitude = set.mMinLonLongitude;
            setMaxLatitude = set.mMaxLonLatitude;
            setMaxLongitude = set.mMaxLonLongitude;
        }
        Address addr1 = lookupAddress(setMinLatitude, setMinLongitude, true);
        Address addr2 = lookupAddress(setMaxLatitude, setMaxLongitude, true);
        if (addr1 == null)
            addr1 = addr2;
        if (addr2 == null)
            addr2 = addr1;
        if (addr1 == null || addr2 == null) {
            return null;
        }

        // Get current location, we decide the granularity of the string based
        // on this.
        LocationManager locationManager =
                (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        Location location = null;
        List<String> providers = locationManager.getAllProviders();
        for (int i = 0; i < providers.size(); ++i) {
            String provider = providers.get(i);
            location = (provider != null) ? locationManager.getLastKnownLocation(provider) : null;
            if (location != null)
                break;
        }
        String currentCity = "";
        String currentAdminArea = "";
        String currentCountry = Locale.getDefault().getCountry();
        if (location != null) {
            Address currentAddress = lookupAddress(
                    location.getLatitude(), location.getLongitude(), true);
            if (currentAddress == null) {
                currentAddress = sCurrentAddress;
            } else {
                sCurrentAddress = currentAddress;
            }
            if (currentAddress != null && currentAddress.getCountryCode() != null) {
                currentCity = checkNull(currentAddress.getLocality());
                currentCountry = checkNull(currentAddress.getCountryCode());
                currentAdminArea = checkNull(currentAddress.getAdminArea());
            }
        }

        String closestCommonLocation = null;
        String addr1Locality = checkNull(addr1.getLocality());
        String addr2Locality = checkNull(addr2.getLocality());
        String addr1AdminArea = checkNull(addr1.getAdminArea());
        String addr2AdminArea = checkNull(addr2.getAdminArea());
        String addr1CountryCode = checkNull(addr1.getCountryCode());
        String addr2CountryCode = checkNull(addr2.getCountryCode());

        if (currentCity.equals(addr1Locality) || currentCity.equals(addr2Locality)) {
            String otherCity = currentCity;
            if (currentCity.equals(addr1Locality)) {
                otherCity = addr2Locality;
                if (otherCity.length() == 0) {
                    otherCity = addr2AdminArea;
                    if (!currentCountry.equals(addr2CountryCode)) {
                        otherCity += " " + addr2CountryCode;
                    }
                }
                addr2Locality = addr1Locality;
                addr2AdminArea = addr1AdminArea;
                addr2CountryCode = addr1CountryCode;
            } else {
                otherCity = addr1Locality;
                if (otherCity.length() == 0) {
                    otherCity = addr1AdminArea;
                    if (!currentCountry.equals(addr1CountryCode)) {
                        otherCity += " " + addr1CountryCode;
                    }
                }
                addr1Locality = addr2Locality;
                addr1AdminArea = addr2AdminArea;
                addr1CountryCode = addr2CountryCode;
            }
            closestCommonLocation = valueIfEqual(addr1.getAddressLine(0), addr2.getAddressLine(0));
            if (closestCommonLocation != null && !("null".equals(closestCommonLocation))) {
                if (!currentCity.equals(otherCity)) {
                    closestCommonLocation += " - " + otherCity;
                }
                return closestCommonLocation;
            }

            // Compare thoroughfare (street address) next.
            closestCommonLocation = valueIfEqual(addr1.getThoroughfare(), addr2.getThoroughfare());
            if (closestCommonLocation != null && !("null".equals(closestCommonLocation))) {
                return closestCommonLocation;
            }
        }

        // Compare the locality.
        closestCommonLocation = valueIfEqual(addr1Locality, addr2Locality);
        if (closestCommonLocation != null && !("".equals(closestCommonLocation))) {
            String adminArea = addr1AdminArea;
            String countryCode = addr1CountryCode;
            if (adminArea != null && adminArea.length() > 0) {
                if (!countryCode.equals(currentCountry)) {
                    closestCommonLocation += ", " + adminArea + " " + countryCode;
                } else {
                    closestCommonLocation += ", " + adminArea;
                }
            }
            return closestCommonLocation;
        }

        // If the admin area is the same as the current location, we hide it and
        // instead show the city name.
        if (currentAdminArea.equals(addr1AdminArea) && currentAdminArea.equals(addr2AdminArea)) {
            if ("".equals(addr1Locality)) {
                addr1Locality = addr2Locality;
            }
            if ("".equals(addr2Locality)) {
                addr2Locality = addr1Locality;
            }
            if (!"".equals(addr1Locality)) {
                if (addr1Locality.equals(addr2Locality)) {
                    closestCommonLocation = addr1Locality + ", " + currentAdminArea;
                } else {
                    closestCommonLocation = addr1Locality + " - " + addr2Locality;
                }
                return closestCommonLocation;
            }
        }

        // Just choose one of the localities if within a MAX_LOCALITY_MILE_RANGE
        // mile radius.
        float[] distanceFloat = new float[1];
        Location.distanceBetween(setMinLatitude, setMinLongitude,
                setMaxLatitude, setMaxLongitude, distanceFloat);
        int distance = (int) GalleryUtils.toMile(distanceFloat[0]);
        if (distance < MAX_LOCALITY_MILE_RANGE) {
            // Try each of the points and just return the first one to have a
            // valid address.
            closestCommonLocation = getLocalityAdminForAddress(addr1, true);
            if (closestCommonLocation != null) {
                return closestCommonLocation;
            }
            closestCommonLocation = getLocalityAdminForAddress(addr2, true);
            if (closestCommonLocation != null) {
                return closestCommonLocation;
            }
        }

        // Check the administrative area.
        closestCommonLocation = valueIfEqual(addr1AdminArea, addr2AdminArea);
        if (closestCommonLocation != null && !("".equals(closestCommonLocation))) {
            String countryCode = addr1CountryCode;
            if (!countryCode.equals(currentCountry)) {
                if (countryCode != null && countryCode.length() > 0) {
                    closestCommonLocation += " " + countryCode;
                }
            }
            return closestCommonLocation;
        }

        // Check the country codes.
        closestCommonLocation = valueIfEqual(addr1CountryCode, addr2CountryCode);
        if (closestCommonLocation != null && !("".equals(closestCommonLocation))) {
            return closestCommonLocation;
        }
        // There is no intersection, let's choose a nicer name.
        String addr1Country = addr1.getCountryName();
        String addr2Country = addr2.getCountryName();
        if (addr1Country == null)
            addr1Country = addr1CountryCode;
        if (addr2Country == null)
            addr2Country = addr2CountryCode;
        if (addr1Country == null || addr2Country == null)
            return null;
        if (addr1Country.length() > MAX_COUNTRY_NAME_LENGTH || addr2Country.length() > MAX_COUNTRY_NAME_LENGTH) {
            closestCommonLocation = addr1CountryCode + " - " + addr2CountryCode;
        } else {
            closestCommonLocation = addr1Country + " - " + addr2Country;
        }
        return closestCommonLocation;
    }

    private String checkNull(String locality) {
        if (locality == null)
            return "";
        if (locality.equals("null"))
            return "";
        return locality;
    }

    private String getLocalityAdminForAddress(final Address addr, final boolean approxLocation) {
        if (addr == null)
            return "";
        String localityAdminStr = addr.getLocality();
        if (localityAdminStr != null && !("null".equals(localityAdminStr))) {
            if (approxLocation) {
                // TODO: Uncomment these lines as soon as we may translations
                // for Res.string.around.
                // localityAdminStr =
                // mContext.getResources().getString(Res.string.around) + " " +
                // localityAdminStr;
            }
            String adminArea = addr.getAdminArea();
            if (adminArea != null && adminArea.length() > 0) {
                localityAdminStr += ", " + adminArea;
            }
            return localityAdminStr;
        }
        return null;
    }

    public Address lookupAddress(final double latitude, final double longitude,
            boolean useCache) {
        try {
            long locationKey = (long) (((latitude + LAT_MAX) * 2 * LAT_MAX
                    + (longitude + LON_MAX)) * EARTH_RADIUS_METERS);
            byte[] cachedLocation = null;
            if (useCache && mGeoCache != null) {
                cachedLocation = mGeoCache.lookup(locationKey);
            }
            Address address = null;
            NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (cachedLocation == null || cachedLocation.length == 0) {
                if (networkInfo == null || !networkInfo.isConnected()) {
                    return null;
                }
                List<Address> addresses = mGeocoder.getFromLocation(latitude, longitude, 1);
                if (!addresses.isEmpty()) {
                    address = addresses.get(0);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(bos);
                    Locale locale = address.getLocale();
                    writeUTF(dos, locale.getLanguage());
                    writeUTF(dos, locale.getCountry());
                    writeUTF(dos, locale.getVariant());

                    writeUTF(dos, address.getThoroughfare());
                    int numAddressLines = address.getMaxAddressLineIndex();
                    dos.writeInt(numAddressLines);
                    for (int i = 0; i < numAddressLines; ++i) {
                        writeUTF(dos, address.getAddressLine(i));
                    }
                    writeUTF(dos, address.getFeatureName());
                    writeUTF(dos, address.getLocality());
                    writeUTF(dos, address.getAdminArea());
                    writeUTF(dos, address.getSubAdminArea());

                    writeUTF(dos, address.getCountryName());
                    writeUTF(dos, address.getCountryCode());
                    writeUTF(dos, address.getPostalCode());
                    writeUTF(dos, address.getPhone());
                    writeUTF(dos, address.getUrl());

                    dos.flush();
                    if (mGeoCache != null) {
                        mGeoCache.insert(locationKey, bos.toByteArray());
                    }
                    dos.close();
                }
            } else {
                // Parsing the address from the byte stream.
                DataInputStream dis = new DataInputStream(
                        new ByteArrayInputStream(cachedLocation));
                String language = readUTF(dis);
                String country = readUTF(dis);
                String variant = readUTF(dis);
                Locale locale = null;
                if (language != null) {
                    if (country == null) {
                        locale = new Locale(language);
                    } else if (variant == null) {
                        locale = new Locale(language, country);
                    } else {
                        locale = new Locale(language, country, variant);
                    }
                }
                if (!locale.getLanguage().equals(Locale.getDefault().getLanguage())) {
                    dis.close();
                    return lookupAddress(latitude, longitude, false);
                }
                address = new Address(locale);

                address.setThoroughfare(readUTF(dis));
                int numAddressLines = dis.readInt();
                for (int i = 0; i < numAddressLines; ++i) {
                    address.setAddressLine(i, readUTF(dis));
                }
                address.setFeatureName(readUTF(dis));
                address.setLocality(readUTF(dis));
                address.setAdminArea(readUTF(dis));
                address.setSubAdminArea(readUTF(dis));

                address.setCountryName(readUTF(dis));
                address.setCountryCode(readUTF(dis));
                address.setPostalCode(readUTF(dis));
                address.setPhone(readUTF(dis));
                address.setUrl(readUTF(dis));
                dis.close();
            }
            return address;
        } catch (Exception e) {
            // Ignore.
        }
        return null;
    }

    private String valueIfEqual(String a, String b) {
        return (a != null && b != null && a.equalsIgnoreCase(b)) ? a : null;
    }

    public static final void writeUTF(DataOutputStream dos, String string) throws IOException {
        if (string == null) {
            dos.writeUTF("");
        } else {
            dos.writeUTF(string);
        }
    }

    public static final String readUTF(DataInputStream dis) throws IOException {
        String retVal = dis.readUTF();
        if (retVal.length() == 0)
            return null;
        return retVal;
    }
}
