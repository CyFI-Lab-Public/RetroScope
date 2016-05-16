/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.camera2.its;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.Rational;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Class to deal with serializing and deserializing between JSON and Camera2 objects.
 */
public class ItsSerializer {
    public static final String TAG = ItsSerializer.class.getSimpleName();

    private static class MetadataEntry {
        public MetadataEntry(String k, Object v) {
            key = k;
            value = v;
        }
        public String key;
        public Object value;
    }

    @SuppressWarnings("unchecked")
    private static MetadataEntry serializeEntry(Type keyType, Object keyObj, CameraMetadata md)
            throws ItsException {
        CameraMetadata.Key key = (CameraMetadata.Key)keyObj;
        try {
            if (md.get(key) == null) {
                return new MetadataEntry(key.getName(), JSONObject.NULL);
            }
            if (keyType == Integer.class || keyType == Long.class    || keyType == Byte.class ||
                keyType == Float.class   || keyType == Boolean.class || keyType == String.class) {
                return new MetadataEntry(key.getName(), md.get(key));
            } else if (keyType == Rational.class) {
                CameraMetadata.Key<Rational> key2 = (CameraMetadata.Key<Rational>)keyObj;
                JSONObject ratObj = new JSONObject();
                ratObj.put("numerator", md.get(key2).getNumerator());
                ratObj.put("denominator", md.get(key2).getDenominator());
                return new MetadataEntry(key.getName(), ratObj);
            } else if (keyType == android.hardware.camera2.Size.class) {
                CameraMetadata.Key<android.hardware.camera2.Size> key2 =
                        (CameraMetadata.Key<android.hardware.camera2.Size>)keyObj;
                JSONObject sizeObj = new JSONObject();
                sizeObj.put("width", md.get(key2).getWidth());
                sizeObj.put("height", md.get(key2).getHeight());
                return new MetadataEntry(key.getName(), sizeObj);
            } else if (keyType == android.graphics.Rect.class) {
                CameraMetadata.Key<android.graphics.Rect> key2 =
                        (CameraMetadata.Key<android.graphics.Rect>)keyObj;
                JSONObject rectObj = new JSONObject();
                rectObj.put("left", md.get(key2).left);
                rectObj.put("right", md.get(key2).right);
                rectObj.put("top", md.get(key2).top);
                rectObj.put("bottom", md.get(key2).bottom);
                return new MetadataEntry(key.getName(), rectObj);
            } else {
                throw new ItsException(
                        "Unsupported key type in metadata serializer: " + keyType);
            }
        } catch (org.json.JSONException e) {
            throw new ItsException("JSON error for key: " + key.getName() + ": ", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static MetadataEntry serializeArrayEntry(Type keyType, Object keyObj, CameraMetadata md)
            throws ItsException {
        CameraMetadata.Key key = (CameraMetadata.Key)keyObj;
        try {
            if (md.get(key) == null) {
                return new MetadataEntry(key.getName(), JSONObject.NULL);
            }
            Type elmtType = ((GenericArrayType)keyType).getGenericComponentType();
            if (elmtType == int.class  || elmtType == float.class || elmtType == byte.class ||
                elmtType == long.class || elmtType == double.class) {
                return new MetadataEntry(key.getName(), new JSONArray(md.get(key)));
            } else if (elmtType == Rational.class) {
                CameraMetadata.Key<Rational[]> key2 = (CameraMetadata.Key<Rational[]>)keyObj;
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < Array.getLength(md.get(key)); i++) {
                    JSONObject ratObj = new JSONObject();
                    ratObj.put("numerator", md.get(key2)[i].getNumerator());
                    ratObj.put("denominator", md.get(key2)[i].getDenominator());
                    jsonArray.put(ratObj);
                }
                return new MetadataEntry(key.getName(), jsonArray);
            } else if (elmtType == android.hardware.camera2.Size.class) {
                CameraMetadata.Key<android.hardware.camera2.Size[]> key2 =
                        (CameraMetadata.Key<android.hardware.camera2.Size[]>)keyObj;
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < Array.getLength(md.get(key)); i++) {
                    JSONObject sizeObj = new JSONObject();
                    sizeObj.put("width", md.get(key2)[i].getWidth());
                    sizeObj.put("height", md.get(key2)[i].getHeight());
                    jsonArray.put(sizeObj);
                }
                return new MetadataEntry(key.getName(), jsonArray);
            } else if (elmtType == android.graphics.Rect.class) {
                CameraMetadata.Key<android.graphics.Rect[]> key2 =
                        (CameraMetadata.Key<android.graphics.Rect[]>)keyObj;
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < Array.getLength(md.get(key)); i++) {
                    JSONObject rectObj = new JSONObject();
                    rectObj.put("left", md.get(key2)[i].left);
                    rectObj.put("right", md.get(key2)[i].right);
                    rectObj.put("top", md.get(key2)[i].top);
                    rectObj.put("bottom", md.get(key2)[i].bottom);
                    jsonArray.put(rectObj);
                }
                return new MetadataEntry(key.getName(), jsonArray);
            } else if (elmtType == android.hardware.camera2.Face.class) {
                CameraMetadata.Key<android.hardware.camera2.Face[]> key2 =
                        (CameraMetadata.Key<android.hardware.camera2.Face[]>)keyObj;

                // TODO: Serialize an array of faces to JSON.
                // Will also need to deserialize JSON faces in the appropriate method.
                if (Array.getLength(md.get(key)) != 0) {
                    throw new ItsException("Serialization of faces not implemented yet");
                }
                return new MetadataEntry(key.getName(), new JSONArray());

            } else {
                throw new ItsException("Unsupported array type: " + elmtType);
            }
        } catch (org.json.JSONException e) {
            throw new ItsException("JSON error for key: " + key.getName() + ": ", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static JSONObject serialize(CameraMetadata md)
            throws ItsException {
        JSONObject jsonObj = new JSONObject();
        Field[] allFields = md.getClass().getDeclaredFields();
        for (Field field : allFields) {
            if (Modifier.isPublic(field.getModifiers()) &&
                    Modifier.isStatic(field.getModifiers()) &&
                    field.getType() == CameraMetadata.Key.class &&
                    field.getGenericType() instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType)field.getGenericType();
                Type[] argTypes = paramType.getActualTypeArguments();
                if (argTypes.length > 0) {
                    try {
                        Type keyType = argTypes[0];
                        Object keyObj = field.get(md);
                        MetadataEntry entry;
                        if (keyType instanceof GenericArrayType) {
                            entry = serializeArrayEntry(keyType, keyObj, md);
                        } else {
                            entry = serializeEntry(keyType, keyObj, md);
                        }
                        if (entry != null) {
                            jsonObj.put(entry.key, entry.value);
                        }
                    } catch (IllegalAccessException e) {
                        throw new ItsException(
                                "Access error for field: " + field + ": ", e);
                    } catch (org.json.JSONException e) {
                        throw new ItsException(
                                "JSON error for field: " + field + ": ", e);
                    } catch (IllegalArgumentException e) {
                        // TODO: Remove this HACK once all keys are implemented (especially
                        // android.statistics.faces).
                        // If the key isn't plumbed all the way down, can get an exception by
                        // trying to get it. Swallow the exception.
                    }
                }
            }
        }
        return jsonObj;
    }

    @SuppressWarnings("unchecked")
    public static CaptureRequest.Builder deserialize(CaptureRequest.Builder mdDefault,
            JSONObject jsonReq) throws ItsException {
        try {
            Log.i(TAG, "Parsing JSON capture request ...");

            // Iterate over the CameraMetadata reflected fields.
            CaptureRequest.Builder md = mdDefault;
            Field[] allFields = CaptureRequest.class.getDeclaredFields();
            for (Field field : allFields) {
                if (Modifier.isPublic(field.getModifiers()) &&
                        Modifier.isStatic(field.getModifiers()) &&
                        field.getType() == CameraMetadata.Key.class &&
                        field.getGenericType() instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType)field.getGenericType();
                    Type[] argTypes = paramType.getActualTypeArguments();
                    try {
                        if (argTypes.length > 0) {
                            CameraMetadata.Key key = (CameraMetadata.Key)field.get(md);
                            String keyName = key.getName();
                            Type keyType = argTypes[0];

                            // For each reflected CameraMetadata entry, look inside the JSON object
                            // to see if it is being set. If it is found, remove the key from the
                            // JSON object. After this process, there should be no keys left in the
                            // JSON (otherwise an invalid key was specified).

                            if (jsonReq.has(keyName) && !jsonReq.isNull(keyName)) {
                                if (keyType instanceof GenericArrayType) {
                                    Type elmtType =
                                            ((GenericArrayType)keyType).getGenericComponentType();
                                    JSONArray ja = jsonReq.getJSONArray(keyName);
                                    Object val[] = new Object[ja.length()];
                                    for (int i = 0; i < ja.length(); i++) {
                                        if (elmtType == int.class) {
                                            Array.set(val, i, ja.getInt(i));
                                        } else if (elmtType == byte.class) {
                                            Array.set(val, i, (byte)ja.getInt(i));
                                        } else if (elmtType == float.class) {
                                            Array.set(val, i, (float)ja.getDouble(i));
                                        } else if (elmtType == long.class) {
                                            Array.set(val, i, ja.getLong(i));
                                        } else if (elmtType == double.class) {
                                            Array.set(val, i, ja.getDouble(i));
                                        } else if (elmtType == boolean.class) {
                                            Array.set(val, i, ja.getBoolean(i));
                                        } else if (elmtType == String.class) {
                                            Array.set(val, i, ja.getString(i));
                                        } else if (elmtType == android.hardware.camera2.Size.class){
                                            JSONObject obj = ja.getJSONObject(i);
                                            Array.set(val, i, new android.hardware.camera2.Size(
                                                    obj.getInt("width"), obj.getInt("height")));
                                        } else if (elmtType == android.graphics.Rect.class) {
                                            JSONObject obj = ja.getJSONObject(i);
                                            Array.set(val, i, new android.graphics.Rect(
                                                    obj.getInt("left"), obj.getInt("top"),
                                                    obj.getInt("bottom"), obj.getInt("right")));
                                        } else if (elmtType == Rational.class) {
                                            JSONObject obj = ja.getJSONObject(i);
                                            Array.set(val, i, new Rational(
                                                    obj.getInt("numerator"),
                                                    obj.getInt("denominator")));
                                        } else {
                                            throw new ItsException(
                                                    "Failed to parse key from JSON: " + keyName);
                                        }
                                    }
                                    if (val != null) {
                                        Log.i(TAG, "Set: "+keyName+" -> "+Arrays.toString(val));
                                        md.set(key, val);
                                        jsonReq.remove(keyName);
                                    }
                                } else {
                                    Object val = null;
                                    if (keyType == Integer.class) {
                                        val = jsonReq.getInt(keyName);
                                    } else if (keyType == Byte.class) {
                                        val = (byte)jsonReq.getInt(keyName);
                                    } else if (keyType == Double.class) {
                                        val = jsonReq.getDouble(keyName);
                                    } else if (keyType == Long.class) {
                                        val = jsonReq.getLong(keyName);
                                    } else if (keyType == Float.class) {
                                        val = (float)jsonReq.getDouble(keyName);
                                    } else if (keyType == Boolean.class) {
                                        val = jsonReq.getBoolean(keyName);
                                    } else if (keyType == String.class) {
                                        val = jsonReq.getString(keyName);
                                    } else if (keyType == android.hardware.camera2.Size.class) {
                                        JSONObject obj = jsonReq.getJSONObject(keyName);
                                        val = new android.hardware.camera2.Size(
                                                obj.getInt("width"), obj.getInt("height"));
                                    } else if (keyType == android.graphics.Rect.class) {
                                        JSONObject obj = jsonReq.getJSONObject(keyName);
                                        val = new android.graphics.Rect(
                                                obj.getInt("left"), obj.getInt("top"),
                                                obj.getInt("right"), obj.getInt("bottom"));
                                    } else if (keyType == Rational.class) {
                                        JSONObject obj = jsonReq.getJSONObject(keyName);
                                        val = new Rational(obj.getInt("numerator"),
                                                           obj.getInt("denominator"));
                                    } else {
                                        throw new ItsException(
                                                "Failed to parse key from JSON: " + keyName);
                                    }
                                    if (val != null) {
                                        Log.i(TAG, "Set: " + keyName + " -> " + val);
                                        md.set(key ,val);
                                        jsonReq.remove(keyName);
                                    }
                                }
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        // TODO: Remove this HACK once all keys are implemented (especially
                        // android.statistics.faces).
                        // If the key isn't plumbed all the way down, can get an exception by
                        // trying to get it. Swallow the exception.
                    }
                }
            }

            // Ensure that there were no invalid keys in the JSON request object.
            if (jsonReq.length() != 0) {
                throw new ItsException("Invalid JSON key(s): " + jsonReq.toString());
            }

            Log.i(TAG, "Parsing JSON capture request completed");
            return md;
        } catch (java.lang.IllegalAccessException e) {
            throw new ItsException("Access error: ", e);
        } catch (org.json.JSONException e) {
            throw new ItsException("JSON error: ", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<CaptureRequest.Builder> deserializeRequestList(
            CameraDevice device, JSONObject jsonObjTop)
            throws ItsException {
        try {
            List<CaptureRequest.Builder> requests = null;
            if (jsonObjTop.has("captureRequest")) {
                JSONObject jsonReq = jsonObjTop.getJSONObject("captureRequest");
                CaptureRequest.Builder templateReq = device.createCaptureRequest(
                        CameraDevice.TEMPLATE_STILL_CAPTURE);
                requests = new LinkedList<CaptureRequest.Builder>();
                requests.add(deserialize(templateReq, jsonReq));
            } else if (jsonObjTop.has("captureRequestList")) {
                JSONArray jsonReqs = jsonObjTop.getJSONArray("captureRequestList");
                requests = new LinkedList<CaptureRequest.Builder>();
                for (int i = 0; i < jsonReqs.length(); i++) {
                    CaptureRequest.Builder templateReq = device.createCaptureRequest(
                            CameraDevice.TEMPLATE_STILL_CAPTURE);
                    requests.add(
                        deserialize(templateReq, jsonReqs.getJSONObject(i)));
                }
            }
            return requests;
        } catch (org.json.JSONException e) {
            throw new ItsException("JSON error: ", e);
        } catch (android.hardware.camera2.CameraAccessException e) {
            throw new ItsException("Access error: ", e);
        }
    }
}
