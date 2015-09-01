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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * <p>
 * This class creates and handles the menu for BZStyledText.
 * </p>
 *
 * @author Mike Gray mgray@aph.org
 */
public final class BZMenu extends BZBase
{
	private final BZFile bzFile;

	/**
	 * <p>
	 * Creates a new <code>BZMenu</code> object.
	 * </p>
	 *
	 * @param bzStyledText the bzStyledText object to operate on (cannot be null)
	 * @param bzFile the bzFile object for file operations (cannot be null)
	 */
	public BZMenu(BZStyledText bzStyledText, BZFile bzFile)
	{
		super(bzStyledText);

		this.bzFile = bzFile;

		Menu menuBar = new Menu(parentShell, SWT.BAR);
		parentShell.setMenuBar(menuBar);

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
		new QuitHandler().addMenuItemTo(menu, "Quit\tCtrl+Q", SWT.MOD1 | 'q');
		new MenuItem(menu, SWT.SEPARATOR);
		new LoadLineMarginBellHandler().addMenuItemTo(menu, "Load Line Bell");
		new LoadPageMarginBellHandler().addMenuItemTo(menu, "Load Page Bell");

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

		new LinesPerPageHandler(parentShell).addMenuItemTo(menu, "Lines Per Page");
		new CharsPerLineHandler(parentShell).addMenuItemTo(menu, "Chars Per Line");
		new LineMarginBellHandler(parentShell).addMenuItemTo(menu, "Line Margin Bell", bzStyledText.getLineMarginBell() != -1);
		new PageMarginBellHandler(parentShell).addMenuItemTo(menu, "Page Margin Bell", bzStyledText.getPageMarginBell() != -1);
		new RewrapFromCursorHandler().addMenuItemTo(menu, "Rewrap From Cursor\tCtrl+F", SWT.MOD1 | 'F');

		//   help menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Help");
		item.setMenu(menu);

		new AboutHandler(parentShell).addMenuItemTo(menu, "About");
		//TODO:  hide on release version
		new LogViewerHandler(parentShell).addMenuItemTo(menu, "View Log");
	}

