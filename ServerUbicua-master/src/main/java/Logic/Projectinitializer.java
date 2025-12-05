package logic;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import mqtt.MQTTBroker;
import mqtt.MQTTSuscriber;

@WebListener
public class Projectinitializer implements ServletContextListener {
    
    private MQTTSuscriber suscriber;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Log.log.info("--> Iniciando Sistema Ubicomp <--");

        new Thread(() -> {
            try {
                Thread.sleep(3000); // Esperamos a que todo cargue
                
                MQTTBroker broker = new MQTTBroker();
                suscriber = new MQTTSuscriber(broker);
                
                // 1. CONECTAR (Solo 1 vez)
                suscriber.connect();
                
                // 2. SUSCRIBIRSE (A todo)
                suscriber.subscribeTopic("ubicua_db/temperatura");
                suscriber.subscribeTopic("ubicua_db/humedad");
                suscriber.subscribeTopic("ubicua_db/luz");
                suscriber.subscribeTopic("ubicua_db/led/set");
                
            } catch (Exception e) {
                Log.log.error("Error fatal MQTT: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}