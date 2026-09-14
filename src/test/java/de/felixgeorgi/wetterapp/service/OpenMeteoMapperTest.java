package de.felixgeorgi.wetterapp.service;

import de.felixgeorgi.wetterapp.Testdaten;
import de.felixgeorgi.wetterapp.model.Ort;
import de.felixgeorgi.wetterapp.model.StundenVorhersage;
import de.felixgeorgi.wetterapp.model.TagesVorhersage;
import de.felixgeorgi.wetterapp.model.WetterBericht;
import de.felixgeorgi.wetterapp.model.WetterCode;
import de.felixgeorgi.wetterapp.service.dto.GeoAntwort;
import de.felixgeorgi.wetterapp.service.dto.WetterAntwort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests der Auswertung von API-Antworten.
 * <p>
 * Geprueft wird ausschliesslich gegen die Beispieldateien unter
 * {@code src/test/resources/fixtures}. Der Netzwerkzugriff steckt in einer
 * eigenen Klasse und ist hier bewusst nicht beteiligt: Getestet wird die
 * Logik, nicht die Internetverbindung.
 *
 * @author Felix Georgi
 */
@DisplayName("OpenMeteoMapper: Auswertung der API-Antworten")
class OpenMeteoMapperTest {

    private static final Ort MITTWEIDA =
            new Ort("Mittweida", "Sachsen", "Deutschland", 50.98617, 12.98187, "Europe/Berlin");

    @Nested
    @DisplayName("Ortssuche")
    class Ortssuche {

        @Test
        @DisplayName("liest den ersten Treffer vollstaendig aus")
        void liestErstenTreffer() throws Exception {
            GeoAntwort antwort = Testdaten.lade("geocoding-mittweida.json", GeoAntwort.class);

            Ort ort = OpenMeteoMapper.zuOrt(antwort, "Mittweida");

            assertAll(
                    () -> assertEquals("Mittweida", ort.name()),
                    () -> assertEquals("Sachsen", ort.region()),
                    () -> assertEquals("Deutschland", ort.land()),
                    () -> assertEquals(50.98617, ort.breite(), 0.00001),
                    () -> assertEquals(12.98187, ort.laenge(), 0.00001),
                    () -> assertEquals("Europe/Berlin", ort.zeitzone()));
        }

        @Test
        @DisplayName("setzt den Anzeigenamen aus Ort, Region und Land zusammen")
        void bildetAnzeigenamen() throws Exception {
            GeoAntwort antwort = Testdaten.lade("geocoding-mittweida.json", GeoAntwort.class);

            Ort ort = OpenMeteoMapper.zuOrt(antwort, "Mittweida");

            assertEquals("Mittweida, Sachsen, Deutschland", ort.anzeigeName());
        }

        @Test
        @DisplayName("meldet eine leere Trefferliste als OrtNichtGefundenException")
        void leereListeFuehrtZuAusnahme() {
            // Genau dieser Fall liess das Vorgaengerprojekt mit einer
            // NullPointerException abstuerzen, sobald man sich vertippt hatte.
            GeoAntwort antwort = Testdaten.lade("geocoding-leer.json", GeoAntwort.class);

            OrtNichtGefundenException fehler = assertThrows(
                    OrtNichtGefundenException.class,
                    () -> OpenMeteoMapper.zuOrt(antwort, "Gibtesnicht"));

            assertEquals("Gibtesnicht", fehler.suchbegriff());
        }

        @Test
        @DisplayName("meldet auch eine fehlende Antwort sauber")
        void nullAntwortFuehrtZuAusnahme() {
            assertThrows(OrtNichtGefundenException.class,
                    () -> OpenMeteoMapper.zuOrt(null, "Irgendwo"));
        }
    }

    @Nested
    @DisplayName("Wetterbericht")
    class Wetterbericht {

