/*******************************************************************************
 * Copyright (c) 2011 Google, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Google, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.wb.internal.core;

import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * The DesignerPlugin class is the "nexus" of the propertysheet. In WindowBuilder,
 * it's the plugin activator, and contains a number of important utility methods, such
 * as resource loading, logging, obtaining a display and shell, etc.
 * <p>
 * In the AOSP fork, most of the functionality has been ripped out, except for the
 * above mentioned pieces, and this class is no longer a plugin. Instead, it *delegates*
 * to the plugin which initializes it via the {@link #initialize} method for things
 * like logging. For things like image loading, it has its own local code such that
 * it can find its image resources locally instead of requiring the embedding plugin
 * to copy the images into its own jar.
 * <p>
 * "DesignerPlugin" is not a very good name for this class since it is not a plugin,
 * but it was left that way to avoid modifying all the various propertysheet classes;
 * we'd like to keep those as unmodified as possible to make absorbing future
 * WindowBuilder improvements as easy as possible.
 */
public class DesignerPlugin {
    private static AbstractUIPlugin sPlugin;
    private static String sPluginId;

    /**
     * Initialize the property sheet for use in the ADT plugin
     *
     * @param hostPlugin the plugin to embed the property sheet
     * @param pluginId the id of the plugin to use in status messages etc
     * @param isWindows whether we're running on Windows
     * @param isMac whether we're running on Mac
     * @param isLinux whether we're running on Linux
     */
    public static void initialize(AbstractUIPlugin hostPlugin, String pluginId,
            boolean isWindows, boolean isMac, boolean isLinux) {
        assert sPlugin == null; // Can only be used by one client in the same classloader
        sPlugin = hostPlugin;
        sPluginId = pluginId;
        EnvironmentUtils.IS_WINDOWS = isWindows;
        EnvironmentUtils.IS_MAC = isMac;
        EnvironmentUtils.IS_LINUX = isLinux;
    }

    /**
     * Dispose the propertysheet library: free up images from the cache, unregister the
     * plugin reference etc.
     */
    public static void dispose() {
        sPlugin = null;
        for (Image image : sImageCache.values()) {
            image.dispose();
        }
        sImageCache.clear();
        sDescriptorCache.clear();
    }

    /**
     * Reads the contents of an {@link InputStreamReader} using the default
     * platform encoding and return it as a String. This method will close the
     * input stream.
     *
     * @param inputStream the input stream to be read from
     * @param charset the charset to use
     * @return the String read from the stream, or null if there was an error
     */
    public static String readFile(InputStream inputStream, Charset charset) {
        if (inputStream == null) {
            return null;
        }
        Closeable closeMe = inputStream;
        try {
            final InputStreamReader isr = new InputStreamReader(inputStream, charset);
            closeMe = isr;
            try {
                return CharStreams.toString(isr);
            } catch (Exception ioe) {
                // pass -- ignore files we can't read
                return null;
            }
        } finally {
            Closeables.closeQuietly(closeMe);
        }
    }

    /**
     * @return the instance of {@link DesignerPlugin}
     */
    public static AbstractUIPlugin getDefault() {
        assert sPlugin != null;
        return sPlugin;
    }

    // //////////////////////////////////////////////////////////////////////////
    //
    // Display/Shell
    //
    // //////////////////////////////////////////////////////////////////////////
    /**
     * @return the {@link Display} instance, current (if in GUI thread) or
     *         default.
     */
    public static Display getStandardDisplay() {
        Display display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }
        return display;
    }

    /**
     * @return the active {@link IWorkbenchWindow}.
     */
    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        return getDefault().getWorkbench().getActiveWorkbenchWindow();
    }

    /**
     * @return the {@link Shell} of active {@link IWorkbenchWindow}.
     */
    public static Shell getShell() {
        if (getActiveWorkbenchWindow() != null) {
            return getActiveWorkbenchWindow().getShell();
        }
        return null;
    }

    /**
     * Logs given {@link IStatus} into Eclipse .log.
     */
    public static void log(IStatus status) {
        getDefault().getLog().log(status);
    }

    /**
     * Logs {@link IStatus} with given message into Eclipse .log.
     */
    public static void log(String message) {
        log(new Status(IStatus.INFO, sPluginId, IStatus.INFO, message, null));
    }

    /**
     * Logs {@link IStatus} with given exception into Eclipse .log.
     */
    public static void log(Throwable e) {
        Status status = new Status(IStatus.ERROR, sPluginId, "", e);
        getDefault().getLog().log(status);
    }

    /**
     * Logs {@link IStatus} with given message and exception into Eclipse .log.
     */
    public static void log(String message, Throwable e) {
        log(createStatus(message, e));
    }

    /**
     * Creates {@link IStatus} for given message and exception.
     */
    public static Status createStatus(String message, Throwable e) {
        return new Status(IStatus.ERROR, "wb", IStatus.ERROR, message, e) {
            @Override
            public boolean isMultiStatus() {
                return true;
            }
        };
    }

    // //////////////////////////////////////////////////////////////////////////
    //
    // Resources
    //
    // //////////////////////////////////////////////////////////////////////////
    private static Map<String, ImageDescriptor> sDescriptorCache = Maps.newHashMap();
    private static Map<String, Image> sImageCache = Maps.newHashMap();

    public static Image getImage(String path) {
        Image image = sImageCache.get(path);
        if (image == null) {
            ImageDescriptor descriptor = getImageDescriptor(path);
            if (descriptor != null) {
                image = descriptor.createImage();
            }
            sImageCache.put(path, image);
        }
        return image;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        ImageDescriptor descriptor = sDescriptorCache.get(path);
        if (descriptor == null) {
            URL url = DesignerPlugin.class.getResource("icons/" + path); //$NON-NLS-1$
            if (url != null) {
                descriptor = ImageDescriptor.createFromURL(url);
                sDescriptorCache.put(path, descriptor);
            }
        }
        return descriptor;
    }
}
