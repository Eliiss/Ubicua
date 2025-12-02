package mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import Database.Topics;
import logic.Log;
import logic.Logic;

import java.sql.Timestamp;
import com.google.gson.*;

public class MQTTSuscriber implements MqttCallback {

    private MqttClient client;
    private String brokerUrl;
    private String clientId;
    private String username;
    private String password;

    // Si necesitas guardar la última medición de cada tipo
    private Float temperature = null;
    private Float humidity = null;
    private Integer light = null;

    public MQTTSuscriber(MQTTBroker broker) {
        this.brokerUrl = broker.getBroker();
        this.clientId = broker.getClientId();
        this.username = broker.getUsername();
        this.password = broker.getPassword();
    }

    public void subscribeTopic(String topic) {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl, MQTTBroker.getSubscriberClientId(), persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            connOpts.setCleanSession(false); // Para mantener la suscripción
            connOpts.setAutomaticReconnect(true); // Reconexión automática
            connOpts.setConnectionTimeout(10);

            client.setCallback(this);
            client.connect(connOpts);

            client.subscribe(topic, 1); // QoS 1 para asegurarse de recibir
            Log.logmqtt.info("Subscribed to {}", topic);

        } catch (MqttException e) {
            Log.logmqtt.error("Error subscribing to topic: {}", e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.logmqtt.warn("MQTT Connection lost, cause: {}", cause.getMessage());
    }
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = message.toString();
        Log.logmqtt.info("Mensaje recibido en {}: {}", topic, payload);
        try {
            // Parsear el payload JSON
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            JsonObject data = json.getAsJsonObject("data");
            Timestamp ts = Timestamp.valueOf(json.get("timestamp").getAsString().replace("T", " ").replace("Z",""));

            if (topic.equals("ubicomp/temperatura")) {
                temperature = data.get("temperature_celsius").getAsFloat();
            } else if (topic.equals("ubicomp/humedad")) {
                humidity = data.get("humidity_percent").getAsFloat();
            } else if (topic.equals("ubicomp/luz")) {
                light = data.get("light_intensity").getAsInt();
            }

            // Lógica para guardar solo si tienes los tres datos:
            if (temperature != null && humidity != null && light != null) {
                Logic.setDataToDB(temperature, humidity, light, ts);
                // Resetear para el siguiente lote
                temperature = null; humidity = null; light = null;
            }
        } catch (Exception e) {
            Log.logmqtt.error("Error procesando mensaje MQTT: " + e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}
}