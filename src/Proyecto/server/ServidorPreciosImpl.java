package server;
import common.InterfazServicioCripto;
import common.Cripto; // Importar la clase Cripto

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*; // Para Date, ArrayList, List, Map, Set, Optional
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ServidorPreciosImpl extends UnicastRemoteObject implements InterfazServicioCripto {

    private final CoinGeckoService coinGeckoService;
    private final Set<String> criptosMonitoreadasActivamente; // Símbolos en mayúsculas
    private static final String MONEDA_COTIZACION = "usd";
    // Ajustar intervalos según necesidad y límites de la API
    private static final int INTERVALO_ACTUALIZACION_PRECIOS_SEGUNDOS = 30; // Ej: 30-60 segundos es más seguro
    private static final int INTERVALO_VERIFICACION_ALERTAS_SEGUNDOS = 10;
    private static final int DELAY_INICIAL_VERIFICACION_ALERTAS_SEGUNDOS = 5;

    // Caché para almacenar objetos Cripto completos (Símbolo -> Objeto Cripto)
    private final Map<String, Cripto> cacheCriptoData = new ConcurrentHashMap<>();

    // Estructura para almacenar las alertas definidas por los usuarios
    private final Map<String, List<AlertaDefinicion>> alertasPorUsuario = new ConcurrentHashMap<>();

    // Clase interna para representar la definición de una alerta.
    private static class AlertaDefinicion {
        String idUsuario;
        String criptomoneda; // Símbolo en mayúsculas, ej. "BTC", "ETH"
        double precioUmbral;
        String tipoCondicion; // "MAYOR_QUE" o "MENOR_QUE"
        boolean activa = true; // Para controlar si la alerta ya se notificó (opcional)

        public AlertaDefinicion(String idUsuario, String criptomoneda, double precioUmbral, String tipoCondicion) {
            this.idUsuario = idUsuario;
            this.criptomoneda = criptomoneda.toUpperCase(); // Normalizar a mayúsculas
            this.precioUmbral = precioUmbral;
            this.tipoCondicion = tipoCondicion;
        }

        @Override
        public String toString() {
            String simboloCondicion = "MAYOR_QUE".equals(tipoCondicion) ? ">" : "<";
            return String.format("%s %s %.2f", criptomoneda, simboloCondicion, precioUmbral);
        }
    }

    public ServidorPreciosImpl() throws RemoteException {
        super(); // Necesario para UnicastRemoteObject
        this.coinGeckoService = new CoinGeckoService();

        // Definir las criptomonedas que se actualizarán activamente en segundo plano
        // Asegúrate que estos símbolos estén en SYMBOL_TO_COINGECKO_ID_MAP de CoinGeckoService
        this.criptosMonitoreadasActivamente = Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList("BTC", "ETH", "ADA", "SOL"))
        );

        System.out.println("ServidorPreciosImpl instanciado.");
        System.out.println("Criptomonedas monitoreadas activamente: " + criptosMonitoreadasActivamente);

        iniciarActualizadorDeCriptoDataDesdeAPI();
        iniciarVerificadorDeAlertas();
    }

    private void iniciarActualizadorDeCriptoDataDesdeAPI() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (criptosMonitoreadasActivamente.isEmpty()) {
                System.out.println("[Servidor DEBUG] No hay criptomonedas para actualizar desde la API.");
                return;
            }
            try {
                System.out.println("[Servidor] Actualizando datos desde CoinGecko para: " + criptosMonitoreadasActivamente);
                // fetchCriptoData ahora devuelve Map<String, Cripto>
                Map<String, Cripto> nuevosDatosCripto = coinGeckoService.fetchCriptoData(criptosMonitoreadasActivamente, MONEDA_COTIZACION);

                if (!nuevosDatosCripto.isEmpty()) {
                    // Actualizar el caché con los nuevos objetos Cripto
                    nuevosDatosCripto.forEach((simbolo, cripto) -> {
                        // Asegurarse que el símbolo en el caché también esté en mayúsculas
                        cacheCriptoData.put(simbolo.toUpperCase(), cripto);
                    });
                    System.out.println("[Servidor] Datos de Criptomonedas actualizados desde CoinGecko. Caché (Símbolo: Precio):");
                    cacheCriptoData.forEach((simbolo, cripto) ->
                            System.out.printf("  %s: %.2f (Actualizado: %tF %<tT)\n",
                                    simbolo, cripto.getPrecioUSD(), new Date(cripto.getUltimaActualizacionTimestamp())));
                } else {
                    System.out.println("[Servidor] No se recibieron nuevos datos de CoinGecko para las criptomonedas monitoreadas activamente.");
                }

            } catch (IOException e) {
                System.err.println("[Servidor ERROR] No se pudo actualizar datos desde CoinGecko: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[Servidor ERROR] Excepción inesperada durante actualización de datos: " + e.getMessage());
                e.printStackTrace(); // Imprimir stack trace para depuración más profunda
            }
        }, 0, INTERVALO_ACTUALIZACION_PRECIOS_SEGUNDOS, TimeUnit.SECONDS);
        System.out.println("Tarea de actualización de datos de criptomonedas desde API iniciada (cada " + INTERVALO_ACTUALIZACION_PRECIOS_SEGUNDOS + " segundos).");
    }

    private void iniciarVerificadorDeAlertas() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (alertasPorUsuario.isEmpty()) {
                return; // No hay alertas que verificar
            }
            // System.out.println("[Servidor DEBUG] Verificando alertas...");
            alertasPorUsuario.forEach((idUsuario, listaDeAlertas) -> {
                for (AlertaDefinicion alerta : listaDeAlertas) {
                    if (!alerta.activa) continue; // Saltar alertas ya disparadas (si se implementa esa lógica)

                    Cripto criptoActual = cacheCriptoData.get(alerta.criptomoneda); // La clave es el símbolo en mayúsculas
                    if (criptoActual != null) {
                        double precioActual = criptoActual.getPrecioUSD();
                        boolean condicionCumplida = false;
                        if ("MAYOR_QUE".equals(alerta.tipoCondicion) && precioActual > alerta.precioUmbral) {
                            condicionCumplida = true;
                        } else if ("MENOR_QUE".equals(alerta.tipoCondicion) && precioActual < alerta.precioUmbral) {
                            condicionCumplida = true;
                        }

                        if (condicionCumplida) {
                            System.out.printf("[ALERTA DISPARADA] Usuario: %s, Alerta: %s, Precio Actual de %s: %.2f %s (Timestamp del precio: %tF %<tT)\n",
                                    idUsuario, alerta.toString(), criptoActual.getSimbolo(), precioActual, MONEDA_COTIZACION.toUpperCase(), new Date(criptoActual.getUltimaActualizacionTimestamp()));
                            // Opcional: Marcar la alerta como inactiva para que no se dispare repetidamente
                            // alerta.activa = false;
                        }
                    }
                }
            });
        }, DELAY_INICIAL_VERIFICACION_ALERTAS_SEGUNDOS, INTERVALO_VERIFICACION_ALERTAS_SEGUNDOS, TimeUnit.SECONDS);
        System.out.println("Tarea de verificación de alertas iniciada (cada " + INTERVALO_VERIFICACION_ALERTAS_SEGUNDOS + " segundos).");
    }

    // Implementación de los métodos de la interfaz ServicioMonitorCripto
    // La interfaz RMI sigue devolviendo double y Map<String, Double>
    @Override
    public String establecerAlerta(String idUsuario, String criptomoneda, double precioUmbral, String tipoCondicion) throws RemoteException {
        if (idUsuario == null || idUsuario.trim().isEmpty() ||
                criptomoneda == null || criptomoneda.trim().isEmpty() ||
                (!tipoCondicion.equals("MAYOR_QUE") && !tipoCondicion.equals("MENOR_QUE"))) {
            throw new RemoteException("Datos de alerta inválidos: Usuario, criptomoneda y tipo de condición son obligatorios.");
        }
        String criptoUpper = criptomoneda.toUpperCase();

        if (!criptosMonitoreadasActivamente.contains(criptoUpper) &&
                !CoinGeckoService.SYMBOL_TO_COINGECKO_ID_MAP.containsKey(criptoUpper) &&
                !cacheCriptoData.containsKey(criptoUpper) // Verificar si ya existe en caché por una consulta previa
        ) {
            System.out.printf("[Servidor WARN] Alerta para %s (%s) que no está en monitoreo activo, ni tiene mapeo conocido en CoinGeckoService, ni está en caché. El precio podría no obtenerse o ser obsoleto si no se consulta explícitamente.\n", criptomoneda, criptoUpper);
        }

        AlertaDefinicion nuevaAlerta = new AlertaDefinicion(idUsuario, criptoUpper, precioUmbral, tipoCondicion);
        alertasPorUsuario.computeIfAbsent(idUsuario, k -> new ArrayList<>()).add(nuevaAlerta);

        System.out.printf("[Servidor] Alerta establecida para %s: %s\n", idUsuario, nuevaAlerta.toString());
        // TODO: En una fase posterior, guardar en base de datos.
        return "Alerta para " + nuevaAlerta.toString() + " establecida correctamente para el usuario " + idUsuario + ".";
    }

    @Override
    public List<String> obtenerAlertasUsuario(String idUsuario) throws RemoteException {
        if (idUsuario == null || idUsuario.trim().isEmpty()) {
            throw new RemoteException("ID de usuario no puede ser nulo o vacío.");
        }
        System.out.println("[Servidor] Solicitud para obtener alertas del usuario: " + idUsuario);
        // TODO: En una fase posterior, leer de base de datos.
        List<AlertaDefinicion> alertas = alertasPorUsuario.getOrDefault(idUsuario, new ArrayList<>());
        return alertas.stream()
                .map(AlertaDefinicion::toString) // Convierte cada AlertaDefinicion a su representación String
                .collect(Collectors.toList());
    }

    @Override
    public double obtenerPrecioActual(String criptomoneda) throws RemoteException {
        if (criptomoneda == null || criptomoneda.trim().isEmpty()) {
            throw new RemoteException("Nombre de criptomoneda no puede ser nulo o vacío.");
        }
        String criptoUpper = criptomoneda.toUpperCase();
        System.out.println("[Servidor] Solicitud de precio para: " + criptoUpper);

        Cripto criptoEnCache = cacheCriptoData.get(criptoUpper);
        if (criptoEnCache != null) {
            System.out.printf("[Servidor] Precio para %s (desde caché): %.2f USD (Actualizado: %tF %<tT)\n",
                    criptoUpper, criptoEnCache.getPrecioUSD(), new Date(criptoEnCache.getUltimaActualizacionTimestamp()));
            return criptoEnCache.getPrecioUSD(); // Devolver solo el precio
        } else {
            // Si no está en caché, intentar obtenerlo de la API bajo demanda
            System.out.println("[Servidor] Precio para " + criptoUpper + " no en caché. Intentando obtener desde API...");
            try {
                Cripto criptoObtenida = coinGeckoService.fetchSingleCriptoData(criptoUpper, MONEDA_COTIZACION);
                if (criptoObtenida != null) {
                    cacheCriptoData.put(criptoUpper, criptoObtenida); // Guardar el objeto Cripto completo en caché
                    System.out.printf("[Servidor] Precio para %s (obtenido de API y cacheado): %.2f USD (Actualizado: %tF %<tT)\n",
                            criptoUpper, criptoObtenida.getPrecioUSD(), new Date(criptoObtenida.getUltimaActualizacionTimestamp()));
                    return criptoObtenida.getPrecioUSD(); // Devolver solo el precio
                } else {
                    System.out.println("[Servidor] No se pudo obtener precio para " + criptoUpper + " desde la API.");
                    return -1.0; // Indicador de no encontrado o error
                }
            } catch (IOException e) {
                System.err.println("[Servidor ERROR] IOException al obtener precio individual para " + criptoUpper + ": " + e.getMessage());
                return -1.0; // Indicador de error
            }
        }
    }

    @Override
    public Map<String, Double> obtenerPreciosMonitoreados(String idUsuario) throws RemoteException {
        // Aunque idUsuario no se usa para filtrar, se mantiene por la interfaz
        System.out.println("[Servidor] Solicitud de precios monitoreados (devolviendo precios desde caché de objetos Cripto).");
        Map<String, Double> preciosParaCliente = new ConcurrentHashMap<>();
        cacheCriptoData.forEach((simbolo, cripto) -> {
            preciosParaCliente.put(simbolo, cripto.getPrecioUSD()); // Poner solo el precio
        });
        System.out.println("[Servidor] Devolviendo " + preciosParaCliente.size() + " precios. (Timestamp del más reciente en caché: " +
                cacheCriptoData.values().stream()
                        .map(Cripto::getUltimaActualizacionTimestamp)
                        .max(Long::compareTo)
                        .map(ts -> String.format("%tF %<tT", new Date(ts))) // Formatear el timestamp
                        .orElse("N/A") + ")");
        return preciosParaCliente;
    }
}