/**
 * Moduldeklaration der WetterApp.
 * <p>
 * Seit Java 9 kann eine Anwendung als Modul beschrieben werden. Die Deklaration
 * legt fest, welche fremden Module benoetigt werden und welche eigenen Pakete
 * nach aussen sichtbar sind. Alles, was hier nicht aufgefuehrt ist, bleibt
 * gekapselt.
 * <p>
 * Der Unterschied zwischen {@code exports} und {@code opens}: {@code exports}
 * gibt ein Paket zur normalen Verwendung frei, {@code opens} erlaubt
 * zusaetzlich den Zugriff ueber Reflexion zur Laufzeit. Beides wird hier
 * gebraucht, weil JavaFX die Oberflaechenklassen und Jackson die
 * Datenklassen ueber Reflexion ansprechen.
 */
module de.felixgeorgi.wetterapp {

    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;

    // Der HTTP-Client liegt seit Java 11 in einem eigenen Modul
    // und ist nicht Teil von java.base.
    requires java.net.http;

    exports de.felixgeorgi.wetterapp;
    exports de.felixgeorgi.wetterapp.model;
    exports de.felixgeorgi.wetterapp.service;
    exports de.felixgeorgi.wetterapp.persistence;

    // JavaFX erzeugt den Controller und weist die mit @FXML
    // gekennzeichneten Felder ueber Reflexion zu.
    opens de.felixgeorgi.wetterapp to javafx.graphics, javafx.fxml;
    opens de.felixgeorgi.wetterapp.ui to javafx.fxml;

    // Jackson liest und schreibt die Felder der Datenklassen ueber Reflexion.
    opens de.felixgeorgi.wetterapp.service.dto to com.fasterxml.jackson.databind;
}
