package server;
import common.InterfazServicioCripto;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class ServidorPreciosImpl extends UnicastRemoteObject implements InterfazServicioCripto {

    private final CoinGeckoService coinGeckoService;
    private final Set<String> criptosMonitoreadasActivamente;
    private static final String MONEDA_COTIZACION = "usd";
    private static final int INTERVALO_ACTUALIZACION_PRECIOS_SEGUNDOS = 5;
    private static final int INTERVALO_VERIFICACION_ALERTAS_SEGUNDOS = 5;
    private static final int DELAY_INICIAL_VERIFICACION_ALERTAS_SEGUNDOS = 5;

    // Constructor
    public ServidorPreciosImpl() throws RemoteException {
        super();
        this.coinGeckoService = new CoinGeckoService();

        this.criptosMonitoreadasActivamente = Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList("BTC", "ETH", "ADA", "SOL"))
        );

        System.out.println("ServidorPreciosImpl instanciado.");
        System.out.println("Criptomonedas monitoreadas activamente: " + criptosMonitoreadasActivamente);

        iniciarActualizadorDePreciosDesdeAPI();
        iniciarVerificadorDeAlertas();

    }

    private final Map<String, Double> cachePrecios = new ConcurrentHashMap<>();

    private final Map<String, List<AlertaDefinicion>> alertasPorUsuario = new ConcurrentHashMap<>();

    private static class AlertaDefinicion {
        String idUsuario;
        String criptomoneda; // Ej. "BTC", "ETH"
        double precioUmbral;
        String tipoCondicion; // "MAYOR_QUE" o "MENOR_QUE"
        boolean activa = true; // Para controlar si la alerta ya se notific

        public AlertaDefinicion(String idUsuario, String criptomoneda, double precioUmbral, String tipoCondicion) {
            this.idUsuario = idUsuario;
            this.criptomoneda = criptomoneda.toUpperCase();
            this.precioUmbral = precioUmbral;
            this.tipoCondicion = tipoCondicion;
        }

        // Método para convertir la alerta a un String descriptivo para el cliente
        @Override
        public String toString() {
            String simboloCondicion = "MAYOR_QUE".equals(tipoCondicion) ? ">" : "<";
            return String.format("%s %s %.2f", criptomoneda, simboloCondicion, precioUmbral);
        }
    }


