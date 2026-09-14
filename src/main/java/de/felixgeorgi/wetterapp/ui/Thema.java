package de.felixgeorgi.wetterapp.ui;

/**
 * Die beiden Farbschemata der Anwendung.
 * <p>
 * Der Name jeder Konstante entspricht einer Stilklasse im Stylesheet. Beim
 * Umschalten tauscht die Oberflaeche lediglich diese Klasse am Wurzelknoten
 * aus; alle Farben sind in der CSS-Datei ueber nachschlagbare Farbwerte
 * definiert und aendern sich dadurch in einem Zug.
 *
 * @author Felix Georgi
 */
public enum Thema {

    /** Helles Farbschema. */
    HELL("hell", UiIkone.Art.MOND, "Zum dunklen Design wechseln"),

    /** Dunkles Farbschema. */
    DUNKEL("dunkel", UiIkone.Art.SONNE, "Zum hellen Design wechseln");

    private final String stilKlasse;
    private final UiIkone.Art schaltflaechenIkone;
    private final String hinweis;

    Thema(String stilKlasse, UiIkone.Art schaltflaechenIkone, String hinweis) {
        this.stilKlasse = stilKlasse;
        this.schaltflaechenIkone = schaltflaechenIkone;
        this.hinweis = hinweis;
    }

    /**
     * @return Name der Stilklasse am Wurzelknoten
     */
    public String stilKlasse() {
        return stilKlasse;
    }

    /**
     * @return das Symbol, das die Umschaltflaeche in diesem Zustand zeigt
     */
    public UiIkone.Art schaltflaechenIkone() {
        return schaltflaechenIkone;
    }

    /**
     * @return Text der Kurzinfo an der Umschaltflaeche
     */
    public String hinweis() {
        return hinweis;
    }

    /**
     * @return das jeweils andere Farbschema
     */
    public Thema wechseln() {
        return this == HELL ? DUNKEL : HELL;
    }
}
