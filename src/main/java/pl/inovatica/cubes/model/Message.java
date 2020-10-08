package pl.inovatica.cubes.model;

public class Message {

	private MessageType type;
	private String[] data;

	public Message(MessageType type, String[] data) {
		this.type = type;
		this.data = data;
	}

	public MessageType getType() {
		return type;
	}

	public String[] getData() {
		return data;
	}

}
