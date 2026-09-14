package de.felixgeorgi.wetterapp;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Laedt die Beispielantworten aus {@code src/test/resources/fixtures}.
 * <p>
 * Die Tests arbeiten ausschliesslich mit diesen fest hinterlegten Dateien und
 * niemals mit der echten Schnittstelle im Internet. Das hat zwei Gruende:
 * <ul>
 *   <li>Die Tests laufen dadurch immer gleich ab. Eine Abfrage im Netz wuerde
 *       jeden Tag andere Werte liefern; damit liesse sich nichts pruefen.</li>
 *   <li>Sie laufen ohne Internetverbindung, also auch auf dem Bauserver.</li>
 * </ul>
 *
 * @author Felix Georgi
 */
public final class Testdaten {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Testdaten() {
    }

    /**
     * Liest eine Beispieldatei und wandelt sie in das gewuenschte Objekt um.
     *
     * @param dateiName Name der Datei im Ordner {@code fixtures}
     * @param typ       Zielklasse
     * @param <T>       Typ des Ergebnisses
     * @return das gelesene Objekt
     */
    public static <T> T lade(String dateiName, Class<T> typ) {
        String pfad = "/fixtures/" + dateiName;
        try (InputStream strom = Testdaten.class.getResourceAsStream(pfad)) {
            Objects.requireNonNull(strom, "Testdatei nicht gefunden: " + pfad);
            return MAPPER.readValue(strom, typ);
        } catch (IOException e) {
            throw new UncheckedIOException("Testdatei nicht lesbar: " + pfad, e);
        }
    }
}
