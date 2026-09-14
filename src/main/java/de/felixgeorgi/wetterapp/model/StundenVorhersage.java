package de.felixgeorgi.wetterapp.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Vorhersage fuer eine einzelne Stunde.
 *
 * @param zeitpunkt              Beginn der Stunde in Ortszeit
 * @param temperatur             Temperatur in Grad Celsius
 * @param niederschlagsRisiko    Niederschlagswahrscheinlichkeit in Prozent (0 bis 100)
 * @param wetterCode             Wetterlage
 * @param istTag                 {@code true}, wenn es zu diesem Zeitpunkt am Ort hell ist
 *
 * @author Felix Georgi
 */
public record StundenVorhersage(
        LocalDateTime zeitpunkt,
        double temperatur,
        int niederschlagsRisiko,
        WetterCode wetterCode,
        boolean istTag) {

    public StundenVorhersage {
        Objects.requireNonNull(zeitpunkt, "zeitpunkt darf nicht null sein");
        Objects.requireNonNull(wetterCode, "wetterCode darf nicht null sein");
    }
}
