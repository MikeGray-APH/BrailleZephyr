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
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
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
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This class is the container for both the style views that display braille
 * and ascii.
 * </p>
 *
 * @author Mike Gray mgray@aph.org
 */
public class BZStyledText
{
	private static final char PARAGRAPH_END = 0xfeff;

	private final Shell parentShell;
	private final Composite composite;
	private final StyledText brailleText, asciiText;
	private final StyledTextContent content;

	private final String versionString;
	private final int versionMajor, versionMinor, versionPatch;

	private final boolean windowBug = System.getProperty("os.name").toLowerCase().startsWith("windows");
	private final AdjustOtherThread adjustOtherThread = new AdjustOtherThread();
	private final Color color;

	private StyledText currentText;

	private String eol = System.getProperty("line.separator");
	private int linesPerPage = 25;
	private int charsPerLine = 40;

	private int lineMarginBell = 33;
	private Clip lineMarginClip;
	private String lineMarginFileName;

	private int pageMarginBell = 25;
	private Clip pageMarginClip;
	private String pageMarginFileName;

	private Clip lineEndClip;
	private String lineEndFileName;

	private final List<ExtendedModifyEvent> changes = new ArrayList<>(1000);
	private int changeIndex, saveIndex;
	private boolean undoing, redoing;

	private final StringWriter logString = new StringWriter();
	private final PrintWriter logWriter = new PrintWriter(logString);

	/**
	 * <p>
	 * Creates a new <code>BZStyledText</code> for parentShell <code>parentShell</code>.
	 * </p>
	 *
	 * @param parentShell parentShell of the new instance (cannot be null)
	 */
	public BZStyledText(Shell parentShell)
	{
		this.parentShell = parentShell;

		//   version from jar manifest
		String version = getClass().getPackage().getImplementationVersion();

		//   version from build file
		if(version == null)
			version = System.getProperty("braillezephyr.version");

		//   no version
		if(version == null)
		{
			logWriter.println("WARNING:  unable to determine version, using 0.0");
			version = "0.0";
		}

		versionString = version;
		String[] versionStrings = versionString.split("\\.");
		versionMajor = Integer.parseInt(versionStrings[0]);
		if(versionStrings.length > 1)
			versionMinor = Integer.parseInt(versionStrings[1]);
		else
			versionMinor = 0;
		if(versionStrings.length > 2)
			versionPatch = Integer.parseInt(versionStrings[2]);
		else
			versionPatch = 0;

		color = parentShell.getDisplay().getSystemColor(SWT.COLOR_BLACK);

		composite = new Composite(parentShell, 0);
		composite.setLayout(new GridLayout(2, true));

		//   load fonts
		loadFont("BrailleZephyr_6.otf");
		loadFont("BrailleZephyr_6b.otf");
		loadFont("BrailleZephyr_6s.otf");
		loadFont("BrailleZephyr_6sb.otf");
		loadFont("BrailleZephyr_8.otf");
		loadFont("BrailleZephyr_8b.otf");
		loadFont("BrailleZephyr_8s.otf");
		loadFont("BrailleZephyr_8sb.otf");
		loadFont("BrailleZephyr_8w.otf");
		loadFont("BrailleZephyr_8wb.otf");
		loadFont("BrailleZephyr_8ws.otf");
		loadFont("BrailleZephyr_8wsb.otf");

		//   load line margin bell
		try
		{
			InputStream inputStreamBellMargin = new BufferedInputStream(getClass().getResourceAsStream("/sounds/line_margin_bell.wav"));
			AudioInputStream audioInputStreamMargin = AudioSystem.getAudioInputStream(inputStreamBellMargin);
			DataLine.Info dataLineInfoMargin = new DataLine.Info(Clip.class, audioInputStreamMargin.getFormat());
			lineMarginClip = (Clip)AudioSystem.getLine(dataLineInfoMargin);
			lineMarginClip.open(audioInputStreamMargin);
		}
		catch(IOException exception)
		{
			logWriter.println("ERROR:  Unable to read default line margin bell file:  " + exception.getMessage());
			lineMarginClip = null;
		}
		catch(UnsupportedAudioFileException exception)
		{
			logWriter.println("ERROR:  Sound file unsupported for default line margin bell:  " + exception.getMessage());
			lineMarginClip = null;
		}
		catch(LineUnavailableException exception)
		{
			logWriter.println("ERROR:  Line unavailable for default line margin bell:  " + exception.getMessage());
			lineMarginClip = null;
		}

		//   load page margin bell
		try
		{
			InputStream inputStreamBellPage = new BufferedInputStream(getClass().getResourceAsStream("/sounds/page_margin_bell.wav"));
			AudioInputStream audioInputStreamPage = AudioSystem.getAudioInputStream(inputStreamBellPage);
			DataLine.Info dataLineInfoPage = new DataLine.Info(Clip.class, audioInputStreamPage.getFormat());
			pageMarginClip = (Clip)AudioSystem.getLine(dataLineInfoPage);
			pageMarginClip.open(audioInputStreamPage);
		}
		catch(IOException exception)
		{
			logWriter.println("ERROR:  Unable to read default page margin bell file:  " + exception.getMessage());
			pageMarginClip = null;
		}
		catch(UnsupportedAudioFileException exception)
		{
			logWriter.println("ERROR:  Sound file unsupported for default page margin bell:  " + exception.getMessage());
			pageMarginClip = null;
		}
		catch(LineUnavailableException exception)
		{
			logWriter.println("ERROR:  Line unavailable for default page margin bell:  " + exception.getMessage());
			pageMarginClip = null;
		}

		//   load line end bell
		try
		{
			InputStream inputStreamBellPage = new BufferedInputStream(getClass().getResourceAsStream("/sounds/line_end_bell.wav"));
			AudioInputStream audioInputStreamPage = AudioSystem.getAudioInputStream(inputStreamBellPage);
			DataLine.Info dataLineInfoPage = new DataLine.Info(Clip.class, audioInputStreamPage.getFormat());
			lineEndClip = (Clip)AudioSystem.getLine(dataLineInfoPage);
			lineEndClip.open(audioInputStreamPage);
		}
		catch(IOException exception)
		{
			logWriter.println("ERROR:  Unable to read default line end bell file:  " + exception.getMessage());
			lineEndClip = null;
		}
		catch(UnsupportedAudioFileException exception)
		{
			logWriter.println("ERROR:  Sound file unsupported for default line end bell:  " + exception.getMessage());
			lineEndClip = null;
		}
		catch(LineUnavailableException exception)
		{
			logWriter.println("ERROR:  Line unavailable for default line end bell:  " + exception.getMessage());
			lineEndClip = null;
		}

		brailleText = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		brailleText.setLayoutData(new GridData(GridData.FILL_BOTH));
		brailleText.setFont(new Font(parentShell.getDisplay(), "BrailleZephyr_6s", 18, SWT.NORMAL));
		brailleText.addFocusListener(new FocusHandler(brailleText));
		brailleText.addPaintListener(new PaintHandler(brailleText));
		BrailleKeyHandler brailleKeyHandler = new BrailleKeyHandler(true);
		brailleText.addKeyListener(brailleKeyHandler);
		brailleText.addVerifyKeyListener(brailleKeyHandler);
		brailleText.addExtendedModifyListener(new ExtendedModifyHandler(brailleText));

		content = brailleText.getContent();

		asciiText = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		asciiText.setContent(content);
		asciiText.setLayoutData(new GridData(GridData.FILL_BOTH));
		asciiText.setFont(new Font(parentShell.getDisplay(), "Monospace", 18, SWT.NORMAL));
		asciiText.addFocusListener(new FocusHandler(asciiText));
		asciiText.addPaintListener(new PaintHandler(asciiText));
		asciiText.addVerifyKeyListener(new BrailleKeyHandler(false));
		asciiText.addExtendedModifyListener(new ExtendedModifyHandler(asciiText));

		brailleText.addCaretListener(new CaretHandler(brailleText, asciiText));
		asciiText.addCaretListener(new CaretHandler(asciiText, brailleText));

		currentText = brailleText;
	}

