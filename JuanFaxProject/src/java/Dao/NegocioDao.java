package Dao;

import Config.conection;
import Model.NegocioDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class NegocioDao {
    
    public NegocioDTO obtenerNegocioPorId(int idNegocio) {
        // Inicializamos el objeto DTO en null por si el negocio no existe
        NegocioDTO n = null;

        // Consulta con LEFT JOIN para traer datos del negocio, su imagen de portada y su ubicación geográfica
        String sql = "SELECT n.id_negocio, n.nit, n.id_categoria, n.nombre_establecimiento, n.descripcion, " +
                     "i.url_imagen AS url_final, p.latitud, p.longitud " +
                     "FROM negocios n " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " + 
                     "LEFT JOIN puntos_ubicacion p ON n.id_negocio = p.id_negocio " +
                     "WHERE n.id_negocio = ?";

        // Bloque try-with-resources: abre y cierra automáticamente la conexión y el PreparedStatement
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Asignamos el ID del negocio al parámetro '?' de la consulta
            ps.setInt(1, idNegocio);

            // Ejecutamos la consulta y manejamos el ResultSet con otro try-with-resources
            try (ResultSet rs = ps.executeQuery()) {
                // Si el ResultSet tiene una fila (el negocio existe), instanciamos el DTO y mapeamos las columnas
                if (rs.next()) {
                    n = new NegocioDTO();
                    n.setIdNegocio(rs.getInt("id_negocio"));
                    n.setNit(rs.getString("nit"));
                    n.setIdCategoria(rs.getInt("id_categoria"));
                    n.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));
                    n.setDescripcion(rs.getString("descripcion"));

                    // Recuperamos la URL usando el alias de la consulta SQL para evitar conflictos
                    n.setUrl_imagen(rs.getString("url_final"));

                    // Mapeamos las coordenadas geográficas flotantes/double
                    n.setLatitud(rs.getDouble("latitud"));
                    n.setLongitud(rs.getDouble("longitud"));
                }
            }
        } catch (SQLException e) {
            // Captura de errores en caso de fallos de red o sintaxis SQL
            System.err.println("Error en obtenerNegocioPorId: " + e.getMessage());
            e.printStackTrace();
        }
        // Retorna el DTO lleno o null si no hubo coincidencias
        return n;
    }

    public Map<String, Object> obtenerMetricasVendedor(int idVendedor, int idNegocio) {
        // Mapa estructurado donde guardaremos todas las diferentes métricas solicitadas
        Map<String, Object> metricas = new HashMap<>();
        
        // Declaramos las variables fuera para poder cerrarlas manualmente en el bloque 'finally'
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = conection.getConnection(); 

            // Control defensivo: si el ID es 0 (no se ha seleccionado un negocio), devolvemos métricas vacías
            if (idNegocio == 0) {
                metricas.put("vistasTotales", 0);
                metricas.put("clicksEnlaces", 0);
                metricas.put("totalResenas", 0);
                metricas.put("puntuacion", 0.0);
                metricas.put("comentariosRecientes", new ArrayList<>());
                metricas.put("distribucionEstrellas", obtenerDistribucionVacia());
                return metricas;
            }

            // === MÉTRICA 1: CONTEO DE VISTAS ===
            String sqlVistas = "SELECT COUNT(*) AS total FROM metricas_negocio WHERE id_negocio = ? AND tipo_evento = 'VISTA'";
            ps = con.prepareStatement(sqlVistas);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();
            // Si hay resultado extraemos el entero, de lo contrario asignamos 0
            int vistasTotales = rs.next() ? rs.getInt("total") : 0;
            metricas.put("vistasTotales", vistasTotales);
            ps.close(); // Cerramos el PreparedStatement para poder reutilizar la variable 'ps'

            // === MÉTRICA 2: CONTEO DE CLICS EN EL MAPA ===
            String sqlClicks = "SELECT COUNT(*) AS total FROM metricas_negocio WHERE id_negocio = ? AND tipo_evento = 'CLIC_MAPA'";
            ps = con.prepareStatement(sqlClicks);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();
            int clicksEnlaces = rs.next() ? rs.getInt("total") : 0;
            metricas.put("clicksEnlaces", clicksEnlaces);
            ps.close();

            // === MÉTRICA 3: TOTAL DE RESEÑAS Y PROMEDIO DE CALIFICACIÓN ===
            String sqlCalificaciones = "SELECT COUNT(*) AS total, AVG(valor_puntuacion) AS promedio " +
                                       "FROM calificaciones_sanciones " +
                                       "WHERE id_negocio = ? AND tipo_registro = 'CALIFICACION'";
            ps = con.prepareStatement(sqlCalificaciones);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();
            int totalResenas = 0;
            double promedioPuntuacion = 0.0;
            if (rs.next()) {
                totalResenas = rs.getInt("total");
                promedioPuntuacion = rs.getDouble("promedio"); // Obtiene el promedio calculado por la DB
            }
            metricas.put("totalResenas", totalResenas);
            metricas.put("puntuacion", promedioPuntuacion);
            ps.close();

            // === MÉTRICA 4: ÚLTIMOS 3 COMENTARIOS CON NOMBRE DE USUARIO ===
            String sqlComentarios = "SELECT c.comentario_justificacion, c.valor_puntuacion, u.nombre_completo " +
                                    "FROM calificaciones_sanciones c " +
                                    "JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                                    "WHERE c.id_negocio = ? AND c.tipo_registro = 'CALIFICACION' " +
                                    "ORDER BY c.id_registro DESC LIMIT 3"; // Orden inverso (más nuevos primero) limitado a 3
            ps = con.prepareStatement(sqlComentarios);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();

            // Lista de mapas internos para estructurar cada comentario de manera flexible
            List<Map<String, Object>> listaComentarios = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> com = new HashMap<>();
                com.put("nombreUsuario", rs.getString("nombre_completo"));
                com.put("calificacion", rs.getInt("valor_puntuacion"));
                com.put("textoComentario", rs.getString("comentario_justificacion"));
                listaComentarios.add(com); // Añadimos cada comentario estructurado a la lista principal
            }
            metricas.put("comentariosRecientes", listaComentarios);
            ps.close();

            // === MÉTRICA 5: DISTRIBUCIÓN PORCENTUAL DE ESTRELLAS ===
            String sqlDistribucion = "SELECT valor_puntuacion, COUNT(*) as cantidad " +
                                     "FROM calificaciones_sanciones " +
                                     "WHERE id_negocio = ? AND tipo_registro = 'CALIFICACION' " +
                                     "GROUP BY valor_puntuacion"; // Agrupamos por el valor de estrella (1, 2, 3, 4, 5)
            ps = con.prepareStatement(sqlDistribucion);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();

            // Variables contadoras independientes para las estrellas críticas del negocio
            int c5 = 0, c4 = 0, c3 = 0;
            while (rs.next()) {
                int estrella = rs.getInt("valor_puntuacion");
                int cant = rs.getInt("cantidad");
                if (estrella == 5) c5 = cant;
                else if (estrella == 4) c4 = cant;
                else if (estrella == 3) c3 = cant;
            }

            Map<String, Integer> distribucion = new HashMap<>();
            // Regla matemática: si hay reseñas calculamos el porcentaje, si no, inicializamos en cero
            if (totalResenas > 0) {
                distribucion.put("cinco", (c5 * 100) / totalResenas);
                distribucion.put("cuatro", (c4 * 100) / totalResenas);
                distribucion.put("tres", (c3 * 100) / totalResenas);
            } else {
                distribucion = obtenerDistribucionVacia();
            }
            metricas.put("distribucionEstrellas", distribucion);

        } catch (SQLException e) {
            System.out.println("Error en NegocioDao.obtenerMetricasVendedor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Bloque obligatorio de cierre de recursos para evitar fugas de memoria (Memory Leaks)
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }

        return metricas;
    }

    private Map<String, Integer> obtenerDistribucionVacia() {
        // Helper utilitario que simplemente arma un mapa limpio con valores iniciales a 0%
        Map<String, Integer> vacia = new HashMap<>();
        vacia.put("cinco", 0);
        vacia.put("cuatro", 0);
        vacia.put("tres", 0);
        return vacia;
    }

    public List<NegocioDTO> obtenerDestinosDestacados() {
        List<NegocioDTO> lista = new ArrayList<>();

        // Consulta que trae hasta 5 negocios que ya estén aprobados en el sistema
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, i.url_imagen " +
                     "FROM negocios n " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " +
                     "WHERE n.estado_revision = 'APROBADO' " +
                     "LIMIT 5";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Recorremos las filas devueltas y creamos DTOs dinámicos para añadirlos a la lista
            while (rs.next()) {
                NegocioDTO n = new NegocioDTO();
                n.setIdNegocio(rs.getInt("id_negocio")); 
                n.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));

                // Control defensivo: si no tiene imagen asignada en la DB, le inyectamos una imagen por defecto
                String foto = rs.getString("url_imagen");
                n.setUrl_imagen(foto != null ? foto : "defecto.jpg"); 

                lista.add(n);
            }
        } catch(SQLException e) { 
            e.printStackTrace(); 
        }
        return lista;
    }

    public List<NegocioDTO> obtenerNegociosPorCategoria(String nombreCategoria) {
        List<NegocioDTO> lista = new ArrayList<>();

        // Consulta que filtra por el nombre de la categoría cruzando tablas mediante INNER JOIN
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, i.url_imagen " +
                     "FROM negocios n " +
                     "INNER JOIN categorias c ON n.id_categoria = c.id_categoria " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " +
                     "WHERE c.nombre_cat = ? AND n.estado_revision = 'APROBADO'";

        try (Connection con = conection.getConnection();
              PreparedStatement ps = con.prepareStatement(sql)) {

            // Pasamos el parámetro del filtro de categoría enviado desde el controlador frontend
            ps.setString(1, nombreCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NegocioDTO n = new NegocioDTO();
                    n.setIdNegocio(rs.getInt("id_negocio")); 
                    n.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));

                    String foto = rs.getString("url_imagen");
                    n.setUrl_imagen(foto != null ? foto : "defecto.jpg");

                    lista.add(n);
                }
            }
        } catch (SQLException e) {
            System.out.println("ERROR EN DAO AL FILTRAR: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    public List<Model.NegocioDTO> obtenerNegociosPorVendedor(int idVendedor) {
        List<Model.NegocioDTO> lista = new java.util.ArrayList<>();

        // Trae los negocios de un usuario particular especificando su estado actual (PENDIENTE/APROBADO)
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, i.url_imagen, n.estado_revision " +
                     "FROM negocios n " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " +
                     "WHERE n.id_vendedor = ?";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVendedor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Model.NegocioDTO n = new Model.NegocioDTO();

                    // Lectura y asignación rápida mediante índices posicionales de columnas (1, 2, 3, 4)
                    n.setIdNegocio(rs.getInt(1));                  
                    n.setNombreEstablecimiento(rs.getString(2));   

                    String foto = rs.getString(3);                 
                    n.setUrl_imagen(foto != null ? foto : "../imagenes/default-negocio.jpg");

                    // Asignamos el estado de revisión recuperado a nuestro DTO para alertar en la interfaz del vendedor
                    n.setEstado(rs.getString(4));                  

                    lista.add(n);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error real en NegocioDao (obtenerNegociosPorVendedor): " + e.getMessage());
            e.printStackTrace();
        }

        return lista;
    }

    public boolean registrarNegocio(NegocioDTO negocio, int idVendedor, int idCategoria, String nit, String descripcion, double latitud, double longitud, String tipoPlan) {
        Connection con = null;
        // Creamos múltiples PreparedStatements individuales para manejar la inserción multi-tabla de forma organizada
        PreparedStatement psNegocio = null;
        PreparedStatement psImagen = null;
        PreparedStatement psUbicacion = null;
        PreparedStatement psSuscripcion = null; 
        ResultSet rsKeys = null;
        boolean guardadoExitoso = false;

        // Definición de las 4 consultas de inserción consecutivas
        String sqlNegocio = "INSERT INTO negocios (id_vendedor, id_categoria, nit, nombre_establecimiento, descripcion) VALUES (?, ?, ?, ?, ?)";
        String sqlImagen = "INSERT INTO imagenes (id_negocio, url_imagen, descripcion, es_portada) VALUES (?, ?, ?, TRUE)";
        String sqlUbicacion = "INSERT INTO puntos_ubicacion (id_negocio, id_destino, latitud, longitud) VALUES (?, 1, ?, ?)";
        // Inserción automatizada con fechas calculadas desde MySQL usando NOW() y DATE_ADD para añadir un mes de vigencia
        String sqlSuscripcion = "INSERT INTO suscripciones (id_negocio, tipo_plan, estado_plan, fecha_inicio, fecha_fin) " +
                                "VALUES (?, ?, 'ACTIVO', NOW(), DATE_ADD(NOW(), INTERVAL 1 MONTH))";

        try {
            con = conection.getConnection();
            
            // ⚠️ CRÍTICO: Desactivamos el AutoCommit. Si un paso falla, ningún dato parcial se guardará en la DB.
            con.setAutoCommit(false); 

            // === PASO 1: INSERTAR DATOS DEL NEGOCIO PRINCIPAL ===
            // Añadimos el flag RETURN_GENERATED_KEYS para capturar el ID auto-incremental generado por la DB
            psNegocio = con.prepareStatement(sqlNegocio, Statement.RETURN_GENERATED_KEYS);
            psNegocio.setInt(1, idVendedor);
            psNegocio.setInt(2, idCategoria);
            psNegocio.setString(3, nit);
            psNegocio.setString(4, negocio.getNombreEstablecimiento());
            psNegocio.setString(5, descripcion);
            psNegocio.executeUpdate();

            // Rescatamos la clave primaria autogenerada (id_negocio)
            rsKeys = psNegocio.getGeneratedKeys();
            if (rsKeys.next()) {
                int idNegocioGenerado = rsKeys.getInt(1);

                // === PASO 2: INSERTAR LA IMAGEN DE PORTADA DEL NEGOCIO ===
                psImagen = con.prepareStatement(sqlImagen);
                psImagen.setInt(1, idNegocioGenerado); // Usamos el ID recuperado del paso 1
                psImagen.setString(2, negocio.getUrl_imagen());
                psImagen.setString(3, "Portada");
                psImagen.executeUpdate();

                // === PASO 3: INSERTAR COORDENADAS DE UBICACIÓN ===
                psUbicacion = con.prepareStatement(sqlUbicacion);
                psUbicacion.setInt(1, idNegocioGenerado);
                psUbicacion.setDouble(2, latitud);
                psUbicacion.setDouble(3, longitud);
                psUbicacion.executeUpdate();

                // === PASO 4: VALIDACIÓN DEFENSIVA DE PLAN Y SUSCRIPCIÓN ===
                if (tipoPlan == null || tipoPlan.trim().isEmpty()) {
                    tipoPlan = "Mensual"; // Valor de contingencia por seguridad
                }

                psSuscripcion = con.prepareStatement(sqlSuscripcion);
                psSuscripcion.setInt(1, idNegocioGenerado);
                psSuscripcion.setString(2, tipoPlan.toUpperCase());
                psSuscripcion.executeUpdate();

                // 🔄 TODO EXITOSO: Guardamos los cambios permanentemente en lote de forma segura
                con.commit(); 
                guardadoExitoso = true;
            }
        } catch (SQLException e) {
            System.err.println("=== ❌ ERROR SQL EN LA TRANSACCIÓN DEL REGISTRO ===");
            System.err.println("Mensaje real: " + e.getMessage());
            e.printStackTrace();

            // 🔄 MANEJO DE CONTINGENCIA: Si cualquiera de las 4 inserciones falló, ejecutamos Rollback completo
            try { 
                if (con != null) {
                    System.err.println("🔄 Ejecutando rollback de la transacción...");
                    con.rollback(); // Limpia la DB de datos huérfanos de este intento de registro
                }
            } catch (SQLException ex) { 
                ex.printStackTrace(); 
            }
        } finally {
            // Cerramos de forma individual e inversa todos los flujos abiertos en la transacción
            try { if (rsKeys != null) rsKeys.close(); } catch (Exception e) {}
            try { if (psNegocio != null) psNegocio.close(); } catch (Exception e) {}
            try { if (psImagen != null) psImagen.close(); } catch (Exception e) {}
            try { if (psUbicacion != null) psUbicacion.close(); } catch (Exception e) {}
            try { if (psSuscripcion != null) psSuscripcion.close(); } catch (Exception e) {} 
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        return guardadoExitoso;
    }

    public int contarNegociosPorEstado(String estado) {
        String sql = "SELECT COUNT(*) FROM negocios WHERE estado_revision = ?";
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Retornamos el entero directo ubicado en la primera columna indexada del conteo
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("ERROR AL CONTAR: " + e.getMessage());
        }
        return 0;
    }

    public List<NegocioDTO> obtenerNegociosPorEstado(String estado) {
        List<NegocioDTO> lista = new ArrayList<>();
        String sql = "SELECT id_negocio, nombre_establecimiento, descripcion " +
                     "FROM negocios WHERE estado_revision = ?";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Mapeo básico simplificado para optimizar las cargas de las bandejas del administrador
                    NegocioDTO n = new NegocioDTO();
                    n.setIdNegocio(rs.getInt("id_negocio"));
                    n.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));
                    n.setDescripcion(rs.getString("descripcion"));
                    lista.add(n);
                }
            }
        } catch (SQLException e) {
            System.out.println("ERROR AL LISTAR POR ESTADO: " + e.getMessage());
        }
        return lista;
    }

    public boolean actualizarEstadoNegocio(int idNegocio, String nuevoEstado) {
        String sql = "UPDATE negocios SET estado_revision = ? WHERE id_negocio = ?";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado);
            ps.setInt(2, idNegocio);

            int filasAfectadas = ps.executeUpdate();
            // Retorna verdadero únicamente si se modificó al menos un registro real en la DB
            return filasAfectadas > 0;

        } catch (SQLException e) {
            // Logs de ayuda por consola en caso de errores en tiempo de ejecución
            System.out.println("Executing UPDATE - ID: " + idNegocio + " - Estado: " + nuevoEstado);
            System.out.println("ERROR AL ACTUALIZAR ESTADO: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarNegocio(int idNegocio) {
        // Consultas estructuradas de eliminación ordenadas de forma rigurosa según dependencias FK
        String sqlPagos = "DELETE FROM pagos_historial WHERE id_suscripcion IN (SELECT id_suscripcion FROM suscripciones WHERE id_negocio = ?)";
        String sqlSuscripciones = "DELETE FROM suscripciones WHERE id_negocio = ?";
        String sqlUbicaciones = "DELETE FROM puntos_ubicacion WHERE id_negocio = ?";
        String sqlSanciones = "DELETE FROM calificaciones_sanciones WHERE id_negocio = ?";
        String sqlMetricas = "DELETE FROM metricas_negocio WHERE id_negocio = ?";
        String sqlNegocio = "DELETE FROM negocios WHERE id_negocio = ?";

        try (Connection con = conection.getConnection()) {
            con.setAutoCommit(false); // Activamos transacción atómica por consistencia relacional

            try (PreparedStatement psPagos = con.prepareStatement(sqlPagos);
                 PreparedStatement psSuscrip = con.prepareStatement(sqlSuscripciones);
                 PreparedStatement psUbicac = con.prepareStatement(sqlUbicaciones);
                 PreparedStatement psSancion = con.prepareStatement(sqlSanciones);
                 PreparedStatement psMetricas = con.prepareStatement(sqlMetricas);
                 PreparedStatement psNeg = con.prepareStatement(sqlNegocio)) {

                // Paso A: Borrar pagos enlazados a las suscripciones de este negocio específico
                psPagos.setInt(1, idNegocio);
                psPagos.executeUpdate();

                // Paso B: Eliminar suscripción del negocio
                psSuscrip.setInt(1, idNegocio);
                psSuscrip.executeUpdate();

                // Paso C: Eliminar coordenadas del negocio
                psUbicac.setInt(1, idNegocio);
                psUbicac.executeUpdate();

                // Paso D: Eliminar registros históricos de calificaciones del negocio
                psSancion.setInt(1, idNegocio);
                psSancion.executeUpdate();

                // Paso E: Eliminar métricas del negocio
                psMetricas.setInt(1, idNegocio);
                psMetricas.executeUpdate();

                // Paso Final: Ahora que el registro principal no posee dependencias activas, se elimina
                psNeg.setInt(1, idNegocio);
                int filasAfectadas = psNeg.executeUpdate();

                con.commit(); // Confirmamos todos los DELETE ejecutados en bloque uniforme
                return filasAfectadas > 0;

            } catch (SQLException e) {
                con.rollback(); // En caso de fallo relacional, cancelamos y revertimos todos los borrados ejecutados en este método
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en NegocioDao.eliminarNegocio: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizarNegocio(Model.NegocioDTO negocio) {
        // Consulta para actualizar los campos comerciales del perfil del establecimiento
        // NOTA: Se fuerza el estado_revision = 'PENDIENTE' para obligar a una re-evaluación por parte de un administrador
        String sql = "UPDATE negocios SET nit = ?, nombre_establecimiento = ?, descripcion = ?, id_categoria = ?, estado_revision = 'PENDIENTE' WHERE id_negocio = ?";
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, negocio.getNit());
            ps.setString(2, negocio.getNombreEstablecimiento());
            ps.setString(3, negocio.getDescripcion());
            ps.setInt(4, negocio.getIdCategoria());
            ps.setInt(5, negocio.getIdNegocio());

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            System.err.println("Error en actualizarNegocio: " + e.getMessage());
            return false;
        }
    }

    public List<NegocioDTO> obtenerTodosLosNegociosAdmin() {
        List<NegocioDTO> lista = new ArrayList<>();

        // Query consolidador masivo: Usa funciones agregadas (COUNT y AVG) combinadas con CASE WHEN 
        // para unificar métricas complejas, optimizando de manera drástica el tráfico de peticiones a la DB
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, n.estado_revision, c.nombre_cat AS categoria, " +
                     "COUNT(DISTINCT CASE WHEN m.tipo_evento = 'VISTA' THEN m.id_metrica END) AS total_vistas, " +
                     "IFNULL(AVG(CASE WHEN cs.tipo_registro = 'CALIFICACION' THEN cs.valor_puntuacion END), 0.0) AS promedio_calificacion " +
                     "FROM negocios n " +
                     "INNER JOIN categorias c ON n.id_categoria = c.id_categoria " +
                     "LEFT JOIN metricas_negocio m ON n.id_negocio = m.id_negocio " +
                     "LEFT JOIN calificaciones_sanciones cs ON n.id_negocio = cs.id_negocio " +
                     "GROUP BY n.id_negocio, n.nombre_establecimiento, n.estado_revision, c.nombre_cat";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Extraemos los datos calculados e inyectamos los resultados al DTO
            while (rs.next()) {
                NegocioDTO n = new NegocioDTO();
                n.setIdNegocio(rs.getInt("id_negocio"));
                n.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));
                n.setEstado(rs.getString("estado_revision"));
                n.setNombreCategoria(rs.getString("categoria")); 
                n.setVistas(rs.getInt("total_vistas")); // Asignamos el alias de conteo procesado
                n.setCalificacion(rs.getDouble("promedio_calificacion")); // Asignamos el promedio float/double
                n.setSuscripcion("Mensual"); // Hardcode temporal sugerido en la lógica base

                lista.add(n);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en obtenerTodosLosNegociosAdmin: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }
}