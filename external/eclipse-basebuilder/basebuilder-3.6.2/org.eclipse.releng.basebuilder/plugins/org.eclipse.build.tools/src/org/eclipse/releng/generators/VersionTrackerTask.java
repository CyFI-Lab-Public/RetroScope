/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * This class finds the version of a plug-in, or fragment listed in a feature
 * and writes <element>=<element>_<version> for each in a properties file.
 * The file produced from this task can be loaded by an Ant script to find files in the
 * binary versions of plugins and fragments.
 */
package org.eclipse.releng.generators;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import java.io.*;
import java.util.Hashtable;
import java.util.Enumeration;
import org.apache.tools.ant.Task;
import java.util.Vector;

public class VersionTrackerTask extends Task {

	private String buildDirectory;
	private Hashtable elements;
	private SAXParser parser;
	private Vector allElements;
	
	//the feature to from which to collect version information
	private String featurePath;
	
	//the path to the file in which to write the results
	private String outputFilePath;
	
	public void execute(){
		VersionTrackerTask tracker =
			new VersionTrackerTask(getBuildDirectory());
		tracker.parse(getFeaturePath(),new FeatureHandler());
		tracker.parse(new PluginHandler());
		tracker.writeProperties(getOutputFilePath(), true);
	}
	
	//test
	public static void main(String[] args) {
		VersionTrackerTask Tracker =
			new VersionTrackerTask(args[1]);
		Tracker.parse(args[0],Tracker.new FeatureHandler());
		Tracker.parse(Tracker.new PluginHandler());
			Tracker.writeProperties(args[2], true);
	}

	public VersionTrackerTask(){
	}
	
	public VersionTrackerTask(String install) {
		elements = new Hashtable();
		allElements=new Vector();
		
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		try {
			parser = saxParserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
		  	e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
        
       	// directory containing the source for a given build
		buildDirectory = install;
	}

	private void parse (DefaultHandler handler){
		for (int i=0; i<allElements.size();i++){
			parse(allElements.elementAt(i).toString(), handler);
		}
	}
	
    public void parse(String xmlFile,DefaultHandler handler){
         try {
          parser.parse(xmlFile,handler);
        } catch (SAXException e) {
            System.err.println (e);
        } catch (IOException e) {
            System.err.println (e);    
        } 
    }

    private class FeatureHandler extends DefaultHandler{
    	//  Start Element Event Handler
    	public void startElement(
		 String uri,
		 String local,
			String qName,
			Attributes atts) {

    		String element = atts.getValue("id");
    		//need to parse the plugin.xml or fragment.xml for the correct version value since the 3.0 features may list these as "0.0.0"
    		if (qName.equals("plugin")) {
    			try{
     			allElements.add(getBuildDirectory()+File.separator+"plugins"+File.separator+element+File.separator+"plugin.xml");
    			} catch (Exception e){
    				e.printStackTrace();
    		
    			}
 			} else if (qName.equals("fragment")){
				allElements.add(getBuildDirectory()+File.separator+"plugins"+File.separator+element+File.separator+"fragment.xml");
			}
    	}
    }
 
    private class PluginHandler extends DefaultHandler{
    	//  Start Element Event Handler
    	public void startElement(
								 String uri,
								 String local,
								 String qName,
								 Attributes atts) {

    		
    		String element = atts.getValue("id");
    		String version = atts.getValue("version");
    		System.out.println("Examining "+element);
    		
    		if (qName.equals("plugin") || qName.equals("fragment")){
    			System.out.println("Found plugin "+element);
    			elements.put(element,element+"_"+version);
    		}
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

	/**
	 * @return Returns the featurePath.
	 */
	public String getFeaturePath() {
		return featurePath;
	}

	/**
	 * @param featurePath The featurePath to set.
	 */
	public void setFeaturePath(String featurePath) {
		this.featurePath = featurePath;
	}

	/**
	 * @return Returns the installDirectory.
	 */
	public String getBuildDirectory() {
		return buildDirectory;
	}

	/**
	 * @param installDirectory The installDirectory to set.
	 */
	public void setBuildDirectory(String buildDirectory) {
		this.buildDirectory = buildDirectory;
	}

	/**
	 * @return Returns the outputFilePath.
	 */
	public String getOutputFilePath() {
		return outputFilePath;
	}

	/**
	 * @param outputFilePath The outputFilePath to set.
	 */
	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}

}
