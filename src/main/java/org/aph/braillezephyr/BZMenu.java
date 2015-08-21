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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

public class BZMenu
{
	private final Shell shell;
	private final BZFile bzFile;
	private final BZStyledText bzStyledText;

	BZMenu(Shell shell, BZFile bzFile, BZStyledText bzStyledText)
	{
		this.shell = shell;
		this.bzFile = bzFile;
		this.bzStyledText = bzStyledText;

		Menu menuBar = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menuBar);

		Menu menu;
		MenuItem item;

		//   file menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&File");
		item.setMenu(menu);

		new NewHandler().addMenuItemTo(menu, "&New");
		new OpenHandler().addMenuItemTo(menu, "&Open\tCtrl+O", SWT.MOD1 | 'o');
		new SaveHandler().addMenuItemTo(menu, "&Save\tCtrl+S", SWT.MOD1 | 's');
		new SaveAsHandler().addMenuItemTo(menu, "Save As\tCtrl+Shift+O", SWT.MOD1 | SWT.MOD2 | 's');

		//   edit menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Edit");
		item.setMenu(menu);

		new UndoHandler().addMenuItemTo(menu, "Undo\tCtrl+Z", SWT.MOD1 | 'z');
		new RedoHandler().addMenuItemTo(menu, "Redo\tCtrl+Z", SWT.MOD1 | SWT.MOD2 | 'z');

		//   view menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&View");
		item.setMenu(menu);

		new VisibleHandler(menu);
		new BrailleFontHandler().addMenuItemTo(menu, "Braille Font");
		new AsciiFontHandler().addMenuItemTo(menu, "ASCII Font");

		//   format menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("F&ormat");
		item.setMenu(menu);

		new LinesPerPageHandler(shell).addMenuItemTo(menu, "Lines Per Page");
		new CharsPerLineHandler(shell).addMenuItemTo(menu, "Chars Per Line");
		new BellLineMarginHandler(shell).addMenuItemTo(menu, "Bell Margin", bzStyledText.getBellLineMargin() != -1);
		new BellPageMarginHandler(shell).addMenuItemTo(menu, "Bell Page", bzStyledText.getBellPageMargin() != -1);
		new RewrapFromCursorHandler().addMenuItemTo(menu, "Rewrap From Cursor\tCtrl+F", SWT.MOD1 | 'F');

