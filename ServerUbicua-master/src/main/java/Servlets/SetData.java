package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
            String temperaturaStr = request.getParameter("temperature");
            String humedadStr = request.getParameter("humidity");
            String luzStr = request.getParameter("light");

            if (temperaturaStr == null || humedadStr == null || luzStr == null) {
                throw new NumberFormatException("Faltan parámetros");
            }
            // Validamos que sean números
            Float.parseFloat(temperaturaStr);
            Float.parseFloat(humedadStr);
            Integer.parseInt(luzStr);

            // Generar Timestamp actual con el formato que espera el suscriptor
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Construir los JSONs manualmente (o podrías usar GSON) para coincidir con MQTTSuscriber
            // Formato esperado: { "timestamp": "...", "data": { "CLAVE_ESPECIFICA": VALOR } }
            
            String jsonTemp = String.format("{\"timestamp\":\"%s\", \"data\":{\"temperature_celsius\":%s}}", ts, temperaturaStr);
            String jsonHum = String.format("{\"timestamp\":\"%s\", \"data\":{\"humidity_percent\":%s}}", ts, humedadStr);
            String jsonLuz = String.format("{\"timestamp\":\"%s\", \"data\":{\"light_intensity\":%s}}", ts, luzStr);

            Log.log.info("Publicando JSONs simulados: " + jsonTemp);

            new Thread(() -> {
                MQTTBroker broker = new MQTTBroker();
                // Usamos los topics separados que definiste
                MQTTPublisher.publish(broker, "ubicua_db/temperatura", jsonTemp);
                MQTTPublisher.publish(broker, "ubicua_db/humedad", jsonHum);
                MQTTPublisher.publish(broker, "ubicua_db/luz", jsonLuz);
            }).start();

            out.println("OK");
        } catch (Exception e) {
            out.println("-1");
            Log.log.error("Error en SetData: " + e);
        } finally {
            out.close();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}