	private void loadFont(String fontFileName)
	{
		try
		{
			InputStream fontInputStream = getClass().getResourceAsStream("/fonts/" + fontFileName);
			if(fontInputStream == null)
				return;
			File fontFile = new File(System.getProperty("java.io.tmpdir") + File.separator + fontFileName);
			FileOutputStream fontOutputStream = new FileOutputStream(fontFile);
			byte buffer[] = new byte[27720];
			int length;
			while((length = fontInputStream.read(buffer)) > 0)
				fontOutputStream.write(buffer, 0, length);
			fontInputStream.close();
			fontOutputStream.close();

			parentShell.getDisplay().loadFont(fontFile.getPath());
		}
		catch(FileNotFoundException exception)
		{
			logWriter.println("ERROR:  Unable to open font file:  " + exception.getMessage());
		}
		catch(IOException exception)
		{
			logWriter.println("ERROR:  Unable to read font file:  " + exception.getMessage());
		}
	}

	Shell getParentShell()
	{
		return parentShell;
	}

	String getVersionString()
	{
		return versionString;
	}

	/**
	 * <p>
	 * Returns the current number of lines per page.
	 * </p>
	 *
	 * @return the current value
	 *
	 * @see #setLinesPerPage(int)
	 */
	public int getLinesPerPage()
	{
		return linesPerPage;
	}

	/**
	 * <p>
	 * Sets the current number of lines per page.
	 * </p><p>
	 * This also resets the page bell relative to the previous settings.
	 * </p>
	 *
	 * @param linesPerPage the new value
	 *
	 * @see #getLinesPerPage()
	 */
	public void setLinesPerPage(int linesPerPage)
	{
		int bellDiff = this.linesPerPage - pageMarginBell;
		this.linesPerPage = linesPerPage;
		pageMarginBell = linesPerPage - bellDiff;
		if(pageMarginBell < 0)
			pageMarginBell = 0;
	}

