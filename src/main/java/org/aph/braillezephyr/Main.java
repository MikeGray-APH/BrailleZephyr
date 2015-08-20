/* Copyright (C) 2015 American Printing House for the Blind Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aph.braillezephyr;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 *
 * @author Mike Gray mgray@aph.org
 *
 */
public class Main
{
	static Display display;
	static Shell shell;
	static BZStyledText bzStyledText;

	public static void main(String args[])
	{
		display = new Display();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setText("BrailleZephyr");
		shell.setSize(1000, 600);

		bzStyledText = new BZStyledText(shell);
		new BZMenu(shell);

		shell.open();
		while(!shell.isDisposed())
		{
			if(!display.readAndDispatch())
				display.sleep();
		}
	}
}

class ConfirmDialog implements SelectionListener, KeyListener
{
	private final Shell shell;
	private final Button okButton;
	private final Button cancelButton;

	private boolean confirm;

	public ConfirmDialog(String title, String tag)
	{
		shell = new Shell(Main.shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.SHEET);
		shell.setText(title);
		shell.setLayout(new GridLayout(1, true));

		Label label = new Label(shell, SWT.CENTER);
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		label.setText(tag);

		Composite composite = new Composite(shell, 0);
		composite.setLayout(new GridLayout(2, true));
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));

		okButton = new Button(composite, SWT.PUSH);
		okButton.setText("OK");
		okButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		okButton.addSelectionListener(this);

		cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setText("Cancel");
		cancelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		cancelButton.addSelectionListener(this);
	}

	public boolean show()
	{
		shell.pack();
		shell.open();
		while(!shell.isDisposed())
		{
			if(!Main.display.readAndDispatch())
				Main.display.sleep();
		}
		return confirm;
	}

	@Override
	public void widgetSelected(SelectionEvent event)
	{
		if(event.widget == okButton)
			confirm = true;
		shell.dispose();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent event){}

	@Override
	public void keyPressed(KeyEvent event)
	{
		if(event.keyCode == '\r' || event.keyCode == '\n')
		{
			confirm = true;
			shell.dispose();
		}
	}

	@Override
	public void keyReleased(KeyEvent event){}
}
