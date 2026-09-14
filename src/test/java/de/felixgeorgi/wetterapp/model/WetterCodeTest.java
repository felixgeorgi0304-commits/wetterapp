package de.felixgeorgi.wetterapp.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests zur Zuordnung der WMO-Wettercodes.
 * <p>
 * Diese Zuordnung entscheidet darueber, welches Symbol der Benutzer zu sehen
 * bekommt. Im Vorgaengerprojekt bestand sie aus einer Kette von Abfragen ueber
 * Zahlenbereiche; ein vertauschter Bereich waere dort niemandem aufgefallen.
 * Die folgenden Tests halten das Verhalten fest.
 *
 * @author Felix Georgi
 */
@DisplayName("WetterCode: Zuordnung der WMO-Codes")
class WetterCodeTest {

    /**
     * Prueft fuer je einen Vertreter jeder Wetterlage, dass der Code auf das
     * erwartete Symbol fuehrt. Ein parametrisierter Test schreibt dieselbe
     * Pruefung einmal und laesst sie mit mehreren Datensaetzen laufen.
     */
    @ParameterizedTest(name = "Code {0} wird zu Symbol {1}")
    @CsvSource({
            "0,  KLAR",
            "1,  LEICHT_BEWOELKT",
            "2,  BEWOELKT",
            "3,  BEDECKT",
            "45, NEBEL",
            "48, NEBEL",
            "51, NIESEL",
            "61, REGEN",
            "65, REGEN",
            "71, SCHNEE",
            "77, SCHNEE",
            "80, REGEN",
            "86, SCHNEE",
            "95, GEWITTER",
            "99, GEWITTER"
    })
    void ordnetCodeDemErwartetenSymbolZu(int code, WetterSymbol erwartet) {
        assertEquals(erwartet, WetterCode.vonCode(code).symbol());
    }

    @Test
    @DisplayName("Ein unbekannter Code liefert UNBEKANNT statt null")
    void unbekannterCodeLiefertRueckfallwert() {
        // Die API koennte kuenftig neue Codes senden. Die Anwendung darf
        // deswegen nicht abstuerzen, sondern muss weiterlaufen.
        assertSame(WetterCode.UNBEKANNT, WetterCode.vonCode(12345));
        assertSame(WetterCode.UNBEKANNT, WetterCode.vonCode(-1));
        assertNotNull(WetterCode.vonCode(12345).symbol());
    }

    @Test
    @DisplayName("Jeder bekannte Code laesst sich zurueckuebersetzen")
    void jederCodeIstUeberVonCodeErreichbar() {
        for (WetterCode wert : WetterCode.values()) {
            if (wert == WetterCode.UNBEKANNT) {
                continue;
            }
            assertSame(wert, WetterCode.vonCode(wert.code()),
                    "Code " + wert.code() + " fuehrt nicht auf " + wert.name());
        }
    }

    @Test
    @DisplayName("Kein Code ist doppelt vergeben")
    void codesSindEindeutig() {
        // Waeren zwei Konstanten mit demselben Code angelegt, wuerde beim
        // Aufbau der Nachschlagetabelle eine davon stillschweigend gewinnen.
        Set<Integer> gesehen = new HashSet<>();
        for (WetterCode wert : WetterCode.values()) {
            if (wert == WetterCode.UNBEKANNT) {
                continue;
            }
            assertTrue(gesehen.add(wert.code()),
                    "Code " + wert.code() + " ist mehrfach vergeben");
        }
    }

    /**
     * Jede Konstante braucht eine Beschreibung und ein Symbol, sonst bleibt
     * in der Oberflaeche eine Luecke.
     */
    @ParameterizedTest(name = "{0} ist vollstaendig beschrieben")
    @EnumSource(WetterCode.class)
    void jedeKonstanteIstVollstaendig(WetterCode wert) {
        assertAll(
                () -> assertNotNull(wert.beschreibung()),
                () -> assertFalse(wert.beschreibung().isBlank()),
                () -> assertNotNull(wert.symbol()));
    }
}
