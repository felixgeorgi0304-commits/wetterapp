package de.felixgeorgi.wetterapp.service;

/**
 * Wird ausgeloest, wenn die Ortssuche keinen Treffer liefert.
 * <p>
 * Ein eigener Ausnahmetyp erlaubt es der Oberflaeche, diesen Fall gezielt
 * anders zu behandeln als einen technischen Fehler: Ein Tippfehler im
 * Ortsnamen ist kein Absturz, sondern ein normaler Zustand, auf den ein
 * freundlicher Hinweis gehoert.
 *
 * @author Felix Georgi
 */
public class OrtNichtGefundenException extends WetterServiceException {

    private static final long serialVersionUID = 1L;

    private final String suchbegriff;

    /**
     * @param suchbegriff der Text, zu dem kein Ort gefunden wurde
     */
    public OrtNichtGefundenException(String suchbegriff) {
        super("Kein Ort gefunden für \"" + suchbegriff + "\".");
        this.suchbegriff = suchbegriff;
    }

    /**
     * @return der erfolglose Suchbegriff
     */
    public String suchbegriff() {
        return suchbegriff;
    }
}
