package client;
import java.util.Scanner;

/**
 * Clase principal del cliente que ofrece un menú interactivo
 * para el sistema de monitoreo de criptomonedas.
 */
public class RunCliente {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_BOLD = "\u001B[1m";

    private static final Scanner scanner = new Scanner(System.in);
    private static TerminalCliente controlador;
    private static String idUsuario = "cliente001"; // ID por defecto

    public static void main(String[] args) {
        // Información de conexión
        String host = (args.length < 1) ? "localhost" : args[0];

        imprimirEncabezado();

        try {
            // Establecer conexión con el servidor
            conectarServidor(host);

            // Bucle principal del menú
            boolean ejecutar = true;
            while (ejecutar) {
                mostrarMenu();
                int opcion = obtenerOpcion();

                ejecutar = procesarOpcion(opcion);
            }

            // Cerrar el scanner al finalizar
            scanner.close();

        } catch (Exception e) {
            System.err.println(ANSI_RED + "Error de conexión: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        }
    }

    /**
     * Establece conexión con el servidor RMI
     */
    private static void conectarServidor(String host) throws Exception {
        System.out.println(ANSI_YELLOW + "Conectando al servidor en " + host + "..." + ANSI_RESET);

        // Inicializa el controlador y se conecta al servidor
        controlador = new TerminalCliente();
        boolean conectado = controlador.conectar(host, idUsuario);

        if (conectado) {
            System.out.println(ANSI_GREEN + "✓ Conexión establecida con éxito" + ANSI_RESET);
        } else {
            throw new Exception("No se pudo establecer conexión con el servidor");
        }
    }

    /**
     * Muestra el encabezado de la aplicación
     */
    private static void imprimirEncabezado() {
        System.out.println(ANSI_CYAN + ANSI_BOLD + "╔═══════════════════════════════════════════════╗");
        System.out.println("║     MONITOR DE CRIPTOMONEDAS EN TIEMPO REAL     ║");
        System.out.println("╚═══════════════════════════════════════════════╝" + ANSI_RESET);
        System.out.println("Desarrollado con Java RMI - v1.0\n");
    }

    /**
     * Muestra el menú principal de opciones
     */
    private static void mostrarMenu() {
        // Usaremos los mismos colores y formato que tenías, adaptados al nuevo menú.
        System.out.println(ANSI_CYAN + "\n╔═══════════════ MENÚ PRINCIPAL ════════════════╗" + ANSI_RESET);
        System.out.println("  1. Ver precios de criptomonedas (general)");
        System.out.println("  2. Ver precios de criptomonedas (buscadas anteriormente)");
        System.out.println("  3. Consultar precio específico");
        System.out.println("  4. Configurar nueva alerta de precio");
        System.out.println("  5. Ver mis alertas configuradas");
        System.out.println("  6. Eliminar alerta configurada");
        System.out.println("  7. Cambiar ID de usuario [actual: " + idUsuario + "]");
        System.out.println("  8. Ayuda");
        System.out.println("  0. Salir");
        System.out.println(ANSI_CYAN + "╚═══════════════════════════════════════════════╝" + ANSI_RESET);
        System.out.print("Seleccione una opción: ");
    }

    /**
     * Obtiene y valida la opción ingresada por el usuario
     */
    private static int obtenerOpcion() {
        int opcion = -1;
        try {
            String entrada = scanner.nextLine().trim();
            if (!entrada.isEmpty()) {
                opcion = Integer.parseInt(entrada);
            }
        } catch (NumberFormatException e) {
            // No hacer nada, la opción ya está inicializada en -1
        }
        return opcion;
    }

    /**
     * Procesa la opción seleccionada por el usuario
     * @return false si se debe salir de la aplicación, true en caso contrario
     */
    private static boolean procesarOpcion(int opcion) {
        switch (opcion) {
            case 0: // Salir
                System.out.println(ANSI_YELLOW + "Cerrando aplicación..." + ANSI_RESET);
                return false;

            case 1: // Ver precios de criptomonedas (general) - NO IMPLEMENTADA AÚN
                System.out.println(ANSI_YELLOW + "[INFO] Opción 1: 'Ver precios de criptomonedas (general)' aún no implementada." + ANSI_RESET);
                // Aquí iría la llamada al método cuando esté implementado.
                // Ejemplo: verPreciosGeneral();
                break;

            case 2: // Ver precios de criptomonedas (buscadas anteriormente) -> Tu antigua opción 1
                verPreciosActuales(); // Esta era tu función para la opción 1 original
                break;

            case 3: // Consultar precio específico -> Tu antigua opción 2
                consultarPrecioEspecifico();
                break;

            case 4: // Configurar alerta -> Tu antigua opción 3
                configurarAlerta();
                break;

            case 5: // Ver alertas -> Tu antigua opción 4
                verAlertasConfiguradas();
                break;

            case 6: // Eliminar alerta -> Tu antigua opción 5 (la que implementamos recientemente)
                eliminarAlertaUsuario();
                break;

            case 7: // Cambiar ID de usuario -> Tu antigua opción 6 (o 5 antes de eliminar alerta)
                cambiarIdUsuario();
                break;

            case 8: // Ayuda -> Tu antigua opción 7 (o 6 antes de eliminar alerta)
                mostrarAyuda();
                break;

            default:
                System.out.println(ANSI_RED + "Opción no válida. Intente nuevamente." + ANSI_RESET);
                break;
        }

        // Pausa antes de volver al menú
        System.out.print("\nPresione ENTER para continuar...");
        scanner.nextLine();

        return true;
    }

    /**
     * Muestra los precios actuales de todas las criptomonedas
     */
    private static void verPreciosActuales() {
        System.out.println(ANSI_CYAN + "\n[PRECIOS ACTUALES DEL MERCADO]" + ANSI_RESET);

        try {
            String resultado = controlador.obtenerPreciosMonitoreados();
            System.out.println(resultado);
        } catch (Exception e) {
            System.out.println(ANSI_RED + "Error al obtener precios: " + e.getMessage() + ANSI_RESET);
        }
    }

    /**
     * Consulta el precio de una criptomoneda específica
     */
    private static void consultarPrecioEspecifico() {
        System.out.println(ANSI_CYAN + "\n[CONSULTA DE PRECIO ESPECÍFICO]" + ANSI_RESET);
        System.out.print("Ingrese el símbolo de la criptomoneda (ej. BTC): ");

        String cripto = scanner.nextLine().trim().toUpperCase();
        if (cripto.isEmpty()) {
            System.out.println(ANSI_RED + "Debe ingresar un símbolo válido." + ANSI_RESET);
            return;
        }

        try {
            String resultado = controlador.obtenerPrecioEspecifico(cripto);
            System.out.println(resultado);
        } catch (Exception e) {
            System.out.println(ANSI_RED + "Error al consultar precio: " + e.getMessage() + ANSI_RESET);
        }
    }

    /**
     * Configura una nueva alerta de precio
     */
    private static void configurarAlerta() {
        System.out.println(ANSI_CYAN + "\n[CONFIGURACIÓN DE ALERTA]" + ANSI_RESET);

        try {
            // Solicitar datos para la alerta
            System.out.print("Ingrese el símbolo de la criptomoneda (ej. BTC): ");
            String cripto = scanner.nextLine().trim().toUpperCase();

            if (cripto.isEmpty()) {
                System.out.println(ANSI_RED + "Debe ingresar un símbolo válido." + ANSI_RESET);
                return;
            }

            System.out.print("Seleccione condición (1: > Mayor que, 2: < Menor que): ");
            int condicionInt = obtenerOpcion();

            String operador;
            String tipoCondicion;

            if (condicionInt == 1) {
                operador = ">";
                tipoCondicion = "MAYOR_QUE";
            } else if (condicionInt == 2) {
                operador = "<";
                tipoCondicion = "MENOR_QUE";
            } else {
                System.out.println(ANSI_RED + "Condición no válida. Debe ser 1 o 2." + ANSI_RESET);
                return;
            }

            System.out.print("Ingrese precio umbral (ej. 50000): ");
            String precioStr = scanner.nextLine().trim();

            double precio;
            try {
                precio = Double.parseDouble(precioStr);
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "El precio debe ser un número válido." + ANSI_RESET);
                return;
            }

            // Enviar datos de la alerta al controlador
            String resultado = controlador.establecerAlerta(cripto, precio, tipoCondicion);
            System.out.println(ANSI_GREEN + resultado + ANSI_RESET);

        } catch (Exception e) {
            System.out.println(ANSI_RED + "Error al configurar alerta: " + e.getMessage() + ANSI_RESET);
        }
    }

    /**
     * Muestra las alertas configuradas por el usuario
     */
    private static void verAlertasConfiguradas() {
        System.out.println(ANSI_CYAN + "\n[MIS ALERTAS CONFIGURADAS]" + ANSI_RESET);

        try {
            String resultado = controlador.obtenerAlertasUsuario();
            System.out.println(resultado);
        } catch (Exception e) {
            System.out.println(ANSI_RED + "Error al obtener alertas: " + e.getMessage() + ANSI_RESET);
        }
    }

    /**
     * Permite al usuario eliminar una de sus alertas configuradas.
     */
    private static void eliminarAlertaUsuario() {
        System.out.println(ANSI_CYAN + "\n[ELIMINAR ALERTA CONFIGURADA]" + ANSI_RESET);

        try {
            // Primero, obtenemos las alertas para verificar si existen
            String alertasActualesStr = controlador.obtenerAlertasUsuario();

            // Imprimimos el resultado (sea la lista de alertas o el mensaje de "no hay alertas")
            System.out.println(alertasActualesStr);

            // Verificamos si el mensaje indica que no hay alertas
            // Esta comparación es sensible al texto exacto devuelto por TerminalCliente.java
            if ("No tienes alertas configuradas.".equals(alertasActualesStr.trim())) {
                // Si no hay alertas, no pedimos ID y terminamos aquí.
                return;
            }

            // Si llegamos aquí, significa que hay alertas y se mostraron.
            // Ahora pedimos el ID.
            System.out.print("Ingrese el ID de la alerta que desea eliminar (ej. 123): ");
            String idStr = scanner.nextLine().trim();
            int idAlertaAEliminar;

            try {
                idAlertaAEliminar = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "El ID de la alerta debe ser un número válido." + ANSI_RESET);
                return;
            }

            if (idAlertaAEliminar <= 0) {
                System.out.println(ANSI_RED + "El ID de la alerta debe ser un número positivo." + ANSI_RESET);
                return;
            }

            // Llamar al controlador para eliminar la alerta
            String resultadoEliminacion = controlador.eliminarAlerta(idAlertaAEliminar);
            System.out.println(ANSI_GREEN + resultadoEliminacion + ANSI_RESET);

        } catch (Exception e) {
            System.out.println(ANSI_RED + "Error al procesar la eliminación de la alerta: " + e.getMessage() + ANSI_RESET);
        }
    }

