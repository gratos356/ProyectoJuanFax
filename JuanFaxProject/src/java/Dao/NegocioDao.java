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
        NegocioDTO n = null;

        // 🌟 CORREGIDO: Cambiamos i.urlImagen e i.idNegocio a snake_case (url_imagen e id_negocio)
        String sql = "SELECT n.id_negocio, n.nit, n.id_categoria, n.nombre_establecimiento, n.descripcion, " +
                     "i.url_imagen AS url_final, p.latitud, p.longitud " +
                     "FROM negocios n " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " + 
                     "LEFT JOIN puntos_ubicacion p ON n.id_negocio = p.id_negocio " +
                     "WHERE n.id_negocio = ?";

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
                    
                    // 🌟 CORREGIDO: Cambiado de setDescription a setDescripcion
                    n.setDescripcion(rs.getString("descripcion"));

                    // Leemos el alias 'url_final' para evitar choques de nombres
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
    // ====================================================================
    // 🌟 MÉTODO: OBTENER MÉTRICAS REALES PARA EL PANEL DEL VENDEDOR
    // ====================================================================
    public Map<String, Object> obtenerMetricasVendedor(int idVendedor, int idNegocio) {
        Map<String, Object> metricas = new HashMap<>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = conection.getConnection(); 

            // 🌟 VALIDACIÓN INICIAL DE SEGURIDAD
            // Si por algún error de sincronización llega en 0, devolvemos un mapa seguro vacío
            if (idNegocio == 0) {
                metricas.put("vistasTotales", 0);
                metricas.put("clicksEnlaces", 0);
                metricas.put("totalResenas", 0);
                metricas.put("puntuacion", 0.0);
                metricas.put("comentariosRecientes", new ArrayList<>());
                metricas.put("distribucionEstrellas", obtenerDistribucionVacia());
                return metricas;
            }

            // 📈 2. CONTAR VISTAS TOTALES (Filtrado por el negocio seleccionado)
            String sqlVistas = "SELECT COUNT(*) AS total FROM metricas_negocio WHERE id_negocio = ? AND tipo_evento = 'VISTA'";
            ps = con.prepareStatement(sqlVistas);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();
            int vistasTotales = rs.next() ? rs.getInt("total") : 0;
            metricas.put("vistasTotales", vistasTotales);

            // Importante: Cerramos el PreparedStatement previo antes de reutilizar la variable ps
            ps.close();

            // 🖱️ 3. CONTAR CLICKS TOTALES
            String sqlClicks = "SELECT COUNT(*) AS total FROM metricas_negocio WHERE id_negocio = ? AND tipo_evento = 'CLIC_MAPA'";
            ps = con.prepareStatement(sqlClicks);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();
            int clicksEnlaces = rs.next() ? rs.getInt("total") : 0;
            metricas.put("clicksEnlaces", clicksEnlaces);

            ps.close();

            // 💬 4. OBTENER TOTAL DE RESEÑAS Y PROMEDIO DE CALIFICACIÓN
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

            // ⭐ 5. OBTENER LAS ÚLTIMAS 3 RESEÑAS CON JOIN A USUARIOS
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

            // 📊 6. DISTRIBUCIÓN PORCENTUAL DE ESTRELLAS
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
            // Cerramos todos los recursos de manera limpia e individual
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
    
    // ====================================================================
    // MÉTODOS ANTERIORES PRESERVADOS COMPLETA Y CORRECTAMENTE
    // ====================================================================
    public List<NegocioDTO> obtenerDestinosDestacados() {
        List<NegocioDTO> lista = new ArrayList<>();

        // 🌟 FILTRAMOS CON "WHERE n.estado_revision = 'APROBADO'" 
        // Esto descarta automáticamente los pendientes, rechazados o bloqueados.
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, i.url_imagen " +
                     "FROM negocios n " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " +
                     "WHERE n.estado_revision = 'APROBADO' " +
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

        // 🌟 1. AÑADIMOS n.id_negocio A LA CONSULTA SQL
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, i.url_imagen " +
                     "FROM negocios n " +
                     "INNER JOIN categorias c ON n.id_categoria = c.id_categoria " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " +
                     "WHERE c.nombre_cat = ? AND n.estado_revision = 'APROBADO'";

        try (Connection con = conection.getConnection();
              PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombreCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NegocioDTO n = new NegocioDTO();

                    // 🌟 2. OBTENEMOS EL ID Y LO GUARDAMOS EN EL DTO
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
    
    public NegocioDTO obtenerNegocioPorNombre(String nombreBuscar) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        NegocioDTO negocio = null;

        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, i.url_imagen " +
                         "FROM negocios n " +
                         "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " +
                         "WHERE n.id_vendedor = ?";

        try {
            con = conection.getConnection(); 
            ps = con.prepareStatement(sql);
            ps.setString(1, nombreBuscar);
            rs = ps.executeQuery();

            if (rs.next()) {
                negocio = new NegocioDTO();
                negocio.setIdNegocio(rs.getInt("id_negocio"));
                negocio.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));
                negocio.setDescripcion(rs.getString("descripcion"));
                negocio.setUrl_imagen(rs.getString("url_imagen"));

                negocio.setLatitud(rs.getDouble("latitud"));
                negocio.setLongitud(rs.getDouble("longitud"));
                negocio.setDireccionTexto(rs.getString("direccion_texto")); 
            }
        } catch (Exception e) {
            System.out.println("Error al obtener detalles geográficos en Juanfax: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }

        return negocio;
    }
    /**
    * Obtiene la lista de negocios asignados a un vendedor específico
    */
    public List<Model.NegocioDTO> obtenerNegociosPorVendedor(int idVendedor) {
        List<Model.NegocioDTO> lista = new java.util.ArrayList<>();

        // 🌟 Agregamos n.estado_revision al final del SELECT
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

                    n.setIdNegocio(rs.getInt(1));                  // 1. n.id_negocio
                    n.setNombreEstablecimiento(rs.getString(2));   // 2. n.nombre_establecimiento

                    // Si el negocio no tiene fotos, se le asigna la ruta por defecto
                    String foto = rs.getString(3);                 // 3. i.url_imagen
                    n.setUrl_imagen(foto != null ? foto : "../imagenes/default-negocio.jpg");

                    // 🌟 LEEMOS EL ESTADO (Índice 4) y lo guardamos en el DTO
                    n.setEstado(rs.getString(4));                  // 4. n.estado_revision (Asegúrate de que tu DTO tenga .setEstado o .setEstadoRevision)

                    lista.add(n);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error real en NegocioDao (obtenerNegociosPorVendedor): " + e.getMessage());
            e.printStackTrace();
        }

        return lista;
    }
   public boolean registrarNegocio(NegocioDTO negocio, int idVendedor, int idCategoria,String nit, String descripcion, double latitud, double longitud) {
        Connection con = null;
        PreparedStatement psNegocio = null;
        PreparedStatement psImagen = null;
        PreparedStatement psUbicacion = null;
        ResultSet rsKeys = null;
        boolean guardadoExitoso = false;

        String sqlNegocio = "INSERT INTO negocios (id_vendedor, id_categoria, nit, nombre_establecimiento, descripcion) VALUES (?, ?, ?, ?, ?)";
        String sqlImagen = "INSERT INTO imagenes (id_negocio, url_imagen, descripcion, es_portada) VALUES (?, ?, ?, TRUE)";
        String sqlUbicacion = "INSERT INTO puntos_ubicacion (id_negocio, id_destino, latitud, longitud) VALUES (?, 1, ?, ?)";

        try {
            con = conection.getConnection();
            con.setAutoCommit(false); // Iniciamos transacción

            // 1. Insertar Negocio
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

                // 2. Insertar Imagen
                psImagen = con.prepareStatement(sqlImagen);
                psImagen.setInt(1, idNegocioGenerado);
                psImagen.setString(2, negocio.getUrl_imagen());
                psImagen.setString(3, "Portada");
                psImagen.executeUpdate();

                // 3. Insertar Ubicación
                psUbicacion = con.prepareStatement(sqlUbicacion);
                psUbicacion.setInt(1, idNegocioGenerado);
                psUbicacion.setDouble(2, latitud);
                psUbicacion.setDouble(3, longitud);
                psUbicacion.executeUpdate();

                con.commit(); // Todo salió bien
                guardadoExitoso = true;
            }
        } catch (SQLException e) {
            System.err.println("=== ERROR SQL: " + e.getMessage() + " ===");
            try { if (con != null) con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
        } finally {
            try { if (rsKeys != null) rsKeys.close(); } catch (Exception e) {}
            try { if (psNegocio != null) psNegocio.close(); } catch (Exception e) {}
            try { if (psImagen != null) psImagen.close(); } catch (Exception e) {}
            try { if (psUbicacion != null) psUbicacion.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        return guardadoExitoso;
    }
    // 1. Método para contar cuántos negocios hay según su estado
    public int contarNegociosPorEstado(String estado) {
        String sql = "SELECT COUNT(*) FROM negocios WHERE estado_revision = ?";
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

    // 2. Método para listar negocios según su estado (para las solicitudes)
    public List<NegocioDTO> obtenerNegociosPorEstado(String estado) {
        List<NegocioDTO> lista = new ArrayList<>();
        String sql = "SELECT id_negocio, nombre_establecimiento, descripcion " +
                     "FROM negocios WHERE estado_revision = ?";

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
        String sql = "UPDATE negocios SET estado_revision = ? WHERE id_negocio = ?";
        
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idNegocio);
            
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0; // Devuelve true si la actualización fue exitosa

        } catch (SQLException e) {
            System.out.println("Executing UPDATE - ID: " + idNegocio + " - Estado: " + nuevoEstado);
            System.out.println("ERROR AL ACTUALIZAR ESTADO: " + e.getMessage());
            return false;
        }
    }
    public boolean eliminarNegocio(int idNegocio) {
        // 1. Definimos las consultas de eliminación en estricto orden jerárquico
        String sqlPagos = "DELETE FROM pagos_historial WHERE id_suscripcion IN (SELECT id_suscripcion FROM suscripciones WHERE id_negocio = ?)";
        String sqlSuscripciones = "DELETE FROM suscripciones WHERE id_negocio = ?";
        String sqlUbicaciones = "DELETE FROM puntos_ubicacion WHERE id_negocio = ?";
        String sqlSanciones = "DELETE FROM calificaciones_sanciones WHERE id_negocio = ?";
        String sqlMetricas = "DELETE FROM metricas_negocio WHERE id_negocio = ?";
        String sqlNegocio = "DELETE FROM negocios WHERE id_negocio = ?";

        try (Connection con = conection.getConnection()) {
            // Activamos el modo transacción para asegurar consistencia
            con.setAutoCommit(false); 

            try (PreparedStatement psPagos = con.prepareStatement(sqlPagos);
                 PreparedStatement psSuscrip = con.prepareStatement(sqlSuscripciones);
                 PreparedStatement psUbicac = con.prepareStatement(sqlUbicaciones);
                 PreparedStatement psSancion = con.prepareStatement(sqlSanciones);
                 PreparedStatement psMetricas = con.prepareStatement(sqlMetricas);
                 PreparedStatement psNeg = con.prepareStatement(sqlNegocio)) {

                // Paso A: Borrar el historial de pagos asociados a las suscripciones de este negocio
                psPagos.setInt(1, idNegocio);
                psPagos.executeUpdate();

                // Paso B: Borrar las suscripciones del negocio
                psSuscrip.setInt(1, idNegocio);
                psSuscrip.executeUpdate();

                // Paso C: Borrar los puntos de ubicación geográfica
                psUbicac.setInt(1, idNegocio);
                psUbicac.executeUpdate();

                // Paso D: Borrar calificaciones y registros de sanciones
                psSancion.setInt(1, idNegocio);
                psSancion.executeUpdate();

                // Paso E: Borrar las métricas y eventos registrados (vistas, clics)
                psMetricas.setInt(1, idNegocio);
                psMetricas.executeUpdate();

                // ⚠️ Paso Final: Ahora que el negocio no tiene ninguna atadura en la DB, lo borramos
                psNeg.setInt(1, idNegocio);
                int filasAfectadas = psNeg.executeUpdate();

                // Si el negocio principal se eliminó con éxito, confirmamos toda la transacción
                con.commit(); 
                return filasAfectadas > 0;

            } catch (SQLException e) {
                // Si cualquier consulta llega a fallar, revertimos todo para no romper los datos
                con.rollback(); 
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en NegocioDao.eliminarNegocio: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    // B. Actualizar los datos en la DB y devolver el estado a 'PENDIENTE'
    public boolean actualizarNegocio(Model.NegocioDTO negocio) {
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

        // Consulta SQL con Joins para traer el nombre de la categoría asignada
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, n.estado_revision, " +
                     "c.nombre_cat AS categoria " +
                     "FROM negocios n " +
                     "INNER JOIN categorias c ON n.id_categoria = c.id_categoria";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                NegocioDTO n = new NegocioDTO();
                int idNegocio = rs.getInt("id_negocio");

                n.setIdNegocio(idNegocio);
                n.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));
                n.setEstado(rs.getString("estado_revision")); // Toma "Activo", "Trial", "Bloqueado", etc.

                // Pasamos el nombre de la categoría obtenido del JOIN
                n.setNombreCategoria(rs.getString("categoria")); 

                // --- CÁLCULO DE MÉTRICAS DINÁMICAS INDIVIDUALES ---
                // 1. Contar vistas reales de este negocio
                String sqlVistas = "SELECT COUNT(*) FROM metricas_negocio WHERE id_negocio = ? AND tipo_evento = 'VISTA'";
                try (PreparedStatement psV = con.prepareStatement(sqlVistas)) {
                    psV.setInt(1, idNegocio);
                    try (ResultSet rsV = psV.executeQuery()) {
                        n.setVistas(rsV.next() ? rsV.getInt(1) : 0);
                    }
                }

                // 2. Calcular promedio de calificación real
                String sqlCalific = "SELECT AVG(valor_puntuacion) FROM calificaciones_sanciones WHERE id_negocio = ? AND tipo_registro = 'CALIFICACION'";
                try (PreparedStatement psC = con.prepareStatement(sqlCalific)) {
                    psC.setInt(1, idNegocio);
                    try (ResultSet rsC = psC.executeQuery()) {
                        n.setCalificacion(rsC.next() ? rsC.getDouble(1) : 0.0);
                    }
                }

                // 3. Simulación de suscripción (Si no tienes tabla de suscripciones aún, dejamos una por defecto)
                n.setSuscripcion("Mensual"); 

                lista.add(n);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en obtenerTodosLosNegociosAdmin: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }
    
}