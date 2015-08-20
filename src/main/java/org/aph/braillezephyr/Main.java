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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 *
 * @author Mike Gray mgray@aph.org
 *
 */
public class Main
{
	@SuppressWarnings("WeakerAccess")
	static Display display;
	static Shell shell;
	static BZStyledText bzStyledText;
	static BZFile bzFile;

	//TODO:  move file stuff to own class

	public static void main(String args[])
	{
		display = new Display();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setText("BrailleZephyr");
		shell.setSize(1000, 600);
		shell.addListener(SWT.Close, new CloseHandler());

		bzStyledText = new BZStyledText(shell);
		bzFile = new BZFile();
		new BZMenu(shell);

		shell.open();
		while(!shell.isDisposed())
		{
			if(!display.readAndDispatch())
				display.sleep();
		}
	}

	private static class CloseHandler implements Listener
	{
		@Override
		public void handleEvent(Event event)
		{
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
		}
	}
}
