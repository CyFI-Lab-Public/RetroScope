/*
 * Created on Apr 8, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.eclipse.releng.generators;
import java.io.File;



/**
 * @author SDimitrov
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class EclipseTestResultsGeneratorNoMail extends TestResultsGenerator {
 
	
	// buildType used to determine if mail should be sent on
	// successful build completion
	private String buildType;
	private boolean sendMail=true;
	/**
	 * @return
	 */
	public String getBuildType() {
		return buildType;
	}

	/**
	 * @param buildType
	 */
	public void setBuildType(String buildType) {
		this.buildType = buildType;
	}

	public static void main(String[] args) {
		String publishingContent="C:\\Documents and Settings\\IBMEmployee\\workspace\\org.eclipse.releng.eclipsebuilder\\eclipse\\publishingFiles";

		EclipseTestResultsGeneratorNoMail test = new EclipseTestResultsGeneratorNoMail();
		test.buildType="N";
		test.setIsBuildTested(true);
		test.setDropTokenList(
		"%sdk%,%tests%,%example%,%rcpruntime%,%rcpsdk%,%deltapack%,%icubase%,%runtime%,%platformsdk%,%jdt%,%jdtsdk%,%jdtc%,%jarprocessor%,%pde%,%pdesdk%,%cvs%,%cvssdk%,%teamextras%,%swt%,%relengtools%");
		test.getDropTokensFromList(test.getDropTokenList());
		test.setXmlDirectoryName("C:\\junk\\testresults\\xml");
		test.setHtmlDirectoryName("C:\\junk\\testresults\\html");
		test.setDropDirectoryName("C:\\junk");
		test.setTestResultsTemplateFileName(
				publishingContent+"\\templateFiles\\testResults.php.template");
		test.setDropTemplateFileName(
				publishingContent+"\\templateFiles\\index.php.template");
		test.setTestResultsHtmlFileName("testResults.php");
		test.setDropHtmlFileName("index.php");
		//test.setDropHtmlFileName("index.html");
		test.setPlatformIdentifierToken("%platform%");
	
		test.setPlatformSpecificTemplateList("Windows,"+publishingContent+"/templateFiles/platform.php.template,winPlatform.php;Linux,"+publishingContent+"/templateFiles/platform.php.template,linPlatform.php;Solaris,"+publishingContent+"/templateFiles/platform.php.template,solPlatform.php;AIX,"+publishingContent+"/templateFiles/platform.php.template,aixPlatform.php;Macintosh,"+publishingContent+"/templateFiles/platform.php.template,macPlatform.php;Source Build,"+publishingContent+"/templateFiles/sourceBuilds.php.template,sourceBuilds.php");
		/*<property name="platformIdentifierToken" value="%platform%" />
			<property name="platformSpecificTemplateList" value="Windows,${publishingContent}/templateFiles/platform.php.template,winPlatform.php;Linux,${publishingContent}/templateFiles/platform.php.template,linPlatform.php;Solaris,${publishingContent}/templateFiles/platform.php.template,solPlatform.php;AIX,${publishingContent}/templateFiles/platform.php.template,aixPlatform.php;Macintosh,${publishingContent}/templateFiles/platform.php.template,macPlatform.php;Source Build,${publishingContent}/templateFiles/sourceBuilds.php.template,sourceBuilds.php" />
			*/
		
		test.setHrefTestResultsTargetPath("testresults");
		test.setCompileLogsDirectoryName(
			"C:\\junk\\compilelogs");
		test.setHrefCompileLogsTargetPath("compilelogs");
		test.setTestManifestFileName("C:\\junk\\testManifest.xml");
		test.execute();
	}
	
	public void execute() {
		super.execute();
//		if (sendMail)
//			mailResults();
	}
	
	protected String processDropRow(PlatformStatus aPlatform) {
		String imageName = "";

		if (aPlatform.hasErrors()) {
			imageName =
				"<a href=\"" + getTestResultsHtmlFileName() + "\"><img src = \"FAIL.gif\" width=19 height=23></a>";
			testResultsStatus = "failed";		
			
		} else {
			if (testsRan()) {
				imageName = "<img src = \"OK.gif\" width=19 height=23>";
			} else {
				if (isBuildTested()) {
					imageName =
						"<font size=\"-1\" color=\"#FF0000\">pending</font>";
					testResultsStatus = "pending";
				} else {
					imageName = "<img src = \"OK.gif\" width=19 height=23>";
				}
			}
		}

		String result = "<tr>";

		result = result + "<td><div align=left>" + imageName + "</div></td>\n";
		result = result + "<td>" + aPlatform.getName() + "</td>";
		
		//generate http, md5 and sha1 links by calling php functions in the template		
		result = result + "<td><?php genLinks($_SERVER[\"SERVER_NAME\"],\"@buildlabel@\",\"" + aPlatform.getFileName() +"\"); ?></td>\n";		
		result = result + "</tr>\n";

		return result;
	}
	
//	private void mailResults() {
//		//send a different message for the following cases:
//		//build is not tested at all
//		//build is tested, tests have not run
//		//build is tested, tests have run with error and or failures
//		//build is tested, tests have run with no errors or failures
//		try {
//			mailer = new Mailer();
//		} catch (NoClassDefFoundError e) {
//			return;
//		}
//		String buildLabel = mailer.getBuildProperties().getBuildLabel();
//		String httpUrl = mailer.getBuildProperties().getHttpUrl()+"/"+buildLabel;
////		String ftpUrl = mailer.getBuildProperties().getftpUrl()+"/"+buildLabel;
//		
//		String subject = "Build is complete.  ";
//		
//		String downloadLinks="\n\nHTTP Download:\n\n\t"+httpUrl+" \n\n";
//	/*	downloadLinks=downloadLinks.concat("FTP Download:\n\n");
//		downloadLinks=downloadLinks.concat("\tuser: anonymous\n\tpassword: (e-mail address or leave blank)\n\tserver:  download.eclipse.org\n\tcd to directory:  "+buildLabel);
//		downloadLinks=downloadLinks.concat("\n\n\tor");
//		downloadLinks=downloadLinks.concat("\n\n\t"+ftpUrl);*/
//		
//		//provide http links
//		String message = "The build is complete."+downloadLinks;
//
//		if (testsRan()) {
//			subject = "Automated JUnit testing complete.  ";
//			message = "Automated JUnit testing is complete.  ";
//			subject =
//				subject.concat(
//					(getTestResultsWithProblems().endsWith("\n"))
//						? "All tests pass."
//						: "Test failures/errors occurred.");
//			message =
//				message.concat(
//					(getTestResultsWithProblems().endsWith("\n"))
//						? "All tests pass."
//						: "Test failures/errors occurred in the following:  "
//							+ getTestResultsWithProblems())+downloadLinks;
//		} else if (isBuildTested() && (!buildType.equals("N"))) {
//			subject = subject.concat("Automated JUnit testing is starting.");
//			message = "The " + subject+downloadLinks;
//		}
//
//		if (subject.endsWith("Test failures/errors occurred."))
//			mailer.sendMessage(subject, message);
//		else if (!buildType.equals("N"))
//			mailer.sendMessage(subject, message);
//
//	}

	public boolean isSendMail() {
		return sendMail;
	}

	public void setSendMail(boolean sendMail) {
		this.sendMail = sendMail;
	}

}
