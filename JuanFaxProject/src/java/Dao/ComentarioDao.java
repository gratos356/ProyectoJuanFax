/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dao;

import Config.conection;
import Model.ComentarioDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComentarioDao {
public boolean insertarComentario(ComentarioDTO comentario) {
        String sql = "INSERT INTO calificaciones_sanciones "
                   + "(id_usuario, id_negocio, comentario_justificacion, valor_puntuacion, tipo_registro, fecha_registro) "
                   + "VALUES (?, ?, ?, ?, 'CALIFICACION', NOW())";
        
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, comentario.getIdUsuario());
            ps.setInt(2, comentario.getIdNegocio());
            ps.setString(3, comentario.getTextoComentario());
            ps.setInt(4, comentario.getCalificacion()); 

            int filas = ps.executeUpdate();
            return filas > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error al insertar comentario en el DAO: " + e.getMessage());
            return false;
        }
    }

    // 2. CONSULTA PARA LISTAR (Traer comentarios y estrellas usando INNER JOIN)
    public List<ComentarioDTO> obtenerComentariosPorNegocio(int idNegocio) {
        List<ComentarioDTO> lista = new ArrayList<>();
        
        // 🌟 CONSULTA AJUSTADA A TUS NOMBRES DE TABLA REALES
        String sql = "SELECT c.id_registro AS id_comentario, "
                   + "c.id_negocio, c.id_usuario, "
                   + "c.comentario_justificacion AS texto_comentario, "
                   + "c.fecha_registro AS fecha_publicacion, "
                   + "c.valor_puntuacion AS calificacion, "
                   + "u.nombre_completo "
                   + "FROM calificaciones_sanciones c "
                   + "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario "
                   + "WHERE c.id_negocio = ? AND c.tipo_registro = 'CALIFICACION' "
                   + "ORDER BY c.fecha_registro DESC";

        try (Connection con = Config.conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idNegocio);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ComentarioDTO c = new ComentarioDTO();
                    c.setIdComentario(rs.getInt("id_comentario"));
                    c.setIdNegocio(rs.getInt("id_negocio"));
                    c.setIdUsuario(rs.getInt("id_usuario"));
                    c.setTextoComentario(rs.getString("texto_comentario"));
                    c.setFechaPublicacion(rs.getTimestamp("fecha_publicacion"));
                    c.setNombreUsuario(rs.getString("nombre_completo"));
                    c.setCalificacion(rs.getInt("calificacion"));
                    
                    lista.add(c);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error crítico en ComentarioDao (verificar conexión/tablas): " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }
}
