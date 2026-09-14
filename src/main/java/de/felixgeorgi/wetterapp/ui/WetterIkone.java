package de.felixgeorgi.wetterapp.ui;

import de.felixgeorgi.wetterapp.model.WetterSymbol;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;

import java.util.ArrayList;
import java.util.List;

/**
 * Erzeugt die Wettersymbole als Vektorgrafik.
 * <p>
 * Die Symbole werden aus einfachen Formen (Kreis, Rechteck, Linie)
 * zusammengesetzt, statt als PNG-Dateien mitgeliefert zu werden. Das hat drei
 * Vorteile:
 * <ul>
 *   <li>Sie bleiben in jeder Groesse scharf, auch auf hochaufloesenden Bildschirmen.</li>
 *   <li>Ihre Farben kommen aus dem Stylesheet und passen sich damit automatisch
 *       an das helle und das dunkle Farbschema an.</li>
 *   <li>Fuer Tag und Nacht laesst sich dieselbe Zeichnung mit Sonne oder Mond
 *       kombinieren, ohne den Bildbestand zu verdoppeln.</li>
 * </ul>
 * Alle Formen werden auf einem gedachten Raster von 24 mal 24 Einheiten
 * entworfen und beim Erzeugen auf die gewuenschte Groesse umgerechnet.
 *
 * @author Felix Georgi
 */
public final class WetterIkone {

    /** Kantenlaenge des Entwurfsrasters. */
    private static final double RASTER = 24.0;

    /** Reine Hilfsklasse: nicht instanziierbar. */
    private WetterIkone() {
    }

    /**
     * Erzeugt das Symbol zu einer Wetterlage.
     *
     * @param symbol  darzustellende Wetterkategorie
     * @param istTag  {@code true} fuer die Tagvariante (Sonne), sonst Mond
     * @param groesse Kantenlaenge des Symbols in Pixeln
     * @return ein fertig eingefaerbter Knoten fuer den Szenengraphen
     */
    public static Node erzeuge(WetterSymbol symbol, boolean istTag, double groesse) {
        double s = groesse / RASTER;
        List<Node> teile = new ArrayList<>();

        switch (symbol) {
            case KLAR -> teile.add(istTag ? sonne(s, 12, 12, 5) : mond(s));
            case LEICHT_BEWOELKT -> {
                teile.add(istTag ? sonne(s, 9, 9, 3.4) : kleinerMond(s));
                teile.add(wolke(s, 1.5, 1.5));
            }
            case BEWOELKT -> teile.add(wolke(s, 0, 0));
            case BEDECKT -> {
                teile.add(hintereWolke(s));
                teile.add(wolke(s, 0.5, 1.5));
            }
            case NEBEL -> {
                teile.add(wolke(s, 0, -2));
                teile.addAll(nebelStreifen(s));
            }
            case NIESEL -> {
                teile.add(wolke(s, 0, -2));
                teile.addAll(tropfen(s, 1.8));
            }
            case REGEN -> {
                teile.add(wolke(s, 0, -2));
                teile.addAll(tropfen(s, 3.4));
            }
            case SCHNEE -> {
                teile.add(wolke(s, 0, -2));
                teile.addAll(flocken(s));
            }
            case GEWITTER -> {
                teile.add(wolke(s, 0, -2.5));
                teile.add(blitz(s));
            }
        }

        Group gruppe = new Group();
        gruppe.getChildren().addAll(teile);
        gruppe.getStyleClass().add("wetter-ikone");
        // Das Symbol darf keine Mausklicks abfangen, sonst reagieren
        // darunterliegende Schaltflaechen nicht mehr.
        gruppe.setMouseTransparent(true);
        return gruppe;
    }

