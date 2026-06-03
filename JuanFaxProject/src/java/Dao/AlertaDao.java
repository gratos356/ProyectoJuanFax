package Dao;

import Config.conection;
import Model.AlertaDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class AlertaDao {

    // OBTENER LAS ALERTAS RECIENTES PARA EL DASHBOARD
    public List<AlertaDTO> obtenerAlertasRecientes() {
        // Inicializamos la lista dinámica que almacenará las alertas recuperadas
        List<AlertaDTO> lista = new ArrayList<>();
        
        // Consulta SQL para extraer los datos de las últimas 5 alertas del sistema.
        // Se ordena por 'id_alerta DESC' de forma que los registros más nuevos salgan primero.
        String sql = "SELECT id_alerta, tipo, mensaje, id_usuario, id_negocio, fecha_creacion " +
                     "FROM alertas_sistema ORDER BY id_alerta DESC LIMIT 5";

        // Bloque try-with-resources: Abre y gestiona automáticamente la conexión, 
        // la preparación de la sentencia y la ejecución del ResultSet. 
        // Todos se cerrarán solos al salir del bloque para optimizar el pool de conexiones.
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Iteramos mientras el cursor del ResultSet encuentre registros (hasta un máximo de 5 por el LIMIT)
            while (rs.next()) {
                // Instanciamos un nuevo DTO para mapear la fila actual de la base de datos
                AlertaDTO a = new AlertaDTO();
                
                // Mapeo directo de campos obligatorios que no son nulos en la DB
                a.setIdAlerta(rs.getInt("id_alerta"));
                a.setTipo(rs.getString("tipo")); // Almacena etiquetas de interfaz como 'info', 'success', 'warning'
                a.setMensaje(rs.getString("mensaje"));
                
                // === CONTROL DE NULOS PARA LLAVES FORÁNEAS OPCIONALES ===
                // En Java, rs.getInt() en un campo NULL de la DB devuelve un 0 primitivo por defecto.
                // Para saber si el valor real era NULL o un 0 legítimo, usamos obligatoriamente rs.wasNull().
                
                // Intentamos leer el ID del usuario
                int idU = rs.getInt("id_usuario");
                // Si la última columna leída fue NULL, asignamos null al DTO (que maneja un objeto Integer), si no, pasamos el ID real
                a.setIdUsuario(rs.wasNull() ? null : idU);
                
                // Repetimos la lógica de control defensivo para el ID del negocio
                int idN = rs.getInt("id_negocio");
                a.setIdNegocio(rs.wasNull() ? null : idN);
                
                // Recuperamos la marca de tiempo exacta (TimeStamp) con fecha y hora de la notificación
                a.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                
                // Agregamos el objeto completamente estructurado a la lista
                lista.add(a);
            }
        } catch (SQLException e) {
            // Captura errores de conexión, nombres de columnas erróneos o problemas de red con el servidor de bases de datos
            System.err.println("❌ Error en AlertaDao.obtenerAlertasRecientes: " + e.getMessage());
            e.printStackTrace(); // Muestra la traza exacta del error en los logs del servidor
        }
        // Retorna la colección con las 5 alertas (o vacía si ocurrió un error o no hay registros)
        return lista;
    }

    // INSERTAR UNA ALERTA NUEVA DESDE CUALQUIER PARTE DEL SISTEMA
    public boolean registrarAlerta(String tipo, String mensaje, Integer idUsuario, Integer idNegocio) {
        // Sentencia SQL parametrizada para la inserción limpia de la alerta en el historial del sistema
        String sql = "INSERT INTO alertas_sistema (tipo, mensaje, id_usuario, id_negocio) VALUES (?, ?, ?, ?)";
        
        // Try-with-resources para garantizar la apertura y cierre automático de la conexión y del PreparedStatement
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            // Seteamos los parámetros básicos de texto
            // Aplicamos toLowerCase() para evitar que falle si la columna 'tipo' es un ENUM en MySQL en minúsculas
            ps.setString(1, tipo.toLowerCase()); 
            ps.setString(2, mensaje);
            
            // === CONTROL DE INSERCIÓN DE VALORES NULOS (NULL-SAFE) ===
            // Como los objetos 'Integer' pueden venir en null desde el controlador si la alerta es global (del sistema),
            // validamos el estado del objeto antes de insertarlo en la consulta SQL.
            
            // Si el objeto idUsuario no es nulo, lo pasamos normalmente como entero
            if (idUsuario != null) {
                ps.setInt(3, idUsuario); 
            } else { 
                // Si es nulo, especificamos explícitamente a JDBC que inserte un valor SQL NULL del tipo INTEGER
                ps.setNull(3, Types.INTEGER); 
            }
            
            // Evaluamos de igual forma el objeto idNegocio por seguridad relacional
            if (idNegocio != null) {
                ps.setInt(4, idNegocio); 
            } else { 
                ps.setNull(4, Types.INTEGER); 
            }

            // Ejecutamos la consulta DML (executeUpdate) y evaluamos si afectó filas.
            // Si el resultado es mayor a 0, significa que el INSERT se procesó correctamente y retorna true.
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            // Captura violaciones de restricciones de llave foránea o errores de desbordamiento de caracteres
            System.err.println("❌ Error al registrar alerta: " + e.getMessage());
            return false;
        }
    }
}