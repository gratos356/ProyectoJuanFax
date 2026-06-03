const expandingCards = document.querySelectorAll(".expanding-card")

// Variables de control global
let destinos = [];
let indiceBase = 0;

// ====================================================================
// 1. SOLICITAR LOS DATOS REALES AL SERVLET DE LOGIN (HERO)
// ====================================================================
async function cargarDatosHero() {
    try {
        // Le pegamos al servlet pasándole el parámetro ?accion=carrusel
        const respuesta = await fetch('../LoginServlet?accion=carrusel'); 
        
        if (!respuesta.ok) {
            throw new Error("El servidor respondió con un error de estado.");
        }
        
        destinos = await respuesta.json(); 
        console.log("¡Conexión exitosa! Datos cargados de la base de datos:", destinos);
        
        // Si la base de datos nos arrojó negocios, pintamos el carrusel
        if (destinos && destinos.length > 0) {
            renderizarCarruselHero();
            iniciarAutoplay();
        } else {
            console.warn("La consulta devolvió una lista vacía.");
        }
    } catch (error) {
        console.error("Error al conectar con el backend o procesar la respuesta:", error);
    }
}

// ====================================================================
// 2. RENDERIZAR Y ROTAR LAS 5 CAJAS SIMULTÁNEAMENTE
// ====================================================================
function renderizarCarruselHero() {
    const cajas = document.querySelectorAll("#HeroContent .banner-hero");
    
    if (cajas.length === 0 || destinos.length === 0) return;

    cajas.forEach((caja, posicionFisica) => {
        const indiceDato = (indiceBase + posicionFisica) % destinos.length;
        const destino = destinos[indiceDato];
        
        // VALIDACIÓN INTELIGENTE DE RUTA DE IMAGEN
        let rutaFinalImagen = "";
        if (destino.urlImagen.startsWith("http://") || destino.urlImagen.startsWith("https://")) {
            rutaFinalImagen = destino.urlImagen;
        } else {
            rutaFinalImagen = `../imagenes/${destino.urlImagen}`;
        }
        
        // Inyectamos el fondo con la ruta corregida
        caja.style.backgroundImage = `linear-gradient(rgba(3, 12, 26, 0.65), rgba(3, 12, 26, 0.85)), url('${rutaFinalImagen}')`;
        caja.style.backgroundSize = "cover";
        caja.style.backgroundPosition = "center";
        caja.style.transition = "background-image 0.5s ease-in-out";

        // Cambiamos el texto dinámicamente
        caja.innerHTML = `
            <div class="contenido-interno-hero">
                <h3 class="hero-title">
                    ${destino.nombreEstablecimiento}
                </h3>
            </div>
        `;

        // REDIRECCIÓN DIRECTA AL HACER CLIC EN UNA TARJETA DEL HERO
        caja.onclick = () => {
            // ESTO ES LO QUE NECESITO QUE MIRES EN TU CONSOLA (F12)
            console.log("DEBUG: Objeto 'destino' completo:", destino);
            console.log("DEBUG: Valor de 'destino.idNegocio':", destino.idNegocio);
            
            // Si al hacer clic aquí, el console.log dice 'undefined' o '0',
            // entonces el error está en el archivo donde se llenó el array 'destinos'.
            window.location.href = `usuarioInformacionNegocio.html?id=${destino.idNegocio}`;
        };
    });
}

// ====================================================================
// 3. MOVIMIENTO DE LOS BOTONES DEL HERO
// ====================================================================
function moverDerecha() {
    if (destinos.length === 0) return;
    indiceBase = (indiceBase + 1) % destinos.length;
    renderizarCarruselHero();
}

function moverIzquierda() {
    if (destinos.length === 0) return;
    indiceBase = (indiceBase - 1 + destinos.length) % destinos.length;
    renderizarCarruselHero();
}

// ====================================================================
// 4. AUTOMATIZACIÓN DEL HERO (Autoplay)
// ====================================================================
let autoplayTimer;
function iniciarAutoplay() {
    clearInterval(autoplayTimer);
    autoplayTimer = setInterval(moverDerecha, 5000);
}

