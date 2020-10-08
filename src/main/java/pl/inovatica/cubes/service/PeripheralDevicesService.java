package pl.inovatica.cubes.service;

import static com.pi4j.io.gpio.PinPullResistance.PULL_UP;
import static com.pi4j.io.gpio.RaspiPin.GPIO_00;
import static com.pi4j.io.gpio.RaspiPin.GPIO_02;
import static com.pi4j.io.gpio.RaspiPin.GPIO_03;
import static com.pi4j.io.gpio.RaspiPin.GPIO_14;
import static com.pi4j.io.gpio.RaspiPin.GPIO_23;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import pl.inovatica.cubes.model.listener.ButtonListener;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.PinEdge;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.wiringpi.Gpio;

public class PeripheralDevicesService {

	private Logger logger = Logger.getLogger(PeripheralDevicesService.class);

	private ButtonListener buttonListener;

	private GpioController gpioController;

	private ExecutorService executor = Executors.newCachedThreadPool();

	private final int pwmRange = 4096;
	private final double pwmOnePercentValue = pwmRange / 100.0;
	private final int speakerPwmPulseWidth = 40;
	private final int pwmClockDivisor = 4095;
	private final long soundDuration = 800;

	private GpioPinDigitalInput buttonInput = null;
	private GpioPinDigitalOutput ledROutput = null;
	private GpioPinDigitalOutput ledGOutput = null;
	private GpioPinDigitalOutput ledBOutput = null;
	private GpioPinPwmOutput speakerPwmOutput = null;

	public void initialize() {
		try {
			gpioController = GpioFactory.getInstance();
			setUpPins();
			createListeners();
		} catch (Exception e) {
			logger.error("Can't initialize gpio pins", e);
		}
	}

	public void turnOnRedLight() {
		ledROutput.high();
		ledGOutput.low();
		ledBOutput.low();
	}

	public void turnOnGreenLight() {
		ledROutput.low();
		ledGOutput.high();
		ledBOutput.low();
	}

	public void turnOnBlueLight() {
		ledROutput.low();
		ledGOutput.low();
		ledBOutput.high();
	}

	public boolean isButtonPressed() {
		return buttonInput.isLow();
	}

	public void setButtonListener(ButtonListener buttonListener) {
		this.buttonListener = buttonListener;
	}

	public void generateSound() {
		makeSound(soundDuration);
	}

	public void generateShortSound() {
		makeSound(soundDuration / 2);
	}

	private void makeSound(long duration) {
		executor.execute(() -> {
			speakerPwmOutput.setPwm((int) (pwmOnePercentValue * speakerPwmPulseWidth));
			try {
				Thread.sleep(duration);
			} catch (Exception e) {
				logger.warn(e.getMessage());
			}
			speakerPwmOutput.setPwm(INTEGER_ZERO);
		});
	}

	private void setUpPins() {
		buttonInput = gpioController.provisionDigitalInputPin(GPIO_14, PULL_UP);
		ledROutput = gpioController.provisionDigitalOutputPin(GPIO_03, PinState.LOW);
		ledGOutput = gpioController.provisionDigitalOutputPin(GPIO_02, PinState.LOW);
		ledBOutput = gpioController.provisionDigitalOutputPin(GPIO_00, PinState.LOW);

		speakerPwmOutput = gpioController.provisionPwmOutputPin(GPIO_23, INTEGER_ZERO);
		Gpio.pwmSetClock(pwmClockDivisor);
		speakerPwmOutput.setPwmRange(pwmRange);
	}

	private void createListeners() {
		buttonInput.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				if (buttonListener == null) {
					return;
				}

				if (PinEdge.RISING.equals(event.getEdge())) {
					buttonListener.onReleased();
					return;
				}

				buttonListener.onPressed();
			}
		});
	}

}
