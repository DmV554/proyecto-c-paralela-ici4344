package client;

import common.InterfazServicioCripto;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
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

    private static final String HOST_PRINCIPAL = "localhost";
    private static final int PUERTO_PRINCIPAL = 1099;
    private static final String SERVICIO_PRINCIPAL = "ServidorCriptoMonitor";

    private static final String HOST_RESPALDO = "localhost";
    private static final int PUERTO_RESPALDO = 1100;
    private static final String SERVICIO_RESPALDO = "ServidorCriptoMonitorRespaldo";


    // Dentro de la clase TerminalCliente
    public void conectarConFailover() throws Exception {
        try {
            System.out.println("Intentando conectar al servidor PRINCIPAL...");
            Registry registry = LocateRegistry.getRegistry(HOST_PRINCIPAL, PUERTO_PRINCIPAL);
            this.servicio = (InterfazServicioCripto) registry.lookup(SERVICIO_PRINCIPAL);
            System.out.println("✓ Conexión establecida con el servidor PRINCIPAL.");
            return;
        } catch (Exception e) {
            System.err.println("✘ Falló la conexión con el servidor principal.");
        }

        try {
            System.out.println("Intentando conectar al servidor de RESPALDO...");
            Registry registry = LocateRegistry.getRegistry(HOST_RESPALDO, PUERTO_RESPALDO);
            this.servicio = (InterfazServicioCripto) registry.lookup(SERVICIO_RESPALDO);
            System.out.println("✓ Conexión establecida con el servidor de RESPALDO.");
        } catch (Exception e) {
            throw new Exception("No hay servidores disponibles. La aplicación no puede continuar.");
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
        Map<String, Double> precios;
        try {
            // 2. PRIMER INTENTO
            precios = servicio.obtenerPreciosMonitoreados(idUsuario);

        } catch (RemoteException e) {
            // 3. SI FALLA, RECONECTAR Y REINTENTAR
            System.err.println("Se perdió la conexión. Intentando reconectar...");

            conectarConFailover(); // Llama al método de reconexión

            System.out.println("Reconexión exitosa. Reintentando la operación...");

            // SEGUNDO INTENTO
            precios = servicio.obtenerPreciosMonitoreados(idUsuario);
        }

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
        Map<String, Double> precios;
        try {
            // 2. PRIMER INTENTO
            precios = servicio.obtenerPreciosDeTodasLasBases(idUsuario);

        } catch (RemoteException e) {
            // 3. SI FALLA, RECONECTAR Y REINTENTAR
            System.err.println("Se perdió la conexión. Intentando reconectar...");

            conectarConFailover(); // Llama al método de reconexión

            System.out.println("Reconexión exitosa. Reintentando la operación...");

            // SEGUNDO INTENTO
            precios = servicio.obtenerPreciosDeTodasLasBases(idUsuario);
        }

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
        double precio;
        try {
            // 2. PRIMER INTENTO
            precio = servicio.obtenerPrecioActual(criptomoneda);

        } catch (RemoteException e) {
            // 3. SI FALLA, RECONECTAR Y REINTENTAR
            System.err.println("Se perdió la conexión. Intentando reconectar...");

            conectarConFailover(); // Llama al método de reconexión

            System.out.println("Reconexión exitosa. Reintentando la operación...");

            // SEGUNDO INTENTO
            precio = servicio.obtenerPrecioActual(criptomoneda);
        }

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
        try {
            // 2. PRIMER INTENTO
            return servicio.establecerAlerta(idUsuario, criptomoneda, precioUmbral, tipoCondicion);

        } catch (RemoteException e) {
            // 3. SI FALLA, RECONECTAR Y REINTENTAR
            System.err.println("Se perdió la conexión. Intentando reconectar...");

            conectarConFailover(); // Llama al método de reconexión

            System.out.println("Reconexión exitosa. Reintentando la operación...");

            // SEGUNDO INTENTO
            return servicio.establecerAlerta(idUsuario, criptomoneda, precioUmbral, tipoCondicion);
        }
    }

    /**
     * Obtiene las alertas configuradas por el usuario
     *
     * @return String formateado con las alertas
     * @throws Exception Si ocurre un error al comunicarse con el servidor
     */
    public String obtenerAlertasUsuario() throws Exception {
        List<String> alertas;

        try {
            // --- PRIMER INTENTO ---
            // Esta es la única línea que puede lanzar una RemoteException
            alertas = servicio.obtenerAlertasUsuario(idUsuario);

        } catch (RemoteException e) {
            // Si el primer intento falla, entramos aquí.
            System.err.println("Se perdió la conexión con el servidor. Intentando reconectar...");

            // Llamamos al método que se encarga de cambiar al servidor de respaldo.
            conectarConFailover();

            System.out.println("Reconexión exitosa. Reintentando obtener las alertas...");

            alertas = servicio.obtenerAlertasUsuario(idUsuario);
        }

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
     * Solicita al servidor eliminar una alerta específica.
     *
     * @param idAlertaDB El ID de la alerta en la base de datos a eliminar.
     * @return Mensaje de confirmación o error del servidor.
     * @throws Exception Si ocurre un error al comunicarse con el servidor.
     */
    public String eliminarAlerta(int idAlertaDB) throws Exception {
        try {
            // 2. PRIMER INTENTO
            return servicio.eliminarAlerta(idUsuario, idAlertaDB);

        } catch (RemoteException e) {
            // 3. SI FALLA, RECONECTAR Y REINTENTAR
            System.err.println("Se perdió la conexión. Intentando reconectar...");

            conectarConFailover(); // Llama al método de reconexión

            System.out.println("Reconexión exitosa. Reintentando la operación...");

            // SEGUNDO INTENTO
            return servicio.eliminarAlerta(idUsuario, idAlertaDB);
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
    // Dentro de la clase TerminalCliente

    /**
     * Solicita al servidor modificar una alerta existente.
     *
     * @param idAlertaDB El ID de la alerta a modificar.
     * @param nuevoPrecio El nuevo precio umbral.
     * @param nuevaCondicion La nueva condición.
     * @return Mensaje de confirmación del servidor.
     * @throws Exception Si hay un error.
     */
    public String modificarAlerta(int idAlertaDB, double nuevoPrecio, String nuevaCondicion) throws Exception {
        try {
            // 2. PRIMER INTENTO
            return servicio.modificarAlerta(idUsuario, idAlertaDB, nuevoPrecio, nuevaCondicion);

        } catch (RemoteException e) {
            // 3. SI FALLA, RECONECTAR Y REINTENTAR
            System.err.println("Se perdió la conexión. Intentando reconectar...");

            conectarConFailover(); // Llama al método de reconexión

            System.out.println("Reconexión exitosa. Reintentando la operación...");

            // SEGUNDO INTENTO
            return servicio.modificarAlerta(idUsuario, idAlertaDB, nuevoPrecio, nuevaCondicion);
        }
    }
}