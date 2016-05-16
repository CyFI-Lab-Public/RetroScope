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
/*
 * Created on Dec 9, 2003
 * 
 */
package org.eclipse.releng;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.util.Vector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author kmoir
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class CvsDiffParser extends Task {

	private String mapDiffFile;
	private Vector updatedMaps;
	
	/**
	 *  
	 */
	public CvsDiffParser() {
		super();
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {

		CvsDiffParser parser = new CvsDiffParser();
		parser.setMapDiffFile("d:/junk/cvsDiff.txt");
		parser.execute();		
	}

	public void execute() throws BuildException {
		parseMapDiffFile();
		sendNotice();
	}

	/**
	 * @return Returns the mapDiffFile.
	 */
	public String getMapDiffFile() {
		return mapDiffFile;
	}

	/**
	 * @param mapDiffFile
	 *            The mapDiffFile to set.
	 */
	public void setMapDiffFile(String mapDiffFile) {
		this.mapDiffFile = mapDiffFile;
	}

	private void parseMapDiffFile() {
		updatedMaps = new Vector();

		//read the contents of the Diff file, and return contents as a String
		if (mapDiffFile.length() == 0)
			updatedMaps=null;

		BufferedReader in = null;
		String aLine;
		
		try {
			in = new BufferedReader(new FileReader(mapDiffFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			while ((aLine = in.readLine()) != null) {
				if (aLine.startsWith("RCS file")) {
					String mapPath =
						(aLine
							.substring(aLine.indexOf(":"), aLine.indexOf(",")))
							.trim();
					
					//verification for actual changes in tags base.plugin
					while ((aLine = in.readLine()) != null && !aLine.startsWith("===")){
						if (aLine.startsWith("< plugin")||aLine.startsWith("< fragment")||aLine.startsWith("< feature")||aLine.startsWith("< base.plugin")){
							updatedMaps.add(new File(mapPath).getName());
							break;
						}
					}
		
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendNotice(){
		
		if (updatedMaps==null || updatedMaps.size()==0){
			throw new BuildException("Build cancelled - map files unchanged.");
		} 
		
		Mailer mailer = new Mailer();
		
		String subject="updated map file listing";
		String message ="these map files have been updated for the build:\n\n";
		
		for (int i=0; i<updatedMaps.size();i++){
			message=message.concat(updatedMaps.elementAt(i).toString()+"\n");
		}
		
		try {
			mailer.sendMessage(subject,message);
		} catch (NoClassDefFoundError e){
			System.out.println(message);
		}		
	}
}
