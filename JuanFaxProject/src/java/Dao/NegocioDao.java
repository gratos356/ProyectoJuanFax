package Dao;

import Config.conection;
import Model.NegocioDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NegocioDao {
    
    // ====================================================================
    // 🌟 MÉTODO: OBTENER MÉTRICAS REALES PARA EL PANEL DEL VENDEDOR
    // ====================================================================
    public Map<String, Object> obtenerMetricasVendedor(int idVendedor) {
        Map<String, Object> metricas = new HashMap<>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = conection.getConnection(); 

            // 1. OBTENER EL ID DEL NEGOCIO PERTENECIENTE AL VENDEDOR
            String sqlNegocio = "SELECT id_negocio FROM negocios WHERE id_vendedor = ? LIMIT 1";
            ps = con.prepareStatement(sqlNegocio);
            ps.setInt(1, idVendedor);
            rs = ps.executeQuery();

            int idNegocio = 0;
            if (rs.next()) {
                idNegocio = rs.getInt("id_negocio");
            }

            // Si el vendedor aún no tiene un negocio registrado, retornamos valores seguros en 0
            if (idNegocio == 0) {
                metricas.put("vistasTotales", 0);
                metricas.put("clicksEnlaces", 0);
                metricas.put("totalResenas", 0);
                metricas.put("puntuacion", 0.0);
                metricas.put("comentariosRecientes", new ArrayList<>());
                metricas.put("distribucionEstrellas", obtenerDistribucionVacia());
                return metricas;
            }

            // 2. CONTAR VISTAS TOTALES
            String sqlVistas = "SELECT COUNT(*) AS total FROM metricas_negocio WHERE id_negocio = ? AND tipo_evento = 'VISTA'";
            ps = con.prepareStatement(sqlVistas);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();
            int vistasTotales = rs.next() ? rs.getInt("total") : 0;
            metricas.put("vistasTotales", vistasTotales);

            // 3. CONTAR CLICKS TOTALES
            String sqlClicks = "SELECT COUNT(*) AS total FROM metricas_negocio WHERE id_negocio = ? AND tipo_evento = 'CLIC_MAPA'";
            ps = con.prepareStatement(sqlClicks);
            ps.setInt(1, idNegocio);
            rs = ps.executeQuery();
            int clicksEnlaces = rs.next() ? rs.getInt("total") : 0;
            metricas.put("clicksEnlaces", clicksEnlaces);

            // 4. OBTENER TOTAL DE RESEÑAS Y PROMEDIO DE CALIFICACIÓN
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

            // 5. OBTENER LAS ÚLTIMAS 3 RESEÑAS CON JOIN A USUARIOS
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

            // 6. DISTRIBUCIÓN PORCENTUAL DE ESTRELLAS
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
            System.out.println("❌ Error en NegocioDao.obtenerMetricasVendedor: " + e.getMessage());
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
    
    // ====================================================================
    // MÉTODOS ANTERIORES PRESERVADOS COMPLETA Y CORRECTAMENTE
    // ====================================================================
    public List<NegocioDTO> obtenerDestinosDestacados() {
        List<NegocioDTO> lista = new ArrayList<>();
        String sql = "SELECT n.nombre_establecimiento, i.url_imagen " +
                     "FROM negocios n " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " +
                     "LIMIT 5";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NegocioDTO n = new NegocioDTO();
                n.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));

                String foto = rs.getString("url_imagen");
                n.setUrl_imagen(foto != null ? foto : "defecto.jpg"); 

                lista.add(n);
            }
        } catch(SQLException e) { e.printStackTrace(); }
        return lista;
    }
    
    public List<NegocioDTO> obtenerNegociosPorCategoria(String nombreCategoria) {
        List<NegocioDTO> lista = new ArrayList<>();

        String sql = "SELECT n.nombre_establecimiento, i.url_imagen " +
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
                    n.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));

                    String foto = rs.getString("url_imagen");
                    n.setUrl_imagen(foto != null ? foto : "defecto.jpg");

                    lista.add(n);
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ ERROR EN DAO AL FILTRAR: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }
    
    public NegocioDTO obtenerNegocioPorNombre(String nombreBuscar) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        NegocioDTO negocio = null;

        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, n.descripcion, "
                           + "i.url_imagen, u.latitud, u.longitud, u.direccion_texto "
                           + "FROM negocios n "
                           + "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio "
                           + "LEFT JOIN puntos_ubicacion u ON n.id_negocio = u.id_negocio "
                           + "WHERE n.nombre_establecimiento = ?";

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
}