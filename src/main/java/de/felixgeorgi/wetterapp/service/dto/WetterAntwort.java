package de.felixgeorgi.wetterapp.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Abbild der Antwort des Open-Meteo-Vorhersagedienstes.
 * <p>
 * Open-Meteo liefert Zeitreihen als parallele Arrays: {@code hourly.time[i]}
 * gehoert zu {@code hourly.temperature_2m[i]}. Diese Struktur wird hier
 * unveraendert uebernommen und erst beim Uebersetzen in die Modellklassen
 * zu Objekten je Zeitpunkt zusammengefuehrt.
 *
 * @param zeitzone Zeitzone, in der die Zeitangaben stehen
 * @param aktuell  aktuelle Messwerte
 * @param stunden  stuendliche Zeitreihen
 * @param tage     taegliche Zeitreihen
 *
 * @author Felix Georgi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WetterAntwort(
        @JsonProperty("timezone") String zeitzone,
        @JsonProperty("current") Aktuell aktuell,
        @JsonProperty("hourly") Stunden stunden,
        @JsonProperty("daily") Tage tage) {

    /**
     * Aktuelle Messwerte am Ort.
     *
     * @param zeit            Zeitstempel im Format {@code yyyy-MM-dd'T'HH:mm}
     * @param temperatur      Temperatur in Grad Celsius
     * @param gefuehlt        gefuehlte Temperatur in Grad Celsius
     * @param luftfeuchte     relative Luftfeuchte in Prozent
     * @param istTag          1 = Tag, 0 = Nacht
     * @param wetterCode      WMO-Wettercode
     * @param windGeschwindigkeit Windgeschwindigkeit in km/h
     * @param windRichtung    Windrichtung in Grad
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Aktuell(
            @JsonProperty("time") String zeit,
            @JsonProperty("temperature_2m") Double temperatur,
            @JsonProperty("apparent_temperature") Double gefuehlt,
            @JsonProperty("relative_humidity_2m") Integer luftfeuchte,
            @JsonProperty("is_day") Integer istTag,
            @JsonProperty("weather_code") Integer wetterCode,
            @JsonProperty("wind_speed_10m") Double windGeschwindigkeit,
            @JsonProperty("wind_direction_10m") Integer windRichtung) {
    }

    /**
     * Stuendliche Zeitreihen. Alle Listen sind gleich lang und index-parallel.
     *
     * @param zeit                 Zeitstempel je Stunde
     * @param temperatur           Temperatur je Stunde
     * @param wetterCode           Wettercode je Stunde
     * @param niederschlagsRisiko  Niederschlagswahrscheinlichkeit je Stunde
     * @param istTag               Tag-/Nachtkennung je Stunde
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stunden(
            @JsonProperty("time") List<String> zeit,
            @JsonProperty("temperature_2m") List<Double> temperatur,
            @JsonProperty("weather_code") List<Integer> wetterCode,
            @JsonProperty("precipitation_probability") List<Integer> niederschlagsRisiko,
            @JsonProperty("is_day") List<Integer> istTag) {
    }

    /**
     * Taegliche Zeitreihen. Alle Listen sind gleich lang und index-parallel.
     *
     * @param datum                Datum je Tag
     * @param wetterCode           vorherrschender Wettercode je Tag
     * @param maxTemperatur        Tageshoechstwert je Tag
     * @param minTemperatur        Tagestiefstwert je Tag
     * @param niederschlagsRisiko  hoechste Niederschlagswahrscheinlichkeit je Tag
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tage(
            @JsonProperty("time") List<String> datum,
            @JsonProperty("weather_code") List<Integer> wetterCode,
            @JsonProperty("temperature_2m_max") List<Double> maxTemperatur,
            @JsonProperty("temperature_2m_min") List<Double> minTemperatur,
            @JsonProperty("precipitation_probability_max") List<Integer> niederschlagsRisiko) {
    }
}
