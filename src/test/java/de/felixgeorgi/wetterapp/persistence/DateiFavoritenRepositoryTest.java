package de.felixgeorgi.wetterapp.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests der Favoritenverwaltung.
 * <p>
 * Die Tests schreiben in ein temporaeres Verzeichnis, das JUnit ueber
 * {@link TempDir} bereitstellt und nach jedem Test wieder loescht. Die echten
 * Favoriten des Benutzers bleiben dadurch unberuehrt.
 *
 * @author Felix Georgi
 */
@DisplayName("DateiFavoritenRepository: Speichern der Favoriten")
class DateiFavoritenRepositoryTest {

    @TempDir
    Path verzeichnis;

    private Path datei;
    private FavoritenRepository favoriten;

    @BeforeEach
    void vorbereiten() {
        datei = verzeichnis.resolve("favoriten.txt");
        favoriten = new DateiFavoritenRepository(datei);
    }

    @Test
    @DisplayName("liefert beim ersten Start eine leere Liste statt eines Fehlers")
    void ohneDateiIstDieListeLeer() throws Exception {
        assertTrue(favoriten.alle().isEmpty());
        assertFalse(Files.exists(datei), "es wird keine Datei auf Vorrat angelegt");
    }

    @Test
    @DisplayName("legt die Datei samt Verzeichnis beim ersten Speichern an")
    void legtVerzeichnisAn() throws Exception {
        FavoritenRepository tief = new DateiFavoritenRepository(
                verzeichnis.resolve("a/b/c/favoriten.txt"));

        tief.hinzufuegen("Dresden");

        assertEquals(List.of("Dresden"), tief.alle());
    }

    @Test
    @DisplayName("behaelt die Reihenfolge der Aufnahme bei")
    void behaeltReihenfolge() throws Exception {
        favoriten.hinzufuegen("Mittweida");
        favoriten.hinzufuegen("Dresden");
        favoriten.hinzufuegen("Leipzig");

        assertIterableEquals(List.of("Mittweida", "Dresden", "Leipzig"), favoriten.alle());
    }

    @Test
    @DisplayName("nimmt denselben Ort kein zweites Mal auf")
    void verhindertDoppelteEintraege() throws Exception {
        // Die alte Fassung haengte jeden Klick unbesehen an die Datei an.
        // Nach fuenf Klicks stand der Ort fuenfmal in der Liste.
        favoriten.hinzufuegen("Dresden");
        favoriten.hinzufuegen("Dresden");
        favoriten.hinzufuegen("dresden");

        assertEquals(1, favoriten.alle().size());
    }

    @Test
    @DisplayName("unterscheidet beim Suchen nicht zwischen Gross- und Kleinschreibung")
    void suchtOhneRuecksichtAufSchreibweise() throws Exception {
        favoriten.hinzufuegen("Mittweida");

        assertAll(
                () -> assertTrue(favoriten.enthaelt("Mittweida")),
                () -> assertTrue(favoriten.enthaelt("mittweida")),
                () -> assertTrue(favoriten.enthaelt("MITTWEIDA")),
                () -> assertFalse(favoriten.enthaelt("Mittweid")));
    }

    @Test
    @DisplayName("entfernt einen Ort unabhaengig von der Schreibweise")
    void entferntOrt() throws Exception {
        favoriten.hinzufuegen("Dresden");
        favoriten.hinzufuegen("Leipzig");

        favoriten.entfernen("DRESDEN");

        assertIterableEquals(List.of("Leipzig"), favoriten.alle());
    }

    @Test
    @DisplayName("das Entfernen eines unbekannten Ortes aendert nichts")
    void entfernenUnbekannterOrtIstFolgenlos() throws Exception {
        favoriten.hinzufuegen("Dresden");

        favoriten.entfernen("Hamburg");

        assertIterableEquals(List.of("Dresden"), favoriten.alle());
    }

    @Test
    @DisplayName("umschalten nimmt auf und entfernt wieder")
    void umschaltenWechseltDenZustand() throws Exception {
        assertTrue(favoriten.umschalten("Chemnitz"), "erster Aufruf nimmt auf");
        assertTrue(favoriten.enthaelt("Chemnitz"));

        assertFalse(favoriten.umschalten("Chemnitz"), "zweiter Aufruf entfernt");
        assertFalse(favoriten.enthaelt("Chemnitz"));
    }

    @Test
    @DisplayName("speichert Umlaute verlustfrei in UTF-8")
    void speichertUmlauteRichtig() throws Exception {
        // Ohne ausdrueckliche Angabe der Kodierung haengt das Ergebnis vom
        // Betriebssystem ab. Auf Windows wuerde aus "München" leicht "MÃ¼nchen".
        favoriten.hinzufuegen("München");
        favoriten.hinzufuegen("Zürich");

        assertIterableEquals(List.of("München", "Zürich"), favoriten.alle());

        String roh = Files.readString(datei, StandardCharsets.UTF_8);
        assertTrue(roh.contains("München"), "Datei ist tatsaechlich in UTF-8 geschrieben");
    }

    @Test
    @DisplayName("ignoriert Leerzeilen und Leerraum in der Datei")
    void ignoriertLeerzeilen() throws Exception {
        // Die Datei ist reiner Text und laesst sich von Hand bearbeiten.
        // Dabei entstehen leicht Leerzeilen.
        Files.writeString(datei, "Dresden\n\n   \n  Leipzig  \n", StandardCharsets.UTF_8);

        assertIterableEquals(List.of("Dresden", "Leipzig"), favoriten.alle());
    }

    @Test
    @DisplayName("ignoriert leere Eingaben")
    void ignoriertLeereEingaben() throws Exception {
        favoriten.hinzufuegen("");
        favoriten.hinzufuegen("   ");
        favoriten.hinzufuegen(null);

        assertTrue(favoriten.alle().isEmpty());
        assertFalse(favoriten.enthaelt(null));
    }

    @Test
    @DisplayName("die zurueckgegebene Liste laesst sich nicht veraendern")
    void listeIstUnveraenderlich() throws Exception {
        favoriten.hinzufuegen("Dresden");

        List<String> liste = favoriten.alle();

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> liste.add("Heimlich"));
    }

    @Test
    @DisplayName("ein neu erzeugtes Repository liest dieselben Daten")
    void datenUeberdauernDenNeustart() throws Exception {
        favoriten.hinzufuegen("Mittweida");

        // Entspricht dem Schliessen und erneuten Starten der Anwendung.
        FavoritenRepository nachNeustart = new DateiFavoritenRepository(datei);

        assertIterableEquals(List.of("Mittweida"), nachNeustart.alle());
    }

    @Test
    @DisplayName("meldet ein nicht lesbares Ziel als FavoritenException")
    void unlesbaresZielMeldetFehler() throws IOException {
        // Zeigt auf ein Verzeichnis statt auf eine Datei: Das Lesen muss
        // fehlschlagen, und zwar mit der fachlichen Ausnahme der Anwendung
        // und nicht mit einer rohen IOException.
        Path alsVerzeichnis = verzeichnis.resolve("kein-file");
        Files.createDirectory(alsVerzeichnis);
        FavoritenRepository kaputt = new DateiFavoritenRepository(alsVerzeichnis);

        org.junit.jupiter.api.Assertions.assertThrows(
                FavoritenException.class, kaputt::alle);
    }
}
