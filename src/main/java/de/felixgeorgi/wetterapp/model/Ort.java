package de.felixgeorgi.wetterapp.model;

import java.util.Objects;

/**
 * Ein geografischer Ort mit seinen Koordinaten.
 * <p>
 * Als {@code record} umgesetzt: Die Werte stehen nach dem Erzeugen fest
 * ({@code final}), {@code equals}, {@code hashCode} und {@code toString}
 * erzeugt der Compiler. Ein unveraenderliches Objekt kann gefahrlos zwischen
 * dem Hintergrund-Thread, der die Daten laedt, und dem Oberflaechen-Thread
 * weitergereicht werden.
 *
 * @param name      Ortsname, zum Beispiel "Mittweida"
 * @param region    Bundesland oder Region, darf {@code null} sein
 * @param land      Staat, zum Beispiel "Deutschland", darf {@code null} sein
 * @param breite    geografische Breite in Grad
 * @param laenge    geografische Laenge in Grad
 * @param zeitzone  IANA-Zeitzone des Ortes, zum Beispiel "Europe/Berlin"
 *
 * @author Felix Georgi
 */
public record Ort(
        String name,
        String region,
        String land,
        double breite,
        double laenge,
        String zeitzone) {

    /**
     * Kompakter Konstruktor: prueft die Pflichtangaben, bevor das Objekt entsteht.
     * So kann es keinen Ort ohne Namen geben, und ein Fehler faellt an der
     * Stelle auf, an der er entsteht, statt irgendwo spaeter in der Oberflaeche.
     *
     * @throws NullPointerException     wenn {@code name} fehlt
     * @throws IllegalArgumentException wenn die Koordinaten ausserhalb des gueltigen Bereichs liegen
     */
    public Ort {
        Objects.requireNonNull(name, "name darf nicht null sein");
        if (breite < -90 || breite > 90) {
            throw new IllegalArgumentException("Breitengrad ausserhalb von -90..90: " + breite);
        }
        if (laenge < -180 || laenge > 180) {
            throw new IllegalArgumentException("Laengengrad ausserhalb von -180..180: " + laenge);
        }
    }

    /**
     * Liefert eine gut lesbare Bezeichnung fuer die Oberflaeche,
     * zum Beispiel "Mittweida, Sachsen, Deutschland". Leere Bestandteile
     * werden weggelassen.
     *
     * @return Anzeigename des Ortes
     */
    public String anzeigeName() {
        StringBuilder sb = new StringBuilder(name);
        if (region != null && !region.isBlank() && !region.equals(name)) {
            sb.append(", ").append(region);
        }
        if (land != null && !land.isBlank()) {
            sb.append(", ").append(land);
        }
        return sb.toString();
    }
}
