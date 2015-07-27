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
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class BZStyledText
{
	private StyledText styledText;

	private int linesPerPage = 10;

	private Color pageSeparatorColor;

	BZStyledText(Shell shell)
	{
		styledText = new StyledText(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		styledText.setLayoutData(new GridData(GridData.FILL_BOTH));
		styledText.addListener(SWT.Paint, new BZStyledTextPaintListener());

		Font font = new Font(styledText.getDisplay(), "SimBraille", 15, SWT.NORMAL);
		if(font != null)
			styledText.setFont(font);

		pageSeparatorColor = styledText.getDisplay().getSystemColor(SWT.COLOR_BLACK);
	}

	class BZStyledTextPaintListener implements Listener
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
				if((i % linesPerPage) == 0)
					event.gc.drawLine(0, at, drawWidth, at);
				if(at + lineHeight > drawHeight)
					break;
			}
		}
	}

	void openFile()
	{
		FileDialog dialog = new FileDialog(styledText.getShell(), SWT.OPEN);
		String fileName = dialog.open();
		if(fileName == null)
			return;

		try
		{
			FileReader fileReader = new FileReader(fileName);
			char buffer[] = new char[65536];
			int cnt;
			while((cnt = fileReader.read(buffer)) > 0)
				styledText.setText(new String(buffer));
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	void openFont()
	{
		FontDialog dialog = new FontDialog(styledText.getShell(), SWT.OPEN);
		dialog.setFontList(styledText.getFont().getFontData());
		FontData fontData = dialog.open();
		if(fontData == null)
			return;
		styledText.setFont(new Font(styledText.getDisplay(), fontData));
	}
}

