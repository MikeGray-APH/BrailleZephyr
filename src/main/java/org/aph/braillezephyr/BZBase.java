package org.aph.braillezephyr;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 *
 * @author Mike Gray mgray@aph.org
 */
public class BZBase
{
	protected final BZStyledText bzStyledText;
	protected final Shell parentShell;

	public BZBase(BZStyledText bzStyledText)
	{
		this.bzStyledText = bzStyledText;
		parentShell = bzStyledText.getParentShell();
	}

	protected void logError(String string, boolean showMessage)
	{
		System.err.println("ERROR:  " + string);
		bzStyledText.getLogWriter().println("ERROR:  " + string);
		if(showMessage)
		{
			MessageBox messageBox = new MessageBox(parentShell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage("ERROR:  " + string);
			messageBox.open();
		}
	}

	protected void logError(String string)
	{
		logError(string, true);
	}

	protected void logMessage(String string)
	{
		System.out.println(string);
		bzStyledText.getLogWriter().println(string);
	}
}