// ====================================================================
// 5. INICIALIZADOR AL CARGAR LA PÁGINA
// ====================================================================
document.addEventListener("DOMContentLoaded", () => {
    // Disparamos la lectura a la base de datos
    cargarDatosHero();
    
    // Escuchamos tus botones reales por sus IDs
    const btnIzquierda = document.getElementById("Boton_Izquierda");
    const btnDerecha = document.getElementById("Boton_Derecha");

    if (btnIzquierda) {
        btnIzquierda.addEventListener("click", () => {
            moverIzquierda();
            iniciarAutoplay(); 
        });
    }

    if (btnDerecha) {
        btnDerecha.addEventListener("click", () => {
            moverDerecha();
            iniciarAutoplay();
        });
    }
});

// ====================================================================
// 6. LÓGICA DE TARJETAS EXPANDIBLES (EXPANDING CARDS)
// ====================================================================
expandingCards.forEach((card) => {
    card.addEventListener("click", () => {
        const isExpanded = card.classList.toggle("cardExpandida");
        
        // Remover títulos cuando se expanda la card
        const titulo = card.querySelector(".titulos");
        if (titulo) titulo.classList.toggle("titulosDisable", isExpanded);

        if (isExpanded) {
            // Obtenemos el nombre de la categoría asignada a esta tarjeta
            const nombreCategoria = card.querySelector("h2") ? card.querySelector("h2").innerText.trim() : "";
            
            // Añadimos un mensaje temporal mientras carga
            const cargando = document.createElement("div");
            cargando.classList.add("targets-expanding", "loading-text");
            cargando.innerText = "Cargando...";
            cargando.style.color = "#fff";
            card.appendChild(cargando);

            // Hacemos la petición en vivo a tu LoginServlet pasándole la categoría
            fetch(`../LoginServlet?accion=negociosPorCategoria&categoria=${encodeURIComponent(nombreCategoria)}`)
                .then(response => response.json())
                .then(negocios => {
                    // Quitamos el mensaje de "Cargando..."
                    const tempLoading = card.querySelector(".loading-text");
                    if (tempLoading) tempLoading.remove();

                    if (negocios.length === 0) {
                        const noData = document.createElement("div");
                        noData.classList.add("targets-expanding");
                        noData.innerHTML = `<span style="color: #aaa; font-style: italic;">No hay negocios</span>`;
                        card.appendChild(noData);
                        return;
                    }

                    // Recorremos los negocios devueltos por el Servlet
                    negocios.forEach(neg => {
                        const ExpandingTarget = document.createElement("div");
                        ExpandingTarget.classList.add("targets-expanding");

                        const rutaImagen = neg.url_imagen || neg.urlImagen; 

                            // VALIDACIÓN SEGURA: Si rutaImagen es nulo o undefined, asignamos la por defecto
                            let fotoFinal = '../imagenes/default-negocio.jpg'; 

                            if (rutaImagen) {
                                fotoFinal = rutaImagen.startsWith("http") 
                                    ? rutaImagen 
                                    : `../verImagen?nombre=${rutaImagen}`;
                            }

                            ExpandingTarget.innerHTML = `
                                <div class="content-expanding-cards">
                                    <img src="${fotoFinal}" class="imagen-expanding-card" alt="${neg.nombreEstablecimiento}">
                                    <span class="spam-expanding-cards">${neg.nombreEstablecimiento}</span>
                                </div>
                            `;

                        // Escuchamos el clic en la sub-tarjeta que acabamos de crear
                        ExpandingTarget.addEventListener("click", (e) => {
                            e.stopPropagation(); 
                            
                            // IMPORTANTE: Asegúrate de que 'neg' tenga la propiedad 'idNegocio'
                            // Si tu Servlet solo envía 'idNegocio' en la acción 'carrusel' pero no en 'negociosPorCategoria', 
                            // entonces aquí seguirás recibiendo undefined.
                            
                            console.log("DEBUG: Objeto negocio recibido en categoría:", neg);
                            
                            if (neg.idNegocio) {
                                window.location.href = `usuarioInformacionNegocio.html?id=${neg.idNegocio}`;
                            } else {
                                console.error("Error: El negocio no tiene un ID válido.", neg);
                            }
                        });

                        // Insertamos el nodo hijo en la tarjeta grande
                        card.appendChild(ExpandingTarget);
                    });
                })
                .catch(error => {
                    console.error("Error al traer negocios en Juanfax:", error);
                    const tempLoading = card.querySelector(".loading-text");
                    if (tempLoading) tempLoading.remove();
                });

        } else {
            // Remueve todos los elementos generados al contraerse la tarjeta
            const removeTargets = card.querySelectorAll(".targets-expanding");
            removeTargets.forEach(target => {
                target.remove();
            });
        }
    });
});

