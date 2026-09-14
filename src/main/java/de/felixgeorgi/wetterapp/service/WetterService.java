package de.felixgeorgi.wetterapp.service;

import de.felixgeorgi.wetterapp.model.Ort;
import de.felixgeorgi.wetterapp.model.WetterBericht;

/**
 * Zugang zu Wetterdaten.
 * <p>
 * Die Oberflaeche kennt nur diese Schnittstelle, nicht den konkreten Anbieter.
 * Dadurch laesst sich die Datenquelle austauschen, ohne die Oberflaeche zu
 * aendern, und in den Tests kann eine Attrappe eingesetzt werden, die feste
 * Beispieldaten liefert, statt tatsaechlich ins Netz zu gehen.
 *
 * @author Felix Georgi
 * @see OpenMeteoWetterService
 */
public interface WetterService {

    /**
     * Sucht den zu einem Suchbegriff passenden Ort.
     *
     * @param suchbegriff eingegebener Ortsname, zum Beispiel "Mittweida"
     * @return der am besten passende Ort samt Koordinaten
     * @throws OrtNichtGefundenException wenn es zu dem Begriff keinen Ort gibt
     * @throws WetterServiceException    bei Netzwerk- oder Auswertungsfehlern
     */
    Ort sucheOrt(String suchbegriff) throws WetterServiceException;

    /**
     * Laedt den vollstaendigen Wetterbericht fuer einen bekannten Ort.
     *
     * @param ort Ort mit gueltigen Koordinaten
     * @return aktuelle Lage, Stundenverlauf und Tagesvorhersage
     * @throws WetterServiceException bei Netzwerk- oder Auswertungsfehlern
     */
    WetterBericht ladeBericht(Ort ort) throws WetterServiceException;

    /**
     * Bequemlichkeitsmethode: sucht den Ort und laedt direkt dessen Bericht.
     *
     * @param suchbegriff eingegebener Ortsname
     * @return der Wetterbericht zum gefundenen Ort
     * @throws OrtNichtGefundenException wenn es zu dem Begriff keinen Ort gibt
     * @throws WetterServiceException    bei Netzwerk- oder Auswertungsfehlern
     */
    default WetterBericht ladeBericht(String suchbegriff) throws WetterServiceException {
        return ladeBericht(sucheOrt(suchbegriff));
    }
}
