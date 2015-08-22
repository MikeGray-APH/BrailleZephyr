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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class BZSettings
{
	private final Shell shell;
	private final BZStyledText bzStyledText;
	private final File file;

	private Point shellSize;

	@SuppressWarnings("SameParameterValue")
	public BZSettings(BZStyledText bzStyledText, String fileName)
	{
		this.bzStyledText = bzStyledText;
		shell = bzStyledText.getShell();

		if(fileName == null)
			fileName = System.getProperty("user.home") + File.separator + ".braillezephyr";
		file = new File(fileName);
		readSettings();
	}

	public BZSettings(BZStyledText bzStyledText)
	{
		this(bzStyledText, null);
	}

	public Point getShellSize()
	{
		if(shellSize == null)
			return new Point(640, 480);

		return shellSize;
	}

	private boolean readLine(String line)
	{
		if(line.length() == 0)
			return true;

		String tokens[] = line.split("[ ]");

		switch(tokens.length)
		{
		case 2:

			switch(tokens[0])
			{
			case "linesPerPage":  bzStyledText.setLinesPerPage(Integer.parseInt(tokens[1]));  break;
			case "charsPerLine":  bzStyledText.setCharsPerLine(Integer.parseInt(tokens[1]));  break;

			case "brailleText.visible":  bzStyledText.setBrailleVisible(Boolean.valueOf(tokens[1]));  break;
			case "asciiText.visible":  bzStyledText.setAsciiVisible(Boolean.valueOf(tokens[1]));  break;

			default:  return false;
			}
			break;

		case 3:

			switch(tokens[0])
			{
			case "size":  shellSize = new Point(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));  break;

			default:  return false;
			}
			break;

		case 4:

			switch(tokens[0])
			{
			case "brailleText.font":

				bzStyledText.setBrailleFont(new Font(shell.getDisplay(),
				                                     tokens[1].replace('_', ' '),
				                                     Integer.parseInt(tokens[2]),
				                                     Integer.parseInt(tokens[3])));
				break;

			case "asciiText.font":

				bzStyledText.setAsciiFont(new Font(shell.getDisplay(),
				                                   tokens[1].replace('_', ' '),
				                                   Integer.parseInt(tokens[2]),
				                                   Integer.parseInt(tokens[3])));
				break;

			default:  return false;
			}
			break;

		default:  return false;
		}

		return true;
	}

	void readSettings()
	{
		if(!file.exists())
			return;

		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(file));
			String line;
			while((line = reader.readLine()) != null)
			{
				if(!readLine(line))
					System.err.println("Unknown setting:  " + line);
			}
		}
		catch(FileNotFoundException ignored)
		{
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage("Unable to open settings file " + file.getPath());
			messageBox.open();
		}
		catch(IOException ignored)
		{
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage("Unable to read settings file " + file.getPath());
			messageBox.open();
		}
		finally
		{
			try
			{
				if(reader != null)
					reader.close();
			}
			catch(IOException ignored)
			{
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
				messageBox.setMessage("Unable to close settings file " + file.getPath());
				messageBox.open();
			}
		}
	}

	private void writeLines(PrintWriter writer)
	{
		shellSize = bzStyledText.getShellSize();
		writer.println("size " + shellSize.x + " " + shellSize.y);
		writer.println("linesPerPage " + bzStyledText.getLinesPerPage());
		writer.println("charsPerLine " + bzStyledText.getCharsPerLine());
		writer.println();

		writer.println("brailleText.visible " + bzStyledText.getBrailleVisible());
		FontData fontData = bzStyledText.getBrailleFont().getFontData()[0];
		writer.println("brailleText.font "
		               + fontData.getName().replace(' ', '_') + " "
		               + fontData.getHeight() + " "
		               + fontData.getStyle());
		writer.println();

		writer.println("asciiText.visible " + bzStyledText.getAsciiVisible());
		fontData = bzStyledText.getAsciiFont().getFontData()[0];
		writer.println("asciiText.font "
		               + fontData.getName().replace(' ', '_') + " "
		               + fontData.getHeight() + " "
		               + fontData.getStyle());
		writer.println();
	}

	void writeSettings()
	{
		try
		{
			file.createNewFile();
		}
		catch(IOException ignored)
		{
			//TODO:  if necessary?
			if(!file.exists())
			{
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
				messageBox.setMessage("Unable to create settings file");
				messageBox.open();
				return;
			}
		}

		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(file);
			writeLines(writer);
		}
		catch(FileNotFoundException ignored)
		{
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage("Unable to open settings file " + file.getPath());
			messageBox.open();
		}
		finally
		{
			if(writer != null)
				writer.close();
		}
	}
}
