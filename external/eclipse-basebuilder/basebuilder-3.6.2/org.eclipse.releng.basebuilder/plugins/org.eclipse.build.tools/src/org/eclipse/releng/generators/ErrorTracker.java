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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.io.File;

import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @version 	1.0
 * @author
 */
public class ErrorTracker {

	// List of test logs expected at end of build
	private Vector testLogs = new Vector();


	// Platforms keyed on 
	private Hashtable platforms = new Hashtable();
	private Hashtable logFiles = new Hashtable();
	private Hashtable typesMap = new Hashtable();
	private Vector typesList = new Vector();
	
	public static void main(String[] args) {
		
		// For testing only.  Should not be invoked
		
		ErrorTracker anInstance = new ErrorTracker();
		anInstance.loadFile("C:\\junk\\testManifest.xml");
		String[] theTypes = anInstance.getTypes();
		for (int i=0; i < theTypes.length; i++) {
			// System.out.println("Type: " + theTypes[i]);
			PlatformStatus[] thePlatforms = anInstance.getPlatforms(theTypes[i]);
			for (int j=0; j < thePlatforms.length; j++) {
				// System.out.println("Out ID: " + thePlatforms[j].getId());
			}
		}
	}
	
	public void loadFile(String fileName) {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser=null;
		try {
			parser = docBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
		try {
			
			Document document = parser.parse(fileName);
			NodeList elements = document.getElementsByTagName("platform");
			int elementCount = elements.getLength();
			for (int i = 0; i < elementCount; i++) {
				PlatformStatus aPlatform = new PlatformStatus((Element) elements.item(i));
				//System.out.println("ID: " + aPlatform.getId());
				platforms.put(aPlatform.getId(), aPlatform);
				
				Node zipType = elements.item(i).getParentNode();
				String zipTypeName = (String) zipType.getAttributes().getNamedItem("name").getNodeValue();
				
				Vector aVector = (Vector) typesMap.get(zipTypeName);
				if (aVector == null) {
					typesList.add(zipTypeName);
					aVector = new Vector();
					typesMap.put(zipTypeName, aVector);
				}
				aVector.add(aPlatform.getId());
				
			}

			NodeList effectedFiles = document.getElementsByTagName("effectedFile");
			int effectedFilesCount = effectedFiles.getLength();
			for (int i = 0; i < effectedFilesCount; i++) {
				Node anEffectedFile = effectedFiles.item(i);
				Node logFile = anEffectedFile.getParentNode();
				String logFileName = (String) logFile.getAttributes().getNamedItem("name").getNodeValue();
				logFileName=convertPathDelimiters(logFileName);
				String effectedFileID = (String) anEffectedFile.getAttributes().getNamedItem("id").getNodeValue();				
				//System.out.println(logFileName);
				Vector aVector = (Vector) logFiles.get(logFileName);
				if (aVector == null) {
					aVector = new Vector();
					logFiles.put(logFileName, aVector);
					
				}
				PlatformStatus ps=(PlatformStatus) platforms.get(effectedFileID);
				if (ps!=null)
					aVector.addElement(ps);
			}
			
			// store a list of the test logs expected after testing
			NodeList testLogList = document.getElementsByTagName("logFile");
				int testLogCount = testLogList.getLength();
				for (int i = 0; i < testLogCount; i++) {
								
					Node testLog = testLogList.item(i);
					String testLogName = (String) testLog.getAttributes().getNamedItem("name").getNodeValue();
					Node typeNode=testLog.getAttributes().getNamedItem("type");
					String type="test";
					if (typeNode!=null){
						type = typeNode.getNodeValue();
					}
					if (testLogName.endsWith(".xml")&&type.equals("test")){
						testLogs.add(testLogName);
						//System.out.println(testLogName);
					}
			
			}


//			// Test this mess.
//			Object[] results = platforms.values().toArray();
//			for (int i=0; i < results.length; i++) {
//				PlatformStatus ap = (PlatformStatus) results[i];
//				System.out.println("ID: " + ap.getId() + " passed: " + ap.getPassed());
//			}
//		
//			Enumeration anEnumeration = logFiles.keys();
//			while (anEnumeration.hasMoreElements()) {
//				String aKey = (String) anEnumeration.nextElement();
//				System.out.println("Whack a key: " + aKey);
//				((PlatformStatus) logFiles.get(aKey)).setPassed(false);
//			}
//			
//			results = platforms.values().toArray();
//			for (int i=0; i < results.length; i++) {
//				PlatformStatus ap = (PlatformStatus) results[i];
//				System.out.println("ID: " + ap.getId() + " passed: " + ap.getPassed());
//			}
			
			
		
			
			
		} catch (IOException e) {
			System.out.println("IOException: " + fileName);
			// e.printStackTrace();
			
		} catch (SAXException e) {
			System.out.println("SAXException: " + fileName);
			e.printStackTrace();
			
		}
	}
	
	public void registerError(String fileName) {
		// System.out.println("Found an error in: " + fileName);
		if (logFiles.containsKey(fileName)) {
			Vector aVector = (Vector) logFiles.get(fileName);
			for (int i = 0; i < aVector.size(); i++) {
				((PlatformStatus) aVector.elementAt(i)).registerError();
			}
		} else {
			
			// If a log file is not specified explicitly it effects
			// all "platforms" except JDT
			
			Enumeration values = platforms.elements();
			while (values.hasMoreElements()) {
				PlatformStatus aValue = (PlatformStatus) values.nextElement();
				if (!aValue.getId().equals("JA") && 
					!aValue.getId().equals("EW") && 
					!aValue.getId().equals("EA")) {
						aValue.registerError();
				}
			}
		}
	}
	
	public boolean hasErrors(String id) {
		return ((PlatformStatus) platforms.get(id)).hasErrors();
	}
	
	// Answer a string array of the zip type names in the order they appear in
	// the .xml file.
	public String[] getTypes() {
		return (String[]) typesList.toArray(new String[typesList.size()]);
	}
	
	// Answer an array of PlatformStatus objects for a given type.

	public PlatformStatus[] getPlatforms(String type) {
		Vector platformIDs = (Vector) typesMap.get(type);
		PlatformStatus[] result = new PlatformStatus[platformIDs.size()];
		for (int i = 0; i < platformIDs.size(); i++) {
			result[i] = (PlatformStatus) platforms.get((String) platformIDs.elementAt(i));
		}
		return  result;
	}	

	/**
	 * Returns the testLogs.
	 * @return Vector
	 */
	public Vector getTestLogs() {
		return testLogs;
	}

	private String convertPathDelimiters(String path){
		return new File(path).getPath();
	}
	
}
