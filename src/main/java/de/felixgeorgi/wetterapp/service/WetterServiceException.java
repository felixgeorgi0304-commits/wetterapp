package de.felixgeorgi.wetterapp.service;

/**
 * Fehler beim Abrufen oder Auswerten von Wetterdaten.
 * <p>
 * Die Anwendung wirft diese Ausnahme an genau einer Stelle nach aussen: an der
 * Grenze des Service-Pakets. Netzwerkfehler, Zeitueberschreitungen und
 * unlesbare Antworten werden dahinter zusammengefasst, sodass die Oberflaeche
 * nur einen Fehlertyp behandeln muss und dem Benutzer eine verstaendliche
 * Meldung anzeigen kann.
 * <p>
 * Bewusst eine gepruefte Ausnahme ({@code extends Exception}): Der Compiler
 * erzwingt damit, dass jeder Aufrufer sich zum Fehlerfall verhaelt. Im
 * Vorgaengerprojekt wurde stattdessen {@code printStackTrace()} aufgerufen
 * und {@code null} zurueckgegeben, was den Fehler unsichtbar machte.
 *
 * @author Felix Georgi
 */
public class WetterServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @param nachricht fuer den Benutzer verstaendliche Beschreibung des Fehlers
     */
    public WetterServiceException(String nachricht) {
        super(nachricht);
    }

    /**
     * @param nachricht fuer den Benutzer verstaendliche Beschreibung des Fehlers
     * @param ursache   die zugrunde liegende technische Ausnahme
     */
    public WetterServiceException(String nachricht, Throwable ursache) {
        super(nachricht, ursache);
    }
}
