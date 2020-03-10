package sensors;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;

/**
 * Class for using EV3-infrared -sensor.
 * Provides methods for checking infrared's distance to a physical object.
 */
public class Infrared {

	private SensorModes sensor;
	private SampleProvider sampleProvider;
	
	public Infrared(String port) {
		Port portEV3 = LocalEV3.get().getPort(port);
		sensor = new EV3IRSensor(portEV3);
		sampleProvider = ((EV3IRSensor) sensor).getDistanceMode();
	}
	
	/**
	 * Fetches a sample from the sensor and returns the infrared's distance to a physical object.
	 * @return infrared's distance
	 */
	public float distance() {
		float[] sample = new float[sampleProvider.sampleSize()];
		sensor.fetchSample(sample, 0);
		
		return sample[0];
	}
	
	/**
	 * Checks if a specific distance limit has been reached.
	 * @param limit
	 * @return true if limit has been reached, false otherwise
	 */
	public boolean distanceLimitReached(float limit) {
	    return distance() <= limit;
	}
}