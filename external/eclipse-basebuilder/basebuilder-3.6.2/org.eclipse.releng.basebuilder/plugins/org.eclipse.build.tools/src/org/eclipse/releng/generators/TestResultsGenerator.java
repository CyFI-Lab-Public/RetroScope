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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Enumeration;

import org.apache.tools.ant.Task;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * @version 	1.0
 * @author Dean Roberts
 */
public class TestResultsGenerator extends Task {
	private static final String WARNING_SEVERITY = "WARNING";
	private static final String ERROR_SEVERITY = "ERROR";
	private static final String ForbiddenReferenceID = "ForbiddenReference";
	private static final String DiscouragedReferenceID = "DiscouragedReference";

	private static final int DEFAULT_READING_SIZE = 8192;

	static final String elementName = "testsuite";
	static final String testResultsToken = "%testresults%";
	static final String compileLogsToken = "%compilelogs%";
	static final String accessesLogsToken = "%accesseslogs%";
	public Vector dropTokens;
	public Vector platformSpecs;
	public Vector differentPlatforms;
	public String testResultsWithProblems = "\n";
	public String testResultsXmlUrls = "\n";

	private DocumentBuilder parser =null;
	public ErrorTracker anErrorTracker;
	public String testResultsTemplateString = "";
	public String dropTemplateString = "";

	public Vector platformDescription;
	public Vector platformTemplateString;
	public Vector platformDropFileName;

	//Status of tests results (pending, successful, failed), used to specify the color
	//of the test Results link on the build pages (standard, green, red), once failures
	//are encountered, this is set to failed
	protected String testResultsStatus = "successful";
	//assume tests ran.  If no html files are found, this is set to false
	private boolean testsRan = true;

	// Parameters
	// build runs JUnit automated tests
	private boolean isBuildTested;

	// buildType, I, N
	public String buildType;

	// Comma separated list of drop tokens
	public String dropTokenList;

	// Token in platform.php.template to be replaced by the desired platform ID
	public String platformIdentifierToken;

	// Location of the xml files
	public String xmlDirectoryName;

	// Location of the html files
	public String htmlDirectoryName;

	// Location of the resulting index.php file.
	public String dropDirectoryName;

	// Location and name of the template index.php file.
	public String testResultsTemplateFileName;

	// Platform specific template and output list (colon separated) in the following format:
	// <descriptor, ie. OS name>,path to template file, path to output file
	public String platformSpecificTemplateList="";

	// Location and name of the template drop index.php file.
	public String dropTemplateFileName;

	// Name of the generated index php file.
	public String testResultsHtmlFileName;

	// Name of the generated drop index php file;
	public String dropHtmlFileName;

	// Arbitrary path used in the index.php page to href the
	// generated .html files.
	public String hrefTestResultsTargetPath;

	// Aritrary path used in the index.php page to reference the compileLogs
	public String hrefCompileLogsTargetPath;

	// Location of compile logs base directory
	public String compileLogsDirectoryName;

	// Location and name of test manifest file
	public String testManifestFileName;

	//Initialize the prefix to a default string
	private String prefix = "default";
	private String testShortName = "";
	private int counter = 0;
	//The four configurations, add new configurations to test results here + update
	//testResults.php.template for changes
	private String[] testsConfig = {"linux.gtk.x86.xml",
			"linux.gtk.x86_6.0.xml",
			"macosx.cocoa.x86_5.0.xml",
			"win32.win32.x86.xml",
			"win32.win32.x86_6.0.xml"};


	public static void main(String[] args) {
		TestResultsGenerator test = new TestResultsGenerator();
		test.setDropTokenList(
			"%sdk%,%tests%,%example%,%rcpruntime%,%rcpsdk%,%deltapack%,%icubase%,%runtime%,%platformsdk%,%jdt%,%jdtsdk%,%pde%,%pdesdk%,%cvs%,%cvssdk%,%teamextras%,%swt%,%relengtools%");
		test.setPlatformIdentifierToken("%platform%");
		test.getDropTokensFromList(test.dropTokenList);
		test.setIsBuildTested(true);
		test.setXmlDirectoryName("C:\\junk\\testresults\\xml");
		test.setHtmlDirectoryName("C:\\junk\\testresults");
		test.setDropDirectoryName("C:\\junk");
		test.setTestResultsTemplateFileName(
			"C:\\junk\\templateFiles\\testResults.php.template");
		test.setPlatformSpecificTemplateList(
				"Windows,C:\\junk\\templateFiles\\platform.php.template,winPlatform.php;Linux,C:\\junk\\templateFiles\\platform.php.template,linPlatform.php;Solaris,C:\\junk\\templateFiles\\platform.php.template,solPlatform.php;AIX,C:\\junk\\templateFiles\\platform.php.template,aixPlatform.php;Macintosh,C:\\junk\\templateFiles\\platform.php.template,macPlatform.php;Source Build,C:\\junk\\templateFiles\\sourceBuilds.php.template,sourceBuilds.php");
		test.setDropTemplateFileName(
			"C:\\junk\\templateFiles\\index.php.template");
		test.setTestResultsHtmlFileName("testResults.php");
		//test.setDropHtmlFileName("index.php");
		test.setDropHtmlFileName("index.html");

		test.setHrefTestResultsTargetPath("testresults");
		test.setCompileLogsDirectoryName(
			"C:\\junk\\compilelogs\\plugins");
		test.setHrefCompileLogsTargetPath("compilelogs");
		test.setTestManifestFileName("C:\\junk\\testManifest.xml");
		test.execute();
	}

