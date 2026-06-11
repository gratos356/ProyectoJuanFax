package Controller;

import Dao.ProductoDao;
import Model.ProductoDTO;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@WebServlet(name = "ProductoServlet", urlPatterns = {"/ProductoServlet"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2MB
    maxFileSize = 1024 * 1024 * 10,       // 10MB
    maxRequestSize = 1024 * 1024 * 50     // 50MB
)
public class ProductoServlet extends HttpServlet {

    private ProductoDao productoDao;

    public ProductoServlet() {
        try {
            this.productoDao = new ProductoDao();
        } catch (Exception e) {
            System.err.println("❌ Error crítico: No se pudo instanciar ProductoDao");
            e.printStackTrace();
        }
    }

    // ===================================================================
    // 🔍 PETICIONES GET: Retorna datos en formato JSON Array
    // ===================================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String accion = request.getParameter("accion");
        
        try {
            if ("listarProductos".equals(accion)) {
                int idNegocio = Integer.parseInt(request.getParameter("idNegocio"));
                List<ProductoDTO> lista = productoDao.listarProductosPorNegocio(idNegocio);
                
                // Mapeo manual a formato JSON String estándar compatible con JS
                StringBuilder json = new StringBuilder();
                json.append("[");
                for (int i = 0; i < lista.size(); i++) {
                    ProductoDTO p = lista.get(i);
                    json.append("{");
                    json.append("\"idProducto\":").append(p.getIdProducto()).append(",");
                    json.append("\"idNegocio\":").append(p.getIdNegocio()).append(",");
                    json.append("\"nombre\":\"").append(p.getNombre().replace("\"", "\\\"")).append("\",");
                    json.append("\"precio\":").append(p.getPrecio()).append(",");
                    json.append("\"stock\":").append(p.getStock()).append(",");
                    json.append("\"urlImagen\":\"").append(p.getUrlImagen()).append("\","); 
                    json.append("\"estado\":\"").append(p.getEstado()).append("\""); 
                    json.append("}");
                    if (i < lista.size() - 1) {
                        json.append(",");
                    }
                }
                json.append("]");
                
                out.print(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"success\": false, \"message\": \"Error en GET: " + e.getMessage() + "\"}");
        } finally {
            out.flush();
        }
    }

    // ===================================================================
    // 💾 PETICIONES POST: Inserta, modifica o procesa estados en la DB
    // ===================================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String accion = request.getParameter("accion");

        try {
            if ("crear".equals(accion)) {
                int idNegocio = Integer.parseInt(request.getParameter("idNegocio"));
                String nombre = request.getParameter("nombre");
                double precio = Double.parseDouble(request.getParameter("precio"));
                int stock = Integer.parseInt(request.getParameter("stock"));

                // --- PROCESAMIENTO DE IMAGEN IGUAL A NEGOCIOSERVLET ---
                Part part = request.getPart("imagen_producto");
                String nombreFotoFinal = "default.png";

                if (part != null && part.getSubmittedFileName() != null && !part.getSubmittedFileName().isEmpty()) {
                    String nombreArchivoOriginal = part.getSubmittedFileName();
                    
                    // Marca de tiempo + remoción de espacios
                    nombreFotoFinal = System.currentTimeMillis() + "_" + nombreArchivoOriginal.replaceAll("\\s+", "_");
                    
                    // Tu ruta absoluta exacta en el entorno local de JuanFax
                    String pathDestino = "D:/doc/descktop/ProyectoSena-Juan_David_Ramirez_Saavedra/imagenesJuanFax/";
                    
                    File folder = new File(pathDestino);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    
                    part.write(pathDestino + nombreFotoFinal);
                }

                // Carga al DTO
                ProductoDTO nuevo = new ProductoDTO();
                nuevo.setIdNegocio(idNegocio);
                nuevo.setNombre(nombre);
                nuevo.setPrecio(precio);
                nuevo.setStock(stock);

                boolean guardado = productoDao.registrarProductoConImagen(nuevo, nombreFotoFinal);

                if (guardado) {
                    out.print("{\"success\": true, \"message\": \"Producto guardado correctamente en el inventario.\"}");
                } else {
                    out.print("{\"success\": false, \"message\": \"Error de inserción SQL al registrar producto.\"}");
                }

            } else if ("actualizarStock".equals(accion)) {
                int idProducto = Integer.parseInt(request.getParameter("idProducto"));
                int nuevoStock = Integer.parseInt(request.getParameter("stock"));

                boolean actualizado = productoDao.actualizarStock(idProducto, nuevoStock);

                if (actualizado) {
                    out.print("{\"success\": true, \"message\": \"El stock fue modificado con éxito.\"}");
                } else {
                    out.print("{\"success\": false, \"message\": \"No se pudo actualizar el stock en la base de datos.\"}");
                }         
            }else if ("editarProducto".equals(accion)) {
                int idProducto = Integer.parseInt(request.getParameter("idProducto"));
                String nombre = request.getParameter("nombre");
                double precio = Double.parseDouble(request.getParameter("precio"));
                int stock = Integer.parseInt(request.getParameter("stock")); // Aquí ya integras el stock nuevo

                // 1. Cargamos los datos de texto modificados al DTO
                ProductoDTO prodEditado = new ProductoDTO();
                prodEditado.setIdProducto(idProducto);
                prodEditado.setNombre(nombre);
                prodEditado.setPrecio(precio);
                prodEditado.setStock(stock);

                // 2. Preparar la lista para las nuevas imágenes de la galería
                java.util.ArrayList<String> listaImagenesNuevas = new java.util.ArrayList<>();
                
                // Mantenemos tu ruta absoluta exacta de JuanFax
                String pathDestino = "D:/doc/descktop/ProyectoSena-Juan_David_Ramirez_Saavedra/imagenesJuanFax/";
                
                File folder = new File(pathDestino);
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                // 3. Recorremos el lote de archivos (si el usuario adjuntó fotos adicionales)
                // Usamos el name "imagenes_producto" para diferenciarlo del "imagen_producto" del creador si lo deseas
                for (Part part : request.getParts()) {
                    if (part.getName().equals("imagenes_producto") && part.getSize() > 0) {
                        String nombreArchivoOriginal = part.getSubmittedFileName();
                        
                        // Aplicamos tu misma lógica de timestamps y limpieza de espacios
                        String nombreFotoFinal = System.currentTimeMillis() + "_" + nombreArchivoOriginal.replaceAll("\\s+", "_");
                        
                        // Guardar el archivo físicamente en tu disco D:
                        part.write(pathDestino + nombreFotoFinal);
                        
                        // Añadir el nombre string a la lista
                        listaImagenesNuevas.add(nombreFotoFinal);
                        
                        // Delay para evitar colisiones en los nombres por milisegundos idénticos
                        Thread.sleep(2);
                    }
                }

                // 4. Enviamos el lote al método transaccional del DAO
                boolean editadoConExito = productoDao.modificarProductoCompleto(prodEditado, listaImagenesNuevas);

                if (editadoConExito) {
                    out.print("{\"success\": true, \"message\": \"El producto y sus imágenes fueron actualizados correctamente.\"}");
                } else {
                    out.print("{\"success\": false, \"message\": \"Error en la base de datos al intentar editar el producto.\"}");
                }

            }else if ("baja".equals(accion)) {
                int idProducto = Integer.parseInt(request.getParameter("idProducto"));

                boolean eliminado = productoDao.darDeBajaProducto(idProducto);

                if (eliminado) {
                    out.print("{\"success\": true, \"message\": \"El artículo fue retirado de la lista de ventas.\"}");
                } else {
                    out.print("{\"success\": false, \"message\": \"No se pudo procesar la baja del producto.\"}");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"success\": false, \"message\": \"Error crítico en Servlets: " + e.getMessage() + "\"}");
        } finally {
            out.flush();
        }
    }
}