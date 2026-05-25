document.addEventListener("DOMContentLoaded", () => {
    configurarAlternanciaVistas();
    configurarFormularioRegistro();
});

// Intercambiar formularios agregando y quitando la clase '.oculto'
function configurarAlternanciaVistas() {
    const btnIrARegistro = document.getElementById("enlaceIrARegistro");
    const btnIrALogin = document.getElementById("enlaceIrALogin");
    
    const bloqueLogin = document.getElementById("bloqueLogin");
    const bloqueRegistro = document.getElementById("bloqueRegistro");


    btnIrARegistro.addEventListener("click", (e) => {
        e.preventDefault();
        bloqueLogin.classList.add("oculto");
        bloqueRegistro.classList.remove("oculto");
    });
    btnIrALogin.addEventListener("click", (e) => {
        e.preventDefault();
        bloqueRegistro.classList.add("oculto");
        bloqueLogin.classList.remove("oculto");
    });
    
}

// Envío del registro mediante Fetch
function configurarFormularioRegistro() {
    const formRegistro = document.getElementById("formRegistro");
    if (!formRegistro) return;

    formRegistro.addEventListener("submit", (e) => {
        e.preventDefault();

        const nombre = document.getElementById("regNombre").value.trim();
        const correo = document.getElementById("regCorreo").value.trim();
        const contrasena = document.getElementById("regPass").value.trim();
        const rolSeleccionado = document.getElementById("regRol").value; // Captura el 2 o el 3
        const aceptaTerminosCheckbox = document.getElementById("regTerminos");

        if (rolSeleccionado === "") {
            alert("Por favor, seleccione un rol para continuar.");
            return;
        }

        if (!aceptaTerminosCheckbox.checked) {
            alert("Debe aceptar los términos y condiciones para continuar.");
            return;
        }

        const datos = new URLSearchParams();
        datos.append("accion", "registrarUsuario");
        datos.append("nombreCompleto", nombre);
        datos.append("correoElectronico", correo);
        datos.append("contrasena", contrasena);
        datos.append("idRol", rolSeleccionado); // 🌟 Ahora viaja el rol real elegido por el usuario
        datos.append("aceptaTerminos", "1");

        fetch("LoginServlet", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: datos.toString()
        })
        .then(response => response.json())
        .then(resultado => {
            if (resultado.status === "success") {
                alert(resultado.message || "¡Registro exitoso!");
                formRegistro.reset();
                
                // Volver a la vista de login
                document.getElementById("bloqueRegistro").classList.add("oculto");
                document.getElementById("bloqueLogin").classList.remove("oculto");
            } else {
                alert("Error al registrarse: " + (resultado.message || resultado.error));
            }
        })
        .catch(error => {
            console.error("Error en la petición de registro:", error);
            alert("Error de conexión con el servidor de Juanfax.");
        });
    });
}