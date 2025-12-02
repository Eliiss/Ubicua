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
        Log.log.info("-- Recibido valores desde Web --");
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            // 1. Obtén los valores de temperatura, humedad, luz del request (por GET o POST)
            String temperaturaStr = request.getParameter("temperature");
            String humedadStr = request.getParameter("humidity");
            String luzStr = request.getParameter("light");

            // Validaciones básicas: revisa que todos estén presentes y sean numéricos
            if (temperaturaStr == null || humedadStr == null || luzStr == null) {
                throw new NumberFormatException("Faltan parámetros");
            }
            float temperatura = Float.parseFloat(temperaturaStr);
            float humedad = Float.parseFloat(humedadStr);
            int luz = Integer.parseInt(luzStr);

            Log.log.info("Valores recibidos - Temp: " + temperatura + " Hum: " + humedad + " Luz: " + luz);

            // 2. PUBLICAR cada valor en su topic MQTT
            new Thread(() -> {
                MQTTBroker broker = new MQTTBroker();
                MQTTPublisher.publish(broker, "ubicomp/temperatura", String.valueOf(temperatura));
                MQTTPublisher.publish(broker, "ubicomp/humedad", String.valueOf(humedad));
                MQTTPublisher.publish(broker, "ubicomp/luz", String.valueOf(luz));
            }).start();

            // 3. Respondemos OK
            out.println("OK");
        } catch (NumberFormatException nfe) {
            out.println("-1");
            Log.log.error("El valor enviado no es válido: " + nfe);
        } catch (Exception e) {
            out.println("-1");
            Log.log.error("Error publicando: " + e);
        } finally {
            out.close();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response); // soporta tanto GET como POST, igual que antes
    }
}