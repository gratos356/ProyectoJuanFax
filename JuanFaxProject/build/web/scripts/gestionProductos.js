document.addEventListener("DOMContentLoaded", () => {
    // 🌟 REGLA DE ORO JUANFAX: Recuperamos el ID del establecimiento desde el localStorage
    const idNegocio = localStorage.getItem("idNegocioGestionar");

    if (!idNegocio) {
        alert("Por favor, selecciona primero un establecimiento para gestionar.");
        window.location.href = "misNegocios.html";
        return;
    }

    // Inicializamos la vista pasándole el ID real
    document.getElementById("labelIdNegocio").textContent = idNegocio;
    document.getElementById("idNegocioHidden").value = idNegocio;

    cargarProductos(idNegocio);
    configurarFormularioRegistro(idNegocio);
    configurarFiltroBusqueda();
    listenerMenuProducto(); 
});

/**
 * 1. CARGAR Y RENDERIZAR TARJETAS DESDE EL SERVLET (JSON)
 */
function cargarProductos(idNegocio) {
    const grid = document.getElementById("gridProductos");
    grid.innerHTML = "<p style='color: var(--texto-gris);'>Cargando inventario...</p>";

    fetch(`../ProductoServlet?accion=listarProductos&idNegocio=${idNegocio}`)
        .then(response => response.json())
        .then(productos => {
            grid.innerHTML = "";

            if (productos.length === 0) {
                grid.innerHTML = `
                    <article class="sin-productos">
                        <h3>No hay productos registrados.</h3>
                        <p>Usa el panel de la izquierda para dar de alta tu primer artículo.</p>
                    </article>`;
                return;
            }

            // Inyectamos dinámicamente cada producto usando el nuevo atributo 'estado'
            productos.forEach(p => {
                const card = document.createElement("article");
                card.className = "producto-card";

                // 🌟 REGLA DE ORO JUANFAX: Control dinámico visual del estado del producto
                let estadoTexto = p.estado ? p.estado.toUpperCase() : 'ACTIVO';
                let estadoClass = 'badge-pendiente';

                if (estadoTexto === 'ACTIVO' || estadoTexto === 'APROBADO') {
                    estadoClass = 'badge-aprobado'; // Se pintará en verde
                } else if (estadoTexto === 'INACTIVO' || estadoTexto === 'BAJA') {
                    card.classList.add("producto-inactivo"); // Opacidad o diseño de inactivo
                    estadoClass = 'badge-rechazado'; // Se pintará en rojo
                }

                // Convertimos el objeto a un String seguro escapando comillas para inyectarlo en el botón
                const productoJsonStr = JSON.stringify(p).replace(/"/g, '&quot;');

                // Inyectamos el HTML incluyendo el nuevo botón de Configuración/Editar
                card.innerHTML = `
                    <figure class="producto-imagen-contenedor" style="position: relative;">
                        <img src="../verImagen?nombre=${p.urlImagen}" alt="${p.nombre}" 
                             onerror="this.src='https://placehold.co/300x200/26262b/ffffff?text=Producto'">
                        
                        <span class="badge ${estadoClass}" style="position: absolute; top: 10px; right: 10px; font-size: 11px; padding: 4px 8px;">
                            ${estadoTexto}
                        </span>
                    </figure>
                    
                    <div class="producto-info">
                        <h3>${p.nombre}</h3>
                        <span class="producto-precio">$${p.precio.toLocaleString()} COP</span>
                        
                        <div class="control-stock">
                            <form class="form-update-stock">
                                <input type="hidden" name="idProducto" value="${p.idProducto}">
                                <label>Stock:</label>
                                <input type="number" name="stock" value="${p.stock}" min="0">
                                <button type="submit" class="btn-stock">Guardar</button>
                            </form>
                        </div>

                        <div class="producto-acciones-footer" style="display: flex; gap: 8px; margin-top: 10px;">
                            <button class="btn-stock" type="button" style="background-color: #007acc;" onclick="abrirModalEditar(${productoJsonStr})">
                                ⚙️ Editar
                            </button>
                            <button class="btn-baja" type="button" style="margin: 0; flex-grow: 1;" onclick="darDeBajaProducto(${p.idProducto}, '${p.nombre}', ${idNegocio})">
                                Dar de Baja
                            </button>
                        </div>
                    </div>
                `;
                grid.appendChild(card);
            });

            // Vinculamos el evento asíncrono a los nuevos formularios de stock
            vincularEventosStock();
        })
        .catch(error => {
            console.error("❌ Error cargando productos:", error);
            grid.innerHTML = "<p style='color:#e81123;'>Error al conectar con el servidor de inventario.</p>";
        });
}

/**
 * 2. REGISTRO ASÍNCRONO DE PRODUCTO CON IMAGEN (MULTIPART/FORM-DATA)
 */
function configurarFormularioRegistro(idNegocio) {
    const formulario = document.getElementById("formCrearProducto");
    
    formulario.addEventListener("submit", (e) => {
        e.preventDefault();

        const formData = new FormData(formulario);
        
        fetch("../ProductoServlet?accion=crear", {
            method: "POST",
            body: formData
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                alert("🙌 Producto agregado con éxito.");
                formulario.reset();
                document.getElementById("idNegocioHidden").value = idNegocio; // Reasignamos el id
                cargarProductos(idNegocio); // 🔄 Recarga el grid asíncronamente sin parpadeos
            } else {
                alert("❌ Error: " + data.message);
            }
        })
        .catch(err => console.error("Error al registrar producto:", err));
    });
}

