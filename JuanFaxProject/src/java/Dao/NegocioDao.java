package Dao;

import Config.conection;
import Model.NegocioDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NegocioDao {
    
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

                // Leemos url_imagen 
                String foto = rs.getString("url_imagen");
                n.setUrl_imagen(foto != null ? foto : "defecto.jpg"); 

                lista.add(n);
            }
        } catch(SQLException e) { e.printStackTrace(); }
        return lista;
    }
    
    public List<NegocioDTO> obtenerNegociosPorCategoria(String nombreCategoria) {
        List<NegocioDTO> lista = new ArrayList<>();

        // Consulta con FILTRO WHERE para traer solo los negocios de la categoría seleccionada
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

        // Tu consulta con el INNER JOIN amarrando la ubicación por id_negocio
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, n.descripcion, "
                           + "i.url_imagen, u.latitud, u.longitud, u.direccion_texto "
                           + "FROM negocios n "
                           + "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio "
                           + "LEFT JOIN puntos_ubicacion u ON n.id_negocio = u.id_negocio "
                           + "WHERE n.nombre_establecimiento = ?";

        try {
            // Reemplaza 'Conexion.getConnection()' por tu método real de conectar a MySQL
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

                // 🌟 CAPTURAMOS LAS COORDENADAS REALES DE LA BASE DE DATOS
                negocio.setLatitud(rs.getDouble("latitud"));
                negocio.setLongitud(rs.getDouble("longitud"));
                // Opcional por si necesitas pintar la dirección en texto en la vista
                negocio.setDireccionTexto(rs.getString("direccion_texto")); 
            }
        } catch (Exception e) {
            System.out.println("Error al obtener detalles geográficos en Juanfax: " + e.getMessage());
        } finally {
            // Buenas prácticas de ADSO: Cerrar flujos para evitar saturar la BD
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }

        return negocio;
    }
}