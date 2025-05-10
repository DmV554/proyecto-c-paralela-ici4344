package common;

import java.io.Serializable;

public class Persona implements Serializable {
    private static final long serialVersionUID = 1L; //controlar compatibiilidad de versiones
    private String nombre;
    private int edad;

    public Persona(String nombre, int edad) {
        setNombre(nombre);
        setEdad(edad);
    }

    private void setEdad(int edad) {
        this.edad = edad;
    }

    private void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return this.nombre;
    }

    public int getEdad() {
        return this.edad;
    }
}