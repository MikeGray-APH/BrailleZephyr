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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class BZMenu
{
	private final Shell shell;
	private String fileName;

	BZMenu(Shell shell)
	{
		this.shell = shell;

		Menu menuBar = new Menu(shell, SWT.BAR);
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
		item.addSelectionListener(new FileNewHandler());

		item = new MenuItem(menu, SWT.PUSH);
		item.setText("Open");
		item.setAccelerator(SWT.CONTROL | 'O');
		item.addSelectionListener(new FileOpenHandler());

		item = new MenuItem(menu, SWT.PUSH);
		item.setText("Save");
		item.setAccelerator(SWT.CONTROL | 'S');
		item.addSelectionListener(new FileSaveHandler());

		item = new MenuItem(menu, SWT.PUSH);
		item.setText("Save As");
		item.setAccelerator(SWT.SHIFT | SWT.CONTROL | 'S');
		item.addSelectionListener(new FileSaveAsHandler());

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
		item.addSelectionListener(new ViewFontHandler());

		//   format menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("F&ormat");
		item.setMenu(menu);

		item = new MenuItem(menu, SWT.PUSH);
		item.setText("Lines Per Page");
		item.addSelectionListener(new LinesPerPageHandler(shell));

		item = new MenuItem(menu, SWT.PUSH);
		item.setText("Chars Per Line");
		item.addSelectionListener(new CharsPerLineHandler(shell));

		//   help menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Help");
		item.setMenu(menu);
	}

	private class FileNewHandler extends SelectionAdapter
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			Main.bzStyledText.setText("");
			fileName = null;
			shell.setText("BrailleZephyr");
		}

	}

	private class FileOpenHandler extends SelectionAdapter
	{
		@SuppressWarnings("CallToPrintStackTrace")
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			FileDialog dialog = new FileDialog(shell, SWT.OPEN);
			dialog.setFilterExtensions(new String[]{"*.brf", "*.bzy", "*.*"});
			dialog.setFilterNames(new String[]{"Braille Ready Format File", "BrailleZephyr File", "All Files"});
			dialog.setFilterIndex(2);
			String _fileName = dialog.open();
			if(_fileName == null)
				return;

			try
			{
				FileReader fileReader = new FileReader(_fileName);
				if(_fileName.endsWith("bzy"))
					Main.bzStyledText.readBZY(fileReader);
				else
					Main.bzStyledText.readBRF(fileReader);
				fileReader.close();
				shell.setText(new File(_fileName).getName() + " - BrailleZephyr");
			}
			catch(IOException e)
			{
				//TODO:  do something when this happens (everywhere)
				e.printStackTrace();
			}
			finally
			{
				fileName = _fileName;
			}
		}
	}

	private class FileSaveHandler extends SelectionAdapter
	{
		@SuppressWarnings("CallToPrintStackTrace")
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(fileName == null)
			{
				//   save as handler never uses event object so just pass it this one
				new FileSaveAsHandler().widgetSelected(event);
				return;
			}
			try
			{
				OutputStreamWriter writer;
				if(fileName.endsWith("brf"))
				{
					writer = new OutputStreamWriter(new FileOutputStream(fileName), Charset.forName("US-ASCII"));
					Main.bzStyledText.writeBRF(writer);
				}
				else if(fileName.endsWith("bzy"))
				{
					writer = new OutputStreamWriter(new FileOutputStream(fileName));
					Main.bzStyledText.writeBZY(writer);
				}
				else
				{
					writer = new OutputStreamWriter(new FileOutputStream(fileName));
					Main.bzStyledText.writeBRF(writer);
				}
				writer.close();
				shell.setText(new File(fileName).getName() + " - BrailleZephyr");
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private class FileSaveAsHandler extends SelectionAdapter
	{
		@SuppressWarnings("CallToPrintStackTrace")
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			FileDialog dialog = new FileDialog(shell, SWT.OPEN);
			dialog.setFileName(fileName);
			dialog.setFilterExtensions(new String[]{"*.brf", "*.bzy", "*.*"});
			dialog.setFilterNames(new String[]{"Braille Ready Format File", "BrailleZephyr File", "All Files"});
			dialog.setFilterIndex(2);
			String _fileName = dialog.open();
			if(_fileName == null)
				return;

			try
			{
				OutputStreamWriter writer;
				if(_fileName.endsWith("brf"))
				{
					writer = new OutputStreamWriter(new FileOutputStream(_fileName), Charset.forName("US-ASCII"));
					Main.bzStyledText.writeBRF(writer);
				}
				else if(_fileName.endsWith("bzy"))
				{
					writer = new OutputStreamWriter(new FileOutputStream(_fileName));
					Main.bzStyledText.writeBZY(writer);
				}
				else
				{
					writer = new OutputStreamWriter(new FileOutputStream(_fileName));
					Main.bzStyledText.writeBRF(writer);
				}
				writer.close();
				shell.setText(new File(_fileName).getName() + " - BrailleZephyr");
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				fileName = _fileName;
			}
		}
	}

	private class ViewFontHandler extends SelectionAdapter
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

	private static class LinesPerPageHandler extends SelectionAdapter
	{
		private final Shell parent;

		private LinesPerPageHandler(Shell parent)
		{
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent e)
		{
			new LinesPerPageDialog(parent);
		}
	}

	private static class LinesPerPageDialog extends SelectionAdapter
	{
		private final Shell shell;
		private final Button okButton;
		private final Button cancelButton;
		private final Spinner spinner;

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

	private static class CharsPerLineHandler extends SelectionAdapter
	{
		private final Shell parent;

		private CharsPerLineHandler(Shell parent)
		{
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent e)
		{
			new CharsPerLineDialog(parent);
		}
	}

	private static class CharsPerLineDialog extends SelectionAdapter
	{
		private final Shell shell;
		private final Button okButton;
		private final Button cancelButton;
		private final Spinner spinner;

		public CharsPerLineDialog(Shell parent)
		{
			shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			shell.setText("Characters Per Line");
			shell.setLayout(new GridLayout(3, true));

			spinner = new Spinner(shell, 0);
			spinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			spinner.setValues(Main.bzStyledText.getCharsPerLine(), 0, 27720, 0, 1, 10);

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
				Main.bzStyledText.setCharsPerLine(spinner.getSelection());
				Main.bzStyledText.redraw();
			}
			shell.dispose();
		}
	}
}
