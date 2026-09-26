package de.felixgeorgi.wetterapp.ui;

import de.felixgeorgi.wetterapp.model.StundenVorhersage;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;

/**
 * Eine Kachel des Stundenverlaufs: Uhrzeit, Wettersymbol,
 * Niederschlagswahrscheinlichkeit und Temperatur.
 * <p>
 * Die Klasse erbt von {@link VBox} und bringt ihren Aufbau selbst mit. Der
 * Controller muss dadurch keine Oberflaechenelemente zusammensetzen, sondern
 * erzeugt nur noch {@code new StundenKachel(...)}. Das haelt den Controller
 * schlank und macht die Kachel an anderer Stelle wiederverwendbar.
 *
 * @author Felix Georgi
 */
public class StundenKachel extends VBox {

    private static final double IKONEN_GROESSE = 34;

    /**
     * Baut die Kachel zu einer Stundenvorhersage.
     *
     * @param vorhersage  darzustellende Stunde
     * @param jetztAmOrt  aktueller Zeitpunkt am Ort, um "Jetzt" hervorzuheben
     */
    public StundenKachel(StundenVorhersage vorhersage, LocalDateTime jetztAmOrt) {
        getStyleClass().add("stunden-kachel");
        setAlignment(Pos.CENTER);
        setSpacing(5);

        String beschriftung = Formatierung.stundenBeschriftung(vorhersage.zeitpunkt(), jetztAmOrt);
        boolean istJetzt = "Jetzt".equals(beschriftung);
        if (istJetzt) {
            getStyleClass().add("jetzt");
        }

        Label zeit = new Label(beschriftung);
        zeit.getStyleClass().add("kachel-zeit");

        var ikone = WetterIkone.erzeuge(
                vorhersage.wetterCode().symbol(), vorhersage.istTag(), IKONEN_GROESSE);

        Label temperatur = new Label(Formatierung.temperatur(vorhersage.temperatur()));
        temperatur.getStyleClass().add("kachel-temperatur");

        getChildren().addAll(zeit, ikone, temperatur);

        // Der Niederschlagshinweis erscheint nur, wenn er etwas aussagt.
        // Eine Zeile mit "0 %" an jeder Kachel waere nur Rauschen.
        if (vorhersage.niederschlagsRisiko() > 0) {
            Label regen = new Label(vorhersage.niederschlagsRisiko() + " %");
            regen.getStyleClass().add("kachel-regen");
            regen.setGraphic(UiIkone.erzeuge(UiIkone.Art.TROPFEN, 11));
            regen.setGraphicTextGap(3);
            getChildren().add(regen);
        } else {
            // Platzhalter gleicher Hoehe, damit alle Kacheln buendig bleiben.
            HBox platzhalter = new HBox();
            platzhalter.setMinHeight(15);
            platzhalter.setPrefHeight(15);
            getChildren().add(platzhalter);
        }

        Tooltip.install(this, new Tooltip(
                Formatierung.uhrzeit(vorhersage.zeitpunkt()) + " Uhr – "
                        + vorhersage.wetterCode().beschreibung() + ", "
                        + Formatierung.temperaturGenau(vorhersage.temperatur())));
    }
}
