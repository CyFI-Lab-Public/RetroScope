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
package org.eclipse.releng.generators;
/**
 * This class finds the version of a plug-in, or fragment listed in a feature
 * and writes <element>=<element>_<version> for each in a properties file.
 * The file produced from this task can be loaded by an Ant script to find files in the
 * binary versions of plugins and fragments.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.Task;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class TestVersionTracker extends Task{

	private Hashtable elements;
	
	//fields to hold temporary values for parsing
	private SAXParser parser;
	
	//the feature to from which to collect version information
	private String featureId;
	
	//buildDirectory
	private String buildDirectory;
	
	//the path to the file in which to write the results
	private String outputFile;
		
	public static void main(String[] args) {
		TestVersionTracker tracker =
		new TestVersionTracker();
		tracker.buildDirectory="D:/src";
		tracker.featureId="org.eclipse.sdk.tests";
		tracker.outputFile="d:/eclipse-testing/test.properties";
		tracker.execute();
		
	}

	public TestVersionTracker(){
		super();
	}
	

	public void execute() {
		elements = new Hashtable();
				
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		try {
			parser = saxParserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
		  	e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
        
		parse(buildDirectory+"/features/"+featureId+"/build.xml",new FeatureHandler());
		getTestPluginProperties();
		writeProperties(outputFile, true);
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

    private void getTestPluginProperties(){
    	//set prerequisites list for each test plugin
    	Enumeration keys=elements.keys();
    	while (keys.hasMoreElements()){
    		String id=keys.nextElement().toString();
    		TestPlugin testPlugin=(TestPlugin)elements.get(id);
    		testPlugin.getPrerequisitePlugins(id);
    		testPlugin.setHasPerformanceTarget();
    	}
    }
    
    private class FeatureHandler extends DefaultHandler{
    	//  Start Element Event Handler
    	public void startElement(
		 String uri,
		 String local,
			String qName,
			Attributes atts) {

    		if (qName.equals("eclipse.idReplacer")) {
    			try{
    				String pluginIds = atts.getValue("pluginIds");
    				
    				//get pluginIDs and versions from generated build.xml.  Create TestPlugin objects
    				StringTokenizer tokenizer= new StringTokenizer(pluginIds,",");
    				while (tokenizer.hasMoreTokens()){
    					String idtmp=tokenizer.nextToken();
    					String id=idtmp.substring(0, idtmp.indexOf(":"));
    					String version=tokenizer.nextToken();
    					TestPlugin testPlugin=new TestPlugin(id,version);
    					elements.put(id,testPlugin);	
    				} 								
    			} catch (Exception e){
    				e.printStackTrace();
       			}
 			} 
    	}
    }
 
    private class TestPlugin {
    	String id;
    	String version;
    	boolean hasPerformanceTarget=false;
    	ArrayList prerequisitePlugins=new ArrayList();
    	File testXml;
    	
    	TestPlugin(String id, String version){
    		this.id=id;
    		this.version=version;
    		this.testXml=new File(buildDirectory,"plugins/"+id+"/test.xml");
    	}
    	
    	private void getPrerequisitePlugins(String id) {
			Headers headers = null;
			String value = null;
			File manifest = new File(buildDirectory, "plugins/" + id + "/META-INF/MANIFEST.MF");
			ManifestElement[] manifestElements=null;
			if (manifest.exists()) {
				try {
					headers = Headers.parseManifest(new FileInputStream(manifest));
					if (headers.get(Constants.REQUIRE_BUNDLE)==null)
						return;
					value = headers.get(Constants.REQUIRE_BUNDLE).toString();
					manifestElements = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, value);

				} catch (BundleException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

				for (int i = 0; i < manifestElements.length; i++) {
					String name = manifestElements[i].getValue();
					if (elements.containsKey(name)){
						if (!prerequisitePlugins.contains(name)){
							boolean prereqAdded=prerequisitePlugins.add(name);
							if (prereqAdded){
								getPrerequisitePlugins(name);
							}
						}
					}
				}
			}
			getPrerequisitePluginsFromPluginXml(id);
		}
    	
    	 /**
		 * Returns the required list of plug-ins from plugin.xml
		 */
		private void getPrerequisitePluginsFromPluginXml(String id) {
			File pluginXml = new File(buildDirectory, "/plugins/" + id + "/plugin.xml");
			if (!pluginXml.exists())
				return;

			InputStream is = null;
			Document doc = null;
			try {
				is = new BufferedInputStream(new FileInputStream(pluginXml));
				InputSource inputSource = new InputSource(is);

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

				DocumentBuilder builder = null;

				try {
					builder = factory.newDocumentBuilder();
				} catch (ParserConfigurationException e1) {
					e1.printStackTrace();
				}

				try {
					doc = builder.parse(inputSource);
				} catch (SAXParseException e) {
					e.printStackTrace();
				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
			// Find the fragment's plugins's name, id and version
			NodeList nodeList = doc.getElementsByTagName("import");
			if (nodeList == null || nodeList.getLength() == 0) {
				return;
			}
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				NamedNodeMap map = node.getAttributes();
				Node namedItem = map.getNamedItem("plugin");
				String name = namedItem.getNodeValue();
				if (namedItem == null) {
					continue;
				} else {
					if (elements.containsKey(name)) {
						if (!prerequisitePlugins.contains(name)) {
							boolean prereqAdded = prerequisitePlugins.add(name);
							if (prereqAdded) {
								getPrerequisitePlugins(name);
							}
						}
					}
				}
			}
		}
    	
   	 /**
		 * Returns the required list of plug-ins from plugin.xml
		 */
		private void setHasPerformanceTarget() {
			File testXml = new File(buildDirectory, "/plugins/" + id + "/test.xml");
			if (!testXml.exists())
				return;

			InputStream is = null;
			Document doc = null;
			try {
				is = new BufferedInputStream(new FileInputStream(testXml));
				InputSource inputSource = new InputSource(is);

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

				DocumentBuilder builder = null;

				try {
					builder = factory.newDocumentBuilder();
				} catch (ParserConfigurationException e1) {
					e1.printStackTrace();
				}

				try {
					doc = builder.parse(inputSource);
				} catch (SAXParseException e) {
					e.printStackTrace();
				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
			// Find a target named "performance"
			NodeList nodeList = doc.getElementsByTagName("target");
			if (nodeList == null || nodeList.getLength() == 0) {
				return;
			}
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				NamedNodeMap map = node.getAttributes();
				Node namedItem = map.getNamedItem("name");
				String name = namedItem.getNodeValue();
				if (namedItem == null) {
					continue;
				} else {
					if (name.equals("performance")){
						hasPerformanceTarget=true;
						return;
					}
				}
			}
		}
    	
    	public String toString(){
    		String keyPrefix=id+"_"+version;
    		String performanceProperty=hasPerformanceTarget?id+".has.performance.target="+hasPerformanceTarget+"\n":"";
    		return id+"="+keyPrefix+"\n"+
    			performanceProperty+
    			id+".prerequisite.testplugins="+getPrerequisiteList()+"\n";
    	}
    	
    	private String getPrerequisiteList(){
    		String prerequisites="";
    		for (int i=0;i<prerequisitePlugins.size();i++){
    			prerequisites=prerequisites.concat("**/${"+prerequisitePlugins.get(i)+"}** ");
    		}
    		return prerequisites;
    	}
    }
      
	public void writeProperties(String propertiesFile,boolean append){
		
		try{
			
		PrintWriter writer = new PrintWriter(new FileWriter(propertiesFile,append));
				
			Object[] keys = elements.keySet().toArray();
			Arrays.sort(keys);
			for (int i=0;i<keys.length;i++){
				Object key = keys[i];
				writer.println(((TestPlugin)elements.get(key)).toString());
				writer.flush();
			}
			writer.close();
		
		} catch (IOException e){
			System.out.println("Unable to write to file "+propertiesFile);
		}
		
		
	}

	
	/**
	 * @return Returns the outputFilePath.
	 */
	public String getOutputFilePath() {
		return outputFile;
	}

	/**
	 * @param outputFilePath The outputFilePath to set.
	 */
	public void setOutputFilePath(String outputFilePath) {
		this.outputFile = outputFilePath;
	}

	public String getBuildDirectory() {
		return buildDirectory;
	}

	public void setBuildDirectory(String buildDirectory) {
		this.buildDirectory = buildDirectory;
	}

	public String getFeatureId() {
		return featureId;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}




}
