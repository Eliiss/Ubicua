package servlets;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import logic.Log;
import logic.Logic;
import logic.Measurement;

@WebServlet("/GetData")
public class GetData extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public GetData() { super(); }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            ArrayList<Measurement> values = Logic.getDataFromDB(); // Consulta mediciones (temperatura, humedad, luz, timestamp)
            String jsonMeasurements = new Gson().toJson(values);
            Log.log.info("Values => " + jsonMeasurements);
            out.println(jsonMeasurements);
        } catch (Exception e) {
            out.println("-1");
            Log.log.error("Exception: " + e);
        } finally {
            out.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response); // Por convención, GET y POST devuelven lo mismo aquí
    }
}