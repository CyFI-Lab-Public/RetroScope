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

import java.io.*;
import java.util.List;


/**
 * This class represents an entry from a class path: a jar, a war, a zip, an
 * ear, or a directory, with a name and a flag to indicates whether the entry is
 * an input entry or an output entry. Optional filters can be specified for the
 * names of the contained resource/classes, jars, wars, ears, and zips.
 *
 * @author Eric Lafortune
 */
public class ClassPathEntry
{
    private File    file;
    private boolean output;
    private List    filter;
    private List    jarFilter;
    private List    warFilter;
    private List    earFilter;
    private List    zipFilter;


    /**
     * Creates a new ClassPathEntry with the given name and type.
     */
    public ClassPathEntry(File file, boolean isOutput)
    {
        this.file   = file;
        this.output = isOutput;
    }


    /**
     * Returns the path name of the entry.
     */
    public String getName()
    {
        try
        {
            return file.getCanonicalPath();
        }
        catch (IOException ex)
        {
            return file.getPath();
        }
    }


    public File getFile()
    {
        return file;
    }


    public void setFile(File file)
    {
        this.file = file;
    }


    public boolean isOutput()
    {
        return output;
    }


    public void setOutput(boolean output)
    {
        this.output = output;
    }


    public List getFilter()
    {
        return filter;
    }

    public void setFilter(List filter)
    {
        this.filter = filter == null || filter.size() == 0 ? null : filter;
    }


    public List getJarFilter()
    {
        return jarFilter;
    }

    public void setJarFilter(List filter)
    {
        this.jarFilter = filter == null || filter.size() == 0 ? null : filter;
    }


    public List getWarFilter()
    {
        return warFilter;
    }

    public void setWarFilter(List filter)
    {
        this.warFilter = filter == null || filter.size() == 0 ? null : filter;
    }


    public List getEarFilter()
    {
        return earFilter;
    }

    public void setEarFilter(List filter)
    {
        this.earFilter = filter == null || filter.size() == 0 ? null : filter;
    }


    public List getZipFilter()
    {
        return zipFilter;
    }

    public void setZipFilter(List filter)
    {
        this.zipFilter = filter == null || filter.size() == 0 ? null : filter;
    }
}
