/* Copyright 2009 Google Inc. All Rights Reserved.
 * Derived from code Copyright (C) 2003 Vladimir Roubtsov.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/cpl-v10.html
 *
 * $Id$
 */

package com.vladium.emma.report.lcov;

import com.vladium.emma.EMMARuntimeException;
import com.vladium.emma.IAppErrorCodes;
import com.vladium.emma.data.ClassDescriptor;
import com.vladium.emma.data.ICoverageData;
import com.vladium.emma.data.IMetaData;
import com.vladium.emma.report.AbstractReportGenerator;
import com.vladium.emma.report.AllItem;
import com.vladium.emma.report.ClassItem;
import com.vladium.emma.report.IItem;
import com.vladium.emma.report.ItemComparator;
import com.vladium.emma.report.MethodItem;
import com.vladium.emma.report.PackageItem;
import com.vladium.emma.report.SourcePathCache;
import com.vladium.emma.report.SrcFileItem;
import com.vladium.util.Descriptors;
import com.vladium.util.Files;
import com.vladium.util.IProperties;
import com.vladium.util.IntObjectMap;
import com.vladium.util.asserts.$assert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Vlad Roubtsov, (C) 2003
 * @author Tim Baverstock, (C) 2009
 *
 * Generates LCOV format files:
 *    http://manpages.ubuntu.com/manpages/karmic/man1/geninfo.1.html
 */