	public void execute() {

		anErrorTracker = new ErrorTracker();
		platformDescription = new Vector();
		platformTemplateString = new Vector();
		platformDropFileName = new Vector();
		anErrorTracker.loadFile(testManifestFileName);
		getDropTokensFromList(dropTokenList);
		testResultsTemplateString = readFile(testResultsTemplateFileName);
		dropTemplateString = readFile(dropTemplateFileName);

		//Specific to the platform build-page
		if(platformSpecificTemplateList!="") {
			String description, platformTemplateFile, platformDropFile;
			//Retrieve the different platforms and their info
			getDifferentPlatformsFromList(platformSpecificTemplateList);
			//Parses the platform info and retrieves the platform name,
			//template file, and drop file
			for(int i=0; i<differentPlatforms.size(); i++) {
				getPlatformSpecsFromList(differentPlatforms.get(i).toString());
				description = platformSpecs.get(0).toString();
				platformTemplateFile = platformSpecs.get(1).toString();
				platformDropFile = platformSpecs.get(2).toString();
				platformDescription.add(description);
				platformTemplateString.add(readFile(platformTemplateFile));
				platformDropFileName.add(platformDropFile);

			}

		}

		System.out.println("Begin: Generating test results index page");
		System.out.println("Parsing XML files");
		parseXml();
		System.out.println("Parsing compile logs");
		parseCompileLogs();
		System.out.println("End: Generating test results index page");
		writeTestResultsFile();
		//For the platform build-page, write platform files, in addition to the index file
		if(platformSpecificTemplateList!="") {
			writeDropFiles();
		}
		else {
			writeDropIndexFile();
		}
	}

	public void parseCompileLogs() {

		StringBuffer compilerString = new StringBuffer();
		StringBuffer accessesString = new StringBuffer();
		processCompileLogsDirectory(
			compileLogsDirectoryName,
			compilerString,
			accessesString);
		if (compilerString.length() == 0) {
			compilerString.append("None");
		}
		if (accessesString.length() == 0) {
			accessesString.append("None");
		}
		testResultsTemplateString =
			replace(testResultsTemplateString, compileLogsToken, String.valueOf(compilerString));

		testResultsTemplateString =
			replace(testResultsTemplateString, accessesLogsToken, String.valueOf(accessesString));
	}

	private void processCompileLogsDirectory(String directoryName, StringBuffer compilerLog, StringBuffer accessesLog) {
		File sourceDirectory = new File(directoryName);
		if (sourceDirectory.isFile()) {
			if (sourceDirectory.getName().endsWith(".log"))
				readCompileLog(sourceDirectory.getAbsolutePath(), compilerLog, accessesLog);
			if (sourceDirectory.getName().endsWith(".xml"))
				parseCompileLog(sourceDirectory.getAbsolutePath(), compilerLog, accessesLog);
		}
		if (sourceDirectory.isDirectory()) {
			File[] logFiles = sourceDirectory.listFiles();
			Arrays.sort(logFiles);
			for (int j = 0; j < logFiles.length; j++) {
				processCompileLogsDirectory(logFiles[j].getAbsolutePath(), compilerLog, accessesLog);
			}
		}
	}

	private void readCompileLog(String log, StringBuffer compilerLog, StringBuffer accessesLog) {
		String fileContents = readFile(log);

		int errorCount = countCompileErrors(fileContents);
		int warningCount = countCompileWarnings(fileContents);
		int forbiddenWarningCount = countForbiddenWarnings(fileContents);
		int discouragedWarningCount = countDiscouragedWarnings(fileContents);
		if (errorCount != 0) {
			//use wildcard in place of version number on directory names
			String logName =
				log.substring(getCompileLogsDirectoryName().length() + 1);
			StringBuffer stringBuffer = new StringBuffer(logName);
			stringBuffer.replace(
				logName.indexOf("_") + 1,
				logName.indexOf(File.separator, logName.indexOf("_") + 1),
				"*");
			logName = new String(stringBuffer);

			anErrorTracker.registerError(logName);
		}
		formatCompileErrorRow(log, errorCount, warningCount, compilerLog);
		formatAccessesErrorRow(log, forbiddenWarningCount, discouragedWarningCount, accessesLog);
	}

