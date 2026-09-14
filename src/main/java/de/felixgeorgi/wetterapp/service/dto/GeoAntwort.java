package de.felixgeorgi.wetterapp.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Abbild der Antwort des Open-Meteo-Geocoding-Dienstes.
 * <p>
 * Klassen in diesem Paket bilden ausschliesslich die JSON-Struktur der API ab
 * ("Data Transfer Objects"). Sie werden nicht in der Oberflaeche verwendet,
 * sondern von {@code OpenMeteoWetterService} in die Modellklassen des Pakets
 * {@code model} uebersetzt. Aendert die API ihr Format, muss nur dieses Paket
 * angepasst werden.
 *
 * @param ergebnisse gefundene Orte, {@code null} wenn die Suche nichts geliefert hat
 *
 * @author Felix Georgi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoAntwort(@JsonProperty("results") List<GeoTreffer> ergebnisse) {

    /**
     * Ein einzelner Ortstreffer der Geocoding-Suche.
     *
     * @param name       Ortsname
     * @param breite     geografische Breite
     * @param laenge     geografische Laenge
     * @param land       Staatsname
     * @param region     Bundesland oder Region (Feld {@code admin1} der API)
     * @param zeitzone   IANA-Zeitzone
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeoTreffer(
            @JsonProperty("name") String name,
            @JsonProperty("latitude") double breite,
            @JsonProperty("longitude") double laenge,
            @JsonProperty("country") String land,
            @JsonProperty("admin1") String region,
            @JsonProperty("timezone") String zeitzone) {
    }
}
