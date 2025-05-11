package server;

import common.Cripto; // Asegúrate de importar tu clase Cripto

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
import java.util.*; // Para Collections, HashMap, Map, Set, Optional
import java.util.stream.Collectors;

public class CoinGeckoService {

    private static final String API_BASE_URL = "https://api.coingecko.com/api/v3/simple/price";
    private static final int TIMEOUT_MILLISECONDS = 10000; // 10 segundos

    private final ObjectMapper objectMapper;
    public static final Map<String, String> SYMBOL_TO_COINGECKO_ID_MAP = new HashMap<>();
    static {
        SYMBOL_TO_COINGECKO_ID_MAP.put("BTC", "bitcoin");
        SYMBOL_TO_COINGECKO_ID_MAP.put("ETH", "ethereum");
        SYMBOL_TO_COINGECKO_ID_MAP.put("ADA", "cardano");
        SYMBOL_TO_COINGECKO_ID_MAP.put("SOL", "solana");
        SYMBOL_TO_COINGECKO_ID_MAP.put("DOGE", "dogecoin");
        // Puedes añadir más mapeos aquí si es necesario
    }

    public CoinGeckoService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Obtiene los datos de múltiples criptomonedas y los devuelve como un mapa de Símbolo a objeto Cripto.
     *
     * @param symbolsUnchecked Set de símbolos de criptomonedas (ej. "BTC", "ETH").
     * @param vsCurrency       La moneda contra la cual cotizar (ej. "usd").
     * @return Un Map donde la clave es el símbolo original (ej. "BTC") y el valor es el objeto Cripto correspondiente.
     * @throws IOException Si ocurre un error de red o al parsear la respuesta.
     */
    public Map<String, Cripto> fetchCriptoData(Set<String> symbolsUnchecked, String vsCurrency) throws IOException {
        if (symbolsUnchecked == null || symbolsUnchecked.isEmpty()) {
            System.out.println("[CoinGeckoService] No se proporcionaron símbolos para buscar precios.");
            return Collections.emptyMap();
        }

        // Mapear símbolos a IDs de CoinGecko, normalizando a mayúsculas y filtrando los no existentes
        Set<String> coingeckoIds = symbolsUnchecked.stream()
                .map(String::toUpperCase)
                .map(SYMBOL_TO_COINGECKO_ID_MAP::get)
                .filter(Objects::nonNull) // Equivalente a filter(id -> id != null)
                .collect(Collectors.toSet());

        if (coingeckoIds.isEmpty()) {
            System.err.println("[CoinGeckoService] No se encontraron IDs de CoinGecko válidos para los símbolos: " + symbolsUnchecked);
            return Collections.emptyMap();
        }

        String idsParam = String.join(",", coingeckoIds);
        String url = String.format("%s?ids=%s&vs_currencies=%s", API_BASE_URL, idsParam, vsCurrency.toLowerCase());

        Map<String, Cripto> symbolCriptoMap = new HashMap<>();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(TIMEOUT_MILLISECONDS))
                .setConnectTimeout(Timeout.ofMilliseconds(TIMEOUT_MILLISECONDS))
                .setResponseTimeout(Timeout.ofMilliseconds(TIMEOUT_MILLISECONDS))
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            HttpGet request = new HttpGet(url);
            System.out.println("[CoinGeckoService] Realizando petición a: " + url); // Log de la URL

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                final HttpEntity entity = response.getEntity();
                int statusCode = response.getCode();

                if (statusCode == HttpStatus.SC_OK && entity != null) {
                    String jsonResponse = EntityUtils.toString(entity);
                    //System.out.println("[CoinGeckoService DEBUG] Respuesta JSON: " + jsonResponse); // Para depuración
                    Map<String, Map<String, Double>> rawPrices = objectMapper.readValue(jsonResponse,
                            new TypeReference<Map<String, Map<String, Double>>>() {});

                    for (Map.Entry<String, Map<String, Double>> rawEntry : rawPrices.entrySet()) {
                        String coingeckoId = rawEntry.getKey(); // ej: "bitcoin"
                        Map<String, Double> currencyPriceMap = rawEntry.getValue();
                        Double price = currencyPriceMap.get(vsCurrency.toLowerCase());

                        if (price != null) {
                            // Encontrar el símbolo original (BTC, ETH) a partir del coingeckoId
                            Optional<String> originalSymbolOpt = SYMBOL_TO_COINGECKO_ID_MAP.entrySet().stream()
                                    .filter(mapEntry -> mapEntry.getValue().equals(coingeckoId))
                                    .map(Map.Entry::getKey)
                                    .findFirst();

                            if (originalSymbolOpt.isPresent()) {
                                String originalSymbol = originalSymbolOpt.get(); // ej: "BTC"
                                Cripto criptoObjeto = new Cripto(originalSymbol, price);
                                symbolCriptoMap.put(originalSymbol, criptoObjeto);
                            }
                        }
                    }
                } else {
                    String responseBody = entity != null ? EntityUtils.toString(entity) : "(sin cuerpo de respuesta)";
                    System.err.printf("[CoinGeckoService] Error al obtener precios: Código %d - %s. URL: %s\n", statusCode, responseBody, url);
                }
                EntityUtils.consume(entity); // Asegurar que la entidad se consume
            } catch (ParseException e) {
                System.err.println("[CoinGeckoService] ParseException al procesar la respuesta de la URL " + url + ": " + e.getMessage());
                throw new IOException("Error al parsear la respuesta de CoinGecko para URL " + url, e);
            }
        } catch (IOException e) {
            System.err.println("[CoinGeckoService] IOException al realizar la petición a " + url + ": " + e.getMessage());
            throw e; // Re-lanzar para que la clase que llama pueda manejarlo
        }
        return symbolCriptoMap;
    }

    /**
     * Obtiene los datos de una sola criptomoneda como objeto Cripto.
     *
     * @param symbol     Símbolo de la criptomoneda (ej. "BTC").
     * @param vsCurrency La moneda contra la cual cotizar (ej. "usd").
     * @return El objeto Cripto correspondiente, o null si no se encuentra o hay un error.
     * @throws IOException Si ocurre un error de red.
     */
    public Cripto fetchSingleCriptoData(String symbol, String vsCurrency) throws IOException {
        if (symbol == null || symbol.trim().isEmpty()) {
            System.err.println("[CoinGeckoService] Símbolo no puede ser nulo o vacío para fetchSingleCriptoData.");
            return null;
        }
        // fetchCriptoData espera un Set y normaliza a mayúsculas internamente si es necesario.
        // Aseguramos que el símbolo que buscamos en el mapa de retorno también esté en mayúsculas.
        String upperSymbol = symbol.toUpperCase();
        Map<String, Cripto> criptoDataMap = fetchCriptoData(Collections.singleton(upperSymbol), vsCurrency);
        return criptoDataMap.get(upperSymbol);
    }
}