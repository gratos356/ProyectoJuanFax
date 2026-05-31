package Model;

public class UsuarioDTO {
    private int idUsuario;
    private String nombreCompleto;
    private String correoElectronico;
    private String nombreRol;
    private String estado;

    // Constructor vacío obligatorio
    public UsuarioDTO() {
    }

    // Constructor lleno por conveniencia
    public UsuarioDTO(int idUsuario, String nombreCompleto, String correoElectronico, String nombreRol, String estado) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correoElectronico = correoElectronico;
        this.nombreRol = nombreRol;
        this.estado = estado;
    }

    // ========================================================================
    // GETTERS Y SETTERS
    // ========================================================================
    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}