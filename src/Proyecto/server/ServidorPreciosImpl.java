package server;
import common.InterfazServicioCripto;
import common.Persona; //????


import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class ServidorPreciosImpl extends UnicastRemoteObject implements InterfazServicioCripto {

    // Caché para almacenar los precios de las criptomonedas (Símbolo -> Precio)
    private final Map<String, Double> cachePrecios = new ConcurrentHashMap<>();

    // Estructura para almacenar las alertas definidas por los usuarios
    // (idUsuario -> Lista de sus alertas)
    private final Map<String, List<AlertaDefinicion>> alertasPorUsuario = new ConcurrentHashMap<>();

    // Clase interna para representar la definición de una alerta.
    // No necesita ser Serializable si solo se usa dentro del servidor
    // y no se envía directamente al cliente como un objeto AlertaDefinicion.
    // Si el método obtenerAlertasUsuario devolviera List<AlertaDefinicion>, entonces sí debería ser Serializable.
    private static class AlertaDefinicion {
        String idUsuario;
        String criptomoneda; // Ej. "BTC", "ETH"
        double precioUmbral;
        String tipoCondicion; // "MAYOR_QUE" o "MENOR_QUE"
        boolean activa = true; // Para controlar si la alerta ya se notificó (opcional)

        public AlertaDefinicion(String idUsuario, String criptomoneda, double precioUmbral, String tipoCondicion) {
            this.idUsuario = idUsuario;
            this.criptomoneda = criptomoneda.toUpperCase(); // Normalizar a mayúsculas
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

    // Constructor
    public ServidorPreciosImpl() throws RemoteException {
        super(); // Necesario para UnicastRemoteObject
        System.out.println("ServidorPreciosImpl instanciado.");
        iniciarActualizadorDePreciosSimulado(); // Inicia la tarea de actualización de precios
        iniciarVerificadorDeAlertas();      // Inicia la tarea de verificación de alertas
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
    // Tarea programada para simular la actualización de precios AQUÍ HAY QUE VER BIEN QUE QUEREMOS HACER CHIQUILLOS
    private void iniciarActualizadorDePreciosSimulado() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            // Simulación de obtención de precios
            cachePrecios.put("BTC", 50000.0 + (Math.random() * 5000 - 2500)); // BTC entre 47500 y 52500
            cachePrecios.put("ETH", 3000.0 + (Math.random() * 500 - 250));   // ETH entre 2750 y 3250
            cachePrecios.put("ADA", 1.0 + (Math.random() * 0.5 - 0.25));     // ADA entre 0.75 y 1.25
            // System.out.println("[Servidor DEBUG] Precios simulados actualizados: " + cachePrecios);
        }, 0, 10, TimeUnit.SECONDS); // Actualiza cada 10 segundos (ajusta según necesidad)
        System.out.println("Tarea de actualización de precios simulados iniciada.");
    }

    // Tarea programada para verificar si alguna alerta se cumple
    private void iniciarVerificadorDeAlertas() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (alertasPorUsuario.isEmpty()) {
                return; // No hay alertas que verificar
            }
            // System.out.println("[Servidor DEBUG] Verificando alertas...");
            alertasPorUsuario.forEach((idUsuario, listaDeAlertas) -> {
                for (AlertaDefinicion alerta : listaDeAlertas) {
                    if (!alerta.activa) continue; // Saltar alertas ya disparadas (si implementas esa lógica)

                    Double precioActual = cachePrecios.get(alerta.criptomoneda);
                    if (precioActual != null) {
                        boolean condicionCumplida = false;
                        if ("MAYOR_QUE".equals(alerta.tipoCondicion) && precioActual > alerta.precioUmbral) {
                            condicionCumplida = true;
                        } else if ("MENOR_QUE".equals(alerta.tipoCondicion) && precioActual < alerta.precioUmbral) {
                            condicionCumplida = true;
                        }

                        if (condicionCumplida) {
                            System.out.printf("[ALERTA DISPARADA] Usuario: %s, Alerta: %s, Precio Actual de %s: %.2f USD\n",
                                    idUsuario, alerta.toString(), alerta.criptomoneda, precioActual);
                            // Opcional: Marcar la alerta como inactiva para que no se dispare repetidamente
                            // alerta.activa = false;
                            // Para la entrega parcial, imprimir en la consola del servidor es suficiente.
                        }
                    }
                }
            });
        }, 5, 15, TimeUnit.SECONDS); // Verifica cada 15 segundos, después de una posible actualización de precios
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

        AlertaDefinicion nuevaAlerta = new AlertaDefinicion(idUsuario, criptomoneda, precioUmbral, tipoCondicion);
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
        // TODO: En una fase posterior, podría haber lógica para forzar actualización desde API si el precio es muy viejo.
        return cachePrecios.getOrDefault(criptoUpper, -1.0); // Devuelve -1.0 si la cripto no está en el caché
    }

    @Override
    public Map<String, Double> obtenerPreciosMonitoreados(String idUsuario) throws RemoteException {
        // Por simplicidad, devolvemos todos los precios que el servidor tiene en caché.
        // No usamos idUsuario aquí, pero podría usarse para personalización futura.
        System.out.println("[Servidor] Solicitud de precios monitoreados (devolviendo todos los precios en caché).");
        return new ConcurrentHashMap<>(cachePrecios); // Devolver una copia para evitar problemas de concurrencia en el cliente
    }
}