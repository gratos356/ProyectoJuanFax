package Dao;

import Config.Conection;
import Model.NegocioDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NegocioDao {

    public List<NegocioDTO> obtenerNegociosPorCategoria(String categoriaNom) {
        List<NegocioDTO> negocios = new ArrayList<>();
        
        String sql = "SELECT n.id_negocio, n.nombre_establecimiento, n.descripcion, c.nombre_cat, " +
                     "       COALESCE(i.url_imagen, 'default.jpg') AS url_imagen, " +
                     "       p.direccion_texto, p.barrio, " +
                     "       COALESCE(AVG(cal.valor_puntuacion), 0.0) AS promedio " +
                     "FROM negocios n " +
                     "INNER JOIN categorias c ON n.id_categoria = c.id_categoria " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.idNegocio AND i.es_portada = TRUE " +
                     "LEFT JOIN puntos_ubicacion p ON n.id_negocio = p.id_negocio " +
                     "LEFT JOIN calificaciones_sanciones cal ON n.id_negocio = cal.id_negocio AND cal.tipo_registro = 'CALIFICACION' " +
                     "WHERE c.nombre_cat = ? AND n.estado_revision = 'APROBADO' " +
                     "GROUP BY n.id_negocio, c.nombre_cat, i.url_imagen, p.direccion_texto, p.barrio";

        try (Connection con = Conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, categoriaNom);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NegocioDTO dto = new NegocioDTO(
                        rs.getInt("id_negocio"),
                        rs.getString("nombre_establecimiento"),
                        rs.getString("descripcion"),
                        rs.getString("nombre_cat"),
                        rs.getString("url_imagen"),
                        rs.getString("direccion_texto"),
                        rs.getString("barrio"),
                        Math.round(rs.getDouble("promedio") * 10) / 10.0
                    );
                    negocios.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en NegocioDao: " + e.getMessage());
        }
        return negocios;
    }
    
    public List<NegocioDTO> obtenerDestinosDestacados() {
        List<NegocioDTO> lista = new ArrayList<>();
        String sql = "SELECT n.nombre_establecimiento, i.url_imagen " +
                     "FROM negocios n " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " +
                     "LIMIT 5";

        try (Connection con = Conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NegocioDTO n = new NegocioDTO();
                n.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));

                // Leemos url_imagen tal cual se llama ahora en tu tabla modificada
                String foto = rs.getString("url_imagen");
                n.setUrl_imagen(foto != null ? foto : "defecto.jpg"); 

                lista.add(n);
            }
        } catch(SQLException e) { e.printStackTrace(); }
        return lista;
    }
}