package de.felixgeorgi.wetterapp.model;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

/**
 * Vorhersage fuer einen ganzen Tag.
 * <p>
 * Im Vorgaengerprojekt wurden diese Angaben als fertig formatierter Text
 * durchgereicht und die Wetterkennung hinten an die Zeichenkette gehaengt
 * ("Mo: Min 3,0°C / Max 9,0°C, Regen |id:500"). Die Oberflaeche musste den
 * Text anschliessend wieder zerlegen. Hier bleiben die Werte typisiert, die
 * Formatierung passiert erst in der Anzeige.
 *
 * @param datum               Kalendertag
 * @param minTemperatur       Tagestiefstwert in Grad Celsius
 * @param maxTemperatur       Tageshoechstwert in Grad Celsius
 * @param niederschlagsRisiko hoechste Niederschlagswahrscheinlichkeit des Tages in Prozent
 * @param wetterCode          vorherrschende Wetterlage des Tages
 *
 * @author Felix Georgi
 */
public record TagesVorhersage(
        LocalDate datum,
        double minTemperatur,
        double maxTemperatur,
        int niederschlagsRisiko,
        WetterCode wetterCode) {

    public TagesVorhersage {
        Objects.requireNonNull(datum, "datum darf nicht null sein");
        Objects.requireNonNull(wetterCode, "wetterCode darf nicht null sein");
        if (minTemperatur > maxTemperatur) {
            throw new IllegalArgumentException(
                    "Tiefstwert (" + minTemperatur + ") liegt ueber dem Hoechstwert (" + maxTemperatur + ")");
        }
    }

    /**
     * Liefert den abgekuerzten deutschen Wochentag, zum Beispiel "Mo".
     * <p>
     * Java liefert die Kurzform mit Abkuerzungspunkt ("Mo."). Der Punkt wird
     * entfernt, weil die Wochentage in der Vorhersage untereinander stehen und
     * ohne ihn ruhiger wirken.
     *
     * @return zweibuchstabiges Kuerzel des Wochentags
     */
    public String wochentagKurz() {
        String kuerzel = datum.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.GERMAN);
        return kuerzel.endsWith(".") ? kuerzel.substring(0, kuerzel.length() - 1) : kuerzel;
    }

    /**
     * Prueft, ob dieser Eintrag den heutigen Tag beschreibt.
     *
     * @param heute das aktuelle Datum am Ort der Vorhersage
     * @return {@code true}, wenn der Eintrag fuer heute gilt
     */
    public boolean istHeute(LocalDate heute) {
        return datum.equals(heute);
    }
}
