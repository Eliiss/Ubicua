package logic;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import mqtt.MQTTBroker;
import mqtt.MQTTPublisher;
import mqtt.MQTTSuscriber;

@WebListener
public class Projectinitializer implements ServletContextListener {
    
    // Lo hacemos estático para poder acceder si fuera necesario, 
    // aunque aquí basta con mantener la referencia viva.
    private MQTTSuscriber suscriber;

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Aquí podrías desconectar si quisieras ser muy limpio
        if(suscriber != null) {
            // suscriber.disconnect(); // Si implementaras un método disconnect
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Log.log.info("--> Iniciando Sistema Ubicomp <--");

        // IMPORTANTE: Lanzamos un hilo nuevo para no bloquear el arranque de Tomcat
        new Thread(() -> {
            try {
                // Pequeña pausa para asegurar que Tomcat ha cargado lo básico
                Thread.sleep(1000); 
                
                Log.log.info("--> Suscribe Topics <--");
                MQTTBroker broker = new MQTTBroker();
                suscriber = new MQTTSuscriber(broker);
                
                // CAMBIO: Escuchamos solo nuestro tema específico, no todo (#)
                suscriber.subscribeTopic("ubicomp/temperatura");
                suscriber.subscribeTopic("ubicomp/humedad");
                suscriber.subscribeTopic("ubicomp/luz");
                suscriber.subscribeTopic("ubicomp/led/set");
              
                
                // Mensaje de prueba
                MQTTPublisher.publish(broker, "ubicomp/test", "Servidor Tomcat Iniciado y Escuchando");
                
            } catch (Exception e) {
                Log.log.error("Error iniciando MQTT (El servidor web sigue funcionando): " + e.getMessage());
            }
        }).start();
    }
}