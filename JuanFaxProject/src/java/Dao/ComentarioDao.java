
package Dao;

import Config.conection;
import Model.ComentarioDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ComentarioDao {

    public boolean insertarComentario(ComentarioDTO comentario) {
        // Consulta SQL para insertar una nueva calificación. 
        // Se quema 'CALIFICACION' en el tipo de registro y se usa NOW() de MySQL para la marca de tiempo del servidor.
        String sql = "INSERT INTO calificaciones_sanciones "
                   + "(id_usuario, id_negocio, comentario_justificacion, valor_puntuacion, tipo_registro, fecha_registro) "
                   + "VALUES (?, ?, ?, ?, 'CALIFICACION', NOW())";
        
        // Try-with-resources: Abre de forma segura la conexión y prepara la sentencia SQL.
        // Ambos recursos se cerrarán automáticamente al finalizar el bloque, incluso si ocurre una excepción.
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Asignación de los datos encapsulados en el DTO a cada parámetro '?' por su índice posicional
            ps.setInt(1, comentario.getIdUsuario());
            ps.setInt(2, comentario.getIdNegocio());
            ps.setString(3, comentario.getTextoComentario());
            ps.setInt(4, comentario.getCalificacion()); 

            // Ejecuta la sentencia DML (INSERT). Retorna el número de filas afectadas en la base de datos.
            int filas = ps.executeUpdate();
            
            // Si el conteo de filas es mayor a 0, la inserción fue exitosa y devuelve true
            return filas > 0;

        } catch (SQLException e) {
            // Captura errores de sintaxis SQL, restricciones de llave foránea o fallos de conexión
            System.err.println("❌ Error al insertar comentario en el DAO: " + e.getMessage());
            return false;
        }
    }

    // 2. CONSULTA PARA LISTAR (Traer comentarios y estrellas usando INNER JOIN)
    public List<ComentarioDTO> obtenerComentariosPorNegocio(int idNegocio) {
        // Inicializamos la lista dinámica que contendrá los DTOs recuperados
        List<ComentarioDTO> lista = new ArrayList<>();
        
        // Consulta con INNER JOIN para cruzar la calificación con la tabla 'usuarios' 
        // y así obtener el nombre real de la persona que comentó. Se usa alias (AS) para mayor claridad.
        String sql = "SELECT c.id_registro AS id_comentario, "
                   + "c.id_negocio, c.id_usuario, "
                   + "c.comentario_justificacion AS texto_comentario, "
                   + "c.fecha_registro AS fecha_publicacion, "
                   + "c.valor_puntuacion AS calificacion, "
                   + "u.nombre_completo "
                   + "FROM calificaciones_sanciones c "
                   + "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario "
                   + "WHERE c.id_negocio = ? AND c.tipo_registro = 'CALIFICACION' "
                   + "ORDER BY c.fecha_registro DESC"; // Ordena cronológicamente: los más recientes primero

        // Bloque try-with-resources principal para gestionar la conexión y el PreparedStatement
        try (Connection con = Config.conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            // Seteamos el ID del negocio en el filtro del WHERE
            ps.setInt(1, idNegocio);
            
            // Segundo try-with-resources anidado para controlar el ciclo de vida del ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Iteramos fila por fila mientras el cursor del ResultSet encuentre registros (rs.next())
                while (rs.next()) {
                    // Instanciamos un nuevo DTO por cada fila devuelta para evitar sobreescritura de referencias
                    ComentarioDTO c = new ComentarioDTO();
                    
                    // Mapeo de datos extrayendo los valores por el nombre de la columna (o el alias asignado en el SQL)
                    c.setIdComentario(rs.getInt("id_comentario"));
                    c.setIdNegocio(rs.getInt("id_negocio"));
                    c.setIdUsuario(rs.getInt("id_usuario"));
                    c.setTextoComentario(rs.getString("texto_comentario"));
                    
                    // Se usa getTimestamp para no perder las horas, minutos y segundos de la publicación
                    c.setFechaPublicacion(rs.getTimestamp("fecha_publicacion"));
                    
                    // Extraemos el nombre del usuario traído mediante el INNER JOIN
                    c.setNombreUsuario(rs.getString("nombre_completo"));
                    c.setCalificacion(rs.getInt("calificacion"));
                    
                    // Agregamos el DTO completamente estructurado a la colección
                    lista.add(c);
                }
            }
        } catch (SQLException e) {
            // Imprime el stack trace completo en consola si el mapeo o la estructura relacional falla
            System.err.println("❌ Error crítico en ComentarioDao (verificar conexión/tablas): " + e.getMessage());
            e.printStackTrace();
        }
        // Retorna la lista (estará vacía si no hubo comentarios o si ocurrió un error)
        return lista;
    }
}