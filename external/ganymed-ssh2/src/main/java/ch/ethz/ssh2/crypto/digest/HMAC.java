/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.crypto.digest;

/**
 * HMAC.
 *
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public final class HMAC implements Digest
{
	Digest md;
	byte[] k_xor_ipad;
	byte[] k_xor_opad;

	byte[] tmp;

	int size;

	public HMAC(Digest md, byte[] key, int size)
	{
		this.md = md;
		this.size = size;

		tmp = new byte[md.getDigestLength()];

		int blocksize = 64;

		k_xor_ipad = new byte[blocksize];
		k_xor_opad = new byte[blocksize];

		if (key.length > blocksize)
		{
			md.reset();
			md.update(key);
			md.digest(tmp);
			key = tmp;
		}

		System.arraycopy(key, 0, k_xor_ipad, 0, key.length);
		System.arraycopy(key, 0, k_xor_opad, 0, key.length);

		for (int i = 0; i < blocksize; i++)
		{
			k_xor_ipad[i] ^= 0x36;
			k_xor_opad[i] ^= 0x5C;
		}
		md.update(k_xor_ipad);
	}

	public int getDigestLength()
	{
		return size;
	}

	public void update(byte b)
	{
		md.update(b);
	}

	public void update(byte[] b)
	{
		md.update(b);
	}

	public void update(byte[] b, int off, int len)
	{
		md.update(b, off, len);
	}

	public void reset()
	{
		md.reset();
		md.update(k_xor_ipad);
	}

	public void digest(byte[] out)
	{
		digest(out, 0);
	}

	public void digest(byte[] out, int off)
	{
		md.digest(tmp);

		md.update(k_xor_opad);
		md.update(tmp);

		md.digest(tmp);

		System.arraycopy(tmp, 0, out, off, size);

		md.update(k_xor_ipad);
	}
}
