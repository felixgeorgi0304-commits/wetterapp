package de.felixgeorgi.wetterapp.service;

import de.felixgeorgi.wetterapp.model.AktuellesWetter;
import de.felixgeorgi.wetterapp.model.Ort;
import de.felixgeorgi.wetterapp.model.StundenVorhersage;
import de.felixgeorgi.wetterapp.model.TagesVorhersage;
import de.felixgeorgi.wetterapp.model.WetterBericht;
import de.felixgeorgi.wetterapp.model.WetterCode;
import de.felixgeorgi.wetterapp.service.dto.GeoAntwort;
import de.felixgeorgi.wetterapp.service.dto.WetterAntwort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Uebersetzt die JSON-nahen Datenstrukturen der Open-Meteo-API in die
 * Modellklassen der Anwendung.
 * <p>
 * Diese Klasse enthaelt bewusst keinerlei Netzwerkzugriff. Genau dadurch
 * laesst sich die gesamte Auswertungslogik mit festen Beispielantworten
 * testen, ohne dass dafuer eine Internetverbindung noetig waere.
 *
 * @author Felix Georgi
 */
final class OpenMeteoMapper {

    /** Anzahl der Stunden, die der Verlauf anzeigt. */
    static final int STUNDEN_IM_VERLAUF = 24;

    /** Reine Hilfsklasse: nicht instanziierbar. */
    private OpenMeteoMapper() {
    }

    /**
     * Waehlt aus einer Geocoding-Antwort den besten Treffer aus.
     *
     * @param antwort     Antwort des Geocoding-Dienstes, darf {@code null} sein
     * @param suchbegriff urspruenglicher Suchbegriff, nur fuer die Fehlermeldung
     * @return der erste und damit bestbewertete Treffer als {@link Ort}
     * @throws OrtNichtGefundenException wenn die Antwort keinen Treffer enthaelt
     */
    static Ort zuOrt(GeoAntwort antwort, String suchbegriff) throws OrtNichtGefundenException {
        if (antwort == null
                || antwort.ergebnisse() == null
                || antwort.ergebnisse().isEmpty()) {
            throw new OrtNichtGefundenException(suchbegriff);
        }

        GeoAntwort.GeoTreffer treffer = antwort.ergebnisse().get(0);
        return new Ort(
                treffer.name(),
                treffer.region(),
                treffer.land(),
                treffer.breite(),
                treffer.laenge(),
                treffer.zeitzone());
    }

    /**
     * Baut aus der Vorhersageantwort einen vollstaendigen Wetterbericht.
     *
     * @param ort     der Ort, zu dem die Antwort gehoert
     * @param antwort Antwort des Vorhersagedienstes
     * @return Bericht mit aktueller Lage, Stundenverlauf und Tagesvorhersage
     * @throws WetterServiceException wenn die Antwort unvollstaendig oder unlesbar ist
     */
    static WetterBericht zuBericht(Ort ort, WetterAntwort antwort) throws WetterServiceException {
        if (antwort == null || antwort.aktuell() == null) {
            throw new WetterServiceException("Die Wetterdaten sind unvollständig angekommen.");
        }

        AktuellesWetter aktuell = zuAktuellemWetter(antwort.aktuell());
        List<StundenVorhersage> stunden = zuStunden(antwort.stunden(), aktuell.zeitpunkt());
        List<TagesVorhersage> tage = zuTagen(antwort.tage());

        return new WetterBericht(ort, aktuell, stunden, tage);
    }

    /**
     * Wandelt den Block mit den aktuellen Messwerten um.
     *
     * @param aktuell Messwerte der API
     * @return die aktuelle Wetterlage
     * @throws WetterServiceException wenn Pflichtwerte fehlen oder unlesbar sind
     */
    private static AktuellesWetter zuAktuellemWetter(WetterAntwort.Aktuell aktuell)
            throws WetterServiceException {

        LocalDateTime zeitpunkt = zeitpunktLesen(aktuell.zeit());
        if (zeitpunkt == null) {
            throw new WetterServiceException("Die API hat keinen gültigen Zeitstempel geliefert.");
        }

        return new AktuellesWetter(
                zeitpunkt,
                wertOder(aktuell.temperatur(), 0.0),
                wertOder(aktuell.gefuehlt(), wertOder(aktuell.temperatur(), 0.0)),
                wertOder(aktuell.luftfeuchte(), 0),
                wertOder(aktuell.windGeschwindigkeit(), 0.0),
                wertOder(aktuell.windRichtung(), 0),
                WetterCode.vonCode(wertOder(aktuell.wetterCode(), -1)),
                wertOder(aktuell.istTag(), 1) == 1);
    }

