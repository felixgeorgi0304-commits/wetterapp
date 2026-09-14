package de.felixgeorgi.wetterapp.persistence;

import java.util.List;

/**
 * Speicher fuer die vom Benutzer gemerkten Orte.
 * <p>
 * Die Oberflaeche arbeitet nur gegen diese Schnittstelle. Ob die Favoriten in
 * einer Textdatei, in einer Datenbank oder nur im Arbeitsspeicher liegen, ist
 * fuer sie ohne Bedeutung. In den Tests wird dadurch ein temporaeres
 * Verzeichnis verwendet, statt die echten Benutzerdaten anzufassen.
 *
 * @author Felix Georgi
 */
public interface FavoritenRepository {

    /**
     * Liefert alle gespeicherten Orte in der Reihenfolge ihrer Aufnahme.
     *
     * @return unveraenderliche Liste der Ortsnamen, moeglicherweise leer
     * @throws FavoritenException wenn der Speicher nicht gelesen werden kann
     */
    List<String> alle() throws FavoritenException;

    /**
     * Prueft, ob ein Ort gespeichert ist. Gross- und Kleinschreibung wird
     * dabei nicht unterschieden.
     *
     * @param ort zu pruefender Ortsname
     * @return {@code true}, wenn der Ort bereits gespeichert ist
     * @throws FavoritenException wenn der Speicher nicht gelesen werden kann
     */
    boolean enthaelt(String ort) throws FavoritenException;

    /**
     * Nimmt einen Ort auf. Ein bereits vorhandener Ort wird nicht erneut
     * hinzugefuegt.
     *
     * @param ort aufzunehmender Ortsname
     * @throws FavoritenException wenn der Speicher nicht geschrieben werden kann
     */
    void hinzufuegen(String ort) throws FavoritenException;

    /**
     * Entfernt einen Ort. Ist er nicht gespeichert, passiert nichts.
     *
     * @param ort zu entfernender Ortsname
     * @throws FavoritenException wenn der Speicher nicht geschrieben werden kann
     */
    void entfernen(String ort) throws FavoritenException;

    /**
     * Nimmt einen Ort auf, falls er fehlt, und entfernt ihn andernfalls.
     *
     * @param ort umzuschaltender Ortsname
     * @return {@code true}, wenn der Ort danach gespeichert ist
     * @throws FavoritenException wenn der Speicher nicht geschrieben werden kann
     */
    default boolean umschalten(String ort) throws FavoritenException {
        if (enthaelt(ort)) {
            entfernen(ort);
            return false;
        }
        hinzufuegen(ort);
        return true;
    }
}
