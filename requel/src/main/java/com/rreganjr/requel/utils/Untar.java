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
package com.rreganjr.requel.utils;

import java.io.*;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loosely based on the Apache Ant Untar class.
 * 
 * @author ron
 */
public class Untar {
	private static final Log log = LogFactory.getLog(Untar.class);

	private Untar() {
	}

	/**
	 * Extract the given tar file to the destination directory.
	 * 
	 * @param archiveFile -
	 *            a tar file that may be gzipped.
	 * @param destinationDirectory -
	 *            a directory to untar the file. If it doesn't exist it will be
	 *            created.
	 * @throws IOException
	 */
	public static void untar(File archiveFile, File destinationDirectory) throws IOException {
		log.debug("extracting " + archiveFile + " to " + destinationDirectory);
		destinationDirectory.mkdirs();
		InputStream archiveInputStream = new FileInputStream(archiveFile);
		if (archiveFile.getName().endsWith(".gz") || archiveFile.getName().endsWith(".tgz")) {
			archiveInputStream = new GZIPInputStream(archiveInputStream);
		}
		TarArchiveInputStream tis = new TarArchiveInputStream(archiveInputStream);
		TarArchiveEntry entry = null;
		while ((entry = tis.getNextTarEntry()) != null) {
			log.debug("extracting " + entry.getName());
			extractFile(destinationDirectory, tis, entry.getName(), entry.isDirectory());
		}
	}

	/**
	 * extract a file to a directory
	 * 
	 * @param destinationDir
	 *            the root destination directory to untar the contents of the
	 *            archive.
	 * @param inputStream
	 *            the tar input stream ( apache commons-compress TarInputStream)
	 * @param newFileName
	 *            the name of the new file or directory
	 * @param entryIsDirectory
	 *            if this is true the entry is a directory
	 * @throws IOException
	 *             on error
	 */
	private static void extractFile(File destinationDir, TarArchiveInputStream inputStream,
			String newFileName, boolean entryIsDirectory) throws IOException {

		File newFile = new File(destinationDir, newFileName);

		if (entryIsDirectory) {
			newFile.mkdirs();
		} else {
			// create intermediary directories - sometimes zip don't add them
			File newFileParentDir = newFile.getParentFile();
			if (newFileParentDir != null) {
				newFileParentDir.mkdirs();
			}
			FileOutputStream outputStream = null;
			try {
				outputStream = new FileOutputStream(newFile);
				// from http://grepcode.com/file_/repo1.maven.org/maven2/ant/ant/1.5.3-1/org/apache/tools/tar/TarInputStream.java/?v=source
                // copyEntryContents()
                byte[] buf = new byte[32 * 1024];
                while (true) {
                    int numRead = inputStream.read(buf, 0, buf.length);
                    if (numRead == -1) {
                        break;
                    }
                    outputStream.write(buf, 0, numRead);
                }
			} finally {
				IOUtils.closeQuietly(outputStream);
			}
		}
	}
}
