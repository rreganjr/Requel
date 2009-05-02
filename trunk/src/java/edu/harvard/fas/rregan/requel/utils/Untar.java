/*
 * $Id: Untar.java,v 1.1 2008/10/20 02:07:50 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarEntry;
import org.apache.commons.compress.archivers.tar.TarInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Loosely based on the Apache Ant Untar class.
 * 
 * @author ron
 */
public class Untar {
	private static final Logger log = Logger.getLogger(Untar.class);

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
		TarInputStream tis = new TarInputStream(archiveInputStream);
		TarEntry entry = null;
		while ((entry = tis.getNextEntry()) != null) {
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
	private static void extractFile(File destinationDir, TarInputStream inputStream,
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
				inputStream.copyEntryContents(outputStream);
			} finally {
				IOUtils.closeQuietly(outputStream);
			}
		}
	}
}
