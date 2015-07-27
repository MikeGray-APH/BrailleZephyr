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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import java.io.*;

public class BZMenu
{
	private Menu menuBar;
	private Shell shell;

	BZMenu(Shell shell)
	{
		this.shell = shell;

		menuBar = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menuBar);

		Menu menu;
		MenuItem item;

		//   file menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("file");
		item.setMenu(menu);

		item = new MenuItem(menu, SWT.PUSH);
		item.setText("open");
		item.addSelectionListener(new FileOpen());

		//   edit menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("edit");
		item.setMenu(menu);

		//   view menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("view");
		item.setMenu(menu);

		item = new MenuItem(menu, SWT.PUSH);
		item.setText("font");
		item.addSelectionListener(new ViewFont());

		//   help menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("help");
		item.setMenu(menu);
	}

	private class FileOpen extends SelectionAdapter
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			FileDialog dialog = new FileDialog(shell, SWT.OPEN);
			String fileName = dialog.open();
			if(fileName == null)
				return;

			try
			{
				FileReader fileReader = new FileReader(fileName);
				Main.bzStyledText.setText(fileReader);
			}
			catch(FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private class ViewFont extends SelectionAdapter
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			FontDialog dialog = new FontDialog(shell, SWT.OPEN);
			dialog.setFontList(Main.bzStyledText.getFont().getFontData());
			FontData fontData = dialog.open();
			if(fontData == null)
				return;
			Main.bzStyledText.setFont(new Font(shell.getDisplay(), fontData));
		}
	}
}
