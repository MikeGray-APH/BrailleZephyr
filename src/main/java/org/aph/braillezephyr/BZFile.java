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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/**
 * <p>
 * This class handles the file operations for BZStyledText.
 * </p>
 *
 * @author Mike Gray mgray@aph.org
 */
public final class BZFile extends BZBase
{
	private String fileName;

	/**
	 * <p>
	 * Creates a new <code>BZFile</code> object.
	 * </p>
	 *
	 * @param bzStyledText the bzStyledText object to operate on (cannot be null)
	 */
	public BZFile(BZStyledText bzStyledText)
	{
		super(bzStyledText);
	}

	boolean newFile()
	{
		//   check if text has been modified
		if(bzStyledText.getModified())
		{
			MessageBox messageBox = new MessageBox(parentShell, SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
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
		parentShell.setText("BrailleZephyr");
		return true;
	}

	boolean openFile()
	{
		//   check if text has been modified
		if(bzStyledText.getModified())
		{
			MessageBox messageBox = new MessageBox(parentShell, SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
			messageBox.setMessage("Would you like to save your changes?");
			int result = messageBox.open();
			if(result == SWT.CANCEL)
				return false;
			else if(result == SWT.YES)
				if(!saveFile())
					return false;
		}

		FileDialog fileDialog = new FileDialog(parentShell, SWT.OPEN);
		fileDialog.setFilterExtensions(new String[]{ "*.brf", "*.bzy", "*.brf;*.bzy", "*.*" });
		fileDialog.setFilterNames(new String[]{ "Braille Ready Format File", "BrailleZephyr File", "Braille Files", "All Files" });
		fileDialog.setFilterIndex(2);
		String fileName = fileDialog.open();
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
			parentShell.setText(new File(fileName).getName() + " - BrailleZephyr");
			this.fileName = fileName;
			return true;
		}
		catch(FileNotFoundException exception)
		{
			logError("Unable to open file", exception);
		}
		catch(IOException exception)
		{
			logError("Unable to read file", exception);
		}
		catch(BZException exception)
		{
			logError("Unable to read file", fileName + ":  " + exception.getMessage());
		}

		return false;
	}

	boolean saveFile()
	{
		String fileName;

		//   check if file name is set
		if(this.fileName == null)
		{
			FileDialog fileDialog = new FileDialog(parentShell, SWT.SAVE);
			fileDialog.setFileName(this.fileName);
			fileDialog.setFilterExtensions(new String[]{ "*.brf", "*.bzy", "*.brf;*.bzy", "*.*" });
			fileDialog.setFilterNames(new String[]{ "Braille Ready Format File", "BrailleZephyr File", "Braille Files", "All Files" });
			fileDialog.setFilterIndex(2);
			fileName = fileDialog.open();
			if(fileName == null)
				return false;
		}
		else
			fileName = this.fileName;

		try
		{
			OutputStreamWriter writer;

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
			parentShell.setText(new File(fileName).getName() + " - BrailleZephyr");
			this.fileName = fileName;
			return true;
		}
		catch(FileNotFoundException exception)
		{
			logError("Unable to open file", exception);
		}
		catch(IOException exception)
		{
			logError("Unable to write file", exception);
		}

		return false;
	}

	boolean saveAsFile()
	{
		//   set fileName to null so saveFile will ask for a new file name
		String fileName = this.fileName;
		this.fileName = null;
		if(saveFile())
			return true;

		//   saveFile didn't save, reset fileName
		this.fileName = fileName;
		return false;
	}
}
