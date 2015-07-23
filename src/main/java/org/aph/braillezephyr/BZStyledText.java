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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class BZStyledText
{
	private StyledText styledText;

	BZStyledText(Shell shell)
	{
		styledText = new StyledText(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		styledText.setLayoutData(new GridData(GridData.FILL_BOTH));
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
				styledText.append(new String(buffer));
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
}
