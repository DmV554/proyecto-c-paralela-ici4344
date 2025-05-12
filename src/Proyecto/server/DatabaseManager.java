    package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.Map;

public class DatabaseManager {

    // --- Configuración de la Base de Datos ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/cripto_monitor_db";

    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";


    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error al cargar el driver JDBC de MySQL: " + e.getMessage());
        }
    }

    // --- Métodos de Conexión y Cierre ---

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
        } catch (SQLException e) {
            System.err.println("Error al cerrar ResultSet: " + e.getMessage());
        }
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            System.err.println("Error al cerrar Statement: " + e.getMessage());
        }
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException e) {
            System.err.println("Error al cerrar Connection: " + e.getMessage());
        }
    }

    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        // Reutiliza el método close anterior para PreparedStatement
        close(conn, (Statement) pstmt, rs);
    }

    public static void close(Connection conn, Statement stmt) {
        close(conn, stmt, null);
    }
    public static void close(Connection conn, PreparedStatement pstmt) {
        close(conn, pstmt, null);
    }


    public static void inicializarCriptomonedasBase() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        Map<String, String> criptosBase = CoinGeckoService.SYMBOL_TO_COINGECKO_ID_MAP;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Iniciar transacción

            // Primero, verificar si la tabla está vacía para evitar inserciones duplicadas en cada inicio
            // O usar INSERT IGNORE o ON DUPLICATE KEY UPDATE para cada cripto
            String sqlCheckEmpty = "SELECT COUNT(*) FROM criptomonedas";
            Statement stmtCheck = conn.createStatement();
            rs = stmtCheck.executeQuery(sqlCheckEmpty);
            if (rs.next() && rs.getInt(1) > 0 && criptosBase.size() == rs.getInt(1) ) {
                System.out.println("[DatabaseManager] La tabla 'criptomonedas' ya parece estar poblada con las bases.");
            }
            rs.close();
            stmtCheck.close();


            String sql = "INSERT IGNORE INTO criptomonedas (simbolo, coingecko_id, nombre_cripto) VALUES (?, ?, ?)";
            pstmt = conn.prepareStatement(sql);

            for (Map.Entry<String, String> entry : criptosBase.entrySet()) {
                String simbolo = entry.getKey();
                String coingeckoId = entry.getValue();
                String nombreCripto = coingeckoId.substring(0, 1).toUpperCase() + coingeckoId.substring(1); // Nombre simple a partir del ID

                pstmt.setString(1, simbolo);
                pstmt.setString(2, coingeckoId);
                pstmt.setString(3, nombreCripto);
                pstmt.addBatch();
                System.out.println("[DatabaseManager] Preparando para insertar/ignorar criptomoneda: " + simbolo);
            }
            pstmt.executeBatch();
            conn.commit();
            System.out.println("[DatabaseManager] Criptomonedas base verificadas/insertadas en la BD.");

        } catch (SQLException e) {
            System.err.println("[DatabaseManager ERROR] Error al inicializar criptomonedas base: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[DatabaseManager ERROR] Error en rollback: " + ex.getMessage());
                }
            }
        } finally {
            close(conn, pstmt, rs); // rs ya debería estar cerrado
        }
    }
    public static void inicializarUsuarioPorDefecto(String nombreUsuarioPorDefecto) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            // Verificar si el usuario ya existe
            String sqlCheck = "SELECT id_usuario FROM usuarios WHERE nombre_usuario = ?";
            pstmt = conn.prepareStatement(sqlCheck);
            pstmt.setString(1, nombreUsuarioPorDefecto);
            rs = pstmt.executeQuery();

            if (!rs.next()) { // Si no existe, insertarlo
                rs.close();
                pstmt.close(); // Cerrar el PreparedStatement anterior

                String sqlInsert = "INSERT INTO usuarios (nombre_usuario) VALUES (?)";
                pstmt = conn.prepareStatement(sqlInsert);
                pstmt.setString(1, nombreUsuarioPorDefecto);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("[DatabaseManager] Usuario por defecto '" + nombreUsuarioPorDefecto + "' creado.");
                }
            } else {
                System.out.println("[DatabaseManager] Usuario por defecto '" + nombreUsuarioPorDefecto + "' ya existe.");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager ERROR] Error al inicializar usuario por defecto: " + e.getMessage());
        } finally {
            close(conn, pstmt, rs);
        }
    }
}