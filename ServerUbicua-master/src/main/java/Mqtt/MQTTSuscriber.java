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
        // Log para ver exactamente qué llega
        Log.logmqtt.info("Mensaje recibido en " + topic + ": " + payload);

        try {
            // 1. Parsear el JSON recibido
            // Usamos JsonParser (de com.google.gson) para convertir el String a objeto
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            
            // 2. Extraer y limpiar el Timestamp
            // Quitamos la 'T' y la 'Z' para que Timestamp.valueOf lo entienda (formato SQL: yyyy-mm-dd hh:mm:ss)
            String tsStr = json.get("timestamp").getAsString().replace("T", " ").replace("Z", "");
            Timestamp ts = Timestamp.valueOf(tsStr);

            // 3. Obtener el objeto "data" interno
            JsonObject data = json.getAsJsonObject("data");

            // 4. Extraer el dato correspondiente según el topic
            if (topic.equals("ubicua_db/temperatura")) {
                if (data.has("temperature_celsius")) {
                    temperature = data.get("temperature_celsius").getAsFloat();
                }
            } else if (topic.equals("ubicua_db/humedad")) {
                if (data.has("humidity_percent")) {
                    humidity = data.get("humidity_percent").getAsFloat();
                }
            } else if (topic.equals("ubicua_db/luz")) {
                if (data.has("light_intensity")) {
                    light = data.get("light_intensity").getAsInt();
                }
            }

            // Log de control para ver cómo se van llenando las variables
            Log.logmqtt.info("Acumulado actual -> Temp: " + temperature + " | Hum: " + humidity + " | Luz: " + light);

            // 5. Guardar en Base de Datos SOLO si tenemos los 3 valores
            if (temperature != null && humidity != null && light != null) {
                Log.logmqtt.info("¡Lote completo! Guardando en base de datos...");
                
                // Llamamos a la lógica para insertar
                Logic.setDataToDB(temperature, humidity, light, ts);
                
                // IMPORTANTE: Reiniciar las variables para esperar el siguiente grupo de datos
                temperature = null;
                humidity = null;
                light = null;
            } else {
                Log.logmqtt.info("Esperando el resto de datos para completar el registro...");
            }

        } catch (Exception e) {
            // Este catch capturará errores de formato JSON, nombres de clave incorrectos, etc.
            Log.logmqtt.error("ERROR procesando mensaje en topic " + topic + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}
}