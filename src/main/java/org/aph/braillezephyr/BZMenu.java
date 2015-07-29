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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import java.io.*;

public class BZMenu
{
	private Menu menuBar;
	private Shell shell;
	private String fileName;

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
		item.setText("&File");
		item.setMenu(menu);

		item = new MenuItem(menu, SWT.PUSH);
		item.setText("New");
		item.addSelectionListener(new FileNewListener());

		item = new MenuItem(menu, SWT.PUSH);
		item.setText("Open");
		item.setAccelerator(SWT.CONTROL | 'O');
		item.addSelectionListener(new FileOpenListener());

		item = new MenuItem(menu, SWT.PUSH);
		item.setText("Save As");
		item.setAccelerator(SWT.SHIFT | SWT.CONTROL | 'S');
		item.addSelectionListener(new FileSaveAsListener());

		//   edit menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Edit");
		item.setMenu(menu);

		//   view menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&View");
		item.setMenu(menu);

		item = new MenuItem(menu, SWT.PUSH);
		item.setText("Font");
		item.addSelectionListener(new ViewFontListener());

		//   format menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("F&ormat");
		item.setMenu(menu);

		item = new MenuItem(menu, SWT.PUSH);
		item.setText("Lines Per Page");
		item.addSelectionListener(new LinesPerPageListener(shell));

		//   help menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Help");
		item.setMenu(menu);
	}

	private class FileNewListener extends SelectionAdapter
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			Main.bzStyledText.setText("");
			fileName = null;
			shell.setText("BrailleZephyr");
		}

	}

	private class FileOpenListener extends SelectionAdapter
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			FileDialog dialog = new FileDialog(shell, SWT.OPEN);
			fileName = dialog.open();
			if(fileName == null)
				return;

			try
			{
				FileReader fileReader = new FileReader(fileName);
				Main.bzStyledText.setText(fileReader);
				fileReader.close();
				shell.setText(new File(fileName).getName() + " - BrailleZephyr");
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

	private class FileSaveAsListener extends SelectionAdapter
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			FileDialog dialog = new FileDialog(shell, SWT.OPEN);
			dialog.setFileName(fileName);
			fileName = dialog.open();
			if(fileName == null)
				return;

			try
			{
				FileWriter fileWriter = new FileWriter(fileName);
				Main.bzStyledText.getText(fileWriter);
				fileWriter.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private class ViewFontListener extends SelectionAdapter
	{
		@Override
		public void widgetSelected(SelectionEvent e)
		{
			FontDialog dialog = new FontDialog(shell, SWT.OPEN);
			dialog.setFontList(Main.bzStyledText.getFont().getFontData());
			FontData fontData = dialog.open();
			if(fontData == null)
				return;
			Main.bzStyledText.setFont(new Font(shell.getDisplay(), fontData));
		}
	}

	private class LinesPerPageListener extends SelectionAdapter
	{
		private Shell parent;

		LinesPerPageListener(Shell parent)
		{
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent e)
		{
			new LinesPerPageDialog(parent);
		}
	}

	private class LinesPerPageDialog extends SelectionAdapter
	{
		private Shell shell;
		private Button okButton, cancelButton;
		private Spinner spinner;

		public LinesPerPageDialog(Shell parent)
		{
			shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			shell.setText("Lines per Page");
			shell.setLayout(new GridLayout(3, true));

			spinner = new Spinner(shell, 0);
			spinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			spinner.setValues(Main.bzStyledText.getLinesPerPage(), 0, 225, 0, 1, 10);

			okButton = new Button(shell, SWT.PUSH);
			okButton.setText("OK");
			okButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			okButton.addSelectionListener(this);

			cancelButton = new Button(shell, SWT.PUSH);
			cancelButton.setText("Cancel");
			cancelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			cancelButton.addSelectionListener(this);

			shell.pack();
			shell.open();
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.getSource() == okButton)
			{
				Main.bzStyledText.setLinesPerPage(spinner.getSelection());
				Main.bzStyledText.redraw();
			}
			shell.dispose();
		}
	}
}
