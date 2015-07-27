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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import java.io.*;

public class BZStyledText
{
	private StyledText styledText;

	private int linesPerPage = 25;
	private int charsPerLine = 40;

	private Color pageSeparatorColor;

	public BZStyledText(Shell shell)
	{
		styledText = new StyledText(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		styledText.setLayoutData(new GridData(GridData.FILL_BOTH));
		styledText.addListener(SWT.Paint, new BZStyledTextPaintListener());

		Font font = new Font(styledText.getDisplay(), "SimBraille", 15, SWT.NORMAL);
		if(font != null)
			styledText.setFont(font);

		pageSeparatorColor = styledText.getDisplay().getSystemColor(SWT.COLOR_BLACK);
	}

	public int getLinesPerPage()
	{
		return linesPerPage;
	}

	public void setLinesPerPage(int linesPerPage)
	{
		this.linesPerPage = linesPerPage;
	}

	public int getCharsPerLine()
	{
		return charsPerLine;
	}

	public void setCharsPerLine(int charsPerLine)
	{
		this.charsPerLine = charsPerLine;
	}

	public Color getPageSeparatorColor()
	{
		return pageSeparatorColor;
	}

	public void setPageSeparatorColor(Color pageSeparatorColor)
	{
		this.pageSeparatorColor = pageSeparatorColor;
	}

	public Font getFont()
	{
		return styledText.getFont();
	}

	public void setFont(Font font)
	{
		styledText.setFont(font);
	}

	public void redraw()
	{
		styledText.redraw();
	}

	public void setText(Reader reader) throws IOException
	{
		boolean checkLinesPerPage = true;
		boolean removeFormFeed = true;
		char buffer[] = new char[65536];
		int cnt, trim;

		styledText.setText("");
		while((cnt = reader.read(buffer)) > 0)
		{
			if(checkLinesPerPage)
			{
				checkLinesPerPage = false;
				int lines = 0, i;
				outer:for(i = 0; i < 65536; i++)
				switch(buffer[i])
				{
				case '\n':  lines++;  break;
				case 0xc:

					linesPerPage = lines;
					break outer;
				}

				if(i == 65536)
					removeFormFeed = false;
			}

			if(removeFormFeed)
			{
				trim = 0;
				for(int i = 0; i < cnt; i++)
				{
					if(buffer[i] != 0xc)
					{
						buffer[trim] = buffer[i];
						trim++;
					}
				}
			}
			else
				trim = cnt;

			styledText.append(new String(buffer, 0, trim));
		}
	}

	private boolean isFirstLineOnPage(int index)
	{
		return index % linesPerPage == 0;
	}

	private boolean isLastLineOnPage(int index)
	{
		return index - 1 % linesPerPage == 0;
	}

	private class BZStyledTextPaintListener implements Listener
	{
		@Override
		public void handleEvent(Event event)
		{
			event.gc.setForeground(pageSeparatorColor);
			int lineHeight = styledText.getLineHeight();
			int drawHeight = styledText.getClientArea().height;
			int drawWidth = styledText.getClientArea().width;
			int at = 0;
			for(int i = styledText.getTopIndex(); i < styledText.getLineCount(); i++)
			{
				at = styledText.getLinePixel(i);
				if(isFirstLineOnPage(i))
					event.gc.drawLine(0, at, drawWidth, at);
				if(at + lineHeight > drawHeight)
					break;
			}
		}
	}
}

