/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.wizards.actions.NewProjectAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.part.IntroPart;

import java.net.URL;

public class AdtIntro extends IntroPart implements IIntroPart {
    private static final int TEXT_WIDTH = 600;

    private FormToolkit mToolkit;
    private ScrolledForm mForm;

    @Override
    public void standbyStateChanged(boolean standby) {
    }

    @Override
    public void createPartControl(Composite parent) {
        mToolkit = new FormToolkit(parent.getDisplay());
        mForm = mToolkit.createScrolledForm(parent);

        mForm.setText("Welcome!");
        mToolkit.decorateFormHeading(mForm.getForm());
        mForm.getToolBarManager().update(true);

        //TableWrapLayout layout = new TableWrapLayout();
        //layout.numColumns = 2;
        GridLayout layout = new GridLayout(1, false);
        mForm.getBody().setLayout(layout);

        createText(mForm.getBody(),
                "The Android Developer Tools provide a first-class development environment for " +
                "building Android apps. This integrated development environment is set up with " +
                "the latest version of the Android platform and system image so you can " +
                "immediately begin building apps and running them on the Android emulator."
                );

        Button newProject = mToolkit.createButton(mForm.getBody(),
                "New Android Application...",
                SWT.PUSH);
        newProject.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                NewProjectAction npwAction = new NewProjectAction();
                npwAction.run(null /*action*/);
                PlatformUI.getWorkbench().getIntroManager().closeIntro(AdtIntro.this);
            }
        });

        Section section = mToolkit.createSection(mForm.getBody(),
                Section.SHORT_TITLE_BAR | Section.CLIENT_INDENT);
        section.setText("Tutorials");

        Composite c = mToolkit.createComposite(section);
        c.setLayout(new GridLayout(2, false));

        createHyperlink(c, "Build Your First App",
                "http://developer.android.com/training/basics/firstapp/index.html");
        createText(c, "If you're new to Android, follow this class to learn the fundamental " +
                "Android APIs for creating a user interface that responds to input.");

        createHyperlink(c, "Design Your App", "http://developer.android.com/design/index.html");
        createText(c,
                "Before you begin developing your app, be sure you understand the design patterns "
                + "that Android users expect from your app.");

        createHyperlink(c, "Test Your App",
                "http://developer.android.com/tools/testing/testing_android.html");
        createText(c, "The Android Framework provides tools that help you test every aspect of "
                + "your app to be sure it behaves as expected under various conditions.");

        section.setClient(c);

        mForm.reflow(true);
    }

    private void createText(Composite body, String text) {
        Label l = mToolkit.createLabel(body, text, SWT.WRAP);
        GridData gd = new GridData();
        gd.widthHint = TEXT_WIDTH;
        l.setLayoutData(gd);
    }

    private void createHyperlink(Composite c, String text, String url) {
        Hyperlink link = mToolkit.createHyperlink(c, text, SWT.WRAP);
        link.setHref(url);
        link.addHyperlinkListener(sHyperLinkListener);
        GridData gd = new GridData();
        gd.verticalAlignment = SWT.TOP;
        link.setLayoutData(gd);
    }

    private static final IHyperlinkListener sHyperLinkListener = new HyperlinkAdapter() {
        @Override
        public void linkActivated(HyperlinkEvent e) {
            if (!(e.getHref() instanceof String)) {
                return;
            }

            String url = (String) e.getHref();
            if (url.isEmpty()) {
                return;
            }

            IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser browser;
            try {
                browser = support.getExternalBrowser();
                browser.openURL(new URL(url));
            } catch (Exception ex) {
                AdtPlugin.log(ex, "Error launching browser for URL: %1$s", url);
            }
        }
    };

    @Override
    public void setFocus() {
        mForm.setFocus();
    }

    @Override
    public void dispose() {
        if (mToolkit != null) {
            mToolkit.dispose();
            mToolkit = null;
        }

        super.dispose();
    }
}
