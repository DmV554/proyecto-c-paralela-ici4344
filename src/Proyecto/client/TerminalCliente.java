package client;

import common.InterfazServicioCripto;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class TerminalCliente {

    public static void main(String[] args) {
        String host = (args.length < 1) ? "localhost" : args[0];
        // Definimos un ID de usuario simple para este cliente.
        // En una aplicación real, esto podría venir de un login o configuración.
        String idUsuario = "cliente001";

        try {
            // 1. Conectarse al RMI Registry
            Registry registry = LocateRegistry.getRegistry(host, 1099); // Usar el mismo puerto que el servidor

            // 2. Buscar el servicio remoto en el registry
            // Usar el mismo nombre con el que se registró el servicio en el servidor
            String serviceName = "ServidorCriptoMonitor";
            InterfazServicioCripto servicio = (InterfazServicioCripto) registry.lookup(serviceName);

            System.out.println("Conectado al " + serviceName + " como usuario: " + idUsuario);
            System.out.println("Escribe 'ayuda' para ver los comandos disponibles.");

            Scanner scanner = new Scanner(System.in);
            String lineaEntrada;

            // Bucle principal para leer comandos del usuario
            while (true) {
                System.out.print("\n" + idUsuario + "@cripto> ");
                lineaEntrada = scanner.nextLine().trim();

                if (lineaEntrada.equalsIgnoreCase("salir")) {
                    System.out.println("Desconectando...");
                    break;
                }

                if (lineaEntrada.isEmpty()) {
                    continue;
                }

                procesarComando(lineaEntrada, idUsuario, servicio);
            }
            scanner.close();

        } catch (Exception e) {
            System.err.println("Excepción en el cliente RMI: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void procesarComando(String lineaEntrada, String idUsuario, InterfazServicioCripto servicio) {
        String[] partes = lineaEntrada.split("\\s+");
        String comando = partes[0].toLowerCase();

        try {
            switch (comando) {
                case "ayuda":
                    mostrarAyuda();
                    break;

                case "precio": // Ejemplo: precio BTC
                    if (partes.length == 2) {
                        String cripto = partes[1].toUpperCase();
                        double precio = servicio.obtenerPrecioActual(cripto);
                        if (precio >= 0) {
                            System.out.printf("Precio actual de %s: %.2f USD\n", cripto, precio);
                        } else {
                            System.out.println("No se pudo obtener el precio para " + cripto + " (puede que no exista o no esté monitoreada por el servidor).");
                        }
                    } else {
                        System.out.println("Uso: precio <SIMBOLO_CRIPTO>");
                    }
                    break;

                case "alerta": // Ejemplo: alerta BTC > 60000  o  alerta ETH < 2800
                    if (partes.length == 4) {
                        String cripto = partes[1].toUpperCase();
                        String operador = partes[2];
                        double umbral = Double.parseDouble(partes[3]);
                        String tipoCondicion;

                        if (">".equals(operador)) {
                            tipoCondicion = "MAYOR_QUE";
                        } else if ("<".equals(operador)) {
                            tipoCondicion = "MENOR_QUE";
                        } else {
                            System.out.println("Operador de condición inválido. Usar '>' o '<'.");
                            return;
                        }
                        String respuesta = servicio.establecerAlerta(idUsuario, cripto, umbral, tipoCondicion);
                        System.out.println("Respuesta del servidor: " + respuesta);
                    } else {
                        System.out.println("Uso: alerta <SIMBOLO_CRIPTO> <OPERADOR: > o < > <PRECIO_UMBRAL>");
                    }
                    break;

                case "ver_alertas":
                    List<String> alertas = servicio.obtenerAlertasUsuario(idUsuario);
                    if (alertas.isEmpty()) {
                        System.out.println("No tienes alertas configuradas.");
                    } else {
                        System.out.println("Tus alertas configuradas:");
                        for (String alertaDesc : alertas) {
                            System.out.println("- " + alertaDesc);
                        }
                    }
                    break;

                case "ver_precios": // Muestra todos los precios que el servidor está cacheando
                    Map<String, Double> precios = servicio.obtenerPreciosMonitoreados(idUsuario); // idUsuario puede no ser usado por el servidor aún
                    if (precios.isEmpty()) {
                        System.out.println("No hay precios disponibles del servidor en este momento.");
                    } else {
                        System.out.println("Precios actuales monitoreados por el servidor (USD):");
                        precios.forEach((cripto, val) -> System.out.printf("- %s: %.2f\n", cripto, val));
                    }
                    break;

                default:
                    System.out.println("Comando desconocido. Escribe 'ayuda' para ver la lista de comandos.");
                    break;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: El valor del precio umbral debe ser un número válido.");
        } catch (Exception e) {
            System.err.println("Error al procesar el comando o comunicarse con el servidor: " + e.getMessage());
            // e.printStackTrace(); // Descomentar para ver el stack trace completo durante el desarrollo
        }
    }

    private static void mostrarAyuda() {
        System.out.println("\nComandos disponibles:");
        System.out.println("  precio <SIMBOLO_CRIPTO>          - Obtener el precio actual de una criptomoneda (ej. precio BTC).");
        System.out.println("  alerta <SIMBOLO_CRIPTO> > <VALOR> - Crear alerta si el precio supera VALOR (ej. alerta ETH > 3000).");
        System.out.println("  alerta <SIMBOLO_CRIPTO> < <VALOR> - Crear alerta si el precio baja de VALOR (ej. alerta ADA < 0.9).");
        System.out.println("  ver_alertas                      - Mostrar tus alertas configuradas.");
        System.out.println("  ver_precios                      - Mostrar precios de todas las monedas monitoreadas por el servidor.");
        System.out.println("  ayuda                            - Mostrar esta ayuda.");
        System.out.println("  salir                            - Salir de la aplicación cliente.");
    }
}