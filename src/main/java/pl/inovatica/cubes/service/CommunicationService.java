package pl.inovatica.cubes.service;

import static org.apache.commons.lang3.math.NumberUtils.INTEGER_MINUS_ONE;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static pl.inovatica.cubes.model.MessageType.PING;
import static pl.inovatica.cubes.model.MessageType.PONG;
import static pl.inovatica.cubes.model.MessageType.RESPONSE;
import static pl.inovatica.cubes.model.MessageType.STATUS;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import pl.inovatica.cubes.CubesApplication;
import pl.inovatica.cubes.model.CommunicationInterface;
import pl.inovatica.cubes.model.Message;
import pl.inovatica.cubes.model.listener.DataListener;
import pl.inovatica.cubes.tools.MessageUtil;

public class CommunicationService implements DataListener {

	private Logger logger = Logger.getLogger(CommunicationService.class);

	private ExecutorService executor = Executors.newCachedThreadPool();

	private final long pongMessageWaitingTimeout = 600;
	private final long responsingDelay = 1000;

	private SerialPortService serialPortService;
	private PeripheralDevicesService peripheralDevicesService;
	private BluetoothService bluetoothService;

	private boolean lastCube = false;
	private int commandSequenceIndex = INTEGER_MINUS_ONE;
	private boolean repeatsCommand = false;

	public CommunicationService(SerialPortService serialPortService, PeripheralDevicesService peripheralDevicesService,
			BluetoothService bluetoothService) {
		this.serialPortService = serialPortService;
		this.peripheralDevicesService = peripheralDevicesService;
		this.bluetoothService = bluetoothService;
		this.serialPortService.registerDataListener(this);
	}

	public void sendMessageStartingCubesScanning() {
		sendPingMessage(CommunicationInterface.FIRST);
	}

	public void sendStatusMessage(String command) {
		sendMesage(MessageUtil.serialize(STATUS, command), CommunicationInterface.FIRST);
	}

	@Override
	public void onDataReceivedByFirstInterface(String message) {
		onDataReceived(MessageUtil.deserialize(message), CommunicationInterface.FIRST);
	}

	@Override
	public void onDataReceivedBySecondInterface(String message) {
		onDataReceived(MessageUtil.deserialize(message), CommunicationInterface.SECOND);
	}

	private boolean sendMesage(String message, CommunicationInterface communicationInterface) {
		switch (communicationInterface) {
		case FIRST:
			return serialPortService.sendByFirstInterface(message);
		case SECOND:
			return serialPortService.sendBySecondInterface(message);
		}
		return false;
	}

	private void onDataReceived(Message message, CommunicationInterface communicationInterface) {
		if (PING.equals(message.getType())) {
			handlePingMessage(message, communicationInterface);
			return;
		}

		if (PONG.equals(message.getType())) {
			handlePongMessage(message);
			return;
		}

		if (RESPONSE.equals(message.getType())) {
			handleResponseMessage(message, communicationInterface);
			return;
		}

		if (STATUS.equals(message.getType())) {
			handleStatusMessage(message, communicationInterface);
			return;
		}
	}

	private void handlePongMessage(Message message) {
		lastCube = false;
		synchronized (this) {
			notify();
		}
	}

	private void handlePingMessage(Message message, CommunicationInterface communicationInterface) {
		if (CubesApplication.isMainCube()) {
			return;
		}

		sendMesage(MessageUtil.serialize(PONG), communicationInterface);
		sendPingMessage(communicationInterface.getOppositeInterface());
	}

	private void sendPingMessage(CommunicationInterface communicationInterface) {
		lastCube = true;
		if (!sendMesage(MessageUtil.serialize(PING), communicationInterface)) {
			commandSequenceIndex = INTEGER_ZERO;
			sendResponseMesage(communicationInterface.getOppositeInterface(), null);
			return;
		}

		executor.execute(() -> {
			try {
				synchronized (this) {
					wait(pongMessageWaitingTimeout);
				}
			} catch (InterruptedException e) {
				logger.warn(e.getMessage(), e);
			}

			if (lastCube) {
				commandSequenceIndex = INTEGER_ZERO;
				sendResponseMesage(communicationInterface.getOppositeInterface(), null);
			}
		});
	}

	private void sendResponseMesage(CommunicationInterface communicationInterface, Message message) {
		if (CubesApplication.isMainCube()) {
			bluetoothService.executProgram(new String[INTEGER_ZERO]);
			return;
		}

		sendMesage(MessageUtil.serialize(RESPONSE, getDataForResponseMessage(message != null ? message.getData() : null)), communicationInterface);
		peripheralDevicesService.turnOnBlueLight();
	}

	private String[] getDataForResponseMessage(String[] data) {
		String[] newData;

		repeatsCommand = peripheralDevicesService.isButtonPressed();
		int commandRepeats = repeatsCommand ? 2 : 1;
		if (data != null) {
			newData = Arrays.copyOf(data, data.length + commandRepeats);
		} else {
			newData = new String[commandRepeats];
		}

		for (int i = 1; i <= commandRepeats; i++) {
			newData[newData.length - i] = CubesApplication.cubeCommand;
		}

		return newData;
	}

	private void handleResponseMessage(Message message, CommunicationInterface communicationInterface) {
		try {
			Thread.sleep(responsingDelay);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage());
		}

		if (CubesApplication.isMainCube()) {
			if (message.getData() != null) {
				peripheralDevicesService.turnOnGreenLight();
				bluetoothService.executProgram(message.getData());
			}
			return;
		}

		commandSequenceIndex = message.getData() != null ? message.getData().length : INTEGER_MINUS_ONE;
		sendResponseMesage(communicationInterface.getOppositeInterface(), message);
	}

	private void handleStatusMessage(Message message, CommunicationInterface communicationInterface) {
		if (CubesApplication.isMainCube()) {
			return;
		}

		boolean processingMyCommand = false;
		try {
			processingMyCommand = (message.getData() != null && message.getData().length > INTEGER_ZERO && isMyIndex(Integer.parseInt(message
					.getData()[INTEGER_ZERO])));
		} catch (NumberFormatException e) {
			logger.warn(e.getMessage());
		}

		if (processingMyCommand) {
			peripheralDevicesService.turnOnGreenLight();
			peripheralDevicesService.generateSound();
		} else {
			peripheralDevicesService.turnOnRedLight();
		}

		sendMesage(MessageUtil.serialize(STATUS, message.getData()), communicationInterface.getOppositeInterface());
	}

	private boolean isMyIndex(int statusCommandIndex) {
		return (commandSequenceIndex == statusCommandIndex) || (repeatsCommand ? ((commandSequenceIndex + 1) == statusCommandIndex) : false);
	}
}
