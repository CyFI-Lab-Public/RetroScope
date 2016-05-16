/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.ServerHostKeyVerifier;

class SimpleVerifier implements ServerHostKeyVerifier
{
	KnownHosts database;

	/*
	 * This class is being used by the UsingKnownHosts.java example.
	 */
	
	public SimpleVerifier(KnownHosts database)
	{
		if (database == null)
			throw new IllegalArgumentException();

		this.database = database;
	}

	public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey)
			throws Exception
	{
		int result = database.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);

		switch (result)
		{
		case KnownHosts.HOSTKEY_IS_OK:

			return true; // We are happy

		case KnownHosts.HOSTKEY_IS_NEW:

			// Unknown host? Blindly accept the key and put it into the cache.
			// Well, you definitely can do better (e.g., ask the user).

			// The following call will ONLY put the key into the memory cache!
			// To save it in a known hosts file, also call "KnownHosts.addHostkeyToFile(...)"
			database.addHostkey(new String[] { hostname }, serverHostKeyAlgorithm, serverHostKey);

			return true;

		case KnownHosts.HOSTKEY_HAS_CHANGED:

			// Close the connection if the hostkey has changed.
			// Better: ask user and add new key to database.
			return false;

		default:
			throw new IllegalStateException();
		}
	}
}