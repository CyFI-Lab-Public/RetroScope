/*
 * Copyright 2010 Google Inc.
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
package com.android.accessibility;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * An object that handles Android xml layout files in conjunction with an
 * XMLParser for the purpose of testing for accessibility based on the following
 * rule:
 * <p>
 * If the Element tag is ImageView (or a subclass of ImageView), then the tag
 * must contain a contentDescription attribute.
 * <p>
 * This class also has logic to ascertain the subclasses of ImageView and thus
 * requires the path to an Android sdk jar. The subclasses are saved for
 * application of the above rule when a new XML document tag needs processing.
 *
 * @author dtseng@google.com (David Tseng)
 */
public class AccessibilityValidationContentHandler extends DefaultHandler {
    /** Used to obtain line information within the XML file. */
    private Locator mLocator;
    /** The location of the file we are handling. */
    private final String mPath;
    /** The total number of errors within the current file. */
    private int mValidationErrors = 0;

    /**
     * Element tags we have seen before and determined not to be
     * subclasses of ImageView.
     */
    private final Set<String> mExclusionList = new HashSet<String>();

    /** The path to the Android sdk jar file. */
    private final File mAndroidSdkPath;

    /**
     * The ImageView class stored for easy comparison while handling content. It
     * gets initialized in the {@link AccessibilityValidationHandler}
     * constructor if not already done so.
     */
    private static Class<?> sImageViewElement;

    /**
     * A class loader properly initialized and reusable across files. It gets
     * initialized in the {@link AccessibilityValidationHandler} constructor if
     * not already done so.
     */
    private static ClassLoader sValidationClassLoader;

    /** Attributes we test existence for (for example, contentDescription). */
    private static final HashSet<String> sExpectedAttributes =
            new HashSet<String>();

    /** The object that handles our logging. */
    private static final Logger sLogger = Logger.getLogger("android.accessibility");

    /**
     * Construct an AccessibilityValidationContentHandler object with the file
     * on which validation occurs and a path to the Android sdk jar. Then,
     * initialize the class members if not previously done so.
     * 
     * @throws IllegalArgumentException
     *             when given an invalid Android sdk path or when unable to
     *             locate {@link ImageView} class.
     */
    public AccessibilityValidationContentHandler(String fullyQualifiedPath,
            File androidSdkPath) throws IllegalArgumentException {
        mPath = fullyQualifiedPath;
        mAndroidSdkPath = androidSdkPath;

        initializeAccessibilityValidationContentHandler();
    }

    /**
     * Used to log line numbers of errors in {@link #startElement}.
     */
    @Override
    public void setDocumentLocator(Locator locator) {
        mLocator = locator;
    }

    /**
     * For each subclass of ImageView, test for existence of the specified
     * attributes.
     */
    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes atts) {
        Class<?> potentialClass;
        String classPath = "android.widget." + localName;
        try {
            potentialClass = sValidationClassLoader.loadClass(classPath);
        } catch (ClassNotFoundException cnfException) {
            return; // do nothing as the class doesn't exist.
        }

        // if we already determined this class path isn't a subclass of
        // ImageView, skip it.
        // Otherwise, check to see if it is a subclass.
        if (mExclusionList.contains(classPath)) {
            return;
        } else if (!sImageViewElement.isAssignableFrom(potentialClass)) {
            mExclusionList.add(classPath);
            return;
        }

        boolean hasAttribute = false;
        StringBuilder extendedOutput = new StringBuilder();
        for (int i = 0; i < atts.getLength(); i++) {
            String currentAttribute = atts.getLocalName(i).toLowerCase();
            if (sExpectedAttributes.contains(currentAttribute)) {
                hasAttribute = true;
                break;
            } else if (currentAttribute.equals("id")) {
                extendedOutput.append("|id=" + currentAttribute);
            } else if (currentAttribute.equals("src")) {
                extendedOutput.append("|src=" + atts.getValue(i));
            }
        }

        if (!hasAttribute) {
            if (getValidationErrors() == 0) {
                sLogger.info(mPath);
            }
            sLogger.info(String.format("ln: %s.  Error in %s%s tag.", 
                    mLocator.getLineNumber(), localName, extendedOutput));
            mValidationErrors++;
        }
    }

    /**
     * Returns the total number of errors encountered in this file.
     */
    public int getValidationErrors() {
        return mValidationErrors;
    }

    /**
     * Set the class loader and ImageView class objects that will be used during
     * the startElement validation logic. The class loader encompasses the class
     * paths provided.
     * 
     * @throws ClassNotFoundException
     *             when the ImageView Class object could not be found within the
     *             provided class loader.
     */
    public static void setClassLoaderAndBaseClass(URL[] urlSearchPaths)
            throws ClassNotFoundException {
        sValidationClassLoader = new URLClassLoader(urlSearchPaths);
        sImageViewElement =
            sValidationClassLoader.loadClass("android.widget.ImageView");
    }

    /**
     * Adds an attribute that will be tested for existence in
     * {@link #startElement}. The search will always be case-insensitive.
     */
    private static void addExpectedAttribute(String attribute) {
        sExpectedAttributes.add(attribute.toLowerCase());
    }

    /**
     * Initializes the class loader and {@link ImageView} Class objects.
     * 
     * @throws IllegalArgumentException
     *             when either an invalid path is provided or ImageView cannot
     *             be found in the classpaths.
     */
    private void initializeAccessibilityValidationContentHandler()
            throws IllegalArgumentException {
        if (sValidationClassLoader != null && sImageViewElement != null) {
            return; // These objects are already initialized.
        }
        try {
            setClassLoaderAndBaseClass(new URL[] { mAndroidSdkPath.toURL() });
        } catch (MalformedURLException mUException) {
            throw new IllegalArgumentException("invalid android sdk path",
                    mUException);
        } catch (ClassNotFoundException cnfException) {
            throw new IllegalArgumentException(
                    "Unable to find ImageView class.", cnfException);
        }

        // Add all of the expected attributes.
        addExpectedAttribute("contentDescription");
    }
}