public final class ReportGenerator extends AbstractReportGenerator
                                   implements IAppErrorCodes
{
    public String getType()
    {
        return TYPE;
    }

    /**
     * Queue-based visitor, starts with the root, node visits enqueue child
     * nodes.
     */
    public void process(final IMetaData mdata,
                        final ICoverageData cdata,
                        final SourcePathCache cache,
                        final IProperties properties)
        throws EMMARuntimeException
    {
        initialize(mdata, cdata, cache, properties);

        long start = 0;
        long end;
        final boolean trace1 = m_log.atTRACE1();

        if (trace1)
        {
            start = System.currentTimeMillis();
        }

        m_queue = new LinkedList();
        for (m_queue.add(m_view.getRoot()); !m_queue.isEmpty(); )
        {
            final IItem head = (IItem) m_queue.removeFirst();
            head.accept(this, null);
        }
        close();

        if (trace1)
        {
            end = System.currentTimeMillis();
            m_log.trace1("process", "[" + getType() + "] report generated in "
                         + (end - start) + " ms");
        }
    }

    public void cleanup()
    {
        m_queue = null;
        close();
        super.cleanup();
    }


    /**
    * Visitor for top-level node; opens output file, enqueues packages.
    */
    public Object visit(final AllItem item, final Object ctx)
    {
        File outFile = m_settings.getOutFile();
        if (outFile == null)
        {
            outFile = new File("coverage.lcov");
            m_settings.setOutFile(outFile);
        }

        final File fullOutFile = Files.newFile(m_settings.getOutDir(), outFile);

        m_log.info("writing [" + getType() + "] report to ["
                   + fullOutFile.getAbsolutePath() + "] ...");

        openOutFile(fullOutFile, m_settings.getOutEncoding(), true);

        // Enqueue packages
        final ItemComparator order =
                m_typeSortComparators[PackageItem.getTypeMetadata().getTypeID()];
        for (Iterator packages = item.getChildren(order); packages.hasNext(); )
        {
            final IItem pkg = (IItem) packages.next();
            m_queue.addLast(pkg);
        }

        return ctx;
    }

    /**
     * Visitor for packages; enqueues source files contained by the package.
     */
    public Object visit(final PackageItem item, final Object ctx)
    {
        if (m_verbose)
        {
            m_log.verbose("  report: processing package [" + item.getName() + "] ...");
        }

        // Enqueue source files
        int id = m_srcView
                 ? SrcFileItem.getTypeMetadata().getTypeID()
                 : ClassItem.getTypeMetadata().getTypeID();
        final ItemComparator order = m_typeSortComparators[id];
        for (Iterator srcORclsFiles = item.getChildren(order);
             srcORclsFiles.hasNext();
            )
        {
            final IItem srcORcls = (IItem) srcORclsFiles.next();
            m_queue.addLast(srcORcls);
        }

        return ctx;
    }

    /**
     * Visitor for source files: doesn't use the enqueue mechanism to examine
     * deeper nodes because it writes the 'end_of_record' decoration here.
     */
    public Object visit (final SrcFileItem item, final Object ctx)
    {
        row("SF:".concat(item.getFullVMName()));

        // TODO: Enqueue ClassItems, then an 'end_of_record' object

        emitFileCoverage(item);

        row("end_of_record");
        return ctx;
    }

    /** Issue a coverage report for all lines in the file, and for each
     * function in the file.
     */
    private void emitFileCoverage(final SrcFileItem item)
    {
        if ($assert.ENABLED)
        {
            $assert.ASSERT(item != null, "null input: item");
        }

        final String fileName = item.getFullVMName();

        final String packageVMName = ((PackageItem) item.getParent()).getVMName();

        if (!m_hasLineNumberInfo)
        {
            m_log.info("source file '"
                       + Descriptors.combineVMName(packageVMName, fileName)
                       + "' has no line number information");
        }
        boolean success = false;

        try
        {
            // For each class in the file, for each method in the class,
            // examine the execution blocks in the method until one with
            // coverage is found. Report coverage or non-coverage on the
            // strength of that one block (much as for now, a line is 'covered'
            // if it's partially covered).

            // TODO: Intertwingle method records and line records

            {
                final ItemComparator order = m_typeSortComparators[
                        ClassItem.getTypeMetadata().getTypeID()];
                int clsIndex = 0;
                for (Iterator classes = item.getChildren(order);
                     classes.hasNext();
                     ++clsIndex)
                {
                    final ClassItem cls = (ClassItem) classes.next();

                    final String className = cls.getName();

                    ClassDescriptor cdesc = cls.getClassDescriptor();

                    // [methodid][blocksinmethod]
                    boolean[][] ccoverage = cls.getCoverage();

                    final ItemComparator order2 = m_typeSortComparators[
                            MethodItem.getTypeMetadata().getTypeID()];
                    for (Iterator methods = cls.getChildren(order2); methods.hasNext(); )
                    {
                        final MethodItem method = (MethodItem) methods.next();
                        String mname = method.getName();
                        final int methodID = method.getID();

                        boolean covered = false;
                        if (ccoverage != null)
                        {
                            if ($assert.ENABLED)
                            {
                                $assert.ASSERT(ccoverage.length > methodID, "index bounds");
                                $assert.ASSERT(ccoverage[methodID] != null, "null: coverage");
                                $assert.ASSERT(ccoverage[methodID].length > 0, "empty array");
                            }
                            covered = ccoverage[methodID][0];
                        }

                        row("FN:" + method.getFirstLine() + "," + className + "::" + mname);
                        row("FNDA:" + (covered ? 1 : 0) + "," + className + "::" + mname);
                    }
                }
            }

            // For each line in the file, emit a DA.

            {
                final int unitsType = m_settings.getUnitsType();
                // line num:int -> SrcFileItem.LineCoverageData
                IntObjectMap lineCoverageMap = null;
                int[] lineCoverageKeys = null;

                lineCoverageMap = item.getLineCoverage();
                $assert.ASSERT(lineCoverageMap != null, "null: lineCoverageMap");
                lineCoverageKeys = lineCoverageMap.keys();
                java.util.Arrays.sort(lineCoverageKeys);

                for (int i = 0; i < lineCoverageKeys.length; ++i)
                {
                    int l = lineCoverageKeys[i];
                    final SrcFileItem.LineCoverageData lCoverageData =
                            (SrcFileItem.LineCoverageData) lineCoverageMap.get(l);

                    if ($assert.ENABLED)
                    {
                        $assert.ASSERT(lCoverageData != null, "lCoverage is null");
                    }
                    switch (lCoverageData.m_coverageStatus)
                    {
                        case SrcFileItem.LineCoverageData.LINE_COVERAGE_ZERO:
                            row("DA:" + l + ",0");
                            break;

                        case SrcFileItem.LineCoverageData.LINE_COVERAGE_PARTIAL:
                            // TODO: Add partial coverage support to LCOV
                            row("DA:" + l + ",1");
                            break;

                        case SrcFileItem.LineCoverageData.LINE_COVERAGE_COMPLETE:
                            row("DA:" + l + ",1");
                            break;

                        default:
                            $assert.ASSERT(false, "invalid line coverage status: "
                                           + lCoverageData.m_coverageStatus);

                    } // end of switch
                }
            }

            success = true;
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.out);
            success = false;
        }

        if (!success)
        {
            m_log.info("[source file '"
                       + Descriptors.combineVMName(packageVMName, fileName)
                       + "' not found in sourcepath]");
        }
    }

    public Object visit (final ClassItem item, final Object ctx)
    {
        return ctx;
    }

    private void row(final StringBuffer str)
    {
        if ($assert.ENABLED)
        {
            $assert.ASSERT(str != null, "str = null");
        }

        try
        {
            m_out.write(str.toString());
            m_out.newLine();
        }
        catch (IOException ioe)
        {
            throw new EMMARuntimeException(IAppErrorCodes.REPORT_IO_FAILURE, ioe);
        }
    }

    private void row(final String str)
    {
        if ($assert.ENABLED)
        {
            $assert.ASSERT(str != null, "str = null");
        }

        try
        {
            m_out.write(str);
            m_out.newLine();
        }
        catch (IOException ioe)
        {
            throw new EMMARuntimeException(IAppErrorCodes.REPORT_IO_FAILURE, ioe);
        }
    }

    private void close()
    {
        if (m_out != null)
        {
            try
            {
                m_out.flush();
                m_out.close();
            }
            catch (IOException ioe)
            {
                throw new EMMARuntimeException(IAppErrorCodes.REPORT_IO_FAILURE, ioe);
            }
            finally
            {
                m_out = null;
            }
        }
    }

    private void openOutFile(final File file, final String encoding, final boolean mkdirs)
    {
        try
        {
            if (mkdirs)
            {
                final File parent = file.getParentFile();
                if (parent != null)
                {
                    parent.mkdirs();
                }
            }
            file.delete();
            if (file.exists())
            {
                throw new EMMARuntimeException("Failed to delete " + file);
            }
            m_out = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), encoding),
                    IO_BUF_SIZE);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new EMMARuntimeException(uee);
        }
        catch (IOException fnfe) // FileNotFoundException
        {
            // note: in J2SDK 1.3 FileOutputStream constructor's throws clause
            // was narrowed to FileNotFoundException:
            throw new EMMARuntimeException(fnfe);
        }
    }

    private LinkedList /* IITem */ m_queue;
    private BufferedWriter m_out;

    private static final String TYPE = "lcov";

    private static final int IO_BUF_SIZE = 32 * 1024;
}

