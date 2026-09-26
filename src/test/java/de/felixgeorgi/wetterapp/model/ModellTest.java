package de.felixgeorgi.wetterapp.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests der Modellklassen.
 * <p>
 * Die Modellklassen pruefen ihre Werte bereits im Konstruktor. Ein Objekt mit
 * widerspruechlichem Inhalt kann dadurch gar nicht erst entstehen, und ein
 * Fehler faellt dort auf, wo er verursacht wird, statt spaeter in der
 * Oberflaeche.
 *
 * @author Felix Georgi
 */
@DisplayName("Modellklassen")
class ModellTest {

    @Nested
    @DisplayName("Ort")
    class OrtTest {

        @Test
        @DisplayName("weist Koordinaten ausserhalb des gueltigen Bereichs zurueck")
        void pruefungDerKoordinaten() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Ort("Nirgendwo", null, null, 95.0, 0.0, "UTC"));
            assertThrows(IllegalArgumentException.class,
                    () -> new Ort("Nirgendwo", null, null, 0.0, 200.0, "UTC"));
        }

        @Test
        @DisplayName("verlangt einen Namen")
        void nameIstPflicht() {
            assertThrows(NullPointerException.class,
                    () -> new Ort(null, null, null, 0.0, 0.0, "UTC"));
        }

        @Test
        @DisplayName("laesst fehlende Region und fehlendes Land im Anzeigenamen weg")
        void anzeigenameOhneZusaetze() {
            Ort ort = new Ort("Atlantis", null, null, 0.0, 0.0, "UTC");

            assertEquals("Atlantis", ort.anzeigeName());
        }

        @ParameterizedTest(name = "{0} / {1} wird zu \"{2}\"")
        @CsvSource({
                // Stadtstaaten: die Region wiederholt nur den Ort
                "Hamburg,    Hamburg,      'Hamburg, Deutschland'",
                "Berlin,     Land Berlin,  'Berlin, Deutschland'",
                "Bremen,     Freie Hansestadt Bremen, 'Bremen, Deutschland'",
                "berlin,     LAND BERLIN,  'berlin, Deutschland'",
                // echte Regionsangaben bleiben erhalten
                "Mittweida,  Sachsen,      'Mittweida, Sachsen, Deutschland'",
                "Bern,       Bernkastel,   'Bern, Bernkastel, Deutschland'"
        })
        @DisplayName("wiederholt den Ortsnamen nicht als Region")
        void anzeigenameOhneDoppelung(String name, String region, String erwartet) {
            // Bei Stadtstaaten liefert die API Ort und Region nahezu
            // gleichlautend. "Bernkastel" enthaelt zwar die Buchstabenfolge
            // "Bern", ist aber eine andere Angabe und muss stehen bleiben.
            Ort ort = new Ort(name, region, "Deutschland", 53.55, 10.0, "Europe/Berlin");

            assertEquals(erwartet, ort.anzeigeName());
        }
    }

    @Nested
    @DisplayName("AktuellesWetter")
    class AktuellesWetterTest {

        private AktuellesWetter mitWindrichtung(int grad) {
            return new AktuellesWetter(LocalDateTime.of(2026, 9, 14, 15, 0),
                    19.3, 18.1, 64, 12.6, grad, WetterCode.KLAR, true);
        }

        @ParameterizedTest(name = "{0} Grad entspricht {1}")
        @CsvSource({
                "0, N", "20, N", "45, NO", "90, O", "135, SO",
                "180, S", "225, SW", "270, W", "315, NW", "350, N", "359, N"
        })
        @DisplayName("rechnet die Windrichtung in eine Himmelsrichtung um")
        void windrichtung(int grad, String erwartet) {
            assertEquals(erwartet, mitWindrichtung(grad).windRichtungKurz());
        }
    }

    @Nested
    @DisplayName("TagesVorhersage")
    class TagesVorhersageTest {

        @Test
        @DisplayName("weist einen Tiefstwert oberhalb des Hoechstwerts zurueck")
        void widerspruechlicheTemperaturen() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TagesVorhersage(LocalDate.of(2026, 9, 14),
                            20.0, 10.0, 0, WetterCode.KLAR));
        }

        @Test
        @DisplayName("erkennt den heutigen Tag")
        void erkenntHeute() {
            LocalDate heute = LocalDate.of(2026, 9, 14);
            TagesVorhersage tag = new TagesVorhersage(heute, 8.0, 18.0, 30, WetterCode.KLAR);

            assertTrue(tag.istHeute(heute));
            assertFalse(tag.istHeute(heute.plusDays(1)));
        }
    }

    @Nested
    @DisplayName("WetterBericht")
    class WetterBerichtTest {

        @Test
        @DisplayName("ersetzt fehlende Listen durch leere Listen")
        void nullListenWerdenLeer() {
            WetterBericht bericht = new WetterBericht(
                    new Ort("Mittweida", null, null, 50.98, 12.98, "Europe/Berlin"),
                    new AktuellesWetter(LocalDateTime.of(2026, 9, 14, 15, 0),
                            19.3, 18.1, 64, 12.6, 213, WetterCode.KLAR, true),
                    null, null);

            assertTrue(bericht.stunden().isEmpty());
            assertTrue(bericht.tage().isEmpty());
        }

        @Test
        @DisplayName("kopiert die uebergebenen Listen, statt sie zu uebernehmen")
        void listenWerdenKopiert() {
            // Wuerde der Bericht die urspruengliche Liste behalten, koennte ein
            // spaeteres Hinzufuegen den bereits angezeigten Bericht veraendern.
            List<TagesVorhersage> veraenderlich = new java.util.ArrayList<>();
            veraenderlich.add(new TagesVorhersage(
                    LocalDate.of(2026, 9, 14), 8.0, 18.0, 30, WetterCode.KLAR));

            WetterBericht bericht = new WetterBericht(
                    new Ort("Mittweida", null, null, 50.98, 12.98, "Europe/Berlin"),
                    new AktuellesWetter(LocalDateTime.of(2026, 9, 14, 15, 0),
                            19.3, 18.1, 64, 12.6, 213, WetterCode.KLAR, true),
                    List.of(), veraenderlich);

            veraenderlich.add(new TagesVorhersage(
                    LocalDate.of(2026, 9, 15), 9.0, 19.0, 10, WetterCode.KLAR));

            assertEquals(1, bericht.tage().size());
        }
    }
}
