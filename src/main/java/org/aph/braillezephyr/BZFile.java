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

public class BZFile
{
	private String fileName;

	void newFile()
	{
		if(Main.bzStyledText.getModified())
		{
			MessageBox messageBox = new MessageBox(Main.shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
			messageBox.setMessage("Would you like to save your changes?");
			int result = messageBox.open();
			if(result == SWT.YES)
				saveFile();
			else if(result == SWT.CANCEL)
				return;
		}
		Main.bzStyledText.setText("");
		fileName = null;
		Main.shell.setText("BrailleZephyr");
	}

	void openFile()
	{
		FileDialog dialog = new FileDialog(Main.shell, SWT.OPEN);
		dialog.setFilterExtensions(new String[]{ "*.brf", "*.bzy", "*.brf;*.bzy", "*.*" });
		dialog.setFilterNames(new String[]{ "Braille Ready Format File", "BrailleZephyr File", "Braille Files", "All Files" });
		dialog.setFilterIndex(2);
		String fileName = dialog.open();
		if(fileName == null)
			return;

		try
		{
			FileReader fileReader = new FileReader(fileName);
			if(fileName.endsWith("bzy"))
				Main.bzStyledText.readBZY(fileReader);
			else
				Main.bzStyledText.readBRF(fileReader);
			fileReader.close();
			Main.shell.setText(new File(fileName).getName() + " - BrailleZephyr");
			this.fileName = fileName;
		}
		catch(FileNotFoundException ignored)
		{
			MessageBox messageBox = new MessageBox(Main.shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage(fileName + " not found");
			messageBox.open();
		}
		catch(IOException ignored)
		{
			MessageBox messageBox = new MessageBox(Main.shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage("Error opening " + fileName);
			messageBox.open();
		}
	}

	void saveFile()
	{
		String fileName;

		if(this.fileName == null)
		{
			FileDialog dialog = new FileDialog(Main.shell, SWT.SAVE);
			dialog.setFileName(this.fileName);
			dialog.setFilterExtensions(new String[]{ "*.brf", "*.bzy", "*.brf;*.bzy", "*.*" });
			dialog.setFilterNames(new String[]{ "Braille Ready Format File", "BrailleZephyr File", "Braille Files", "All Files" });
			dialog.setFilterIndex(2);
			fileName = dialog.open();
			if(fileName == null)
				return;
		}
		else
			fileName = this.fileName;

		OutputStreamWriter writer;
		try
		{
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
			Main.shell.setText(new File(fileName).getName() + " - BrailleZephyr");
			this.fileName = fileName;
		}
		catch(FileNotFoundException ignored)
		{
			MessageBox messageBox = new MessageBox(Main.shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage(fileName + " not found");
			messageBox.open();
			this.fileName = null;
		}
		catch(IOException ignored)
		{
			MessageBox messageBox = new MessageBox(Main.shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage("Error opening " + fileName);
			messageBox.open();
			this.fileName = null;
		}
	}

	void saveAsFile()
	{
		String fileName = this.fileName;
		this.fileName = null;
		saveFile();
		if(this.fileName == null)
			this.fileName = fileName;

	}
}
