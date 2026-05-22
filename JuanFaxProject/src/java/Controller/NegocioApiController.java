package Controller;

import Dao.NegocioDao;
import Model.NegocioDTO;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/v1/negocios")
public class NegocioApiController extends HttpServlet {

    private final NegocioDao negocioDao = new NegocioDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String categoria = request.getParameter("categoria");

        if (categoria != null && !categoria.isEmpty()) {
            try {
                List<NegocioDTO> lista = negocioDao.obtenerNegociosPorCategoria(categoria);
                out.print(this.gson.toJson(lista));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"error\": \"Incapacidad de procesar la solicitud en el servidor Backend.\"}");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Parámetro 'categoria' requerido.\"}");
        }
        out.flush();
    }
}