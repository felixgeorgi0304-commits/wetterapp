package de.felixgeorgi.wetterapp.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Speichert die Favoriten zeilenweise in einer Textdatei.
 * <p>
 * Die Datei liegt standardmaessig unter
 * {@code ~/.wetterapp/favoriten.txt} im Benutzerverzeichnis. Das
 * Vorgaengerprojekt legte sie im Arbeitsverzeichnis ab, sodass die Favoriten
 * verschwanden, sobald die Anwendung aus einem anderen Verzeichnis gestartet
 * wurde.
 * <p>
 * Geschrieben und gelesen wird ausdruecklich in UTF-8. Ohne diese Angabe
 * haengt die Zeichenkodierung vom Betriebssystem ab, was bei Umlauten zu
 * unlesbaren Eintraegen fuehrt, sobald die Datei zwischen Systemen wandert.
 *
 * @author Felix Georgi
 */
public class DateiFavoritenRepository implements FavoritenRepository {

    private static final String VERZEICHNIS = ".wetterapp";
    private static final String DATEINAME = "favoriten.txt";

    private final Path datei;

    /**
     * Legt den Speicher im Benutzerverzeichnis an.
     */
    public DateiFavoritenRepository() {
        this(Path.of(System.getProperty("user.home"), VERZEICHNIS, DATEINAME));
    }

    /**
     * Legt den Speicher an einem frei gewaehlten Ort an.
     * <p>
     * Wird in den Tests mit einem temporaeren Verzeichnis benutzt.
     *
     * @param datei Pfad der Favoritendatei
     */
    public DateiFavoritenRepository(Path datei) {
        this.datei = datei;
    }

    @Override
    public List<String> alle() throws FavoritenException {
        if (!Files.exists(datei)) {
            return List.of();
        }
        try {
            List<String> zeilen = Files.readAllLines(datei, StandardCharsets.UTF_8);
            // Leerzeilen herausfiltern, die beim Bearbeiten von Hand entstehen koennen
            return zeilen.stream()
                    .map(String::strip)
                    .filter(z -> !z.isEmpty())
                    .toList();
        } catch (IOException e) {
            throw new FavoritenException("Die Favoriten konnten nicht gelesen werden.", e);
        }
    }

    @Override
    public boolean enthaelt(String ort) throws FavoritenException {
        if (ort == null || ort.isBlank()) {
            return false;
        }
        String gesucht = ort.strip();
        return alle().stream().anyMatch(f -> f.equalsIgnoreCase(gesucht));
    }

    @Override
    public void hinzufuegen(String ort) throws FavoritenException {
        if (ort == null || ort.isBlank() || enthaelt(ort)) {
            return;
        }
        List<String> aktualisiert = new ArrayList<>(alle());
        aktualisiert.add(ort.strip());
        schreiben(aktualisiert);
    }

    @Override
    public void entfernen(String ort) throws FavoritenException {
        if (ort == null || ort.isBlank()) {
            return;
        }
        String gesucht = ort.strip();
        List<String> aktualisiert = new ArrayList<>(alle());
        if (aktualisiert.removeIf(f -> f.equalsIgnoreCase(gesucht))) {
            schreiben(aktualisiert);
        }
    }

    /**
     * Schreibt die vollstaendige Liste und legt dabei fehlende Verzeichnisse an.
     *
     * @param favoriten die zu speichernde Liste
     * @throws FavoritenException wenn die Datei nicht geschrieben werden kann
     */
    private void schreiben(List<String> favoriten) throws FavoritenException {
        try {
            Path verzeichnis = datei.getParent();
            if (verzeichnis != null) {
                Files.createDirectories(verzeichnis);
            }
            Files.write(datei, favoriten, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FavoritenException("Die Favoriten konnten nicht gespeichert werden.", e);
        }
    }

    /**
     * @return der Pfad, unter dem die Favoriten abgelegt werden
     */
    public Path datei() {
        return datei;
    }
}
