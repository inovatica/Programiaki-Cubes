package pl.inovatica.cubes;

import org.apache.commons.lang3.StringUtils;

import pl.inovatica.cubes.model.listener.ButtonListener;
import pl.inovatica.cubes.model.listener.ProgramProgressListener;
import pl.inovatica.cubes.service.BluetoothService;
import pl.inovatica.cubes.service.CommunicationService;
import pl.inovatica.cubes.service.PeripheralDevicesService;
import pl.inovatica.cubes.service.SerialPortService;
import pl.inovatica.cubes.tools.ManifestUtil;

public class CubesApplication {

	// private static Logger logger = Logger.getLogger(CubesApplication.class);

	public static String cubeCommand = StringUtils.EMPTY;

	private static SerialPortService serialPortService;

	private static CommunicationService communicationService;

	private static PeripheralDevicesService peripheralDevicesService;

	private static BluetoothService bluetoothService;

	static {
		cubeCommand = ManifestUtil.getCommandAttribute();
		serialPortService = new SerialPortService();
		peripheralDevicesService = new PeripheralDevicesService();
		bluetoothService = new BluetoothService();
		communicationService = new CommunicationService(serialPortService, peripheralDevicesService, bluetoothService);
	}

	public static void main(String[] args) {
		serialPortService.scanPorts();
		bluetoothService.createRobotScriptFiles();
		peripheralDevicesService.initialize();
		peripheralDevicesService.turnOnRedLight();
		peripheralDevicesService.generateShortSound();
		peripheralDevicesService.setButtonListener(createButtonListener());
		bluetoothService.setProgressListener(createProgramProgressListener());
	}

	public static boolean isMainCube() {
		return cubeCommand.isEmpty();
	}

	private static ButtonListener createButtonListener() {
		return new ButtonListener() {
			@Override
			public void onReleased() {
				if (!isMainCube()) {
					return;
				}

				onChange();
			}

			@Override
			public void onPressed() {
				if (!isMainCube()) {
					return;
				}

				onChange();
			}

			private void onChange() {
				if (bluetoothService.isProgramExecuting()) {
					bluetoothService.finishProgramExecuting();
				} else {
					communicationService.sendMessageStartingCubesScanning();
					peripheralDevicesService.turnOnBlueLight();
				}
			}
		};
	}

	private static ProgramProgressListener createProgramProgressListener() {
		return new ProgramProgressListener() {
			@Override
			public void onCommandExecution(String command) {
				communicationService.sendStatusMessage(command);
			}

			@Override
			public void onProgramEnd() {
				communicationService.sendStatusMessage(StringUtils.EMPTY);
				peripheralDevicesService.turnOnRedLight();
			}
		};
	}

}