    /**
     * Cambia el ID de usuario
     */
    private static void cambiarIdUsuario() {
        System.out.println(ANSI_CYAN + "\n[CAMBIO DE ID DE USUARIO]" + ANSI_RESET);
        System.out.println("ID actual: " + idUsuario);
        System.out.print("Ingrese nuevo ID de usuario: ");

        String nuevoId = scanner.nextLine().trim();
        if (nuevoId.isEmpty()) {
            System.out.println(ANSI_RED + "El ID no puede estar vacío." + ANSI_RESET);
            return;
        }

        idUsuario = nuevoId;
        controlador.actualizarIdUsuario(idUsuario);
        System.out.println(ANSI_GREEN + "ID de usuario actualizado correctamente a: " + idUsuario + ANSI_RESET);
    }

    /**
     * Muestra información de ayuda
     */
    private static void mostrarAyuda() {
        System.out.println(ANSI_CYAN + "\n[AYUDA DEL SISTEMA]" + ANSI_RESET);
        System.out.println("Este sistema le permite monitorear precios de criptomonedas y configurar alertas");
        System.out.println("cuando se alcancen ciertos umbrales de precio.\n");

        System.out.println(ANSI_BOLD + "Funcionalidades disponibles:" + ANSI_RESET);
        System.out.println("• Ver precios actuales: Muestra todos los precios de criptomonedas disponibles");
        System.out.println("• Consultar precio específico: Obtiene el precio actual de una criptomoneda");
        System.out.println("• Configurar alerta: Establece una notificación cuando una criptomoneda");
        System.out.println("  alcance un precio mayor o menor que un umbral definido");
        System.out.println("• Ver alertas: Muestra todas sus alertas configuradas");
        System.out.println("• Cambiar ID: Permite cambiar su identificador en el sistema");
        System.out.println("\nSi tiene problemas, contacte al administrador del sistema.");
    }
}