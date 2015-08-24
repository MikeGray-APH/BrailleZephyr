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
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
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

/**
 * <p>
 * This class handles access to the settings file on the system.  This allows
 * the settings to be restored between executions.
 * </p>
 *
 * @author Mike Gray mgray@aph.org
 */
public class BZSettings
{
	private final BZStyledText bzStyledText;
	private final Shell parentShell;
	private final File file;

	private Point shellSize;
	private boolean shellMaximized;

	/**
	 * <p>
	 * Creates a new <code>BZSettings</code> object for BZStyledText.
	 * </p><p>
	 * If <code>fileName</code> is null, then the default file
	 * &quot;.braillezephyr.conf&quot; in the user's home directory is tried.
	 * </p><p>
	 * When <code>useSize</code> is true, then the parent shell of the
	 * <code>bzStyledText</code> object's size will be stored and restored.
	 * This is used for when the <code>bzStyledText</code> object is or isn't
	 * a top level window.
	 * </p>
	 *
	 * @param bzStyledText the bzStyledText object to operate on (cannot be null)
	 * @param fileName the filename of the settings file
	 * @param useSize whether or not to resize the parent of bzStyledText
	 */
	public BZSettings(BZStyledText bzStyledText, String fileName, boolean useSize)
	{
		this.bzStyledText = bzStyledText;
		parentShell = bzStyledText.getParentShell();

		if(fileName == null)
			fileName = System.getProperty("user.home") + File.separator + ".braillezephyr.conf";
		file = new File(fileName);
		readSettings();

		if(useSize)
		{
			parentShell.addControlListener(new ControlHandler());
			if(shellSize == null)
				shellSize = new Point(640, 480);
			parentShell.setSize(shellSize);
			parentShell.setMaximized(shellMaximized);
		}
	}

	/**
	 * <p>
	 * Creates a new <code>BZSettings</code> object with the default
	 * <code>fileName</code>.
	 * </p>
	 *
	 * @param bzStyledText the bzStyledText to operate on (cannot be null)
	 * @param useSize whether or not to resize the parent of bzStyledText
	 *
	 * @see #BZSettings(BZStyledText, String, boolean)
	 */
	public BZSettings(BZStyledText bzStyledText, boolean useSize)
	{
		this(bzStyledText, null, useSize);
	}

	/**
	 * <p>
	 * Creates a new <code>BZSettings</code> object with resizing.
	 * </p>
	 *
	 * @param bzStyledText the bzStyledText to operate on (cannot be null)
	 * @param fileName the filename of the settings file
	 *
	 * @see #BZSettings(BZStyledText, String, boolean)
	 */
	public BZSettings(BZStyledText bzStyledText, String fileName)
	{
		this(bzStyledText, fileName, true);
	}

	/**
	 * <p>
	 * Creates a new <code>BZSettings</code> object with the default
	 * <code>fileName</code> and resizing.
	 * </p>
	 *
	 * @param bzStyledText the bzStyledText to operate on (cannot be null)
	 *
	 * @see #BZSettings(BZStyledText, String, boolean)
	 */
	public BZSettings(BZStyledText bzStyledText)
	{
		this(bzStyledText, null);
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

		case 4:

			switch(tokens[0])
			{
			case "size":

				shellSize = new Point(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
				shellMaximized = Boolean.valueOf(tokens[3]);
				break;

			case "brailleText.font":

				bzStyledText.setBrailleFont(new Font(parentShell.getDisplay(),
				                                     tokens[1].replace('_', ' '),
				                                     Integer.parseInt(tokens[2]),
				                                     Integer.parseInt(tokens[3])));
				break;

			case "asciiText.font":

				bzStyledText.setAsciiFont(new Font(parentShell.getDisplay(),
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
			if(!readLine(line))
				System.err.println("Unknown setting:  " + line);
		}
		catch(FileNotFoundException ignored)
		{
			MessageBox messageBox = new MessageBox(parentShell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage("Unable to open settings file " + file.getPath());
			messageBox.open();
		}
		catch(IOException ignored)
		{
			MessageBox messageBox = new MessageBox(parentShell, SWT.ICON_ERROR | SWT.OK);
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
				MessageBox messageBox = new MessageBox(parentShell, SWT.ICON_ERROR | SWT.OK);
				messageBox.setMessage("Unable to close settings file " + file.getPath());
				messageBox.open();
			}
		}
	}

	private void writeLines(PrintWriter writer)
	{
		if(shellSize != null)
			writer.println("size " + shellSize.x + " " + shellSize.y + " " + shellMaximized);

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
			//TODO:  is this if necessary?
			if(!file.exists())
			{
				MessageBox messageBox = new MessageBox(parentShell, SWT.ICON_ERROR | SWT.OK);
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
			MessageBox messageBox = new MessageBox(parentShell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage("Unable to open settings file " + file.getPath());
			messageBox.open();
		}
		finally
		{
			if(writer != null)
				writer.close();
		}
	}

	private class ControlHandler implements ControlListener
	{
		/**
		 * @see org.aph.braillezephyr.BZSettings.ControlHandler.CheckMaximizeThread
		 */
		private volatile boolean checkingMaximize;

		private Point prevShellSize;

		@Override
		public void controlResized(ControlEvent ignored)
		{
			prevShellSize = shellSize;
			shellSize = parentShell.getSize();

			if(!checkingMaximize)
			{
				checkingMaximize = true;
				parentShell.getDisplay().timerExec(100, new CheckMaximizeThread());
			}

		}

		@Override
		public void controlMoved(ControlEvent ignored){}

		/**
		 * <p>
		 * The getMaximized method does not work with some window managers
		 * inside the controlResized method.  It needs to be called after the
		 * controlResized method returns.  Unlike the AdjustOtherThread object
		 * there is no corresponding event for which to wait.  So the thread
		 * for this class is run inside controlResized with a delay and it
		 * checks getMaximized.
		 * </p>
		 */
		private class CheckMaximizeThread implements Runnable
		{
			@Override
			public void run()
			{
				shellMaximized = parentShell.getMaximized();
				if(shellMaximized)
					shellSize = prevShellSize;
				checkingMaximize = false;
			}
		}
	}
}