		//   help menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Help");
		item.setMenu(menu);
	}

	private class NewHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			bzFile.newFile();
		}
	}

	private class OpenHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			bzFile.openFile();
		}
	}

	private class SaveHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			bzFile.saveFile();
		}
	}

	private class SaveAsHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			bzFile.saveAsFile();
		}
	}

	private class UndoHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			bzStyledText.undo();
		}
	}

	private class RedoHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			bzStyledText.redo();
		}
	}

	private class VisibleHandler extends SelectionAdapter
	{
		private final MenuItem braille;
		private final MenuItem ascii;

		private VisibleHandler(Menu menu)
		{
			braille = new MenuItem(menu, SWT.PUSH);
			braille.setText("Hide Braille");
			braille.addSelectionListener(this);

			ascii = new MenuItem(menu, SWT.PUSH);
			ascii.setText("Hide ASCII");
			ascii.addSelectionListener(this);
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == braille)
			{
				if(bzStyledText.getBrailleVisible())
				{
					bzStyledText.setBrailleVisible(false);
					braille.setText("Show Braille");
//					if(!bzStyledText.getAsciiVisible())
//					{
//						bzStyledText.setAsciiVisible(true);
//						ascii.setText("Hide ASCII");
//					}
				}
				else
				{
					bzStyledText.setBrailleVisible(true);
					braille.setText("Hide Braille");
				}
			}
			else
			{
				if(bzStyledText.getAsciiVisible())
				{
					bzStyledText.setAsciiVisible(false);
					ascii.setText("Show ASCII");
//					if(!bzStyledText.getBrailleVisible())
//					{
//						bzStyledText.setBrailleVisible(true);
//						braille.setText("Hide Braille");
//					}
				}
				else
				{
					bzStyledText.setAsciiVisible(true);
					ascii.setText("Hide ASCII");
				}
			}
		}
	}

	private class BrailleFontHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent e)
		{
			FontDialog dialog = new FontDialog(shell, SWT.OPEN);
			dialog.setFontList(bzStyledText.getBrailleFont().getFontData());
			FontData fontData = dialog.open();
			if(fontData == null)
				return;
			bzStyledText.setBrailleFont(new Font(shell.getDisplay(), fontData));
		}
	}

	private class AsciiFontHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent e)
		{
			FontDialog dialog = new FontDialog(shell, SWT.OPEN);
			dialog.setFontList(bzStyledText.getAsciiFont().getFontData());
			FontData fontData = dialog.open();
			if(fontData == null)
				return;
			bzStyledText.setAsciiFont(new Font(shell.getDisplay(), fontData));
		}
	}

	private class LinesPerPageHandler extends AbstractAction
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

	private class LinesPerPageDialog implements SelectionListener, KeyListener
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
			spinner.setValues(bzStyledText.getLinesPerPage(), 0, 225, 0, 1, 10);
			spinner.addKeyListener(this);

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

		private void setLinesPerPage()
		{
			bzStyledText.setLinesPerPage(spinner.getSelection());
			bzStyledText.redraw();
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == okButton)
				setLinesPerPage();
			shell.dispose();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent event){}

		@Override
		public void keyPressed(KeyEvent event)
		{
			if(event.keyCode == '\r' || event.keyCode == '\n')
			{
				setLinesPerPage();
				shell.dispose();
			}
		}

		@Override
		public void keyReleased(KeyEvent event){}
	}

	private class CharsPerLineHandler extends AbstractAction
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

	private class CharsPerLineDialog implements SelectionListener, KeyListener
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
			spinner.setValues(bzStyledText.getCharsPerLine(), 0, 27720, 0, 1, 10);
			spinner.addKeyListener(this);

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

		private void setCharsPerLine()
		{
			bzStyledText.setCharsPerLine(spinner.getSelection());
			bzStyledText.redraw();
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == okButton)
				setCharsPerLine();
			shell.dispose();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent event){}

		@Override
		public void keyPressed(KeyEvent event)
		{
			//TODO:  is the \r necessary?
			if(event.keyCode == '\r' || event.keyCode == '\n')
			{
				setCharsPerLine();
				shell.dispose();
			}
		}

		@Override
		public void keyReleased(KeyEvent event){}
	}

	private class BellLineMarginHandler extends AbstractAction
	{
		private final Shell parent;

		private BellLineMarginHandler(Shell parent)
		{
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent e)
		{
			new BellLineMarginDialog(parent);
		}
	}

	private class BellLineMarginDialog implements SelectionListener, KeyListener
	{
		private final Shell shell;
		private final Button okButton;
		private final Button cancelButton;
		private final Spinner spinner;

		public BellLineMarginDialog(Shell parent)
		{
			shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			shell.setText("Bell Margin");
			shell.setLayout(new GridLayout(3, true));

			spinner = new Spinner(shell, 0);
			spinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			spinner.setValues(bzStyledText.getBellLineMargin(), 0, 27720, 0, 1, 10);
			spinner.addKeyListener(this);

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

		private void setBellLineMargin()
		{
			bzStyledText.setBellLineMargin(spinner.getSelection());
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == okButton)
				setBellLineMargin();
			shell.dispose();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent event){}

		@Override
		public void keyPressed(KeyEvent event)
		{
			if(event.keyCode == '\r' || event.keyCode == '\n')
			{
				setBellLineMargin();
				shell.dispose();
			}
		}

		@Override
		public void keyReleased(KeyEvent event){}
	}

	private class BellPageMarginHandler extends AbstractAction
	{
		private final Shell parent;

		private BellPageMarginHandler(Shell parent)
		{
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent e)
		{
			new BellPageMarginDialog(parent);
		}
	}

	private class BellPageMarginDialog implements SelectionListener, KeyListener
	{
		private final Shell shell;
		private final Button okButton;
		private final Button cancelButton;
		private final Spinner spinner;

		public BellPageMarginDialog(Shell parent)
		{
			shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			shell.setText("Bell Page");
			shell.setLayout(new GridLayout(3, true));

			spinner = new Spinner(shell, 0);
			spinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			spinner.setValues(bzStyledText.getBellPageMargin(), 0, 27720, 0, 1, 10);
			spinner.addKeyListener(this);

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

		private void setBellPageMargin()
		{
			bzStyledText.setBellPageMargin(spinner.getSelection());
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == okButton)
				setBellPageMargin();
			shell.dispose();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent event){}

		@Override
		public void keyPressed(KeyEvent event)
		{
			if(event.keyCode == '\r' || event.keyCode == '\n')
			{
				setBellPageMargin();
				shell.dispose();
			}
		}

		@Override
		public void keyReleased(KeyEvent event){}
	}

	private class RewrapFromCursorHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent e)
		{
			bzStyledText.rewrapFromCaret();
		}
	}
}

abstract class AbstractAction implements SelectionListener
{
	@SuppressWarnings("WeakerAccess")
	MenuItem addMenuItemTo(Menu menu,
	                       String tag,
	                       int accelerator,
	                       boolean enabled)
	{
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(tag);
		if(accelerator != 0)
			item.setAccelerator(accelerator);
		item.addSelectionListener(this);
		item.setEnabled(enabled);
		return item;
	}

	MenuItem addMenuItemTo(Menu menu, String tag, int accelerator)
	{
		return addMenuItemTo(menu, tag, accelerator, true);
	}

	MenuItem addMenuItemTo(Menu menu, String tag, boolean enabled)
	{
		return addMenuItemTo(menu, tag, 0, enabled);
	}

	MenuItem addMenuItemTo(Menu menu, String tag)
	{
		return addMenuItemTo(menu, tag, 0, true);
	}

	@Override
	public void widgetSelected(SelectionEvent event){}

	@Override
	public void widgetDefaultSelected(SelectionEvent event){}
}