    /**
     * Zeichnet eine Sonne aus Scheibe und acht Strahlen.
     *
     * @param s   Umrechnungsfaktor vom Raster in Pixel
     * @param cx  Mittelpunkt auf der x-Achse im Raster
     * @param cy  Mittelpunkt auf der y-Achse im Raster
     * @param r   Radius der Scheibe im Raster
     * @return Gruppe aus Scheibe und Strahlen
     */
    private static Node sonne(double s, double cx, double cy, double r) {
        Group g = new Group();

        Circle scheibe = new Circle(cx * s, cy * s, r * s);
        scheibe.getStyleClass().add("ik-sonne");
        g.getChildren().add(scheibe);

        double innen = (r + 1.8) * s;
        double aussen = (r + 3.6) * s;
        for (int i = 0; i < 8; i++) {
            double winkel = Math.toRadians(i * 45.0);
            Line strahl = new Line(
                    cx * s + Math.cos(winkel) * innen,
                    cy * s + Math.sin(winkel) * innen,
                    cx * s + Math.cos(winkel) * aussen,
                    cy * s + Math.sin(winkel) * aussen);
            strahl.setStrokeWidth(1.7 * s);
            strahl.setStrokeLineCap(StrokeLineCap.ROUND);
            strahl.getStyleClass().add("ik-strahl");
            g.getChildren().add(strahl);
        }
        return g;
    }

    /**
     * Zeichnet eine Mondsichel.
     * <p>
     * Die Sichel entsteht, indem von einer Scheibe eine zweite, versetzte
     * Scheibe abgezogen wird. Das Ergebnis ist eine einzelne Form mit sauberer
     * Kante, im Gegensatz zu zwei uebereinandergelegten Kreisen.
     *
     * @param s Umrechnungsfaktor vom Raster in Pixel
     * @return die Sichel als Form
     */
    private static Node mond(double s) {
        return mondSichel(s, 12.5, 12, 8.0, 3.6);
    }

    /**
     * Kleinere Mondsichel fuer die Kombination mit einer Wolke.
     *
     * @param s Umrechnungsfaktor vom Raster in Pixel
     * @return die Sichel als Form
     */
    private static Node kleinerMond(double s) {
        return mondSichel(s, 9, 9, 5.2, 2.4);
    }

    /**
     * Baut eine Mondsichel aus der Differenz zweier Kreise.
     *
     * @param s       Umrechnungsfaktor vom Raster in Pixel
     * @param cx      Mittelpunkt der Vollscheibe auf der x-Achse
     * @param cy      Mittelpunkt der Vollscheibe auf der y-Achse
     * @param r       Radius der Vollscheibe
     * @param versatz Verschiebung der abziehenden Scheibe
     * @return die Sichel als Form
     */
    private static Shape mondSichel(double s, double cx, double cy, double r, double versatz) {
        Circle voll = new Circle(cx * s, cy * s, r * s);
        Circle ausschnitt = new Circle((cx + versatz) * s, (cy - versatz) * s, r * s);
        Shape sichel = Shape.subtract(voll, ausschnitt);
        sichel.getStyleClass().add("ik-mond");
        return sichel;
    }

    /**
     * Zeichnet eine Wolke aus drei Kreisen und einem abgerundeten Sockel.
     *
     * @param s   Umrechnungsfaktor vom Raster in Pixel
     * @param dx  Verschiebung auf der x-Achse im Raster
     * @param dy  Verschiebung auf der y-Achse im Raster
     * @return Gruppe der Wolkenformen
     */
    private static Node wolke(double s, double dx, double dy) {
        return wolkenForm(s, dx, dy, 1.0, "ik-wolke");
    }

    /**
     * Zeichnet die kleinere, versetzte Wolke im Hintergrund einer bedeckten Lage.
     *
     * @param s Umrechnungsfaktor vom Raster in Pixel
     * @return Gruppe der Wolkenformen
     */
    private static Node hintereWolke(double s) {
        return wolkenForm(s, -2.5, -4.0, 0.78, "ik-wolke-hinten");
    }

