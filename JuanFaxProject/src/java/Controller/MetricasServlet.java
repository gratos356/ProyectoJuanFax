package Controller;

import Dao.NegocioDao;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import Config.conection;

/**
 * Servlet controlador encargado de interceptar y registrar las interacciones de los usuarios
 * (vistas de perfiles y clics en la ubicación) de manera asíncrona para alimentar las métricas.
 */
@WebServlet("/MetricasServlet")
public class MetricasServlet extends HttpServlet {

    // El método doPost recibe las solicitudes HTTP de tipo POST para insertar datos de eventos con seguridad
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Recuperamos los parámetros enviados desde el cuerpo o la URL de la petición
        String accion = request.getParameter("accion"); // Determina si es una vista o un clic
        String idNegocioStr = request.getParameter("idNegocio"); // ID del negocio en formato de texto
        
        // === VALIDACIÓN DE PARÁMETROS CONTROL DEFENSIVO ===
        // Si el ID del negocio no fue enviado o viene vacío, cortamos el flujo inmediatamente
        if (idNegocioStr == null || idNegocioStr.isEmpty()) {
            // Enviamos un código de estado HTTP 400 (Bad Request) indicando que la petición está incompleta
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return; // Termina la ejecución del método de manera temprana
        }
        
        // Convertimos el parámetro de ID String a un entero primitivo para poder operar en la DB
        int idNegocio = Integer.parseInt(idNegocioStr);
        
        // === PROCESAMIENTO Y REGISTRO DE LA MÉTRICA ===
        // Evaluamos si la acción solicitada corresponde a los flujos que este servlet debe procesar
        if ("registrarVista".equals(accion) || "registrarClic".equals(accion)) {
            
            // Operador ternario: Si la acción es 'registrarVista' asignamos 'VISTA', de lo contrario mapeamos 'CLIC_MAPA'.
            // Esto asegura que guardemos exactamente los strings esperados por el ENUM o restricción de la base de datos.
            String tipoEvento = "registrarVista".equals(accion) ? "VISTA" : "CLIC_MAPA";
            
            // Sentencia SQL parametrizada para realizar la inserción atómica del evento
            String sql = "INSERT INTO metricas_negocio (id_negocio, tipo_evento) VALUES (?, ?)";
            
            // Bloque try-with-resources: Abre de forma segura y cierra en lote la Conexión y el PreparedStatement
            // garantizando la liberación del canal en el pool de conexiones, incluso si la inserción falla.
            try (Connection con = conection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                
                // Inyectamos los parámetros posicionales en los marcadores '?'
                ps.setInt(1, idNegocio);
                ps.setString(2, tipoEvento);
                
                // Ejecutamos la consulta DML (INSERT) en el servidor MySQL
                ps.executeUpdate();
                
                // Asignamos el estado de respuesta HTTP 200 (OK) para indicarle al frontend que el registro fue exitoso
                response.setStatus(HttpServletResponse.SC_OK); 
                
                // Configuramos el flujo de salida de texto y respondemos con un objeto JSON básico confirmando la operación
                response.getWriter().print("{\"status\": \"success\"}");
                
            } catch (SQLException e) {
                // Captura fallos de conectividad con la DB o violaciones de restricciones de llave foránea (si el idNegocio no existe)
                System.out.println("❌ Error al registrar métrica: " + e.getMessage());
                
                // Ante un fallo del servidor, respondemos con un código HTTP 500 (Internal Server Error) 
                // para que el bloque .catch() de JavaScript del lado del cliente pueda gestionar la contingencia
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}