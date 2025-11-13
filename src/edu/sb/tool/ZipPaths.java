package edu.sb.tool;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;


/**
 * This facade provides operations that return a {@link Path}
 * by converting a ZIP path string or {@link URI}.
 * @see Paths
 */
@Copyright(year=2021, holders="Sascha Baumeister")
public final class ZipPaths {
	static private final byte[] EMPTY_ZIP_CONTENT = { 80, 75, 5, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	static private final FileSystem ZIP_FILE_SYSTEM;
	static {
		try {
			final Path path = Files.createTempFile("temp-", ".zip");
			try {
				Files.write(path, EMPTY_ZIP_CONTENT);
				ZIP_FILE_SYSTEM = FileSystems.newFileSystem(path, Collections.emptyMap(), null);
			} finally {
				Files.delete(path);
			}
		} catch (final IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}


	/**
	 * Prevents external instantiation.
	 */
	private ZipPaths () {};


	/**
	 * Converts a path string, or a sequence of strings that when joined form a path string, to a ZIP {@code Path}.
	 * @param first the path string or initial part of the path string
	 * @param more additional strings to be joined to form the path string
	 * @return the resulting {@code Path}
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws InvalidPathException if the path string cannot be converted to a {@code Path}
	 * @see FileSystem#getPath
	 * @see java.nio.file.Paths#get(String,String...)
	 */
	static public Path get (final String first, final String... more) throws NullPointerException, InvalidPathException {
		return ZIP_FILE_SYSTEM.getPath(first, more);
	}


	/**
	 * Converts the given URI to a ZIP {@link Path} object.
	 * @param uri the URI to convert
	 * @return the resulting {@code Path}
	 * @throws NullPointerException if the {@code uri} argument is {@code null}
	 * @throws IllegalArgumentException if preconditions on the {@code uri} parameter do not hold.
	 * @throws FileSystemNotFoundException The file system, identified by the URI, does not exist and cannot be created
	 *         automatically, or the provider identified by the URI's scheme component is not installed
	 * @see Paths#get(URI)
	 */
	static public Path get (final URI uri) throws NullPointerException, IllegalArgumentException, FileSystemNotFoundException {
		return ZIP_FILE_SYSTEM.provider().getPath(uri);
	}


	/**
	 * Returns whether or not the given path is a ZIP path.
	 * @param path the path
	 * @return {@code true} if the given argument is a ZIP path, otherwise {@code false} 
	 * @throws NullPointerException if the {@code path} argument is {@code null}
	 */
	static public boolean isZipPath (final Path path) throws NullPointerException, IllegalArgumentException, FileSystemNotFoundException {
		return "jdk.nio.zipfs.ZipPath".equals(path.getClass().getName());
	}
}
