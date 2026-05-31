package Dao;

import Config.conection;
import Model.AlertaDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlertaDao {

    // 🔔 MÉTODO 1: OBTENER LAS ALERTAS RECIENTES PARA EL DASHBOARD
    public List<AlertaDTO> obtenerAlertasRecientes() {
        List<AlertaDTO> lista = new ArrayList<>();
        String sql = "SELECT id_alerta, tipo, mensaje, id_usuario, id_negocio, fecha_creacion " +
                     "FROM alertas_sistema ORDER BY id_alerta DESC LIMIT 5";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                AlertaDTO a = new AlertaDTO();
                a.setIdAlerta(rs.getInt("id_alerta"));
                a.setTipo(rs.getString("tipo")); // 'info', 'success', etc.
                a.setMensaje(rs.getString("mensaje"));
                
                // Control de nulos para los IDs opcionales
                int idU = rs.getInt("id_usuario");
                a.setIdUsuario(rs.wasNull() ? null : idU);
                
                int idN = rs.getInt("id_negocio");
                a.setIdNegocio(rs.wasNull() ? null : idN);
                
                a.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                lista.add(a);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en AlertaDao.obtenerAlertasRecientes: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    // 📝 MÉTOPDO 2: INSERTAR UNA ALERTA NUEVA DESDE CUALQUIER PARTE DEL SISTEMA
    public boolean registrarAlerta(String tipo, String mensaje, Integer idUsuario, Integer idNegocio) {
        String sql = "INSERT INTO alertas_sistema (tipo, mensaje, id_usuario, id_negocio) VALUES (?, ?, ?, ?)";
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, tipo.toLowerCase()); // Nos aseguramos de que vaya en minúsculas para el ENUM
            ps.setString(2, mensaje);
            
            if (idUsuario != null) ps.setInt(3, idUsuario); else ps.setNull(3, Types.INTEGER);
            if (idNegocio != null) ps.setInt(4, idNegocio); else ps.setNull(4, Types.INTEGER);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error al registrar alerta: " + e.getMessage());
            return false;
        }
    }
}