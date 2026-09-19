package de.felixgeorgi.wetterapp.ui;

import de.felixgeorgi.wetterapp.model.AktuellesWetter;
import de.felixgeorgi.wetterapp.model.Ort;
import de.felixgeorgi.wetterapp.model.TagesVorhersage;
import de.felixgeorgi.wetterapp.model.WetterBericht;
import de.felixgeorgi.wetterapp.model.WetterSymbol;
import de.felixgeorgi.wetterapp.persistence.FavoritenException;
import de.felixgeorgi.wetterapp.persistence.FavoritenRepository;
import de.felixgeorgi.wetterapp.service.OrtNichtGefundenException;
import de.felixgeorgi.wetterapp.service.WetterService;
import de.felixgeorgi.wetterapp.service.WetterServiceException;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Steuert das Hauptfenster: nimmt Eingaben entgegen, stoesst das Laden der
 * Wetterdaten an und fuellt die Oberflaeche mit dem Ergebnis.
 * <p>
 * <strong>Nebenlaeufigkeit.</strong> JavaFX zeichnet die gesamte Oberflaeche in
 * einem einzigen Thread. Wird in diesem Thread auf eine Netzwerkantwort
 * gewartet, friert das Fenster fuer die Dauer der Abfrage ein: Es reagiert
 * nicht mehr auf Klicks und zeichnet sich nicht neu. Deshalb laeuft jede
 * Abfrage hier in einem {@link Task} auf einem Hintergrund-Thread. Erst dessen
 * Ergebnis wird von JavaFX wieder im Oberflaechen-Thread zugestellt
 * ({@code setOnSucceeded}), wo das Fenster gefahrlos veraendert werden darf.
 * <p>
 * <strong>Abhaengigkeiten.</strong> Wetterdienst und Favoritenspeicher werden
 * dem Controller im Konstruktor uebergeben. Er beschafft sie sich nicht selbst
 * und kennt nur die Schnittstellen. Dadurch bleibt er von der konkreten
 * Datenquelle unabhaengig und liesse sich mit Attrappen testen.
 *
 * @author Felix Georgi
 */
public class MainController {

    private static final double GROSSE_IKONE = 118;
    private static final double START_IKONE = 92;
    private static final double KNOPF_IKONE = 19;

    /**
     * Hoehe, die die waagerechte Bildlaufleiste am unteren Rand des
     * Stundenverlaufs einnimmt. Sie wird bei der Hoehenberechnung eingerechnet,
     * damit die Kacheln vollstaendig sichtbar bleiben.
     */
    private static final double BILDLAUFLEISTE_HOEHE = 16;

    // --- Oberflaechenelemente aus MainView.fxml -----------------------------

    @FXML private BorderPane wurzel;
    @FXML private HBox suchRahmen;
    @FXML private StackPane suchIkone;
    @FXML private TextField suchfeld;
    @FXML private Button suchenKnopf;
    @FXML private Button favoritKnopf;
    @FXML private Button favoritenKnopf;
    @FXML private Button themaKnopf;

    @FXML private HBox meldungsLeiste;
    @FXML private Label meldungsText;
    @FXML private Button meldungSchliessen;

    @FXML private VBox startAnsicht;
    @FXML private StackPane startIkone;
    @FXML private VBox ladeAnsicht;
    @FXML private Label ladeText;

    @FXML private ScrollPane inhaltScroll;
    @FXML private VBox inhalt;
    @FXML private VBox heldKarte;
    @FXML private Label ortLabel;
    @FXML private Label zeitLabel;
    @FXML private StackPane grossIkone;
    @FXML private Label temperaturLabel;
    @FXML private Label beschreibungLabel;
    @FXML private VBox kennzahlen;

    @FXML private ScrollPane stundenScroll;
    @FXML private HBox stundenLeiste;
    @FXML private VBox tageListe;

