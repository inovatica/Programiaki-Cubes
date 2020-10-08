package pl.inovatica.cubes.tools;

import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

import java.util.Arrays;

import pl.inovatica.cubes.model.Message;
import pl.inovatica.cubes.model.MessageType;

public class MessageUtil {

	private static final String separator = ";";

	public static String serialize(MessageType type, String... data) {
		if (type == null) {
			return null;
		}

		StringBuilder builder = new StringBuilder(type.name());
		if (data != null) {
			for (String part : data) {
				builder.append(separator).append(part);
			}
		}

		return builder.toString();
	}

	public static Message deserialize(String message) {
		if (message == null || message.isEmpty()) {
			return new Message(null, null);
		}

		String[] parts = message.split(separator);
		return new Message(MessageType.valueOf(parts[INTEGER_ZERO]),
				parts.length > INTEGER_ONE ? Arrays.copyOfRange(parts, INTEGER_ONE, parts.length) : null);
	}
}
