/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr;

import java.security.MessageDigest;

/**
 * HashUtils
 * 
 * @author ron
 */
public class HashUtils {

	private static final char[] hexadecimal = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * calculate the MD5 hash on the supplied input bytes and return it as a 16
	 * byte array.
	 * 
	 * @param input
	 *            a byte array
	 * @return the 128bit digest
	 * @throws ApplicationException
	 */
	public static byte[] getMD5HashDigest(byte[] input) throws ApplicationException {
		try {
			return MessageDigest.getInstance("MD5").digest(input);
		} catch (Exception e) {
			throw new ApplicationException(e, "Exception creating MD5 hash digest");
		}
	}

	/**
	 * @param input
	 *            a string
	 * @return a 32 character string representation of the 128bit digest
	 *         compatable with the results of
	 *         org.apache.catalina.realm.RealmBase digest method
	 * @throws ApplicationException
	 */
	public static String getMD5HashDigestString(String input) throws ApplicationException {
		return encode(getMD5HashDigest(input.getBytes()));
	}

	/**
	 * Encodes the 128 bit (16 bytes) MD5 into a 32 character String.
	 * 
	 * @param binaryData
	 *            Array containing the digest
	 * @return Encoded MD5, or null if encoding failed This was taken from
	 *         org.apache.catalina.util.MD5Encoder so that the hash created/used
	 *         by the Tomcat container managed authentication matches what is
	 *         used by the user management to reset passwords.
	 */
	private static String encode(byte[] binaryData) {

		if (binaryData.length != 16) {
			return null;
		}

		char[] buffer = new char[32];

		for (int i = 0; i < 16; i++) {
			int low = (binaryData[i] & 0x0f);
			int high = ((binaryData[i] & 0xf0) >> 4);
			buffer[i * 2] = hexadecimal[high];
			buffer[i * 2 + 1] = hexadecimal[low];
		}

		return new String(buffer);

	}

}
