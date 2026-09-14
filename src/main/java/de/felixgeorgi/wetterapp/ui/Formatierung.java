package de.felixgeorgi.wetterapp.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Einheitliche Aufbereitung von Zahlen und Zeiten fuer die Anzeige.
 * <p>
 * Die Formatierung ist bewusst hier gebuendelt und nicht ueber die
 * Oberflaeche verstreut. Soll die Anwendung spaeter Fahrenheit anzeigen oder
 * eine andere Sprache unterstuetzen, ist nur diese Klasse betroffen.
 *
 * @author Felix Georgi
 */
public final class Formatierung {

    private static final DateTimeFormatter UHRZEIT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATUM =
            DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN);

    /** Reine Hilfsklasse: nicht instanziierbar. */
    private Formatierung() {
    }

    /**
     * Rundet eine Temperatur auf ganze Grad.
     *
     * @param celsius Temperatur in Grad Celsius
     * @return zum Beispiel {@code "18°"}
     */
    public static String temperatur(double celsius) {
        return Math.round(celsius) + "°";
    }

    /**
     * Gibt eine Temperatur mit einer Nachkommastelle aus.
     *
     * @param celsius Temperatur in Grad Celsius
     * @return zum Beispiel {@code "18,4 °C"}
     */
    public static String temperaturGenau(double celsius) {
        return String.format(Locale.GERMAN, "%.1f °C", celsius);
    }

    /**
     * Gibt eine Windgeschwindigkeit aus.
     *
     * @param kmh Geschwindigkeit in Kilometern je Stunde
     * @return zum Beispiel {@code "12 km/h"}
     */
    public static String wind(double kmh) {
        return Math.round(kmh) + " km/h";
    }

    /**
     * Gibt einen Prozentwert aus.
     *
     * @param prozent Wert zwischen 0 und 100
     * @return zum Beispiel {@code "60 %"}
     */
    public static String prozent(int prozent) {
        return prozent + " %";
    }

    /**
     * Gibt die Uhrzeit eines Zeitpunkts aus.
     *
     * @param zeitpunkt darzustellender Zeitpunkt
     * @return zum Beispiel {@code "18:00"}
     */
    public static String uhrzeit(LocalDateTime zeitpunkt) {
        return zeitpunkt.format(UHRZEIT);
    }

    /**
     * Gibt ein Datum in ausgeschriebener Form aus.
     *
     * @param datum darzustellendes Datum
     * @return zum Beispiel {@code "14. September"}
     */
    public static String datum(LocalDate datum) {
        return datum.format(DATUM);
    }

    /**
     * Beschreibt einen Zeitpunkt als Stundenangabe fuer den Verlauf.
     * Die laufende Stunde wird mit "Jetzt" hervorgehoben.
     *
     * @param zeitpunkt  darzustellender Zeitpunkt
     * @param jetztAmOrt aktueller Zeitpunkt in der Zeitzone des Ortes
     * @return {@code "Jetzt"} oder die Uhrzeit
     */
    public static String stundenBeschriftung(LocalDateTime zeitpunkt, LocalDateTime jetztAmOrt) {
        boolean gleicheStunde = zeitpunkt.getHour() == jetztAmOrt.getHour()
                && zeitpunkt.toLocalDate().equals(jetztAmOrt.toLocalDate());
        return gleicheStunde ? "Jetzt" : uhrzeit(zeitpunkt);
    }
}
