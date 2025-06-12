package server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class RunServerRespaldo {
    public static void main(String[] args) {
        try {
            // 1. Iniciar el RMI Registry
            // Se utilizará 1100 para el respaldo.
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1100);
                System.out.println("RMI Registry creado en el puerto 1100.");
            } catch (RemoteException e) {
                // Si ya está creado, simplemente lo obtenemos
                System.out.println("RMI Registry ya podría estar corriendo. Intentando obtenerlo...");
                registry = LocateRegistry.getRegistry(1100);
            }

            // 2. Instanciar nuestra implementación del servidor
            ServidorPreciosImpl cryptoService = new ServidorPreciosImpl();
            System.out.println("Instancia de ServidorPreciosImpl creada.");

            // 3. Registrar (bind) el objeto remoto en el RMI Registry
            // El cliente usará este nombre para buscar el servicio.
            String serviceName = "ServidorCriptoMonitorRespaldo";
            registry.rebind(serviceName, cryptoService);

            System.out.println("Servicio de RESPALDO '" + serviceName + "' registrado y listo en el puerto " + 1100 + ".");
            System.out.println("El servidor está esperando conexiones de clientes...");

        } catch (Exception e) {
            System.err.println("Excepción en el servidor RMI: " + e.toString());
            e.printStackTrace();
        }
    }
}