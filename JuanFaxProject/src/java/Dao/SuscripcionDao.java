package Dao;

// 🌟 IMPORT CORREGIDO: Apunta exactamente a tu clase de configuración
import Config.conection; 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SuscripcionDao {

    // 🔹 PARA OPCIÓN 1: Obtener datos y calcular días restantes en SQL
    public String obtenerDatosSuscripcionJSON(int idNegocio) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String jsonResultado = "{\"success\": false, \"mensaje\": \"No se encontró suscripción\"}";

        String sql = "SELECT tipo_plan, estado_plan, fecha_fin, DATEDIFF(fecha_fin, NOW()) AS dias_restantes " +
                     "FROM suscripciones WHERE id_negocio = ? LIMIT 1";

        try {
            // 🌟 USADO DIRECTAMENTE: Igual que en tu NegocioDao
            con = conection.getConnection(); 
            ps = con.prepareStatement(sql);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();

            if (rs.next()) {
                String tipoPlan = rs.getString("tipo_plan");
                String estado = rs.getString("estado_plan");
                String fechaFin = rs.getString("fecha_fin");
                int diasRestantes = rs.getInt("dias_restantes");

                if (diasRestantes < 0) diasRestantes = 0;

                jsonResultado = String.format(
                    "{\"success\": true, \"tipoPlan\": \"%s\", \"estado\": \"%s\", \"fechaFin\": \"%s\", \"diasRestantes\": %d}",
                    tipoPlan, estado, fechaFin, diasRestantes
                );
            }
        } catch (SQLException e) {
            System.err.println("🚨 Error en obtenerDatosSuscripcionJSON: " + e.getMessage());
            jsonResultado = "{\"success\": false, \"mensaje\": \"Error de base de datos\"}";
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        return jsonResultado;
    }

    // 🔹 PARA OPCIÓN 2: Cambiar de 'TRIAL' a 'ACTIVO' y renovar tiempo
    public boolean renovarSuscripcion(int idNegocio) {
        Connection con = null;
        PreparedStatement ps = null;
        boolean exito = false;

        String sql = "UPDATE suscripciones SET estado_plan = 'ACTIVO', fecha_fin = DATE_ADD(NOW(), INTERVAL 1 MONTH) WHERE id_negocio = ?";

        try {
            // 🌟 USADO DIRECTAMENTE: Igual que en tu NegocioDao
            con = conection.getConnection(); 
            ps = con.prepareStatement(sql);
            ps.setInt(1, idNegocio);
            
            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas > 0) {
                exito = true;
            }
        } catch (SQLException e) {
            System.err.println("🚨 Error en renovarSuscripcion: " + e.getMessage());
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        return exito;
    }

    // 🌟 NUEVO MÉTODO: Resuelve el error 'cannot find symbol' en tu Servlet
    public boolean actualizarPlan(int idNegocio, String tipoPlan) {
        Connection con = null;
        PreparedStatement ps = null;
        boolean exito = false;

        // SQL inteligente: Si es 'ANUAL' suma 1 año, de lo contrario suma 1 mes. Pone el estado en 'ACTIVO'
        String sql = "UPDATE suscripciones SET tipo_plan = ?, estado_plan = 'ACTIVO', " +
                     "fecha_fin = CASE WHEN ? = 'ANUAL' THEN DATE_ADD(NOW(), INTERVAL 1 YEAR) ELSE DATE_ADD(NOW(), INTERVAL 1 MONTH) END " +
                     "WHERE id_negocio = ?";

        try {
            con = conection.getConnection();
            ps = con.prepareStatement(sql);
            
            // Forzamos mayúsculas para evitar el error de truncado con el ENUM de la base de datos
            String planUpper = (tipoPlan != null) ? tipoPlan.toUpperCase().trim() : "MENSUAL";
            
            ps.setString(1, planUpper); // Para el tipo_plan
            ps.setString(2, planUpper); // Para la validación del CASE WHEN
            ps.setInt(3, idNegocio);     // Para el WHERE

            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas > 0) {
                exito = true;
            }
        } catch (SQLException e) {
            System.err.println("🚨 Error en actualizarPlan: " + e.getMessage());
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        return exito;
    }
}