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
            window.location.href = `usuarioInformacionNegocio.html?nombre=${encodeURIComponent(destino.nombreEstablecimiento)}`;
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

                        // Validamos si la imagen es local o externa
                        let fotoFinal = neg.urlImagen.startsWith("http") ? neg.urlImagen : `../imagenes/${neg.urlImagen}`;

                        // Tu estructura HTML original intacta sin estilos incrustados
                        ExpandingTarget.innerHTML = `
                            <div class="content-expanding-cards">
                                <img src="${fotoFinal}" class="imagen-expanding-card">
                                <span class="spam-expanding-cards">${neg.nombreEstablecimiento}</span>
                            </div>
                        `;

                        // 🌟 ¡AQUÍ ESTÁ EL EVENTO QUE HACÍA FALTA! 🌟
                        // Escuchamos el clic en la sub-tarjeta que acabamos de crear
                        ExpandingTarget.addEventListener("click", (e) => {
                            // Detiene la propagación para que no se cierre la tarjeta grande al hacer clic adentro
                            e.stopPropagation(); 
                            
                            console.log("Redirigiendo al negocio:", neg.nombreEstablecimiento);
                            
                            // Redirecciona pasándole el nombre del establecimiento por parámetro URL
                            window.location.href = `usuarioInformacionNegocio.html?nombre=${encodeURIComponent(neg.nombreEstablecimiento)}`;
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