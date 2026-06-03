package Controller;

import Dao.NegocioDao;
import Model.NegocioDTO;
import java.io.File;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

/**
 * Servlet controlador encargado de procesar la creación y registro de nuevos establecimientos.
 * Gestiona de manera unificada la carga de archivos multimedia y los metadatos comerciales del negocio.
 */
@WebServlet(name = "NegocioServlet", urlPatterns = {"/NegocioServlet"})
// 📦 CONFIGURACIÓN MULTIPART: Habilita al servlet para procesar formularios con codificación 'multipart/form-data'
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2, // 2MB: Umbral en el que el archivo se guardará temporalmente en disco antes de procesarse
    maxFileSize = 1024 * 1024 * 10,       // 10MB: Tamaño máximo permitido para un archivo individual (la foto de portada)
    maxRequestSize = 1024 * 1024 * 50     // 50MB: Tamaño máximo total permitido para la solicitud entera (formulario + archivos)
)
public class NegocioServlet extends HttpServlet {

    // El método doPost recibe la información del formulario de creación por motivos de seguridad y manejo de bytes masivos
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // === CONTROL DE ACCESO SEGURIDAD BASADA EN SESIÓN ===
        // Intentamos recuperar la sesión actual del cliente. Pasamos 'false' para evitar que se cree una sesión nueva si no existe.
        HttpSession session = request.getSession(false);
        
        // Control defensivo: Si no hay sesión iniciada o el atributo de identificación es nulo, se rechaza la operación
        if (session == null || session.getAttribute("idUsuario") == null) {
            // Desvía al cliente a la página principal del sistema alertando la falta de credenciales activas
            response.sendRedirect("../index.html?error=SinSesion");
            return; // Detiene la ejecución temprana del servlet para proteger la capa DAO
        }
        
        // Extraemos el identificador numérico único del vendedor desde la memoria de la sesión
        int idVendedor = (int) session.getAttribute("idUsuario");

        // Bloque general try-catch para capturar errores de parseo numérico, desbordamiento de archivos o fallos en el disco duro
        try {
            
            // === LECTURA DE CAMPOS ALFANUMÉRICOS DEL FORMULARIO ===
            String nombreNegocio = request.getParameter("nombre");
            String nit = request.getParameter("nit");
            // Conversión explícita: El formulario envía texto, transformamos a entero primitivo para la base de datos
            int idCategoria = Integer.parseInt(request.getParameter("categoria"));
            String descripcion = request.getParameter("descripcion");
            
            // Captura del plan comercial seleccionado por el usuario en el select del frontend
            String tipoPlan = request.getParameter("tipoPlan"); 

            // Conversión explícita de coordenadas de geolocalización a tipos de datos Double con precisión decimal estricta
            double latitud = Double.parseDouble(request.getParameter("latitud"));
            double longitud = Double.parseDouble(request.getParameter("longitud"));

            // === GESTIÓN Y ALMACENAMIENTO DE LA IMAGEN (FILE UPLOAD) ===
            // Recuperamos el flujo binario del archivo subido a través del input de tipo 'file' llamado "foto"
            Part part = request.getPart("foto");
            // Definimos un valor de contingencia por si el usuario decide registrarse sin subir una foto personalizada
            String nombreFotoFinal = "default-negocio.jpg";

            // Validamos que el objeto part exista, contenga un nombre de archivo válido y no sea una cadena vacía
            if (part != null && part.getSubmittedFileName() != null && !part.getSubmittedFileName().isEmpty()) {
                String nombreArchivoOriginal = part.getSubmittedFileName();
                
                // REGLA DE SEGURIDAD Y CONCURRENCIA: 
                // Añadimos System.currentTimeMillis() (marca de tiempo en milisegundos) al inicio para evitar que dos usuarios
                // que suban un archivo con el mismo nombre (ej. "portada.jpg") se sobreescriban entre sí.
                // Reemplazamos todos los espacios en blanco por guiones bajos para evitar rutas rotas en el servidor web.
                nombreFotoFinal = System.currentTimeMillis() + "_" + nombreArchivoOriginal.replaceAll("\\s+", "_");
                
                // Ruta absoluta de almacenamiento físico local configurada para el entorno de desarrollo Juanfax
                String pathDestino = "D:/doc/descktop/ProyectoSena-Juan_David_Ramirez_Saavedra/imagenesJuanFax/";

                // Verificación del entorno de archivos local
                File folder = new File(pathDestino);
                if (!folder.exists()) {
                    // Si la ruta de carpetas no existe en el disco D, la creamos dinámicamente con mkdirs()
                    folder.mkdirs(); 
                }
                
                // Escribe y guarda físicamente los bytes del archivo cargado dentro de la ruta especificada
                part.write(pathDestino + nombreFotoFinal);
            }
      
            // === INSTANCIACIÓN Y CARGA DE DATOS AL OBJETO DE TRANSFERENCIA (DTO) ===
            Model.NegocioDTO nuevoNegocio = new Model.NegocioDTO();
            nuevoNegocio.setNombreEstablecimiento(nombreNegocio);
            nuevoNegocio.setUrl_imagen(nombreFotoFinal); // Guardamos únicamente el nombre final formateado, no la ruta absoluta

            // === COMUNICACIÓN CON LA CAPA DE PERSISTENCIA (DAO) ===
            Dao.NegocioDao negocioDao = new Dao.NegocioDao();
            // Invocamos la transacción multi-tabla del DAO pasando los parámetros recolectados y el DTO
            boolean exito = negocioDao.registrarNegocio(nuevoNegocio, idVendedor, idCategoria, nit, descripcion, latitud, longitud, tipoPlan);

            // === RESPUESTA Y REDIRECCIONAMIENTO HTTP AL FRONTEND ===
            if (exito) {
                // Si la base de datos procesó el lote completo con commit exitoso, redirige confirmando la operación
                response.sendRedirect("vistas/misNegocios.html?registro=ok");
            } else {
                // Si ocurrió un rollback o una falla lógica interna en las consultas SQL del DAO
                response.sendRedirect("vistas/misNegocios.html?error=SqlError");
            }

        } catch (Exception e) {
            // Captura de contingencias inesperadas (Ej. NumberFormatException en el parseo, caídas del disco duro al escribir)
            System.err.println("🚨 ERROR EN SERVLET: " + e.getMessage());
            e.printStackTrace(); // Imprime la traza completa de la excepción en la consola de depuración del IDE
            
            // Redirige al panel del vendedor notificando que la transacción falló por una excepción crítica de software
            response.sendRedirect("vistas/misNegocios.html?error=Excepcion");
        }
    }
}