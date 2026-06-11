package Dao;

import Config.conection;
import Model.ProductoDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDao {

    public List<ProductoDTO> listarProductosPorNegocio(int idNegocio) {
        List<ProductoDTO> lista = new ArrayList<>();
        // 🌟 Añadimos 'estado' al SELECT
        String sql = "SELECT p.id_producto, p.id_negocio, p.nombre, p.precio, p.stock, p.estado, i.url_imagen " +
                 "FROM productos p " +
                 "LEFT JOIN imagenes_productos i ON p.id_producto = i.id_producto AND i.es_principal = 1 " +
                 "WHERE p.id_negocio = ? AND p.estado = 1";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idNegocio);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductoDTO p = new ProductoDTO();
                    p.setIdProducto(rs.getInt("id_producto"));
                    p.setIdNegocio(rs.getInt("id_negocio"));
                    p.setNombre(rs.getString("nombre"));
                    p.setPrecio(rs.getDouble("precio"));
                    p.setStock(rs.getInt("stock"));
                    p.setUrlImagen(rs.getString("url_imagen"));
                    p.setEstado(rs.getString("estado")); // 🌟 ASIGNAMOS EL ESTADO REAL DE LA BD
                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en ProductoDao.listarProductosPorNegocio: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    // 💾 2. Registrar un producto nuevo vinculando su imagen de disco
    public boolean registrarProductoConImagen(ProductoDTO producto, String urlImagen) {
        // 📝 Separamos las consultas para tus dos tablas
        String sqlProducto = "INSERT INTO productos (id_negocio, nombre, precio, stock, estado) VALUES (?, ?, ?, ?, 1)";
        String sqlImagen = "INSERT INTO imagenes_productos (id_producto, url_imagen, es_principal) VALUES (?, ?, 1)";

        Connection con = null;
        PreparedStatement psProd = null;
        PreparedStatement psImg = null;
        ResultSet generatedKeys = null;

        try {
            con = conection.getConnection();
            con.setAutoCommit(false); // 🛡️ Iniciamos transacción para asegurar consistencia multi-tabla

            // 1️⃣ INSERTAR EN LA TABLA PRODUCTOS (Solicitando el ID generado)
            psProd = con.prepareStatement(sqlProducto, Statement.RETURN_GENERATED_KEYS);
            psProd.setInt(1, producto.getIdNegocio());
            psProd.setString(2, producto.getNombre());
            psProd.setDouble(3, producto.getPrecio());
            psProd.setInt(4, producto.getStock());

            int filasProducto = psProd.executeUpdate();
            if (filasProducto == 0) {
                throw new SQLException("No se pudo registrar la información base del producto.");
            }

            // 2️⃣ OBTENER EL ID_PRODUCTO GENERADO POR EL AUTO_INCREMENT
            int idProductoGenerado = 0;
            generatedKeys = psProd.getGeneratedKeys();
            if (generatedKeys.next()) {
                idProductoGenerado = generatedKeys.getInt(1);
            } else {
                throw new SQLException("Error crítico: No se pudo recuperar el ID generado para el producto.");
            }

            // 3️⃣ INSERTAR EN LA TABLA IMAGENES_PRODUCTOS (Vinculado al ID anterior)
            psImg = con.prepareStatement(sqlImagen);
            psImg.setInt(1, idProductoGenerado);
            psImg.setString(2, urlImagen); // Ej: 1717436000_papa.png
            psImg.executeUpdate();

            // 🏁 Si ambas operaciones fueron exitosas, confirmamos los cambios en la BD
            con.commit();
            return true;

        } catch (SQLException e) {
            // ↩️ Si algo falla en cualquier punto, deshacemos todo el lote para no dejar datos corruptos
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            System.err.println("❌ Error en ProductoDao.registrarProductoConImagen: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // 🔒 Cerramos todos los recursos abiertos de forma segura
            try { if (generatedKeys != null) generatedKeys.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (psProd != null) psProd.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (psImg != null) psImg.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (con != null) con.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // 🔄 3. Actualizar únicamente el stock de forma asíncrona
    public boolean actualizarStock(int idProducto, int nuevoStock) {
        String sql = "UPDATE productos SET stock = ? WHERE id_producto = ?";
        
        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, nuevoStock);
            ps.setInt(2, idProducto);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error en ProductoDao.actualizarStock: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ⚠️ 4. Borrado lógico o dar de baja un artículo
    public boolean darDeBajaProducto(int idProducto) {
        String sql = "UPDATE productos SET estado = ? WHERE id_producto = ?"; // O el nombre exacto de tu tabla
        try (Connection con = conection.getConnection(); // Tu método de conexión
             PreparedStatement ps = con.prepareStatement(sql)) {

            // ❌ ERROR ANTERIOR: ps.setString(1, "INACTIVO");
            // 👇 SOLUCIÓN: Envía el número entero 0 (que representa Inactivo/Baja)
            ps.setInt(1, 0); 
            ps.setInt(2, idProducto);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error en ProductoDao.darDeBajaProducto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean modificarProductoCompleto(ProductoDTO producto, List<String> nuevasUrlsImagenes) {
        // 📝 1. Actualiza todos los campos básicos (incluyendo stock y nombre)
        String sqlProducto = "UPDATE productos SET nombre = ?, precio = ?, stock = ? WHERE id_producto = ?";

        // 📝 2. Query para verificar si el producto ya tiene alguna imagen principal
        String sqlCheckPrincipal = "SELECT COUNT(*) FROM imagenes_productos WHERE id_producto = ? AND es_principal = 1";

        // 📝 3. Query para insertar las nuevas imágenes de la galería
        String sqlInsertImagen = "INSERT INTO imagenes_productos (id_producto, url_imagen, es_principal) VALUES (?, ?, ?)";

        Connection con = null;
        PreparedStatement psProd = null;
        PreparedStatement psCheck = null;
        PreparedStatement psImg = null;
        ResultSet rsCheck = null;

        try {
            con = conection.getConnection();
            con.setAutoCommit(false); // 🛡️ Transacción para que si algo falla, no se guarde nada a medias

            // 1️⃣ ACTUALIZAR DATOS DEL PRODUCTO (Nombre, Precio, Stock)
            psProd = con.prepareStatement(sqlProducto);
            psProd.setString(1, producto.getNombre());
            psProd.setDouble(2, producto.getPrecio());
            psProd.setInt(3, producto.getStock()); // 👈 Integrado el cambio de stock
            psProd.setInt(4, producto.getIdProducto());
            psProd.executeUpdate();

            // 2️⃣ AGREGAR LAS NUEVAS IMÁGENES A LA GALERÍA (Si es que subió alguna)
            if (nuevasUrlsImagenes != null && !nuevasUrlsImagenes.isEmpty()) {

                // Averiguar si ya existe una imagen principal
                psCheck = con.prepareStatement(sqlCheckPrincipal);
                psCheck.setInt(1, producto.getIdProducto());
                rsCheck = psCheck.executeQuery();

                boolean yaTienePrincipal = false;
                if (rsCheck.next()) {
                    yaTienePrincipal = rsCheck.getInt(1) > 0;
                }

                // Insertar cada una de las nuevas imágenes de la lista
                psImg = con.prepareStatement(sqlInsertImagen);
                for (int i = 0; i < nuevasUrlsImagenes.size(); i++) {
                    String url = nuevasUrlsImagenes.get(i);

                    psImg.setInt(1, producto.getIdProducto());
                    psImg.setString(2, url);

                    // Si el producto no tenía foto antes, la primera de este lote será la principal (1).
                    // Si ya tenía, todas las nuevas entran como secundarias (0) para la galería.
                    if (!yaTienePrincipal && i == 0) {
                        psImg.setInt(3, 1); 
                    } else {
                        psImg.setInt(3, 0); 
                    }

                    psImg.addBatch(); // Las acumulamos para ejecutarlas juntas eficientemente
                }
                psImg.executeBatch(); // Ejecuta todas las inserciones de imágenes de un solo golpe
            }

            con.commit(); // 🏁 Guardado definitivo en la BD
            return true;

        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            System.err.println("❌ Error en ProductoDao.modificarProductoCompleto: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // Cierre seguro de recursos
            try { if (rsCheck != null) rsCheck.close(); } catch (SQLException e) {}
            try { if (psProd != null) psProd.close(); } catch (SQLException e) {}
            try { if (psCheck != null) psCheck.close(); } catch (SQLException e) {}
            try { if (psImg != null) psImg.close(); } catch (SQLException e) {}
            try { if (con != null) con.close(); } catch (SQLException e) {}
        }
    }
}