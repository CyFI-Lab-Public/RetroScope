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

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * @version 	1.0
 * @author
 */
public class PlatformStatus {
	
	private String id;
	private String name;
	private String fileName;
	private boolean hasErrors = false;
	
	PlatformStatus(Element anElement) {
		super();
		NamedNodeMap attributes = anElement.getAttributes();
		this.id = (String) attributes.getNamedItem("id").getNodeValue();
		this.name = (String) attributes.getNamedItem("name").getNodeValue();
		this.fileName = (String) attributes.getNamedItem("fileName").getNodeValue();

	}

	/**
	 * Gets the id.
	 * @return Returns a String
	 */
	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getFileName() {
		return fileName;
	}
	
	public void registerError() {
		this.hasErrors = true;
	}
	
	public boolean hasErrors() {
		return this.hasErrors;
	}
}