        private WetterBericht berichtLaden() throws Exception {
            WetterAntwort antwort =
                    Testdaten.lade("forecast-mittweida.json", WetterAntwort.class);
            return OpenMeteoMapper.zuBericht(MITTWEIDA, antwort);
        }

        @Test
        @DisplayName("uebernimmt die aktuellen Messwerte unveraendert")
        void liestAktuelleWerte() throws Exception {
            WetterBericht bericht = berichtLaden();
            var aktuell = bericht.aktuell();

            assertAll(
                    () -> assertEquals(LocalDateTime.of(2026, 9, 14, 15, 0), aktuell.zeitpunkt()),
                    () -> assertEquals(19.3, aktuell.temperatur(), 0.001),
                    () -> assertEquals(18.1, aktuell.gefuehlt(), 0.001),
                    () -> assertEquals(64, aktuell.luftfeuchte()),
                    () -> assertEquals(12.6, aktuell.windGeschwindigkeit(), 0.001),
                    () -> assertEquals(213, aktuell.windRichtung()),
                    () -> assertSame(WetterCode.REGEN_LEICHT, aktuell.wetterCode()),
                    () -> assertTrue(aktuell.istTag()));
        }

        @Test
        @DisplayName("zeigt 24 Stunden ab der laufenden Stunde, nicht ab Mitternacht")
        void schneidetStundenAbJetztZu() throws Exception {
            // Die API liefert den Verlauf ab 00:00 Uhr des laufenden Tages.
            // Angezeigt werden soll, was noch kommt: der Verlauf muss also bei
            // der Uhrzeit aus dem Block "current" beginnen (hier 15:00 Uhr).
            WetterBericht bericht = berichtLaden();
            List<StundenVorhersage> stunden = bericht.stunden();

            assertAll(
                    () -> assertEquals(24, stunden.size()),
                    () -> assertEquals(LocalDateTime.of(2026, 9, 14, 15, 0),
                            stunden.get(0).zeitpunkt()),
                    () -> assertEquals(LocalDateTime.of(2026, 9, 15, 14, 0),
                            stunden.get(23).zeitpunkt()));
        }

        @Test
        @DisplayName("haelt den Stundenverlauf chronologisch")
        void stundenSindAufsteigendSortiert() throws Exception {
            List<StundenVorhersage> stunden = berichtLaden().stunden();

            for (int i = 1; i < stunden.size(); i++) {
                assertTrue(stunden.get(i - 1).zeitpunkt().isBefore(stunden.get(i).zeitpunkt()),
                        "Eintrag " + i + " liegt nicht nach seinem Vorgaenger");
            }
        }

        @Test
        @DisplayName("liest die Tagesvorhersage mit Tief- und Hoechstwerten")
        void liestTagesvorhersage() throws Exception {
            List<TagesVorhersage> tage = berichtLaden().tage();

            assertEquals(7, tage.size());

            TagesVorhersage ersterTag = tage.get(0);
            assertAll(
                    () -> assertEquals(LocalDate.of(2026, 9, 14), ersterTag.datum()),
                    () -> assertEquals(8.1, ersterTag.minTemperatur(), 0.001),
                    () -> assertEquals(17.6, ersterTag.maxTemperatur(), 0.001),
                    () -> assertEquals(70, ersterTag.niederschlagsRisiko()),
                    () -> assertSame(WetterCode.REGEN_LEICHT, ersterTag.wetterCode()));
        }

        @Test
        @DisplayName("verarbeitet Frosttage mit negativen Temperaturen korrekt")
        void behandeltNegativeTemperaturen() throws Exception {
            // Im Vorgaengerprojekt begann die Suche nach dem Hoechstwert bei
            // Double.MIN_VALUE. Dieser Wert ist die kleinste *positive* Zahl,
            // nicht die kleinstmoegliche: An einem Tag mit durchgehend
            // negativen Temperaturen wurde deshalb faelschlich rund 0 Grad
            // als Tageshoechstwert angezeigt.
            TagesVorhersage frosttag = berichtLaden().tage().get(6);

            assertAll(
                    () -> assertEquals(-2.4, frosttag.minTemperatur(), 0.001),
                    () -> assertEquals(1.9, frosttag.maxTemperatur(), 0.001),
                    () -> assertTrue(frosttag.minTemperatur() < frosttag.maxTemperatur()));
        }

