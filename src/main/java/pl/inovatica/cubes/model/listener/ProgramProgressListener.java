package pl.inovatica.cubes.model.listener;

public interface ProgramProgressListener {

	public void onCommandExecution(String command);

	public void onProgramEnd();

}
