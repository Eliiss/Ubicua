#include <WiFi.h>           // Conectarse al wifi
#include <PubSubClient.h>   // Implementar protocolo MQTT 
#include <DHT11.h>          // Sensor de humedad y temperatura
#include <LiquidCrystal.h>  // Para el LCD
#include <ArduinoJson.h>    // Para el JSON

// ---------- CONFIGURACIÓN DE RED ----------
const char* ssid = "cubicua";
const char* password = "";

// ---------- CONFIGURACIÓN MQTT ----------
const char* mqtt_server = "172.29.41.88";   // IP del broker Mosquitto
const int mqtt_port = 1883;
const char* mqtt_client_id = "LAB12JAV-G3";
// --- Topics ---
const char* mqtt_topic_publish = "sensors/ST_0889/Weather_station"; // Para ENVIAR datos
const char* mqtt_topic_command = "sensors/ST_0889/led/set";          // Para RECIBIR órdenes (NUEVO)

// ---------- CONFIGURACIÓN DE SENSORES ----------
#define DHTPIN 13       // Pin del DATA del sensor de temperatura y humedad
#define DHTTYPE DHT11
#define LDR_PIN 34      // Pin del fotorresistor
#define LED_R 14
#define LED_G 27
#define LED_B 26

// ---------- CONFIGURACIÓN LCD1602 ----------
#define RS 19
#define E 18
#define D4 5
#define D5 17
#define D6 16
#define D7 15

DHT11 dht(DHTPIN);
LiquidCrystal lcd(RS, E, D4, D5, D6, D7);

WiFiClient espClient;
PubSubClient client(espClient);

// ---------- ESTADO GLOBAL ----------
bool manualLedControl = false; // Flag para anular el LDR (NUEVO)

// -------------------------------------------------------
// ---------- FUNCIÓN CALLBACK ----------
// Esta función se llama cada vez que llega un mensaje 
// a un topic al que estamos suscritos.
// -------------------------------------------------------
void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Mensaje recibido en [");
  Serial.print(topic);
  Serial.print("]: ");

  // Convertir el payload (bytes) a un String
  String message = "";
  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }
  Serial.println(message);

  // Comprobar a qué topic ha llegado
  if (String(topic) == mqtt_topic_command) {
    
    // Lógica para controlar los LEDs
    if (message == "red") {
      manualLedControl = true; // Activa el modo manual
      digitalWrite(LED_R, HIGH);
      digitalWrite(LED_G, LOW);
      digitalWrite(LED_B, LOW);
      Serial.println("Cambiando LED a ROJO");
    } 
    else if (message == "green") {
      manualLedControl = true;
      digitalWrite(LED_R, LOW);
      digitalWrite(LED_G, HIGH);
      digitalWrite(LED_B, LOW);
      Serial.println("Cambiando LED a VERDE");
    } 
    else if (message == "blue") {
      manualLedControl = true;
      digitalWrite(LED_R, LOW);
      digitalWrite(LED_G, LOW);
      digitalWrite(LED_B, HIGH);
      Serial.println("Cambiando LED a AZUL");
    } 
    else if (message == "off") {
      manualLedControl = true;
      digitalWrite(LED_R, LOW);
      digitalWrite(LED_G, LOW);
      digitalWrite(LED_B, LOW);
      Serial.println("Apagando LEDs");
    } 
    else if (message == "auto") {
      manualLedControl = false; // Vuelve al modo automático (LDR)
      Serial.println("Cambiando a modo AUTOMÁTICO (LDR)");
    }
  }
}

// ---------- FUNCIONES DE CONEXIÓN ----------
void setup_wifi() {
  delay(100);
  Serial.println("Conectando a WiFi...");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi conectado!");
  Serial.print("IP: ");
  Serial.println(WiFi.localIP());
}

