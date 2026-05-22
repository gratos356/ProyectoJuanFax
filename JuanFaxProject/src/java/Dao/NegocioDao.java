package Dao;

import Config.Conection;
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
    
    public List<NegocioDTO> obtenerNegociosPorCategoria(String nombreCategoria) {
        List<NegocioDTO> lista = new ArrayList<>();

        // Consulta con FILTRO WHERE para traer solo los negocios de la categoría seleccionada
        String sql = "SELECT n.nombre_establecimiento, i.url_imagen " +
                     "FROM negocios n " +
                     "INNER JOIN categorias c ON n.id_categoria = c.id_categoria " +
                     "LEFT JOIN imagenes i ON n.id_negocio = i.id_negocio " +
                     "WHERE c.nombre_cat = ? AND n.estado_revision = 'APROBADO'";

        try (Connection con = Conection.getConnection();
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
}