/*⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣤⣤⣤⣤⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⢠⣿⠋⠀⠀⠙⢿⣦⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⣸⡇⠀⠀⠀⠀⠀⠙⢿⣦⡀⠀⠀⢀⣀⣀⣠⣤⣀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⣿⠇⠀⠀⠀⠀⠀⠀⠀⠙⠿⠿⠟⠛⠛⠋⠉⠉⠛⣷⡄
⠀⠀⠀⠀⠀⠀⠀⢠⣿⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢸⡇
⠀⠀⠀⠀⣀⣤⣶⠿⠋⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢠⡿⠃
⠀⣠⣶⠿⠛⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⡿⠃⠀
⢸⡟⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢠⡿⠁⠀⠀
⢸⣧⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⣷⡀⠀⠀
⠀⠙⠿⣶⣤⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⣷⡄⠀
⠀⠀⠀⠀⠉⠛⠿⣶⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⣿⡄
⠀⠀⠀⠀⠀⠀⠀⠘⣿⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢸⡇
⠀⠀⠀⠀⠀⠀⠀⠀⣿⡆⠀⠀⠀⠀⠀⠀⠀⣠⣶⣶⣦⣤⣤⣄⣀⣀⣤⡿⠃
⠀⠀⠀⠀⠀⠀⠀⠀⢹⡇⠀⠀⠀⠀⠀⣠⣾⠏⠀⠀⠀⠈⠉⠉⠙⠛⠉⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠘⣿⣄⠀⠀⣠⣾⠟⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠛⠛⠛⠛⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
*
*
* */
private void iniciarActualizadorDePreciosDesdeAPI() {
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(() -> {
        if (criptosMonitoreadasActivamente.isEmpty()) {
            System.out.println("[Servidor DEBUG] No hay criptomonedas para actualizar desde la API.");
            return;
        }
        try {
            System.out.println("[Servidor] Actualizando precios desde CoinGecko para: " + criptosMonitoreadasActivamente);
            Map<String, Double> nuevosPrecios = coinGeckoService.fetchPrices(criptosMonitoreadasActivamente, MONEDA_COTIZACION);

            if (!nuevosPrecios.isEmpty()) {
                nuevosPrecios.forEach(cachePrecios::put);
                System.out.println("[Servidor] Precios actualizados desde CoinGecko: " + cachePrecios);
            } else {
                System.out.println("[Servidor] No se recibieron nuevos precios de CoinGecko para las criptomonedas monitoreadas.");
            }

        } catch (IOException e) {
            System.err.println("[Servidor ERROR] No se pudo actualizar precios desde CoinGecko: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Servidor ERROR] Excepción inesperada durante actualización de precios: " + e.getMessage());
            e.printStackTrace();
        }
    }, 0, INTERVALO_ACTUALIZACION_PRECIOS_SEGUNDOS, TimeUnit.SECONDS); // Ajusta el intervalo según necesidad y límites de API
    System.out.println("Tarea de actualización de precios desde API iniciada (cada " + INTERVALO_ACTUALIZACION_PRECIOS_SEGUNDOS + " segundos).");
}

    // Tarea programada para verificar si alguna alerta se cumple
    private void iniciarVerificadorDeAlertas() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (alertasPorUsuario.isEmpty()) {
                return;
            }

            alertasPorUsuario.forEach((idUsuario, listaDeAlertas) -> {
                for (AlertaDefinicion alerta : listaDeAlertas) {
                    if (!alerta.activa) continue;

                    Double precioActual = cachePrecios.get(alerta.criptomoneda);
                    if (precioActual != null) {
                        boolean condicionCumplida = false;
                        if ("MAYOR_QUE".equals(alerta.tipoCondicion) && precioActual > alerta.precioUmbral) {
                            condicionCumplida = true;
                        } else if ("MENOR_QUE".equals(alerta.tipoCondicion) && precioActual < alerta.precioUmbral) {
                            condicionCumplida = true;
                        }

                        if (condicionCumplida) {
                            System.out.printf("[ALERTA DISPARADA] Usuario: %s, Alerta: %s, Precio Actual de %s: %.2f %s\n",
                                    idUsuario, alerta.toString(), alerta.criptomoneda, precioActual, MONEDA_COTIZACION.toUpperCase());
                        }
                    }
                }
            });
        }, DELAY_INICIAL_VERIFICACION_ALERTAS_SEGUNDOS, INTERVALO_VERIFICACION_ALERTAS_SEGUNDOS, TimeUnit.SECONDS);
        System.out.println("Tarea de verificación de alertas iniciada.");
    }

    // Implementación de los métodos de la interfaz ServicioMonitorCripto
    @Override
    public String establecerAlerta(String idUsuario, String criptomoneda, double precioUmbral, String tipoCondicion) throws RemoteException {
        if (idUsuario == null || idUsuario.trim().isEmpty() ||
                criptomoneda == null || criptomoneda.trim().isEmpty() ||
                (!tipoCondicion.equals("MAYOR_QUE") && !tipoCondicion.equals("MENOR_QUE"))) {
            throw new RemoteException("Datos de alerta inválidos: Usuario, criptomoneda y tipo de condición son obligatorios.");
        }
        String criptoUpper = criptomoneda.toUpperCase();

        // Advertencia opcional
        if (!criptosMonitoreadasActivamente.contains(criptoUpper) &&
                !CoinGeckoService.SYMBOL_TO_COINGECKO_ID_MAP.containsKey(criptoUpper)) {
            System.out.printf("[Servidor WARN] Alerta para %s (%s) que no está en monitoreo activo ni tiene mapeo conocido en CoinGeckoService. El precio podría no obtenerse.\n", criptomoneda, criptoUpper);
        }


        AlertaDefinicion nuevaAlerta = new AlertaDefinicion(idUsuario, criptoUpper, precioUmbral, tipoCondicion);
        alertasPorUsuario.computeIfAbsent(idUsuario, k -> new ArrayList<>()).add(nuevaAlerta);

        System.out.printf("[Servidor] Alerta establecida para %s: %s\n", idUsuario, nuevaAlerta.toString());
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
                .map(AlertaDefinicion::toString)
                .collect(Collectors.toList());
    }

    @Override
    public double obtenerPrecioActual(String criptomoneda) throws RemoteException {
        if (criptomoneda == null || criptomoneda.trim().isEmpty()) {
            throw new RemoteException("Nombre de criptomoneda no puede ser nulo o vacío.");
        }
        String criptoUpper = criptomoneda.toUpperCase();
        System.out.println("[Servidor] Solicitud de precio para: " + criptoUpper);

        Double precioEnCache = cachePrecios.get(criptoUpper);
        if (precioEnCache != null) {
            return precioEnCache;
        } else {
            System.out.println("[Servidor] Precio para " + criptoUpper + " no en caché. Intentando obtener desde API...");
            try {
                Double precioObtenido = coinGeckoService.fetchSinglePrice(criptoUpper, MONEDA_COTIZACION);
                if (precioObtenido != null) {
                    cachePrecios.put(criptoUpper, precioObtenido); // Guardar en caché para futuras solicitudes
                    System.out.println("[Servidor] Precio para " + criptoUpper + " obtenido de API y cacheado: " + precioObtenido);
                    return precioObtenido;
                } else {
                    System.out.println("[Servidor] No se pudo obtener precio para " + criptoUpper + " desde la API.");
                    return -1.0;
                }
            } catch (IOException e) {
                System.err.println("[Servidor ERROR] IOException al obtener precio individual para " + criptoUpper + ": " + e.getMessage());
                return -1.0;
            }
        }
    }

    @Override
    public Map<String, Double> obtenerPreciosMonitoreados(String idUsuario) throws RemoteException {
        System.out.println("[Servidor] Solicitud de precios monitoreados (devolviendo todos los precios en caché).");
        return new ConcurrentHashMap<>(cachePrecios);
    }
}