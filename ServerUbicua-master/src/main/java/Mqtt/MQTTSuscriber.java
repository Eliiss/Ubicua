package mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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

    // Acumuladores de datos
    private Float temperature = null;
    private Float humidity = null;
    private Integer light = null;

    public MQTTSuscriber(MQTTBroker broker) {
        this.brokerUrl = broker.getBroker();
        this.clientId = broker.getSubscriberClientId();
        this.username = broker.getUsername();
        this.password = broker.getPassword();
    }

    // --- NUEVO: Conectar una sola vez ---
    public void connect() {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl, clientId, persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true);

            client.setCallback(this);
            Log.logmqtt.info("Conectando al broker " + brokerUrl + "...");
            client.connect(connOpts);
            Log.logmqtt.info("Conectado correctamente.");

        } catch (MqttException e) {
            Log.logmqtt.error("Error al conectar: " + e.getMessage());
        }
    }

    // --- MODIFICADO: Solo suscribirse ---
    public void subscribeTopic(String topic) {
        try {
            if (client != null && client.isConnected()) {
                client.subscribe(topic, 1);
                Log.logmqtt.info("Suscrito a: " + topic);
            }
        } catch (MqttException e) {
            Log.logmqtt.error("Error al suscribirse a " + topic + ": " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.logmqtt.warn("Conexión perdida: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        Log.logmqtt.info("RECIBIDO en " + topic + ": " + payload);

        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            
            // Gestión básica de timestamp
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            if(json.has("timestamp")) {
                try {
                     String tsStr = json.get("timestamp").getAsString().replace("T", " ").replace("Z", "");
                     ts = Timestamp.valueOf(tsStr);
                } catch(Exception e) { /* Ignoramos error de fecha y usamos la actual */ }
            }

            JsonObject data = json.getAsJsonObject("data");

            // Lógica de acumulado
            if (topic.equals("ubicua_db/temperatura") && data.has("temperature_celsius")) {
                temperature = data.get("temperature_celsius").getAsFloat();
            } else if (topic.equals("ubicua_db/humedad") && data.has("humidity_percent")) {
                humidity = data.get("humidity_percent").getAsFloat();
            } else if (topic.equals("ubicua_db/luz")) {
                if (data.has("light_intensity")) light = data.get("light_intensity").getAsInt();
                else if (data.has("light")) light = data.get("light").getAsInt();
            }

            // Guardar solo si tenemos los 3
            if (temperature != null && humidity != null && light != null) {
                Log.logmqtt.info(">>> GRUPO COMPLETO: Guardando en BD...");
                Logic.setDataToDB(temperature, humidity, light, ts);
                temperature = null; humidity = null; light = null; // Reset
            }

        } catch (Exception e) {
            Log.logmqtt.error("Error procesando mensaje: " + e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}
}