package Controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession; // 🌟 IMPORTANTE: Agregada la importación de la sesión
import Dao.PedidoDao;
import Model.PedidoDTO;
import Model.DetallePedidoDTO;
import Model.ReporteVentaDTO;

@WebServlet("/PedidoServlet")
public class PedidoServlet extends HttpServlet {
    
    private final PedidoDao pedidoDao = new PedidoDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String accion = request.getParameter("accion");
        
        if ("historialPedidos".equals(accion)) {
            // 🌟 CONTROL DE SEGURIDAD: Leemos el ID desde la sesión del servidor
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("idUsuario") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Sesión no válida o expirada. Por favor inicie sesión.\"}");
                return;
            }
            
            // Extraemos el idUsuario como un entero seguro
            int idUsuarioLogueado = (int) session.getAttribute("idUsuario");
            // 2. 🌟 NUEVO: Capturamos el idNegocio desde la URL del fetch
            String idNegocioParam = request.getParameter("idNegocio");
            
            int idNegocio = (idNegocioParam != null && !idNegocioParam.isEmpty()) ? Integer.parseInt(idNegocioParam) : 0;
            
            try {
                // 🌟 DAO ADAPTADO: Ahora le pasas el idUsuarioLogueado (int) en lugar del string del nombre
                // Recuerda cambiar la firma de este método en tu PedidoDao a: public List<PedidoDTO> listarHistorialPorUsuario(int idUsuario)
                List<PedidoDTO> historial = pedidoDao.listarHistorialPorUsuario(idUsuarioLogueado, idNegocio);
                
                String jsonResponse = gson.toJson(historial);
                
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(jsonResponse);
                
            } catch (Exception e) {
                System.err.println("Error en el GET de PedidoServlet: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"Error interno al procesar el historial.\"}");
            }
        }else if ("resumenVentasNegocio".equals(accion)) { // 🌟 Corregido el formato del equals seguro
            try {
                // 1. Capturar el parámetro idNegocio enviado por el fetch
                int idNegocio = Integer.parseInt(request.getParameter("idNegocio"));
                
                // 2. Ejecutar la consulta llamando al atributo global ya existente
                List<ReporteVentaDTO> resumen = pedidoDao.obtenerResumenVentasPorNegocio(idNegocio);
                
                // 3. Convertir la lista recuperada a formato JSON String
                String json = gson.toJson(resumen); // Usamos el objeto gson global
                
                // 4. Retornar la respuesta al frontend de manera exitosa
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(json);
                
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"ID de negocio inválido o ausente.\"}");
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"Error en el servidor al generar el reporte.\"}");
            }
        }
        else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Acción no válida o no especificada en el GET.\"}");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String accion = request.getParameter("accion");
        JsonObject jsonRespuesta = new JsonObject();
        
        if ("registrarPedido".equals(accion)) {
            // 🌟 CONTROL DE SEGURIDAD: Validamos sesión antes de procesar inserciones en la BD
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("idUsuario") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                jsonRespuesta.addProperty("success", false);
                jsonRespuesta.addProperty("message", "No autorizado. Inicie sesión.");
                response.getWriter().write(gson.toJson(jsonRespuesta));
                return;
            }
            
            int idUsuarioLogueado = (int) session.getAttribute("idUsuario");
            
            try {
                // 1. Leer el cuerpo JSON de la petición HTTP
                BufferedReader reader = request.getReader();
                JsonObject jsonEntrada = gson.fromJson(reader, JsonObject.class);
                
                // 2. Extraer datos del Pedido (Ya no se extrae ni se pide un 'nombreTurista')
                int idNegocio = jsonEntrada.get("idNegocio").getAsInt();
                
                // Calcular el total mapeando los ítems enviados
                List<DetallePedidoDTO> listaDetalles = gson.fromJson(
                    jsonEntrada.get("items"), 
                    new TypeToken<List<DetallePedidoDTO>>(){}.getType()
                );
                
                double totalCalculado = 0;
                for (DetallePedidoDTO det : listaDetalles) {
                    totalCalculado += (det.getCantidad() * det.getPrecioUnitario());
                }
                
                // Mapear al DTO principal
                PedidoDTO pedido = new PedidoDTO();
                pedido.setIdNegocio(idNegocio);
                pedido.setIdUsuario(idUsuarioLogueado); // 🌟 LLAVE FORÁNEA: Seteamos el ID transaccional seguro
                pedido.setTotal(totalCalculado);
                
                // 3. Invocar al DAO transaccional
                boolean resultado = pedidoDao.registrarPedidoCompleto(pedido, listaDetalles);
                
                if (resultado) {
                    jsonRespuesta.addProperty("success", true);
                    jsonRespuesta.addProperty("message", "Pedido registrado exitosamente.");
                    response.setStatus(HttpServletResponse.SC_CREATED);
                } else {
                    jsonRespuesta.addProperty("success", false);
                    jsonRespuesta.addProperty("message", "No se pudo procesar la transacción en la base de datos.");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                
            } catch (Exception e) {
                jsonRespuesta.addProperty("success", false);
                jsonRespuesta.addProperty("message", "Error al procesar el JSON de entrada: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            
            // Enviar respuesta JSON de vuelta
            response.getWriter().write(gson.toJson(jsonRespuesta));
        }
    }
}