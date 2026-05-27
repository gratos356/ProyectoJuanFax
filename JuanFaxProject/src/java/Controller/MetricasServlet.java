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

@WebServlet("/MetricasServlet")
public class MetricasServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String accion = request.getParameter("accion");
        String idNegocioStr = request.getParameter("idNegocio");
        
        if (idNegocioStr == null || idNegocioStr.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        int idNegocio = Integer.parseInt(idNegocioStr);
        
        // 🛠️ REGISTRAR VISTA O CLIC EN LA BASE DE DATOS
        if ("registrarVista".equals(accion) || "registrarClic".equals(accion)) {
            String tipoEvento = "registrarVista".equals(accion) ? "VISTA" : "CLIC_MAPA";
            
            String sql = "INSERT INTO metricas_negocio (id_negocio, tipo_evento) VALUES (?, ?)";
            
            try (Connection con = conection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                
                ps.setInt(1, idNegocio);
                ps.setString(2, tipoEvento);
                ps.executeUpdate();
                
                response.setStatus(HttpServletResponse.SC_OK); // 200 OK
                response.getWriter().print("{\"status\": \"success\"}");
                
            } catch (SQLException e) {
                System.out.println("❌ Error al registrar métrica: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}