    /**
     * Gemeinsame Geometrie aller Wolken.
     * <p>
     * Die Stilklasse wird jeder Einzelform zugewiesen und nicht nur der Gruppe.
     * In JavaFX vererbt sich {@code -fx-fill} naemlich nicht an Kindknoten;
     * eine Regel auf der Gruppe allein bliebe wirkungslos.
     *
     * @param s          Umrechnungsfaktor vom Raster in Pixel
     * @param dx         Verschiebung auf der x-Achse im Raster
     * @param dy         Verschiebung auf der y-Achse im Raster
     * @param skalierung zusaetzliche Verkleinerung der Wolke
     * @param stilKlasse Stilklasse fuer die Einfaerbung
     * @return Gruppe aus Sockel und drei Kreisen
     */
    private static Group wolkenForm(double s, double dx, double dy,
                                    double skalierung, String stilKlasse) {
        Group g = new Group();
        double k = s * skalierung;
        double vx = dx * s;
        double vy = dy * s;

        Rectangle sockel = new Rectangle(4.5 * k + vx, 12.8 * k + vy, 15.5 * k, 4.8 * k);
        sockel.setArcWidth(4.8 * k);
        sockel.setArcHeight(4.8 * k);

        Circle links = new Circle(9.0 * k + vx, 12.6 * k + vy, 4.4 * k);
        Circle mitte = new Circle(13.4 * k + vx, 10.6 * k + vy, 5.4 * k);
        Circle rechts = new Circle(17.0 * k + vx, 13.0 * k + vy, 3.6 * k);

        for (Shape form : List.<Shape>of(sockel, links, mitte, rechts)) {
            form.getStyleClass().add(stilKlasse);
            g.getChildren().add(form);
        }
        return g;
    }

    /**
     * Zeichnet drei schraege Regenstriche unterhalb der Wolke.
     *
     * @param s      Umrechnungsfaktor vom Raster in Pixel
     * @param laenge Laenge der Striche im Raster
     * @return Liste der Striche
     */
    private static List<Node> tropfen(double s, double laenge) {
        List<Node> striche = new ArrayList<>();
        double[] x = {8.5, 12.0, 15.5};
        double y = 17.5;
        for (double xi : x) {
            Line strich = new Line(xi * s, y * s, (xi - 1.2) * s, (y + laenge) * s);
            strich.setStrokeWidth(1.8 * s);
            strich.setStrokeLineCap(StrokeLineCap.ROUND);
            strich.getStyleClass().add("ik-tropfen");
            striche.add(strich);
        }
        return striche;
    }

    /**
     * Zeichnet drei sechsstrahlige Schneesterne unterhalb der Wolke.
     *
     * @param s Umrechnungsfaktor vom Raster in Pixel
     * @return Liste der Sternformen
     */
    private static List<Node> flocken(double s) {
        List<Node> sterne = new ArrayList<>();
        double[][] mittelpunkte = {{8.5, 19.0}, {12.0, 21.0}, {15.5, 19.0}};
        for (double[] m : mittelpunkte) {
            Group stern = new Group();
            for (int i = 0; i < 3; i++) {
                double winkel = Math.toRadians(i * 60.0);
                double r = 1.7;
                Line arm = new Line(
                        (m[0] - Math.cos(winkel) * r) * s,
                        (m[1] - Math.sin(winkel) * r) * s,
                        (m[0] + Math.cos(winkel) * r) * s,
                        (m[1] + Math.sin(winkel) * r) * s);
                arm.setStrokeWidth(1.1 * s);
                arm.setStrokeLineCap(StrokeLineCap.ROUND);
                arm.getStyleClass().add("ik-flocke");
                stern.getChildren().add(arm);
            }
            sterne.add(stern);
        }
        return sterne;
    }

    /**
     * Zeichnet zwei waagerechte Streifen als Nebelandeutung.
     *
     * @param s Umrechnungsfaktor vom Raster in Pixel
     * @return Liste der Streifen
     */
    private static List<Node> nebelStreifen(double s) {
        List<Node> streifen = new ArrayList<>();
        double[][] linien = {{6.0, 18.5, 18.0}, {8.0, 21.0, 16.0}};
        for (double[] l : linien) {
            Line linie = new Line(l[0] * s, l[1] * s, l[2] * s, l[1] * s);
            linie.setStrokeWidth(1.8 * s);
            linie.setStrokeLineCap(StrokeLineCap.ROUND);
            linie.getStyleClass().add("ik-nebel");
            streifen.add(linie);
        }
        return streifen;
    }

    /**
     * Zeichnet einen Blitz unterhalb der Wolke.
     *
     * @param s Umrechnungsfaktor vom Raster in Pixel
     * @return die Blitzform
     */
    private static Node blitz(double s) {
        Polygon p = new Polygon(
                13.2 * s, 15.0 * s,
                9.0 * s, 20.2 * s,
                11.8 * s, 20.2 * s,
                10.2 * s, 23.6 * s,
                15.2 * s, 18.0 * s,
                12.4 * s, 18.0 * s);
        p.getStyleClass().add("ik-blitz");
        return p;
    }
}
