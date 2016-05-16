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
package org.eclipse.wb.internal.core.utils.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

/**
 * Implementation of {@link ImageDescriptor} for {@link Image} instance.
 * 
 * @author scheglov_ke
 * @coverage core.ui
 */
public final class ImageImageDescriptor extends ImageDescriptor {
  private final Image m_Image;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public ImageImageDescriptor(Image image) {
    m_Image = image;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // ImageDescriptor
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public ImageData getImageData() {
    return m_Image == null ? null : m_Image.getImageData();
  }
}
