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
package org.eclipse.releng;

import org.apache.tools.ant.Task;

public class SystemProperty extends Task{
	//utility to allow modification of System properties from Ant script.
	private String key;
	private String value;
	
	public SystemProperty(){
		super();
	}
	
	public void execute(){
		System.setProperty(key, value);	
		if (System.getProperty(key).equals(value))
			System.out.println("System property "+key+" set to "+System.getProperty(key));
		else{
			System.out.println("System property "+key+" could not be set. Currently set to "+System.getProperty(key));
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
