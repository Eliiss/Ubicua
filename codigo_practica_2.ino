#include <WiFi.h>           // Conectarse al wifi
#include <PubSubClient.h>   // Implementar protocolo MQTT 
#include <DHT11.h>          // Sensor de humedad y temperatura
#include <LiquidCrystal.h>  // Para el LCD
#include <ArduinoJson.h>    // Para el JSON

// ---------- CONFIGURACIÓN DE RED ----------
const char* ssid = "MOVISTAR_1038";
const char* password = "3kV3rYeXRWJmEHe9g7g4";

// ---------- CONFIGURACIÓN MQTT ----------
const char* mqtt_server = "192.168.0.13";   // IP del broker Mosquitto
const int mqtt_port = 1883;
const char* mqtt_client_id = "LAB12JAV-G3";

// --- Topics de ENVÍO (NUEVOS: Separados por tipo) ---
const char* topic_temp = "ubicua_db/temperatura";
const char* topic_hum  = "ubicua_db/humedad";
const char* topic_luz  = "ubicua_db/luz";

// --- Topic de RECEPCIÓN (Comandos) ---
const char* mqtt_topic_command = "ubicua_db/led/set"; 

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
bool manualLedControl = false; // Flag para anular el LDR

// -------------------------------------------------------
// ---------- FUNCIÓN CALLBACK (ACTUADOR) ----------
// -------------------------------------------------------
void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Mensaje recibido en [");
  Serial.print(topic);
  Serial.print("]: ");

  String message = "";
  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }
  Serial.println(message);

  if (String(topic) == mqtt_topic_command) {
    if (message == "red") {
      manualLedControl = true;
      digitalWrite(LED_R, HIGH); digitalWrite(LED_G, LOW); digitalWrite(LED_B, LOW);
      Serial.println("Cambiando LED a ROJO");
    } 
    else if (message == "green") {
      manualLedControl = true;
      digitalWrite(LED_R, LOW); digitalWrite(LED_G, HIGH); digitalWrite(LED_B, LOW);
      Serial.println("Cambiando LED a VERDE");
    } 
    else if (message == "blue") {
      manualLedControl = true;
      digitalWrite(LED_R, LOW); digitalWrite(LED_G, LOW); digitalWrite(LED_B, HIGH);
      Serial.println("Cambiando LED a AZUL");
    } 
    else if (message == "off") {
      manualLedControl = true;
      digitalWrite(LED_R, LOW); digitalWrite(LED_G, LOW); digitalWrite(LED_B, LOW);
      Serial.println("Apagando LEDs");
    } 
    else if (message == "auto") {
      manualLedControl = false;
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

void reconnect() {
  while (!client.connected()) {
    Serial.print("Intentando conexión MQTT...");
    if (client.connect(mqtt_client_id)) {
      Serial.println("Conectado al broker MQTT!");
      if (client.subscribe(mqtt_topic_command)) {
        Serial.println("Suscrito a " + String(mqtt_topic_command));
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
// ---------- SETUP ----------
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
  client.setCallback(callback); 

  randomSeed(analogRead(35));
}

// -------------------------------------------------------
// ---------- LOOP (MODIFICADO PARA 3 TOPICS) ----------
// -------------------------------------------------------
void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop(); 

  // 1. Lectura de sensores
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();
  int lightValue = analogRead(LDR_PIN);

  // 2. Lógica LEDs
  String lightStatus;
  if (!manualLedControl) {
    if (lightValue < 30) {
      lightStatus = "Oscuro";
      digitalWrite(LED_R, HIGH); digitalWrite(LED_G, LOW); digitalWrite(LED_B, LOW);
    } else if (lightValue < 60) {
      lightStatus = "Medio";
      digitalWrite(LED_R, LOW); digitalWrite(LED_G, HIGH); digitalWrite(LED_B, LOW);
    } else {
      lightStatus = "Claro";
      digitalWrite(LED_R, LOW); digitalWrite(LED_G, LOW); digitalWrite(LED_B, HIGH);
    }
  } else {
    lightStatus = "Manual"; 
  }

  // 3. Mostrar en LCD
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("T:"); lcd.print(temperature, 1);
  lcd.print(" H:"); lcd.print(humidity, 0);
  lcd.setCursor(0, 1);
  lcd.print("Luz:"); lcd.print(lightStatus);

  // ---------------------------------------------------
  // ENVÍO DE DATOS SEPARADOS (3 PUBLICACIONES)
  // ---------------------------------------------------

  // --- A) Publicar TEMPERATURA ---
  {
    StaticJsonDocument<512> doc;
    doc["sensor_id"] = mqtt_client_id;
    doc["timestamp"] = "2025-10-19T12:00:00Z";
    
    // Solo enviamos temperatura
    doc["data"]["temperature_celsius"] = temperature;

    String payload;
    serializeJson(doc, payload);
    client.publish(topic_temp, payload.c_str());
    Serial.println("[Temp] Enviado a: " + String(topic_temp));
  }

  // --- B) Publicar HUMEDAD ---
  {
    StaticJsonDocument<512> doc;
    doc["sensor_id"] = mqtt_client_id;
    doc["timestamp"] = "2025-10-19T12:00:00Z";
    
    // Solo enviamos humedad
    doc["data"]["humidity_percent"] = humidity;

    String payload;
    serializeJson(doc, payload);
    client.publish(topic_hum, payload.c_str());
    Serial.println("[Hum] Enviado a: " + String(topic_hum));
  }

  // --- C) Publicar LUZ ---
  {
    StaticJsonDocument<512> doc;
    doc["sensor_id"] = mqtt_client_id;
    doc["timestamp"] = "2025-10-19T12:00:00Z";
    
    // Enviamos valor crudo y estado
    doc["data"]["light_intensity"] = lightValue;
    doc["data"]["light_status"] = lightStatus;

    String payload;
    serializeJson(doc, payload);
    client.publish(topic_luz, payload.c_str());
    Serial.println("[Luz] Enviado a: " + String(topic_luz));
  }

  delay(5000); // Esperar 5 segundos antes del siguiente ciclo
}