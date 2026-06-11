package Model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PedidoDTO {
    private int idPedido;
    private int idNegocio;
    private int idUsuario;
    private Timestamp fechaCompra;
    private double total;
    private int estadoPedido;

    private String nombreNegocio;
    private List<DetallePedidoDTO> items = new ArrayList<>();
    // Constructores
    public PedidoDTO() {}

    // Getters y Setters

    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public int getIdNegocio() {
        return idNegocio;
    }

    public void setIdNegocio(int idNegocio) {
        this.idNegocio = idNegocio;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    

    public Timestamp getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(Timestamp fechaCompra) {
        this.fechaCompra = fechaCompra;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public int getEstadoPedido() {
        return estadoPedido;
    }

    public void setEstadoPedido(int estadoPedido) {
        this.estadoPedido = estadoPedido;
    }

    public List<DetallePedidoDTO> getItems() {
        return items;
    }

    public void setItems(List<DetallePedidoDTO> items) {
        this.items = items;
    }

    public String getNombreNegocio() {
        return nombreNegocio;
    }

    public void setNombreNegocio(String nombreNegocio) {
        this.nombreNegocio = nombreNegocio;
    }
    
    
    
}