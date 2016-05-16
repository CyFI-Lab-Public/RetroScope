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
package org.eclipse.releng;

/**
 * A custom Ant task that finds compile logs containing compile
 * errors.  The compile logs with errors are sent as email attachments using
 * information in monitor.properties.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CompileErrorCheck extends Task {

	private static final class CompilerErrorCheckerHandler extends DefaultHandler {
		boolean hasErrors = false;
		
		public void startElement(String uri, String localName,
				String name, Attributes attributes) throws SAXException {
			if (this.hasErrors) return;
			if ("problem_summary".equals(name)) {
				// problem_summary name
				String value = attributes.getValue("errors");
				this.hasErrors = value != null && !value.equals("0");
			}
		}
		public boolean hasErrors() {
			return this.hasErrors;
		}
	}

	//directory containing of build source, parent of features and plugins
	private String install = "";

	//keep track of compile logs containing errors
	private Vector logsWithErrors;
	
	// keep track of the factory to use
	private SAXParser parser;
	
	public CompileErrorCheck() {
		this.logsWithErrors = new Vector();
		SAXParserFactory factory = SAXParserFactory.newInstance();
		this.parser = null;

		try {
			this.parser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	public void execute() throws BuildException {
		if (parser == null) return;
		findLogs(new File(install));
		sendNotice();
	}

	// test
	public static void main(String[] args) {
		CompileErrorCheck checker = new CompileErrorCheck();
		checker.install="d:/compilelogs";
		checker.execute();
	}

	private void findLogs(File aFile) {
		if (!aFile.exists()) return;
		// basis case
		if (aFile.isFile()) {
			String absolutePath = aFile.getAbsolutePath();
			if (absolutePath.endsWith(".xml")) {
				parse(aFile);
			} else if (absolutePath.endsWith(".jar.bin.log")||absolutePath.endsWith("dot.bin.log")){
				read(aFile);
			}
		} else {
			//recurse into directories looking for and reading compile logs
			File files[] = aFile.listFiles();

			if (files != null) {
				for (int i = 0, max = files.length; i < max; i++) {
					findLogs(files[i]);
				}
			}
		}
	}

	private void read(File file) {
		//read the contents of the log file, and return contents as a String
		if (file.length()==0)
			return;
		
		BufferedReader in = null;
		String aLine;

		try {
			in = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			while ((aLine = in.readLine()) != null) {
				int statusSummaryIndex=aLine.indexOf("problem (");
				if (statusSummaryIndex==-1)
					statusSummaryIndex=aLine.indexOf("problems (");
				
				if (statusSummaryIndex!=-1&&(aLine.indexOf("error", statusSummaryIndex) != -1)){
					logsWithErrors.add(file);
					System.out.println(file.getName()+" has compile errors.");
					return;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// make sure we don't leave any file handle open
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	private void parse(File file) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		InputSource inputSource = new InputSource(reader);

		CompilerErrorCheckerHandler compilerErrorCheckerHandler = new CompilerErrorCheckerHandler();
		try {
			parser.parse(inputSource, compilerErrorCheckerHandler);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// make sure we don't leave any file handle open
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		
		if (compilerErrorCheckerHandler.hasErrors()) {
			logsWithErrors.add(new File(file.getParentFile(),file.getName().replaceAll(".xml", ".html")));
			System.out.println(file.getName()+" has compile errors.");
		}
	}
	
	private void sendNotice() {
		//send email notification that there are compile errors in the build
		//send the logs as attachments
		Enumeration enumeration = logsWithErrors.elements();

		if (logsWithErrors.size() > 0) {
			try{

				Mailer mailer = new Mailer();
				String [] logFiles = new String [logsWithErrors.size()];

				int i=0;

				while (enumeration.hasMoreElements()) {
					logFiles[i++]=((File) enumeration.nextElement()).getAbsolutePath();
				}

				mailer.sendMultiPartMessage("Compile errors in build", "Compile errors in build.  See attached compile logs.", logFiles);
			} catch (NoClassDefFoundError e){
				while (enumeration.hasMoreElements()) {
					String path=((File) enumeration.nextElement()).getAbsolutePath();
					String nameWithPlugin=path.substring(path.indexOf("plugins"),path.length());
					System.out.println("Compile errors detected in "+nameWithPlugin);
				}

				System.out.println("Unable to send email notice of compile errors.");
				System.out.println("The j2ee.jar may not be on the Ant classpath.");

			}

		}

	}

	/**
	 * Gets the install.
	 * @return Returns a String
	 */
	public String getInstall() {
		return install;
	}

	/**
	 * Sets the install.
	 * @param install The install to set
	 */
	public void setInstall(String install) {
		this.install = install;
	}

}
