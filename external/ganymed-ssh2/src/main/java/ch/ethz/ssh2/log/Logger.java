/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.log;

import java.util.logging.Level;

/**
 * Logger delegating to JRE logging.
 *
 * @author Christian Plattner
 * @version $Id: Logger.java 41 2011-06-02 10:36:41Z dkocher@sudo.ch $
 */
public class Logger
{

	private java.util.logging.Logger delegate;

	public static Logger getLogger(Class x)
	{
		return new Logger(x);
	}

	public Logger(Class x)
	{
		this.delegate = java.util.logging.Logger.getLogger(x.getName());
	}

	public boolean isDebugEnabled()
	{
		return delegate.isLoggable(Level.FINER);
	}

	public void debug(String message)
	{
		delegate.fine(message);
	}

	public boolean isInfoEnabled()
	{
		return delegate.isLoggable(Level.FINE);
	}

	public void info(String message)
	{
		delegate.info(message);
	}

	public boolean isWarningEnabled()
	{
		return delegate.isLoggable(Level.WARNING);
	}

	public void warning(String message)
	{
		delegate.warning(message);
	}
}