        @Test
        @DisplayName("gibt den Wochentag auf Deutsch aus")
        void liefertDeutschenWochentag() throws Exception {
            // Der 14. September 2026 ist ein Montag.
            assertEquals("Mo", berichtLaden().tage().get(0).wochentagKurz());
        }

        @Test
        @DisplayName("reicht den Ort unveraendert durch")
        void behaeltDenOrt() throws Exception {
            assertEquals(MITTWEIDA, berichtLaden().ort());
        }
    }

    @Nested
    @DisplayName("Unvollstaendige Antworten")
    class Luecken {

        @Test
        @DisplayName("fuellt fehlende Einzelwerte mit sinnvollen Ersatzwerten")
        void toleriertFehlendeWerte() throws Exception {
            // Open-Meteo laesst einzelne Werte weg, etwa die
            // Niederschlagswahrscheinlichkeit am Rand des Vorhersagezeitraums.
            // Die Anwendung muss das aushalten, statt abzubrechen.
            WetterAntwort antwort =
                    Testdaten.lade("forecast-luecken.json", WetterAntwort.class);

            WetterBericht bericht = OpenMeteoMapper.zuBericht(MITTWEIDA, antwort);

            assertAll(
                    () -> assertNotNull(bericht.aktuell()),
                    () -> assertEquals(0, bericht.aktuell().luftfeuchte()),
                    () -> assertEquals(11.0, bericht.aktuell().gefuehlt(), 0.001,
                            "ohne eigenen Wert wird die Lufttemperatur verwendet"),
                    () -> assertEquals(2, bericht.stunden().size()),
                    () -> assertEquals(0, bericht.stunden().get(0).niederschlagsRisiko()),
                    () -> assertEquals(1, bericht.tage().size()),
                    () -> assertEquals(0, bericht.tage().get(0).niederschlagsRisiko()));
        }

        @Test
        @DisplayName("meldet eine Antwort ohne aktuellen Block als Fehler")
        void leereAntwortFuehrtZuAusnahme() {
            assertThrows(WetterServiceException.class,
                    () -> OpenMeteoMapper.zuBericht(MITTWEIDA, null));
        }

        @Test
        @DisplayName("liefert leere Listen statt null, wenn Zeitreihen fehlen")
        void fehlendeZeitreihenErgebenLeereListen() throws Exception {
            WetterAntwort ohneReihen = new WetterAntwort(
                    "Europe/Berlin",
                    new WetterAntwort.Aktuell("2026-09-14T15:00", 10.0, null, null, 1, 0, null, null),
                    null, null);

            WetterBericht bericht = OpenMeteoMapper.zuBericht(MITTWEIDA, ohneReihen);

            assertAll(
                    () -> assertNotNull(bericht.stunden()),
                    () -> assertNotNull(bericht.tage()),
                    () -> assertTrue(bericht.stunden().isEmpty()),
                    () -> assertTrue(bericht.tage().isEmpty()));
        }

        @Test
        @DisplayName("der Bericht laesst sich nachtraeglich nicht veraendern")
        void listenSindUnveraenderlich() throws Exception {
            WetterAntwort antwort =
                    Testdaten.lade("forecast-mittweida.json", WetterAntwort.class);
            WetterBericht bericht = OpenMeteoMapper.zuBericht(MITTWEIDA, antwort);

            assertThrows(UnsupportedOperationException.class,
                    () -> bericht.tage().clear());
            assertFalse(bericht.tage().isEmpty());
        }
    }
}
