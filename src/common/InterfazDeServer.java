package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface InterfazDeServer extends Remote {
    //  get lista de personas
    public ArrayList<Persona> getPersona() throws RemoteException;

    // agregrar persona a la lista
    public void agregarPersona(String nombre, int edad) throws RemoteException;

    // quitar personas de la lista cuando se presione 0 en el menú, al salir, si se vuelve a ejecutar el cliente
    //y se usa la opción de mostrar lista, esta estará limpia (vacía)
    public void limpiarPersonas() throws RemoteException;

}