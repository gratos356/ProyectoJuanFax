const expandingCards = document.querySelectorAll(".expanding-card")

// Variables de control global
let destinos = [];
let indiceBase = 0;

// 1. SOLICITAR LOS DATOS REALES AL SERVLET DE LOGIN
async function cargarDatosHero() {
    try {
        // Le pegamos al servlet que sí funciona pasándole el parámetro ?accion=carrusel
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

// 2. RENDERIZAR Y ROTAR LAS 5 CAJAS SIMULTÁNEAMENTE
function renderizarCarruselHero() {
    const cajas = document.querySelectorAll("#HeroContent .banner-hero");
    
    if (cajas.length === 0 || destinos.length === 0) return;

    cajas.forEach((caja, posicionFisica) => {
        const indiceDato = (indiceBase + posicionFisica) % destinos.length;
        const destino = destinos[indiceDato];
        
        // 🌟 VALIDACIÓN INTELIGENTE DE RUTA
        let rutaFinalImagen = "";
        
        if (destino.urlImagen.startsWith("http://") || destino.urlImagen.startsWith("https://")) {
            // Si viene de Google o internet, usamos la URL tal cual
            rutaFinalImagen = destino.urlImagen;
        } else {
            // Si es una imagen local que guardaste en el proyecto
            rutaFinalImagen = `../imagenes/${destino.urlImagen}`;
        }
        
        // Inyectamos el estilo visual con la ruta corregida
        caja.style.backgroundImage = `linear-gradient(rgba(3, 12, 26, 0.65), rgba(3, 12, 26, 0.85)), url('${rutaFinalImagen}')`;
        caja.style.backgroundSize = "cover";
        caja.style.backgroundPosition = "center";
        caja.style.transition = "background-image 0.5s ease-in-out";

        // Cambiamos el texto dinámicamente
        caja.innerHTML = `
            <div class="contenido-interno-hero" style="padding: 20px; text-align: center; pointer-events: none;">
                <h3 style="color: var(--ColorBotonesDeAccionDorado); font-family: var(--Fuente-General); font-size: 1.1rem; text-transform: uppercase; margin: 0; text-shadow: 1px 1px 3px rgba(0,0,0,0.9);">
                    ${destino.nombreEstablecimiento}
                </h3>
            </div>
        `;
    });
}

// 3. MOVIMIENTO DE LOS BOTONES
function moverDerecha() {
    if (destinos.length === 0) return;
    indiceBase = (indiceBase + 1) % destinos.length;
    renderizarCarruselHero();
}

// Sumamos el tamaño del arreglo para evitar que dé números negativos al ir hacia atrás
function moverIzquierda() {
    if (destinos.length === 0) return;
    indiceBase = (indiceBase - 1 + destinos.length) % destinos.length;
    renderizarCarruselHero();
}

// 4. AUTOMATIZACIÓN (Cambio de imágenes automático cada 5 segundos)
let autoplayTimer;
function iniciarAutoplay() {
    clearInterval(autoplayTimer);
    autoplayTimer = setInterval(moverDerecha, 5000);
}

// 5. INICIALIZADOR AL CARGAR LA PÁGINA
document.addEventListener("DOMContentLoaded", () => {
    // Disparamos la lectura a la base de datos
    cargarDatosHero();
    
    // Escuchamos tus botones reales por sus IDs
    const btnIzquierda = document.getElementById("Boton_Izquierda");
    const btnDerecha = document.getElementById("Boton_Derecha");

    if (btnIzquierda) {
        btnIzquierda.addEventListener("click", () => {
            moverIzquierda();
            iniciarAutoplay(); // Resetea el tiempo para una mejor experiencia de usuario
        });
    }

    if (btnDerecha) {
        btnDerecha.addEventListener("click", () => {
            moverDerecha();
            iniciarAutoplay();
        });
    }
});


expandingCards.forEach((card) => {
    card.addEventListener("click", () => {
        const isExpanded = card.classList.toggle("cardExpandida");
        //remover titulos cuando se expanda la card
        const titulo = card.querySelector(".titulos");
        titulo.classList.toggle("titulosDisable", isExpanded);

        if (isExpanded) {
            // 1. Obtenemos el nombre de la categoría asignada a esta tarjeta
            // Si tus tarjetas tienen un título h2 con el nombre (ej: "Restaurantes"), lo extraemos así:
            const nombreCategoria = card.querySelector("h2") ? card.querySelector("h2").innerText.trim() : "";
            
            // Añadimos un mensaje temporal mientras carga
            const cargando = document.createElement("div");
            cargando.classList.add("targets-expanding", "loading-text");
            cargando.innerText = "Cargando...";
            cargando.style.color = "#fff";
            card.appendChild(cargando);

            // 2. Hacemos la petición en vivo a tu LoginServlet pasándole la categoría
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

                    // 3. Reemplazamos el bucle fijo por la cantidad de negocios reales de la BD
                    negocios.forEach(neg => {
                        const ExpandingTarget = document.createElement("div");
                        ExpandingTarget.classList.add("targets-expanding");

                        // Validamos la ruta de la foto de forma inteligente (local o Google)
                        let fotoFinal = neg.urlImagen.startsWith("http") ? neg.urlImagen : `../imagenes/${neg.urlImagen}`;

                        // Estructura interna elegante para cada negocio inyectado en la tarjeta
                        ExpandingTarget.innerHTML = `
                            <div class="content-expanding-cards">
                                <img src="${fotoFinal}" class="imagen-expanding-card">
                                <span class="spam-expanding-cards">${neg.nombreEstablecimiento}</span>
                            </div>
                        `;

                        // Lo insertamos dentro de la tarjeta expandida
                        card.appendChild(ExpandingTarget);
                    });
                })
                .catch(error => {
                    console.error("Error al traer negocios en Juanfax:", error);
                    const tempLoading = card.querySelector(".loading-text");
                    if (tempLoading) tempLoading.remove();
                });

        } else {
            // Tu lógica de cierre existente: remueve todos los elementos generados de golpe
            const removeTargets = card.querySelectorAll(".targets-expanding");
            removeTargets.forEach(target => {
                target.remove();
            });
        }
    });
});
