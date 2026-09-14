package de.felixgeorgi.wetterapp.model;

/**
 * Grobe Kategorie einer Wetterlage.
 * <p>
 * Die API liefert sehr viele einzelne Wettercodes (siehe {@link WetterCode}),
 * fuer die Darstellung reichen jedoch wenige Bildgruppen aus. Diese Aufzaehlung
 * entkoppelt die Anzeige von den konkreten Codes: die Oberflaeche muss nur
 * neun Symbole kennen, nicht dreissig Codes.
 *
 * @author Felix Georgi
 */
public enum WetterSymbol {

    /** Wolkenloser Himmel. */
    KLAR,

    /** Einzelne Wolken, Sonne bzw. Mond noch sichtbar. */
    LEICHT_BEWOELKT,

    /** Ueberwiegend bewoelkt. */
    BEWOELKT,

    /** Geschlossene Wolkendecke. */
    BEDECKT,

    /** Nebel oder Reifnebel. */
    NEBEL,

    /** Nieselregen. */
    NIESEL,

    /** Regen und Regenschauer. */
    REGEN,

    /** Schneefall und Schneeschauer. */
    SCHNEE,

    /** Gewitter, gegebenenfalls mit Hagel. */
    GEWITTER
}
