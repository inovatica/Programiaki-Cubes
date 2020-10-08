package pl.inovatica.cubes.tools;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.jar.Manifest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class ManifestUtil {

	private static Logger logger = Logger.getLogger(ManifestUtil.class);

	private static final String commandAttributeName = "command";

	public static String getCommandAttribute() {
		return getAttribute(commandAttributeName);
	}

	private static String getAttribute(String attribute) {
		URLClassLoader classLoader = (URLClassLoader) ManifestUtil.class.getClassLoader();
		try {
			Manifest manifest = new Manifest(classLoader.findResource("META-INF/MANIFEST.MF").openStream());
			return manifest.getMainAttributes().getValue(attribute);
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		return StringUtils.EMPTY;
	}

}
