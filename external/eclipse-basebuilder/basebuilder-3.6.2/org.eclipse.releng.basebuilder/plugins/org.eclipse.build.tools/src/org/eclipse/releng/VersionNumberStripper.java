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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import java.io.File;

/*
 * A class that strips version numbers off built plugin directory names.  This
 * is helpful when prebuilt plugins are used in generating javadoc (on the
 * classpath).
 */
 
public class VersionNumberStripper extends Task {

	//the directory containing the directories and files from which to remove version information
	private String directory;
	
	public VersionNumberStripper() {
		super();
	}

	public void setDirectory(String dir){directory=dir;}
	
	public String getDirectory(){return directory;}

  	public void execute() throws BuildException {
  		setDirectory(directory);
  		stripVersions();
  	}

	public static void main(String[] args) {
		new VersionNumberStripper().execute();
	}

	private void stripVersions(){
		/* rename directories by removing anything from an underscore onward,
		 * assuming that anything following the first
		 * occurence of an underscore is a version number
		 */
		 
		File file=new File(directory);
		
		File [] files = file.listFiles();
		
		for (int i=0; i<files.length; i++){
			String absolutePath = files[i].getAbsolutePath();
			String path = absolutePath.substring(0, absolutePath.length() 
					- files[i].getName().length());
			
			int underScorePos = files[i].getName().indexOf("_");
			int jarExtPos = files[i].getName().indexOf(".jar");
			if (underScorePos != -1) {
				String targetPath;
				if (jarExtPos != -1) {
					targetPath =path
							+ files[i].getName().substring(0, underScorePos)
							+ ".jar";
				} else {
					targetPath = path  
							+ files[i].getName().substring(0, underScorePos);
				}
				files[i].renameTo(new File(targetPath));
			}

		}
	}
}

