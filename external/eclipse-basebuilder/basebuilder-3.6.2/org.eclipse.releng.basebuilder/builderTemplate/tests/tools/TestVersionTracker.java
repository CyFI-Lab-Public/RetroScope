/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * This class finds the version of a feature, plugin, or fragment in a given
 * build source tree.
 */

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.SAXException;
import java.io.*;
import java.util.Hashtable;
import java.util.Enumeration;

public class TestVersionTracker extends DefaultHandler {

	private String installDirectory;
	private Hashtable elements;
	private SAXParser parser;
	private String xmlFile;
	
	//test
	public static void main(String[] args) {
		TestVersionTracker Tracker =
			new TestVersionTracker(args[1]);
		Tracker.parse(args[0]);
		Tracker.writeProperties(args[2], true);
	}

	public TestVersionTracker(String install, Hashtable elements) {
		//  Create a Xerces SAX Parser
		parser = new SAXParser();
        
		//  Set Content Handler
		parser.setContentHandler (this);
		
		// directory containing the source for a given build
		installDirectory = install;

		//  instantiate hashtable that will hold directory names with versions for elements
		this.elements = elements;
	}
	
	public TestVersionTracker(String install) {
		this(install, new Hashtable());
	}

	public void parse(String xmlFile){
		this.xmlFile = xmlFile;	
		//  Parse the Document      
		try {
			parser.parse(this.xmlFile);
		} catch (SAXException e) {
			System.err.println (e);
		} catch (IOException e) {
			System.err.println (e);
          
		}
	}

	//  Start Element Event Handler
	public void startElement(
		String uri,
		String local,
		String qName,
		Attributes atts) {

		String element = atts.getValue("id");
		String version = atts.getValue("version");

		if (local.equals("plugin") || local.equals("fragment")) {
				elements.put(element,element+"_"+version);
		} else if (local.equals("feature"))
				elements.put(element+"-feature",element+"_"+version);
		else if (local.equals("includes")) {
			File thisFile = new File(xmlFile);
			String includeFile = thisFile.getParentFile().getParent() + '/' + element+"_"+version + "/feature.xml";
			TestVersionTracker recurseTracker = new TestVersionTracker(installDirectory, elements);
			recurseTracker.parse(includeFile);
		}
	}

	public void writeProperties(String propertiesFile,boolean append){
		try{
			
		PrintWriter writer = new PrintWriter(new FileWriter(propertiesFile,append));
				
			Enumeration keys = elements.keys();

			while (keys.hasMoreElements()){
				Object key = keys.nextElement();
				writer.println(key.toString()+"="+elements.get(key).toString());
				writer.flush();
			}
			writer.close();
		
		} catch (IOException e){
			System.out.println("Unable to write to file "+propertiesFile);
		}
		
		
	}

}
