package org.aph.braillezephyr;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * <p>
 * This class is the base class for the other BZ* classes.  It contains the
 * common logging methods used by the other classes.
 * </p>
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

	protected void logError(String message, String info, boolean showMessage)
	{
		String string;

		if(info == null)
			string = "ERROR:  " + message;
		else
			string = "ERROR:  " + message + ":  " + info;
		System.err.println(string);
		System.err.flush();
		bzStyledText.getLogWriter().println(string);
		bzStyledText.getLogWriter().flush();

		if(showMessage)
		{
			if(info == null)
				string = "ERROR:  " + message;
			else
				string = "ERROR:  " + message + ":\n" + info;
			MessageBox messageBox = new MessageBox(parentShell, SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage(string);
			messageBox.open();
		}
	}

	protected void logError(String message, Exception exception, boolean showMessage)
	{
		logError(message, exception.getMessage(), showMessage);
	}

	protected void logError(String message, Exception exception)
	{
		logError(message, exception, true);
	}

	protected void logError(String message, String info)
	{
		logError(message, info, true);
	}

	protected void logError(String message, boolean showMessage)
	{
		logError(message, (String)null, showMessage);
	}

	protected void logError(String message)
	{
		logError(message, true);
	}

	protected void logMessage(String string)
	{
		System.out.println(string);
		bzStyledText.getLogWriter().println(string);
		bzStyledText.getLogWriter().flush();
	}
}
