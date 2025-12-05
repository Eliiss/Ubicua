package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Statement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import logic.Log;
import Database.ConectionDDBB;

@WebServlet("/DeleteData")
public class DeleteData extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public DeleteData() { super(); }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Log.log.info("-- Solicitud de eliminación de todos los datos --");
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        
        try {
            con = conector.obtainConnection(false);
            Log.log.info("Database Connected");

            Statement stmt = con.createStatement();
            int rowsDeleted = stmt.executeUpdate("DELETE FROM SENSOR_DATA");
            
            conector.closeTransaction(con);
            Log.log.info("Registros eliminados: " + rowsDeleted);
            out.println("OK");
            
        } catch (Exception e) {
            Log.log.error("Error al eliminar datos: " + e);
            out.println("-1");
            if (con != null) {
                conector.cancelTransaction(con);
            }
        } finally {
            conector.closeConnection(con);
            out.close();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}