	/**
	 * <p>
	 * Returns the current log messages.
	 * </p>
	 *
	 * @return the current log messages
	 */
	public String getLogString()
	{
		return logString.toString();
	}

	PrintWriter getLogWriter()
	{
		return logWriter;
	}

	/**
	 * <p>
	 * Returns the current number of characters per line.
	 * </p>
	 *
	 * @return the current value
	 *
	 * @see #setCharsPerLine(int)
	 */
	public int getCharsPerLine()
	{
		return charsPerLine;
	}

	/**
	 * <p>
	 * Sets the current number of characters per line.
	 * </p><p>
	 * This also resets the line bell relative to the previous settings.
	 * </p><p>
	 * The current lines are not reformatted.
	 * </p>
	 *
	 * @param charsPerLine the new value.
	 *
	 * @see #getCharsPerLine()
	 */
	public void setCharsPerLine(int charsPerLine)
	{
		int bellDiff = this.charsPerLine - lineMarginBell;
		this.charsPerLine = charsPerLine;
		lineMarginBell = charsPerLine - bellDiff;
		if(lineMarginBell < 0)
			lineMarginBell = 0;
	}

	/**
	 * <p>
	 * Returns the margin which the line bell is played.
	 * </p>
	 *
	 * @return the current value, -1 if no margin bell
	 *
	 * @see #setLineMarginBell(int)
	 */
	public int getLineMarginBell()
	{
		if(lineMarginClip == null)
			return -1;
		return lineMarginBell;
	}

	/**
	 * <p>
	 * Sets the current number of lines per page.
	 * </p><p>
	 * The sound is only played when the caret moves from one space before
	 * the margin to the margin.
	 * </p>
	 *
	 * @param lineMarginBell the new value
	 *
	 * @see #getLineMarginBell()
	 */
	public void setLineMarginBell(int lineMarginBell)
	{
		if(lineMarginClip == null)
			return;
		this.lineMarginBell = lineMarginBell;
	}

	/**
	 * <p>
	 * Returns the file name of the sound file used for the line margin bell.
	 *
	 * @return the current file name or <code>null</code> if none
	 *
	 * @see #loadLineMarginFileName(String)
	 * </p>
	 */
	public String getLineMarginFileName()
	{
		return lineMarginFileName;
	}

	/**
	 * <p>
	 * Loads the sound file specified by fileName and uses it for the line
	 * margin bell.
	 *
	 * @param fileName the name of the file to load
	 *
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 * @throws LineUnavailableException
	 *
	 * @see #getLineMarginFileName()
	 * </p>
	 */
	public void loadLineMarginFileName(String fileName) throws FileNotFoundException,
	                                                           IOException,
	                                                           UnsupportedAudioFileException,
	                                                           LineUnavailableException
	{
		InputStream inputStream = new BufferedInputStream(new FileInputStream(fileName));
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
		DataLine.Info dataLineInfo = new DataLine.Info(Clip.class, audioInputStream.getFormat());
		Clip clip = lineMarginClip;
		try
		{
			lineMarginClip = (Clip)AudioSystem.getLine(dataLineInfo);
			lineMarginClip.open(audioInputStream);
		}
		catch(IOException | LineUnavailableException exception)
		{
			lineMarginClip = clip;
			throw exception;
		}
		lineMarginFileName = fileName;
		if(clip != null)
			clip.close();
	}

	/**
	 * <p>
	 * Returns the current number of characters per line.
	 * </p>
	 *
	 * @return the current value, -1 if no page bell
	 *
	 * @see #setPageMarginBell(int)
	 */
	public int getPageMarginBell()
	{
		if(pageMarginClip == null)
			return -1;
		return pageMarginBell;
	}

	/**
	 * <p>
	 * Sets the current number of lines per page.
	 * </p><p>
	 * The sound is only played when the caret moves from the line before
	 * the margin to the margin when the enter key is pressed.
	 * </p>
	 *
	 * @param pageMarginBell the new value
	 *
	 * @see #getPageMarginBell()
	 */
	public void setPageMarginBell(int pageMarginBell)
	{
		if(pageMarginClip == null)
			return;
		this.pageMarginBell = pageMarginBell;
	}

	/**
	 * <p>
	 * Returns the file name of the sound file used for the page margin bell.
	 *
	 * @return the current file name or <code>null</code> if none
	 *
	 * @see #loadPageMarginFileName(String)
	 * </p>
	 */
	public String getPageMarginFileName()
	{
		return pageMarginFileName;
	}

	/**
	 * <p>
	 * Loads the sound file specified by fileName and uses it for the page
	 * margin bell.
	 *
	 * @param fileName the name of the file to load
	 *
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 * @throws LineUnavailableException
	 *
	 * @see #getPageMarginFileName()
	 * </p>
	 */
	public void loadPageMarginFileName(String fileName) throws FileNotFoundException,
	                                                           IOException,
	                                                           UnsupportedAudioFileException,
	                                                           LineUnavailableException
	{
		InputStream inputStream = new BufferedInputStream(new FileInputStream(fileName));
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
		DataLine.Info dataLineInfo = new DataLine.Info(Clip.class, audioInputStream.getFormat());
		Clip clip = pageMarginClip;
		try
		{
			pageMarginClip = (Clip)AudioSystem.getLine(dataLineInfo);
			pageMarginClip.open(audioInputStream);
		}
		catch(IOException | LineUnavailableException exception)
		{
			pageMarginClip = clip;
			throw exception;
		}
		pageMarginFileName = fileName;
		if(clip != null)
			clip.close();
	}

