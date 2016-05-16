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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Properties;
import java.io.PrintWriter;

/**
 * @author SDimitrov
 * 
 * This class finds an available build machine by reading a registry of build
 * machines and their status.
 *  
 */
public class BuildMachineManager extends Thread {
	// registry mapping of machines being used by a given build
	//list is the path to the configuration of build machines available for a
	// given build type
	// waitInterval is the number of seconds to wait if no machines are
	// available for a given build purpose
	// findKey represents the key in cfg from which which to obtain the list of
	// machines
	// createKey is written to the registry with the machine name that is
	// available

	private String markerContainer;
	private int waitInterval;
	private String markerName;
	private String markerKey="0";
	private String cfgKey;
	private String cfg;

	public BuildMachineManager() {
		super();
	}
	public BuildMachineManager(
		String cfg,
		String markerContainer,
		int waitInterval,
		String markerName,
		String markerKey,
		String cfgKey) {
		this.waitInterval = waitInterval;
		this.cfg = cfg;
		this.markerContainer = markerContainer;
		this.markerName = markerName;
		this.markerKey = markerKey;
		this.cfgKey = cfgKey;
		this.run();
	}

	public void run() {
		String[] machines = getNames();
		
		if (new File(markerContainer+"/"+markerName+".marker").exists()){
			System.out.println("Marker already exists: "+markerName+".marker");
			return;
		}
		
		boolean machineFound = false;
		try {
			while (!machineFound) {
				for (int i = 0; i < machines.length; i++) {
					if (!inUse(machines[i])) {
						if (createNewMarker(machines[i])) {
							machineFound = true;
							return;
						}
					}
				}

				//wait a given interval before re-checking for an available
				// build machine.
				sleep(1000 * waitInterval);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private boolean createNewMarker(String machineName) {
		//create a temporary lock on marker container
		
		File lock = new File(markerContainer + "/" + "lock");
		if (lock.exists())
			return false;

		try {
			File markerFile =  new File(markerContainer+"/"+markerName+".marker");
			lock.createNewFile();
			markerFile.createNewFile();
			PrintWriter out = new PrintWriter(new FileWriter(markerFile));
			out.println(markerKey+"=" + machineName);
			out.flush();
			out.close();
			lock.delete();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private boolean inUse(String machineName) {
		
		File container = new File(markerContainer);

		if (container.exists() && container.isDirectory()) {
			Properties properties = null;
			File[] markerFiles = container.listFiles();
			for (int i = 0; i < markerFiles.length; i++) {
				File markerFile = markerFiles[i];
				if (markerFile.getName().endsWith(".marker")) {
					properties = new Properties();
					try {
						FileInputStream in = new FileInputStream(markerFiles[i]);
						properties.load(in);
						in.close();
						if (properties.containsValue(machineName)){
							return true;
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return false;
	}

	private String[] getNames() {
		StringTokenizer tokenizer = new StringTokenizer(getList(), ",");
		String[] names = new String[tokenizer.countTokens()];
		int i = 0;

		while (tokenizer.hasMoreTokens()) {
			names[i++] = tokenizer.nextToken();
		}
		return names;
	}

	private String getList() {
		Properties cfgProperties = new Properties();
		try {
			FileInputStream in = new FileInputStream(new File(cfg));
			cfgProperties.load(in);
			in.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cfgProperties.getProperty(cfgKey);
	}
}
