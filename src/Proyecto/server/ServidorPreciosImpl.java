package server;

import common.InterfazServicioCripto;
import common.Cripto;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*; // Para Date, ArrayList, List, Map, Set, Optional, Timestamp
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServidorPreciosImpl extends UnicastRemoteObject implements InterfazServicioCripto {

    private final CoinGeckoService coinGeckoService;
    private static final String MONEDA_COTIZACION = "usd";
    private static final int INTERVALO_ACTUALIZACION_PRECIOS_SEGUNDOS = 60; // API tiene límites, 60s es más seguro
    private static final int INTERVALO_VERIFICACION_ALERTAS_SEGUNDOS = 15;
    private static final int DELAY_INICIAL_VERIFICACION_ALERTAS_SEGUNDOS = 5;
    private static final String USUARIO_POR_DEFECTO = "default_user";

    private final Map<String, Cripto> cacheCriptoData = new ConcurrentHashMap<>();

    private static class AlertaDefinicion {
        String idAlertaDB;
        String idUsuario;
        String criptomoneda;
        double precioUmbral;
        String tipoCondicion;
        boolean activa = true; // Esta 'activa' es del objeto, la BD tiene la suya

        public AlertaDefinicion(String idAlertaDB, String idUsuario, String criptomoneda, double precioUmbral, String tipoCondicion, boolean activa) {
            this.idAlertaDB = idAlertaDB;
            this.idUsuario = idUsuario;
            this.criptomoneda = criptomoneda.toUpperCase();
            this.precioUmbral = precioUmbral;
            this.tipoCondicion = tipoCondicion;
            this.activa = activa;
        }

        public AlertaDefinicion(String idUsuario, String criptomoneda, double precioUmbral, String tipoCondicion) {
            this(null, idUsuario, criptomoneda, precioUmbral, tipoCondicion, true);
        }


        @Override
        public String toString() {
            String simboloCondicion = "MAYOR_QUE".equals(tipoCondicion) ? ">" : "<";
            return String.format("%s %s %.2f (Activa: %b)", criptomoneda, simboloCondicion, precioUmbral, activa);
        }
    }


    public ServidorPreciosImpl() throws RemoteException {
        super();
        this.coinGeckoService = new CoinGeckoService();

        System.out.println("[ServidorPreciosImpl] Instanciado.");
        DatabaseManager.inicializarCriptomonedasBase();
        DatabaseManager.inicializarUsuarioPorDefecto(USUARIO_POR_DEFECTO);

        iniciarActualizadorDeCriptoDataDesdeAPI();
        iniciarVerificadorDeAlertas();
    }

    private Set<String> obtenerSimbolosCriptoConAlertasActivas() {
        Set<String> simbolos = new HashSet<>();
        String sql = "SELECT DISTINCT c.simbolo FROM alertas a " +
                "JOIN criptomonedas c ON a.id_cripto_fk = c.id_cripto " +
                "WHERE a.activa = TRUE";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                simbolos.add(rs.getString("simbolo").toUpperCase());
            }
        } catch (SQLException e) {
            System.err.println("[Servidor ERROR] Error al obtener símbolos de criptos con alertas activas: " + e.getMessage());
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
        return simbolos;
    }

    private void iniciarActualizadorDeCriptoDataDesdeAPI() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName("APIPriceUpdaterThread");
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            Set<String> simbolosParaActualizar = obtenerSimbolosCriptoConAlertasActivas();

            if (simbolosParaActualizar.isEmpty()) {
                System.out.println("[ServidorPreciosImpl API Updater] No hay criptomonedas con alertas activas para actualizar desde la API en este momento.");
                return;
            }
            try {
                System.out.println("[ServidorPreciosImpl API Updater] Actualizando datos desde CoinGecko para: " + simbolosParaActualizar);
                Map<String, Cripto> nuevosDatosCripto = coinGeckoService.fetchCriptoData(simbolosParaActualizar, MONEDA_COTIZACION);

                if (!nuevosDatosCripto.isEmpty()) {
                    actualizarCacheYGuardarHistorial(nuevosDatosCripto);
                } else {
                    System.out.println("[ServidorPreciosImpl API Updater] No se recibieron nuevos datos de CoinGecko para las criptomonedas con alertas activas.");
                }

            } catch (IOException e) {
                System.err.println("[ServidorPreciosImpl API Updater ERROR] No se pudo actualizar datos desde CoinGecko: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[ServidorPreciosImpl API Updater ERROR] Excepción inesperada: " + e.getMessage());
                e.printStackTrace();
            }
        }, 5, INTERVALO_ACTUALIZACION_PRECIOS_SEGUNDOS, TimeUnit.SECONDS); // Delay inicial de 5s
        System.out.println("Tarea de actualización de datos de criptomonedas (basada en alertas activas) desde API iniciada (cada " + INTERVALO_ACTUALIZACION_PRECIOS_SEGUNDOS + " segundos).");
    }

    private void actualizarCacheYGuardarHistorial(Map<String, Cripto> nuevosDatosCripto) {
        Connection conn = null;
        PreparedStatement pstmtHistorial = null;
        PreparedStatement pstmtGetCriptoId = null;
        ResultSet rsCriptoId = null;

        String sqlGetCriptoId = "SELECT id_cripto FROM criptomonedas WHERE simbolo = ?";
        String sqlInsertHistorial = "INSERT INTO historial_precios (id_cripto_fk, precio, moneda_cotizacion, timestamp_precio) VALUES (?, ?, ?, ?)";

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Iniciar transacción

            pstmtGetCriptoId = conn.prepareStatement(sqlGetCriptoId);
            pstmtHistorial = conn.prepareStatement(sqlInsertHistorial);

            for (Map.Entry<String, Cripto> entry : nuevosDatosCripto.entrySet()) {
                String simbolo = entry.getKey().toUpperCase();
                Cripto cripto = entry.getValue();

                // 1. Actualizar caché
                cacheCriptoData.put(simbolo, cripto);

                // 2. Obtener id_cripto_fk para el historial
                pstmtGetCriptoId.setString(1, simbolo);
                rsCriptoId = pstmtGetCriptoId.executeQuery();
                int idCriptoFk = -1;
                if (rsCriptoId.next()) {
                    idCriptoFk = rsCriptoId.getInt("id_cripto");
                }
                rsCriptoId.close();

                if (idCriptoFk != -1) {
                    // 3. Guardar en historial_precios
                    pstmtHistorial.setInt(1, idCriptoFk);
                    pstmtHistorial.setDouble(2, cripto.getPrecioUSD());
                    pstmtHistorial.setString(3, MONEDA_COTIZACION.toLowerCase());
                    pstmtHistorial.setLong(4, cripto.getUltimaActualizacionTimestamp());
                    pstmtHistorial.addBatch();
                } else {
                    System.err.println("[ServidorPreciosImpl ERROR] No se encontró id_cripto para el símbolo: " + simbolo + " al guardar historial.");
                }
            }
            pstmtHistorial.executeBatch();
            conn.commit();
            System.out.println("[ServidorPreciosImpl] Datos de Criptomonedas actualizados en caché y guardados en historial_precios.");
            cacheCriptoData.forEach((simbolo, cripto) ->
                    System.out.printf("  Cache: %s: %.2f (Actualizado: %tF %<tT)\n",
                            simbolo, cripto.getPrecioUSD(), new Date(cripto.getUltimaActualizacionTimestamp())));

        } catch (SQLException e) {
            System.err.println("[ServidorPreciosImpl ERROR] Error al actualizar caché y guardar historial: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[ServidorPreciosImpl ERROR] Error en rollback de historial: " + ex.getMessage());
                }
            }
        } finally {
            // rsCriptoId ya se cierra en el bucle
            DatabaseManager.close(conn, pstmtGetCriptoId);
            DatabaseManager.close(null, pstmtHistorial);
            DatabaseManager.close(conn, null, null); // Finalmente cierra la conexión
        }
    }

    private void iniciarVerificadorDeAlertas() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName("AlertVerifierThread");
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            // System.out.println("[ServidorPreciosImpl Alert Verifier] Verificando alertas...");
            List<AlertaDefinicion> alertasActivas = obtenerDefinicionesDeAlertasActivasDeDB();

            if (alertasActivas.isEmpty()){
                // System.out.println("[ServidorPreciosImpl Alert Verifier] No hay alertas activas en la BD para verificar.");
                return;
            }

            for (AlertaDefinicion alerta : alertasActivas) {
                Cripto criptoActual = cacheCriptoData.get(alerta.criptomoneda); // La clave es el símbolo en mayúsculas
                if (criptoActual != null) {
                    double precioActual = criptoActual.getPrecioUSD();
                    boolean condicionCumplida = false;
                    if ("MAYOR_QUE".equals(alerta.tipoCondicion) && precioActual > alerta.precioUmbral) {
                        condicionCumplida = true;
                    } else if ("MENOR_QUE".equals(alerta.tipoCondicion) && precioActual < alerta.precioUmbral) {
                        condicionCumplida = true;
                    }

                    if (condicionCumplida) {
                        System.out.printf("[ALERTA DISPARADA] Usuario: %s, Alerta DB ID: %s, Detalles: %s, Precio Actual de %s: %.2f %s (Timestamp del precio: %tF %<tT)\n",
                                alerta.idUsuario, alerta.idAlertaDB, alerta.toString(), criptoActual.getSimbolo(), precioActual, MONEDA_COTIZACION.toUpperCase(), new Date(criptoActual.getUltimaActualizacionTimestamp()));
                    }
                } else {
                    // System.out.println("[ServidorPreciosImpl Alert Verifier] No hay datos en caché para " + alerta.criptomoneda + " para verificar alerta ID " + alerta.idAlertaDB);
                }
            }
        }, DELAY_INICIAL_VERIFICACION_ALERTAS_SEGUNDOS, INTERVALO_VERIFICACION_ALERTAS_SEGUNDOS, TimeUnit.SECONDS);
        System.out.println("Tarea de verificación de alertas (desde BD) iniciada (cada " + INTERVALO_VERIFICACION_ALERTAS_SEGUNDOS + " segundos).");
    }

    private List<AlertaDefinicion> obtenerDefinicionesDeAlertasActivasDeDB() {
        List<AlertaDefinicion> alertas = new ArrayList<>();
        String sql = "SELECT a.id_alerta, u.nombre_usuario, c.simbolo, a.precio_umbral, a.tipo_condicion, a.activa " +
                "FROM alertas a " +
                "JOIN usuarios u ON a.id_usuario_fk = u.id_usuario " +
                "JOIN criptomonedas c ON a.id_cripto_fk = c.id_cripto " +
                "WHERE a.activa = TRUE";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                alertas.add(new AlertaDefinicion(
                        rs.getString("id_alerta"),
                        rs.getString("nombre_usuario"),
                        rs.getString("simbolo").toUpperCase(),
                        rs.getDouble("precio_umbral"),
                        rs.getString("tipo_condicion"),
                        rs.getBoolean("activa")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[Servidor ERROR] Error al obtener alertas activas de la BD: " + e.getMessage());
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
        return alertas;
    }

    //OPCIONAL -> Para q no se dispare mil veces
    private void desactivarAlertaEnDB(String idAlertaDB) {
        if (idAlertaDB == null) return;
        String sql = "UPDATE alertas SET activa = FALSE WHERE id_alerta = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, Integer.parseInt(idAlertaDB));
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("[ServidorPreciosImpl] Alerta ID " + idAlertaDB + " desactivada en la BD.");
            }
        } catch (SQLException e) {
            System.err.println("[Servidor ERROR] Error al desactivar alerta ID " + idAlertaDB + " en la BD: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("[Servidor ERROR] ID de alerta inválido para desactivar: " + idAlertaDB);
        }
        finally {
            DatabaseManager.close(conn, pstmt);
        }
    }

    @Override
    public String establecerAlerta(String nombreUsuario, String criptomoneda, double precioUmbral, String tipoCondicion) throws RemoteException {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            nombreUsuario = USUARIO_POR_DEFECTO; // Usar default si no se provee
            System.out.println("[ServidorPreciosImpl] Nombre de usuario no provisto para alerta, usando por defecto: " + USUARIO_POR_DEFECTO);
        }
        if (criptomoneda == null || criptomoneda.trim().isEmpty() ||
                (!tipoCondicion.equalsIgnoreCase("MAYOR_QUE") && !tipoCondicion.equalsIgnoreCase("MENOR_QUE"))) {
            throw new RemoteException("Datos de alerta inválidos: Criptomoneda y tipo de condición ('MAYOR_QUE' o 'MENOR_QUE') son obligatorios.");
        }
        String criptoUpper = criptomoneda.toUpperCase();
        String tipoCondicionUpper = tipoCondicion.toUpperCase();

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // 1. Obtener/Crear id_usuario
            int idUsuarioFk = -1;
            String sqlGetUsuario = "SELECT id_usuario FROM usuarios WHERE nombre_usuario = ?";
            pstmt = conn.prepareStatement(sqlGetUsuario);
            pstmt.setString(1, nombreUsuario);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                idUsuarioFk = rs.getInt("id_usuario");
            } else { // Si el usuario no existe, lo creamos (esto también podría hacerse al inicio o manejarse de otra forma)
                rs.close();
                pstmt.close();
                String sqlInsertUsuario = "INSERT INTO usuarios (nombre_usuario) VALUES (?)";
                pstmt = conn.prepareStatement(sqlInsertUsuario, Statement.RETURN_GENERATED_KEYS);
                pstmt.setString(1, nombreUsuario);
                pstmt.executeUpdate();
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    idUsuarioFk = rs.getInt(1);
                    System.out.println("[ServidorPreciosImpl] Usuario '" + nombreUsuario + "' creado con ID: " + idUsuarioFk);
                } else {
                    conn.rollback();
                    throw new RemoteException("No se pudo crear el usuario '" + nombreUsuario + "' en la base de datos.");
                }
            }
            rs.close();
            pstmt.close();

            // 2. Obtener id_cripto
            int idCriptoFk = -1;
            String sqlGetCripto = "SELECT id_cripto FROM criptomonedas WHERE simbolo = ?";
            pstmt = conn.prepareStatement(sqlGetCripto);
            pstmt.setString(1, criptoUpper);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                idCriptoFk = rs.getInt("id_cripto");
            } else { // Si la cripto no existe en nuestra BD, no podemos crear la alerta
                conn.rollback();
                throw new RemoteException("Criptomoneda '" + criptoUpper + "' no encontrada en la base de datos. Asegúrate que esté en la lista de criptomonedas monitoreadas/conocidas.");
            }
            rs.close();
            pstmt.close();

            // 3. Insertar la alerta
            String sqlInsertAlerta = "INSERT INTO alertas (id_usuario_fk, id_cripto_fk, precio_umbral, tipo_condicion, activa) VALUES (?, ?, ?, ?, TRUE)";
            pstmt = conn.prepareStatement(sqlInsertAlerta);
            pstmt.setInt(1, idUsuarioFk);
            pstmt.setInt(2, idCriptoFk);
            pstmt.setDouble(3, precioUmbral);
            pstmt.setString(4, tipoCondicionUpper);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                conn.commit();
                AlertaDefinicion nuevaAlerta = new AlertaDefinicion(nombreUsuario, criptoUpper, precioUmbral, tipoCondicionUpper); // Para el mensaje de retorno
                String mensaje = "Alerta para " + nuevaAlerta.toString().replace("(Activa: true)","") + " establecida correctamente para el usuario " + nombreUsuario + ".";
                System.out.printf("[ServidorPreciosImpl] %s\n", mensaje);
                return mensaje;
            } else {
                conn.rollback();
                throw new RemoteException("No se pudo establecer la alerta en la base de datos.");
            }

        } catch (SQLException e) {
            System.err.println("[Servidor ERROR] SQLException al establecer alerta: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[Servidor ERROR] Error en rollback de alerta: " + ex.getMessage());
                }
            }
            throw new RemoteException("Error de base de datos al establecer alerta: " + e.getMessage());
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
    }

    @Override
    public List<String> obtenerAlertasUsuario(String nombreUsuario) throws RemoteException {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            nombreUsuario = USUARIO_POR_DEFECTO; // Usar default si no se provee
            System.out.println("[ServidorPreciosImpl] Nombre de usuario no provisto para obtener alertas, usando por defecto: " + USUARIO_POR_DEFECTO);
        }
        System.out.println("[ServidorPreciosImpl] Solicitud para obtener alertas del usuario: " + nombreUsuario);

        List<String> alertasString = new ArrayList<>();
        String sql = "SELECT c.simbolo, a.precio_umbral, a.tipo_condicion, a.activa " +
                "FROM alertas a " +
                "JOIN usuarios u ON a.id_usuario_fk = u.id_usuario " +
                "JOIN criptomonedas c ON a.id_cripto_fk = c.id_cripto " +
                "WHERE u.nombre_usuario = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, nombreUsuario);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                AlertaDefinicion ad = new AlertaDefinicion(
                        null, // idAlertaDB no es crucial aquí para el string de retorno
                        nombreUsuario,
                        rs.getString("simbolo").toUpperCase(),
                        rs.getDouble("precio_umbral"),
                        rs.getString("tipo_condicion"),
                        rs.getBoolean("activa")
                );
                alertasString.add(ad.toString());
            }
        } catch (SQLException e) {
            System.err.println("[Servidor ERROR] Error al obtener alertas para el usuario " + nombreUsuario + ": " + e.getMessage());
            throw new RemoteException("Error de base de datos al obtener alertas: " + e.getMessage());
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
        return alertasString;
    }

    @Override
    public double obtenerPrecioActual(String criptomoneda) throws RemoteException {
        if (criptomoneda == null || criptomoneda.trim().isEmpty()) {
            throw new RemoteException("Nombre de criptomoneda no puede ser nulo o vacío.");
        }
        String criptoUpper = criptomoneda.toUpperCase();
        System.out.println("[ServidorPreciosImpl] Solicitud de precio para: " + criptoUpper);

        Cripto criptoEnCache = cacheCriptoData.get(criptoUpper);
        if (criptoEnCache != null && (System.currentTimeMillis() - criptoEnCache.getUltimaActualizacionTimestamp() < (INTERVALO_ACTUALIZACION_PRECIOS_SEGUNDOS * 1000 / 2))) { // Considerar caché válida por un tiempo
            System.out.printf("[ServidorPreciosImpl] Precio para %s (desde caché reciente): %.2f USD (Actualizado: %tF %<tT)\n",
                    criptoUpper, criptoEnCache.getPrecioUSD(), new Date(criptoEnCache.getUltimaActualizacionTimestamp()));
            return criptoEnCache.getPrecioUSD();
        }

        // Si no está en caché o es muy vieja, intentar obtenerlo de la API bajo demanda
        System.out.println("[ServidorPreciosImpl] Precio para " + criptoUpper + " no en caché o desactualizado. Intentando obtener desde API...");
        try {
            Cripto criptoObtenida = coinGeckoService.fetchSingleCriptoData(criptoUpper, MONEDA_COTIZACION);
            if (criptoObtenida != null) {
                // Actualizar caché y guardar en historial (como hace el actualizador principal)
                Map<String, Cripto> singleCriptoMap = new HashMap<>();
                singleCriptoMap.put(criptoUpper, criptoObtenida);
                actualizarCacheYGuardarHistorial(singleCriptoMap);

                System.out.printf("[ServidorPreciosImpl] Precio para %s (obtenido de API y cacheado/historial): %.2f USD (Actualizado: %tF %<tT)\n",
                        criptoUpper, criptoObtenida.getPrecioUSD(), new Date(criptoObtenida.getUltimaActualizacionTimestamp()));
                return criptoObtenida.getPrecioUSD();
            } else {
                System.out.println("[ServidorPreciosImpl] No se pudo obtener precio para " + criptoUpper + " desde la API. Intentando último precio conocido de BD.");
                return obtenerUltimoPrecioConocidoDeDB(criptoUpper);
            }
        } catch (IOException e) {
            System.err.println("[ServidorPreciosImpl ERROR] IOException al obtener precio individual para " + criptoUpper + ": " + e.getMessage());
            System.out.println("[ServidorPreciosImpl] Intentando último precio conocido de BD para " + criptoUpper + " debido a error de API.");
            return obtenerUltimoPrecioConocidoDeDB(criptoUpper);
        }
    }

    private double obtenerUltimoPrecioConocidoDeDB(String criptoSimbol) {
        String sql = "SELECT hp.precio FROM historial_precios hp " +
                "JOIN criptomonedas c ON hp.id_cripto_fk = c.id_cripto " +
                "WHERE c.simbolo = ? ORDER BY hp.timestamp_precio DESC LIMIT 1";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, criptoSimbol.toUpperCase());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                double precio = rs.getDouble("precio");
                System.out.println("[ServidorPreciosImpl] Último precio desde BD para " + criptoSimbol + ": " + precio);
                return precio;
            } else {
                System.out.println("[ServidorPreciosImpl] No se encontró último precio en BD para " + criptoSimbol);
                return -1.0; // Indicador de no encontrado
            }
        } catch (SQLException e) {
            System.err.println("[Servidor ERROR] Error al obtener último precio de BD para " + criptoSimbol + ": " + e.getMessage());
            return -1.0; // Indicador de error
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
    }


    @Override
    public Map<String, Double> obtenerPreciosMonitoreados(String nombreUsuario) throws RemoteException {
        // Devuelve los precios de las criptos que están en el caché (que son las que tienen alertas activas o se consultaron)
        System.out.println("[ServidorPreciosImpl] Solicitud de precios monitoreados (devolviendo precios desde caché).");
        Map<String, Double> preciosParaCliente = new ConcurrentHashMap<>();
        cacheCriptoData.forEach((simbolo, cripto) -> {
            preciosParaCliente.put(simbolo, cripto.getPrecioUSD());
        });

        System.out.println("[ServidorPreciosImpl] Devolviendo " + preciosParaCliente.size() + " precios. (Timestamp del más reciente en caché: " +
                cacheCriptoData.values().stream()
                        .map(Cripto::getUltimaActualizacionTimestamp)
                        .max(Long::compareTo)
                        .map(ts -> String.format("%tF %<tT", new Date(ts)))
                        .orElse("N/A") + ")");
        return preciosParaCliente;
    }
}