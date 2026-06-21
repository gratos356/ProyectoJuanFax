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
const regexContrasena = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

// ===========================================================================
// HELPER VISUAL: INYECTOR DE ERRORES EN EL DOM
// ===========================================================================
function mostrarErrorFormulario(formulario, mensaje) {
    // Limpiar cualquier alerta previa dentro de este formulario específico
    const errorPrevio = formulario.querySelector(".feedback-error-inline");
    if (errorPrevio) {
        errorPrevio.remove();
    }

    // Crear el contenedor adaptado para el ecosistema de componentes Boxicons
    const contenedorError = document.createElement("div");
    contenedorError.className = "feedback-error-inline";
    contenedorError.innerHTML = `<i class='bx bx-error-circle'></i> <span>${mensaje}</span>`;

    // Buscar el botón del formulario para posicionar el error exactamente arriba de él
    const botonEnvio = formulario.querySelector("button[type='submit']") || formulario.querySelector(".formulario-login__boton");
    
    if (botonEnvio) {
        formulario.insertBefore(contenedorError, botonEnvio);
    } else {
        formulario.appendChild(contenedorError);
    }

    // Desvanecimiento controlado automático tras 4 segundos
    setTimeout(() => {
        contenedorError.style.opacity = "0";
        contenedorError.style.transform = "translateY(-4px)";
        setTimeout(() => contenedorError.remove(), 300);
    }, 4000);
}


// Envío del registro asíncrono (Evita la pantalla negra)
function configurarFormularioRegistro() {
    const formRegistro = document.querySelector("#bloqueRegistro form");
    if (!formRegistro) return;

    // Forzar desactivación de alertas nativas de forma segura
    formRegistro.setAttribute("novalidate", "true");

    formRegistro.addEventListener("submit", async (e) => { // 1. Agregamos async aquí
        // 1. Captura de elementos del DOM
        const inputNombre = formRegistro.querySelector("[name='nombre_completo']") || formRegistro.querySelector("input[type='text']");
        const inputCorreo = formRegistro.querySelector("[name='correo']") || formRegistro.querySelector("input[type='email']");
        const inputPass = formRegistro.querySelector("[name='contrasena']") || formRegistro.querySelector("input[type='password']");
        const selectRol = formRegistro.querySelector("select");
        const checkboxTerminos = document.getElementById("regTerminos");

        // 🔍 CONTROL DE SEGURIDAD
        if (!inputNombre || !inputCorreo || !inputPass || !selectRol || !checkboxTerminos) {
            e.preventDefault(); 
            console.error("❌ Error en Juanfax: No se pudo mapear algún input en el HTML.", {
                inputNombre, inputCorreo, inputPass, selectRol, checkboxTerminos
            });
            alert("⚠️ Error interno en el formulario de registro. Revisa la consola del desarrollador (F12).");
            return;
        }

        // Extraer valores limpios
        const nombre = inputNombre.value.trim();
        const correo = inputCorreo.value.trim();
        const contrasena = inputPass.value.trim();
        const rol = selectRol.value;

        // 2. Batería de validaciones secuenciales en orden estricto
        
        // A. Campos vacíos principales
        if (nombre === "" || correo === "" || contrasena === "") {
            e.preventDefault();
            mostrarErrorFormulario(formRegistro, "Todos los campos son obligatorios. Por favor, completa el formulario.");
            return;
        }

        // B. Formato del Nombre (Regex)
        if (!regexNombre.test(nombre)) {
            e.preventDefault();
            mostrarErrorFormulario(formRegistro, "El nombre no es válido. Solo se permiten letras y espacios.");
            return;
        }

        // C. Formato del Correo (Regex)
        if (!regexCorreo.test(correo)) {
            e.preventDefault();
            mostrarErrorFormulario(formRegistro, "El formato del correo electrónico no es válido debe llevar @.");
            return;
        }

        // D. Longitud de contraseña
        if (!regexContrasena.test(contrasena) && contrasena.length < 8) {
            e.preventDefault();
            mostrarErrorFormulario(formRegistro, "La contraseña debe tener al menos 8 caracteres, letras y números.");
            return;
        }

        // E. Selección de Rol válida
        if (rol === "" || rol === null) {
            e.preventDefault();
            mostrarErrorFormulario(formRegistro, "Por favor, selecciona un rol (Cliente o Vendedor) para continuar.");
            return;
        }

        // F. Checkbox de términos obligatorios
        if (!checkboxTerminos.checked) {
            e.preventDefault();
            mostrarErrorFormulario(formRegistro, "Debes aceptar los términos y condiciones para poder registrarte.");
            return;
        }
        
        // ===========================================================================
        // NUEVO: INTERCEPCIÓN DEL ENVÍO AL SERVLET VIA FETCH
        // ===========================================================================
        e.preventDefault(); // Evitamos por completo que la página viaje y se ponga negra

        // Preparamos los parámetros exactamente como los espera recibir el Servlet de Java
        const formData = new URLSearchParams();
        formData.append("accion", "registrarUsuario");
        formData.append("nombreCompleto", nombre);
        formData.append("correoElectronico", correo);
        formData.append("contrasena", contrasena);
        formData.append("idRol", rol);

        try {
            // Realizamos la petición al servlet de manera silenciosa
            const respuesta = await fetch("LoginServlet", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: formData.toString()
            });

            // Parseamos el JSON ("{"status":"error", ...}")
            const resultado = await respuesta.json();

            if (resultado.status === "error") {
                // ¡Aquí ocurre la magia! Reutilizamos tu función inyectora de errores estética
                mostrarErrorFormulario(formRegistro, resultado.message);
            } else {
                // Si el registro es exitoso, puedes redirigirlo a su panel o login
                alert("¡Registro exitoso! Ya puedes iniciar sesión.");
                window.location.reload(); 
            }

        } catch (error) {
            console.error("Error en la comunicación con el servidor:", error);
            mostrarErrorFormulario(formRegistro, "No se pudo conectar con el servidor. Inténtalo más tarde.");
        }
    });
}

// Envío del login
function configurarFormularioLogin() {
    const formLogin = document.getElementById("formulario_login");
    if (!formLogin) return;

    formRegistro.setAttribute("novalidate", "true");

    formLogin.addEventListener("submit", (e) => {
        const inputCorreo = formLogin.querySelector("input[type='email']") || formLogin.querySelector("[name='correo']");
        const inputPass = formLogin.querySelector("input[type='password']") || formLogin.querySelector("[name='contrasena']");

        if (!inputCorreo || !inputPass) return;

        const correo = inputCorreo.value.trim();
        const contrasena = inputPass.value.trim();

        if (correo === "" || contrasena === "") {
            e.preventDefault();
            mostrarErrorFormulario(formLogin, "Por favor, ingresa tu correo electrónico y contraseña para continuar.");
            return;
        }

        if (!regexCorreo.test(correo)) {
            e.preventDefault();
            mostrarErrorFormulario(formLogin, "El formato del correo electrónico ingresado no es válido.");
            return;
        }

        if (!regexContrasena.test(contrasena)) {
            e.preventDefault();
            mostrarErrorFormulario(formLogin, "Credenciales inválidas. Recuerda que la contraseña debe tener al menos 8 caracteres, letras y números.");
            return;
        }
    });
}