    /**
     * Fuehrt die parallelen Stundenlisten der API zu Objekten je Stunde zusammen
     * und schneidet daraus das Fenster ab der aktuellen Stunde heraus.
     * <p>
     * Open-Meteo liefert den Verlauf immer ab Mitternacht des aktuellen Tages.
     * Angezeigt werden soll aber, was noch kommt, nicht was schon vorbei ist.
     *
     * @param stunden       Zeitreihen der API, darf {@code null} sein
     * @param jetztAmOrt    aktueller Zeitpunkt in der Zeitzone des Ortes
     * @return bis zu {@value #STUNDEN_IM_VERLAUF} Eintraege ab der laufenden Stunde
     */
    private static List<StundenVorhersage> zuStunden(WetterAntwort.Stunden stunden,
                                                     LocalDateTime jetztAmOrt) {
        if (stunden == null || stunden.zeit() == null) {
            return List.of();
        }

        List<StundenVorhersage> alle = new ArrayList<>();
        for (int i = 0; i < stunden.zeit().size(); i++) {
            LocalDateTime zeitpunkt = zeitpunktLesen(stunden.zeit().get(i));
            if (zeitpunkt == null) {
                continue;
            }
            alle.add(new StundenVorhersage(
                    zeitpunkt,
                    wertAus(stunden.temperatur(), i, 0.0),
                    wertAus(stunden.niederschlagsRisiko(), i, 0),
                    WetterCode.vonCode(wertAus(stunden.wetterCode(), i, -1)),
                    wertAus(stunden.istTag(), i, 1) == 1));
        }

        LocalDateTime ab = jetztAmOrt.withMinute(0).withSecond(0).withNano(0);
        List<StundenVorhersage> kuenftige = alle.stream()
                .filter(s -> !s.zeitpunkt().isBefore(ab))
                .limit(STUNDEN_IM_VERLAUF)
                .toList();

        // Notnagel: Liegt der Zeitstempel der API ausserhalb des gelieferten
        // Fensters, wird lieber der Anfang der Reihe gezeigt als gar nichts.
        if (kuenftige.isEmpty()) {
            return alle.stream().limit(STUNDEN_IM_VERLAUF).toList();
        }
        return kuenftige;
    }

    /**
     * Fuehrt die parallelen Tageslisten der API zu Objekten je Tag zusammen.
     *
     * @param tage Zeitreihen der API, darf {@code null} sein
     * @return chronologische Tagesvorhersage, moeglicherweise leer
     */
    private static List<TagesVorhersage> zuTagen(WetterAntwort.Tage tage) {
        if (tage == null || tage.datum() == null) {
            return List.of();
        }

        List<TagesVorhersage> ergebnis = new ArrayList<>();
        for (int i = 0; i < tage.datum().size(); i++) {
            LocalDate datum;
            try {
                datum = LocalDate.parse(tage.datum().get(i));
            } catch (DateTimeParseException | NullPointerException e) {
                continue;
            }

            double min = wertAus(tage.minTemperatur(), i, 0.0);
            double max = wertAus(tage.maxTemperatur(), i, 0.0);

            // Sollte die API die Werte einmal vertauscht liefern, wird hier
            // getauscht statt eine Ausnahme aus dem Record auszuloesen.
            if (min > max) {
                double tausch = min;
                min = max;
                max = tausch;
            }

            ergebnis.add(new TagesVorhersage(
                    datum,
                    min,
                    max,
                    wertAus(tage.niederschlagsRisiko(), i, 0),
                    WetterCode.vonCode(wertAus(tage.wetterCode(), i, -1))));
        }
        return List.copyOf(ergebnis);
    }

    /**
     * Liest einen Zeitstempel im Format {@code yyyy-MM-dd'T'HH:mm}.
     *
     * @param text Zeitstempel der API, darf {@code null} sein
     * @return der gelesene Zeitpunkt oder {@code null}, wenn der Text unbrauchbar ist
     */
    private static LocalDateTime zeitpunktLesen(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Greift auf ein Element einer moeglicherweise fehlenden oder zu kurzen
     * Liste zu. Einzelne Zeitreihen koennen bei Open-Meteo {@code null}-Werte
     * enthalten, zum Beispiel die Niederschlagswahrscheinlichkeit am Ende des
     * Vorhersagezeitraums.
     *
     * @param liste       Zeitreihe, darf {@code null} sein
     * @param index       gewuenschte Position
     * @param ersatzwert  Wert, der bei fehlendem Eintrag verwendet wird
     * @param <T>         Typ der Werte in der Zeitreihe
     * @return der Wert an der Position oder der Ersatzwert
     */
    private static <T> T wertAus(List<T> liste, int index, T ersatzwert) {
        if (liste == null || index >= liste.size()) {
            return ersatzwert;
        }
        return wertOder(liste.get(index), ersatzwert);
    }

    /**
     * Ersetzt {@code null} durch einen Ersatzwert.
     *
     * @param wert       zu pruefender Wert
     * @param ersatzwert Ersatz bei {@code null}
     * @param <T>        Typ des Wertes
     * @return {@code wert}, oder {@code ersatzwert} falls {@code wert} null ist
     */
    private static <T> T wertOder(T wert, T ersatzwert) {
        return wert != null ? wert : ersatzwert;
    }
}
