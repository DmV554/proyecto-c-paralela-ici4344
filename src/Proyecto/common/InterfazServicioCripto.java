package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
/*
* ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣠⡤⠤⣤⢼⣩⡥⡤⣤⠤⢄⡀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠠⣔⠊⡱⠁⢀⠎⠀⠀⠀⠑⡄⠀⠉⢆⡈⠢⢄⡀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⡔⣽⣁⣀⣸⠀⠀⠀⠀⡰⠣⣀⣀⣈⡎⠉⠣⡀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⡜⠀⢀⠀⠀⠀⢇⡠⠔⠋⠀⠀⠀⠀⠀⠀⡀⠀⢣⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⡇⠀⠑⠀⠀⡠⠀⠁⠀⣄⠀⠀⢠⠄⠀⠈⠊⠀⢸⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⢣⠀⠀⣄⠀⠈⠀⠀⠀⠉⠀⠀⠀⠁⠀⣄⠀⠀⡜⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠈⢆⠀⠉⠀⠀⠐⠅⠀⠀⠀⠪⠂⠀⠀⠉⠀⡰⠁⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠈⢆⠀⠸⠇⠀⠀⠀⣶⠀⠀⠀⠸⠃⠀⡰⠁⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⢢⡀⠀⢠⡆⠀⠀⠀⢐⠄⠀⠀⡜⠁⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠑⢄⠀⠀⠀⣦⠀⠀⠀⢠⠊⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠑⠦⣀⣀⣀⡠⠒⠁⠀⠀⠀⠀⠀⠀⠀⠀
* */

public interface InterfazServicioCripto extends Remote {
    /**
     * Permite a un usuario establecer una nueva alerta de precio para una criptomoneda.
     *
     * @param idUsuario      Identificador único del usuario que establece la alerta.
     * @param criptomoneda   Símbolo de la criptomoneda (ej. "BTC", "ETH").
     * @param precioUmbral   El precio que, al ser alcanzado, disparará la alerta.
     * @param tipoCondicion  Indica si la alerta se dispara cuando el precio es "MAYOR_QUE" o "MENOR_QUE" el umbral.
     * @return Un mensaje de confirmación o error.
     * @throws RemoteException Si ocurre un error durante la comunicación RMI.
     */
    String establecerAlerta(String idUsuario, String criptomoneda, double precioUmbral, String tipoCondicion) throws RemoteException;

    /**
     * Recupera la lista de alertas activas para un usuario específico.
     * Por ahora, cada alerta se representará como un String descriptivo.
     *
     * @param idUsuario Identificador único del usuario.
     * @return Una lista de Strings, donde cada String describe una alerta (ej. "BTC > 70000 USD").
     * @throws RemoteException Si ocurre un error durante la comunicación RMI.
     */
    List <String> obtenerAlertasUsuario(String idUsuario) throws RemoteException;

    /**
     * Obtiene el precio actual de una criptomoneda específica.
     *
     * @param criptomoneda Símbolo de la criptomoneda (ej. "BTC", "ETH").
     * @return El precio actual de la criptomoneda en USD, o un valor negativo/especial si no se encuentra.
     * @throws RemoteException Si ocurre un error durante la comunicación RMI.
     */
    double obtenerPrecioActual(String criptomoneda) throws RemoteException;

    /**
     * Obtiene un mapa con los precios actuales de un conjunto de criptomonedas monitoreadas
     * (generalmente aquellas en caché debido a búsquedas recientes o alertas activas).
     *
     * @param idUsuario (Opcional por ahora, podría usarse en el futuro para personalizar qué monedas se devuelven
     * o simplemente para logging).
     * @return Un Map donde la clave es el símbolo de la criptomoneda (String) y el valor es su precio actual (Double).
     * @throws RemoteException Si ocurre un error durante la comunicación RMI.
     */
    Map<String, Double> obtenerPreciosMonitoreados(String idUsuario) throws RemoteException;

    /**
     * Elimina una alerta específica de un usuario.
     *
     * @param idUsuario Identificador único del usuario propietario de la alerta.
     * @param idAlertaDB El ID único de la alerta en la base de datos (obtenido de la tabla 'alertas').
     * @return Un mensaje de confirmación o error.
     * @throws RemoteException Si ocurre un error durante la comunicación RMI o en el servidor.
     */
    String eliminarAlerta(String idUsuario, int idAlertaDB) throws RemoteException;

    /**
     * Obtiene un mapa con los precios actuales de todas las criptomonedas base definidas en el servidor.
     * Esto puede implicar múltiples consultas a la API si no están en caché.
     *
     * @param idUsuario (Opcional, para logging o futuras personalizaciones).
     * @return Un Map donde la clave es el símbolo de la criptomoneda (String) y el valor es su precio actual (Double).
     * Puede contener valores negativos si algún precio no pudo ser obtenido.
     * @throws RemoteException Si ocurre un error durante la comunicación RMI.
     */
    Map<String, Double> obtenerPreciosDeTodasLasBases(String idUsuario) throws RemoteException; // NUEVO MÉTODO

    // Dentro de la interfaz InterfazServicioCripto

    /**
     * Modifica una alerta de precio existente para un usuario.
     *
     * @param idUsuario      Identificador del usuario propietario de la alerta.
     * @param idAlertaDB     El ID de la alerta que se desea modificar.
     * @param nuevoPrecio    El nuevo precio umbral para la alerta.
     * @param nuevaCondicion La nueva condición ("MAYOR_QUE" o "MENOR_QUE").
     * @return Un mensaje de confirmación o error.
     * @throws RemoteException Si ocurre un error durante la comunicación RMI.
     */
    String modificarAlerta(String idUsuario, int idAlertaDB, double nuevoPrecio, String nuevaCondicion) throws RemoteException;
}
