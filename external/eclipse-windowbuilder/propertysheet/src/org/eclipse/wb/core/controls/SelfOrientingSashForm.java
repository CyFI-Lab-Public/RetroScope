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
package org.eclipse.wb.core.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

/**
 * Instances of the class <code>SelfOrientingSashForm</code> implement a sash form that will
 * automatically reset its orientation based on the relationship between the width and height of the
 * client area. This is done so that the sash form can be placed in a view that will sometimes be
 * tall and narrow and sometimes be short and wide and still lay out its children in a pleasing way.
 * <p>
 * 
 * @author unknown
 * @author Brian Wilkerson
 * @version $Revision: 1.2 $
 * @coverage core.control
 */
public class SelfOrientingSashForm extends SashForm {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructors
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Initialize a newly created control to have the given parent and style. The style describes the
   * behavior and appearance of this control.
   * <p>
   * The style value is either one of the style constants defined in the class <code>SWT</code>
   * which is applicable to instances of this class, or must be built by <em>bitwise OR</em>'ing
   * together (that is, using the <code>int</code> "|" operator) two or more of those
   * <code>SWT</code> style constants. The class description for all SWT widget classes should
   * include a comment which describes the style constants which are applicable to the class.
   * </p>
   * 
   * @param parent
   *          a widget which will be the parent of the new instance (not null)
   * @param style
   *          the style of widget to construct
   * 
   * @exception IllegalArgumentException
   *              <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
   *              </ul>
   * @exception SWTException
   *              <ul>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
   *              parent</li>
   *              <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
   *              </ul>
   */
  public SelfOrientingSashForm(Composite parent, int style) {
    super(parent, style);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Layout
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Returns SWT.HORIZONTAL if the controls in the SashForm are laid out side by side or
   * SWT.VERTICAL if the controls in the SashForm are laid out top to bottom.
   * 
   * @return SWT.HORIZONTAL or SWT.VERTICAL
   */
  @Override
  public int getOrientation() {
    int currentOrientation = super.getOrientation();
    if (inSetOrientation) {
      return currentOrientation;
    }
    int preferredOrientation = isDisposed() ? currentOrientation : getPreferredOrientation();
    if (currentOrientation != preferredOrientation) {
      setOrientation(preferredOrientation);
    }
    return preferredOrientation;
  }

  boolean inSetOrientation = false;

  @Override
  public void setOrientation(int orientation) {
    if (inSetOrientation) {
      return;
    }
    inSetOrientation = true;
    super.setOrientation(orientation);
    inSetOrientation = false;
  }

  /**
   * If the receiver has a layout, ask the layout to <em>lay out</em> (that is, set the size and
   * location of) the receiver's children. If the argument is <code>true</code> the layout must not
   * rely on any cached information it is keeping about the children. If it is <code>false</code>
   * the layout may (potentially) simplify the work it is doing by assuming that the state of the
   * none of the receiver's children has changed since the last layout. If the receiver does not
   * have a layout, do nothing.
   * 
   * @param changed
   *          <code>true</code> if the layout must flush its caches, and <code>false</code>
   *          otherwise
   * 
   * @exception SWTException
   *              <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
   *              receiver</li>
   *              </ul>
   */
  @Override
  public void layout(boolean changed) {
    Rectangle area;
    int oldOrientation, newOrientation;
    area = getClientArea();
    if (area.width > 0 && area.height > 0) {
      oldOrientation = super.getOrientation();
      newOrientation = SWT.HORIZONTAL;
      if (area.width < area.height) {
        newOrientation = SWT.VERTICAL;
      }
      if (newOrientation != oldOrientation) {
        setOrientation(newOrientation);
        changed = true;
      }
    }
    super.layout(changed);
  }

  private int getPreferredOrientation() {
    Rectangle area = getClientArea();
    if (area.width > 0 && area.height > 0 && area.width < area.height) {
      return SWT.VERTICAL;
    }
    return SWT.HORIZONTAL;
  }
}