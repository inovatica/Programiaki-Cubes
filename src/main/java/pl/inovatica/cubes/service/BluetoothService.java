package pl.inovatica.cubes.service;

import static org.apache.commons.lang3.math.NumberUtils.INTEGER_MINUS_ONE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import pl.inovatica.cubes.model.listener.ProgramProgressListener;
import pl.inovatica.cubes.tools.ResourcesUtil;

public class BluetoothService {

	private Logger logger = Logger.getLogger(BluetoothService.class);

	private final String robotResourcesDirectory = "robot/";
	private final String robotScriptDirectory = "/home/pi/java/robot/";
	private final String command = "python3.5 " + robotScriptDirectory + "robot.py";
	private final long commandsInterval = 2000;
	private final String connectedResponse = "connected";
	private final String errorResponse = "fail";
	private final String commandResponse = "[f/r/l/q]:";
	private final String quitCommand = "q";

	private boolean programExecuting;

	private ProgramProgressListener progressListener;

	public void createRobotScriptFiles() {
		ResourcesUtil.ceateFilesFormResourceIfnotExists(robotResourcesDirectory, robotScriptDirectory);
	}

	public void setProgressListener(ProgramProgressListener progressListener) {
		this.progressListener = progressListener;
	}

	public void finishProgramExecuting() {
		if (!programExecuting) {
			return;
		}

		programExecuting = false;
		if (progressListener != null) {
			progressListener.onProgramEnd();
		}
	}

	public void executProgram(String... instructions) {
		if (programExecuting || instructions == null) {
			return;
		}

		Process process = launchProcess();
		if (process == null) {
			return;
		}

		programExecuting = true;
		try (InputStream is = process.getInputStream(); BufferedWriter out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
			sendInstructionsSequence(process, is, out, instructions);
			process.destroyForcibly();
		} catch (IOException e) {
			logger.error(e.getMessage());
		} finally {
			programExecuting = false;
		}
	}

	private Process launchProcess() {
		try {
			return Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		return null;
	}

	private void sendInstructionsSequence(Process process, InputStream is, BufferedWriter out, String... instructions) throws IOException {
		int index = 0;
		String response = null;
		while (programExecuting && process.isAlive() && (response = readResponse(is)) != null && index < instructions.length) {
			if (response.endsWith(connectedResponse) || response.contains(commandResponse)) {
				sendInstruction(out, instructions[instructions.length - ++index], instructions.length - index);
				continue;
			}

			if (response.endsWith(errorResponse) || !process.isAlive()) {
				finishProgramExecuting();
			}
		}

		try {
			sendInstruction(out, quitCommand, INTEGER_MINUS_ONE);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		finishProgramExecuting();
	}

	private String readResponse(InputStream in) throws IOException {
		byte[] buffer = new byte[4096];

		int bytesRead = in.read(buffer);
		if (bytesRead != -1) {
			return new String(buffer);
		}

		return StringUtils.EMPTY;
	}

	private void sendInstruction(BufferedWriter out, String instruction, int index) throws IOException {
		out.write(instruction.toLowerCase());
		out.newLine();
		out.flush();

		if (progressListener != null) {
			progressListener.onCommandExecution(String.valueOf(index));
		}

		try {
			Thread.sleep(commandsInterval);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage());
		}
	}

	public boolean isProgramExecuting() {
		return programExecuting;
	}

}
