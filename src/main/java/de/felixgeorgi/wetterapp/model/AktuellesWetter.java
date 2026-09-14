package de.felixgeorgi.wetterapp.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Momentaufnahme des Wetters an einem Ort.
 *
 * @param zeitpunkt        Zeitpunkt der Messung in der Ortszeit
 * @param temperatur       Lufttemperatur in Grad Celsius
 * @param gefuehlt         gefuehlte Temperatur in Grad Celsius
 * @param luftfeuchte      relative Luftfeuchte in Prozent
 * @param windGeschwindigkeit Windgeschwindigkeit in km/h
 * @param windRichtung     Windrichtung in Grad (0 = Nord, 90 = Ost)
 * @param wetterCode       Wetterlage
 * @param istTag           {@code true}, wenn die Sonne am Ort gerade ueber dem Horizont steht
 *
 * @author Felix Georgi
 */
public record AktuellesWetter(
        LocalDateTime zeitpunkt,
        double temperatur,
        double gefuehlt,
        int luftfeuchte,
        double windGeschwindigkeit,
        int windRichtung,
        WetterCode wetterCode,
        boolean istTag) {

    public AktuellesWetter {
        Objects.requireNonNull(zeitpunkt, "zeitpunkt darf nicht null sein");
        Objects.requireNonNull(wetterCode, "wetterCode darf nicht null sein");
    }

    /**
     * Wandelt die Windrichtung in Grad in eine Himmelsrichtung um.
     * <p>
     * Der Vollkreis wird in acht Sektoren zu je 45 Grad geteilt. Durch das
     * kaufmaennische Runden faellt jeder Winkel in den naechstgelegenen Sektor;
     * das abschliessende {@code % 8} bildet den Ueberlauf bei 360 Grad wieder
     * auf Norden ab.
     *
     * @return Kuerzel der Himmelsrichtung, zum Beispiel "NO"
     */
    public String windRichtungKurz() {
        String[] richtungen = {"N", "NO", "O", "SO", "S", "SW", "W", "NW"};
        int sektor = (int) Math.round(((windRichtung % 360) / 45.0)) % 8;
        return richtungen[sektor];
    }
}
