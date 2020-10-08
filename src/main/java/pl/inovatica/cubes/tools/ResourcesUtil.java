package pl.inovatica.cubes.tools;

import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class ResourcesUtil {

	private static Logger logger = Logger.getLogger(ResourcesUtil.class);

	public static void ceateFilesFormResourceIfnotExists(String resourcesDirectory, String destinationDirectory) {
		File destinationDirectoryFile = new File(destinationDirectory);
		if (!destinationDirectoryFile.exists() && !destinationDirectoryFile.mkdirs()) {
			return;
		}

		for (String resource : getResourceFiles(resourcesDirectory)) {
			ceateFilesFormResourcIfnotExists(resourcesDirectory, resource, destinationDirectory);
		}
	}

	private static Set<String> getResourceFiles(String resourcesDirectory) {
		Set<String> result = new HashSet<String>();
		URL resourcesURL = ResourcesUtil.class.getClassLoader().getResource(resourcesDirectory);
		Enumeration<JarEntry> entries;

		try (JarFile jar = new JarFile(URLDecoder.decode(StringUtils.substringBetween(resourcesURL.getPath(), ":", "!"), "UTF-8"))) {
			entries = jar.entries();
			String entryName;
			while (entries.hasMoreElements()) {
				entryName = entries.nextElement().getName();
				if (entryName.startsWith(resourcesDirectory) && !entryName.equals(resourcesDirectory)) {
					result.add(entryName.substring(resourcesDirectory.length()));
				}
			}
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		return result;
	}

	private static void ceateFilesFormResourcIfnotExists(String resourcesDirectory, String resource, String destinationDirectory) {
		File destinationFile = new File(destinationDirectory + resource);
		if (destinationFile.exists()) {
			return;
		}

		try {
			if (!destinationFile.createNewFile()) {
				return;
			}
		} catch (IOException e) {
			logger.error(e.getStackTrace(), e);
			return;
		}

		copyDataFromResourceToFile(resourcesDirectory + resource, destinationFile);
	}

	private static void copyDataFromResourceToFile(String resource, File file) {
		try (InputStream in = ResourcesUtil.class.getClassLoader().getResourceAsStream(resource); OutputStream out = new FileOutputStream(file)) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, INTEGER_ZERO, bytesRead);
			}
		} catch (IOException e) {
			logger.error(e.getStackTrace(), e);
		}
	}
}