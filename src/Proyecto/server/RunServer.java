package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;


public class RunServer {
    public static void main(String[] args) {
        try {
            // 1. Iniciar el RMI Registry
            // 1099 es el estándar.
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
                System.out.println("RMI Registry creado en el puerto 1099.");
            } catch (RemoteException e) {
                // Si ya está creado, simplemente lo obtenemos
                System.out.println("RMI Registry ya podría estar corriendo. Intentando obtenerlo...");
                registry = LocateRegistry.getRegistry(1099);
            }

            // 2. Instanciar nuestra implementación del servidor
            ServidorPreciosImpl cryptoService = new ServidorPreciosImpl();
            System.out.println("Instancia de ServidorPreciosImpl creada.");

            // 3. Registrar (bind) el objeto remoto en el RMI Registry
            // El cliente usará este nombre para buscar el servicio
            String serviceName = "ServidorCriptoMonitor"; // Nuevo nombre para el servicio
            registry.rebind(serviceName, cryptoService);

            System.out.println("Servicio '" + serviceName + "' registrado y listo en el puerto 1099.");
            System.out.println("El servidor está esperando conexiones de clientes...");

        } catch (Exception e) {
            System.err.println("Excepción en el servidor RMI: " + e.toString());
            e.printStackTrace();
        }
    }
}