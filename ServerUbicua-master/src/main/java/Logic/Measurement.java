package logic;

import java.sql.Timestamp;

public class Measurement 
{
    private Float temperature;
    private Float humidity;
    private Integer light;
    private Timestamp timestamp;

    // Constructor vacío
    public Measurement() {}

    // Constructor con todos los campos
    public Measurement(Float temperature, Float humidity, Integer light, Timestamp timestamp) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.light = light;
        this.timestamp = timestamp;
    }

    // Getters y Setters
    public Float getTemperature() {
        return temperature;
    }

    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }

    public Float getHumidity() {
        return humidity;
    }

    public void setHumidity(Float humidity) {
        this.humidity = humidity;
    }

    public Integer getLight() {
        return light;
    }

    public void setLight(Integer light) {
        this.light = light;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}