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
package org.eclipse.wb.internal.core.utils.execution;

/**
 * Analog of {@link Runnable} where method <code>run</code> can throw {@link Exception}.
 * 
 * @author scheglov_ke
 * @coverage core.util
 */
public interface RunnableObjectEx<T> {
  /**
   * Executes operation that can cause {@link Exception}.
   * 
   * @return some {@link Object} result for caller.
   */
  T runObject() throws Exception;
}
