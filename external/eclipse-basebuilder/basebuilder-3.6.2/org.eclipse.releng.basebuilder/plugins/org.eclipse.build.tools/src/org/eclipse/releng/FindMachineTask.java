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
 * Created on 8-Jan-2004
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.releng;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
/**
 * @author SDimitrov
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
public class FindMachineTask extends Task {

	// findKey represents the key in cfg from which which to obtain the list of machines
	// createKey is written to the registry with the machine name that is available
	//registry mapping of machines being used by a given build
	private String markerContainer;
	
//time in seconds to wait before re-checking for an available machine
	private int waitInterval;
	private String markerName;
	private String markerKey="0";
//list is the path to the configuration of build machines available for a given build type
	private String cfgKey;
	private String cfg;	
	
	/**
	 * 
	 */
	public static void main(String args[]){
		FindMachineTask test = new FindMachineTask();
		test.markerContainer = "D:\\junk\\markers";
		test.cfg="D:\\workspaces\\current\\eclipseInternalBuildTools\\testConfig.properties";
		test.cfgKey="windowstesting";
		test.markerName = "200412091500";
		test.waitInterval = 10;
		test.execute();
	}
	public FindMachineTask() {
		super();
	}
	public void execute() throws BuildException {
		new BuildMachineManager(cfg,markerContainer,waitInterval,markerName,markerKey,cfgKey);
	}
	/**
	 * @return Returns the cfg.
	 */
	public String getCfg() {
		return cfg;
	}

	/**
	 * @param cfg The cfg to set.
	 */
	public void setCfg(String cfg) {
		this.cfg = cfg;
	}

	/**
	 * @return Returns the createKey.
	 */
	public String getMarkerName() {
		return markerName;
	}

	/**
	 * @param createKey The createKey to set.
	 */
	public void setMarkerName(String markerName) {
		this.markerName = markerName;
	}

/**
 * @return Returns the findKey.
 */
public String getCfgKey() {
	return cfgKey;
}

/**
 * @param findKey The findKey to set.
 */
public void setCfgKey(String cfgKey) {
	this.cfgKey = cfgKey;
}

	/**
	 * @return Returns the registry.
	 */
	public String getMarkerContainer() {
		return markerContainer;
	}

	/**
	 * @param registry The registry to set.
	 */
	public void setMarkerContainer(String markerContainer) {
		this.markerContainer = markerContainer;
	}

/**
 * @return Returns the waitInterval.
 */
public int getWaitInterval() {
	return waitInterval;
}

/**
 * @param waitInterval The waitInterval to set.
 */
public void setWaitInterval(int waitInterval) {
	this.waitInterval = waitInterval;
}

	/**
	 * @return Returns the markerKey.
	 */
	public String getMarkerKey() {
		return markerKey;
	}

	/**
	 * @param markerKey The markerKey to set.
	 */
	public void setMarkerKey(String markerKey) {
		this.markerKey = markerKey;
	}

}
