package pl.inovatica.cubes.service;

import static gnu.io.SerialPort.DATABITS_8;
import static gnu.io.SerialPort.PARITY_NONE;
import static gnu.io.SerialPort.STOPBITS_1;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import org.apache.log4j.Logger;

import pl.inovatica.cubes.CubesApplication;
import pl.inovatica.cubes.model.listener.DataListener;

import com.pi4j.io.serial.RaspberryPiSerial;

public class SerialPortService implements SerialPortEventListener {

	private static Logger logger = Logger.getLogger(SerialPortService.class);

	private final int timeout = 2000;
	private final int boudrate = 9600;

	private final String firstPortName = RaspberryPiSerial.S0_COM_PORT;
	private final String firstOptionalPortName = "/dev/ttyS80";
	private final String secondPortName = "/dev/ttyUSB0";
	private final String createPortLinkCommand = "sudo ln -s /dev/ttyAMA0 %s";

	private SerialPort firstPort = null;
	private SerialPort secondPort = null;

	private BufferedReader firstPortReader = null;
	private BufferedWriter firstPortWriter = null;

	private BufferedReader secondPortReader = null;
	private BufferedWriter secondPortWriter = null;

	private DataListener listener = null;

	public void scanPorts() {
		preparePorts();
		Enumeration<?> availablePortIdentifiers = CommPortIdentifier.getPortIdentifiers();

		CommPortIdentifier portIdentifier;
		while (availablePortIdentifiers.hasMoreElements()) {
			portIdentifier = (CommPortIdentifier) availablePortIdentifiers.nextElement();

			try {
				handleDetectedPort(portIdentifier);
			} catch (UnsupportedCommOperationException e) {
				logger.warn(e.getMessage(), e);
				continue;
			}
		}
	}

	public boolean isFirstInterfaceAvailable() {
		return (firstPortWriter != null && firstPortReader != null);
	}

	public boolean isSecondInterfaceAvailable() {
		return (secondPortWriter != null && secondPortReader != null);
	}

	public synchronized boolean sendByFirstInterface(String message) {
		return sendMessage(firstPortWriter, message);
	}

	public synchronized boolean sendBySecondInterface(String message) {
		return sendMessage(secondPortWriter, message);
	}

	public void registerDataListener(DataListener listener) {
		this.listener = listener;
	}

	private void handleDetectedPort(CommPortIdentifier portIdentifier) throws UnsupportedCommOperationException {
		if (portIdentifier.getPortType() != CommPortIdentifier.PORT_SERIAL) {
			return;
		}

		if (portIdentifier.isCurrentlyOwned()) {
			return;
		}

		SerialPort port;
		try {
			port = (SerialPort) portIdentifier.open(CubesApplication.class.getName(), timeout);
		} catch (PortInUseException e) {
			logger.warn(e.getMessage(), e);
			return;
		}

		if (isFirstPort(port)) {
			setupFirstPort(port);
			return;
		}

		if (isSecondPort(port)) {
			setupSecondPort(port);
			return;
		}

		port.close();
	}

	private void setupFirstPort(SerialPort port) throws UnsupportedCommOperationException {
		firstPort = port;
		firstPort.setSerialPortParams(boudrate, DATABITS_8, STOPBITS_1, PARITY_NONE);
		try {
			firstPortReader = new BufferedReader(new InputStreamReader(firstPort.getInputStream()));
			firstPortWriter = new BufferedWriter(new OutputStreamWriter(firstPort.getOutputStream()));
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
		try {
			firstPort.addEventListener(this);
			firstPort.notifyOnDataAvailable(true);
		} catch (TooManyListenersException e) {
			logger.warn(e.getMessage());
		}
	}

	private void setupSecondPort(SerialPort port) throws UnsupportedCommOperationException {
		secondPort = port;
		secondPort.setSerialPortParams(boudrate, DATABITS_8, STOPBITS_1, PARITY_NONE);
		try {
			secondPortReader = new BufferedReader(new InputStreamReader(secondPort.getInputStream()));
			secondPortWriter = new BufferedWriter(new OutputStreamWriter(secondPort.getOutputStream()));
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
		try {
			secondPort.addEventListener(this);
			secondPort.notifyOnDataAvailable(true);
		} catch (TooManyListenersException e) {
			logger.warn(e.getMessage());
		}
	}

	private boolean isFirstPort(SerialPort port) {
		return port != null && (firstPortName.equals(port.getName()) || firstOptionalPortName.equals(port.getName()));
	}

	private boolean isSecondPort(SerialPort port) {
		return port != null && secondPortName.equals(port.getName());
	}

	private String readFirstInterfaceLine() throws IOException {
		return (firstPortReader != null && firstPortReader.ready()) ? firstPortReader.readLine() : null;
	}

	private String readSecondInterfaceLine() throws IOException {
		return (secondPortReader != null && secondPortReader.ready()) ? secondPortReader.readLine() : null;
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		if (listener == null || SerialPortEvent.DATA_AVAILABLE != event.getEventType()) {
			return;
		}

		RXTXPort sourcePort = (RXTXPort) event.getSource();

		if (isFirstPort(sourcePort)) {
			try {
				listener.onDataReceivedByFirstInterface(readFirstInterfaceLine());
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}
			return;
		}

		if (isSecondPort(sourcePort)) {
			try {
				listener.onDataReceivedBySecondInterface(readSecondInterfaceLine());
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}
			return;
		}
	}

	protected void closeFirstInterface() {
		if (firstPort != null) {
			firstPort.removeEventListener();
		}

		if (firstPortReader != null) {
			try {
				firstPortReader.close();
				firstPortReader = null;
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}
		}

		if (firstPortWriter != null) {
			try {
				firstPortWriter.close();
				firstPortWriter = null;
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		if (firstPort != null) {
			firstPort.close();
		}
	}

	protected void closeSecondInterface() {
		if (secondPort != null) {
			secondPort.removeEventListener();
		}

		if (secondPortReader != null) {
			try {
				secondPortReader.close();
				secondPortReader = null;
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}
		}

		if (secondPortWriter != null) {
			try {
				secondPortWriter.close();
				secondPortWriter = null;
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		if (secondPort != null) {
			secondPort.close();
		}
	}

	private boolean sendMessage(BufferedWriter writer, String message) {
		if (writer == null || message == null) {
			return false;
		}

		try {
			writer.write(message);
			writer.write("\r");
			writer.flush();
		} catch (IOException e) {
			logger.warn(e.getMessage());
			return false;
		}

		return true;
	}

	private void preparePorts() {
		try {
			if (portExists(firstPortName)) {
				return;
			}

			if (portExists(firstOptionalPortName)) {
				return;
			}

			Runtime.getRuntime().exec(String.format(createPortLinkCommand, firstOptionalPortName));
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}
	}

	private boolean portExists(String name) throws IOException {
		return new File(name).exists();
	}
}
