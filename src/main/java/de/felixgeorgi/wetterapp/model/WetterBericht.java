package de.felixgeorgi.wetterapp.model;

import java.util.List;
import java.util.Objects;

/**
 * Vollstaendiger Wetterbericht fuer einen Ort: aktuelle Lage, Stundenverlauf
 * und Tagesvorhersage in einem Objekt.
 * <p>
 * Dieses Objekt ist das Ergebnis eines einzigen Ladevorgangs. Die Oberflaeche
 * bekommt damit alle Daten auf einmal und kann sich in einem Zug aktualisieren,
 * statt nacheinander mehrere Abfragen anzustossen.
 *
 * @param ort        Ort, auf den sich der Bericht bezieht
 * @param aktuell    aktuelle Wetterlage
 * @param stunden    Verlauf der naechsten Stunden, chronologisch sortiert
 * @param tage       Vorhersage der naechsten Tage, chronologisch sortiert
 *
 * @author Felix Georgi
 */
public record WetterBericht(
        Ort ort,
        AktuellesWetter aktuell,
        List<StundenVorhersage> stunden,
        List<TagesVorhersage> tage) {

    /**
     * Kompakter Konstruktor. Die Listen werden in unveraenderliche Kopien
     * ueberfuehrt, damit ein Aufrufer den Bericht nachtraeglich nicht
     * versehentlich veraendern kann.
     */
    public WetterBericht {
        Objects.requireNonNull(ort, "ort darf nicht null sein");
        Objects.requireNonNull(aktuell, "aktuell darf nicht null sein");
        stunden = List.copyOf(Objects.requireNonNullElse(stunden, List.of()));
        tage = List.copyOf(Objects.requireNonNullElse(tage, List.of()));
    }
}
