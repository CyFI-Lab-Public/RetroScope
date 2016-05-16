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
package org.eclipse.wb.internal.core.utils.binding.editors.controls;

import com.google.common.collect.Lists;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;

import java.util.List;

/**
 * Manager for installing/unistalling global handlers for {@link Control} actions commands.
 *
 * @author sablin_aa
 * @author mitin_aa
 */
public abstract class AbstractControlActionsManager {
  @SuppressWarnings("deprecation")
  protected final Object[] COMMAND_HANDLER_IDS = new Object[]{
      IWorkbenchActionDefinitionIds.COPY,
      IWorkbenchActionDefinitionIds.CUT,
      IWorkbenchActionDefinitionIds.PASTE,
      IWorkbenchActionDefinitionIds.DELETE,
      IWorkbenchActionDefinitionIds.SELECT_ALL,
      IWorkbenchActionDefinitionIds.UNDO,
      IWorkbenchActionDefinitionIds.REDO};
  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  protected final Control m_control;
  private boolean m_active = false;

  public AbstractControlActionsManager(final Control control) {
    m_control = control;
    m_control.addFocusListener(new FocusListener() {
      @Override
    public void focusGained(FocusEvent e) {
        activateHandlers();
        m_active = true;
      }

      @Override
    public void focusLost(FocusEvent e) {
        deactivateHandlers();
        m_active = false;
      }
    });
    m_control.addDisposeListener(new DisposeListener() {
      @Override
    public void widgetDisposed(DisposeEvent e) {
        if (m_active) {
          // deactivate on dispose
          deactivateHandlers();
        }
      }
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Handlers
  //
  ////////////////////////////////////////////////////////////////////////////
  protected static final IHandler EMPTY_HANDLER = new AbstractHandler() {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
      // do nothing
      return null;
    }

    @Override
    public boolean isEnabled() {
      // of course, it is enabled ;)
      return true;
    }

    @Override
    public boolean isHandled() {
      // we do not handle the actions; since action not handled, its' underlying SWT event
      // would not be filtered by workbench, so it get a chance to be handled by SWT code
      // or to be passed to the OS; see WorkbenchKeyboard.press() method.
      return false;
    }
  };

  /**
   * @returns the global handler service.
   */
  public static IHandlerService getHandlerService() {
    return (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
  }

  /**
   * Activates the handlers for list of commands (see COMMAND_HANDLERS) with:<br>
   * 1. The empty handler (except 'selectAll'), so underlying SWT event wouldn't be filtered by the
   * workbench;<br>
   * 2. Highest priority {@link Expression}, so this handler has a chance to be set.
   */
  protected void activateHandlers() {
    IHandlerService service = getHandlerService();
    for (int i = 0; i < COMMAND_HANDLER_IDS.length; ++i) {
      String actionName = (String) COMMAND_HANDLER_IDS[i];
      IHandler handler = getHandlerFor(actionName);
      activateHandler(actionName, service, handler, new Expression() {
        @Override
        public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
          return EvaluationResult.TRUE;
        }

        @Override
        public void collectExpressionInfo(ExpressionInfo info) {
          // get the highest priority
          // note, if someone else has such priority, there will be key-binding conflicts logged.
          info.markSystemPropertyAccessed();
          info.markDefaultVariableAccessed();
          info.addVariableNameAccess(ISources.ACTIVE_MENU_NAME);
        }
      });
    }
  }

  protected IHandler getHandlerFor(String actionName) {
    return EMPTY_HANDLER;
  }

  /**
   * Activates handler and stores it into a collection for further deactivation.
   */
  private final List<IHandlerActivation> m_activations = Lists.newLinkedList();

  private void activateHandler(String actionName,
      IHandlerService service,
      IHandler handler,
      Expression highPriorityExpression) {
    // activate handler and store it into a collection for further deactivation
    m_activations.add(service.activateHandler(actionName, handler, highPriorityExpression));
  }

  /**
   * Deactivates all handlers and clears handlers collection.
   */
  protected void deactivateHandlers() {
    getHandlerService().deactivateHandlers(m_activations);
    m_activations.clear();
  }
}
