package Dao;

// 🌟 IMPORT CORREGIDO: Apunta exactamente a tu clase de configuración
import Config.conection; 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SuscripcionDao {

    /**
     * 1. OBTENER DATOS DE SUSCRIPCIÓN EN FORMATO JSON
     * Consulta el estado del plan y calcula los días restantes directamente en MySQL.
     */
    public String obtenerDatosSuscripcionJSON(int idNegocio) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        String jsonResultado = "{\"success\": false, \"mensaje\": \"No se encontró suscripción\"}";

        String sql = "SELECT tipo_plan, estado_plan, fecha_fin, DATEDIFF(fecha_fin, NOW()) AS dias_restantes " +
                     "FROM suscripciones WHERE id_negocio = ? LIMIT 1";

        try {
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

    /**
     * 2. PROCESAR TRANSACCIÓN COMPLETA (PAGO + RENOVACIÓN ANTICIPADA ACUMULATIVA)
     * REGLA DE NEGOCIO B: Si renueva antes de vencerse, se toman los días acumulados 
     * a partir de la 'fecha_fin' previa registrada, evitando pisar la vigencia actual.
     */
    public boolean registrarPagoYActualizarPlan(int idNegocio, String tipoPlan, double monto, String idTransaccion) {
        Connection con = null;
        PreparedStatement psBuscar = null;
        PreparedStatement psPago = null;
        PreparedStatement psSuscripcion = null;
        ResultSet rs = null;
        boolean exito = false;

        String sqlBuscarSuscripcion = "SELECT id_suscripcion FROM suscripciones WHERE id_negocio = ? LIMIT 1";
        String sqlInsertarPago = "INSERT INTO pagos (id_suscripcion, monto, fecha_pago, id_transaccion, estado) VALUES (?, ?, NOW(), ?, 'Aprobado')";
        
        // 🌟 REFINADO CON LOGICA ACUMULATIVA: Evita pisar la fecha actual si el plan sigue vigente
        String sqlActualizarSuscripcion = "UPDATE suscripciones SET tipo_plan = ?, estado_plan = 'ACTIVO', " +
                     "fecha_fin = DATE_ADD(" +
                     "  IF(fecha_fin > NOW(), fecha_fin, NOW()), " +
                     "  INTERVAL (CASE WHEN ? = 'ANUAL' THEN 365 ELSE 30 END) DAY" +
                     ") " +
                     "WHERE id_suscripcion = ?";

        try {
            con = conection.getConnection();
            con.setAutoCommit(false); // 🔒 CONTROL TRANSACCIONAL MANUAL

            // === PASO A: LOCALIZAR LA SUSCRIPCIÓN BASE ===
            psBuscar = con.prepareStatement(sqlBuscarSuscripcion);
            psBuscar.setInt(1, idNegocio);
            rs = psBuscar.executeQuery();

            if (rs.next()) {
                int idSuscripcion = rs.getInt("id_suscripcion");

                // === PASO B: INSERTAR LA AUDITORÍA DEL PAGO ===
                psPago = con.prepareStatement(sqlInsertarPago);
                psPago.setInt(1, idSuscripcion);
                psPago.setDouble(2, monto);
                psPago.setString(3, idTransaccion); 
                psPago.executeUpdate();

                // === PASO C: EXTENDER VIGENCIA ACUMULANDO TIEMPO ===
                psSuscripcion = con.prepareStatement(sqlActualizarSuscripcion);
                String planUpper = (tipoPlan != null) ? tipoPlan.toUpperCase().trim() : "MENSUAL";
                
                psSuscripcion.setString(1, planUpper);       // tipo_plan = ?
                psSuscripcion.setString(2, planUpper);       // CASE WHEN ? = 'ANUAL'
                psSuscripcion.setInt(3, idSuscripcion);      // WHERE id_suscripcion = ?
                psSuscripcion.executeUpdate();

                con.commit(); // ✅ SE PERSISTEN LOS CAMBIOS EN LOTE
                exito = true;
            } else {
                con.rollback(); 
                System.err.println("🚨 No se encontró una suscripción base para el negocio: " + idNegocio);
            }
        } catch (SQLException e) {
            System.err.println("🚨 Error en la transacción registrarPagoYActualizarPlan: " + e.getMessage());
            if (con != null) {
                try { 
                    System.err.println("🔄 Ejecutando rollback para limpiar la transacción...");
                    con.rollback(); 
                } catch (SQLException ex) { ex.printStackTrace(); } 
            }
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (psBuscar != null) psBuscar.close(); } catch (Exception e) {}
            try { if (psPago != null) psPago.close(); } catch (Exception e) {}
            try { if (psSuscripcion != null) psSuscripcion.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        return exito;
    }

    /**
     * 3. OBTENER HISTORIAL DE PAGOS (Filtrado por negocio)
     */
    public java.util.List<Model.PagoDTO> obtenerHistorialPagos(int idNegocio) {
        java.util.List<Model.PagoDTO> lista = new java.util.ArrayList<>();
        String sql = "SELECT p.* FROM pagos p " +
                     "JOIN suscripciones s ON p.id_suscripcion = s.id_suscripcion " +
                     "WHERE s.id_negocio = ? ORDER BY p.fecha_pago DESC";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idNegocio);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Model.PagoDTO pago = new Model.PagoDTO();
                    pago.setIdPago(rs.getInt("id_pago"));
                    pago.setMonto(rs.getDouble("monto"));
                    pago.setFechaPago(rs.getTimestamp("fecha_pago"));
                    pago.setIdTransaccion(rs.getString("id_transaccion"));
                    pago.setEstado(rs.getString("estado"));
                    lista.add(pago);
                }
            }
        } catch (SQLException e) {
            System.err.println("🚨 Error en obtenerHistorialPagos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * 🌟 4. REVERSIÓN DE FONDOS POR BLOQUEO ADMINISTRATIVO (REGLA DE NEGOCIO A)
     * Desactiva de forma atómica la suscripción vigente y cambia el estado de los pagos 
     * activos a 'RECHAZADO' para auditoría visual del vendedor.
     */
    public boolean procesarReembolsoPorBloqueo(int idNegocio, int idVendedor) {
        Connection con = null;
        PreparedStatement psSuscripcion = null;
        PreparedStatement psPago = null;
        boolean exito = false;

        String sqlInactivarSuscripcion = "UPDATE suscripciones SET estado_plan = 'SUSPENDIDO' WHERE id_negocio = ?";
        
        // Buscamos el último pago 'Aprobado' de la suscripción vinculada a ese negocio para cambiar su estado a 'RECHAZADO'
        String sqlRevertirPago = "UPDATE pagos SET estado = 'RECHAZADO' " +
                                 "WHERE id_suscripcion = (SELECT id_suscripcion FROM suscripciones WHERE id_negocio = ? LIMIT 1) " +
                                 "AND estado = 'Aprobado'";

        try {
            con = conection.getConnection();
            con.setAutoCommit(false); // 🔒 INICIO DE TRANSACCIÓN ATÓMICA

            // Paso A: Dar de baja el estado de la suscripción del negocio
            psSuscripcion = con.prepareStatement(sqlInactivarSuscripcion);
            psSuscripcion.setInt(1, idNegocio);
            psSuscripcion.executeUpdate();

            // Paso B: Marcar el pago en el historial como RECHAZADO para reflejar la anulación
            psPago = con.prepareStatement(sqlRevertirPago);
            psPago.setInt(1, idNegocio);
            psPago.executeUpdate();

            con.commit(); // ✅ OPERACIÓN FINANCIERA COMPLETADA CON ÉXITO
            exito = true;
            System.out.println("✅ Transacción de Reembolso exitosa para el Negocio ID: " + idNegocio + " (Vendedor ID: " + idVendedor + ")");
            
        } catch (SQLException e) {
            System.err.println("🚨 Error crítico en la transacción de reembolso por bloqueo: " + e.getMessage());
            if (con != null) {
                try {
                    System.err.println("🔄 Ejecutando rollback de reversión de fondos...");
                    con.rollback();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        } finally {
            try { if (psSuscripcion != null) psSuscripcion.close(); } catch (Exception e) {}
            try { if (psPago != null) psPago.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        return exito;
    }
}