    @FXML private VBox favoritenBereich;
    @FXML private Button favoritenSchliessen;
    @FXML private Label favoritenLeerText;
    @FXML private ListView<String> favoritenListe;

    @FXML private Label standLabel;

    // --- Abhaengigkeiten und Zustand ---------------------------------------

    private final WetterService wetterService;
    private final FavoritenRepository favoriten;

    /**
     * Thread fuer die Netzwerkabfragen.
     * <p>
     * Ein einzelner Thread genuegt, weil immer nur eine Abfrage gleichzeitig
     * sinnvoll ist. Er laeuft als Daemon, damit er das Beenden der Anwendung
     * nicht verhindert.
     */
    private final ExecutorService hintergrund = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "wetter-abfrage");
        t.setDaemon(true);
        return t;
    });

    /** Laufende Abfrage, um sie bei einer neuen Suche abbrechen zu koennen. */
    private Task<WetterBericht> laufendeAbfrage;

    /** Zuletzt erfolgreich geladener Ort, Grundlage fuer die Favoritenschaltflaeche. */
    private Ort aktuellerOrt;

    /** Aktuelles Farbschema. */
    private Thema thema = Thema.HELL;

    /**
     * Erzeugt den Controller.
     *
     * @param wetterService Dienst, der die Wetterdaten liefert
     * @param favoriten     Speicher fuer die gemerkten Orte
     */
    public MainController(WetterService wetterService, FavoritenRepository favoriten) {
        this.wetterService = Objects.requireNonNull(wetterService, "wetterService");
        this.favoriten = Objects.requireNonNull(favoriten, "favoriten");
    }

    /**
     * Wird von JavaFX nach dem Laden der FXML-Datei aufgerufen und richtet
     * Symbole, Ereignisbehandlung und Ausgangszustand ein.
     */
    @FXML
    private void initialize() {
        symboleSetzen();
        ereignisseVerbinden();
        stundenLeisteEinrichten();

        wurzel.getStyleClass().add(thema.stilKlasse());
        favoritenListe.setCellFactory(liste -> new FavoritenZelle());
        favoritenAktualisieren();

        // Beim Start steht der Schreibcursor im Suchfeld.
        Platform.runLater(suchfeld::requestFocus);
    }

    /**
     * Haengt die Vektorsymbole an die Schaltflaechen.
     */
    private void symboleSetzen() {
        suchIkone.getChildren().setAll(UiIkone.erzeuge(UiIkone.Art.SUCHE, 16));
        startIkone.getChildren().setAll(
                WetterIkone.erzeuge(WetterSymbol.LEICHT_BEWOELKT, true, START_IKONE));

        knopfSymbol(suchenKnopf, UiIkone.Art.SUCHE, "Wetter abrufen (Enter)");
        knopfSymbol(favoritKnopf, UiIkone.Art.LESEZEICHEN, "Ort zu den Favoriten hinzufügen");
        knopfSymbol(favoritenKnopf, UiIkone.Art.LISTE, "Favoriten anzeigen");
        knopfSymbol(themaKnopf, thema.schaltflaechenIkone(), thema.hinweis());
        knopfSymbol(meldungSchliessen, UiIkone.Art.SCHLIESSEN, "Hinweis ausblenden");
        knopfSymbol(favoritenSchliessen, UiIkone.Art.SCHLIESSEN, "Favoriten ausblenden");

        suchenKnopf.getStyleClass().add("haupt-knopf");
        favoritKnopf.setDisable(true);
    }

    /**
     * Richtet den Stundenverlauf so ein, dass er sich ausschliesslich
     * waagerecht bewegen laesst.
     * <p>
     * Dafuer sind zwei Dinge noetig. Erstens nimmt die waagerechte
     * Bildlaufleiste am unteren Rand einige Pixel Hoehe weg. Der sichtbare
     * Bereich wird dadurch niedriger als die Kacheln, und genau um diese
     * Differenz liesse sich die Leiste senkrecht verschieben. Die Hoehe des
     * Rollbereichs richtet sich deshalb nach der Hoehe des Inhalts zuzueglich
     * des Platzes fuer die Bildlaufleiste. Zweitens wird der senkrechte
     * Rollweg mit {@code setVmax(0)} auf null gesetzt: Entsteht doch einmal
     * eine Differenz, bleibt der Inhalt trotzdem oben stehen.
     * <p>
     * Zusaetzlich wird das Mausrad umgelenkt. Ohne das bliebe ein Drehen ueber
     * der Leiste wirkungslos, weil es senkrecht rollen wuerde – und das ist
     * nun abgeschaltet.
     */
    private void stundenLeisteEinrichten() {
        // Senkrechter Rollweg von 0 bis 0: es gibt nichts zu verschieben.
        stundenScroll.setVmax(0);

        // Hoehe des Rollbereichs an den Inhalt anpassen, sobald die Kacheln
        // stehen. Die Bildlaufleiste bekommt ihren Platz zusaetzlich.
        stundenLeiste.heightProperty().addListener((o, alt, neu) -> {
            double hoehe = neu.doubleValue() + BILDLAUFLEISTE_HOEHE;
            stundenScroll.setMinHeight(hoehe);
            stundenScroll.setPrefHeight(hoehe);
            stundenScroll.setMaxHeight(hoehe);
        });

        // Mausrad und senkrechte Wischgeste bewegen die Leiste waagerecht.
        stundenScroll.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() == 0) {
                // Eine bereits waagerechte Geste laeuft unveraendert weiter.
                return;
            }
            double ueberhang = stundenLeiste.getWidth()
                    - stundenScroll.getViewportBounds().getWidth();
            if (ueberhang <= 0) {
                // Alles passt ohnehin ins Bild.
                return;
            }
            stundenScroll.setHvalue(stundenScroll.getHvalue() - e.getDeltaY() / ueberhang);
            e.consume();
        });
    }

    /**
     * Versieht eine Schaltflaeche mit Symbol und Kurzinfo.
     *
     * @param knopf   zu bestueckende Schaltflaeche
     * @param art     gewuenschtes Symbol
     * @param hinweis Text der Kurzinfo
     */
    private void knopfSymbol(Button knopf, UiIkone.Art art, String hinweis) {
        knopf.setGraphic(UiIkone.erzeuge(art, KNOPF_IKONE));
        knopf.setTooltip(new Tooltip(hinweis));
    }

    /**
     * Verbindet alle Bedienelemente mit ihrem Verhalten.
     */
    private void ereignisseVerbinden() {
        suchenKnopf.setOnAction(e -> sucheStarten(suchfeld.getText()));
        suchfeld.setOnAction(e -> sucheStarten(suchfeld.getText()));

        // Sobald der Benutzer wieder tippt, verschwindet eine alte Fehlermeldung.
        suchfeld.textProperty().addListener((o, alt, neu) -> meldungVerbergen());
        suchfeld.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                suchfeld.clear();
            }
        });

        // Das Suchfeld bekommt eine Rahmenhervorhebung, solange es den Fokus hat.
        suchfeld.focusedProperty().addListener((o, alt, neu) ->
                suchRahmen.pseudoClassStateChanged(
                        javafx.css.PseudoClass.getPseudoClass("fokus"), neu));

        favoritKnopf.setOnAction(e -> favoritUmschalten());
        favoritenKnopf.setOnAction(e -> favoritenBereichUmschalten());
        favoritenSchliessen.setOnAction(e -> favoritenBereichZeigen(false));
        themaKnopf.setOnAction(e -> themaWechseln());
        meldungSchliessen.setOnAction(e -> meldungVerbergen());

    }

    // --- Suche -------------------------------------------------------------

    /**
     * Startet eine Wetterabfrage im Hintergrund.
     * <p>
     * Eine bereits laufende Abfrage wird abgebrochen. Ohne das wuerde bei
     * schnellen Eingaben die langsamere von zwei Antworten die schnellere
     * ueberschreiben und ein veraltetes Ergebnis anzeigen.
     *
     * @param eingabe der eingegebene Ortsname
     */
    private void sucheStarten(String eingabe) {
        String ortsName = eingabe == null ? "" : eingabe.strip();
        if (ortsName.isEmpty()) {
            meldungZeigen("Bitte gib einen Ort ein.");
            suchfeld.requestFocus();
            return;
        }

        if (laufendeAbfrage != null && laufendeAbfrage.isRunning()) {
            laufendeAbfrage.cancel();
        }

        meldungVerbergen();
        ladenAnzeigen(true);

        Task<WetterBericht> aufgabe = new Task<>() {
            @Override
            protected WetterBericht call() throws WetterServiceException {
                // Dieser Abschnitt laeuft im Hintergrund-Thread.
                // Hier darf kein Oberflaechenelement angefasst werden.
                return wetterService.ladeBericht(ortsName);
            }
        };

        // Die folgenden Rueckmeldungen stellt JavaFX im Oberflaechen-Thread zu.
        aufgabe.setOnSucceeded(e -> {
            ladenAnzeigen(false);
            berichtAnzeigen(aufgabe.getValue());
        });
        aufgabe.setOnFailed(e -> {
            ladenAnzeigen(false);
            fehlerBehandeln(aufgabe.getException(), ortsName);
        });
        aufgabe.setOnCancelled(e -> ladenAnzeigen(false));

        laufendeAbfrage = aufgabe;
        hintergrund.execute(aufgabe);
    }

    /**
     * Uebersetzt eine Ausnahme der Dienstschicht in eine Meldung fuer den Benutzer.
     *
     * @param fehler    aufgetretene Ausnahme
     * @param ortsName  gesuchter Ort, fuer die Formulierung der Meldung
     */
    private void fehlerBehandeln(Throwable fehler, String ortsName) {
        if (fehler instanceof OrtNichtGefundenException) {
            meldungZeigen("Kein Ort mit dem Namen „" + ortsName
                    + "“ gefunden. Bitte Schreibweise prüfen.");
        } else if (fehler instanceof WetterServiceException) {
            meldungZeigen(fehler.getMessage());
        } else {
            meldungZeigen("Unerwarteter Fehler: " + fehler.getMessage());
        }
        suchRahmen.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("fehler"), true);
    }

    // --- Anzeige -----------------------------------------------------------

    /**
     * Uebertraegt einen geladenen Bericht in die Oberflaeche.
     *
     * @param bericht der anzuzeigende Wetterbericht
     */
    private void berichtAnzeigen(WetterBericht bericht) {
        aktuellerOrt = bericht.ort();
        AktuellesWetter aktuell = bericht.aktuell();

        ortLabel.setText(bericht.ort().anzeigeName());
        zeitLabel.setText("Stand " + Formatierung.uhrzeit(aktuell.zeitpunkt())
                + " Uhr Ortszeit");
        temperaturLabel.setText(Formatierung.temperatur(aktuell.temperatur()));
        beschreibungLabel.setText(grossAnfang(aktuell.wetterCode().beschreibung()));

        grossIkone.getChildren().setAll(WetterIkone.erzeuge(
                aktuell.wetterCode().symbol(), aktuell.istTag(), GROSSE_IKONE));

        kennzahlenFuellen(aktuell);
        stimmungSetzen(aktuell);
        stundenFuellen(bericht);
        tageFuellen(bericht);

        favoritKnopf.setDisable(false);
        favoritSymbolAktualisieren();

        standLabel.setText("Zuletzt aktualisiert: "
                + Formatierung.uhrzeit(java.time.LocalDateTime.now()) + " Uhr");

        startAnsicht.setVisible(false);
        startAnsicht.setManaged(false);
        inhaltScroll.setVisible(true);
        inhaltScroll.setManaged(true);

        einblenden(inhalt);
        suchRahmen.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("fehler"), false);
    }

    /**
     * Baut die drei Kennzahlen neben der Temperatur auf.
     *
     * @param aktuell aktuelle Wetterlage
     */
    private void kennzahlenFuellen(AktuellesWetter aktuell) {
        kennzahlen.getChildren().setAll(
                kennzahl(UiIkone.Art.THERMOMETER, "Gefühlt",
                        Formatierung.temperatur(aktuell.gefuehlt())),
                kennzahl(UiIkone.Art.WIND, "Wind",
                        Formatierung.wind(aktuell.windGeschwindigkeit())
                                + " aus " + aktuell.windRichtungKurz()),
                kennzahl(UiIkone.Art.TROPFEN, "Luftfeuchte",
                        Formatierung.prozent(aktuell.luftfeuchte())));
    }

    /**
     * Baut eine einzelne Kennzahlzeile.
     *
     * @param art          Symbol der Kennzahl
     * @param bezeichnung  Name der Kennzahl
     * @param wert         formatierter Wert
     * @return die fertige Zeile
     */
    private Node kennzahl(UiIkone.Art art, String bezeichnung, String wert) {
        Label name = new Label(bezeichnung);
        name.getStyleClass().add("kennzahl-name");

        Label zahl = new Label(wert);
        zahl.getStyleClass().add("kennzahl-wert");

        VBox texte = new VBox(name, zahl);
        texte.setSpacing(1);

        HBox zeile = new HBox(10, UiIkone.erzeuge(art, 17), texte);
        zeile.setAlignment(Pos.CENTER_LEFT);
        zeile.getStyleClass().add("kennzahl");
        return zeile;
    }

    /**
     * Passt den Farbverlauf der Hauptkarte an die Wetterlage an.
     * <p>
     * Die Karte bekommt je nach Lage eine eigene Stilklasse; die zugehoerigen
     * Verlaeufe stehen im Stylesheet. Fruehere Klassen werden zuvor entfernt,
     * damit sie sich nicht ansammeln.
     *
     * @param aktuell aktuelle Wetterlage
     */
    private void stimmungSetzen(AktuellesWetter aktuell) {
        heldKarte.getStyleClass().removeIf(k -> k.startsWith("stimmung-"));
        heldKarte.getStyleClass().add("stimmung-" + stimmungsName(aktuell));
    }

    /**
     * Ordnet einer Wetterlage den Namen ihres Farbverlaufs zu.
     *
     * @param aktuell aktuelle Wetterlage
     * @return Namensbestandteil der Stilklasse
     */
    private static String stimmungsName(AktuellesWetter aktuell) {
        if (!aktuell.istTag()) {
            return "nacht";
        }
        return switch (aktuell.wetterCode().symbol()) {
            case KLAR, LEICHT_BEWOELKT -> "klar";
            case BEWOELKT, BEDECKT, NEBEL -> "wolken";
            case NIESEL, REGEN -> "regen";
            case SCHNEE -> "schnee";
            case GEWITTER -> "gewitter";
        };
    }

    /**
     * Fuellt den Stundenverlauf und laesst die Kacheln nacheinander einblenden.
     *
     * @param bericht geladener Wetterbericht
     */
    private void stundenFuellen(WetterBericht bericht) {
        stundenLeiste.getChildren().clear();
        stundenScroll.setHvalue(0);

        int index = 0;
        for (var stunde : bericht.stunden()) {
            StundenKachel kachel = new StundenKachel(stunde, bericht.aktuell().zeitpunkt());
            stundenLeiste.getChildren().add(kachel);

            // Versetztes Einblenden: Die Kacheln erscheinen von links nach rechts.
            kachel.setOpacity(0);
            FadeTransition blende = new FadeTransition(Duration.millis(260), kachel);
            blende.setFromValue(0);
            blende.setToValue(1);
            blende.setDelay(Duration.millis(index * 22.0));
            blende.play();
            index++;
        }
    }

    /**
     * Fuellt die Tagesvorhersage.
     *
     * @param bericht geladener Wetterbericht
     */
    private void tageFuellen(WetterBericht bericht) {
        tageListe.getChildren().clear();
        List<TagesVorhersage> tage = bericht.tage();
        if (tage.isEmpty()) {
            Label leer = new Label("Für diesen Ort liegt keine Tagesvorhersage vor.");
            leer.getStyleClass().add("leer-hinweis");
            tageListe.getChildren().add(leer);
            return;
        }

        // Gemeinsame Ober- und Untergrenze fuer die Spannenbalken aller Tage.
        double wocheMin = tage.stream().mapToDouble(TagesVorhersage::minTemperatur).min().orElse(0);
        double wocheMax = tage.stream().mapToDouble(TagesVorhersage::maxTemperatur).max().orElse(1);
        LocalDate heute = bericht.aktuell().zeitpunkt().toLocalDate();

        for (TagesVorhersage tag : tage) {
            tageListe.getChildren().add(new TagesZeile(tag, heute, wocheMin, wocheMax));
        }
    }

    /**
     * Blendet einen Bereich weich ein und laesst ihn dabei leicht aufsteigen.
     *
     * @param knoten einzublendender Knoten
     */
    private void einblenden(Node knoten) {
        FadeTransition blende = new FadeTransition(Duration.millis(320), knoten);
        blende.setFromValue(0);
        blende.setToValue(1);

        TranslateTransition schub = new TranslateTransition(Duration.millis(320), knoten);
        schub.setFromY(14);
        schub.setToY(0);
        schub.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(blende, schub).play();
    }

    /**
     * Schaltet zwischen Lade- und Ergebnisansicht um.
     *
     * @param laedt {@code true}, solange eine Abfrage laeuft
     */
    private void ladenAnzeigen(boolean laedt) {
        ladeAnsicht.setVisible(laedt);
        ladeAnsicht.setManaged(laedt);
        suchenKnopf.setDisable(laedt);
        ladeText.setText("Wetterdaten werden geladen …");

        if (laedt) {
            inhaltScroll.setVisible(false);
            inhaltScroll.setManaged(false);
            startAnsicht.setVisible(false);
            startAnsicht.setManaged(false);
        } else if (aktuellerOrt == null) {
            startAnsicht.setVisible(true);
            startAnsicht.setManaged(true);
        }
    }

    // --- Meldungen ---------------------------------------------------------

    /**
     * Zeigt einen Hinweis in der Leiste unter der Kopfzeile.
     * <p>
     * Das Vorgaengerprojekt oeffnete hierfuer ein modales Dialogfenster. Eine
     * eingebettete Leiste unterbricht den Arbeitsfluss nicht und laesst sich
     * bequem wegtippen.
     *
     * @param text anzuzeigender Hinweis
     */
    private void meldungZeigen(String text) {
        meldungsText.setText(text);
        meldungsLeiste.setVisible(true);
        meldungsLeiste.setManaged(true);

        FadeTransition blende = new FadeTransition(Duration.millis(200), meldungsLeiste);
        blende.setFromValue(0);
        blende.setToValue(1);
        blende.play();
    }

    /**
     * Blendet den Hinweis wieder aus.
     */
    private void meldungVerbergen() {
        if (!meldungsLeiste.isVisible()) {
            return;
        }
        meldungsLeiste.setVisible(false);
        meldungsLeiste.setManaged(false);
        suchRahmen.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("fehler"), false);
    }

    // --- Favoriten ---------------------------------------------------------

    /**
     * Nimmt den aktuellen Ort in die Favoriten auf oder entfernt ihn.
     */
    private void favoritUmschalten() {
        if (aktuellerOrt == null) {
            return;
        }
        try {
            favoriten.umschalten(aktuellerOrt.name());
            favoritSymbolAktualisieren();
            favoritenAktualisieren();
        } catch (FavoritenException e) {
            meldungZeigen(e.getMessage());
        }
    }

    /**
     * Setzt das Lesezeichensymbol passend zum Zustand des aktuellen Ortes.
     */
    private void favoritSymbolAktualisieren() {
        if (aktuellerOrt == null) {
            return;
        }
        try {
            boolean gemerkt = favoriten.enthaelt(aktuellerOrt.name());
            knopfSymbol(favoritKnopf,
                    gemerkt ? UiIkone.Art.LESEZEICHEN_VOLL : UiIkone.Art.LESEZEICHEN,
                    gemerkt ? "Aus den Favoriten entfernen" : "Zu den Favoriten hinzufügen");
            favoritKnopf.pseudoClassStateChanged(
                    javafx.css.PseudoClass.getPseudoClass("aktiv"), gemerkt);
        } catch (FavoritenException e) {
            meldungZeigen(e.getMessage());
        }
    }

    /**
     * Laedt die Favoritenliste neu und blendet den Leerhinweis passend ein.
     */
    private void favoritenAktualisieren() {
        try {
            List<String> orte = favoriten.alle();
            favoritenListe.getItems().setAll(orte);

            boolean leer = orte.isEmpty();
            favoritenLeerText.setVisible(leer);
            favoritenLeerText.setManaged(leer);
            favoritenListe.setVisible(!leer);
            favoritenListe.setManaged(!leer);
        } catch (FavoritenException e) {
            meldungZeigen(e.getMessage());
        }
    }

    /**
     * Klappt die Favoritenleiste ein oder aus.
     */
    private void favoritenBereichUmschalten() {
        favoritenBereichZeigen(!favoritenBereich.isVisible());
    }

    /**
     * Zeigt oder verbirgt die Favoritenleiste.
     *
     * @param sichtbar gewuenschter Zustand
     */
    private void favoritenBereichZeigen(boolean sichtbar) {
        if (sichtbar) {
            favoritenAktualisieren();
            favoritenBereich.setVisible(true);
            favoritenBereich.setManaged(true);

            TranslateTransition schub = new TranslateTransition(
                    Duration.millis(220), favoritenBereich);
            schub.setFromX(favoritenBereich.getPrefWidth());
            schub.setToX(0);
            schub.setInterpolator(Interpolator.EASE_OUT);
            schub.play();
        } else {
            TranslateTransition schub = new TranslateTransition(
                    Duration.millis(180), favoritenBereich);
            schub.setToX(favoritenBereich.getPrefWidth());
            schub.setOnFinished(e -> {
                favoritenBereich.setVisible(false);
                favoritenBereich.setManaged(false);
                favoritenBereich.setTranslateX(0);
            });
            schub.play();
        }
        favoritenKnopf.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("aktiv"), sichtbar);
    }

    /**
     * Listenzelle eines Favoriten: Ortsname mit Schaltflaeche zum Entfernen.
     * <p>
     * Eine eigene Zellenklasse ist noetig, weil eine {@code ListView} ihre
     * Zellen wiederverwendet. Wuerde man stattdessen fertige Bedienelemente in
     * die Liste legen, waeren bei langen Listen unnoetig viele Objekte im
     * Speicher.
     */
    private class FavoritenZelle extends ListCell<String> {

        private final Label name = new Label();
        private final Button entfernen = new Button();
        private final HBox zeile = new HBox();

        FavoritenZelle() {
            name.getStyleClass().add("favorit-name");

            entfernen.setGraphic(UiIkone.erzeuge(UiIkone.Art.PAPIERKORB, 15));
            entfernen.getStyleClass().add("favorit-entfernen");
            entfernen.setTooltip(new Tooltip("Favorit entfernen"));
            entfernen.setFocusTraversable(false);
            entfernen.setOnAction(e -> {
                e.consume();
                entferneFavorit(getItem());
            });
            // Zusaetzlich das Mausereignis abfangen: Ohne das wuerde der Klick
            // weiter nach oben zur Zeile laufen und dort die Wetterabfrage
            // ausloesen, obwohl der Ort gerade entfernt wird.
            entfernen.setOnMouseClicked(javafx.event.Event::consume);

            Region abstand = new Region();
            HBox.setHgrow(abstand, javafx.scene.layout.Priority.ALWAYS);

            zeile.getChildren().addAll(name, abstand, entfernen);
            zeile.setAlignment(Pos.CENTER_LEFT);
            zeile.setSpacing(6);
            zeile.getStyleClass().add("favorit-zeile");

            // Ein Klick auf die Zeile laedt den Ort. Der Verweis liegt
            // bewusst an der Zelle und nicht an der Liste: So loest ein Klick
            // in den leeren Bereich unterhalb der Eintraege nichts aus.
            zeile.setOnMouseClicked(e -> {
                String ort = getItem();
                if (ort != null) {
                    suchfeld.setText(ort);
                    sucheStarten(ort);
                }
            });
        }

        @Override
        protected void updateItem(String ort, boolean leer) {
            super.updateItem(ort, leer);
            if (leer || ort == null) {
                setText(null);
                setGraphic(null);
            } else {
                name.setText(ort);
                setText(null);
                setGraphic(zeile);
            }
        }
    }

    /**
     * Entfernt einen Favoriten und aktualisiert die Anzeige.
     *
     * @param ort zu entfernender Ortsname
     */
    private void entferneFavorit(String ort) {
        if (ort == null) {
            return;
        }
        try {
            favoriten.entfernen(ort);
            favoritenAktualisieren();
            favoritSymbolAktualisieren();
        } catch (FavoritenException e) {
            meldungZeigen(e.getMessage());
        }
    }

    // --- Farbschema --------------------------------------------------------

    /**
     * Wechselt zwischen hellem und dunklem Farbschema.
     */
    private void themaWechseln() {
        wurzel.getStyleClass().remove(thema.stilKlasse());
        thema = thema.wechseln();
        wurzel.getStyleClass().add(thema.stilKlasse());
        knopfSymbol(themaKnopf, thema.schaltflaechenIkone(), thema.hinweis());

        // Kurzes Aufblenden, damit der Wechsel nicht hart wirkt.
        FadeTransition blende = new FadeTransition(Duration.millis(220), wurzel);
        blende.setFromValue(0.75);
        blende.setToValue(1);
        blende.play();
    }

    // --- Sonstiges ---------------------------------------------------------

    /**
     * Schreibt den ersten Buchstaben gross.
     *
     * @param text umzuwandelnder Text
     * @return der Text mit grossem Anfangsbuchstaben
     */
    private static String grossAnfang(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /**
     * Sucht einen Ort beim Programmstart, ohne dass der Benutzer etwas eingeben muss.
     * <p>
     * Wird von {@code WetterApp} mit dem ersten gespeicherten Favoriten
     * aufgerufen, damit die Anwendung nicht leer startet.
     *
     * @param ortsName anzuzeigender Ort
     */
    public void startOrtLaden(String ortsName) {
        suchfeld.setText(ortsName);
        // Kurz warten, damit das Fenster zuerst erscheint und der Ladevorgang
        // sichtbar wird, statt beim Start unbemerkt abzulaufen.
        PauseTransition warten = new PauseTransition(Duration.millis(180));
        warten.setOnFinished(e -> sucheStarten(ortsName));
        warten.play();
    }

    /**
     * Beendet den Hintergrund-Thread. Wird beim Schliessen des Fensters aufgerufen.
     */
    public void herunterfahren() {
        if (laufendeAbfrage != null) {
            laufendeAbfrage.cancel();
        }
        hintergrund.shutdownNow();
    }
}