	/**
	 * <p>
	 * Loads the sound file specified by fileName and uses it for the line end
	 * bell.
	 *
	 * @param fileName the name of the file to load
	 *
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 * @throws LineUnavailableException
	 *
	 * @see #getLineEndFileName()
	 * </p>
	 */
	public void loadLineEndFileName(String fileName) throws FileNotFoundException,
	                                                        IOException,
	                                                        UnsupportedAudioFileException,
	                                                        LineUnavailableException
	{
		InputStream inputStream = new BufferedInputStream(new FileInputStream(fileName));
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
		DataLine.Info dataLineInfo = new DataLine.Info(Clip.class, audioInputStream.getFormat());
		Clip clip = lineEndClip;
		try
		{
			lineEndClip = (Clip)AudioSystem.getLine(dataLineInfo);
			lineEndClip.open(audioInputStream);
		}
		catch(IOException | LineUnavailableException exception)
		{
			lineEndClip = clip;
			throw exception;
		}
		lineEndFileName = fileName;
		if(clip != null)
			clip.close();
	}

	/**
	 * <p>
	 * Returns the file name of the sound file used for the line end bell.
	 *
	 * @return the current file name or <code>null</code> if none
	 *
	 * @see #loadLineEndFileName(String)
	 * </p>
	 */
	public String getLineEndFileName()
	{
		return lineEndFileName;
	}

	/**
	 * <p>
	 * Returns if the braille text is visible.
	 * </p>
	 *
	 * @return the current visibility of the braille text
	 *
	 * @see #setBrailleVisible(boolean)
	 */
	public boolean getBrailleVisible()
	{
		return brailleText.getVisible();
	}

	/**
	 * <p>
	 * Sets the visibility of the braille text.
	 * </p>
	 *
	 * @param visible the visibility of the braille text
	 *
	 * @see #getBrailleVisible()
	 */
	public void setBrailleVisible(boolean visible)
	{
		((GridData)brailleText.getLayoutData()).exclude = !visible;
		brailleText.setVisible(visible);
		((GridLayout)composite.getLayout()).makeColumnsEqualWidth = visible && asciiText.getVisible();
		composite.layout();
	}

	/**
	 * <p>
	 * Returns if the ascii text is visible.
	 * </p>
	 *
	 * @return the current visibility of the ascii text
	 *
	 * @see #setAsciiVisible(boolean)
	 */
	public boolean getAsciiVisible()
	{
		return asciiText.getVisible();
	}

	/**
	 * <p>
	 * Sets the visibility of the ascii text.
	 * </p>
	 *
	 * @param visible the visibility of the ascii text
	 *
	 * @see #getAsciiVisible()
	 */
	public void setAsciiVisible(boolean visible)
	{
		((GridData)asciiText.getLayoutData()).exclude = !visible;
		asciiText.setVisible(visible);
		((GridLayout)composite.getLayout()).makeColumnsEqualWidth = visible && brailleText.getVisible();
		composite.layout();
	}

	/**
	 * <p>
	 * Returns the current font of the braille text.
	 * </p>
	 *
	 * @return the current font
	 *
	 * @see #setBrailleFont(Font)
	 */
	public Font getBrailleFont()
	{
		return brailleText.getFont();
	}

	/**
	 * <p>
	 * Sets the font for the braille text.
	 * </p>
	 *
	 * @param font the new font
	 *
	 * @see #getAsciiVisible()
	 */
	public void setBrailleFont(Font font)
	{
		brailleText.setFont(font);
	}

	/**
	 * <p>
	 * Returns the current font of the ascii text.
	 * </p>
	 *
	 * @return the current font
	 *
	 * @see #setAsciiFont(Font)
	 */
	public Font getAsciiFont()
	{
		return asciiText.getFont();
	}

	/**
	 * <p>
	 * Sets the font for the ascii text.
	 * </p>
	 *
	 * @param font the new font
	 *
	 * @see #getAsciiFont()
	 */
	public void setAsciiFont(Font font)
	{
		asciiText.setFont(font);
	}

	//TODO:  getText()

	/**
	 * <p>
	 * Sets the text for both braille and ascii texts.
	 * </p>
	 *
	 * @param text the string to set the text
	 */
	public void setText(String text)
	{
		content.setText(text);
		changes.clear();
		changeIndex = saveIndex = 0;
	}

	/**
	 * <p>
	 * Redraw both braille and ascii texts.
	 * </p>
	 */
	public void redraw()
	{
		//TODO:  both?
		brailleText.redraw();
		asciiText.redraw();
	}

	/**
	 * <p>
	 * Cut from selected text to the clipboard.
	 * </p>
	 */
	public void cut()
	{
		currentText.cut();
	}

