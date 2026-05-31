package Dao;

import Config.conection; // Importación exacta de tu clase de conexión
import Model.UsuarioDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDao {

    /**
     * Recupera todos los usuarios registrados en el sistema.
     * Ajusta los nombres de las columnas ("id_usuario", "nombre", etc.) a como estén en tu tabla MySQL.
     */
    public List<UsuarioDTO> obtenerTodosLosUsuarios() {
        List<UsuarioDTO> lista = new ArrayList<>();
        String sql = "SELECT u.id_usuario, u.nombre_completo, u.correo_electronico, r.nombre_rol AS rol, u.estado " +
                 "FROM usuarios u " +
                 "INNER JOIN roles r ON u.id_rol = r.id_rol";

        // Reemplaza conection.getConect() por tu método de conexión real si varía
        try (Connection con = conection.getConnection(); 
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UsuarioDTO usuario = new UsuarioDTO();
                usuario.setIdUsuario(rs.getInt("id_usuario"));
                usuario.setNombreCompleto(rs.getString("nombre_completo"));
                usuario.setCorreoElectronico(rs.getString("correo_electronico"));
                usuario.setNombreRol(rs.getString("rol"));
                usuario.setEstado(rs.getString("estado"));

                lista.add(usuario);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en UsuarioDao.obtenerTodosLosUsuarios: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Modifica el estado de acceso de un usuario (ACTIVO / BLOQUEADO)
     */
    public boolean cambiarEstadoUsuario(int idUsuario, String nuevoEstado) {
        String sql = "UPDATE usuarios SET estado = ? WHERE id_usuario = ?";
        
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idUsuario);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error en UsuarioDao.cambiarEstadoUsuario: " + e.getMessage());
            return false;
        }
    }
}