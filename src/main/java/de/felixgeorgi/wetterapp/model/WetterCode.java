package de.felixgeorgi.wetterapp.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wettercodes nach dem WMO-Standard 4677, wie sie die Open-Meteo-API liefert.
 * <p>
 * Jeder Code traegt seine deutsche Beschreibung und das zugehoerige
 * {@link WetterSymbol} direkt bei sich. Dadurch entfaellt die frueher noetige
 * Kette aus {@code if}-Abfragen ueber magische Zahlenbereiche: Eine neue
 * Wetterlage wird ergaenzt, indem hier eine Konstante hinzukommt.
 *
 * @author Felix Georgi
 * @see <a href="https://open-meteo.com/en/docs">Open-Meteo API-Dokumentation</a>
 */
public enum WetterCode {

    KLAR(0, "klar", WetterSymbol.KLAR),
    UEBERWIEGEND_KLAR(1, "überwiegend klar", WetterSymbol.LEICHT_BEWOELKT),
    TEILWEISE_BEWOELKT(2, "teilweise bewölkt", WetterSymbol.BEWOELKT),
    BEDECKT(3, "bedeckt", WetterSymbol.BEDECKT),

    NEBEL(45, "Nebel", WetterSymbol.NEBEL),
    REIFNEBEL(48, "gefrierender Nebel", WetterSymbol.NEBEL),

    NIESEL_LEICHT(51, "leichter Nieselregen", WetterSymbol.NIESEL),
    NIESEL_MAESSIG(53, "mäßiger Nieselregen", WetterSymbol.NIESEL),
    NIESEL_STARK(55, "starker Nieselregen", WetterSymbol.NIESEL),
    NIESEL_GEFRIEREND_LEICHT(56, "leichter gefrierender Nieselregen", WetterSymbol.NIESEL),
    NIESEL_GEFRIEREND_STARK(57, "starker gefrierender Nieselregen", WetterSymbol.NIESEL),

    REGEN_LEICHT(61, "leichter Regen", WetterSymbol.REGEN),
    REGEN_MAESSIG(63, "mäßiger Regen", WetterSymbol.REGEN),
    REGEN_STARK(65, "starker Regen", WetterSymbol.REGEN),
    REGEN_GEFRIEREND_LEICHT(66, "leichter gefrierender Regen", WetterSymbol.REGEN),
    REGEN_GEFRIEREND_STARK(67, "starker gefrierender Regen", WetterSymbol.REGEN),

    SCHNEE_LEICHT(71, "leichter Schneefall", WetterSymbol.SCHNEE),
    SCHNEE_MAESSIG(73, "mäßiger Schneefall", WetterSymbol.SCHNEE),
    SCHNEE_STARK(75, "starker Schneefall", WetterSymbol.SCHNEE),
    SCHNEEGRIESEL(77, "Schneegriesel", WetterSymbol.SCHNEE),

    REGENSCHAUER_LEICHT(80, "leichte Regenschauer", WetterSymbol.REGEN),
    REGENSCHAUER_MAESSIG(81, "mäßige Regenschauer", WetterSymbol.REGEN),
    REGENSCHAUER_STARK(82, "heftige Regenschauer", WetterSymbol.REGEN),

    SCHNEESCHAUER_LEICHT(85, "leichte Schneeschauer", WetterSymbol.SCHNEE),
    SCHNEESCHAUER_STARK(86, "starke Schneeschauer", WetterSymbol.SCHNEE),

    GEWITTER(95, "Gewitter", WetterSymbol.GEWITTER),
    GEWITTER_HAGEL_LEICHT(96, "Gewitter mit leichtem Hagel", WetterSymbol.GEWITTER),
    GEWITTER_HAGEL_STARK(99, "Gewitter mit starkem Hagel", WetterSymbol.GEWITTER),

    /** Rueckfallwert fuer Codes, die die API zwar sendet, die hier aber unbekannt sind. */
    UNBEKANNT(-1, "unbekannt", WetterSymbol.BEWOELKT);

    /**
     * Nachschlagetabelle Code -&gt; Konstante.
     * <p>
     * Wird einmalig beim Laden der Klasse aufgebaut, damit
     * {@link #vonCode(int)} in konstanter Zeit arbeitet statt die
     * Konstanten jedes Mal linear zu durchsuchen.
     */
    private static final Map<Integer, WetterCode> NACH_CODE =
            Arrays.stream(values())
                  .filter(w -> w != UNBEKANNT)
                  .collect(Collectors.toMap(WetterCode::code, Function.identity()));

    private final int code;
    private final String beschreibung;
    private final WetterSymbol symbol;

    WetterCode(int code, String beschreibung, WetterSymbol symbol) {
        this.code = code;
        this.beschreibung = beschreibung;
        this.symbol = symbol;
    }

    /**
     * @return der numerische WMO-Code, wie ihn die API liefert
     */
    public int code() {
        return code;
    }

    /**
     * @return die deutsche Beschreibung der Wetterlage, zum Beispiel "leichter Regen"
     */
    public String beschreibung() {
        return beschreibung;
    }

    /**
     * @return die Bildkategorie, mit der diese Wetterlage dargestellt wird
     */
    public WetterSymbol symbol() {
        return symbol;
    }

    /**
     * Ordnet einen numerischen WMO-Code der passenden Konstanten zu.
     *
     * @param code numerischer Wettercode der API
     * @return die passende Konstante oder {@link #UNBEKANNT}, falls der Code
     *         nicht bekannt ist. Die Methode gibt bewusst nie {@code null}
     *         zurueck, damit aufrufender Code keine Null-Pruefung braucht.
     */
    public static WetterCode vonCode(int code) {
        return NACH_CODE.getOrDefault(code, UNBEKANNT);
    }
}
