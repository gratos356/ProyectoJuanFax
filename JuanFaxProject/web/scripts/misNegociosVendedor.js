document.addEventListener("DOMContentLoaded", () => {
    cargarMisNegocios();
});

function cargarMisNegocios() {
    // Llamamos al servlet pidiendo la nueva acción
    fetch("../LoginServlet?accion=listarNegociosPorVendedor")
        .then(response => response.json())
        .then(negocios => {
            const contenedor = document.getElementById("listaNegociosVendedor");
            contenedor.innerHTML = "";

            // 1. Renderizar los negocios existentes (si los hay)
            if (negocios.length > 0) {
                negocios.forEach(negocio => {
                    const card = document.createElement("div");
                    card.className = "card-negocio";
                    
                    const imagen = negocio.urlImagen 
                        ? (negocio.urlImagen.startsWith("http") ? negocio.urlImagen : `../verImagen?nombre=${negocio.urlImagen}`)
                        : '../imagenes/default-negocio.jpg';

                    card.innerHTML = `
                        <img src="${imagen}" alt="${negocio.nombreEstablecimiento}">
                        <h3>${negocio.nombreEstablecimiento}</h3>
                        <p style="color: #94a3b8; font-size: 13px;">ID: # ${negocio.idNegocio}</p>
                    `;

                    card.addEventListener("click", () => {
                        localStorage.setItem("idNegocioGestionar", negocio.idNegocio);
                        window.location.href = "mainVendedor.html";
                    });

                    contenedor.appendChild(card);
                });
            } else {
                // Mensaje opcional si no tiene ninguno, aunque ahora siempre habrá al menos la tarjeta de crear
                const info = document.createElement("p");
                info.style.color = "#94a3b8";
                info.innerText = "No tienes ningún negocio asignado en el sistema todavía.";
                contenedor.appendChild(info);
            }

            // 🌟 2. AÑADIR LA TARJETA ESPECIAL PARA CREAR UN NUEVO NEGOCIO
            const cardCrear = document.createElement("div");
            cardCrear.className = "card-negocio card-crear-nuevo"; // Le sumamos una clase extra por si quieres darle estilos diferentes
            
            cardCrear.innerHTML = `
                <div class="icono-crear" style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; min-height: 200px; color: #3b82f6;">
                    <span style="font-size: 50px; font-weight: bold;">+</span>
                    <h3 style="margin-top: 10px; color: #f8fafc;">Agregar Establecimiento</h3>
                    <p style="color: #94a3b8; font-size: 12px; text-align: center; padding: 0 10px;">Registra un nuevo local en el sistema</p>
                </div>
            `;

            // Acción al hacer clic en la tarjeta de agregar
            cardCrear.addEventListener("click", () => {
                // Redirige a tu formulario HTML/JSP para registrar el negocio
                window.location.href = "crearNegocio.html"; 
            });

            contenedor.appendChild(cardCrear);
        })
        .catch(error => {
            console.error("❌ Error al cargar los negocios del vendedor:", error);
        });
}