	/**
	 * <p>
	 * Copy selected text to the clipboard.
	 * </p>
	 */
	public void copy()
	{
		currentText.copy();
	}

	/**
	 * <p>
	 * Paste text from the clipboard to caret offset, or replace selected text.
	 * </p>
	 */
	public void paste()
	{
		currentText.paste();
	}

	private void scrollToCaret()
	{
		int caretOffset = currentText.getCaretOffset();
		int lineIndex = currentText.getLineAtOffset(caretOffset);
		int lineHeight = currentText.getLineHeight();
		int linesVisible = currentText.getSize().y / lineHeight;
		int lineMiddle = linesVisible / 2;
		int lineTop = lineIndex - lineMiddle;
		if(lineTop < 0)
			lineTop = 0;
		currentText.setTopIndex(lineTop);
	}

	private void clearChanges()
	{
		changes.clear();
		changeIndex = saveIndex = 0;
	}

	private void resetChanges()
	{
		saveIndex = changeIndex;
	}

	/**
	 * <p>
	 * Returns whether or not the text has been modified and needs to be
	 * saved.
	 * </p>
	 *
	 * @return whether or not the text has been modified.
	 *
	 * @see #undo()
	 * @see #redo()
	 */
	public boolean getModified()
	{
		return saveIndex != changeIndex;
	}

	/**
	 * <p>
	 * Undoes the last change.
	 * </p><p>
	 * Currently only simply changes are recorded.
	 * </p>
	 *
	 * @see #redo()
	 * @see #getModified()
	 */
	public void undo()
	{
		if(changeIndex < 1)
			return;
		undoing = true;
		changeIndex--;
		ExtendedModifyEvent change = changes.remove(changeIndex);
		currentText.replaceTextRange(change.start, change.length, change.replacedText);
		currentText.setCaretOffset(change.start + change.replacedText.length());
		scrollToCaret();
	}

	/**
	 * <p>
	 * Undoes the last undo.
	 * </p>
	 *
	 * @see #undo()
	 * @see #getModified()
	 */
	public void redo()
	{
		if(changeIndex == changes.size())
			return;
		redoing = true;
		ExtendedModifyEvent change = changes.remove(changeIndex);
		currentText.replaceTextRange(change.start, change.length, change.replacedText);
		currentText.setCaretOffset(change.start + change.replacedText.length());
		scrollToCaret();
	}

	private boolean isFirstLineOnPage(int index)
	{
		if(linesPerPage == 0)
			return false;
		return index % linesPerPage == 0;
	}

	/**
	 * <p>
	 * Reads data in BRF format from <code>Reader</code>.
	 * </p><p>
	 * An attempt is made to determine the number of lines per page.
	 * </p>
	 *
	 * @param reader the reader stream from which to read the data.
	 *
	 * @exception IOException
	 *
	 * @see #writeBRF(Writer)
	 */
	public void readBRF(Reader reader) throws IOException
	{
		StringBuilder stringBuilder = new StringBuilder(65536);
		boolean checkLinesPerPage = true;
		boolean removeFormFeed = true;
		char buffer[] = new char[65536];
		int cnt, trim;

		eol = null;
		while((cnt = reader.read(buffer)) > 0)
		{
			//   see if lines per page can be determined
			if(checkLinesPerPage)
			{
				checkLinesPerPage = false;
				int lines = 0, i;
				outer:for(i = 0; i < cnt; i++)
				switch(buffer[i])
				{
				case '\n':  lines++;  break;

				case '\r':

					if(eol == null)
						eol = "\r\n";
					break;

				case 0xc:

					linesPerPage = lines;
					break outer;
				}

				if(eol == null)
					eol = "\n";
				if(i == cnt)
					removeFormFeed = false;
			}

			//   remove form feeds
			if(removeFormFeed)
			{
				trim = 0;
				for(int i = 0; i < cnt; i++)
				if(buffer[i] != 0xc)
				{
					buffer[trim] = buffer[i];
					trim++;
				}
			}
			else
				trim = cnt;

			stringBuilder.append(buffer, 0, trim);
		}

		content.setText(stringBuilder.toString());
		clearChanges();
	}

	/**
	 * <p>
	 * Writes data in BRF format to <code>Writer</code>.
	 * </p>
	 *
	 * @param writer the writer stream to write the data.
	 *
	 * @exception IOException
	 *
	 * @see #readBRF(Reader)
	 */
	public void writeBRF(Writer writer) throws IOException
	{
		//   write first line
		String line = content.getLine(0);
		if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
			writer.write(line.substring(0, line.length() - 1));
		else
			writer.write(line);

		//   write remaining lines
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
		resetChanges();
	}

