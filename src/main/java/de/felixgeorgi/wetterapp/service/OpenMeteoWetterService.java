package de.felixgeorgi.wetterapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixgeorgi.wetterapp.model.Ort;
import de.felixgeorgi.wetterapp.model.WetterBericht;
import de.felixgeorgi.wetterapp.service.dto.GeoAntwort;
import de.felixgeorgi.wetterapp.service.dto.WetterAntwort;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * Wetterdaten von <a href="https://open-meteo.com">Open-Meteo</a>.
 * <p>
 * Open-Meteo ist fuer nicht kommerzielle Nutzung frei und kommt ohne
 * Registrierung und ohne Schluessel aus. Das Projekt laesst sich dadurch von
 * jedem klonen und sofort starten, und es kann kein Zugangsschluessel im
 * Quelltext landen.
 * <p>
 * Die Klasse verwendet den seit Java 11 mitgelieferten {@link HttpClient}
 * anstelle des aelteren {@code HttpURLConnection}. Er bringt Zeitlimits,
 * automatische Weiterleitungen und eine Antwortverarbeitung mit, die den
 * Zeichensatz korrekt behandelt.
 *
 * @author Felix Georgi
 */
public class OpenMeteoWetterService implements WetterService {

    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    /** Zeitlimit je Anfrage. Ohne Limit koennte die Anwendung unbegrenzt warten. */
    private static final Duration ZEITLIMIT = Duration.ofSeconds(10);

    /** Anzahl der Tage in der Vorhersage. */
    private static final int VORHERSAGE_TAGE = 7;

    private final HttpClient httpClient;
    private final ObjectMapper jsonMapper;
    private final String geocodingUrl;
    private final String forecastUrl;

    /**
     * Erzeugt den Dienst mit den oeffentlichen Open-Meteo-Adressen.
     */
    public OpenMeteoWetterService() {
        this(GEOCODING_URL, FORECAST_URL);
    }

    /**
     * Erzeugt den Dienst mit frei waehlbaren Adressen.
     * <p>
     * Dieser Konstruktor existiert, damit in Tests ein lokaler Testserver
     * angesprochen werden kann, ohne die echte API zu belasten.
     *
     * @param geocodingUrl Basisadresse der Ortssuche
     * @param forecastUrl  Basisadresse der Vorhersage
     */
    public OpenMeteoWetterService(String geocodingUrl, String forecastUrl) {
        this.geocodingUrl = Objects.requireNonNull(geocodingUrl);
        this.forecastUrl = Objects.requireNonNull(forecastUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(ZEITLIMIT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.jsonMapper = new ObjectMapper();
    }

    @Override
    public Ort sucheOrt(String suchbegriff) throws WetterServiceException {
        if (suchbegriff == null || suchbegriff.isBlank()) {
            throw new OrtNichtGefundenException("");
        }

        String url = geocodingUrl
                + "?name=" + kodieren(suchbegriff.trim())
                + "&count=1&language=de&format=json";

        GeoAntwort antwort = abrufen(url, GeoAntwort.class);
        return OpenMeteoMapper.zuOrt(antwort, suchbegriff.trim());
    }

    @Override
    public WetterBericht ladeBericht(Ort ort) throws WetterServiceException {
        Objects.requireNonNull(ort, "ort darf nicht null sein");

        String url = forecastUrl
                + "?latitude=" + ort.breite()
                + "&longitude=" + ort.laenge()
                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,"
                + "is_day,weather_code,wind_speed_10m,wind_direction_10m"
                + "&hourly=temperature_2m,weather_code,precipitation_probability,is_day"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,"
                + "precipitation_probability_max"
                + "&timezone=auto"
                + "&forecast_days=" + VORHERSAGE_TAGE;

        WetterAntwort antwort = abrufen(url, WetterAntwort.class);
        return OpenMeteoMapper.zuBericht(ort, antwort);
    }

    /**
     * Fuehrt eine GET-Anfrage aus und wertet die JSON-Antwort aus.
     * <p>
     * Alle technischen Fehler werden hier in eine {@link WetterServiceException}
     * mit verstaendlichem Text uebersetzt. Die urspruengliche Ausnahme bleibt
     * als Ursache erhalten, damit sie beim Suchen eines Fehlers nicht verloren geht.
     *
     * @param url    vollstaendige Anfrageadresse
     * @param typ    Klasse, auf die die Antwort abgebildet werden soll
     * @param <T>    Typ des Ergebnisobjekts
     * @return das ausgewertete Antwortobjekt
     * @throws WetterServiceException bei Netzwerkfehlern, Zeitueberschreitung,
     *                                Fehlercodes oder unlesbarer Antwort
     */
    private <T> T abrufen(String url, Class<T> typ) throws WetterServiceException {
        HttpRequest anfrage = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(ZEITLIMIT)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> antwort =
                    httpClient.send(anfrage, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (antwort.statusCode() != 200) {
                throw new WetterServiceException(
                        "Der Wetterdienst hat mit Fehlercode " + antwort.statusCode() + " geantwortet.");
            }
            return jsonMapper.readValue(antwort.body(), typ);

        } catch (java.net.http.HttpTimeoutException | java.net.ConnectException e) {
            throw new WetterServiceException(
                    "Der Wetterdienst ist nicht erreichbar. Bitte Internetverbindung prüfen.", e);
        } catch (IOException e) {
            throw new WetterServiceException(
                    "Die Antwort des Wetterdienstes konnte nicht gelesen werden.", e);
        } catch (InterruptedException e) {
            // Den Unterbrechungsstatus wiederherstellen, damit ein abbrechender
            // Aufrufer die Unterbrechung weiterhin bemerkt.
            Thread.currentThread().interrupt();
            throw new WetterServiceException("Die Abfrage wurde abgebrochen.", e);
        }
    }

    /**
     * Kodiert einen Suchbegriff fuer die Verwendung in einer URL.
     * <p>
     * Im Vorgaengerprojekt wurden lediglich Leerzeichen durch {@code +} ersetzt.
     * Umlaute und Sonderzeichen, wie sie in deutschen Ortsnamen haeufig
     * vorkommen, fuehrten dadurch zu fehlerhaften Anfragen.
     *
     * @param text zu kodierender Text
     * @return der URL-sichere Text
     */
    private static String kodieren(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}