	private void parseCompileLog(String log, StringBuffer compilerLog, StringBuffer accessesLog) {
		int errorCount = 0;
		int warningCount = 0;
		int forbiddenWarningCount = 0;
		int discouragedWarningCount = 0;

		File file=new File(log);
		Document aDocument=null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			InputSource inputSource = new InputSource(reader);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			aDocument = builder.parse(inputSource);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch(IOException e) {
					// ignore
				}
			}
		}

		if (aDocument == null) return;
		// Get summary of problems
		NodeList nodeList = aDocument.getElementsByTagName("problem");
		if (nodeList == null ||nodeList.getLength()==0)
			return;

		int length = nodeList.getLength();
		for (int i = 0; i < length; i++) {
			Node problemNode = nodeList.item(i);
			NamedNodeMap aNamedNodeMap = problemNode.getAttributes();
			Node severityNode = aNamedNodeMap.getNamedItem("severity");
			Node idNode = aNamedNodeMap.getNamedItem("id");
			if (severityNode != null) {
				String severityNodeValue = severityNode.getNodeValue();
				if (WARNING_SEVERITY.equals(severityNodeValue)) {
					// this is a warning
					// need to check the id
					String nodeValue = idNode.getNodeValue();
					if (ForbiddenReferenceID.equals(nodeValue)) {
						forbiddenWarningCount++;
					} else if (DiscouragedReferenceID.equals(nodeValue)) {
						discouragedWarningCount++;
					} else {
						warningCount++;
					}
				} else if (ERROR_SEVERITY.equals(severityNodeValue)) {
					// this is an error
					errorCount++;
				}
			}
		}
		if (errorCount != 0) {
			//use wildcard in place of version number on directory names
			//System.out.println(log + "/n");
			String logName =
				log.substring(getCompileLogsDirectoryName().length() + 1);
			StringBuffer buffer = new StringBuffer(logName);
			buffer.replace(
				logName.indexOf("_") + 1,
				logName.indexOf(File.separator, logName.indexOf("_") + 1),
				"*");
			logName = new String(buffer);

			anErrorTracker.registerError(logName);
		}
		String logName = log.replaceAll(".xml", ".html");
		formatCompileErrorRow(
				logName,
				errorCount,
				warningCount,
				compilerLog);
		formatAccessesErrorRow(
				logName,
				forbiddenWarningCount,
				discouragedWarningCount,
				accessesLog);
	}

	public static byte[] getFileByteContent(String fileName) throws IOException {
		InputStream stream = null;
		try {
			File file = new File(fileName);
			stream = new FileInputStream(file);
			return getInputStreamAsByteArray(stream, (int) file.length());
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Returns the given input stream's contents as a byte array.
	 * If a length is specified (ie. if length != -1), only length bytes
	 * are returned. Otherwise all bytes in the stream are returned.
	 * Note this doesn't close the stream.
	 * @throws IOException if a problem occured reading the stream.
	 */
	public static byte[] getInputStreamAsByteArray(InputStream stream, int length)
		throws IOException {
		byte[] contents;
		if (length == -1) {
			contents = new byte[0];
			int contentsLength = 0;
			int amountRead = -1;
			do {
				int amountRequested = Math.max(stream.available(), DEFAULT_READING_SIZE);  // read at least 8K

				// resize contents if needed
				if (contentsLength + amountRequested > contents.length) {
					System.arraycopy(
						contents,
						0,
						contents = new byte[contentsLength + amountRequested],
						0,
						contentsLength);
				}

				// read as many bytes as possible
				amountRead = stream.read(contents, contentsLength, amountRequested);

				if (amountRead > 0) {
					// remember length of contents
					contentsLength += amountRead;
				}
			} while (amountRead != -1);

			// resize contents if necessary
			if (contentsLength < contents.length) {
				System.arraycopy(
					contents,
					0,
					contents = new byte[contentsLength],
					0,
					contentsLength);
			}
		} else {
			contents = new byte[length];
			int len = 0;
			int readSize = 0;
			while ((readSize != -1) && (len != length)) {
				// See PR 1FMS89U
				// We record first the read size. In this case len is the actual read size.
				len += readSize;
				readSize = stream.read(contents, len, length - len);
			}
		}

		return contents;
	}

	public String readFile(String fileName) {
		byte[] aByteArray = null;
		try {
			aByteArray = getFileByteContent(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (aByteArray == null) {
			return "";
		}
		return new String(aByteArray);
	}

	private int countCompileErrors(String aString) {
		return extractNumber(aString, "error");
	}

	private int countCompileWarnings(String aString) {
		return extractNumber(aString, "warning");
	}

	private int countForbiddenWarnings(String aString) {
		return extractNumber(aString, "Access restriction:");
	}

	private int countDiscouragedWarnings(String aString) {
		return extractNumber(aString, "Discouraged access:");
	}

	private int extractNumber(String aString, String endToken) {
		int endIndex = aString.lastIndexOf(endToken);
		if (endIndex == -1) {
			return 0;
		}

		int startIndex = endIndex;
		while (startIndex >= 0
			&& aString.charAt(startIndex) != '('
			&& aString.charAt(startIndex) != ',') {
			startIndex--;
		}

		String count = aString.substring(startIndex + 1, endIndex).trim();
		try {
			return Integer.parseInt(count);
		} catch (NumberFormatException e) {
			return 0;
		}

	}

	private int missingCount = 0;
	private String verifyAllTestsRan(String directory) {
		Enumeration enumeration = (anErrorTracker.getTestLogs()).elements();

		String replaceString="";
		while (enumeration.hasMoreElements()) {
			String testLogName = enumeration.nextElement().toString();

			if (new File(directory + File.separator + testLogName)
				.exists())
				continue;

			anErrorTracker.registerError(testLogName);
			String tmp=((platformSpecificTemplateList.equals(""))?formatRow(testLogName, -1, false):formatRowReleng(testLogName, -1, false));
			if(missingCount==0) {
				replaceString=replaceString+"</table></br>"+"\n"+
				"<table width=\"65%\" border=\"1\" bgcolor=\"#EEEEEE\" rules=\"groups\" align=\"center\">"+
				"<tr bgcolor=\"#9999CC\"> <th width=\"80%\" align=\"center\"> Missing Files </th><th  align=\"center\"> Status </th></tr>";
			}
			replaceString=replaceString+tmp;
			testResultsWithProblems=testResultsWithProblems.concat("\n" + testLogName.substring(0,testLogName.length()-4) +" (file missing)");
			missingCount++;
		}
		return replaceString;
	}

	public void parseXml() {

		File sourceDirectory = new File(xmlDirectoryName);

		if (sourceDirectory.exists()) {

			String replaceString = "";

			File[] xmlFileNames = sourceDirectory.listFiles();
			Arrays.sort(xmlFileNames)	;

			File sourceDirectoryParent = sourceDirectory.getParentFile();
			if (sourceDirectoryParent != null) {
				sourceDirectoryParent = sourceDirectoryParent.getParentFile();
			}
			String sourceDirectoryCanonicalPath = null;
			try {
				sourceDirectoryCanonicalPath = sourceDirectoryParent.getCanonicalPath();
			} catch (IOException e) {
				// ignore
			}
			for (int i = 0; i < xmlFileNames.length; i++) {
				if (xmlFileNames[i].getPath().endsWith(".xml")) {
					String fullName = xmlFileNames[i].getPath();
					int errorCount = countErrors(fullName);
					if (errorCount != 0) {
						String testName =
							xmlFileNames[i].getName().substring(
								0,
								xmlFileNames[i].getName().length() - 4);
						testResultsWithProblems =
							testResultsWithProblems.concat("\n" + testName);
						testResultsXmlUrls =
							testResultsXmlUrls.concat("\n" + extractXmlRelativeFileName(sourceDirectoryCanonicalPath, xmlFileNames[i]));
						anErrorTracker.registerError(
							fullName.substring(
								getXmlDirectoryName().length() + 1));
					}


					String tmp=((platformSpecificTemplateList.equals(""))?formatRow(xmlFileNames[i].getPath(), errorCount,true):formatRowReleng(xmlFileNames[i].getPath(), errorCount,true));
					replaceString=replaceString+tmp;


				}
			}
			//check for missing test logs
			replaceString=replaceString+verifyAllTestsRan(xmlDirectoryName);

			testResultsTemplateString =
				replace(
					testResultsTemplateString,
					testResultsToken,
					replaceString);
			testsRan = true;

		} else {
			testsRan = false;
			System.out.println(
				"Test results not found in "
					+ sourceDirectory.getAbsolutePath());
		}

	}
	private static String extractXmlRelativeFileName(String rootCanonicalPath, File xmlFile) {
		if (rootCanonicalPath != null) {
			String xmlFileCanonicalPath = null;
			try {
				xmlFileCanonicalPath = xmlFile.getCanonicalPath();		
			} catch (IOException e) {
				// ignore
			}
			if (xmlFileCanonicalPath != null) {
				// + 1 to remove the '\'
				return xmlFileCanonicalPath.substring(rootCanonicalPath.length() + 1).replace('\\', '/');
			}
		}
		return "";
	}
	private String replace(
		String source,
		String original,
		String replacement) {

		int replaceIndex = source.indexOf(original);
		if (replaceIndex > -1) {
			String resultString = source.substring(0, replaceIndex);
			resultString = resultString + replacement;
			resultString =
				resultString
					+ source.substring(replaceIndex + original.length());
			return resultString;
		} else {
			System.out.println("Could not find token: " + original);
			return source;
		}

	}

	protected void writeDropFiles() {
		writeDropIndexFile();
		//Write all the platform files
		for(int i=0; i<platformDescription.size(); i++) {
			writePlatformFile(platformDescription.get(i).toString(), platformTemplateString.get(i).toString(), platformDropFileName.get(i).toString());
		}
	}

	protected void writeDropIndexFile() {

		String[] types = anErrorTracker.getTypes();
		for (int i = 0; i < types.length; i++) {
			PlatformStatus[] platforms = anErrorTracker.getPlatforms(types[i]);
			String replaceString = processDropRows(platforms);
			dropTemplateString =
				replace(
					dropTemplateString,
					dropTokens.get(i).toString(),
					replaceString);
			}
		//Replace the token %testsStatus% with the status of the test results
		dropTemplateString = replace(dropTemplateString,"%testsStatus%",testResultsStatus);
		String outputFileName =
			dropDirectoryName + File.separator + dropHtmlFileName;
		writeFile(outputFileName, dropTemplateString);
	}

	//Writes the platform file (dropFileName) specific to "desiredPlatform"
	protected void writePlatformFile(String desiredPlatform, String templateString, String dropFileName) {

		String[] types = anErrorTracker.getTypes();
		for (int i = 0; i < types.length; i++) {
			PlatformStatus[] platforms = anErrorTracker.getPlatforms(types[i]);
			//Call processPlatformDropRows passing the platform's name
			String replaceString = processPlatformDropRows(platforms, desiredPlatform);
			templateString =
				replace(
					templateString,
					dropTokens.get(i).toString(),
					replaceString);
		}
		//Replace the platformIdentifierToken with the platform's name and the testsStatus
		//token with the status of the test results
		templateString = replace(templateString, platformIdentifierToken, desiredPlatform);
		templateString = replace(templateString,"%testsStatus%",testResultsStatus);
		String outputFileName =
			dropDirectoryName + File.separator + dropFileName;
		writeFile(outputFileName, templateString);
	}

	//Process drop rows specific to each of the platforms
	protected String processPlatformDropRows(PlatformStatus[] platforms, String name) {

		String result = "";
		boolean found = false;
		for (int i = 0; i < platforms.length; i++) {
			//If the platform description indicates the platform's name, or "All",
			//call processDropRow
			if(platforms[i].getName().startsWith(name.substring(0, 3)) || platforms[i].getName().equals("All")) {
				result = result + processDropRow(platforms[i]);
				found = true;
			}
			//If the platform description indicates "All Other Platforms", process
			//the row locally
			else if(platforms[i].getName().equals("All Other Platforms") && !found)
			{
				String imageName = "";

				if (platforms[i].hasErrors()) {
					imageName =
						"<a href=\"" + getTestResultsHtmlFileName() + "\"><img src = \"FAIL.gif\" width=19 height=23></a>";
				} else {
					if (testsRan) {
						imageName = "<img src = \"OK.gif\" width=19 height=23>";
					} else {
						if (isBuildTested) {
							imageName =
								"<font size=\"-1\" color=\"#FF0000\">pending</font>";
						} else {
							imageName = "<img src = \"OK.gif\" width=19 height=23>";
						}
					}
				}

				result = result + "<tr>";
				result = result + "<td><div align=left>" + imageName + "</div></td>\n";
				result = result + "<td>All " + name + "</td>";
				//generate http, md5 and sha1 links by calling php functions in the template
				result = result + "<td><?php genLinks($_SERVER[\"SERVER_NAME\"],\"@buildlabel@\",\"" + platforms[i].getFileName() +"\"); ?></td>\n";
				result = result + "</tr>\n";
			}
		}

		return result;
	}

	protected String processDropRows(PlatformStatus[] platforms) {

		String result = "";
		for (int i = 0; i < platforms.length; i++) {
			result = result + processDropRow(platforms[i]);
		}

		return result;
	}

	protected String processDropRow(PlatformStatus aPlatform) {

		String imageName = "";

		if (aPlatform.hasErrors()) {
			imageName =
				"<a href=\"" + getTestResultsHtmlFileName()+ "\"><img src = \"FAIL.gif\" width=19 height=23></a>";
			//Failure in tests
			testResultsStatus = "failed";
		} else {
			if (testsRan) {
				imageName = "<img src = \"OK.gif\" width=19 height=23>";
			} else {
				if (isBuildTested) {
					imageName =
						"<font size=\"-1\" color=\"#FF0000\">pending</font>";
					//Tests are pending
					testResultsStatus = "pending";
				} else {
					imageName = "<img src = \"OK.gif\" width=19 height=23>";
				}
			}
		}

		String result = "<tr>";

		result = result + "<td><div align=left>" + imageName + "</div></td>\n";
		result = result + "<td>" + aPlatform.getName() + "</td>";
		result = result + "<td>" + aPlatform.getFileName() + "</td>\n";
		result = result + "</tr>\n";

		return result;
	}

	public void writeTestResultsFile() {

		String outputFileName =
			dropDirectoryName + File.separator + testResultsHtmlFileName;
		writeFile(outputFileName, testResultsTemplateString);
	}

	private void writeFile(String outputFileName, String contents) {
		FileOutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(outputFileName);
			outputStream.write(contents.getBytes());
		} catch (FileNotFoundException e) {
			System.out.println(
				"File not found exception writing: " + outputFileName);
		} catch (IOException e) {
			System.out.println("IOException writing: " + outputFileName);
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch(IOException e) {
					// ignore
				}
			}
		}
	}

	public void setTestResultsHtmlFileName(String aString) {
		testResultsHtmlFileName = aString;
	}

	public String getTestResultsHtmlFileName() {
		return testResultsHtmlFileName;
	}

	public void setTestResultsTemplateFileName(String aString) {
		testResultsTemplateFileName = aString;
	}

	public String getTestResultsTemplateFileName() {
		return testResultsTemplateFileName;
	}

	public void setXmlDirectoryName(String aString) {
		xmlDirectoryName = aString;
	}

	public String getXmlDirectoryName() {
		return xmlDirectoryName;
	}

	public void setHtmlDirectoryName(String aString) {
		htmlDirectoryName = aString;
	}

	public String getHtmlDirectoryName() {
		return htmlDirectoryName;
	}

	public void setDropDirectoryName(String aString) {
		dropDirectoryName = aString;
	}

	public String getDropDirectoryName() {
		return dropDirectoryName;
	}

	private void formatCompileErrorRow(
		String fileName,
		int errorCount,
		int warningCount,
		StringBuffer buffer) {

		if (errorCount == 0 && warningCount == 0) {
			return;
		}

		String hrefCompileLogsTargetPath2 = getHrefCompileLogsTargetPath();
		int i = fileName.indexOf(hrefCompileLogsTargetPath2);

		String shortName =
			fileName.substring(i + hrefCompileLogsTargetPath2.length());

		buffer
			.append("<tr>\n<td>\n")
			.append("<a href=")
			.append("\"")
			.append(hrefCompileLogsTargetPath2)
			.append(shortName)
			.append("\">")
			.append(shortName)
			.append("</a>")
			.append("</td>\n")
			.append("<td align=\"center\">")
			.append("<a href=")
			.append("\"")
			.append(hrefCompileLogsTargetPath2)
			.append(shortName)
			.append("#ERRORS")
			.append("\">")
			.append(errorCount)
			.append("</a>")
			.append("</td>\n")
			.append("<td align=\"center\">")
			.append("<a href=")
			.append("\"")
			.append(hrefCompileLogsTargetPath2)
			.append(shortName)
			.append("#OTHER_WARNINGS")
			.append("\">")
			.append(warningCount)
			.append("</a>")
			.append("</td>\n")
			.append("</tr>\n");
	}

	private void formatAccessesErrorRow(
			String fileName,
			int forbiddenAccessesWarningsCount,
			int discouragedAccessesWarningsCount,
			StringBuffer buffer) {

		if (forbiddenAccessesWarningsCount == 0 && discouragedAccessesWarningsCount == 0) {
			return;
		}

		String hrefCompileLogsTargetPath2 = getHrefCompileLogsTargetPath();
		int i = fileName.indexOf(hrefCompileLogsTargetPath2);

		String shortName =
			fileName.substring(i + hrefCompileLogsTargetPath2.length());

		buffer
			.append("<tr>\n<td>\n")
			.append("<a href=")
			.append("\"")
			.append(hrefCompileLogsTargetPath2)
			.append(shortName)
			.append("\">")
			.append(shortName)
			.append("</a>")
			.append("</td>\n")
			.append("<td align=\"center\">")
			.append("<a href=")
			.append("\"")
			.append(hrefCompileLogsTargetPath2)
			.append(shortName)
			.append("#FORBIDDEN_WARNINGS")
			.append("\">")
			.append(forbiddenAccessesWarningsCount)
			.append("</a>")
			.append("</td>\n")
			.append("<td align=\"center\">")
			.append("<a href=")
			.append("\"")
			.append(hrefCompileLogsTargetPath2)
			.append(shortName)
			.append("#DISCOURAGED_WARNINGS")
			.append("\">")
			.append(discouragedAccessesWarningsCount)
			.append("</a>")
			.append("</td>\n")
			.append("</tr>\n");
		}

	private String formatRow(String fileName, int errorCount, boolean link) {

		// replace .xml with .html

		String aString = "";
		if (!link) {
			return "<tr><td>" + fileName + " (missing)" + "</td><td>" + "DNF";
		}

		if (fileName.endsWith(".xml")) {

			int begin = fileName.lastIndexOf(File.separatorChar);
			int end = fileName.lastIndexOf(".xml");

			String shortName = fileName.substring(begin + 1, end);
			String displayName = shortName;
			if (errorCount != 0)
			   aString = aString + "<tr><td><b>";
			else
				aString = aString + "<tr><td>";


			if (errorCount!=0){
				displayName="<font color=\"#ff0000\">"+displayName+"</font>";
			}
			if (errorCount==-1){
				aString=aString.concat(displayName);
			}else {
				aString=aString
					+ "<a href="
					+ "\""
					+ hrefTestResultsTargetPath
					+ "/"
					+ shortName
					+ ".html"
					+ "\">"
					+ displayName
					+ "</a>";
			}
			if (errorCount > 0)
				   aString = aString + "</td><td><b>";
			else
				aString = aString + "</td><td>";

			if (errorCount == -1)
				aString = aString + "<font color=\"#ff0000\">DNF";

			else if (errorCount >0)
				aString = aString + "<font color=\"#ff0000\">"+String.valueOf(errorCount);
			else
				aString = aString +String.valueOf(errorCount);

			if (errorCount != 0)
				aString = aString + "</font></b></td></tr>";
			else
				aString = aString + "</td></tr>";
		}

		return aString;

	}

	//Specific to the RelEng test results page
	private String formatRowReleng(String fileName, int errorCount, boolean link) {

		//If the file name doesn't end with any of the set test configurations, do nothing
		boolean endsWithConfig = false;
		int card = testsConfig.length;
		for(int i=0; i<card; i++) {
			if(fileName.endsWith(testsConfig[i]))
				endsWithConfig = true;
		}
		if(!endsWithConfig)
			return "";

		String aString = "";
		if (!link) {
			return "<tr><td>" + fileName + "</td><td align=\"center\">" + "DNF </tr>";
		}

		if (fileName.endsWith(".xml")) {

			int begin = fileName.lastIndexOf(File.separatorChar);

			//Get org.eclipse. out of the component name
			String shortName = fileName.substring(begin + 13, fileName.indexOf('_'));
			String displayName = shortName;

			//If the short name does not start with this prefix
			if(!shortName.startsWith(prefix)) {
				//If the prefix is not yet set
				if(prefix=="default"){
					//Set the testShortName variable to the current short name
					testShortName = shortName;
					counter=0;
					//Set new prefix
					prefix = shortName.substring(0, shortName.indexOf(".tests") + 6);
					aString = aString + "<tbody><tr><td><b>" + prefix + ".*" + "</b><td><td><td><td>";
					aString = aString + "<tr><td><P>" + shortName;

					//Loop until the matching string postfix(test config.) is found
					while(counter<card && !fileName.endsWith(testsConfig[counter])) {
						aString = aString + "<td align=\"center\">-</td>";
						counter++;
					}
				}
				else {
					//Set new prefix
					prefix = shortName.substring(0, shortName.indexOf(".tests") + 6);

					//Loop until the matching string postfix(test config.) is found
					while(counter<card && !fileName.endsWith(testsConfig[counter])) {
						aString = aString + "<td align=\"center\">-</td>";
						counter++;
					}

					//In this case, the new prefix should be set with the short name under it,
					//since this would mean that the team has more than one component test
					if(!shortName.endsWith("tests")) {
						aString = aString + "<tbody><tr><td><b>" + prefix + ".*" + "</b><td><td><td><td>";
						aString = aString + "<tr><td><P>" + shortName;
					}
					//The team has only one component test
					else
						aString = aString + "<tbody><tr><td><b>" + shortName;
					testShortName = shortName;

					counter = 0;
				}
			}
			//If the file's short name starts with the current prefix
			if(shortName.startsWith(prefix)) {
				//If the new file has a different short name than the current one
				if(!shortName.equals(testShortName)){
					//Fill the remaining cells with '-'. These files will later be listed as
					//missing
					while(counter<card) {
						aString = aString + "<td align=\"center\">-</td>";
						counter++;
					}
					counter = 0;
					//Print the component name
					aString = aString + "<tr><td><P>" + shortName;
					//Loop until the matching string postfix(test config.) is found
					while(counter<card && !fileName.endsWith(testsConfig[counter])) {
						aString = aString + "<td align=\"center\">-</td>";
						counter++;
					}
				}
				else {
					//Loop until the matching string postfix(test config.) is found
					while(counter<card && !fileName.endsWith(testsConfig[counter])) {
						aString = aString + "<td align=\"center\">-</td>";
						counter++;
					}
					//If the previous component has no more test files left
					if(counter==card) {
						counter = 0;
						//Print the new component name
						aString = aString + "<tr><td><P>" + shortName;
						//Loop until the matching string postfix(test config.) is found
						while(counter<card && !fileName.endsWith(testsConfig[counter])) {
							aString = aString + "<td align=\"center\">-</td>";
							counter++;
						}
					}
				}

				testShortName = shortName;

				if (errorCount != 0)
					aString = aString + "<td align=\"center\"><b>";
				else
					aString = aString + "<td align=\"center\">";

				//Print number of errors
				if (errorCount!=0){
					displayName="<font color=\"#ff0000\">"+ "(" + String.valueOf(errorCount) + ")" +"</font>";
				}
				else {
					displayName="(0)";
				}

				//Reference
				if (errorCount==-1){
					aString=aString.concat(displayName);
				}else {
					aString=aString
						+ "<a href="
						+ "\""
						+ hrefTestResultsTargetPath
						+ "/"
						+ fileName.substring(begin+1, fileName.length()-4)
						+ ".html"
						+ "\">"
						+ displayName
						+ "</a>";
				}

				if (errorCount == -1)
					aString = aString + "<font color=\"#ff0000\">DNF";

				if (errorCount != 0)
					aString = aString + "</font></b></td>";
				else
					aString = aString + "</td>";
				counter++;
			}
		}

		return aString;
	}

	private int countErrors(String fileName) {
		int errorCount = 0;

		if (new File(fileName).length()==0)
			return -1;

		try {
			DocumentBuilderFactory docBuilderFactory=DocumentBuilderFactory.newInstance();
			parser=docBuilderFactory.newDocumentBuilder();

			Document document = parser.parse(fileName);
			NodeList elements = document.getElementsByTagName(elementName);

			int elementCount = elements.getLength();
			if (elementCount == 0)
				return -1;
			for (int i = 0; i < elementCount; i++) {
				Element element = (Element) elements.item(i);
				NamedNodeMap attributes = element.getAttributes();
				Node aNode = attributes.getNamedItem("errors");
				errorCount =
					errorCount + Integer.parseInt(aNode.getNodeValue());
				aNode = attributes.getNamedItem("failures");
				errorCount =
					errorCount + Integer.parseInt(aNode.getNodeValue());

			}

		} catch (IOException e) {
			System.out.println("IOException: " + fileName);
			// e.printStackTrace();
			return 0;
		} catch (SAXException e) {
			System.out.println("SAXException: " + fileName);
			// e.printStackTrace();
			return 0;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		return errorCount;
	}



	/**
	 * Gets the hrefTestResultsTargetPath.
	 * @return Returns a String
	 */
	public String getHrefTestResultsTargetPath() {
		return hrefTestResultsTargetPath;
	}

	/**
	 * Sets the hrefTestResultsTargetPath.
	 * @param hrefTestResultsTargetPath The hrefTestResultsTargetPath to set
	 */
	public void setHrefTestResultsTargetPath(String htmlTargetPath) {
		this.hrefTestResultsTargetPath = htmlTargetPath;
	}

	/**
	 * Gets the compileLogsDirectoryName.
	 * @return Returns a String
	 */
	public String getCompileLogsDirectoryName() {
		return compileLogsDirectoryName;
	}

	/**
	 * Sets the compileLogsDirectoryName.
	 * @param compileLogsDirectoryName The compileLogsDirectoryName to set
	 */
	public void setCompileLogsDirectoryName(String compileLogsDirectoryName) {
		this.compileLogsDirectoryName = compileLogsDirectoryName;
	}

	/**
	 * Gets the hrefCompileLogsTargetPath.
	 * @return Returns a String
	 */
	public String getHrefCompileLogsTargetPath() {
		return hrefCompileLogsTargetPath;
	}

	/**
	 * Sets the hrefCompileLogsTargetPath.
	 * @param hrefCompileLogsTargetPath The hrefCompileLogsTargetPath to set
	 */
	public void setHrefCompileLogsTargetPath(String hrefCompileLogsTargetPath) {
		this.hrefCompileLogsTargetPath = hrefCompileLogsTargetPath;
	}

	/**
	 * Gets the testManifestFileName.
	 * @return Returns a String
	 */
	public String getTestManifestFileName() {
		return testManifestFileName;
	}

	/**
	 * Sets the testManifestFileName.
	 * @param testManifestFileName The testManifestFileName to set
	 */
	public void setTestManifestFileName(String testManifestFileName) {
		this.testManifestFileName = testManifestFileName;
	}

	/**
	 * Gets the dropHtmlFileName.
	 * @return Returns a String
	 */
	public String getDropHtmlFileName() {
		return dropHtmlFileName;
	}

	/**
	 * Sets the dropHtmlFileName.
	 * @param dropHtmlFileName The dropHtmlFileName to set
	 */
	public void setDropHtmlFileName(String dropHtmlFileName) {
		this.dropHtmlFileName = dropHtmlFileName;
	}

	/**
	 * Gets the dropTemplateFileName.
	 * @return Returns a String
	 */
	public String getDropTemplateFileName() {
		return dropTemplateFileName;
	}

	/**
	 * Sets the dropTemplateFileName.
	 * @param dropTemplateFileName The dropTemplateFileName to set
	 */
	public void setDropTemplateFileName(String dropTemplateFileName) {
		this.dropTemplateFileName = dropTemplateFileName;
	}

	protected void getDropTokensFromList(String list) {
		StringTokenizer tokenizer = new StringTokenizer(list, ",");
		dropTokens = new Vector();

		while (tokenizer.hasMoreTokens()) {
			dropTokens.add(tokenizer.nextToken());
		}
	}

	protected void getDifferentPlatformsFromList(String list) {
		StringTokenizer tokenizer = new StringTokenizer(list, ";");
		differentPlatforms = new Vector();

		while (tokenizer.hasMoreTokens()) {
			differentPlatforms.add(tokenizer.nextToken());
		}
	}

	protected void getPlatformSpecsFromList(String list) {
		StringTokenizer tokenizer = new StringTokenizer(list, ",");
		platformSpecs = new Vector();

		while (tokenizer.hasMoreTokens()) {
			platformSpecs.add(tokenizer.nextToken());
		}
	}

	public String getDropTokenList() {
		return dropTokenList;
	}

	public void setDropTokenList(String dropTokenList) {
		this.dropTokenList = dropTokenList;
	}

	public boolean isBuildTested() {
		return isBuildTested;
	}

	public void setIsBuildTested(boolean isBuildTested) {
		this.isBuildTested = isBuildTested;
	}


	/**
	 * @return
	 */
	public boolean testsRan() {
		return testsRan;
	}

	/**
	 * @param b
	 */
	public void setTestsRan(boolean b) {
		testsRan = b;
	}

	/**
	 * @return
	 */
	public Vector getDropTokens() {
		return dropTokens;
	}

	/**
	 * @param vector
	 */
	public void setDropTokens(Vector vector) {
		dropTokens = vector;
	}

	/**
	 * @return
	 */
	public String getTestResultsWithProblems() {
		return testResultsWithProblems;
	}

	/**
	 * @return
	 */
	public String getTestResultsXmlUrls() {
		return testResultsXmlUrls;
	}

	/**
	 * @param string
	 */
	public void setTestResultsWithProblems(String string) {
		testResultsWithProblems = string;
	}

	public String getBuildType() {
		return buildType;
	}

	public void setBuildType(String buildType) {
		this.buildType = buildType;
	}

	public String getPlatformSpecificTemplateList() {
		return platformSpecificTemplateList;
	}

	public void setPlatformSpecificTemplateList(String platformSpecificTemplateList) {
		this.platformSpecificTemplateList = platformSpecificTemplateList;
	}

	public void setPlatformIdentifierToken(String platformIdentifierToken) {
		this.platformIdentifierToken = platformIdentifierToken;
	}

	public String getPlatformIdentifierToken() {
		return platformIdentifierToken;
	}

}
