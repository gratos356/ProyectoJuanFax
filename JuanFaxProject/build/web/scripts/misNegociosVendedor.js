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

                    // 🎨 Control dinámico visual del texto y estilo del estado
                    let estadoTexto = negocio.estado ? negocio.estado.toUpperCase() : 'PENDIENTE';
                    let estadoClass = 'badge-pendiente';

                    if (estadoTexto === 'APROBADO' || estadoTexto === 'ACTIVO') {
                        estadoClass = 'badge-aprobado';
                    } else if (estadoTexto === 'RECHAZADO' || estadoTexto === 'BLOQUEADO') {
                        estadoClass = 'badge-rechazado';
                    }

                    // Inyectamos la imagen, información, el BADGE de estado y el BOTÓN de eliminar
                    card.innerHTML = `
                        <img src="${imagen}" alt="${negocio.nombreEstablecimiento}">
                        <h3>${negocio.nombreEstablecimiento}</h3>
                        <p style="color: #94a3b8; font-size: 13px; margin-bottom: 10px;">ID: # ${negocio.idNegocio}</p>
                        
                        <div style="margin-bottom: 15px;">
                            <span class="badge-estado ${estadoClass}">${estadoTexto}</span>
                        </div>
                        
                        <div style="display: flex; gap: 8px;">
                            <button class="btn-editar-negocio" style="flex: 1; background: #3b82f6; border: none; color: white; padding: 6px 0; border-radius: 6px; cursor: pointer; font-weight: 500;">
                                ✏️ Editar
                            </button>
                            <button class="btn-eliminar-negocio" style="flex: 1; background: transparent; border: 1px solid #e71d36; color: #e71d36; padding: 6px 0; border-radius: 6px; cursor: pointer; font-weight: 500;">
                                🗑️ Eliminar
                            </button>
                        </div>
                    `;

                    // Acción al hacer clic en cualquier parte de la tarjeta (Ir a gestionar)
                    card.addEventListener("click", () => {
                        localStorage.setItem("idNegocioGestionar", negocio.idNegocio);
                        window.location.href = "mainVendedor.html";
                    });

                    const btnEditar = card.querySelector(".btn-editar-negocio");
                    btnEditar.addEventListener("click", (e) => {
                        e.stopPropagation();
                        localStorage.setItem("idNegocioEditar", negocio.idNegocio); // Guardamos cuál va a editar
                        window.location.href = "editarNegocio.html"; // Redirige al formulario
                    });
                    
                    const btnEliminar = card.querySelector(".btn-eliminar-negocio");
                    btnEliminar.addEventListener("click", (e) => {
                        e.stopPropagation(); // 🛑 DETIENE EL CLIC: Así no se ejecuta el redireccionamiento de la tarjeta
                        eliminarNegocioVendedor(negocio.idNegocio, negocio.nombreEstablecimiento);
                    });

                    contenedor.appendChild(card);
                });
            } else {
                const info = document.createElement("p");
                info.style.color = "#94a3b8";
                info.innerText = "No tienes ningún negocio asignado en el sistema todavía.";
                contenedor.appendChild(info);
            }

            // 2. TARJETA ESPECIAL PARA CREAR UN NUEVO NEGOCIO
            const cardCrear = document.createElement("div");
            cardCrear.className = "card-negocio card-crear-nuevo"; 
            
            cardCrear.innerHTML = `
                <div class="icono-crear" style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; min-height: 200px; color: #3b82f6;">
                    <span style="font-size: 50px; font-weight: bold;">+</span>
                    <h3 style="margin-top: 10px; color: #f8fafc;">Agregar Establecimiento</h3>
                    <p style="color: #94a3b8; font-size: 12px; text-align: center; padding: 0 10px;">Registra un nuevo local en el sistema</p>
                </div>
            `;

            cardCrear.addEventListener("click", () => {
                window.location.href = "crearNegocio.html"; 
            });

            contenedor.appendChild(cardCrear);
        })
        .catch(error => {
            console.error("❌ Error al cargar los negocios del vendedor:", error);
        });
}

// 🌟 NUEVA FUNCIÓN: Se conecta al controlador para remover el negocio
function eliminarNegocioVendedor(id, nombre) {
    if (confirm(`⚠️ ¿Estás completamente seguro de que deseas eliminar permanentemente el establecimiento "${nombre}"? Esta acción no se puede deshacer.`)) {
        
        // Petición al servlet con la acción que creamos previamente
        fetch(`../LoginServlet?accion=eliminarNegocio&id=${id}`, { method: 'POST' })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert("🙌 " + data.message);
                    cargarMisNegocios(); // 🔄 Volvemos a renderizar las tarjetas actualizadas inmediatamente
                } else {
                    alert("❌ Error: " + data.message);
                }
            })
            .catch(error => {
                console.error("Error al procesar la eliminación:", error);
                alert("Hubo un fallo de comunicación con el servidor.");
            });
    }
}