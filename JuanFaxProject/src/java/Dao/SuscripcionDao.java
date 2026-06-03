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
        // Declaramos las variables de JDBC de forma tradicional para cerrarlas en el bloque finally
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        // Respuesta por defecto con formato JSON estructurado en caso de que el negocio no tenga plan asignado
        String jsonResultado = "{\"success\": false, \"mensaje\": \"No se encontró suscripción\"}";

        // Consulta SQL utilizando la función DATEDIFF de MySQL para calcular la diferencia en días 
        // entre la fecha de vencimiento (fecha_fin) y la fecha actual del servidor (NOW())
        String sql = "SELECT tipo_plan, estado_plan, fecha_fin, DATEDIFF(fecha_fin, NOW()) AS dias_restantes " +
                     "FROM suscripciones WHERE id_negocio = ? LIMIT 1";

        try {
            con = conection.getConnection(); 
            ps = con.prepareStatement(sql);
            
            // Pasamos el ID del negocio al filtro del WHERE
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();

            // Si el cursor encuentra la suscripción del negocio, extraemos sus columnas
            if (rs.next()) {
                String tipoPlan = rs.getString("tipo_plan");
                String estado = rs.getString("estado_plan");
                String fechaFin = rs.getString("fecha_fin");
                int diasRestantes = rs.getInt("dias_restantes");

                // Control defensivo: Si el plan ya expiró, DATEDIFF devuelve un número negativo.
                // Lo normalizamos a 0 para no enviar valores extraños o inconsistentes al frontend.
                if (diasRestantes < 0) diasRestantes = 0;

                // Construimos manualmente la cadena JSON usando String.format() para inyectar las variables de forma limpia
                jsonResultado = String.format(
                    "{\"success\": true, \"tipoPlan\": \"%s\", \"estado\": \"%s\", \"fechaFin\": \"%s\", \"diasRestantes\": %d}",
                    tipoPlan, estado, fechaFin, diasRestantes
                );
            }
        } catch (SQLException e) {
            // Captura de fallos de red o de sintaxis en el motor de bases de datos
            System.err.println("🚨 Error en obtenerDatosSuscripcionJSON: " + e.getMessage());
            jsonResultado = "{\"success\": false, \"mensaje\": \"Error de base de datos\"}";
        } finally {
            // Bloque obligatorio de cierre manual e individual para liberar los recursos del servidor y del pool
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        // Retorna la cadena JSON armada lista para que el Servlet la imprima con el Content-Type adecuado
        return jsonResultado;
    }

    /**
     * 2. PROCESAR TRANSACCIÓN COMPLETA (PAGO + ACTUALIZACIÓN DE PLAN)
     * Inserta el registro del dinero en la tabla 'pagos' y modifica la vigencia de la 
     * suscripción de forma atómica. Si algún paso falla, hace un rollback automático.
     */
    public boolean registrarPagoYActualizarPlan(int idNegocio, String tipoPlan, double monto, String idTransaccion) {
        Connection con = null;
        // Creamos múltiples PreparedStatements para segmentar con claridad cada paso de la transacción
        PreparedStatement psBuscar = null;
        PreparedStatement psPago = null;
        PreparedStatement psSuscripcion = null;
        ResultSet rs = null;
        boolean exito = false;

        // Sentencias SQL que se ejecutarán en cadena secuencial
        String sqlBuscarSuscripcion = "SELECT id_suscripcion FROM suscripciones WHERE id_negocio = ? LIMIT 1";
        String sqlInsertarPago = "INSERT INTO pagos (id_suscripcion, monto, fecha_pago, id_transaccion, estado) VALUES (?, ?, NOW(), ?, 'Aprobado')";
        
        // Uso de la estructura lógica CASE WHEN en MySQL para sumar un mes o un año a la vigencia actual con DATE_ADD()
        String sqlActualizarSuscripcion = "UPDATE suscripciones SET tipo_plan = ?, estado_plan = 'ACTIVO', " +
                     "fecha_fin = CASE WHEN ? = 'ANUAL' THEN DATE_ADD(NOW(), INTERVAL 1 YEAR) ELSE DATE_ADD(NOW(), INTERVAL 1 MONTH) END " +
                     "WHERE id_suscripcion = ?";

        try {
            con = conection.getConnection();
            
            // 🔒 CRÍTICO: Desactivamos el AutoCommit. Le quitamos el control a MySQL para manejar nosotros la transacción de forma manual.
            con.setAutoCommit(false); 

            // === PASO A: LOCALIZAR LA SUSCRIPCIÓN BASE ===
            psBuscar = con.prepareStatement(sqlBuscarSuscripcion);
            psBuscar.setInt(1, idNegocio);
            rs = psBuscar.executeQuery();

            // Validamos que el negocio posea una fila previa en la tabla suscripciones (creada cuando se registró por primera vez)
            if (rs.next()) {
                int idSuscripcion = rs.getInt("id_suscripcion");

                // === PASO B: INSERTAR LA AUDITORÍA DEL PAGO ===
                psPago = con.prepareStatement(sqlInsertarPago);
                psPago.setInt(1, idSuscripcion); // Asociamos el pago a la PK de la suscripción localizada
                psPago.setDouble(2, monto);
                psPago.setString(3, idTransaccion); // Código de aprobación de la pasarela de pagos
                psPago.executeUpdate();

                // === PASO C: EXTENDER VIGENCIA Y ACTUALIZAR PLAN ===
                psSuscripcion = con.prepareStatement(sqlActualizarSuscripcion);
                
                // Control defensivo de Strings: Si viene nulo, por defecto le asignamos el plan MENSUAL en mayúsculas
                String planUpper = (tipoPlan != null) ? tipoPlan.toUpperCase().trim() : "MENSUAL";
                
                psSuscripcion.setString(1, planUpper); // Reemplaza el primer '?' (tipo_plan = ?)
                psSuscripcion.setString(2, planUpper); // Reemplaza el segundo '?' (en el CASE WHEN ? = 'ANUAL')
                psSuscripcion.setInt(3, idSuscripcion); // Reemplaza el tercer '?' (WHERE id_suscripcion = ?)
                psSuscripcion.executeUpdate();

                // ✅ COMMIT: Si los tres pasos se completaron sin excepciones, confirmamos los cambios permanentemente en lote
                con.commit(); 
                exito = true;
            } else {
                // Si el negocio no tiene una fila base en suscripciones, la base de datos quedaría inconsistente. 
                // Ejecutamos rollback preventivo por seguridad.
                con.rollback(); 
                System.err.println("🚨 No se encontró una suscripción base para el negocio: " + idNegocio);
            }
        } catch (SQLException e) {
            System.err.println("🚨 Error en la transacción registrarPagoYActualizarPlan: " + e.getMessage());
            // ↩️ CONTINGENCIA: Si cualquiera de los PreparedStatements falló, entramos al catch y revertimos todo
            if (con != null) {
                try { 
                    System.err.println("🔄 Ejecutando rollback para limpiar los datos huérfanos de la transacción...");
                    con.rollback(); // Elimina el registro del pago y deshaz los cambios del UPDATE
                } catch (SQLException ex) { 
                    ex.printStackTrace(); 
                } 
            }
        } finally {
            // Cerramos de forma individual e inversa los objetos abiertos para evitar fugas de memoria
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
     * Recupera todas las transacciones monetarias efectuadas por el establecimiento comercial.
     */
    public java.util.List<Model.PagoDTO> obtenerHistorialPagos(int idNegocio) {
        // Instanciamos la lista dinámica encargada de almacenar las facturas/pagos encontrados
        java.util.List<Model.PagoDTO> lista = new java.util.ArrayList<>();
        
        // Consulta relacional con JOIN: La tabla 'pagos' solo conoce el id_suscripcion,
        // por lo tanto cruzamos con la tabla 'suscripciones' para poder filtrar directamente por el id_negocio
        String sql = "SELECT p.* FROM pagos p " +
                     "JOIN suscripciones s ON p.id_suscripcion = s.id_suscripcion " +
                     "WHERE s.id_negocio = ? ORDER BY p.fecha_pago DESC"; // Ordena las facturas de la más reciente a la más antigua

        // Bloque try-with-resources moderno: Abre y destruye de forma automática la conexión y el PreparedStatement
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            // Seteamos el parámetro de búsqueda
            ps.setInt(1, idNegocio);
            
            // Bloque try-with-resources anidado para controlar el recorrido del ResultSet de forma segura
            try (ResultSet rs = ps.executeQuery()) {
                // Recorremos el historial fila por fila
                while (rs.next()) {
                    Model.PagoDTO pago = new Model.PagoDTO();
                    
                    // Asignamos las columnas del registro actual de la base de datos a las propiedades de nuestro DTO
                    pago.setIdPago(rs.getInt("id_pago"));
                    pago.setMonto(rs.getDouble("monto"));
                    
                    // Recuperamos la fecha y hora exacta con getTimestamp para conservar el rastro de auditoría completo
                    pago.setFechaPago(rs.getTimestamp("fecha_pago"));
                    pago.setIdTransaccion(rs.getString("id_transaccion"));
                    pago.setEstado(rs.getString("estado"));
                    
                    // Añadimos el DTO mapeado a la colección
                    lista.add(pago);
                }
            }
        } catch (SQLException e) {
            // Reporte en consola si hay problemas de mapeo o caídas de conexión en el servidor local
            System.err.println("🚨 Error en obtenerHistorialPagos: " + e.getMessage());
        }
        // Retorna la lista con los pagos encontrados para renderizar en la vista de facturación del usuario
        return lista;
    }
}