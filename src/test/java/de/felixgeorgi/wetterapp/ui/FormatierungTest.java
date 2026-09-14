package de.felixgeorgi.wetterapp.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests der Anzeigeformatierung.
 * <p>
 * Die Klasse {@link Formatierung} enthaelt keine JavaFX-Elemente und laesst
 * sich deshalb wie jede andere Klasse testen, ohne dass dafuer ein Fenster
 * geoeffnet werden muesste. Genau dafuer wurde die Formatierung aus dem
 * Controller herausgeloest.
 *
 * @author Felix Georgi
 */
@DisplayName("Formatierung: Aufbereitung fuer die Anzeige")
class FormatierungTest {

    @ParameterizedTest(name = "{0} Grad wird als {1} angezeigt")
    @CsvSource({
            "19.3, 19°",
            "19.5, 20°",
            "-0.4, 0°",
            "-2.6, -3°",
            "0.0, 0°"
    })
    @DisplayName("rundet Temperaturen kaufmaennisch auf ganze Grad")
    void rundetTemperaturen(double celsius, String erwartet) {
        assertEquals(erwartet, Formatierung.temperatur(celsius));
    }

    @Test
    @DisplayName("nutzt bei genauen Temperaturen das deutsche Dezimalkomma")
    void deutschesDezimaltrennzeichen() {
        assertEquals("19,3 °C", Formatierung.temperaturGenau(19.3));
    }

    @Test
    @DisplayName("gibt Wind und Luftfeuchte mit Einheit aus")
    void einheitenWerdenAngehaengt() {
        assertEquals("13 km/h", Formatierung.wind(12.6));
        assertEquals("64 %", Formatierung.prozent(64));
    }

    @Test
    @DisplayName("stellt Uhrzeiten zweistellig dar")
    void uhrzeitIstZweistellig() {
        assertEquals("07:05", Formatierung.uhrzeit(LocalDateTime.of(2026, 9, 14, 7, 5)));
    }

    @Test
    @DisplayName("schreibt den Monatsnamen auf Deutsch aus")
    void datumAufDeutsch() {
        assertEquals("14. September", Formatierung.datum(LocalDate.of(2026, 9, 14)));
    }

    @Test
    @DisplayName("beschriftet die laufende Stunde mit \"Jetzt\"")
    void laufendeStundeHeisstJetzt() {
        LocalDateTime jetzt = LocalDateTime.of(2026, 9, 14, 15, 42);

        assertEquals("Jetzt", Formatierung.stundenBeschriftung(
                LocalDateTime.of(2026, 9, 14, 15, 0), jetzt));
        assertEquals("16:00", Formatierung.stundenBeschriftung(
                LocalDateTime.of(2026, 9, 14, 16, 0), jetzt));
    }

    @Test
    @DisplayName("verwechselt dieselbe Stunde am naechsten Tag nicht mit \"Jetzt\"")
    void gleicheStundeAnderenTags() {
        // Der Verlauf reicht 24 Stunden weit und enthaelt deshalb dieselbe
        // Uhrzeit ein zweites Mal. Nur der erste Eintrag ist "Jetzt".
        LocalDateTime jetzt = LocalDateTime.of(2026, 9, 14, 15, 42);

        assertEquals("15:00", Formatierung.stundenBeschriftung(
                LocalDateTime.of(2026, 9, 15, 15, 0), jetzt));
    }
}
