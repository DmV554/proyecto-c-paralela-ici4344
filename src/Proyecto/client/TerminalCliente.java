package client;

import common.InterfazServicioCripto;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;

/**
 * Controlador que maneja la lógica de comunicación con el servidor RMI.
 * Esta clase actúa como intermediario entre la interfaz de usuario (RunCliente)
 * y la lógica de negocio del servidor.
 */
public class TerminalCliente {
    private InterfazServicioCripto servicio;
    private String idUsuario;
    private boolean conectado = false;

    /**
     * Establece conexión con el servidor RMI
     *
     * @param host Host donde se encuentra el servidor RMI
     * @param idUsuario Identificador del usuario
     * @return true si la conexión fue exitosa, false en caso contrario
     */
    public boolean conectar(String host, String idUsuario) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, 1099);
            String serviceName = "ServidorCriptoMonitor";
            servicio = (InterfazServicioCripto) registry.lookup(serviceName);
            this.idUsuario = idUsuario;
            this.conectado = true;
            return true;
        } catch (Exception e) {
            System.err.println("Error al conectar con el servidor: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza el ID de usuario
     *
     * @param idUsuario Nuevo ID de usuario
     */
    public void actualizarIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    /**
     * Obtiene los precios de todas las criptomonedas monitoreadas/cacheadas por el servidor.
     *
     * @return String formateado con los precios
     * @throws Exception Si ocurre un error al comunicarse con el servidor
     */
    public String obtenerPreciosMonitoreados() throws Exception {
        verificarConexion();
        Map<String, Double> precios = servicio.obtenerPreciosMonitoreados(idUsuario);

        if (precios.isEmpty()) {
            return "No hay precios (cacheados/monitoreados) disponibles en este momento.";
        }

        StringBuilder resultado = new StringBuilder();
        resultado.append("Precios actuales (cacheados/monitoreados) en USD:\n");
        resultado.append("╔════════╦═══════════════╗\n");
        resultado.append("║ SÍMBOLO ║    PRECIO     ║\n");
        resultado.append("╠════════╬═══════════════╣\n");

        for (Map.Entry<String, Double> entrada : precios.entrySet()) {
            if (entrada.getValue() >= 0) {
                resultado.append(String.format("║ %-7s ║ $%-12.2f ║\n",
                        entrada.getKey(), entrada.getValue()));
            } else {
                resultado.append(String.format("║ %-7s ║ %-13s ║\n", // Ajuste para texto
                        entrada.getKey(), (entrada.getValue() == -1.0 ? "No disponible" : "Error")));
            }
        }
        resultado.append("╚════════╩═══════════════╝");
        return resultado.toString();
    }

    /**
     * Obtiene los precios de todas las criptomonedas base conocidas por el servidor.
     *
     * @return String formateado con los precios
     * @throws Exception Si ocurre un error al comunicarse con el servidor
     */
    public String obtenerPreciosDeTodasLasBases() throws Exception { // NUEVO MÉTODO
        verificarConexion();
        Map<String, Double> precios = servicio.obtenerPreciosDeTodasLasBases(idUsuario);

        if (precios.isEmpty()) {
            return "No hay precios de criptomonedas base disponibles o configuradas en el servidor.";
        }

        StringBuilder resultado = new StringBuilder();
        resultado.append("Precios actuales de criptomonedas base en USD:\n");
        resultado.append("╔════════╦═══════════════╗\n");
        resultado.append("║ SÍMBOLO ║    PRECIO     ║\n");
        resultado.append("╠════════╬═══════════════╣\n");

        for (Map.Entry<String, Double> entrada : precios.entrySet()) {
            if (entrada.getValue() >= 0) {
                resultado.append(String.format("║ %-7s ║ $%-12.2f ║\n",
                        entrada.getKey(), entrada.getValue()));
            } else {
                resultado.append(String.format("║ %-7s ║ %-13s ║\n",
                        entrada.getKey(), (entrada.getValue() == -1.0 ? "No disponible" : "Error (-2.0)")));
            }
        }
        resultado.append("╚════════╩═══════════════╝");
        return resultado.toString();
    }


    /**
     * Obtiene el precio de una criptomoneda específica
     *
     * @param criptomoneda Símbolo de la criptomoneda
     * @return String formateado con el precio
     * @throws Exception Si ocurre un error al comunicarse con el servidor
     */
    public String obtenerPrecioEspecifico(String criptomoneda) throws Exception {
        verificarConexion();
        double precio = servicio.obtenerPrecioActual(criptomoneda);

        if (precio < 0) { // Incluye -1.0 (no encontrado) y -2.0 (error general)
            if (precio == -1.0) {
                return "No se encontró información para " + criptomoneda + ".";
            } else {
                return "Error al obtener el precio para " + criptomoneda + ".";
            }
        }
        return String.format("Precio actual de %s: $%.2f USD",
                criptomoneda, precio);
    }

    /**
     * Establece una alerta de precio
     *
     * @param criptomoneda Símbolo de la criptomoneda
     * @param precioUmbral Precio umbral para la alerta
     * @param tipoCondicion MAYOR_QUE o MENOR_QUE
     * @return Mensaje de confirmación
     * @throws Exception Si ocurre un error al comunicarse con el servidor
     */
    public String establecerAlerta(String criptomoneda, double precioUmbral,
                                   String tipoCondicion) throws Exception {
        verificarConexion();
        return servicio.establecerAlerta(idUsuario, criptomoneda, precioUmbral, tipoCondicion);
    }

    /**
     * Obtiene las alertas configuradas por el usuario
     *
     * @return String formateado con las alertas
     * @throws Exception Si ocurre un error al comunicarse con el servidor
     */
    public String obtenerAlertasUsuario() throws Exception {
        verificarConexion();
        List<String> alertas = servicio.obtenerAlertasUsuario(idUsuario);

        if (alertas.isEmpty()) {
            return "No tienes alertas configuradas.";
        }

        StringBuilder resultado = new StringBuilder();
        resultado.append("Alertas configuradas:\n");
        resultado.append("╔═══════════════════════════════╗\n");
        resultado.append("║           ALERTAS             ║\n");
        resultado.append("╠═══════════════════════════════╣\n");

        for (String alerta : alertas) {
            resultado.append(String.format("║ %-29s ║\n", alerta));
        }
        resultado.append("╚═══════════════════════════════╝");
        return resultado.toString();
    }

    /**
     * Verifica que exista una conexión con el servidor
     *
     * @throws Exception Si no hay conexión establecida
     */
    private void verificarConexion() throws Exception {
        if (!conectado || servicio == null) {
            throw new Exception("No hay conexión con el servidor.");
        }
    }

    /**
     * Método main original mantenido para compatibilidad,
     * pero marcado como obsoleto
     *
     * @deprecated Utilizar RunCliente en su lugar
     */
    @Deprecated
    public static void main(String[] args) {
        System.out.println("Este método está obsoleto. Por favor, utilice RunCliente como punto de entrada.");
        RunCliente.main(args);
    }
}