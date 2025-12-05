package logic;

import Database.ConectionDDBB;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Logic 
{
    /***
     * Consulta los últimos 100 registros de SENSOR_DATA y los devuelve como lista.
     */
    public static ArrayList<Measurement> getDataFromDB() {
        ArrayList<Measurement> values = new ArrayList<>();
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;

        try {
            con = conector.obtainConnection(true);
            Log.log.info("Database Connected");

            PreparedStatement ps = ConectionDDBB.GetDataBD(con);
            Log.log.info("Query => " + ps.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Measurement measure = new Measurement();
                measure.setTemperature(rs.getFloat("temperature"));
                measure.setHumidity(rs.getFloat("humidity"));
                measure.setLight(rs.getInt("light"));
                measure.setTimestamp(rs.getTimestamp("timestamp"));
                values.add(measure);
            }
        } catch (SQLException | NullPointerException e) {
            Log.log.error("Error: " + e);
            values = new ArrayList<>();
        } finally {
            conector.closeConnection(con);
        }
        return values;
    }

    /**
     * Inserta un nuevo registro en la tabla SENSOR_DATA.
     */
    public static void setDataToDB(Float temperature, Float humidity, Integer light, Timestamp ts) {
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        try {
            con = conector.obtainConnection(true);  // ← CAMBIAR A TRUE
            Log.log.info("Database Connected");

            PreparedStatement ps = ConectionDDBB.SetDataBD(con);
            ps.setFloat(1, temperature);
            ps.setFloat(2, humidity);
            ps.setInt(3, light);
            ps.setTimestamp(4, ts);
            Log.log.info("Query => " + ps.toString());
            ps.executeUpdate();

            Log.log.info("Data saved successfully");

        } catch (SQLException | NullPointerException e) {
            Log.log.error("Error: " + e);
        } finally {
            conector.closeConnection(con);
        }
    }
}