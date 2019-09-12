package me.steffenjacobs.homeautomationdummy;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeAutomationDummy implements MqttCallback {

	private static final Logger LOG = LoggerFactory.getLogger(HomeAutomationDummy.class);
	private static final int[] RF_CODES = new int[] { 4414, 4415, 4424, 4425, 4434, 4435, 4444, 4445, 4315 };

	private static final String SERVER_URI = "tcp://127.0.0.1:1883";

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
	MqttClient client;

	public static void main(String[] args) {
		new HomeAutomationDummy().setupAndStart();
	}

	class ValueWithDirection<T> {
		private final boolean direction;
		private final T value;

		public ValueWithDirection(boolean direction, T value) {
			super();
			this.direction = direction;
			this.value = value;
		}

		public T getValue() {
			return value;
		}

		public boolean isDirection() {
			return direction;
		}

	}

	Map<String, AtomicReference<ValueWithDirection<?>>> concurrentCache = new ConcurrentHashMap<>();

	private void fake(MqttClient client, String topic, int[] arr) throws MqttException {
		concurrentCache.put(topic, new AtomicReference<ValueWithDirection<?>>(new ValueWithDirection<Integer>(false, 0)));
		scheduler.scheduleAtFixedRate(() -> {
			final AtomicReference<ValueWithDirection<?>> v = concurrentCache.get(topic);

			final Integer lastValue = (Integer) v.get().getValue();

			final int nextValue = lastValue >= arr.length - 1 ? 0 : lastValue + 1;
			try {
				MqttMessage msg = new MqttMessage();
				msg.setPayload(Integer.toString(arr[nextValue]).getBytes(Charset.forName("UTF-8")));
				client.publish(topic, msg);
			} catch (MqttException e) {
				e.printStackTrace();
			}

			v.set(new ValueWithDirection<>(false, nextValue));
		}, (int) (Math.random() * 5000), 2500, TimeUnit.MILLISECONDS);
		LOG.info("Setup dummy on topic {}", topic);
		client.subscribe(topic);
	}

	private void fake(MqttClient client, String topic, double lowerLimit, double upperLimit, double avgStepSize, double deviation) throws MqttException {
		concurrentCache.put(topic, new AtomicReference<ValueWithDirection<?>>(
				new ValueWithDirection<Double>(Math.random() > .5, ((Math.random() * avgStepSize) / avgStepSize) * (lowerLimit + upperLimit) / 2.0)));
		scheduler.scheduleAtFixedRate(() -> {
			AtomicReference<ValueWithDirection<?>> v = concurrentCache.get(topic);
			final Double lastValue = (Double) v.get().getValue();

			boolean direction;
			if (lastValue > upperLimit) {
				direction = false;
			} else if (lastValue < lowerLimit) {
				direction = true;
			} else {
				direction = v.get().isDirection();
			}
			double nextValue;

			if (direction) {
				nextValue = lastValue + deviation - Math.random() * deviation * 2 + avgStepSize;
			} else {
				nextValue = lastValue - deviation + Math.random() * deviation * 2 - avgStepSize;
			}
			try {
				MqttMessage msg = new MqttMessage();
				msg.setPayload(String.format(Locale.US, "%.2f", nextValue).getBytes(Charset.forName("UTF-8")));
				client.publish(topic, msg);
			} catch (MqttException e) {
				e.printStackTrace();
			}

			v.set(new ValueWithDirection<>(direction, nextValue));
		}, (int) (Math.random() * 5000), 2500, TimeUnit.MILLISECONDS);
		LOG.info("Setup dummy on topic {}", topic);
		client.subscribe(topic);
	}

	public void setupAndStart() {
		try {
			client = new MqttClient(SERVER_URI, "Home-Automation-Dummy");
			client.connect();
			LOG.info("Connected to MQTT server @ {}", SERVER_URI);

			fake(client, "/sensor/temperature1", 17, 30, .2, .1);
			fake(client, "/sensor/temperature1_2", 17, 30, .2, .05);
			fake(client, "/sensor/temperature2", 20, 33, .2, .1);
			fake(client, "/sensor/temperature3", 18, 31, .2, .1);

			fake(client, "/sensor/humidity1", 5, 70, 3, 1);
			fake(client, "/sensor/humidity2", 5, 70, 3, 1);
			fake(client, "/sensor/humidity3", 5, 70, 3, 1);

			fake(client, "/sensor/co2_1", 400, 1800, 80, 20);
			fake(client, "/sensor/co2_2", 400, 1800, 80, 20);
			fake(client, "/sensor/co2_3", 400, 1800, 80, 20);

			fake(client, "/sensor/rf", RF_CODES);
			fake(client, "/sensor/light_level_outside", 0, 20, 1, .5);

			client.setCallback(this);
		} catch (MqttException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public void connectionLost(Throwable cause) {
		LOG.error(cause.getMessage(), cause);
	}

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		LOG.info("[{}] - {}", topic, message);
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
		// ignore
	}

}
