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
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class BZStyledText
{
	private final static char PARAGRAPH_END = 0xfeff;

	private StyledText styledText;

	private int linesPerPage = 25;
	private int charsPerLine = 40;
	String eol = System.getProperty("line.separator");

	private Color pageSeparatorColor;

	public BZStyledText(Shell shell)
	{
		styledText = new StyledText(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		styledText.setLayoutData(new GridData(GridData.FILL_BOTH));
		KeyHandler keyHandler = new KeyHandler();
		styledText.addKeyListener(keyHandler);
		styledText.addVerifyKeyListener(keyHandler);
		styledText.addPaintListener(new PaintHandler());

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

	public void setText(String text)
	{
		styledText.setText(text);
	}

	public void redraw()
	{
		styledText.redraw();
	}

	private boolean isFirstLineOnPage(int index)
	{
		return index % linesPerPage == 0;
	}

	private boolean isLastLineOnPage(int index)
	{
		return (index + 1) % linesPerPage == 0;
	}

	public void readBRF(Reader reader) throws IOException
	{
		boolean checkLinesPerPage = true;
		boolean removeFormFeed = true;
		char buffer[] = new char[65536];
		int cnt, trim;

		styledText.setText("");
		eol = null;
		while((cnt = reader.read(buffer)) > 0)
		{
			if(checkLinesPerPage)
			{
				checkLinesPerPage = false;
				int lines = 0, i;
				outer:for(i = 0; i < cnt; i++)
					switch(buffer[i])
					{
					case '\r':

						if(eol == null)
							eol = new String("\r\n");
						break;

					case '\n':  lines++;  break;
					case 0xc:

						linesPerPage = lines;
						break outer;
					}

				if(eol == null)
					eol = new String("\n");
				if(i == cnt)
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

	public void writeBRF(Writer writer) throws IOException
	{
		String line = styledText.getLine(0);
		if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
			writer.write(line.substring(0, line.length() - 1));
		else
			writer.write(line);
		for(int i = 1; i < styledText.getLineCount(); i++)
		{
			writer.write(eol);
			if(isFirstLineOnPage(i))
				writer.write(0xc);
			line = styledText.getLine(i);
			if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
				writer.write(line.substring(0, line.length() - 1));
			else
				writer.write(line);
		}
		writer.flush();
	}

	public void readBZY(Reader reader) throws IOException
	{
		styledText.setText("");
		eol = System.getProperty("line.separator");
		BufferedReader buffer = new BufferedReader(reader);

		//TODO:  verify file format
		String line = buffer.readLine();
		charsPerLine = Integer.parseInt(line.substring(17));
		line = buffer.readLine();
		linesPerPage = Integer.parseInt(line.substring(17));

		while((line = buffer.readLine()) != null)
		{
			if(line.length() > 0 && line.charAt(line.length() - 1) == 0xb6)
				styledText.append(line.substring(0, line.length() - 1) + PARAGRAPH_END + eol);
			else
				styledText.append(line + eol);
		}
	}

	public void writeBZY(Writer writer) throws IOException
	{
		writer.write("Chars Per Line:  " + charsPerLine + eol);
		writer.write("Lines Per Page:  " + linesPerPage + eol);

		for(int i = 0; i < styledText.getLineCount(); i++)
		{
			String line = styledText.getLine(i);
			if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
				writer.write(line.substring(0, line.length() - 1) + (char)0xb6 + eol);
			else
				writer.write(line + eol);
		}

		writer.flush();
	}

	public void rewrapFromCaret()
	{
		for(int i = styledText.getLineAtOffset(styledText.getCaretOffset()); i < styledText.getLineCount(); i++)
		{
			String line = styledText.getLine(i);
			if(line.length() == 0)
				continue;

			//   line too long
			if(line.length() > charsPerLine)
			{
				int wordWrap = 0;
				int wordEnd = 0;

				//   find beginning of word being wrapped
				if(line.charAt(charsPerLine - 1) != ' ')
				{
					for(wordWrap = charsPerLine - 1; wordWrap > charsPerLine / 2; wordWrap--)
						if(line.charAt(wordWrap) == ' ')
							break;
					if(wordWrap == charsPerLine / 2)
						continue;
					wordWrap++;
				}
				else
				{
					for(wordWrap = charsPerLine - 1; wordWrap < line.length(); wordWrap++)
						if(line.charAt(wordWrap) != ' ')
							break;
					if(wordWrap == line.length())
						continue;
				}

				//   find end of word
				for(wordEnd = wordWrap - 1; wordEnd > charsPerLine / 4; wordEnd--)
					if(line.charAt(wordEnd) != ' ')
						break;
				if(wordEnd == charsPerLine / 4)
					continue;
				wordEnd++;

				int length = line.length();
				StringBuilder builder = new StringBuilder(line.substring(0, wordEnd) + eol + line.substring(wordWrap,  length));
				if(length > 0 && line.charAt(length - 1) != PARAGRAPH_END)
				if(i < styledText.getLineCount() - 1)
				{
					String next = styledText.getLine(i + 1);
					builder.append(" " + next);
					length += eol.length() + next.length();
				}

				styledText.replaceTextRange(styledText.getOffsetAtLine(i), length, builder.toString());
			}
			else if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
				break;
		}
	}

	private class KeyHandler implements KeyListener, VerifyKeyListener
	{
		char dotState = 0, dotChar = 0x2800;

		@Override
		public void keyPressed(KeyEvent event)
		{
			switch(event.character)
			{
			case 'f':

				dotState |= 0x01;
				dotChar |= 0x01;
				break;

			case 'd':

				dotState |= 0x02;
				dotChar |= 0x02;
				break;

			case 's':

				dotState |= 0x04;
				dotChar |= 0x04;
				break;

			case 'j':

				dotState |= 0x08;
				dotChar |= 0x08;
				break;

			case 'k':

				dotState |= 0x10;
				dotChar |= 0x10;
				break;

			case 'l':

				dotState |= 0x20;
				dotChar |= 0x20;
				break;

			}
		}

		@Override
		public void keyReleased(KeyEvent event)
		{
			switch(event.character)
			{
			case 'f':

				dotState &= ~0x01;
				break;

			case 'd':

				dotState &= ~0x02;
				break;

			case 's':

				dotState &= ~0x04;
				break;

			case 'j':

				dotState &= ~0x08;
				break;

			case 'k':

				dotState &= ~0x10;
				break;

			case 'l':

				dotState &= ~0x20;
				break;

			}

			if(dotState == 0 && (dotChar & 0xff) != 0)
			{
				dotChar = asciiBraille.charAt((dotChar & 0xff));
				styledText.insert(Character.toString(dotChar));
				styledText.setCaretOffset(styledText.getCaretOffset() + 1);
				dotChar = 0x2800;
			}
		}

		private String asciiBraille = " A1B'K2L@CIF/MSP\"E3H9O6R^DJG>NTQ,*5<-U8V.%[$+X!&;:4\\0Z7(_?W]#Y)=";

		@Override
		public void verifyKey(VerifyEvent event)
		{
			if(event.keyCode == '\r' || event.keyCode == '\n')
			if((event.stateMask & SWT.SHIFT) != 0)
			{
				event.doit = false;
				int index = styledText.getLineAtOffset(styledText.getCaretOffset());
				String line = styledText.getLine(index);
				if(line.length() > 0)
				if(line.charAt(line.length() - 1) != PARAGRAPH_END)
					styledText.replaceTextRange(styledText.getOffsetAtLine(index), line.length(), line + Character.toString(PARAGRAPH_END));
				else
					styledText.replaceTextRange(styledText.getOffsetAtLine(index), line.length(), line.substring(0, line.length() - 1));
				return;
			}

			if((event.stateMask & SWT.CONTROL) != 0)
			if(event.keyCode == 'f')
			{
				event.doit = false;
				rewrapFromCaret();
				return;
			}

			if(event.character > ' ' && event.character < 0x7f)
				event.doit = false;
		}
	}

	private class PaintHandler implements PaintListener
	{
		@Override
		public void paintControl(PaintEvent event)
		{
			event.gc.setForeground(pageSeparatorColor);
			event.gc.setBackground(pageSeparatorColor);
			int lineHeight = styledText.getLineHeight();
			int drawHeight = styledText.getClientArea().height;
			int drawWidth = styledText.getClientArea().width;
			int rightMargin = event.gc.stringExtent(" ").x * charsPerLine;

			event.gc.drawLine(rightMargin, 0, rightMargin, drawHeight);

			int at = 0;
			for(int i = styledText.getTopIndex(); i < styledText.getLineCount(); i++)
			{
				at = styledText.getLinePixel(i);
				if(isFirstLineOnPage(i))
					event.gc.drawLine(0, at, drawWidth, at);

				String line = styledText.getLine(i);
				if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
				{
					Point point = event.gc.stringExtent(line);
					int span = point.y / 2;
					event.gc.fillOval(point.x + span / 2, at + span / 2, span, span);
				}

				if(at + lineHeight > drawHeight)
					break;
			}
		}
	}
}

