/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class StdoutAndStderr
{
	public static void main(String[] args)
	{
		String hostname = "127.0.0.1";
		String username = "joe";
		String password = "joespass";

		try
		{
			/* Create a connection instance */

			Connection conn = new Connection(hostname);

			/* Now connect */

			conn.connect();

			/* Authenticate */

			boolean isAuthenticated = conn.authenticateWithPassword(username, password);

			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");

			/* Create a session */

			Session sess = conn.openSession();

			sess.execCommand("echo \"Text on STDOUT\"; echo \"Text on STDERR\" >&2");

			InputStream stdout = new StreamGobbler(sess.getStdout());
			InputStream stderr = new StreamGobbler(sess.getStderr());
	
			BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout));
			BufferedReader stderrReader = new BufferedReader(new InputStreamReader(stderr));
			
			System.out.println("Here is the output from stdout:");
	
			while (true)
			{
				String line = stdoutReader.readLine();
				if (line == null)
					break;
				System.out.println(line);
			}
			
			System.out.println("Here is the output from stderr:");
			
			while (true)
			{
				String line = stderrReader.readLine();
				if (line == null)
					break;
				System.out.println(line);
			}
			
			/* Close this session */
			
			sess.close();

			/* Close the connection */

			conn.close();

		}
		catch (IOException e)
		{
			e.printStackTrace(System.err);
			System.exit(2);
		}
	}
}
