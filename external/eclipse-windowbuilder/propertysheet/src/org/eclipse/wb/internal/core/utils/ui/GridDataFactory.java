/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wb.internal.core.utils.ui;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;

/**
 * This class provides a convienient shorthand for creating and initializing GridData. This offers
 * several benefits over creating GridData normal way:
 * 
 * <ul>
 * <li>The same factory can be used many times to create several GridData instances</li>
 * <li>The setters on GridDataFactory all return "this", allowing them to be chained</li>
 * <li>GridDataFactory uses vector setters (it accepts Points), making it easy to set X and Y values
 * together</li>
 * </ul>
 * 
 * <p>
 * GridDataFactory instances are created using one of the static methods on this class.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * <code>
 * 
 * ////////////////////////////////////////////////////////////
 * // Example 1: Typical grid data for a non-wrapping label
 * 
 *     // GridDataFactory version
 *     GridDataFactory.fillDefaults().applyTo(myLabel);
 * 
 *     // Equivalent SWT version
 *     GridData labelData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
 *     myLabel.setLayoutData(labelData);
 * 
 * ///////////////////////////////////////////////////////////
 * // Example 2: Typical grid data for a wrapping label
 * 
 *     // GridDataFactory version
 *     GridDataFactory.fillDefaults()
 *          .align(SWT.FILL, SWT.CENTER)
 *    	    .hint(150, SWT.DEFAULT)
 *    	    .grab(true, false)
 *          .applyTo(wrappingLabel);
 *      
 *     // Equivalent SWT version
 *     GridData wrappingLabelData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
 *     wrappingLabelData.minimumWidth = 1;
 *     wrappingLabelData.widthHint = 150;
 *     wrappingLabel.setLayoutData(wrappingLabelData);
 * 
 * //////////////////////////////////////////////////////////////
 * // Example 3: Typical grid data for a scrollable control (a list box, tree, table, etc.)
 * 
 *     // GridDataFactory version
 *     GridDataFactory.fillDefaults().grab(true, true).hint(150, 150).applyTo(listBox);
 * 
 *     // Equivalent SWT version
 *     GridData listBoxData = new GridData(GridData.FILL_BOTH);
 *     listBoxData.widthHint = 150;
 *     listBoxData.heightHint = 150;
 *     listBoxData.minimumWidth = 1;
 *     listBoxData.minimumHeight = 1;
 *     listBox.setLayoutData(listBoxData);
 * 
 * /////////////////////////////////////////////////////////////
 * // Example 4: Typical grid data for a button
 * 
 *     // GridDataFactory version
 *     Point preferredSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
 *     Point hint = Geometry.max(LayoutConstants.getMinButtonSize(), preferredSize);
 *     GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).hint(hint).applyTo(button);
 * 
 *     // Equivalent SWT version
 *     Point preferredSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
 *     Point hint = Geometry.max(LayoutConstants.getMinButtonSize(), preferredSize);
 *     GridData buttonData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
 *     buttonData.widthHint = hint.x;
 *     buttonData.heightHint = hint.y;
 *     button.setLayoutData(buttonData); 
 * </code>
 * 
 * <p>
 * IMPORTANT: WHEN ASSIGNING LAYOUT DATA TO A CONTROL, BE SURE TO USE
 * gridDataFactory.applyTo(control) AND NEVER control.setLayoutData(gridDataFactory).
 * </p>
 * 
 * @since 3.2
 */
public final class GridDataFactory {
  private final Control m_control;
  private final PixelConverter m_pixelConverter;
  private final GridData m_data;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  private GridDataFactory(Control control, GridData gridData) {
    m_control = control;
    m_pixelConverter = new PixelConverter(m_control);
    //
    m_data = gridData;
    if (m_control.getLayoutData() != m_data) {
      m_control.setLayoutData(m_data);
    }
  }

  /**
   * Creates new {@link GridDataFactory} with new {@link GridData}.
   */
  public static GridDataFactory create(Control control) {
    return new GridDataFactory(control, new GridData());
  }

