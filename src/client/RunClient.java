package client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.InputMismatchException; // Importar para manejo de errores de entrada
import java.util.Scanner;

public class RunClient {
    public static void main(String[] args) {
        Client client = new Client();
        Scanner scanner = new Scanner(System.in);
        boolean salir = false;

        try {
            client.startClient(); // conectar al servidor
            System.out.println("Cliente conectado al servidor.");

            while (!salir) {
                // mostrar menú de opciones
                System.out.println("\n--- Menú Cliente RMI ---");
                System.out.println("1. Mostrar lista de personas");
                System.out.println("2. Agregar nueva persona");
                System.out.println("0. Salir");
                System.out.print("Seleccione una opción: ");

                int opcion = -1; // inicializa opción
                try {
                    opcion = scanner.nextInt(); // lee la opción que ingresa el usuario
                    scanner.nextLine(); // consumir el salto de línea pendiente después de nextInt()
                } catch (InputMismatchException e) {
                    System.out.println("Error: Por favor, ingrese un número válido.");
                    scanner.nextLine(); // consumir la entrada inválida
                    continue;
                }


                // casos según lo ingresado en menú de opciones
                switch (opcion) {
                    case 1:
                        System.out.println("\n--- Lista de Personas ---");
                        try {
                            // llamar al método para mostrarPersonas, también maneja lista vacía
                            client.mostrarPersonas();
                        } catch (RemoteException e) {
                            System.err.println("Error al obtener la lista del servidor: " + e.getMessage());

                        }
                        break;
                    case 2:
                        System.out.println("\n--- Agregar Nueva Persona ---");
                        try {
                            System.out.print("Ingrese el nombre: ");
                            String nombre = scanner.nextLine();

                            System.out.print("Ingrese la edad: ");
                            int edad = -1;
                            try {
                                edad = scanner.nextInt();
                                scanner.nextLine(); // consume salto de línea
                                if (edad < 0) { // valida la edad
                                    System.out.println("Edad inválida, no se agregará la persona.");
                                    continue;
                                }
                            } catch (InputMismatchException e) {
                                System.out.println("Error: La edad debe ser un número entero.");
                                scanner.nextLine(); // consume entrada inválida
                                continue; // vuelve al menú
                            }


                            client.crearPersona(nombre, edad); // crea persona en el servidor
                            System.out.println("Persona agregada exitosamente.");

                        } catch (RemoteException e) {
                            System.err.println("Error al agregar persona en el servidor: " + e.getMessage());
                        } catch (Exception e) { // maneja excepciones de entrada
                            System.err.println("Error en la entrada de datos: " + e.getMessage());
                            // si es necesario limpia el buffer
                            if (scanner.hasNextLine()) scanner.nextLine();
                        }
                        break;
                    case 0:
                        try {
                            // Llamar al método para limpiar todas las personas
                            client.limpiarPersonas();
                            System.out.println("Lista de personas limpiada exitosamente.");
                        } catch (RemoteException e) {
                            System.err.println("Error al limpiar la lista de personas: " + e.getMessage());
                        }

                        salir = true;
                        System.out.println("Desconectando cliente...");
                        break;
                    default:
                        System.out.println("Opción no válida. Intente de nuevo.");
                        break;
                }
            }

        } catch (RemoteException | NotBoundException e) {
            System.err.println("Error crítico de conexión con el servidor: " + e.getMessage());

        } finally {
            scanner.close(); // cerrar el Scanner al final
            System.out.println("Cliente terminado.");
        }
    }
}