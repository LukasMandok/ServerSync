package com.superzanti.serversync.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

import runme.Main;

/**
 * Helper for working with paths and directories
 * 
 * @author Rheimus
 *
 */
public class PathUtils {

	private static StringBuilder pathBuilder;

	/**
	 * Go to last occurrence of target in path
	 * 
	 * @param target
	 *            what directory you want to go to
	 * @param path
	 *            the absolute path of your current directory
	 * @param offset
	 *            walk further up the chain
	 * @return String rebuilt up to the target directory <br>
	 * 
	 *         <pre>
	 * e.g. 1) target = foo, path = bar/foo/ring/ding/ 
	 * returns: bar/foo/ <br>     2) target = foo, path = bar/foo/ring/ding, offset = 1
	 * returns: bar/
	 *         </pre>
	 */
	public static String walkTo(String target, String path, int offset) {
		List<String> pathParts = getPathParts(path);
		target = target.toLowerCase();

		pathBuilder = new StringBuilder();
		int locationOfTarget = pathParts.lastIndexOf(target);

		ListIterator<String> iter = pathParts.listIterator();

		while (iter.hasNext()) {
			String part = iter.next();
			int index = iter.nextIndex();
			if (part.equalsIgnoreCase(target) && index >= locationOfTarget) {
				pathBuilder.append(part);
				pathBuilder.append("/");
				System.out.println("found target");
				System.out.println(pathBuilder.toString());
				break;
			} else {
				pathBuilder.append(part);
				pathBuilder.append("/");
				System.out.println("appended: " + part);
			}
		}

		if (offset > 0) {
			try {
				return walkUp(offset, pathBuilder.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return pathBuilder.toString();
	}

	/**
	 * Go up offset number of parts in path
	 * 
	 * @param offset
	 *            how far to walk up the path
	 * @param path
	 *            the absolute path of your current directory
	 * @return String rebuilt up to the offset amount <br>
	 * 
	 *         <pre>
	 * e.g. offset = 2, path = bar/foo/ring/ding/ 
	 * returns: bar/foo/
	 *         </pre>
	 * 
	 * @throws Exception
	 *             if offset is longer than path
	 */
	public static String walkUp(int offset, String path) throws Exception {
		List<String> pathParts = getPathParts(path);
		int ppLen = pathParts.size();
		pathBuilder = new StringBuilder();

		if (offset > ppLen) {
			throw new Exception("Offset is longer than path chain");
		}

		for (int i = 0; i < ppLen - offset; i++) {
			pathBuilder.append(pathParts.get(i));
			pathBuilder.append("/");
		}
		return pathBuilder.toString();
	}

	/**
	 * Uses Java reflection magic and ServerSync's {@linkplain Main} class to get
	 * jar file as {@linkplain File} object.
	 * 
	 * @return ServerSync jar file
	 */
	public static File getServerSyncFile() {
		return new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
	}

	/**
	 * Tries to guess Minecraft directory intelligently.
	 * 
	 * @return Minecraft directory location as {@link Path} object
	 */
	public static String getMinecraftDirectory() {
		File jarFile = getServerSyncFile();
		String jarFilePath = jarFile.getAbsolutePath();

		List<String> parts = Arrays.asList(jarFilePath.split("[\\\\/]"));
		
		if (parts.contains("file:")) {
			// Shift past the file declaration when loaded in a forge environment
			parts = parts.subList(parts.indexOf("file:") + 1, parts.size() - 1);
		}

		if (parts.contains("mods")) {
			// ASSUMPTION: We are most likely in the mods directory of a minecraft directory
			List<String> root = parts.subList(0, parts.indexOf("mods"));
			PathBuilder builder = new PathBuilder();
			root.forEach(builder::add);

			return builder.toString();
		}
		
		// ASSUMPTION: As users are instructed to put ServerSync in the Minecraft
		// directory we can assume that the current directory is where serversync is
		// supposed to be, as we are asking for the Minecraft directory it should be
		// handled elsewhere when the directory can not be found
		return null;
	}

	private static List<String> getPathParts(String path) {
		path = path.replace('\\', '/');
		String[] pp = path.split("/");
		List<String> ppl = new ArrayList<>();
		for (String s : pp) {
			ppl.add(s.toLowerCase());
		}
		return ppl;
	}

	public static File[] fileList(String directory) {
		File contents = new File(directory);
		return contents.listFiles();
	}

	public static ArrayList<Path> fileListDeep(Path dir) {
		try {
			if (Files.exists(dir)) {
				Stream<Path> ds = Files.walk(dir);

				ArrayList<Path> dirList = new ArrayList<>();

				Iterator<Path> it = ds.iterator();
				while (it.hasNext()) {
					Path tp = it.next();
					// discard directories
					if (!Files.isDirectory(tp)) {
						dirList.add(tp);
					}
				}
				ds.close();
				return dirList;
			} else {
				return null;
			}

		} catch (IOException e) {
			System.out.println("Could not traverse directory");
		}
		return null;
	}
}