  /**
   * Creates new {@link GridDataFactory} for modifying {@link GridData} already installed in
   * control.
   */
  public static GridDataFactory modify(Control control) {
    GridData gridData;
    {
      Object existingLayoutData = control.getLayoutData();
      if (existingLayoutData instanceof GridData) {
        gridData = (GridData) existingLayoutData;
      } else {
        gridData = new GridData();
      }
    }
    return new GridDataFactory(control, gridData);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Span
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the GridData span. The span controls how many cells are filled by the control.
   * 
   * @param hSpan
   *          number of columns spanned by the control
   * @param vSpan
   *          number of rows spanned by the control
   * @return this
   */
  public GridDataFactory span(int hSpan, int vSpan) {
    m_data.horizontalSpan = hSpan;
    m_data.verticalSpan = vSpan;
    return this;
  }

  /**
   * Sets the GridData span. The span controls how many cells are filled by the control.
   * 
   * @param hSpan
   *          number of columns spanned by the control
   * @return this
   */
  public GridDataFactory spanH(int hSpan) {
    m_data.horizontalSpan = hSpan;
    return this;
  }

  /**
   * Sets the GridData span. The span controls how many cells are filled by the control.
   * 
   * @param vSpan
   *          number of rows spanned by the control
   * @return this
   */
  public GridDataFactory spanV(int vSpan) {
    m_data.verticalSpan = vSpan;
    return this;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Hint
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the width and height hints. The width and height hints override the control's preferred
   * size. If either hint is set to SWT.DEFAULT, the control's preferred size is used.
   * 
   * @param xHint
   *          horizontal hint (pixels), or SWT.DEFAULT to use the control's preferred size
   * @param yHint
   *          vertical hint (pixels), or SWT.DEFAULT to use the control's preferred size
   * @return this
   */
  public GridDataFactory hint(int xHint, int yHint) {
    m_data.widthHint = xHint;
    m_data.heightHint = yHint;
    return this;
  }

  /**
   * Sets hint in chars.
   */
  public GridDataFactory hintC(int xHintInChars, int yHintInChars) {
    hintHC(xHintInChars);
    hintVC(yHintInChars);
    return this;
  }

  /**
   * Sets the width hint.
   * 
   * @return this
   */
  public GridDataFactory hintH(int xHint) {
    m_data.widthHint = xHint;
    return this;
  }

  /**
   * Sets the width hint to the minimum of current hint and given <code>otherHint</code>.
   * 
   * @return this
   */
  public GridDataFactory hintHMin(int otherHint) {
    m_data.widthHint = Math.min(m_data.widthHint, otherHint);
    return this;
  }

  /**
   * Sets the width hint in chars.
   * 
   * @return this
   */
  public GridDataFactory hintHC(int hintInChars) {
    return hintH(m_pixelConverter.convertWidthInCharsToPixels(hintInChars));
  }

  /**
   * Sets the width hint.
   * 
   * @return this
   */
  public GridDataFactory hintHU(int hintInDLU) {
    return hintH(m_pixelConverter.convertHorizontalDLUsToPixels(hintInDLU));
  }

  /**
   * Sets the height hint.
   * 
   * @return this
   */
  public GridDataFactory hintV(int yHint) {
    m_data.heightHint = yHint;
    return this;
  }

  /**
   * Sets the height hint in chars.
   * 
   * @return this
   */
  public GridDataFactory hintVC(int hintInChars) {
    return hintV(m_pixelConverter.convertHeightInCharsToPixels(hintInChars));
  }

  /**
   * Increments horizontal hint on given value.
   * 
   * @return this
   */
  public GridDataFactory hintHAdd(int increment) {
    return hintV(m_data.widthHint + increment);
  }

  /**
   * Increments vertical hint on given value.
   * 
   * @return this
   */
  public GridDataFactory hintVAdd(int increment) {
    return hintV(m_data.heightHint + increment);
  }

  /**
   * Sets the width and height hints. The width and height hints override the control's preferred
   * size. If either hint is set to SWT.DEFAULT, the control's preferred size is used.
   * 
   * @param hint
   *          size (pixels) to be used instead of the control's preferred size. If the x or y values
   *          are set to SWT.DEFAULT, the control's computeSize() method will be used to obtain that
   *          dimension of the preferred size.
   * @return this
   */
  public GridDataFactory hint(Point hint) {
    m_data.widthHint = hint.x;
    m_data.heightHint = hint.y;
    return this;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Minimum size
  //
  ////////////////////////////////////////////////////////////////////////////
  public GridDataFactory minH(int minimumWidth) {
    m_data.minimumWidth = minimumWidth;
    return this;
  }

  public GridDataFactory minHC(int widthInChars) {
    return minH(m_pixelConverter.convertWidthInCharsToPixels(widthInChars));
  }

  public GridDataFactory minV(int minimumHeight) {
    m_data.minimumHeight = minimumHeight;
    return this;
  }

  public GridDataFactory minVC(int heightInChars) {
    return minV(m_pixelConverter.convertHeightInCharsToPixels(heightInChars));
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Alignment
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the alignment of the control within its cell.
   * 
   * @param hAlign
   *          horizontal alignment. One of SWT.BEGINNING, SWT.CENTER, SWT.END, or SWT.FILL.
   * @param vAlign
   *          vertical alignment. One of SWT.BEGINNING, SWT.CENTER, SWT.END, or SWT.FILL.
   * @return this
   */
  public GridDataFactory align(int hAlign, int vAlign) {
    m_data.horizontalAlignment = hAlign;
    m_data.verticalAlignment = vAlign;
    return this;
  }

  /**
   * Sets the horizontal and vertical alignment to GridData.FILL.
   */
  public GridDataFactory fill() {
    return align(GridData.FILL, GridData.FILL);
  }

  /**
   * Sets the horizontal alignment of the control within its cell.
   * 
   * @param hAlign
   *          horizontal alignment. One of SWT.BEGINNING, SWT.CENTER, SWT.END, or SWT.FILL.
   * @return this
   */
  public GridDataFactory alignH(int hAlign) {
    m_data.horizontalAlignment = hAlign;
    return this;
  }

  /**
   * Sets the horizontal alignment of the control to GridData.BEGINNING
   * 
   * @return this
   */
  public GridDataFactory alignHL() {
    return alignH(GridData.BEGINNING);
  }

  /**
   * Sets the horizontal alignment of the control to GridData.CENTER
   * 
   * @return this
   */
  public GridDataFactory alignHC() {
    return alignH(GridData.CENTER);
  }

  /**
   * Sets the horizontal alignment of the control to GridData.FILL
   * 
   * @return this
   */
  public GridDataFactory alignHF() {
    return alignH(GridData.FILL);
  }

  /**
   * Sets the horizontal alignment of the control to GridData.FILL
   * 
   * @return this
   */
  public GridDataFactory fillH() {
    return alignHF();
  }

  /**
   * Sets the horizontal alignment of the control to GridData.END
   * 
   * @return this
   */
  public GridDataFactory alignHR() {
    return alignH(GridData.END);
  }

  /**
   * Sets the vertical alignment of the control within its cell.
   * 
   * @param vAlign
   *          vertical alignment. One of SWT.BEGINNING, SWT.CENTER, SWT.END, or SWT.FILL.
   * @return this
   */
  public GridDataFactory alignV(int vAlign) {
    m_data.verticalAlignment = vAlign;
    return this;
  }

  /**
   * Sets the vertical alignment of the control to GridData.BEGINNING
   * 
   * @return this
   */
  public GridDataFactory alignVT() {
    return alignV(GridData.BEGINNING);
  }

  /**
   * Sets the vertical alignment of the control to GridData.CENTER
   * 
   * @return this
   */
  public GridDataFactory alignVM() {
    return alignV(GridData.CENTER);
  }

  /**
   * Sets the vertical alignment of the control to GridData.FILL
   * 
   * @return this
   */
  public GridDataFactory alignVF() {
    return alignV(GridData.FILL);
  }

  /**
   * Sets the vertical alignment of the control to GridData.FILL
   * 
   * @return this
   */
  public GridDataFactory fillV() {
    return alignVF();
  }

  /**
   * Sets the vertical alignment of the control to GridData.END
   * 
   * @return this
   */
  public GridDataFactory alignVB() {
    return alignV(GridData.END);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Indent
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the indent of the control within the cell in pixels.
   */
  public GridDataFactory indentH(int hIndent) {
    m_data.horizontalIndent = hIndent;
    return this;
  }

  /**
   * Sets the indent of the control within the cell in characters.
   */
  public GridDataFactory indentHC(int hIndent) {
    m_data.horizontalIndent = m_pixelConverter.convertWidthInCharsToPixels(hIndent);
    return this;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Grab
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Determines whether extra horizontal or vertical space should be allocated to this control's
   * column when the layout resizes. If any control in the column is set to grab horizontal then the
   * whole column will grab horizontal space. If any control in the row is set to grab vertical then
   * the whole row will grab vertical space.
   * 
   * @param horizontal
   *          true if the control's column should grow horizontally
   * @param vertical
   *          true if the control's row should grow vertically
   * @return this
   */
  public GridDataFactory grab(boolean horizontal, boolean vertical) {
    m_data.grabExcessHorizontalSpace = horizontal;
    m_data.grabExcessVerticalSpace = vertical;
    return this;
  }

  public GridDataFactory grabH() {
    m_data.grabExcessHorizontalSpace = true;
    return this;
  }

  public GridDataFactory grabV() {
    m_data.grabExcessVerticalSpace = true;
    return this;
  }

  public GridDataFactory grab() {
    return grab(true, true);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Exclude
  //
  ////////////////////////////////////////////////////////////////////////////
  public GridDataFactory exclude(boolean value) {
    m_data.exclude = value;
    return this;
  }
}
