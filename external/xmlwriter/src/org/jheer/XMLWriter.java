/**
 * Copyright (c) 2004-2006 Regents of the University of California.
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

  1. Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright
  notice and this list of conditions.

  3. The name of the University may not be used to endorse or promote products 
  derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
  OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  SUCH DAMAGE.
 */
 
package org.jheer;

//import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Utility class for writing XML files. This class provides convenience
 * methods for creating XML documents, such as starting and ending
 * tags, and adding content and comments. This class handles correct
 * XML formatting and will properly escape text to ensure that the
 * text remains valid XML.
 * 
 * <p>To use this class, create a new instance with the desired
 * [Print]FileWriter to write the XML to. Call the {@link #begin()} or
 * {@link #begin(String, int)} method when ready to start outputting
 * XML. Then use the provided methods to generate the XML file.
 * Finally, call either the {@link #finish()} or {@link #finish(String)}
 * methods to signal the completion of the file.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 *
 * Modified to take a FileWriter and now throws IOException.
 */
 
public class XMLWriter {
    
//    private PrintWriter m_out;
    private FileWriter m_out;
    private int m_bias = 0;
    private int m_tab;
    private ArrayList m_tagStack = new ArrayList();
    
    /**
     * Create a new XMLWriter.
     * @param out the  FileWriter to write the XML to
     */
//    public XMLWriter(PrintWriter out) {
    public XMLWriter(FileWriter out) {
        this(out, 2);
    }

    /**
     * Create a new XMLWriter.
     * @param out the FileWriter to write the XML to
     * @param tabLength the number of spaces to use for each
     *  level of indentation in the XML file
     */
//    public XMLWriter(PrintWriter out, int tabLength) {
    public XMLWriter(FileWriter out, int tabLength) {
        m_out = out;
        m_tab = 2;
    }
    
    /**
     * Write <em>unescaped</em> text into the XML file. To write
     * escaped text, use the {@link #content(String)} method instead.
     * @param s the text to write. This String will not be escaped.
     */
    public void write(String s) throws IOException {
        m_out.write(s);
    }

    /**
     * Write <em>unescaped</em> text into the XML file, followed by
     * a newline. To write escaped text, use the {@link #content(String)}
     * method instead.
     * @param s the text to write. This String will not be escaped.
     */
    public void writeln(String s) throws IOException {
        m_out.write(s);
        m_out.write("\n");
    }
    
    /**
     * Write a newline into the XML file.
     */
    public void writeln() throws IOException {
        m_out.write("\n");
    }
    
    /**
     * Begin the XML document. This must be called before any other
     * formatting methods. This method writes an XML header into
     * the top of the output stream.
     */
    public void begin() throws IOException {
        m_out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writeln();
    }
    
    /**
     * Begin the XML document. This must be called before any other
     * formatting methods. This method writes an XML header into
     * the top of the output stream, plus additional header text
     * provided by the client
     * @param header header text to insert into the document
     * @param bias the spacing bias to use for all subsequent indenting
     */
    public void begin(String header, int bias) throws IOException {
        begin();
        m_out.write(header);
        m_bias = bias;
    }
    
    /**
     * Write a comment in the XML document. The comment will be written
     * according to the current spacing and followed by a newline.
     * @param comment the comment text
     */
    public void comment(String comment) throws IOException {
        spacing();
        m_out.write("<!-- ");
        m_out.write(comment);
        m_out.write(" -->");
        writeln();
    }
    
    /**
     * Internal method for writing a tag with attributes.
     * @param tag the tag name
     * @param names the names of the attributes
     * @param values the values of the attributes
     * @param nattr the number of attributes
     * @param close true to close the tag, false to leave it
     * open and adjust the spacing
     */
    protected void tag(String tag, String[] names, String[] values, 
            int nattr, boolean close) throws IOException
    {
        spacing();
        m_out.write('<');
        m_out.write(tag);
        for ( int i=0; i<nattr; ++i ) {
            m_out.write(' ');
            m_out.write(names[i]);
            m_out.write('=');
            m_out.write('\"');
            escapeString(values[i]);
            m_out.write('\"');
        }
        if ( close ) m_out.write('/');
        m_out.write('>');
        writeln();
        
        if ( !close ) {
            m_tagStack.add(tag);
        }
    }
    
    /**
     * Write a closed tag with attributes. The tag will be followed by a
     * newline.
     * @param tag the tag name
     * @param names the names of the attributes
     * @param values the values of the attributes
     * @param nattr the number of attributes
     */
    public void tag(String tag, String[] names, String[] values, int nattr) throws IOException
    {
        tag(tag, names, values, nattr, true);
    }
    
    /**
     * Write a start tag with attributes. The tag will be followed by a
     * newline, and the indentation level will be increased.
     * @param tag the tag name
     * @param names the names of the attributes
     * @param values the values of the attributes
     * @param nattr the number of attributes
     */
    public void start(String tag, String[] names, String[] values, int nattr) throws IOException
    {
        tag(tag, names, values, nattr, false);
    }
       
    /**
     * Write a new attribut to an existing tag.  The attribute will be followed by a newline.
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
     public void addAttribute(String name, String value) throws IOException {
        spacing();
        m_out.write(name);
        m_out.write('=');
        m_out.write('\"');
        escapeString(value);
        m_out.write('\"');
        writeln();
     }
     
     /**
     * Internal method for writing a tag with a single attribute.
     * @param tag the tag name
     * @param name the name of the attribute
     * @param value the value of the attribute
     * @param close true to close the tag, false to leave it
     * open and adjust the spacing
     */
    protected void tag(String tag, String name, String value, boolean close) throws IOException {
        spacing();
        m_out.write('<');
        m_out.write(tag);
        m_out.write(' ');
        m_out.write(name);
        m_out.write('=');
        m_out.write('\"');
        escapeString(value);
        m_out.write('\"');
        if ( close ) m_out.write('/');
        m_out.write('>');
        writeln();
        
        if ( !close ) {
            m_tagStack.add(tag);
        }
    }
    
    /**
     * Write a closed tag with one attribute. The tag will be followed by a
     * newline.
     * @param tag the tag name
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    public void tag(String tag, String name, String value) throws IOException
    {
        tag(tag, name, value, true);
    }
    
    /**
     * Write a start tag with one attribute. The tag will be followed by a
     * newline, and the indentation level will be increased.
     * @param tag the tag name
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    public void start(String tag, String name, String value) throws IOException
    {
        tag(tag, name, value, false);
    }
    
    /**
     * Internal method for writing a tag with attributes.
     * @param tag the tag name
     * @param names the names of the attributes
     * @param values the values of the attributes
     * @param nattr the number of attributes
     * @param close true to close the tag, false to leave it
     * open and adjust the spacing
     */
    protected void tag(String tag, ArrayList names, ArrayList values,
            int nattr, boolean close) throws IOException
    {
        spacing();
        m_out.write('<');
        m_out.write(tag);
        for ( int i=0; i<nattr; ++i ) {
            m_out.write(' ');
            m_out.write((String)names.get(i));
            m_out.write('=');
            m_out.write('\"');
            escapeString((String)values.get(i));
            m_out.write('\"');
        }
        if ( close ) m_out.write('/');
        m_out.write('>');
        writeln();
        
        if ( !close ) {
            m_tagStack.add(tag);
        }
    }
    
    /**
     * Write a closed tag with attributes. The tag will be followed by a
     * newline.
     * @param tag the tag name
     * @param names the names of the attributes
     * @param values the values of the attributes
     * @param nattr the number of attributes
     */
    public void tag(String tag, ArrayList names, ArrayList values, int nattr) throws IOException
    {
        tag(tag, names, values, nattr, true);
    }
    
    /**
     * Write a start tag with attributes. The tag will be followed by a
     * newline, and the indentation level will be increased.
     * @param tag the tag name
     * @param names the names of the attributes
     * @param values the values of the attributes
     * @param nattr the number of attributes
     */
    public void start(String tag, ArrayList names, ArrayList values, int nattr) throws IOException
    {
        tag(tag, names, values, nattr, false);
    }
    
    /**
     * Write a start tag without attributes. The tag will be followed by a
     * newline, and the indentation level will be increased.
     * @param tag the tag name
     */
    public void start(String tag) throws IOException {
        tag(tag, (String[])null, null, 0, false);
    }

    /**
     * Close the most recently opened tag. The tag will be followed by a
     * newline, and the indentation level will be decreased.
     */
    public void end() throws IOException {
        String tag = (String)m_tagStack.remove(m_tagStack.size()-1);
        spacing();
        m_out.write('<');
        m_out.write('/');
        m_out.write(tag);
        m_out.write('>');
        writeln();
    }
    
    /**
     * Write a new content tag with a single attribute, consisting of an
     * open tag, content text, and a closing tag, all on one line.
     * @param tag the tag name
     * @param name the name of the attribute
     * @param value the value of the attribute, this text will be escaped
     * @param content the text content, this text will be escaped
     */
    public void contentTag(String tag, String name, String value, String content) throws IOException
    {
        spacing();
        m_out.write('<'); m_out.write(tag); m_out.write(' ');
        m_out.write(name); m_out.write('=');
        m_out.write('\"'); escapeString(value); m_out.write('\"');
        m_out.write('>');    
        escapeString(content);
        m_out.write('<'); m_out.write('/'); m_out.write(tag); m_out.write('>');
        writeln();
    }
    
    /**
     * Write a new content tag with no attributes, consisting of an
     * open tag, content text, and a closing tag, all on one line.
     * @param tag the tag name
     * @param content the text content, this text will be escaped
     */
    public void contentTag(String tag, String content) throws IOException {
        spacing();
        m_out.write('<'); m_out.write(tag); m_out.write('>');
        escapeString(content);
        m_out.write('<'); m_out.write('/'); m_out.write(tag); m_out.write('>');
        writeln();
    }
    
    /**
     * Write content text.
     * @param content the content text, this text will be escaped
     */
    public void content(String content) throws IOException {
        escapeString(content);
    }
    
    /**
     * Finish the XML document.
     */
    public void finish() throws IOException {
        m_bias = 0;
        m_out.flush();
    }
    
    /**
     * Finish the XML document, writing the given footer text at the
     * end of the document.
     * @param footer the footer text, this will not be escaped
     */
    public void finish(String footer) throws IOException {
        m_bias = 0;
        m_out.write(footer);
        m_out.flush();
    }
    
    /**
     * Write the current spacing (determined by the indentation level)
     * into the document. This method is used by many of the other
     * formatting methods, and so should only need to be called in
     * the case of custom text writing outside the mechanisms
     * provided by this class.
     */
    public void spacing() throws IOException {
        int len = m_bias + m_tagStack.size() * m_tab;
        for ( int i=0; i<len; ++i )
            m_out.write(' ');
    }
    
    // ------------------------------------------------------------------------
    // Escape Text
    
    // unicode ranges and valid/invalid characters
    private static final char   LOWER_RANGE = 0x20;
    private static final char   UPPER_RANGE = 0x7f;
    private static final char[] VALID_CHARS = { 0x9, 0xA, 0xD };
    
    private static final char[] INVALID = { '<', '>', '"', '\'', '&' };
    private static final String[] VALID = 
        { "&lt;", "&gt;", "&quot;", "&apos;", "&amp;" };
    
    /**
     * Escape a string such that it is safe to use in an XML document.
     * @param str the string to escape
     */
    protected void escapeString(String str) throws IOException {
        if ( str == null ) {
            m_out.write("null");
            return;
        }
        
        int len = str.length();
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            
            if ( (c < LOWER_RANGE     && c != VALID_CHARS[0] && 
                  c != VALID_CHARS[1] && c != VALID_CHARS[2]) 
                 || (c > UPPER_RANGE) )
            {
                // character out of range, escape with character value
                m_out.write("&#");
                m_out.write(Integer.toString(c));
                m_out.write(';');
            } else {
                boolean valid = true;
                // check for invalid characters (e.g., "<", "&", etc)
                for (int j=INVALID.length-1; j >= 0; --j )
                {
                    if ( INVALID[j] == c) {
                        valid = false;
                        m_out.write(VALID[j]);
                        break;
                    }
                }
                // if character is valid, don't escape
                if (valid) {
                    m_out.write(c);
                }
            }
        }
    }
    
} // end of class XMLWriter