	private class NewHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			bzFile.newFile();
		}
	}

	private class OpenHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			bzFile.openFile();
		}
	}

	private class SaveHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			bzFile.saveFile();
		}
	}

	private class SaveAsHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			bzFile.saveAsFile();
		}
	}

	private class QuitHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			parentShell.close();
		}
	}

	private class LoadLineMarginBellHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			FileDialog fileDialog = new FileDialog(parentShell, SWT.OPEN);
			fileDialog.setFileName(bzStyledText.getLineMarginFileName());
			String fileName = fileDialog.open();
			if(fileName == null)
				return;
			try
			{
				bzStyledText.loadLineMarginFileName(fileName);
			}
			catch(FileNotFoundException exception)
			{
				logError("Unable to open file:  " + fileName + " -- " + exception.getMessage());
			}
			catch(IOException exception)
			{
				logError("Unable to read file:  " + fileName + " -- " + exception.getMessage());
			}
			catch(UnsupportedAudioFileException ignore)
			{
				logError("Sound file unsupported for line margin bell:  " + fileName);
			}
			catch(LineUnavailableException ignore)
			{
				logError("Line unavailable for line margin bell:  " + fileName);
			}
		}
	}

	private class LoadPageMarginBellHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			FileDialog fileDialog = new FileDialog(parentShell, SWT.OPEN);
			fileDialog.setFileName(bzStyledText.getPageMarginFileName());
			String fileName = fileDialog.open();
			if(fileName == null)
				return;
			try
			{
				bzStyledText.loadPageMarginFileName(fileName);
			}
			catch(FileNotFoundException exception)
			{
				logError("Unable to open file:  " + fileName + " -- " + exception.getMessage());
			}
			catch(IOException exception)
			{
				logError("Unable to read file:  " + fileName + " -- " + exception.getMessage());
			}
			catch(UnsupportedAudioFileException ignore)
			{
				logError("Sound file unsupported for page margin bell:  " + fileName);
			}
			catch(LineUnavailableException ignore)
			{
				logError("Line unavailable for page margin bell:  " + fileName);
			}
		}
	}

	private class UndoHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			bzStyledText.undo();
		}
	}

	private class RedoHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			bzStyledText.redo();
		}
	}

	private final class VisibleHandler extends SelectionAdapter
	{
		private final MenuItem brailleItem;
		private final MenuItem asciiItem;

		private VisibleHandler(Menu menu)
		{
			brailleItem = new MenuItem(menu, SWT.PUSH);
			if(bzStyledText.getBrailleVisible())
				brailleItem.setText("Hide Braille");
			else
				brailleItem.setText("Show Braille");
			brailleItem.addSelectionListener(this);

			asciiItem = new MenuItem(menu, SWT.PUSH);
			if(bzStyledText.getAsciiVisible())
				asciiItem.setText("Hide ASCII");
			else
				asciiItem.setText("Show ASCII");
			asciiItem.addSelectionListener(this);
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == brailleItem)
			{
				if(bzStyledText.getBrailleVisible())
				{
					bzStyledText.setBrailleVisible(false);
					brailleItem.setText("Show Braille");
//					if(!bzStyledText.getAsciiVisible())
//					{
//						bzStyledText.setAsciiVisible(true);
//						ascii.setText("Hide ASCII");
//					}
				}
				else
				{
					bzStyledText.setBrailleVisible(true);
					brailleItem.setText("Hide Braille");
				}
			}
			else
			{
				if(bzStyledText.getAsciiVisible())
				{
					bzStyledText.setAsciiVisible(false);
					asciiItem.setText("Show ASCII");
//					if(!bzStyledText.getBrailleVisible())
//					{
//						bzStyledText.setBrailleVisible(true);
//						braille.setText("Hide Braille");
//					}
				}
				else
				{
					bzStyledText.setAsciiVisible(true);
					asciiItem.setText("Hide ASCII");
				}
			}
		}
	}

	private class BrailleFontHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			FontDialog fontDialog = new FontDialog(parentShell, SWT.OPEN);
			fontDialog.setFontList(bzStyledText.getBrailleFont().getFontData());
			FontData fontData = fontDialog.open();
			if(fontData == null)
				return;
			bzStyledText.setBrailleFont(new Font(parentShell.getDisplay(), fontData));
		}
	}

	private class AsciiFontHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			FontDialog fontDialog = new FontDialog(parentShell, SWT.OPEN);
			fontDialog.setFontList(bzStyledText.getAsciiFont().getFontData());
			FontData fontData = fontDialog.open();
			if(fontData == null)
				return;
			bzStyledText.setAsciiFont(new Font(parentShell.getDisplay(), fontData));
		}
	}

	private final class LinesPerPageHandler extends AbstractAction
	{
		private final Shell parentShell;

		private LinesPerPageHandler(Shell parentShell)
		{
			this.parentShell = parentShell;
		}

		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			new LinesPerPageDialog(parentShell);
		}
	}

	private final class LinesPerPageDialog implements SelectionListener, KeyListener
	{
		private final Shell shell;
		private final Button okButton;
		private final Button cancelButton;
		private final Spinner spinner;

		private LinesPerPageDialog(Shell parentShell)
		{
			shell = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
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
		public void widgetDefaultSelected(SelectionEvent ignored){}

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
		public void keyReleased(KeyEvent ignored){}
	}

	private final class CharsPerLineHandler extends AbstractAction
	{
		private final Shell parentShell;

		private CharsPerLineHandler(Shell parentShell)
		{
			this.parentShell = parentShell;
		}

		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			new CharsPerLineDialog(parentShell);
		}
	}

	private final class CharsPerLineDialog implements SelectionListener, KeyListener
	{
		private final Shell shell;
		private final Button okButton;
		private final Button cancelButton;
		private final Spinner spinner;

		private CharsPerLineDialog(Shell parentShell)
		{
			shell = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
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
		public void widgetDefaultSelected(SelectionEvent ignored){}

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
		public void keyReleased(KeyEvent ignored){}
	}

	private final class LineMarginBellHandler extends AbstractAction
	{
		private final Shell parentShell;

		private LineMarginBellHandler(Shell parentShell)
		{
			this.parentShell = parentShell;
		}

		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			new LineMarginBellDialog(parentShell);
		}
	}

	private final class LineMarginBellDialog implements SelectionListener, KeyListener
	{
		private final Shell shell;
		private final Button okButton;
		private final Button cancelButton;
		private final Spinner spinner;

		private LineMarginBellDialog(Shell parentShell)
		{
			shell = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			shell.setText("Bell Margin");
			shell.setLayout(new GridLayout(3, true));

			spinner = new Spinner(shell, 0);
			spinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			spinner.setValues(bzStyledText.getLineMarginBell(), 0, 27720, 0, 1, 10);
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
			bzStyledText.setLineMarginBell(spinner.getSelection());
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == okButton)
				setBellLineMargin();
			shell.dispose();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent ignored){}

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
		public void keyReleased(KeyEvent ignored){}
	}

	private final class PageMarginBellHandler extends AbstractAction
	{
		private final Shell parentShell;

		private PageMarginBellHandler(Shell parentShell)
		{
			this.parentShell = parentShell;
		}

		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			new PageMarginBellDialog(parentShell);
		}
	}

	private final class PageMarginBellDialog implements SelectionListener, KeyListener
	{
		private final Shell shell;
		private final Button okButton;
		private final Button cancelButton;
		private final Spinner spinner;

		private PageMarginBellDialog(Shell parentShell)
		{
			shell = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			shell.setText("Bell Page");
			shell.setLayout(new GridLayout(3, true));

			spinner = new Spinner(shell, 0);
			spinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			spinner.setValues(bzStyledText.getPageMarginBell(), 0, 27720, 0, 1, 10);
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
			bzStyledText.setPageMarginBell(spinner.getSelection());
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == okButton)
				setBellPageMargin();
			shell.dispose();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent ignored){}

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
		public void keyReleased(KeyEvent ignored){}
	}

	private class RewrapFromCursorHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			bzStyledText.rewrapFromCaret();
		}
	}

	private static final class AboutHandler extends AbstractAction
	{
		private final Shell parentShell;

		private AboutHandler(Shell parentShell)
		{
			this.parentShell = parentShell;
		}

		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			MessageBox messageBox = new MessageBox(parentShell, SWT.ICON_INFORMATION | SWT.OK);
			String version = getClass().getPackage().getImplementationVersion();
			if(version == null)
				version = "development";
			messageBox.setMessage("BrailleZephyr\nVersion " + version);
			messageBox.open();
		}
	}

	private final class LogViewerHandler extends AbstractAction
	{
		private final Shell parentShell;

		private LogViewerHandler(Shell parentShell)
		{
			this.parentShell = parentShell;
		}

		@Override
		public void widgetSelected(SelectionEvent ignored)
		{
			new LogViewerDialog(parentShell);
		}
	}

	private final class LogViewerDialog implements SelectionListener
	{
		private final Shell parentShell;

		private LogViewerDialog(Shell parentShell)
		{
			this.parentShell = parentShell;

			Shell dialog = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
			dialog.setLayout(new FillLayout());
			dialog.setText("Log Messages");

			Text text = new Text(dialog, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
			text.setText(bzStyledText.getLogString());

			dialog.open();
			while(!dialog.isDisposed())
				if(!dialog.getDisplay().readAndDispatch())
					dialog.getDisplay().sleep();
		}

		@Override
		public void widgetSelected(SelectionEvent ignored)
		{

		}

		@Override
		public void widgetDefaultSelected(SelectionEvent ignored){}
	}

	private abstract static class AbstractAction implements SelectionListener
	{
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
		public void widgetSelected(SelectionEvent ignored){}

		@Override
		public void widgetDefaultSelected(SelectionEvent ignored){}
	}
}
