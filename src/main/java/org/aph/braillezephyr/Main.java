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
public class Main
{
	private static Shell shell;
	private static BZStyledText bzStyledText;
	private static BZFile bzFile;
	private static BZSettings bzSettings;

	public static void main(String args[])
	{
		Display display = new Display();

		//   needed to catch Quit (Command-Q) on Macs
		display.addListener(SWT.Close, new CloseHandler());

		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setText("BrailleZephyr");
		shell.addShellListener(new ShellHandler());

		bzStyledText = new BZStyledText(shell);
		bzFile = new BZFile(bzStyledText);
		bzSettings = new BZSettings(bzStyledText);
		new BZMenu(bzStyledText, bzFile);

		shell.open();
		while(!shell.isDisposed())
		if(!display.readAndDispatch())
			display.sleep();
	}

	/**
	 * <p>
	 * Needed to catch Quit (Command-Q) on Macs
	 * </p>
	 */
	private static class CloseHandler implements Listener
	{
		@Override
		public void handleEvent(Event event)
		{
			//   check if text has been modified
			if(bzStyledText.getModified())
			{
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
				messageBox.setMessage("Would you like to save your changes?");
				int result = messageBox.open();
				if(result == SWT.CANCEL)
					event.doit = false;
				else if(result == SWT.YES)
					if(!bzFile.saveFile())
						event.doit = false;
			}

			//   write settings file
			if(event.doit)
				bzSettings.writeSettings();
		}
	}

	private static class ShellHandler implements ShellListener
	{
		@Override
		public void shellClosed(ShellEvent event)
		{
			//   check if text has been modified
			if(bzStyledText.getModified())
			{
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
				messageBox.setMessage("Would you like to save your changes?");
				int result = messageBox.open();
				if(result == SWT.CANCEL)
					event.doit = false;
				else if(result == SWT.YES)
					if(!bzFile.saveFile())
						event.doit = false;
			}

			//   write settings file
			if(event.doit)
				bzSettings.writeSettings();
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
