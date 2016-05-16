/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2009 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard;

import java.io.File;

/**
 * This class checks whether the output is up to date.
 *
 * @author Eric Lafortune
 */
public class UpToDateChecker
{
    private final Configuration configuration;


    /**
     * Creates a new UpToDateChecker with the given configuration.
     */
    public UpToDateChecker(Configuration configuration)
    {
        this.configuration = configuration;
    }


    /**
     * Returns whether the output is up to date, based on the modification times
     * of the input jars, output jars, and library jars (or directories).
     */
    public boolean check()
    {
        long inputLastModified  = configuration.lastModified;
        long outputLastModified = Long.MAX_VALUE;

        ClassPath programJars = configuration.programJars;
        ClassPath libraryJars = configuration.libraryJars;

        // Check the dates of the program jars, if any.
        if (programJars != null)
        {
            for (int index = 0; index < programJars.size(); index++)
            {
                // Break early, if possible.
                if (inputLastModified >= outputLastModified)
                {
                    break;
                }

                // Update the input and output modification times.
                ClassPathEntry classPathEntry = programJars.get(index);
                if (classPathEntry.isOutput())
                {
                    long lastModified = lastModified(classPathEntry.getFile(), true);
                    if (outputLastModified > lastModified)
                    {
                        outputLastModified = lastModified;
                    }
                }
                else
                {
                    long lastModified = lastModified(classPathEntry.getFile(), false);
                    if (inputLastModified < lastModified)
                    {
                        inputLastModified = lastModified;
                    }
                }
            }
        }

        // Check the dates of the library jars, if any.
        if (libraryJars != null)
        {
            for (int index = 0; index < libraryJars.size(); index++)
            {
                // Break early, if possible.
                if (inputLastModified >= outputLastModified)
                {
                    break;
                }

                // Update the input modification time.
                ClassPathEntry classPathEntry = libraryJars.get(index);
                long lastModified = lastModified(classPathEntry.getFile(), false);
                if (inputLastModified < lastModified)
                {
                    inputLastModified = lastModified;
                }
            }
        }

        boolean outputUpToDate = inputLastModified < outputLastModified;
        if (outputUpToDate)
        {
            System.out.println("The output is up to date");
        }

        return outputUpToDate;
    }


    /**
     * Returns the minimum or maximum modification time of the given file or
     * of the files in the given directory (recursively).
     */
    private long lastModified(File file, boolean minimum)
    {
        // Is it a directory?
        if (file.isDirectory())
        {
            // Ignore the directory's modification time; just recurse on its files.
            File[] files = file.listFiles();

            // Still, an empty output directory is probably a sign that it is
            // not up to date.
            long lastModified = files.length != 0 && minimum ?
                Long.MAX_VALUE : 0L;

            for (int index = 0; index < files.length; index++)
            {
                long fileLastModified = lastModified(files[index], minimum);
                if ((lastModified < fileLastModified) ^ minimum)
                {
                    lastModified = fileLastModified;
                }
            }

            return lastModified;
        }
        else
        {
            // Return the file's modification time.
            return file.lastModified();
        }
    }
}