	/**
	 * <p>
	 * Reads data in BrailleZephyr file format from <code>Reader</code>.
	 * </p>
	 *
	 * @param reader the reader stream from which to read the data.
	 *
	 * @exception IOException
	 *
	 * @see #writeBZY(Writer)
	 */
	public void readBZY(Reader reader) throws IOException, BZException
	{
		String line;
		boolean returnAtEnd = false;
		int caretOffset = 0;
		int unknown = 0;

		content.setText("");
		eol = System.getProperty("line.separator");
		BufferedReader buffer = new BufferedReader(reader);

		//   read configuration lines
		header:while((line = buffer.readLine()) != null)
		{
			String tokens[] = line.split(" ");
			switch(tokens[0])
			{

			//   don't do anything for now
			case "Version":  break;

			case "CharsPerLine":  charsPerLine = Integer.parseInt(tokens[1]);  break;
			case "LinesPerPage":  linesPerPage = Integer.parseInt(tokens[1]);  break;

			case "CaretOffset":  caretOffset  = Integer.parseInt(tokens[1]);  break;
			case "ViewFocus":

				if(tokens[1].equals("braille"))
				{
					if(brailleText.isVisible())
						brailleText.setFocus();
				}
				else if(tokens[1].equals("ascii"))
				{
					if(asciiText.isVisible())
						asciiText.setFocus();
				}
				else
					logWriter.println("ERROR:  Invalid ViewFocus value:  " + line);
				break;

			case "ReturnAtEnd":  returnAtEnd = Boolean.parseBoolean(tokens[1]); break;

			case "HeaderEnd":  break header;

			default:

				logWriter.println("WARNING:  Unknown file format parameter:  " + line);
				unknown++;
				if(unknown > 6)
					throw new BZException("Invalid file format");
				break;
			}
		}
		if(line == null)
			throw new BZException("Invalid file format");

		//   read first line, may be null for empty document
		if((line = buffer.readLine()) != null)
		{
			if(line.length() > 0 && line.charAt(line.length() - 1) == 0xb6)
				content.replaceTextRange(0, 0, line.substring(0, line.length() - 1) + PARAGRAPH_END);
			else
				content.replaceTextRange(0, 0, line);

			//   read next lines
			while((line = buffer.readLine()) != null)
			{
				content.replaceTextRange(content.getCharCount(), 0, eol);
				if(line.length() > 0 && line.charAt(line.length() - 1) == 0xb6)
					content.replaceTextRange(content.getCharCount(), 0, line.substring(0, line.length() - 1) + PARAGRAPH_END);
				else
					content.replaceTextRange(content.getCharCount(), 0, line);
			}
		}

		if(returnAtEnd)
			content.replaceTextRange(content.getCharCount(), 0, eol);

		clearChanges();
		brailleText.setCaretOffset(caretOffset);
		asciiText.setCaretOffset(caretOffset);
	}

	/**
	 * <p>
	 * Writes data in BrailleZephyr file format to <code>Writer</code>.
	 * </p>
	 *
	 * @param writer the writer stream to write the data.
	 *
	 * @exception IOException
	 *
	 * @see #readBZY(Reader)
	 */
	public void writeBZY(Writer writer) throws IOException
	{
		//   write configuration lines
		writer.write("Version " + versionMajor + ' ' + versionMinor + ' ' + versionPatch + eol);

		writer.write("CharsPerLine " + charsPerLine + eol);
		writer.write("LinesPerPage " + linesPerPage + eol);

		writer.write("CaretOffset " + currentText.getCaretOffset() + eol);
		writer.write("ViewFocus ");
		if(currentText == brailleText)
			writer.write("braille" + eol);
		else
			writer.write("ascii" + eol);

		if(content.getCharCount() > 0)
			writer.write("ReturnAtEnd " + (content.getLine(content.getLineCount() - 1).length() == 0) + eol);
		else
			writer.write("ReturnAtEnd false" + eol);

		writer.write("HeaderEnd" + eol);

		//   write first line
		String line = content.getLine(0);
		if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
			writer.write(line.substring(0, line.length() - 1) + (char)0xb6);
		else
			writer.write(line);

		//   write text
		for(int i = 1; i < content.getLineCount(); i++)
		{
			writer.write(eol);
			line = content.getLine(i);
			if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
				writer.write(line.substring(0, line.length() - 1) + (char)0xb6);
			else
				writer.write(line);
		}

		writer.flush();
		resetChanges();
	}

