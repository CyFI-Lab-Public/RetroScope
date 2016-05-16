/*******************************************************************************
 * Copyright (c) 2011 Google, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Google, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.wb.internal.core.model.property.table;

import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.editor.PropertyEditor;

/**
 * Handler for any {@link Exception} that happens in {@link PropertyTable}, i.e. exceptions during
 * {@link Property} modifications using {@link PropertyEditor}'s.
 * 
 * @author scheglov_ke
 * @coverage core.model.property.table
 */
public interface IPropertyExceptionHandler {
  void handle(Throwable e);
}