// ============================================================================
// LOGICA DE INTEGRACIÓN DE MI PERFIL (TURISTA) - ACTUALIZADO
// ============================================================================

// 1. Apuntamos al nuevo id="profile" que agregaste en el HTML
const profileAvatar = document.getElementById("profile");
if (profileAvatar) {
    profileAvatar.style.cursor = "pointer"; // Hace que aparezca la mano al pasar el mouse
    profileAvatar.addEventListener("click", abrirModalPerfil);
}

// Configurar cierres del modal e interacciones internas
document.getElementById("btnCerrarModal").addEventListener("click", () => {
    document.getElementById("modalPerfil").style.display = "none";
});
document.getElementById("btnLogOut").addEventListener("click", () => {
    window.location.href = "../LoginServlet?accion=cerrarSesion";
});
document.getElementById("formMiPerfil").addEventListener("submit", ejecutarActualizacionPerfil);
document.getElementById("btnBorradoLogico").addEventListener("click", ejecutarBorradoLogicoCuenta);

async function abrirModalPerfil() {
    try {
        const res = await fetch("../LoginServlet?accion=verPerfil");
        const user = await res.json();
        
        if (user.error) {
            alert(user.error);
            window.location.href = "../index.html";
            return;
        }

        // Cargamos los datos dentro del modal
        document.getElementById("perfilNombre").value = user.nombre;
        document.getElementById("perfilCorreo").value = user.correo;
        document.getElementById("perfilRol").innerText = user.rol;
        document.getElementById("perfilEstado").innerText = user.estado;

        // Desplegamos el modal
        document.getElementById("modalPerfil").style.display = "flex";
    } catch (err) {
        console.error("Error al abrir perfil:", err);
        alert("No se pudo obtener la información de tu cuenta.");
    }
}

async function ejecutarActualizacionPerfil(e) {
    e.preventDefault();
    const nombre = document.getElementById("perfilNombre").value.trim();
    const correo = document.getElementById("perfilCorreo").value.trim();

    const params = new URLSearchParams();
    params.append("accion", "actualizarPerfil");
    params.append("nombre", nombre);
    params.append("correo", correo);

    try {
        const res = await fetch("../LoginServlet", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: params
        });
        const data = await res.json();
        
        alert(data.mensaje);
        if (data.success) {
            document.getElementById("modalPerfil").style.display = "none";
            
            // 🌟 NUEVO: Actualiza las iniciales en el header del turista inmediatamente sin recargar
            if (document.querySelector("#profile p")) {
                const iniciales = nombre.split(" ").map(n => n[0]).join("").substring(0,2).toUpperCase();
                document.querySelector("#profile p").innerText = iniciales;
            }
        }
    } catch (err) {
        alert("Error de comunicación con el servidor.");
    }
}

function ejecutarBorradoLogicoCuenta() {
    const seguro = confirm("¿Estás seguro de que deseas dar de baja tu cuenta? Tu acceso será restringido inmediatamente.");
    if (!seguro) return;

    const params = new URLSearchParams();
    params.append("accion", "eliminarMiCuenta");

    fetch("../LoginServlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        alert(data.mensaje);
        if (data.success) {
            window.location.href = "../index.html";
        }
    })
    .catch(() => alert("No se pudo procesar la baja de tu cuenta."));
}