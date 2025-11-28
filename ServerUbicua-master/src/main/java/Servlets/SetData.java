package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import logic.Log;
import mqtt.MQTTBroker;
import mqtt.MQTTPublisher;

@WebServlet("/SetData")
public class SetData extends HttpServlet {
    private static final long serialVersionUID = 1L;
       
    public SetData() { super(); }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Log.log.info("-- Recibido valor desde Web --");
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            // 1. Obtenemos el valor
            String valueStr = request.getParameter("value");
            // Validamos que sea número para no enviar basura, aunque lo enviamos como String
            int value = Integer.parseInt(valueStr); 

            // 2. PUBLICAMOS EN MQTT (El suscriptor se encargará de guardarlo en la DB)
            // Usamos un hilo rápido para no hacer esperar al usuario web
            new Thread(() -> {
                MQTTBroker broker = new MQTTBroker();
                // Publicamos en el MISMO topic que escuchamos en Projectinitializer
                MQTTPublisher.publish(broker, "ubicomp/temperatura", String.valueOf(value));
            }).start();
            
            // 3. Respondemos OK
            Log.log.info("Valor publicado en MQTT: " + value);
            out.println("OK"); // Respondemos algo simple para que AJAX sepa que fue bien

        } catch (NumberFormatException nfe) {
            out.println("-1");
            Log.log.error("El valor no es un numero valido: " + nfe);
        } catch (Exception e) {
            out.println("-1");
            Log.log.error("Error publicando: " + e);
        } finally {
            out.close();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}