/**
 * 3. ACTUALIZACIÓN DE STOCK ASÍNCRONA (FETCH POST)
 */
function vincularEventosStock() {
    const formulariosStock = document.querySelectorAll(".form-update-stock");

    formulariosStock.forEach(form => {
        form.addEventListener("submit", (e) => {
            e.preventDefault();
            
            const btn = form.querySelector(".btn-stock");
            const formData = new FormData(form);
            const params = new URLSearchParams(formData).toString();

            btn.textContent = "⚙️";
            btn.disabled = true;

            fetch("../ProductoServlet?accion=actualizarStock", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: params
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    btn.textContent = "✅";
                    setTimeout(() => { btn.textContent = "Guardar"; btn.disabled = false; }, 1200);
                } else {
                    alert("No se pudo actualizar el stock.");
                    btn.textContent = "❌";
                    btn.disabled = false;
                }
            })
            .catch(err => {
                console.error(err);
                btn.disabled = false;
            });
        });
    });
}

/**
 * 4. ELIMINACIÓN / BORRADO LÓGICO ASÍNCRONO
 */
function darDeBajaProducto(idProducto, nombre, idNegocio) {
    if (confirm(`⚠️ ¿Estás seguro de que deseas dar de baja el producto "${nombre}" en JuanFax?`)) {
        fetch(`../ProductoServlet?accion=baja&idProducto=${idProducto}`, { method: 'POST' })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    alert("Producto dado de baja correctamente.");
                    cargarProductos(idNegocio); // 🔄 Recargamos el grid actualizado
                } else {
                    alert("❌ Error: " + data.message);
                }
            })
            .catch(err => console.error("Error al dar de baja:", err));
    }
}

/**
 * 5. FILTRO DE BÚSQUEDA EN TIEMPO REAL (LOCAL)
 */
function configurarFiltroBusqueda() {
    document.getElementById("buscar-producto").addEventListener("input", (e) => {
        const busqueda = e.target.value.toLowerCase().trim();
        const cards = document.querySelectorAll(".producto-card");

        cards.forEach(card => {
            const nombre = card.querySelector("h3").textContent.toLowerCase();
            card.style.display = nombre.includes(busqueda) ? "" : "none";
        });
    });
}

function listenerMenuProducto(){
    const tituloHeader = document.getElementById("titulo");
    if (tituloHeader) {
        tituloHeader.style.cursor = "pointer"; 
        
        tituloHeader.addEventListener("click", () => {
            console.log("🔄 Navegando al Dashboard principal de Juanfax...");
            window.location.href = "mainVendedor.html";
        });
    }
}

// ===================================================================
// 🪟 FUNCIONES NUEVAS: GESTIÓN INTEGRADA DEL MODAL DE EDICIÓN
// ===================================================================
function abrirModalEditar(producto) {
    // Cargamos la información del objeto de la base de datos en los inputs correspondientes del modal
    document.getElementById("editIdProducto").value = producto.idProducto;
    document.getElementById("editNombre").value = producto.nombre;
    document.getElementById("editPrecio").value = producto.precio;
    document.getElementById("editStock").value = producto.stock; // Sincroniza el stock

    // Reseteamos el campo input file por si se seleccionaron fotos anteriormente
    document.getElementById("editImagenes").value = "";

    // Mostramos el modal manipulando la clase CSS de visualización
    const modal = document.getElementById("modalEditarProducto");
    modal.style.display = "flex";
}

function cerrarModalEditar() {
    const modal = document.getElementById("modalEditarProducto");
    modal.style.display = "none";
}

function guardarEdicionCompleta() {
    const idProducto = document.getElementById("editIdProducto").value;
    const nombre = document.getElementById("editNombre").value;
    const precio = document.getElementById("editPrecio").value;
    const stock = document.getElementById("editStock").value;
    const fileInput = document.getElementById("editImagenes");

    if (!nombre || !precio || !stock) {
        alert("Todos los campos obligatorios deben ser rellenados.");
        return;
    }

    // Encapsulamos todo en FormData para procesar textos y archivos de galería combinados
    const formData = new FormData();
    formData.append("idProducto", idProducto);
    formData.append("nombre", nombre);
    formData.append("precio", precio);
    formData.append("stock", stock);

    // Adjuntamos secuencialmente los ficheros del input múltiple
    if (fileInput.files.length > 0) {
        for (let i = 0; i < fileInput.files.length; i++) {
            formData.append("imagenes_producto", fileInput.files[i]);
        }
    }

    // Petición POST asíncrona hacia el else if("editarProducto") del backend
    fetch("../ProductoServlet?accion=editarProducto", {
        method: "POST",
        body: formData // Dejamos que el navegador asigne las cabeceras multipart por su cuenta
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            alert("🙌 " + data.message);
            cerrarModalEditar();
            
            // Recargamos el grid consumiendo el id real persistido en localStorage
            const idNegocioReal = localStorage.getItem("idNegocioGestionar");
            cargarProductos(idNegocioReal); 
        } else {
            alert("❌ Error: " + data.message);
        }
    })
    .catch(err => {
        console.error("Error crítico durante la edición masiva:", err);
        alert("Hubo un fallo en la red al conectar con el servidor.");
    });
}