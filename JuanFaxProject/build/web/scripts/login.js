document.addEventListener("DOMContentLoaded", () => {
    configurarAlternanciaVistas();
    configurarFormularioRegistro();
    configurarFormularioLogin();
});

// Intercambiar formularios agregando y quitando la clase '.oculto'
function configurarAlternanciaVistas() {
    const btnIrARegistro = document.getElementById("enlaceIrARegistro");
    const btnIrALogin = document.getElementById("enlaceIrALogin");
    
    const bloqueLogin = document.getElementById("bloqueLogin");
    const bloqueRegistro = document.getElementById("bloqueRegistro");

    if (btnIrARegistro && btnIrALogin && bloqueLogin && bloqueRegistro) {
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
}

// ===========================================================================
// EXPRESIONES REGULARES DE CONTROL GLOBAL
// ===========================================================================
const regexCorreo = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
const regexNombre = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]{3,50}$/; // Letras, acentos y espacios (3-50 caracteres)

// Envío del registro mediante Fetch
function configurarFormularioRegistro() {
    const formRegistro = document.getElementById("formRegistro");
    if (!formRegistro) return;

    formRegistro.addEventListener("submit", (e) => {
        e.preventDefault();

        const nombre = document.getElementById("regNombre").value.trim();
        const correo = document.getElementById("regCorreo").value.trim();
        const contrasena = document.getElementById("regPass").value;
        const rolSeleccionado = document.getElementById("regRol").value;
        const terminos = document.getElementById("regTerminos").checked;

        // Validaciones previas en cliente
        if (!regexNombre.test(nombre)) {
            alert("⚠️ El Nombre Completo solo debe contener letras y espacios (entre 3 y 50 caracteres).");
            return;
        }

        if (!regexCorreo.test(correo)) {
            alert("⚠️ El formato del correo electrónico no es válido.");
            return;
        }

        if (contrasena.length < 6) {
            alert("⚠️ La contraseña debe contar con una longitud mínima de 6 caracteres.");
            return;
        }

        if (!terminos) {
            alert("⚠️ Debe aceptar los términos y condiciones para continuar.");
            return;
        }

        // --- SI PASA LAS VALIDACIONES, SE ENVÍA AL SERVLET ---
        const datos = new URLSearchParams();
        datos.append("accion", "registrarUsuario");
        datos.append("nombreCompleto", nombre);
        datos.append("correoElectronico", correo);
        datos.append("contrasena", contrasena);
        datos.append("idRol", rolSeleccionado); // Envía el rol real elegido por el usuario (Turista o Vendedor)
        datos.append("aceptaTerminos", "1");

        fetch("LoginServlet", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: datos.toString()
        })
        .then(response => {
            if (!response.ok) throw new Error("Error en la respuesta de red del servidor.");
            return response.json();
        })
        .then(resultado => {
            if (resultado.status === "success") {
                alert(resultado.message || "¡Registro exitoso!");
                formRegistro.reset();
                
                // Volver automáticamente a la vista de login
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

// Colocar al final de login.js o junto a las demás funciones de configuración
function configurarFormularioLogin() {
    const formLogin = document.getElementById("formulario_login");
    if (!formLogin) return;

    formLogin.addEventListener("submit", (e) => {
        // Seleccionamos los inputs de manera flexible por su tipo o atributos
        const inputCorreo = formLogin.querySelector("input[type='email']") || formLogin.querySelector("[name='correo']");
        const inputPass = formLogin.querySelector("input[type='password']") || formLogin.querySelector("[name='contrasena']");

        if (!inputCorreo || !inputPass) return;

        const correo = inputCorreo.value.trim();
        const contrasena = inputPass.value.trim();

        // A. Validar que no existan campos vacíos o con puros espacios
        if (correo === "" || contrasena === "") {
            e.preventDefault(); // Frena el envío tradicional al servlet
            alert("⚠️ Por favor, ingresa tu correo electrónico y contraseña para continuar.");
            return;
        }

        // B. Reutilizar la expresión regular existente para validar la estructura del correo
        if (!regexCorreo.test(correo)) {
            e.preventDefault();
            alert("⚠️ El formato del correo electrónico ingresado no es válido.");
            return;
        }

        // C. Validación de longitud básica preventiva antes de consultar la BD
        if (contrasena.length < 4) {
            e.preventDefault();
            alert("⚠️ La contraseña es demasiado corta.");
            return;
        }
    });
}