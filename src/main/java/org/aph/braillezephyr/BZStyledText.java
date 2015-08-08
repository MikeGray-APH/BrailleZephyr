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
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

public class BZStyledText
{
	private final static char PARAGRAPH_END = 0xfeff;

	private final StyledTextContent content;
	private final StyledText brailleText, asciiText;
	private final Composite composite;

	private final Color color;
	private final boolean windowBug = System.getProperty("os.name").toLowerCase().startsWith("windows");

	private StyledText currentText;

	private String eol = System.getProperty("line.separator");
	private int linesPerPage = 25;
	private int charsPerLine = 40;
	private int bellMargin = 31;
	private Clip bellClip = null;

	public BZStyledText(Shell shell)
	{
		color = shell.getDisplay().getSystemColor(SWT.COLOR_BLACK);

		composite = new Composite(shell, 0);
		composite.setLayout(new GridLayout(2, true));

		brailleText = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		brailleText.setLayoutData(new GridData(GridData.FILL_BOTH));
		brailleText.setFont(new Font(shell.getDisplay(), "SimBraille", 15, SWT.NORMAL));

		brailleText.addFocusListener(new FocusHandler(brailleText));
		brailleText.addCaretListener(new CaretHandler(brailleText));
		BrailleKeyHandler brailleKeyHandler = new BrailleKeyHandler();
		brailleText.addKeyListener(brailleKeyHandler);
		brailleText.addVerifyKeyListener(brailleKeyHandler);

		content = brailleText.getContent();

		asciiText = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		asciiText.setContent(content);
		asciiText.setLayoutData(new GridData(GridData.FILL_BOTH));
		asciiText.setFont(new Font(shell.getDisplay(), "Courier", 15, SWT.NORMAL));

		asciiText.addFocusListener(new FocusHandler(asciiText));
		asciiText.addCaretListener(new CaretHandler(asciiText));

		brailleText.addPaintListener(new PaintHandler(brailleText, asciiText));
		asciiText.addPaintListener(new PaintHandler(asciiText, brailleText));

		currentText = brailleText;

		try
		{
			InputStream inputStream = getClass().getResourceAsStream("/sounds/bell.wav");
			if(inputStream != null)
			{
				AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
				DataLine.Info dataLineInfo = new DataLine.Info(Clip.class, audioInputStream.getFormat());
				bellClip = (Clip)AudioSystem.getLine(dataLineInfo);
				bellClip.open(audioInputStream);
			}
		}
		catch(UnsupportedAudioFileException e)
		{
			e.printStackTrace();
			bellClip = null;
		}
		catch(LineUnavailableException e)
		{
			e.printStackTrace();
			bellClip = null;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			bellClip = null;
		}
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
		int bellDiff = this.charsPerLine - bellMargin;
		this.charsPerLine = charsPerLine;
		bellMargin = charsPerLine - bellDiff;
		if(bellMargin < 0)
			bellMargin = 0;
	}

	public int getBellMargin()
	{
		if(bellClip == null)
			return -1;
		return bellMargin;
	}

	public void setBellMargin(int bellMargin)
	{
		if(bellClip == null)
			return;
		this.bellMargin = bellMargin;
	}

	public boolean getBrailleVisible()
	{
		return brailleText.getVisible();
	}

	public void setBrailleVisible(boolean visible)
	{
		((GridData)brailleText.getLayoutData()).exclude = !visible;
		brailleText.setVisible(visible);
		((GridLayout)composite.getLayout()).makeColumnsEqualWidth = visible && asciiText.getVisible();
		composite.layout();

	}

	public boolean getAsciiVisible()
	{
		return asciiText.getVisible();
	}

	public void setAsciiVisible(boolean visible)
	{
		((GridData)asciiText.getLayoutData()).exclude = !visible;
		asciiText.setVisible(visible);
		((GridLayout)composite.getLayout()).makeColumnsEqualWidth = visible && brailleText.getVisible();
		composite.layout();
	}

	public Font getBrailleFont()
	{
		return brailleText.getFont();
	}

	public void setBrailleFont(Font font)
	{
		brailleText.setFont(font);
	}

