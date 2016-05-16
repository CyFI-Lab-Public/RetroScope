/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;

import java.math.BigInteger;

/**
 * PacketKexDHInit.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class PacketKexDHInit
{
	byte[] payload;

	BigInteger e;

	public PacketKexDHInit(BigInteger e)
	{
		this.e = e;
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_KEXDH_INIT);
			tw.writeMPInt(e);
			payload = tw.getBytes();
		}
		return payload;
	}
}