	/**
	 * <p>
	 * Wraps lines at and below the caret that exceed the number of
	 * characters per line.
	 * </p><p>
	 * Lines are wrapped at spaces between words when possible.  Lines that
	 * don't exceed the number of characters per line are not changed.
	 * </p><p>
	 * Currently this cannot be undone.
	 * </p>
	 */
	public void rewrapFromCaret()
	{
		StringBuilder stringBuilder = new StringBuilder(charsPerLine * 3);

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
				stringBuilder.setLength(0);
				stringBuilder.append(line.substring(0, wordEnd)).append(eol).append(line.substring(wordWrap, length));
				if(length > 0 && line.charAt(length - 1) != PARAGRAPH_END)
				if(i < content.getLineCount() - 1)
				{
					String next = content.getLine(i + 1);
					stringBuilder.append(' ').append(next);
					length += eol.length() + next.length();
				}

				content.replaceTextRange(content.getOffsetAtLine(i), length, stringBuilder.toString());
			}
			else if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
				break;
		}

		clearChanges();
	}

	private final class FocusHandler implements FocusListener
	{
		private final StyledText source;

		private FocusHandler(StyledText source)
		{
			this.source = source;
		}

		@Override
		public void focusGained(FocusEvent ignored)
		{
			currentText = source;
		}

		@Override
		public void focusLost(FocusEvent ignored){}
	}

	private final class CaretHandler implements CaretListener
	{
		private final StyledText source, other;

		private int prevCaretOffset, prevLineIndex;

		private CaretHandler(StyledText source, StyledText other)
		{
			this.source = source;
			this.other = other;
		}

		@Override
		public void caretMoved(CaretEvent ignored)
		{
			int caretOffset = source.getCaretOffset();
			int lineIndex = source.getLineAtOffset(caretOffset);
			int lineOffset = source.getOffsetAtLine(lineIndex);

			//   play line margin bell
			if(lineMarginClip != null && lineMarginBell > 0 && caretOffset == prevCaretOffset + 1)
			{
				if(caretOffset - lineOffset == lineMarginBell)
				if(!lineMarginClip.isActive())
				{
					lineMarginClip.setFramePosition(0);
					lineMarginClip.start();
				}
			}

			//   play line end bell
			if(lineEndClip != null && charsPerLine > 0 && caretOffset == prevCaretOffset + 1)
			{
				if(caretOffset - lineOffset == charsPerLine)
					if(!lineEndClip.isActive())
					{
						lineEndClip.setFramePosition(0);
						lineEndClip.start();
					}
			}

			prevCaretOffset = caretOffset;

			//   scroll other text to match current
			if(lineIndex == prevLineIndex)
				return;
			prevLineIndex = lineIndex;
			if(source != currentText)
				return;

			int sourceLinePixel = source.getLinePixel(lineIndex);
			int sourceHeight = source.getClientArea().height;
			int sourceLineHeight = source.getLineHeight();

			//   check if have to wait until after paint event
			if(sourceLinePixel < 0 || sourceLinePixel + sourceLineHeight > sourceHeight)
				adjustOtherThread.waitPainted(source, other);
			else
				adjustOtherThread.adjustOther(source, other);
		}
	}

	/**
	 * <p>
	 * The caretMoved event may trigger a paint event.  The getLinePixel method
	 * inside the caretMoved will return the pixel before the object has been
	 * painted, and the value after it has been painted is needed to adjust the
	 * other view.  So the object of this class will be notified after a paint
	 * event has occurred and it will adjust the other view then.  Care must be
	 * taken as some caretMoved events do not result in the object being
	 * redrawn.
	 * </p>
	 */
	private class AdjustOtherThread implements Runnable
	{
		private volatile boolean paintEvent;
		private StyledText source, other;
		private int tries;

		private synchronized void adjustOther(StyledText source, StyledText other)
		{
			if(source == null)
				source = this.source;
			if(other == null)
				other = this.other;

			if(source == null)
			{
				logWriter.println("WARNING:  attempting to adjust source but no source StyledText");
				return;
			}
			if(other == null)
			{
				logWriter.println("WARNING:  attempting to adjust other but no other StyledText");
				return;
			}

			int caretOffset = source.getCaretOffset();
			int lineIndex = source.getLineAtOffset(caretOffset);
			int otherLineHeight = other.getLineHeight();
			int otherLineRealPixel = lineIndex * otherLineHeight;
			int sourceLinePixel = source.getLinePixel(lineIndex);
			int otherTopPixel = otherLineRealPixel - sourceLinePixel;

			int otherHeight = other.getClientArea().height;
			int otherLineBelow = otherHeight - (sourceLinePixel + otherLineHeight);

			int otherLineCount = other.getLineCount();
			int otherLinesBelow = otherLineCount - lineIndex;
			int otherLinesBelowHeight = otherLinesBelow * otherLineHeight;
			int otherBottomGap = otherHeight - (sourceLinePixel + otherLinesBelowHeight);

			//   other would have to scroll before first line
			if(otherTopPixel < 0)
			{
				int sourceTopPixel = source.getTopPixel() - otherTopPixel;
				otherTopPixel = 0;
				source.setTopPixel(sourceTopPixel);
			}

			//   other line would be partially past the bottom of view
			else if(otherLineBelow < 0)
			{
				int sourceTopPixel = source.getTopPixel() - otherLineBelow;
				otherTopPixel -= otherLineBelow;
				source.setTopPixel(sourceTopPixel);
			}

			//   other would have to scroll past last line
			else if(otherBottomGap > 0)
			{
				int sourceTopPixel = source.getTopPixel() - otherBottomGap;
				otherTopPixel -= otherBottomGap;
				source.setTopPixel(sourceTopPixel);
			}

			other.setTopPixel(otherTopPixel);
			redraw();
		}

		private synchronized void waitPainted(StyledText source, StyledText other)
		{
			if(this.source != null)
			{
				logWriter.println("WARNING:  attempting to wait to adjust other but already waiting");
				return;
			}

			this.source = source;
			this.other = other;
			tries = 1;
			paintEvent = false;
			parentShell.getDisplay().asyncExec(this);
		}

		private synchronized void notifyPainted(StyledText source)
		{
			if(this.source == source)
				paintEvent = true;
		}

		@Override
		public synchronized void run()
		{
			if(!paintEvent)
			{
				logWriter.println("adjusting other try #" + ++tries);
				if(tries > 2)
				{
					logWriter.println("ERROR:  adjusting other failed");
					return;
				}
				parentShell.getDisplay().asyncExec(this);
				return;
			}

			adjustOther(null, null);
			source = other = null;
		}
	}

	private final class PaintHandler implements PaintListener
	{
		private final StyledText source;

		private int charsPerLine, rightMargin;

		private PaintHandler(StyledText source)
		{
			this.source = source;
		}

		@Override
		public void paintControl(PaintEvent event)
		{
			/*   Using event.gc.getFontMetrics().getAverageCharWidth()) was not
			     enough on low resolutions, as the rounding to an int seemed
			     enough to screw up the right margin, even when using a
			     monospaced font (this is the current theory).
			 */
			if(charsPerLine != getCharsPerLine())
			{
				charsPerLine = getCharsPerLine();
				char buffer[] = new char[charsPerLine];
				for(int i = 0; i < charsPerLine; i++)
					buffer[i] = 'm';
				rightMargin = event.gc.stringExtent(new String(buffer)).x;
			}

			int lineHeight = source.getLineHeight();
			int drawHeight = source.getClientArea().height;
			int drawWidth = source.getClientArea().width;

			event.gc.setForeground(color);
			event.gc.setBackground(color);

			//   draw right margin
			event.gc.drawLine(rightMargin, 0, rightMargin, drawHeight);

			for(int i = source.getTopIndex(); i < source.getLineCount(); i++)
			{
				//   draw page lines
				int at = source.getLinePixel(i);
				if(isFirstLineOnPage(i))
					event.gc.drawLine(0, at, drawWidth, at);

				//   draw paragraph end markers
				String line = source.getLine(i);
				if(line.length() > 0 && line.charAt(line.length() - 1) == PARAGRAPH_END)
				{
					Point point = event.gc.stringExtent(line);
					int span = point.y / 2;
					event.gc.fillOval(point.x + span / 2, at + span / 2, span, span);
				}

				//   check if line still visible
				if(at + lineHeight > drawHeight)
					break;
			}

			adjustOtherThread.notifyPainted(source);
		}
	}

	private final class BrailleKeyHandler implements KeyListener, VerifyKeyListener
	{
		private static final String ASCII_BRAILLE = " A1B'K2L@CIF/MSP\"E3H9O6R^DJG>NTQ,*5<-U8V.%[$+X!&;:4\\0Z7(_?W]#Y)=";
		private final boolean brailleEntry;

		private char dotState, dotChar = 0x2800;
		private int prevLine;

		private BrailleKeyHandler(boolean brailleEntry)
		{
			this.brailleEntry = brailleEntry;
		}

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

			//   insert resulting braille character
			if(dotState == 0 && (dotChar & 0xff) != 0)
			{
				dotChar = ASCII_BRAILLE.charAt(dotChar & 0xff);
				brailleText.insert(Character.toString(dotChar));
				brailleText.setCaretOffset(brailleText.getCaretOffset() + 1);
				dotChar = 0x2800;
			}
		}

		@Override
		public void verifyKey(VerifyEvent event)
		{
			StyledText styledText = (StyledText)event.widget;

			if(event.keyCode == '\r' || event.keyCode == '\n')
			if((event.stateMask & SWT.SHIFT) != 0)
			{
				//   toggle paragraph end character
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
			else
			{
				//   play page bell
				int index = styledText.getLineAtOffset(styledText.getCaretOffset());
				if(index == prevLine + 1 && index % linesPerPage == pageMarginBell - 2)
				if(!pageMarginClip.isActive())
				{
					pageMarginClip.setFramePosition(0);
					pageMarginClip.start();
				}
				prevLine = index;
			}

			//   check if using braille entry
			if(brailleEntry)
			if(event.character > ' ' && event.character < 0x7f)
				event.doit = false;
		}
	}

	private final class ExtendedModifyHandler implements ExtendedModifyListener
	{
		private final StyledText source;

		//TODO:  should prev variables be reset on setText?
		private int prevLineCount;

		private ExtendedModifyHandler(StyledText source)
		{
			this.source = source;
		}

		@Override
		public void modifyText(ExtendedModifyEvent event)
		{
			//TODO:  is this ever not true?
			if(source != currentText)
				return;

			if(undoing)
				changes.add(changeIndex, event);
			else if(redoing)
				changes.add(changeIndex++, event);
			else
			{
				if(changeIndex < changes.size())
					changes.subList(changeIndex, changes.size()).clear();
				changes.add(changeIndex++, event);
			}
			undoing = redoing = false;

			//   need to redraw page lines
			int lineCount = source.getLineCount();
			if(lineCount != prevLineCount)
				redraw();
			prevLineCount = lineCount;
		}
	}
}
