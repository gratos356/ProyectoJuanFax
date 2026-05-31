package Model;
import java.sql.Timestamp;

public class SuscripcionDTO {
    private int idSuscripcion;
    private int idNegocio;
    private String tipoPlan;
    private Timestamp fechaFin;
    private String estadoPlan;

    // Getters y Setters

    public int getIdSuscripcion() {
        return idSuscripcion;
    }

    public void setIdSuscripcion(int idSuscripcion) {
        this.idSuscripcion = idSuscripcion;
    }

    public int getIdNegocio() {
        return idNegocio;
    }

    public void setIdNegocio(int idNegocio) {
        this.idNegocio = idNegocio;
    }

    public String getTipoPlan() {
        return tipoPlan;
    }

    public void setTipoPlan(String tipoPlan) {
        this.tipoPlan = tipoPlan;
    }

    public Timestamp getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(Timestamp fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getEstadoPlan() {
        return estadoPlan;
    }

    public void setEstadoPlan(String estadoPlan) {
        this.estadoPlan = estadoPlan;
    }
    
}