// --- MODIFICADA ---
void reconnect() {
  while (!client.connected()) {
    Serial.print("Intentando conexión MQTT...");
    if (client.connect(mqtt_client_id)) {
      Serial.println("Conectado al broker MQTT!");
      
      // Suscribirse al topic de comandos (NUEVO)
      if (client.subscribe(mqtt_topic_command)) {
        Serial.println("Suscrito a " + String(mqtt_topic_command));
      } else {
        Serial.println("Error al suscribirse al topic de comandos");
      }
      
    } else {
      Serial.print("Error, rc=");
      Serial.print(client.state());
      Serial.println(" Reintentando en 5s");
      delay(5000);
    }
  }
}

// -------------------------------------------------------
// ---------- SETUP (MODIFICADO) ----------
// -------------------------------------------------------
void setup() {
  Serial.begin(115200);

  lcd.begin(16, 2); 
  lcd.print("Alcala SkyGuard");
  delay(2000);
  lcd.clear();

  pinMode(LED_R, OUTPUT);
  pinMode(LED_G, OUTPUT);
  pinMode(LED_B, OUTPUT);

  setup_wifi();

  client.setBufferSize(1024); 
  client.setServer(mqtt_server, mqtt_port);
  
  // Registrar la función callback (NUEVO)
  client.setCallback(callback); 

  randomSeed(analogRead(35));
}

// -------------------------------------------------------
// ---------- LOOP (MODIFICADO) ----------
// -------------------------------------------------------
void loop() {
  if (!client.connected()) {
    reconnect();
  }
  // client.loop() es CRÍTICO: revisa si han llegado nuevos mensajes
  client.loop(); 

  // --- Lectura de sensores ---
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();
  int lightValue = analogRead(LDR_PIN);

  // --- Lógica de estado de luz y LEDs (MODIFICADA) ---
  String lightStatus;
  
  // Solo controla los LEDs con el LDR si NO está en modo manual
  if (!manualLedControl) {
    if (lightValue < 30) {
      lightStatus = "Oscuro";
      digitalWrite(LED_R, HIGH);
      digitalWrite(LED_G, LOW);
      digitalWrite(LED_B, LOW);
    } else if (lightValue < 60) {
      lightStatus = "Medio";
      digitalWrite(LED_R, LOW);
      digitalWrite(LED_G, HIGH);
      digitalWrite(LED_B, LOW);
    } else {
      lightStatus = "Claro";
      digitalWrite(LED_R, LOW);
      digitalWrite(LED_G, LOW);
      digitalWrite(LED_B, HIGH);
    }
  } else {
    // Si está en modo manual, el JSON reportará "Manual"
    lightStatus = "Manual"; 
  }

  // ---------- Mostrar datos en LCD ----------
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("T:");
  lcd.print(temperature, 1);
  lcd.print("C H:");
  lcd.print(humidity, 0);
  lcd.print("%");

  lcd.setCursor(0, 1);
  lcd.print("Luz:");
  lcd.print(lightStatus); // <-- Mostrará "Manual" si se controla por MQTT

  // ---------- Crear JSON con ArduinoJson ----------
  StaticJsonDocument<1024> doc;

  doc["sensor_id"] = mqtt_client_id;
  doc["sensor_type"] = "weather";
  doc["street_id"] = "ST_0889";
  doc["timestamp"] = "2025-10-19T12:00:00Z"; 

  JsonObject loc = doc.createNestedObject("location");
  loc["latitude"] = 40.38503;
  loc["longitude"] = -3.651995;
  loc["district"] = "Moratalaz";
  loc["neighborhood"] = "Moratalaz";

  JsonObject data = doc.createNestedObject("data");
  data["temperature_celsius"] = temperature;
  data["humidity_percent"] = humidity;
  data["light_intensity"] = lightValue;
  data["light_status"] = lightStatus; // <-- Reporta el estado actual

  // ---------- Serializar y Publicar ----------
  String payload;
  serializeJson(doc, payload);

  // Publica en el topic de *sensores*
  bool ok = client.publish(mqtt_topic_publish, payload.c_str());
  
  if (ok) {
    Serial.println("[MQTT] Estado publicado en " + String(mqtt_topic_publish));
    // Serial.println(payload); // Opcional: descomentar si quieres ver el JSON enviado
  } else {
    Serial.println("[MQTT] ERROR al publicar estado.");
  }

  delay(5000); // cada 10 segundos
}