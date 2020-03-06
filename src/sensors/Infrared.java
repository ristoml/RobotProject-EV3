package sensors;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;

public class Infrared {

	private SensorModes sensor;
	private SampleProvider sampleProvider;
	
	public Infrared(String portti) {
		Port port = LocalEV3.get().getPort(portti);
		sensor = new EV3IRSensor(port);
		sampleProvider = ((EV3IRSensor) sensor).getDistanceMode();
	}
	
	public float distance() {
		float[] sample = new float[sampleProvider.sampleSize()];
		sensor.fetchSample(sample, 0);
		
		return sample[0];
	}
	
	public boolean distanceLimitReached(float limit) {
	    return distance() <= limit;
	}
}