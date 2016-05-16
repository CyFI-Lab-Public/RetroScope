/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class UsingKnownHosts
{
	static KnownHosts database = new KnownHosts();

	public static void main(String[] args) throws IOException
	{
		String hostname = "somehost";
		String username = "joe";
		String password = "joespass";

		File knownHosts = new File("~/.ssh/known_hosts");

		try
		{
			/* Load known_hosts file into in-memory database */

			if (knownHosts.exists())
				database.addHostkeys(knownHosts);

			/* Create a connection instance */

			Connection conn = new Connection(hostname);

			/* Now connect and use the SimpleVerifier */

			conn.connect(new SimpleVerifier(database));

			/* Authenticate */

			boolean isAuthenticated = conn.authenticateWithPassword(username, password);

			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");

			/* Create a session */

			Session sess = conn.openSession();

			sess.execCommand("uname -a && date && uptime && who");

			InputStream stdout = new StreamGobbler(sess.getStdout());
			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

			System.out.println("Here is some information about the remote host:");

			while (true)
			{
				String line = br.readLine();
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
