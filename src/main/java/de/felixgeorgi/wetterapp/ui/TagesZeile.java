package de.felixgeorgi.wetterapp.ui;

import de.felixgeorgi.wetterapp.model.TagesVorhersage;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;

/**
 * Eine Zeile der Tagesvorhersage.
 * <p>
 * Neben Wochentag, Symbol und Temperaturen zeigt die Zeile einen Balken, der
 * die Spanne zwischen Tief- und Hoechstwert im Verhaeltnis zur gesamten Woche
 * darstellt. Dadurch ist auf einen Blick erkennbar, welche Tage waermer oder
 * kaelter ausfallen als die uebrigen, ohne die Zahlen vergleichen zu muessen.
 *
 * @author Felix Georgi
 */
public class TagesZeile extends HBox {

    private static final double IKONEN_GROESSE = 26;
    private static final double BALKEN_BREITE = 120;

    /**
     * Baut die Zeile zu einer Tagesvorhersage.
     *
     * @param vorhersage    darzustellender Tag
     * @param heute         heutiges Datum am Ort, um "Heute" hervorzuheben
     * @param wocheMin      niedrigste Temperatur der gesamten Vorhersage
     * @param wocheMax      hoechste Temperatur der gesamten Vorhersage
     */
    public TagesZeile(TagesVorhersage vorhersage, LocalDate heute,
                      double wocheMin, double wocheMax) {
        getStyleClass().add("tages-zeile");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);

        Label tag = new Label(vorhersage.istHeute(heute) ? "Heute" : vorhersage.wochentagKurz());
        tag.getStyleClass().add("zeile-tag");
        tag.setMinWidth(52);
        if (vorhersage.istHeute(heute)) {
            getStyleClass().add("heute");
        }

        var ikone = WetterIkone.erzeuge(vorhersage.wetterCode().symbol(), true, IKONEN_GROESSE);

        Label regen = new Label(vorhersage.niederschlagsRisiko() > 0
                ? vorhersage.niederschlagsRisiko() + " %" : "");
        regen.getStyleClass().add("zeile-regen");
        regen.setMinWidth(46);

        Label min = new Label(Formatierung.temperatur(vorhersage.minTemperatur()));
        min.getStyleClass().add("zeile-min");
        min.setMinWidth(38);
        min.setAlignment(Pos.CENTER_RIGHT);

        Region abstand = new Region();
        HBox.setHgrow(abstand, Priority.ALWAYS);

        Label max = new Label(Formatierung.temperatur(vorhersage.maxTemperatur()));
        max.getStyleClass().add("zeile-max");
        max.setMinWidth(38);

        getChildren().addAll(tag, ikone, regen, abstand, min,
                temperaturBalken(vorhersage, wocheMin, wocheMax), max);

        Tooltip.install(this, new Tooltip(
                Formatierung.datum(vorhersage.datum()) + " – "
                        + vorhersage.wetterCode().beschreibung() + ", "
                        + Formatierung.temperaturGenau(vorhersage.minTemperatur()) + " bis "
                        + Formatierung.temperaturGenau(vorhersage.maxTemperatur())));
    }

    /**
     * Baut den Spannenbalken eines Tages.
     * <p>
     * Die Tagesspanne wird auf die Gesamtspanne der Woche abgebildet: Ein Tag,
     * der genau den Bereich zwischen Wochentief und Wochenhoch abdeckt, fuellt
     * den Balken vollstaendig aus. Ein schmaler, weit rechts liegender Balken
     * bedeutet einen gleichmaessig warmen Tag.
     *
     * @param vorhersage darzustellender Tag
     * @param wocheMin   niedrigste Temperatur der gesamten Vorhersage
     * @param wocheMax   hoechste Temperatur der gesamten Vorhersage
     * @return der fertige Balken
     */
    private static Region temperaturBalken(TagesVorhersage vorhersage,
                                           double wocheMin, double wocheMax) {
        // Division durch null verhindern, falls alle Tage identisch ausfallen.
        double spanne = Math.max(wocheMax - wocheMin, 0.1);

        double anteilStart = (vorhersage.minTemperatur() - wocheMin) / spanne;
        double anteilBreite = (vorhersage.maxTemperatur() - vorhersage.minTemperatur()) / spanne;

        Region schiene = new Region();
        schiene.getStyleClass().add("balken-schiene");
        schiene.setPrefSize(BALKEN_BREITE, 6);
        schiene.setMinWidth(BALKEN_BREITE);
        schiene.setMaxWidth(BALKEN_BREITE);

        Region fuellung = new Region();
        fuellung.getStyleClass().add("balken-fuellung");
        // Mindestbreite, damit ein Tag ohne Temperaturunterschied nicht unsichtbar wird.
        double breite = Math.max(anteilBreite * BALKEN_BREITE, 8);
        fuellung.setPrefSize(breite, 6);
        fuellung.setMinWidth(breite);
        fuellung.setMaxWidth(breite);
        StackPane.setMargin(fuellung, new javafx.geometry.Insets(
                0, 0, 0, Math.min(anteilStart * BALKEN_BREITE, BALKEN_BREITE - breite)));

        StackPane stapel = new StackPane(schiene, fuellung);
        stapel.setAlignment(Pos.CENTER_LEFT);
        stapel.setMinWidth(BALKEN_BREITE);
        stapel.setPrefWidth(BALKEN_BREITE);
        stapel.setMaxWidth(BALKEN_BREITE);
        return stapel;
    }
}
