package Dao;

// 🌟 IMPORT CORREGIDO: Apunta exactamente a tu clase de configuración
import Config.conection; 
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
        // Inicializamos la lista dinámica encargada de recopilar todos los registros devueltos
        List<UsuarioDTO> lista = new ArrayList<>();
        
        // Consulta SQL con INNER JOIN para enlazar el ID de rol del usuario 
        // con su nombre descriptivo en la tabla 'roles'. Usa alias para evitar colisiones.
        String sql = "SELECT u.id_usuario, u.nombre_completo, u.correo_electronico, r.nombre_rol AS rol, u.estado " +
                     "FROM usuarios u " +
                     "INNER JOIN roles r ON u.id_rol = r.id_rol";

        // Bloque try-with-resources moderno: abre automáticamente la conexión, prepara el statement
        // y ejecuta la consulta directa. Al finalizar, destruye los tres recursos de forma controlada.
        try (Connection con = conection.getConnection(); 
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Iteramos fila por fila mientras el ResultSet devuelva registros válidos (rs.next())
            while (rs.next()) {
                // Instanciamos un DTO limpio por cada iteración del bucle
                UsuarioDTO usuario = new UsuarioDTO();
                
                // Extraemos y mapeamos cada columna usando el nombre exacto o el alias definido en el SELECT
                usuario.setIdUsuario(rs.getInt("id_usuario"));
                usuario.setNombreCompleto(rs.getString("nombre_completo"));
                usuario.setCorreoElectronico(rs.getString("correo_electronico"));
                
                // Rescatamos el alias 'rol' calculado a partir del INNER JOIN con la tabla roles
                usuario.setNombreRol(rs.getString("rol"));
                usuario.setEstado(rs.getString("estado")); // Almacena estados como 'ACTIVO' o 'BLOQUEADO'

                // Añadimos el DTO completamente estructurado a la colección principal
                lista.add(usuario);
            }
        } catch (SQLException e) {
            // Imprime fallos relacionales, errores en la sintaxis de las tablas o desconexiones del servidor
            System.err.println("❌ Error en UsuarioDao.obtenerTodosLosUsuarios: " + e.getMessage());
        }
        // Retorna la lista con los usuarios (estará vacía si ocurrió un error o no existen registros)
        return lista;
    }

    /**
     * Modifica el estado de acceso de un usuario (ACTIVO / BLOQUEADO)
     */
    public boolean cambiarEstadoUsuario(int idUsuario, String nuevoEstado) {
        // Sentencia DML estructurada para modificar de forma directa los permisos de un usuario específico
        String sql = "UPDATE usuarios SET estado = ? WHERE id_usuario = ?";
        
        // Try-with-resources para garantizar la apertura y liberación automática de la conexión y del PreparedStatement
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            // Asignación de los parámetros del filtro en base a su índice posicional
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idUsuario);
            
            // executeUpdate() procesa la sentencia UPDATE y devuelve el total de registros modificados en la DB.
            // Si el valor es mayor a 0, significa que el cambio fue exitoso y retorna true.
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            // Captura errores por pérdida de conexión o ID inexistente en la base de datos
            System.err.println("❌ Error en UsuarioDao.cambiarEstadoUsuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca un usuario específico por su ID para pintar su perfil privado.
     */
    public UsuarioDTO obtenerUsuarioPorId(int idUsuario) {
        // Inicializamos el objeto de transferencia en null por seguridad si el ID de usuario no existe
        UsuarioDTO usuario = null;
        
        // Consulta selectiva combinada con un INNER JOIN para recuperar el perfil y su rol de acceso correspondiente
        String sql = "SELECT u.id_usuario, u.nombre_completo, u.correo_electronico, r.nombre_rol AS rol, u.estado " +
                     "FROM usuarios u " +
                     "INNER JOIN roles r ON u.id_rol = r.id_rol " +
                     "WHERE u.id_usuario = ?";

        // Bloque try-with-resources principal para gestionar la conexión y el PreparedStatement
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Asignamos el ID solicitado al parámetro '?' de la consulta condicional
            ps.setInt(1, idUsuario);
            
            // Segundo try-with-resources anidado para controlar de forma segura el cursor de lectura del ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                // Si rs.next() es verdadero, encontramos una coincidencia exacta para este usuario en la DB
                if (rs.next()) {
                    // Instanciamos el DTO para mapear las columnas correspondientes
                    usuario = new UsuarioDTO();
                    usuario.setIdUsuario(rs.getInt("id_usuario"));
                    usuario.setNombreCompleto(rs.getString("nombre_completo"));
                    usuario.setCorreoElectronico(rs.getString("correo_electronico"));
                    usuario.setNombreRol(rs.getString("rol")); // Recupera el nombre de la tabla roles
                    usuario.setEstado(rs.getString("estado"));
                }
            }
        } catch (SQLException e) {
            // Logs de asistencia en consola para identificar fallos relacionales en tiempo de ejecución
            System.err.println("❌ Error en UsuarioDao.obtenerUsuarioPorId: " + e.getMessage());
        }
        // Devuelve el objeto DTO lleno o null si no se localizó el ID del usuario en el sistema
        return usuario;
    }

    /**
     * Permite al usuario actualizar sus propios datos básicos de perfil.
     */
    public boolean actualizarPerfilUsuario(int idUsuario, String nuevoNombre, String nuevoCorreo) {
        // Sentencia DML para sobreescribir los datos personales modificables desde la vista de configuración del perfil
        String sql = "UPDATE usuarios SET nombre_completo = ?, correo_electronico = ? WHERE id_usuario = ?";

        // Apertura y auto-cierre de flujos JDBC usando try-with-resources preventivo
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Asignación de variables parametrizadas respetando estrictamente los tipos de datos de las columnas
            ps.setString(1, nuevoNombre);
            ps.setString(2, nuevoCorreo);
            ps.setInt(3, idUsuario);

            // Retorna verdadero únicamente si se alteró al menos una fila real dentro de la tabla usuarios
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            // Captura errores como duplicación de llaves únicas (por ejemplo, si el nuevo correo ya está registrado por otra cuenta)
            System.err.println("❌ Error en UsuarioDao.actualizarPerfilUsuario: " + e.getMessage());
            return false;
        }
    }
}