package de.felixgeorgi.wetterapp.persistence;

/**
 * Fehler beim Lesen oder Schreiben der gespeicherten Favoriten.
 *
 * @author Felix Georgi
 */
public class FavoritenException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @param nachricht verstaendliche Beschreibung des Fehlers
     * @param ursache   die zugrunde liegende technische Ausnahme
     */
    public FavoritenException(String nachricht, Throwable ursache) {
        super(nachricht, ursache);
    }
}
