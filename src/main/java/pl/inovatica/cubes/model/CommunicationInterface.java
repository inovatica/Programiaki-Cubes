package pl.inovatica.cubes.model;

public enum CommunicationInterface {
	FIRST, SECOND;

	public CommunicationInterface getOppositeInterface() {
		switch (this) {
		case FIRST:
			return SECOND;
		case SECOND:
			return FIRST;
		default:
			return FIRST;
		}
	}
}
