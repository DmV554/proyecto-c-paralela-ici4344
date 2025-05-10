package client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

import common.InterfazDeServer;
import common.Persona;

public class Client {
    private InterfazDeServer server;

    public Client() {}

    public void startClient() throws RemoteException, NotBoundException {
        // conectar al registro RMI en el puerto 1099
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        server = (InterfazDeServer) registry.lookup("server");  // buscar el objeto remoto
    }

    public void mostrarPersonas() throws RemoteException {
        // obtener lista de personas del servidor
        ArrayList<Persona> personas = server.getPersona();

        // indica si la lista está vacía
        if (personas.isEmpty()) {
            System.out.println(">> No hay personas registradas en el servidor.");
        } else {
            // si hay personas, las imprime
            System.out.println(">> Lista de Personas:");
            for (Persona persona : personas) {
                System.out.println("   - Nombre: " + persona.getNombre() + ", Edad: " + persona.getEdad());
            }
        }
    }

    public void crearPersona(String nombre, int edad) throws RemoteException {
        // para crear una nueva persona en el servidor
        server.agregarPersona(nombre, edad);
    }

    public void limpiarPersonas() throws RemoteException {
        // llama al método del servidor para limpiar la lista
        server.limpiarPersonas();
    }
}