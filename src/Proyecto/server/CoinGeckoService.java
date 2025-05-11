package server;

// Imports para Apache HttpClient 5
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;

// Imports para Jackson
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CoinGeckoService {

    private static final String API_BASE_URL = "https://api.coingecko.com/api/v3/simple/price";
    private static final int TIMEOUT_MILLISECONDS = 10000; // 10 segundos en milisegundos

    private final ObjectMapper objectMapper;
    public static final Map<String, String> SYMBOL_TO_COINGECKO_ID_MAP = new HashMap<>();
    static {
        SYMBOL_TO_COINGECKO_ID_MAP.put("BTC", "bitcoin");
        SYMBOL_TO_COINGECKO_ID_MAP.put("ETH", "ethereum");
        SYMBOL_TO_COINGECKO_ID_MAP.put("ADA", "cardano");
        SYMBOL_TO_COINGECKO_ID_MAP.put("SOL", "solana");
        SYMBOL_TO_COINGECKO_ID_MAP.put("DOGE", "dogecoin");
    }

    public CoinGeckoService() {
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Double> fetchPrices(Set<String> symbolsUnchecked, String vsCurrency) throws IOException {
        if (symbolsUnchecked == null || symbolsUnchecked.isEmpty()) {
            System.out.println("[CoinGeckoService] No se proporcionaron símbolos para buscar precios.");
            return Collections.emptyMap();
        }

        Set<String> coingeckoIds = symbolsUnchecked.stream()
                .map(String::toUpperCase)
                .map(SYMBOL_TO_COINGECKO_ID_MAP::get)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (coingeckoIds.isEmpty()) {
            System.err.println("[CoinGeckoService] No se encontraron IDs de CoinGecko válidos para los símbolos: " + symbolsUnchecked);
            return Collections.emptyMap();
        }

        String idsParam = String.join(",", coingeckoIds);
        String url = String.format("%s?ids=%s&vs_currencies=%s", API_BASE_URL, idsParam, vsCurrency.toLowerCase());

        Map<String, Double> symbolPriceMap = new HashMap<>();

        // Configuración de timeouts usando RequestConfig
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(TIMEOUT_MILLISECONDS)) // Timeout para obtener una conexión del pool
                .setConnectTimeout(Timeout.ofMilliseconds(TIMEOUT_MILLISECONDS))           // Timeout para establecer la conexión
                .setResponseTimeout(Timeout.ofMilliseconds(TIMEOUT_MILLISECONDS))        // Timeout para esperar datos (socket timeout)
                .build();

        // Se crea un cliente nuevo para cada petición con la configuración de request por defecto
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig) // Aplicar el RequestConfig por defecto al cliente
                .build()) {

            HttpGet request = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                final HttpEntity entity = response.getEntity();
                int statusCode = response.getCode();

                if (statusCode == HttpStatus.SC_OK && entity != null) {
                    String jsonResponse = EntityUtils.toString(entity);
                    Map<String, Map<String, Double>> rawPrices = objectMapper.readValue(jsonResponse,
                            new TypeReference<Map<String, Map<String, Double>>>() {});

                    for (Map.Entry<String, Map<String, Double>> entry : rawPrices.entrySet()) {
                        String coingeckoId = entry.getKey();
                        Map<String, Double> currencyPriceMap = entry.getValue();
                        Double price = currencyPriceMap.get(vsCurrency.toLowerCase());

                        if (price != null) {
                            SYMBOL_TO_COINGECKO_ID_MAP.entrySet().stream()
                                    .filter(mapEntry -> mapEntry.getValue().equals(coingeckoId))
                                    .map(Map.Entry::getKey)
                                    .findFirst()
                                    .ifPresent(symbol -> symbolPriceMap.put(symbol, price));
                        }
                    }
                } else {
                    String responseBody = entity != null ? EntityUtils.toString(entity) : "No body";
                    System.err.printf("[CoinGeckoService] Error al obtener precios: Código %d - %s. URL: %s\n", statusCode, responseBody, url);
                }
                EntityUtils.consume(entity);
            } catch (ParseException e) {
                System.err.println("[CoinGeckoService] ParseException al procesar la respuesta de la URL " + url + ": " + e.getMessage());
                throw new IOException("Error al parsear la respuesta de CoinGecko para URL " + url, e);
            }
        } catch (IOException e) {
            System.err.println("[CoinGeckoService] IOException al realizar la petición a " + url + ": " + e.getMessage());
            throw e;
        }
        return symbolPriceMap;
    }

    public Double fetchSinglePrice(String symbol, String vsCurrency) throws IOException {
        if (symbol == null || symbol.trim().isEmpty()) {
            System.err.println("[CoinGeckoService] Símbolo no puede ser nulo o vacío para fetchSinglePrice.");
            return null;
        }
        Map<String, Double> prices = fetchPrices(Collections.singleton(symbol), vsCurrency);
        return prices.get(symbol.toUpperCase());
    }
}