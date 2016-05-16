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
package org.eclipse.wb.internal.core.model.property.editor;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;

/**
 * Abstract {@link PropertyEditor} for that uses {@link Text} as control.
 *
 * @author scheglov_ke
 * @coverage core.model.property.editor
 */
public abstract class AbstractTextPropertyEditor extends TextDisplayPropertyEditor {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Editing
  //
  ////////////////////////////////////////////////////////////////////////////
  private Text m_textControl;
  private boolean m_ignoreFocusLost;

  // BEGIN ADT MODIFICATIONS
  // ContentProposalAdapter which exposes the openProposalPopup method such
  // that we can open the dialog up immediately on focus gain to show all available
  // alternatives (the default implementation requires at least one keytroke before
  // it shows up)
    private class ImmediateProposalAdapter extends ContentProposalAdapter
        implements FocusListener, IContentProposalListener, IContentProposalListener2 {
        private final PropertyTable m_propertyTable;
        private final IContentProposalProvider m_proposalProvider;
        public ImmediateProposalAdapter(
                Text control,
                IControlContentAdapter controlContentAdapter,
                IContentProposalProvider proposalProvider,
                KeyStroke keyStroke,
                char[] autoActivationCharacters,
                PropertyTable propertyTable) {
            super(control, controlContentAdapter, proposalProvider, keyStroke,
                    autoActivationCharacters);
            m_propertyTable = propertyTable;
            m_proposalProvider = proposalProvider;

            // On focus gain, start completing
            control.addFocusListener(this);

            // Listen on popup open and close events, in order to disable
            // focus handling on the textfield during those events.
            // This is necessary since otherwise as soon as the user clicks
            // on the popup with the mouse, the text field loses focus, and
            // then instantly closes the popup -- without the selection being
            // applied. See for example
            //   http://dev.eclipse.org/viewcvs/viewvc.cgi/org.eclipse.jface.snippets/
            //      Eclipse%20JFace%20Snippets/org/eclipse/jface/snippets/viewers/
            //      Snippet060TextCellEditorWithContentProposal.java?view=markup
            // for another example of this technique.
            addContentProposalListener((IContentProposalListener) this);
            addContentProposalListener((IContentProposalListener2) this);

            /* Triggering on empty is disabled for now: it has the unfortunate side-effect
               that it's impossible to enter a blank text field - blank matches everything,
               so the first item will automatically be selected when you press return.


            // If you edit the text and delete everything, the normal implementation
            // will close the popup; we'll reopen it
            control.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent event) {
                    if (((Text) getControl()).getText().isEmpty()) {
                        openIfNecessary();
                    }
                }
            });
            */
        }

        private void openIfNecessary() {
            if (m_textControl == null || m_textControl.isDisposed() ||
                    m_proposalProvider.getProposals(m_textControl.getText(),
                            m_textControl.getCaretPosition()).length == 0) {
                return;
            }

            getControl().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (!isProposalPopupOpen()) {
                        openProposalPopup();
                    }
                }
            });
        }

        // ---- Implements FocusListener ----

        @Override
        public void focusGained(FocusEvent event) {
            openIfNecessary();
        }

        @Override
        public void focusLost(FocusEvent event) {
        }

        // ---- Implements IContentProposalListener ----

        @Override
        public void proposalAccepted(IContentProposal proposal) {
            closeProposalPopup();
            m_propertyTable.deactivateEditor(true);
        }

        // ---- Implements IContentProposalListener2 ----

        @Override
        public void proposalPopupClosed(ContentProposalAdapter adapter) {
            m_ignoreFocusLost = false;
        }

        @Override
        public void proposalPopupOpened(ContentProposalAdapter adapter) {
            m_ignoreFocusLost = true;
        }
    }
  // END ADT MODIFICATIONS

  @Override
  public boolean activate(final PropertyTable propertyTable, final Property property, Point location)
      throws Exception {
    // create Text
    {
      m_textControl = new Text(propertyTable, SWT.NONE);

      @SuppressWarnings("unused")
      TextControlActionsManager manager = new TextControlActionsManager(m_textControl);

      m_textControl.setEditable(isEditable());

        // BEGIN ADT MODIFICATIONS
        // Add support for field completion, if the property provides an IContentProposalProvider
        // via its the getAdapter method.
        IContentProposalProvider completion = property.getAdapter(IContentProposalProvider.class);
        if (completion != null) {
            ImmediateProposalAdapter adapter = new ImmediateProposalAdapter(
                    m_textControl, new TextContentAdapter(), completion, null, null,
                    propertyTable);
            adapter.setFilterStyle(ContentProposalAdapter.FILTER_NONE);
            adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
            ILabelProvider labelProvider = property.getAdapter(ILabelProvider.class);
            if (labelProvider != null) {
                adapter.setLabelProvider(labelProvider);
            }
        }
        // END ADT MODIFICATIONS
      m_textControl.setFocus();
    }
    // add listeners
    m_textControl.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        try {
          handleKeyPressed(propertyTable, property, e);
        } catch (Throwable ex) {
          propertyTable.deactivateEditor(false);
          propertyTable.handleException(ex);
        }
      }
    });
    m_textControl.addListener(SWT.FocusOut, new Listener() {
      @Override
    public void handleEvent(Event event) {
        if (!m_ignoreFocusLost) {
          propertyTable.deactivateEditor(true);
        }
      }
    });
    // set data
    toWidget(property);
    // keep us active
    return true;
  }

  @Override
  public final void setBounds(Rectangle bounds) {
    m_textControl.setBounds(bounds);
  }

  @Override
  public final void deactivate(PropertyTable propertyTable, Property property, boolean save) {
    if (save) {
      try {
        toProperty(property);
      } catch (Throwable e) {
        propertyTable.deactivateEditor(false);
        propertyTable.handleException(e);
      }
    }
    // dispose Text widget
    if (m_textControl != null) {
      m_textControl.dispose();
      m_textControl = null;
    }
  }

  @Override
  public void keyDown(PropertyTable propertyTable, Property property, KeyEvent event)
      throws Exception {
    boolean withAlt = (event.stateMask & SWT.ALT) != 0;
    boolean withCtrl = (event.stateMask & SWT.CTRL) != 0;
    if (event.character != 0 && !(withAlt || withCtrl)) {
      propertyTable.activateEditor(property, null);
      postKeyEvent(SWT.KeyDown, event);
      postKeyEvent(SWT.KeyUp, event);
    }
  }

  /**
   * Posts low-level {@link SWT.KeyDown} or {@link SWT.KeyUp} event.
   */
  private static void postKeyEvent(int type, KeyEvent event) {
    Event lowEvent = new Event();
    lowEvent.type = type;
    lowEvent.keyCode = event.keyCode;
    lowEvent.character = event.character;
    // post event
    Display.getCurrent().post(lowEvent);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Events
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Handles {@link KeyListener#keyPressed(KeyEvent)}.
   */
  private void handleKeyPressed(PropertyTable propertyTable, Property property, KeyEvent e)
      throws Exception {
    if (e.keyCode == SWT.CR) {
      toProperty(property);

      // BEGIN ADT MODIFICATIONS
      // After pressing return, dismiss the text cursor to make the value "committed".
      // I'm not sure why this is necessary here and not in WindowBuilder proper.
      propertyTable.deactivateEditor(true);
      // END ADT MODIFICATIONS
    } else if (e.keyCode == SWT.ESC) {
      propertyTable.deactivateEditor(false);
    } else if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) {
      e.doit = false;
      boolean success = toProperty(property);
      // don't allow navigation if current text can not be transferred to property
      if (!success) {
        return;
      }
      // OK, deactivate and navigate
      propertyTable.deactivateEditor(true);
      propertyTable.navigate(e);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Implementation
  //
  ////////////////////////////////////////////////////////////////////////////
  private String m_currentText;

  /**
   * Transfers data from {@link Property} to widget.
   */
  private void toWidget(Property property) throws Exception {
    // prepare text
    String text = getEditorText(property);
    if (text == null) {
      text = "";
    }
    // set text
    m_currentText = text;
    m_textControl.setText(text);
    m_textControl.selectAll();
  }

  /**
   * Transfers data from widget to {@link Property}.
   *
   * @return <code>true</code> if transfer was successful.
   */
  private boolean toProperty(Property property) throws Exception {
    // BEGIN ADT MODIFICATIONS
    if (m_textControl == null) {
      return false;
    }
    // END ADT MODIFICATIONS
    String text = m_textControl.getText();
    // change property only if text was changed
    if (!m_currentText.equals(text)) {
      m_ignoreFocusLost = true;
      try {
        boolean success = setEditorText(property, text);
        if (!success) {
          return false;
        }
      } finally {
        m_ignoreFocusLost = false;
      }
      // if value was successfully changed, update current text
      m_currentText = text;
    }
    // OK, success
    return true;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Operations
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return <code>true</code> if this editor can modify text.
   */
  protected boolean isEditable() {
    return true;
  }

  /**
   * @return the text to display in {@link Text} control.
   */
  protected abstract String getEditorText(Property property) throws Exception;

  /**
   * Modifies {@link Property} using given text.
   *
   * @return <code>true</code> if {@link Property} was successfully modified.
   */
  protected abstract boolean setEditorText(Property property, String text) throws Exception;
}
