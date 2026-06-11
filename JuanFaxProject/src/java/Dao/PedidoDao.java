package Dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import Model.PedidoDTO;
import Model.DetallePedidoDTO;
import Model.ReporteVentaDTO;
import Config.conection;

public class PedidoDao {

    public boolean registrarPedidoCompleto(PedidoDTO pedido, List<DetallePedidoDTO> detalles) {
        Connection conn = null;
        PreparedStatement psPedido = null;
        PreparedStatement psDetalle = null;
        PreparedStatement psStock = null;
        ResultSet rs = null;
        boolean exito = false;

        // 🌟 CAMBIO: Se reemplaza 'nombre_turista' por 'id_usuario' en los campos y los VALUES
        String sqlPedido = "INSERT INTO pedido (id_negocio, id_usuario, total) VALUES (?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario) VALUES (?, ?, ?, ?)";
        String sqlStock = "UPDATE productos SET stock = stock - ? WHERE id_producto = ? AND stock >= ?";
        
        try {
            conn = conection.getConnection();
            conn.setAutoCommit(false); // 1. DETENER EL AUTO-COMMIT

            // 2. Insertar Cabecera y recuperar el ID autogenerado
            psPedido = conn.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS);
            psPedido.setInt(1, pedido.getIdNegocio());
            psPedido.setInt(2, pedido.getIdUsuario()); // 🌟 CAMBIO: Ahora pasamos un entero (int) seguro de la sesión
            psPedido.setDouble(3, pedido.getTotal());
            psPedido.executeUpdate();

            rs = psPedido.getGeneratedKeys();
            int idPedidoGenerado = 0;
            if (rs.next()) {
                idPedidoGenerado = rs.getInt(1);
            } else {
                throw new SQLException("No se pudo obtener el ID del pedido generado.");
            }

            // 3. Preparar el lote (Batch) de los detalles
            psDetalle = conn.prepareStatement(sqlDetalle);
            psStock = conn.prepareStatement(sqlStock);
            
            for (DetallePedidoDTO det : detalles) {
                psDetalle.setInt(1, idPedidoGenerado);
                psDetalle.setInt(2, det.getIdProducto());
                psDetalle.setInt(3, det.getCantidad());
                psDetalle.setDouble(4, det.getPrecioUnitario()); // Congelamos el precio del momento
                psDetalle.addBatch();
                
                psStock.setInt(1, det.getCantidad());       
                psStock.setInt(2, det.getIdProducto());    
                psStock.setInt(3, det.getCantidad());
                int filasAfectadas = psStock.executeUpdate();
    
                if (filasAfectadas == 0) {
                    throw new SQLException("Stock insuficiente o producto no encontrado para el ID: " + det.getIdProducto());
                }
            }

            psDetalle.executeBatch(); // Ejecutar todas las inserciones del detalle juntas
            conn.commit();            // 4. PERSISTIR TODO EN DISCO SI NO HUBO ERRORES
            exito = true;

        } catch (SQLException e) {
            System.err.println("Error transaccional en PedidoDao: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();  // 5. ABORTAR OPERACIÓN COMPLETA
                    System.out.println("Rollback ejecutado con éxito.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            // Cerrar recursos limpiamente
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (psPedido != null) psPedido.close(); } catch (Exception e) {}
            try { if (psDetalle != null) psDetalle.close(); } catch (Exception e) {}
            try { if (psStock != null) psStock.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
        return exito;
    }

    //  CAMBIO: Ahora recibe un int (idUsuario) en vez de un String
    public List<PedidoDTO> listarHistorialPorUsuario(int idUsuario, int idNegocio) {
        List<PedidoDTO> historial = new ArrayList<>();

        //  Condición dinámica: Si pasan idNegocio > 0, agregamos el filtro AND
        String sql = "SELECT p.id_pedido, p.id_negocio, p.fecha_compra, p.total, p.estado_pedido, " +
                     "n.nombre_establecimiento AS nombre_negocio, " + 
                     "d.id_detalle, d.id_producto, d.cantidad, d.precio_unitario, pr.nombre AS nombre_producto " +
                     "FROM pedido p " +
                     "JOIN negocios n ON p.id_negocio = n.id_negocio " + 
                     "JOIN detalle_pedido d ON p.id_pedido = d.id_pedido " +
                     "JOIN productos pr ON d.id_producto = pr.id_producto " +
                     "WHERE p.id_usuario = ? " + 
                     (idNegocio > 0 ? "AND p.id_negocio = ? " : "") +
                     "ORDER BY p.id_pedido DESC";

        try (Connection conn = conection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            if (idNegocio > 0) {
                ps.setInt(2, idNegocio); // Seteamos el negocio si aplica
            }

            try (ResultSet rs = ps.executeQuery()) {
                PedidoDTO pedidoActual = null;
                while (rs.next()) {
                    int idPedido = rs.getInt("id_pedido");
                    if (pedidoActual == null || pedidoActual.getIdPedido() != idPedido) {
                        pedidoActual = new PedidoDTO();
                        pedidoActual.setIdPedido(idPedido);
                        pedidoActual.setIdNegocio(rs.getInt("id_negocio"));
                        pedidoActual.setFechaCompra(rs.getTimestamp("fecha_compra"));
                        pedidoActual.setTotal(rs.getDouble("total"));
                        pedidoActual.setEstadoPedido(rs.getInt("estado_pedido"));
                        pedidoActual.setNombreNegocio(rs.getString("nombre_negocio"));
                        pedidoActual.setItems(new ArrayList<>()); 
                        historial.add(pedidoActual);
                    }

                    DetallePedidoDTO detalle = new DetallePedidoDTO();
                    detalle.setIdDetalle(rs.getInt("id_detalle"));
                    detalle.setIdProducto(rs.getInt("id_producto"));
                    detalle.setCantidad(rs.getInt("cantidad"));
                    detalle.setPrecioUnitario(rs.getDouble("precio_unitario"));
                    detalle.setNombreProducto(rs.getString("nombre_producto")); 

                    pedidoActual.getItems().add(detalle);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar el historial en PedidoDao: " + e.getMessage());
        }
        return historial;
    }
    public List<ReporteVentaDTO> obtenerResumenVentasPorNegocio(int idNegocio) {
        List<ReporteVentaDTO> lista = new ArrayList<>();
        
        // Consulta SQL combinada y agrupada por el id del producto
        String sql = "SELECT p.id_producto, p.nombre, " +
                     "SUM(dp.cantidad) AS total_vendido, " +
                     "SUM(dp.cantidad * dp.precio_unitario) AS total_ingresos " +
                     "FROM detalle_pedido dp " +
                     "JOIN productos p ON dp.id_producto = p.id_producto " +
                     "JOIN pedido ped ON dp.id_pedido = ped.id_pedido " +
                     "WHERE ped.id_negocio = ? AND ped.estado_pedido = 1 " +
                     "GROUP BY p.id_producto, p.nombre " +
                     "ORDER BY total_ingresos DESC";

        try (Connection con = conection.getConnection(); 
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idNegocio);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReporteVentaDTO reporte = new ReporteVentaDTO(
                        rs.getInt("id_producto"),
                        rs.getString("nombre"),
                        rs.getInt("total_vendido"),
                        rs.getDouble("total_ingresos")
                    );
                    lista.add(reporte);
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error en obtenerResumenVentasPorNegocio: " + e.getMessage());
        }
        return lista;
    }
}