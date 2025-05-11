package common;

import java.io.Serializable;
import java.util.Objects;
import java.util.Date;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public class Cripto implements Serializable {
    private static final long serialVersionUID = 4L; // Incrementar si la estructura cambia

    private final String simbolo;         // Ej: "BTC", "ETH" (Identificador y nombre para mostrar)
    private double precioUSD;
    private long ultimaActualizacionTimestamp; // Momento en que se actualizó/creó este objeto Cripto

    // Constructor principal
    public Cripto(String simbolo, double precioUSD) {
        this.simbolo = Objects.requireNonNull(simbolo, "El símbolo no puede ser nulo").toUpperCase();
        this.precioUSD = precioUSD;
        this.ultimaActualizacionTimestamp = System.currentTimeMillis(); // Se establece al crear o actualizar
    }

    // Getters
    public String getSimbolo() {
        return simbolo;
    }

    public double getPrecioUSD() {
        return precioUSD;
    }

    public long getUltimaActualizacionTimestamp() {
        return ultimaActualizacionTimestamp;
    }

    // Setter para actualizar el precio y el timestamp
    // Se usaría cuando CoinGeckoService actualiza un objeto Cripto existente
    // o cuando se crea uno nuevo con datos frescos.
    public void setPrecioUSD(double precioUSD) {
        this.precioUSD = precioUSD;
        this.ultimaActualizacionTimestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        // Usamos el símbolo como nombre descriptivo aquí
        return String.format("%s: $%.2f USD (Actualizado: %tF %<tT)",
                simbolo, precioUSD, new Date(ultimaActualizacionTimestamp));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cripto cripto = (Cripto) o;
        return simbolo.equals(cripto.simbolo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(simbolo);
    }
}