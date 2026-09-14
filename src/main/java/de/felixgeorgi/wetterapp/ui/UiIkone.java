package de.felixgeorgi.wetterapp.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Kleine Symbole fuer die Bedienelemente: Lupe, Lesezeichen, Liste und so fort.
 * <p>
 * Wie bei den Wettersymbolen handelt es sich um Vektorgrafik. Alle Pfade sind
 * auf einem Raster von 24 mal 24 Einheiten entworfen und werden beim Erzeugen
 * auf die gewuenschte Groesse skaliert.
 *
 * @author Felix Georgi
 * @see WetterIkone
 */
public final class UiIkone {

    /** Kantenlaenge des Entwurfsrasters. */
    private static final double RASTER = 24.0;

    /** Symbole dieser Aufzaehlung stehen zur Verfuegung. */
    public enum Art {

        /** Lupe fuer die Suche. */
        SUCHE("M10.5 3.5a7 7 0 1 0 0 14 7 7 0 0 0 0-14z M15.6 15.6 L21 21", false),

        /** Umrissenes Lesezeichen: Ort ist kein Favorit. */
        LESEZEICHEN("M6 3.5h12v17l-6-4.6-6 4.6z", false),

        /** Ausgefuelltes Lesezeichen: Ort ist Favorit. */
        LESEZEICHEN_VOLL("M6 3.5h12v17l-6-4.6-6 4.6z", true),

        /** Liste fuer die Favoritenuebersicht. */
        LISTE("M4 6.5h16 M4 12h16 M4 17.5h16", false),

        /** Sonne fuer den Wechsel in das helle Farbschema. */
        SONNE("M12 7.6a4.4 4.4 0 1 0 0 8.8 4.4 4.4 0 0 0 0-8.8z"
                + " M12 1.8v2.6 M12 19.6v2.6 M1.8 12h2.6 M19.6 12h2.6"
                + " M4.8 4.8l1.9 1.9 M17.3 17.3l1.9 1.9 M19.2 4.8l-1.9 1.9 M6.7 17.3l-1.9 1.9", false),

        /** Mond fuer den Wechsel in das dunkle Farbschema. */
        MOND("M20.2 14.6A8.4 8.4 0 0 1 9.4 3.8 8.7 8.7 0 1 0 20.2 14.6z", true),

        /** Windfahne fuer die Windgeschwindigkeit. */
        WIND("M3 8.2h10.4a2.7 2.7 0 1 0-2.7-2.7 M3 12.8h14a2.9 2.9 0 1 1-2.9 2.9 M3 17.4h7.6", false),

        /** Wassertropfen fuer die Luftfeuchte. */
        TROPFEN("M12 3.2s-6.4 7.3-6.4 11.1a6.4 6.4 0 0 0 12.8 0C18.4 10.5 12 3.2 12 3.2z", false),

        /** Thermometer fuer die gefuehlte Temperatur. */
        THERMOMETER("M14 14.6V5.4a2 2 0 1 0-4 0v9.2a4 4 0 1 0 4 0z", false),

        /** Kreuz zum Schliessen. */
        SCHLIESSEN("M6.5 6.5 17.5 17.5 M17.5 6.5 6.5 17.5", false),

        /** Papierkorb zum Entfernen eines Favoriten. */
        PAPIERKORB("M4.5 6.5h15 M9.5 6.5V4.2h5v2.3 M6.8 6.5l1 13h8.4l1-13"
                + " M10.5 10v6 M13.5 10v6", false);

        private final String pfad;
        private final boolean gefuellt;

        Art(String pfad, boolean gefuellt) {
            this.pfad = pfad;
            this.gefuellt = gefuellt;
        }
    }

    /** Reine Hilfsklasse: nicht instanziierbar. */
    private UiIkone() {
    }

    /**
     * Erzeugt ein Bedien-Symbol.
     *
     * @param art     gewuenschtes Symbol
     * @param groesse Kantenlaenge in Pixeln
     * @return ein Knoten mit der passenden Stilklasse {@code ui-ikone}
     */
    public static Node erzeuge(Art art, double groesse) {
        SVGPath pfad = new SVGPath();
        pfad.setContent(art.pfad);
        pfad.setStrokeWidth(1.7);
        pfad.setStrokeLineCap(StrokeLineCap.ROUND);
        pfad.setStrokeLineJoin(StrokeLineJoin.ROUND);
        pfad.getStyleClass().add(art.gefuellt ? "ui-ikone-voll" : "ui-ikone");

        // Die Gruppe kapselt den Pfad, damit die Skalierung um die Mitte
        // erfolgt und der StackPane die Groesse im Layout korrekt beruecksichtigt.
        Group gruppe = new Group(pfad);
        double faktor = groesse / RASTER;
        gruppe.setScaleX(faktor);
        gruppe.setScaleY(faktor);

        StackPane rahmen = new StackPane(gruppe);
        rahmen.setMinSize(groesse, groesse);
        rahmen.setPrefSize(groesse, groesse);
        rahmen.setMaxSize(groesse, groesse);
        rahmen.setMouseTransparent(true);
        return rahmen;
    }
}
