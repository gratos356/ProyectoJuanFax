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
        // Inicializamos el objeto DTO en null por si el negocio no existe o está borrado
        NegocioDTO n = null;

        // 🆕 Agregamos filtro para no buscar negocios borrados lógicamente
        String sql = "SELECT n.id_negocio, n.nit, n.id_categoria, n.nombre_establecimiento, n.descripcion, " +
                     "i.url_imagen AS url_final, p.latitud, p.longitud " +
                     "FROM negocios n " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " + 
                     "LEFT JOIN puntos_ubicacion p ON n.id_negocio = p.id_negocio " +
                     "WHERE n.id_negocio = ? AND n.estado != 'borrado'";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idNegocio);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    n = new NegocioDTO();
                    n.setIdNegocio(rs.getInt("id_negocio"));
                    n.setNit(rs.getString("nit"));
                    n.setIdCategoria(rs.getInt("id_categoria"));
                    n.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));
                    n.setDescripcion(rs.getString("descripcion"));
                    n.setUrl_imagen(rs.getString("url_final"));
                    n.setLatitud(rs.getDouble("latitud"));
                    n.setLongitud(rs.getDouble("longitud"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en obtenerNegocioPorId: " + e.getMessage());
            e.printStackTrace();
        }
        return n;
    }

    public Map<String, Object> obtenerMetricasVendedor(int idVendedor, int idNegocio) {
        Map<String, Object> metricas = new HashMap<>();
        
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = conection.getConnection(); 

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
            int vistasTotales = rs.next() ? rs.getInt("total") : 0;
            metricas.put("vistasTotales", vistasTotales);
            ps.close();

            // === MÉTRICA 2: CONTEO DE CLICS EN EL MAPA ===
            String sqlClicks = "SELECT COUNT(*) AS total FROM metricas_negocio WHERE id_negocio = ? AND tipo_evento = 'CLIC_MAPA'";
            ps = con.prepareStatement(sqlClicks);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();
            int clicksEnlaces = rs.next() ? rs.getInt("total") : 0;
            metricas.put("clicksEnlaces", clicksEnlaces);
            ps.close();

            // === MÉTRICA 3: TOTAL DE RESEÑAS Y PROMEDIO ===
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
                promedioPuntuacion = rs.getDouble("promedio");
            }
            metricas.put("totalResenas", totalResenas);
            metricas.put("puntuacion", promedioPuntuacion);
            ps.close();

            // === MÉTRICA 4: ÚLTIMOS 3 COMENTARIOS ===
            String sqlComentarios = "SELECT c.comentario_justificacion, c.valor_puntuacion, u.nombre_completo " +
                                    "FROM calificaciones_sanciones c " +
                                    "JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                                    "WHERE c.id_negocio = ? AND c.tipo_registro = 'CALIFICACION' " +
                                    "ORDER BY c.id_registro DESC LIMIT 3";
            ps = con.prepareStatement(sqlComentarios);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();

            List<Map<String, Object>> listaComentarios = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> com = new HashMap<>();
                com.put("nombreUsuario", rs.getString("nombre_completo"));
                com.put("calificacion", rs.getInt("valor_puntuacion"));
                com.put("textoComentario", rs.getString("comentario_justificacion"));
                listaComentarios.add(com);
            }
            metricas.put("comentariosRecientes", listaComentarios);
            ps.close();

            // === MÉTRICA 5: DISTRIBUCIÓN PORCENTUAL DE ESTRELLAS ===
            String sqlDistribucion = "SELECT valor_puntuacion, COUNT(*) as cantidad " +
                                     "FROM calificaciones_sanciones " +
                                     "WHERE id_negocio = ? AND tipo_registro = 'CALIFICACION' " +
                                     "GROUP BY valor_puntuacion";
            ps = con.prepareStatement(sqlDistribucion);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();

            int c5 = 0, c4 = 0, c3 = 0;
            while (rs.next()) {
                int estrella = rs.getInt("valor_puntuacion");
                int cant = rs.getInt("cantidad");
                if (estrella == 5) c5 = cant;
                else if (estrella == 4) c4 = cant;
                else if (estrella == 3) c3 = cant;
            }

            Map<String, Integer> distribucion = new HashMap<>();
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
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }

        return metricas;
    }

    private Map<String, Integer> obtenerDistribucionVacia() {
        Map<String, Integer> vacia = new HashMap<>();
        vacia.put("cinco", 0);
        vacia.put("cuatro", 0);
        vacia.put("tres", 0);
        return vacia;
    }

    public List<NegocioDTO> obtenerDestinosDestacados() {
        List<NegocioDTO> lista = new ArrayList<>();

        // 🆕 Aseguramos que solo muestre destacados aprobados y en estado 'activo'
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, i.url_imagen, " +
                     "       COALESCE(AVG(CASE WHEN c.tipo_registro = 'calificacion' THEN c.valor_puntuacion END), 0) AS promedio_calificacion " +
                     "FROM negocios n " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " +
                     "LEFT JOIN calificaciones_sanciones c ON n.id_negocio = c.id_negocio " +
                     "WHERE n.estado_revision = 'APROBADO' AND n.estado = 'activo' " +
                     "GROUP BY n.id_negocio, n.nombre_establecimiento, i.url_imagen " +
                     "ORDER BY promedio_calificacion DESC " +
                     "LIMIT 5";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                NegocioDTO n = new NegocioDTO();
                n.setIdNegocio(rs.getInt("id_negocio")); 
                n.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));

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

        // 🆕 Agregamos filtro de estado operativo 'activo' para los clientes de la app
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, i.url_imagen " +
                     "FROM negocios n " +
                     "INNER JOIN categorias c ON n.id_categoria = c.id_categoria " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " +
                     "WHERE c.nombre_cat = ? AND n.estado_revision = 'APROBADO' AND n.estado = 'activo'";

        try (Connection con = conection.getConnection();
              PreparedStatement ps = con.prepareStatement(sql)) {

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

        // 🆕 El vendedor no debe ver sus propios negocios borrados en su panel principal
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, i.url_imagen, n.estado_revision " +
                     "FROM negocios n " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " +
                     "WHERE n.id_vendedor = ? AND n.estado != 'borrado'";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVendedor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Model.NegocioDTO n = new Model.NegocioDTO();

                    n.setIdNegocio(rs.getInt(1));                  
                    n.setNombreEstablecimiento(rs.getString(2));   

                    String foto = rs.getString(3);                 
                    n.setUrl_imagen(foto != null ? foto : "../imagenes/default-negocio.jpg");

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
        PreparedStatement psNegocio = null;
        PreparedStatement psImagen = null;
        PreparedStatement psUbicacion = null;
        PreparedStatement psSuscripcion = null; 
        ResultSet rsKeys = null;
        boolean guardadoExitoso = false;

        String sqlNegocio = "INSERT INTO negocios (id_vendedor, id_categoria, nit, nombre_establecimiento, descripcion) VALUES (?, ?, ?, ?, ?)";
        String sqlImagen = "INSERT INTO imagenes (id_negocio, url_imagen, descripcion, es_portada) VALUES (?, ?, ?, TRUE)";
        String sqlUbicacion = "INSERT INTO puntos_ubicacion (id_negocio, latitud, longitud) VALUES (?, ?, ?)";
        String sqlSuscripcion = "INSERT INTO suscripciones (id_negocio, tipo_plan, estado_plan, fecha_inicio, fecha_fin) " +
                                "VALUES (?, ?, 'ACTIVO', NOW(), DATE_ADD(NOW(), INTERVAL 1 MONTH))";

        try {
            con = conection.getConnection();
            con.setAutoCommit(false); 

            psNegocio = con.prepareStatement(sqlNegocio, Statement.RETURN_GENERATED_KEYS);
            psNegocio.setInt(1, idVendedor);
            psNegocio.setInt(2, idCategoria);
            psNegocio.setString(3, nit);
            psNegocio.setString(4, negocio.getNombreEstablecimiento());
            psNegocio.setString(5, descripcion);
            psNegocio.executeUpdate();

            rsKeys = psNegocio.getGeneratedKeys();
            if (rsKeys.next()) {
                int idNegocioGenerado = rsKeys.getInt(1);

                psImagen = con.prepareStatement(sqlImagen);
                psImagen.setInt(1, idNegocioGenerado); 
                psImagen.setString(2, negocio.getUrl_imagen());
                psImagen.setString(3, "Portada");
                psImagen.executeUpdate();

                psUbicacion = con.prepareStatement(sqlUbicacion);
                psUbicacion.setInt(1, idNegocioGenerado);
                psUbicacion.setDouble(2, latitud);
                psUbicacion.setDouble(3, longitud);
                psUbicacion.executeUpdate();

                if (tipoPlan == null || tipoPlan.trim().isEmpty()) {
                    tipoPlan = "Mensual"; 
                }

                psSuscripcion = con.prepareStatement(sqlSuscripcion);
                psSuscripcion.setInt(1, idNegocioGenerado);
                psSuscripcion.setString(2, tipoPlan.toUpperCase());
                psSuscripcion.executeUpdate();

                con.commit(); 
                guardadoExitoso = true;
            }
        } catch (SQLException e) {
            System.err.println("=== ❌ ERROR SQL EN LA TRANSACCIÓN DEL REGISTRO ===");
            e.printStackTrace();
            try { 
                if (con != null) {
                    System.err.println("🔄 Ejecutando rollback de la transacción...");
                    con.rollback(); 
                }
            } catch (SQLException ex) { 
                ex.printStackTrace(); 
            }
        } finally {
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
        // 🆕 Un negocio borrado lógicamente no debe contarse en las métricas de revisión del Administrador
        String sql = "SELECT COUNT(*) FROM negocios WHERE estado_revision = ? AND estado != 'borrado'";
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
        // 🆕 Filtrar para que la bandeja del Administrador no muestre los negocios que ya se eliminaron
        String sql = "SELECT id_negocio, nombre_establecimiento, descripcion " +
                     "FROM negocios WHERE estado_revision = ? AND estado != 'borrado'";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
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
        // 🆕 Añadimos control preventivo para no interactuar con negocios eliminados
        String sql = "UPDATE negocios SET estado_revision = ? WHERE id_negocio = ? AND estado != 'borrado'";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado);
            ps.setInt(2, idNegocio);

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.out.println("ERROR AL ACTUALIZAR ESTADO: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarNegocio(int idNegocio) {
        String sqlDesactivarProductos = "UPDATE productos SET estado = 0 WHERE id_negocio = ?"; 
        String sqlBorradoLogicoNegocio = "UPDATE negocios SET estado = 'borrado' WHERE id_negocio = ?";

        try (Connection con = conection.getConnection()) {
            con.setAutoCommit(false); 

            try (PreparedStatement psProd = con.prepareStatement(sqlDesactivarProductos);
                 PreparedStatement psNeg = con.prepareStatement(sqlBorradoLogicoNegocio)) {

                psProd.setInt(1, idNegocio);
                psProd.executeUpdate();

                psNeg.setInt(1, idNegocio);
                int filasAfectadas = psNeg.executeUpdate();

                con.commit(); 
                return filasAfectadas > 0;

            } catch (SQLException e) {
                con.rollback(); 
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en NegocioDao.eliminarNegocio (Borrado Lógico): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizarNegocio(Model.NegocioDTO negocio) {
        // 🆕 Asegurar que no se actualice por error un negocio borrado
        String sql = "UPDATE negocios SET nit = ?, nombre_establecimiento = ?, descripcion = ?, id_categoria = ?, estado_revision = 'PENDIENTE' WHERE id_negocio = ? AND estado != 'borrado'";
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

        // 🆕 Se añade la condición `WHERE n.estado != 'borrado'` antes del `GROUP BY`
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, n.estado_revision, c.nombre_cat AS categoria, " +
                     "COUNT(DISTINCT CASE WHEN m.tipo_evento = 'VISTA' THEN m.id_metrica END) AS total_vistas, " +
                     "IFNULL(AVG(CASE WHEN cs.tipo_registro = 'CALIFICACION' THEN cs.valor_puntuacion END), 0.0) AS promedio_calificacion " +
                     "FROM negocios n " +
                     "INNER JOIN categorias c ON n.id_categoria = c.id_categoria " +
                     "LEFT JOIN metricas_negocio m ON n.id_negocio = m.id_negocio " +
                     "LEFT JOIN calificaciones_sanciones cs ON n.id_negocio = cs.id_negocio " +
                     "WHERE n.estado != 'borrado' " +
                     "GROUP BY n.id_negocio, n.nombre_establecimiento, n.estado_revision, c.nombre_cat";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                NegocioDTO n = new NegocioDTO();
                n.setIdNegocio(rs.getInt("id_negocio"));
                n.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));
                n.setEstado(rs.getString("estado_revision"));
                n.setNombreCategoria(rs.getString("categoria")); 
                n.setVistas(rs.getInt("total_vistas")); 
                n.setCalificacion(rs.getDouble("promedio_calificacion")); 
                n.setSuscripcion("Mensual"); 

                lista.add(n);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en obtenerTodosLosNegociosAdmin: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    public int obtenerIdVendedorPorNegocio(int idNegocio) {
        // 🆕 Añadimos el filtro para evitar recuperar el dueño de un negocio eliminado
        String sql = "SELECT id_vendedor FROM negocios WHERE id_negocio = ? AND estado != 'borrado'";
        
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idNegocio);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_vendedor");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en NegocioDao.obtenerIdVendedorPorNegocio: " + e.getMessage());
            e.printStackTrace();
        }
        return 0; 
    }
}