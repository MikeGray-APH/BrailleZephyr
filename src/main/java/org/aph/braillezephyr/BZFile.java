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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class BZFile
{
	private final Shell shell;
	private final BZStyledText bzStyledText;

	private String fileName;

	public BZFile(Shell shell, BZStyledText bzStyledText)
	{
		this.shell = shell;
		this.bzStyledText = bzStyledText;
	}

	boolean newFile()
	{
		if(bzStyledText.getModified())
		{
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
			messageBox.setMessage("Would you like to save your changes?");
			int result = messageBox.open();
			if(result == SWT.CANCEL)
				return false;
			else if(result == SWT.YES)
				if(!saveFile())
					return false;
		}
		bzStyledText.setText("");
		fileName = null;
		shell.setText("BrailleZephyr");
		return true;
	}

	boolean openFile()
	{
		if(bzStyledText.getModified())
		{
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
			messageBox.setMessage("Would you like to save your changes?");
			int result = messageBox.open();
			if(result == SWT.CANCEL)
				return false;
			else if(result == SWT.YES)
				if(!saveFile())
					return false;
		}

		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setFilterExtensions(new String[]{ "*.brf", "*.bzy", "*.brf;*.bzy", "*.*" });
		dialog.setFilterNames(new String[]{ "Braille Ready Format File", "BrailleZephyr File", "Braille Files", "All Files" });
		dialog.setFilterIndex(2);
		String fileName = dialog.open();
		if(fileName == null)
			return false;

		try
		{
			FileReader fileReader = new FileReader(fileName);
			if(fileName.endsWith("bzy"))
				bzStyledText.readBZY(fileReader);
			else
				bzStyledText.readBRF(fileReader);
			fileReader.close();
			shell.setText(new File(fileName).getName() + " - BrailleZephyr");
			this.fileName = fileName;
			return true;
		}
		catch(FileNotFoundException ignored)
		{
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage(fileName + " not found");
			messageBox.open();
		}
		catch(IOException ignored)
		{
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage("Error opening " + fileName);
			messageBox.open();
		}

		return false;
	}

	boolean saveFile()
	{
		String fileName;

		if(this.fileName == null)
		{
			FileDialog dialog = new FileDialog(shell, SWT.SAVE);
			dialog.setFileName(this.fileName);
			dialog.setFilterExtensions(new String[]{ "*.brf", "*.bzy", "*.brf;*.bzy", "*.*" });
			dialog.setFilterNames(new String[]{ "Braille Ready Format File", "BrailleZephyr File", "Braille Files", "All Files" });
			dialog.setFilterIndex(2);
			fileName = dialog.open();
			if(fileName == null)
				return false;
		}
		else
			fileName = this.fileName;

		OutputStreamWriter writer;
		try
		{
			if(fileName.endsWith("brf"))
			{
				writer = new OutputStreamWriter(new FileOutputStream(fileName), Charset.forName("US-ASCII"));
				bzStyledText.writeBRF(writer);
			}
			else if(fileName.endsWith("bzy"))
			{
				writer = new OutputStreamWriter(new FileOutputStream(fileName));
				bzStyledText.writeBZY(writer);
			}
			else
			{
				writer = new OutputStreamWriter(new FileOutputStream(fileName));
				bzStyledText.writeBRF(writer);
			}
			writer.close();
			shell.setText(new File(fileName).getName() + " - BrailleZephyr");
			this.fileName = fileName;
			return true;
		}
		catch(FileNotFoundException ignored)
		{
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage(fileName + " not found");
			messageBox.open();
			this.fileName = null;
		}
		catch(IOException ignored)
		{
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage("Error opening " + fileName);
			messageBox.open();
			this.fileName = null;
		}

		return false;
	}

	boolean saveAsFile()
	{
		String fileName = this.fileName;
		this.fileName = null;
		if(saveFile())
			return true;
		this.fileName = fileName;
		return false;
	}
}
