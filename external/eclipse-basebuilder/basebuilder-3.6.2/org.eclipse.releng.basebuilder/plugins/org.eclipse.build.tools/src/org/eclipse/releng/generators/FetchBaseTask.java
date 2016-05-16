/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.generators;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.tools.ant.Task;

public class FetchBaseTask extends Task {

	private String mapFile;
	private String outputFile;
	private Hashtable entries;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		FetchBaseTask task=new FetchBaseTask();
		task.mapFile="d:/workspace/org.eclipse.releng/maps/base.map";
		task.outputFile="d:/workspace/org.eclipse.releng/fetch.xml";
		task.execute();
	}

	public FetchBaseTask(){
		entries=new Hashtable();
	}
	public void execute(){
		readMap();
		printScript(fetchScript());
	}

	private void readMap (){
		File file=new File(mapFile);
		Properties properties=new Properties();
		try {
			properties.load(new FileInputStream(file));
			Enumeration keys=properties.keys();
			while (keys.hasMoreElements()){
				String key=keys.nextElement().toString();
				String script=getScript(key,properties.get(key).toString());
				entries.put("fetch."+key,script);
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String getScript(String key,String entry){
		String[] keyParts=key.split("@");
		if (keyParts.length==0)
			return null;
		String [] cvsinfo=entry.split(",");
		if (cvsinfo.length<3)
			return null;

		String[] typeParts=keyParts[0].split("base.");
		String type=typeParts[1];
		String id=keyParts[1];
		
		String fullName=entry.substring(entry.lastIndexOf("/")+1,entry.length());
		if (fullName.endsWith(".jar"))
			return fetchJarTarget(type, id, cvsinfo);
		else
			return fetchDirectoryTarget(type, id, fullName, cvsinfo);	
	}
	
	public String getMapFile() {
		return mapFile;
	}

	public void setMapFile(String mapFile) {
		this.mapFile = mapFile;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}
	
	private String fetchJarTarget(String type, String id, String[] cvsinfo){
		return "\t<target name=\"fetch.base."+type+"@"+id+"\">\n" +
		"\t\t<mkdir dir=\"${baseLocation}/"+type+"s\" />\n" +
		"\t\t<property name=\"cvsroot\" value=\""+cvsinfo[1]+"\" />\n" +
		"\t\t<property name=\"fetchTag\" value=\""+cvsinfo[0]+"\" />\n" +
		"\t\t<cvs command=\"export -d "+type+"s\"\n" +
		"\t\t\tcvsRoot=\"${cvsroot}\"\n" +
		"\t\t\tpackage=\""+cvsinfo[3]+"\"\n" +
		"\t\t\ttag=\"${fetchTag}\"\n" +
		"\t\t\tdest=\"${baseLocation}\"\n" +
		"\t\t\tquiet=\"true\"/>\n" +
		"\t\t<delete includeemptydirs=\"true\">\n"+
		"\t\t\t<fileset dir=\"${baseLocation}/"+type+"s\" includes=\"**/CVS/**\" defaultexcludes=\"no\"/>\n"+
		"\t\t</delete>\n"+
		"\t</target>\n";
	
	}
	
	private String fetchDirectoryTarget(String type, String id, String fullName,String[] cvsinfo){
		return "\t<target name=\"fetch.base."+type+"@"+id+"\">\n" +
		"\t\t<mkdir dir=\"${baseLocation}/"+type+"s\" />\n" +
		"\t\t<property name=\"cvsroot\" value=\""+cvsinfo[1]+"\" />\n" +
		"\t\t<cvs command=\"export -d "+fullName+"\"\n" +
		"\t\t\tcvsRoot=\"${cvsroot}\"\n" +
		"\t\t\tpackage=\""+cvsinfo[3]+"\"\n" +
		"\t\t\ttag=\""+cvsinfo[0]+"\"\n" +
		"\t\t\tdest=\"${baseLocation}/"+type+"s\"\n" +
		"\t\t\tquiet=\"true\"/>\n" +
		"\t\t<delete includeemptydirs=\"true\">\n"+
		"\t\t\t<fileset dir=\"${baseLocation}/"+type+"s\" includes=\"**/CVS/**\" defaultexcludes=\"no\"/>\n"+
		"\t\t</delete>\n"+
		"\t</target>\n";
	
	}
	
	private String fetchScript(){
		String script="<project default=\"all.elements\">\n" +
			"<!--Ant script which will fetch pre-built plug-ins and features to a location where they\n" +
			"will be consumed by the build, i.e. ${baseLocation}.  Stored in this project to capture revisions/urls of\n" +
			"binaries.-->\n" +
			"\t<property name=\"baseLocation\" value=\"${basedir}/baseLocation\" />\n" +
			"\t<target name=\"all.elements\">\n";
		
		Enumeration keys=entries.keys();
		while (keys.hasMoreElements()){
			script=script.concat("\t\t<antcall target=\""+keys.nextElement()+"\" />\n");
		}
		script=script.concat("\t</target>");
		keys=entries.keys();
		while (keys.hasMoreElements()){
			script=script.concat("\n\n"+entries.get(keys.nextElement()));
		}
		script=script.concat("</project>");

		return script;	
	}
	
	private void printScript(String script){
		try {
			PrintWriter out = new PrintWriter(new FileWriter(new File(outputFile)));
			out.print(script);
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
