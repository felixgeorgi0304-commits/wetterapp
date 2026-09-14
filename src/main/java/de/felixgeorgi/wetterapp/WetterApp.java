package de.felixgeorgi.wetterapp;

import de.felixgeorgi.wetterapp.persistence.DateiFavoritenRepository;
import de.felixgeorgi.wetterapp.persistence.FavoritenException;
import de.felixgeorgi.wetterapp.persistence.FavoritenRepository;
import de.felixgeorgi.wetterapp.service.OpenMeteoWetterService;
import de.felixgeorgi.wetterapp.service.WetterService;
import de.felixgeorgi.wetterapp.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Objects;

/**
 * Einstiegspunkt der Anwendung.
 * <p>
 * Die Klasse setzt das Programm aus seinen Bausteinen zusammen: Sie erzeugt
 * den Wetterdienst und den Favoritenspeicher, uebergibt beide an den
 * Controller und zeigt das Fenster an. Diese Aufgabe ist bewusst an einer
 * einzigen Stelle gebuendelt. Alle uebrigen Klassen kennen nur Schnittstellen
 * und wissen nicht, welche Umsetzung tatsaechlich verwendet wird.
 *
 * @author Felix Georgi
 * @version 2.0
 */
public class WetterApp extends Application {

    private static final String FXML = "/de/felixgeorgi/wetterapp/view/MainView.fxml";
    private static final String STYLESHEET = "/de/felixgeorgi/wetterapp/css/app.css";
    private static final String APP_ICON = "/de/felixgeorgi/wetterapp/icons/app-icon.png";

    private MainController controller;

    /**
     * Baut das Fenster auf und zeigt es an.
     *
     * @param buehne das von JavaFX bereitgestellte Hauptfenster
     * @throws IOException wenn die Oberflaechenbeschreibung nicht geladen werden kann
     */
    @Override
    public void start(Stage buehne) throws IOException {
        WetterService wetterService = new OpenMeteoWetterService();
        FavoritenRepository favoriten = new DateiFavoritenRepository();

        controller = new MainController(wetterService, favoriten);

        FXMLLoader lader = new FXMLLoader(ressource(FXML));
        // Der Controller wird von aussen gesetzt, damit ihm seine
        // Abhaengigkeiten im Konstruktor uebergeben werden koennen.
        lader.setController(controller);
        Parent wurzel = lader.load();

        Scene szene = new Scene(wurzel);
        szene.getStylesheets().add(ressource(STYLESHEET).toExternalForm());

        buehne.setTitle("WetterApp");
        buehne.setScene(szene);
        buehne.setMinWidth(760);
        buehne.setMinHeight(600);
        anwendungsSymbolSetzen(buehne);

        // Beim Schliessen den Hintergrund-Thread ordentlich beenden.
        buehne.setOnHidden(e -> controller.herunterfahren());

        buehne.show();

        ersteAnzeigeVorbereiten(favoriten);
    }

    /**
     * Zeigt beim Start den zuletzt gemerkten Ort an, sofern es einen gibt.
     *
     * @param favoriten Speicher der gemerkten Orte
     */
    private void ersteAnzeigeVorbereiten(FavoritenRepository favoriten) {
        try {
            List<String> orte = favoriten.alle();
            if (!orte.isEmpty()) {
                controller.startOrtLaden(orte.get(0));
            }
        } catch (FavoritenException e) {
            // Beim Start ist das kein Grund, den Benutzer zu stoeren:
            // Die Anwendung zeigt dann einfach ihre leere Startansicht.
            System.getLogger(WetterApp.class.getName())
                  .log(System.Logger.Level.WARNING, "Favoriten nicht lesbar", e);
        }
    }

    /**
     * Setzt das Fenstersymbol, sofern die Bilddatei vorhanden ist.
     *
     * @param buehne das Hauptfenster
     */
    private void anwendungsSymbolSetzen(Stage buehne) {
        try (InputStream strom = WetterApp.class.getResourceAsStream(APP_ICON)) {
            if (strom != null) {
                buehne.getIcons().add(new Image(strom));
            }
        } catch (IOException e) {
            // Ein fehlendes Fenstersymbol ist kein Grund, den Start abzubrechen.
        }
    }

    /**
     * Sucht eine Ressource im Klassenpfad und meldet klar, wenn sie fehlt.
     * <p>
     * Ohne diese Pruefung wuerde ein falscher Pfad erst spaeter zu einer
     * nichtssagenden {@code NullPointerException} fuehren.
     *
     * @param pfad absoluter Ressourcenpfad
     * @return die gefundene Ressource
     * @throws IllegalStateException wenn die Ressource nicht im Klassenpfad liegt
     */
    private static URL ressource(String pfad) {
        return Objects.requireNonNull(
                WetterApp.class.getResource(pfad),
                "Ressource nicht gefunden: " + pfad + " – wurde das Projekt neu gebaut?");
    }

    /**
     * Startet die Anwendung.
     *
     * @param args Kommandozeilenargumente, werden nicht ausgewertet
     */
    public static void main(String[] args) {
        launch(args);
    }
}