	public Font getAsciiFont()
	{
		return asciiText.getFont();
	}

	public void setAsciiFont(Font font)
	{
		asciiText.setFont(font);
	}

	@SuppressWarnings("SameParameterValue")
	public void setText(String text)
	{
		content.setText(text);
	}

	public void redraw()
	{
		//TODO:  both?
		brailleText.redraw();
		asciiText.redraw();
	}

	private boolean isFirstLineOnPage(int index)
	{
		return index % linesPerPage == 0;
	}

	public void readBRF(Reader reader) throws IOException
	{
		boolean checkLinesPerPage = true;
		boolean removeFormFeed = true;
		char buffer[] = new char[65536];
		int cnt, trim;

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
							eol = "\r\n";
						break;

					case '\n':  lines++;  break;
					case 0xc:

						linesPerPage = lines;
						break outer;
					}

				if(eol == null)
					eol = "\n";
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

			content.setText(new String(buffer, 0, trim));
		}
	}

	public void writeBRF(Writer writer) throws IOException
	{
		String line = content.getLine(0);
		if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
			writer.write(line.substring(0, line.length() - 1));
		else
			writer.write(line);
		for(int i = 1; i < content.getLineCount(); i++)
		{
			writer.write(eol);
			if(isFirstLineOnPage(i))
				writer.write(0xc);
			line = content.getLine(i);
			if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
				writer.write(line.substring(0, line.length() - 1));
			else
				writer.write(line);
		}
		writer.flush();
	}

	public void readBZY(Reader reader) throws IOException
	{
		content.setText("");
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
				content.replaceTextRange(content.getCharCount(), 0, line.substring(0, line.length() - 1) + PARAGRAPH_END + eol);
			else
				content.replaceTextRange(content.getCharCount(), 0, line + eol);
		}
	}

	public void writeBZY(Writer writer) throws IOException
	{
		writer.write("Chars Per Line:  " + charsPerLine + eol);
		writer.write("Lines Per Page:  " + linesPerPage + eol);

		for(int i = 0; i < content.getLineCount(); i++)
		{
			String line = content.getLine(i);
			if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
				writer.write(line.substring(0, line.length() - 1) + (char)0xb6 + eol);
			else
				writer.write(line + eol);
		}

		writer.flush();
	}

	@SuppressWarnings("WeakerAccess")
	public void rewrapFromCaret()
	{
		for(int i = content.getLineAtOffset(currentText.getCaretOffset()); i < content.getLineCount(); i++)
		{
			String line = content.getLine(i);
			if(line.length() == 0)
				continue;

			//   line too long
			if(line.length() > charsPerLine)
			{
				int wordWrap, wordEnd;

				//   find beginning of word being wrapped
				if(line.charAt(charsPerLine) != ' ')
				{
					for(wordWrap = charsPerLine; wordWrap > charsPerLine / 2; wordWrap--)
						if(line.charAt(wordWrap) == ' ')
							break;
					if(wordWrap == charsPerLine / 2)
						continue;
					wordWrap++;
				}
				else
				{
					for(wordWrap = charsPerLine; wordWrap < line.length(); wordWrap++)
						if(line.charAt(wordWrap) != ' ')
							break;
					if(wordWrap == line.length())
						continue;
				}

				//   find end of word before word being wrapped
				for(wordEnd = wordWrap - 1; wordEnd > charsPerLine / 4; wordEnd--)
					if(line.charAt(wordEnd) != ' ')
						break;
				if(wordEnd == charsPerLine / 4)
					continue;
				wordEnd++;

				//   build replacement text
				int length = line.length();
				StringBuilder builder = new StringBuilder();
				builder.append(line.substring(0, wordEnd)).append(eol).append(line.substring(wordWrap, length));
				if(length > 0 && line.charAt(length - 1) != PARAGRAPH_END)
				if(i < content.getLineCount() - 1)
				{
					String next = content.getLine(i + 1);
					builder.append(" ").append(next);
					length += eol.length() + next.length();
				}

				content.replaceTextRange(content.getOffsetAtLine(i), length, builder.toString());
			}
			else if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
				break;
		}
	}

	private class FocusHandler implements FocusListener
	{
		private final StyledText styledText;

		private FocusHandler(StyledText styledText)
		{
			this.styledText = styledText;
		}

		@Override
		public void focusGained(FocusEvent e)
		{
			currentText = styledText;
		}

		@Override
		public void focusLost(FocusEvent event){}
	}

	private class CaretHandler implements CaretListener
	{
		private final StyledText styledText;

		private int prevOffset;

		private CaretHandler(StyledText styledText)
		{
			this.styledText = styledText;
		}

		@Override
		public void caretMoved(CaretEvent event)
		{
			if(bellClip != null && bellMargin > 0)
			{
				int caretOffset = currentText.getCaretOffset();
				if(bellMargin > 0 && caretOffset == prevOffset + 1)
				{
					int lineOffset = currentText.getOffsetAtLine(currentText.getLineAtOffset(caretOffset));
					if(caretOffset - lineOffset == bellMargin)
						if(!bellClip.isActive())
						{
							bellClip.setFramePosition(0);
							bellClip.start();
						}
				}
				prevOffset = caretOffset;
			}

			if(styledText == currentText)
				styledText.redraw();
		}
	}

	private class PaintHandler implements PaintListener
	{
		private final StyledText source, other;

		private PaintHandler(StyledText source, StyledText other)
		{
			this.source = source;
			this.other = other;
		}

		@Override
		public void paintControl(PaintEvent event)
		{
			event.gc.setForeground(color);
			event.gc.setBackground(color);
			int lineHeight = source.getLineHeight();
			int drawHeight = source.getClientArea().height;
			int drawWidth = source.getClientArea().width;
			int rightMargin = event.gc.getFontMetrics().getAverageCharWidth() * charsPerLine;

			event.gc.drawLine(rightMargin, 0, rightMargin, drawHeight);

			int at;
			for(int i = source.getTopIndex(); i < source.getLineCount(); i++)
			{
				at = source.getLinePixel(i);
				if(isFirstLineOnPage(i))
					event.gc.drawLine(0, at, drawWidth, at);

				String line = source.getLine(i);
				if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
				{
					Point point = event.gc.stringExtent(line);
					int span = point.y / 2;
					event.gc.fillOval(point.x + span / 2, at + span / 2, span, span);
				}

				if(at + lineHeight > drawHeight)
					break;
			}

			if(source != currentText)
				return;
			int lineOffset = source.getLineAtOffset(source.getCaretOffset());
			int srcLinePixel = source.getLinePixel(lineOffset);
			int othLineHeight = other.getLineHeight();
			int othLineRealPixel = lineOffset * othLineHeight;
			other.setTopPixel(othLineRealPixel - srcLinePixel);
		}
	}

	private class BrailleKeyHandler implements KeyListener, VerifyKeyListener
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
			if(windowBug)
			    dotState = 0;
			else switch(event.character)
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
				brailleText.insert(Character.toString(dotChar));
				brailleText.setCaretOffset(brailleText.getCaretOffset() + 1);
				dotChar = 0x2800;
			}
		}

		private final String asciiBraille = " A1B'K2L@CIF/MSP\"E3H9O6R^DJG>NTQ,*5<-U8V.%[$+X!&;:4\\0Z7(_?W]#Y)=";

		@Override
		public void verifyKey(VerifyEvent event)
		{
			if(event.keyCode == '\r' || event.keyCode == '\n')
			if((event.stateMask & SWT.SHIFT) != 0)
			{
				event.doit = false;
				int index = brailleText.getLineAtOffset(brailleText.getCaretOffset());
				String line = brailleText.getLine(index);
				if(line.length() > 0)
				if(line.charAt(line.length() - 1) != PARAGRAPH_END)
					brailleText.replaceTextRange(brailleText.getOffsetAtLine(index), line.length(), line + Character.toString(PARAGRAPH_END));
				else
					brailleText.replaceTextRange(brailleText.getOffsetAtLine(index), line.length(), line.substring(0, line.length() - 1));
				return;
			}

			if(event.character > ' ' && event.character < 0x7f)
				event.doit = false;
		}
	}
}

