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
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * <p>
 * Contains the main method.
 * </p>
 *
 * @author Mike Gray mgray@aph.org
 */
public final class Main
{
	private final Shell shell;
	private final BZStyledText bzStyledText;
	private final BZFile bzFile;
	private final BZSettings bzSettings;

	public static void main(String args[])
	{
		new Main(args);
	}

	public Main(String args[])
	{
		//   must be before display is created (on Macs at least)
		Display.setAppName("BrailleZephyr");

		Display display = Display.getDefault();

		//   needed to catch Quit (Command-Q) on Macs
		display.addListener(SWT.Close, new CloseHandler());

		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setText("BrailleZephyr");
		shell.addShellListener(new ShellHandler());

		bzStyledText = new BZStyledText(shell);
		bzFile = new BZFile(bzStyledText);
		bzSettings = new BZSettings(bzStyledText);
		new BZMenu(bzStyledText, bzFile, bzSettings);

		//   assume any argument is a file to open
		if(args.length > 0)
			bzFile.openFile(args[0]);

		shell.open();
		while(!shell.isDisposed())
		if(!display.readAndDispatch())
			display.sleep();

		display.dispose();
	}

	private boolean checkClosing()
	{
		boolean doit = true;

		//   check if text has been modified
		if(bzStyledText.getModified())
		{
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
			messageBox.setMessage("Would you like to save your changes?");
			int result = messageBox.open();
			if(result == SWT.CANCEL)
				doit = false;
			else if(result == SWT.YES)
				doit = bzFile.saveFile();
		}

		//   write settings file
		if(doit)
		if(!bzSettings.writeSettings())
		{
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
			messageBox.setMessage("Would you like to exit anyway?");
			doit = messageBox.open() == SWT.YES;
		}

		return doit;
	}

	/**
	 * <p>
	 * Needed to catch Quit (Command-Q) on Macs
	 * </p>
	 */
	private class CloseHandler implements Listener
	{
		@Override
		public void handleEvent(Event event)
		{
			event.doit = checkClosing();
		}
	}

	private class ShellHandler implements ShellListener
	{
		@Override
		public void shellClosed(ShellEvent event)
		{
			event.doit = checkClosing();
		}

		@Override
		public void shellActivated(ShellEvent ignored){}
		@Override
		public void shellDeactivated(ShellEvent ignored){}
		@Override
		public void shellDeiconified(ShellEvent ignored){}
		@Override
		public void shellIconified(ShellEvent ignored){}
	}
}
