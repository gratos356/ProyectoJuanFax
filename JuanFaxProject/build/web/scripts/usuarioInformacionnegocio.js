let idNegocioActual = null;
let productosCargados = []; 

// 1. Ejecutamos la carga apenas se abra la página
document.addEventListener("DOMContentLoaded", () => {
    // 1. Extraer el ID de la URL
    const params = new URLSearchParams(window.location.search);
    const idEnUrl = params.get("id"); // O el nombre de tu parámetro
    
    // 2. Asignar a la variable global
    if (idEnUrl) {
        idNegocioActual = idEnUrl;
    }

    // 3. Ahora sí, ejecutar las funciones
    cargarDetallesNegocio();
    configurarFormularioComentario();
    cargarProductosParaUsuario(idNegocioActual);

    cargarHistorialUsuario();
    actualizarInterfazCarrito();
});

function cargarDetallesNegocio() {
    const params = new URLSearchParams(window.location.search);
    const idNegocio = params.get("id");

    console.log("-> ID recibido en la página de detalle:", idNegocio); // <--- ESTO ES VITAL

    if (!idNegocio) {
        console.error("No se encontró ID en la URL");
        return;
    }

    fetch(`../LoginServlet?accion=detalleNegocioUnico&id=${idNegocio}`)
    .then(response => response.json())
    .then(negocio => {
        console.log("Datos recibidos del servidor:", negocio);
        if (!negocio || negocio.error) return;
        
        idNegocioActual = negocio.idNegocio; 
        
        // PINTAR DATOS ORIGINALES
        const txtNombre = document.getElementById("nombreNegocio");
        const txtDescripcion = document.getElementById("descripcionNegocio");
        const contenedorImagen = document.getElementById("imagenNegocio");
        
        if (txtNombre) txtNombre.innerText = negocio.nombreEstablecimiento || "Sin nombre";
        if (txtDescripcion) txtDescripcion.innerText = negocio.descripcion || "Sin descripción";
        
        let rutaImagen = negocio.url_imagen || negocio.urlImagen;
        let fotoFinal = '../imagenes/default-negocio.jpg';  

        if (rutaImagen) {
            fotoFinal = rutaImagen.startsWith("http") 
                ? rutaImagen 
                : `../verImagen?nombre=${rutaImagen}`;
        }

        if (contenedorImagen) {
            contenedorImagen.innerHTML = `<div class="contenedorImagen" style="background-image: url('${fotoFinal}');" alt="${negocio.nombreEstablecimiento}"></div>`;
        }
        
        if (idNegocioActual) {
            registrarMetricaSilenciosa(idNegocioActual, "registrarVista");
            cargarComentarios(idNegocioActual);
        }

        if (negocio.latitud && negocio.longitud) {
            inicializarMapaGoogle(parseFloat(negocio.latitud), parseFloat(negocio.longitud), negocio.nombreEstablecimiento);
        }
    })
    .catch(error => console.error("Error en Juanfax JS:", error));
}

// 2. FUNCIÓN PARA TRAER LOS COMENTARIOS DE LA BD
function cargarComentarios(idNegocio) {
    fetch(`../LoginServlet?accion=listarComentarios&idNegocio=${idNegocio}`)
        .then(response => response.json())
        .then(comentarios => {
            const contenedorLista = document.getElementById("listaComentarios");
            if (!contenedorLista) return;

            contenedorLista.innerHTML = "";

            if (comentarios.length === 0) {
                contenedorLista.innerHTML = `<p style="color: #94a3b8; font-style: italic;">Sé el primero en dejar una reseña para este establecimiento.</p>`;
                return;
            }

            comentarios.forEach(c => {
                const estrellasMaximas = 5;
                let estrellasHTML = "";
                for (let i = 1; i <= estrellasMaximas; i++) {
                    if (i <= c.calificacion) {
                        estrellasHTML += '<span style="color: #ffca28; font-size: 1.1rem; margin-right: 2px;">★</span>';
                    } else {
                        estrellasHTML += '<span style="color: #cbd5e1; font-size: 1.1rem; margin-right: 2px;">☆</span>';
                    }
                }

                const card = document.createElement("div");
                card.className = "comentarioCard";
                card.innerHTML = `
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                        <p class="usuarioNombre" style="margin: 0;"><strong>${c.nombreUsuario}</strong> <span class="fecha" style="font-size: 0.85rem; color: #94a3b8; margin-left: 8px;">${c.fecha}</span></p>
                        <div class="estrellasContenedor">${estrellasHTML}</div>
                    </div>
                    <p class="comentarioTexto" style="margin: 0; color: #f8fafc;">${c.textoComentario}</p>
                `;
                contenedorLista.appendChild(card);
            });
        })
        .catch(error => console.error("Error al listar comentarios:", error));
}

// 3. FUNCIÓN PARA ENVIAR UN NUEVO COMENTARIO (POST)
function configurarFormularioComentario() {
    const formulario = document.getElementById("formComentario");
    if (!formulario) return;

    formulario.addEventListener("submit", (e) => {
        e.preventDefault();

        const cajaTexto = document.getElementById("txtComentario");
        const estrellaSeleccionada = document.querySelector('input[name="puntuacion"]:checked');
        
        if (!estrellaSeleccionada) {
            alert("Por favor, selecciona una calificación en estrellas antes de publicar tu comentario.");
            return;
        }

        const texto = cajaTexto.value.trim();
        if (texto === "") {
            alert("El comentario no puede estar vacío.");
            return;
        }

        const datos = new URLSearchParams();
        datos.append("accion", "guardarComentario");
        datos.append("idNegocio", idNegocioActual);
        datos.append("textoComentario", texto);
        datos.append("valorPuntuacion", estrellaSeleccionada.value); 

        fetch("../LoginServlet", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: datos.toString()
        })
        .then(response => response.json())
        .then(resultado => {
            if (resultado.status === "success") {
                cajaTexto.value = "";
                estrellaSeleccionada.checked = false;
                cargarComentarios(idNegocioActual);
                alert("¡Comentario y puntuación publicados con éxito!");
            } else {
                alert("Error al guardar: " + resultado.message);
            }
        })
        .catch(error => console.error("Error en la petición:", error));
    });
}

const botonVolver = document.querySelector("#back");
function volverAtras() {
    if (document.referrer && window.history.length > 1) {
        window.history.back();
    } else {
        window.location.href = "mainUser.html"; 
    }
}
if (botonVolver) botonVolver.addEventListener("click", volverAtras);

// SE MODIFICÓ: Ya no resta el stock de manera local/visual basándose en lo que haya en el carrito
function cargarProductosParaUsuario(idNegocio) {
    const gridProductos = document.getElementById("gridProductosUsuario");
    if (!gridProductos) return; 
    
    gridProductos.innerHTML = "<p class='cargando-texto'>Cargando menú de productos...</p>";

    fetch(`../ProductoServlet?accion=listarProductos&idNegocio=${idNegocio}`)
        .then(response => {
            if (!response.ok) throw new Error("Error de red al recuperar catálogo.");
            return response.json();
        })
        .then(productos => {
            // Guardamos globalmente los productos válidos tal cual vienen de la Base de Datos
            productosCargados = productos.filter(p => 
                p.estado !== 0 && p.estado !== "baja" && p.estado !== "INACTIVO"
            );

            // Renderizado directo y limpio con stock 100% real de persistencia
            renderizarCatalogoVisual();
        })
        .catch(error => {
            console.error("❌ Error al renderizar catálogo de usuario:", error);
            gridProductos.innerHTML = "<p class='error-texto'>No se pudo cargar el catálogo de productos en este momento.</p>";
        });
}

function renderizarCatalogoVisual() {
    const gridProductos = document.getElementById("gridProductosUsuario");
    if (!gridProductos) return;

    gridProductos.innerHTML = "";

    if (productosCargados.length === 0) {
        gridProductos.innerHTML = `
            <div class="sin-productos-usuario">
                <p>Por el momento este establecimiento no tiene artículos disponibles en catálogo.</p>
            </div>`;
        return;
    }

    productosCargados.forEach(p => {
        const tarjeta = document.createElement("article");
        tarjeta.className = "tarjeta-producto-usuario";

        const precioNumerico = Number(p.precio) || 0;
        const precioFormateado = precioNumerico.toLocaleString('es-CO');

        tarjeta.innerHTML = `
            <div class="producto-usuario-imagen-wrapper">
                <img src="../verImagen?nombre=${p.urlImagen}" alt="${p.nombre}" 
                     onerror="this.src='https://placehold.co/300x200/26262b/ffffff?text=Producto'">
            </div>
            <div class="producto-usuario-detalles">
                <h3>${p.nombre}</h3>
                <p class="producto-usuario-precio">$${precioFormateado} COP</p>
                
                <span class="producto-usuario-stock ${p.stock <= 5 ? 'stock-critico' : ''}">
                    ${p.stock > 0 ? `Disponibles: ${p.stock}` : 'Agotado'}
                </span>
                <div class="contenedor-boton-carrito"></div>
            </div>
        `;

        const contenedorBoton = tarjeta.querySelector(".contenedor-boton-carrito");
        const boton = document.createElement("button");
        boton.className = "btn-juanfax";
        boton.style.marginTop = "10px";
        boton.style.width = "100%";

        if (p.stock > 0) {
            boton.classList.add("btn-agregar-carrito");
            boton.innerHTML = "🛒 Agregar al carrito";
            boton.addEventListener("click", () => {
                agregarAlCarrito(p.idProducto, p.nombre, precioNumerico);
            });
        } else {
            boton.innerHTML = "Agotado";
            boton.style.backgroundColor = "#475569";
            boton.disabled = true;
        }

        contenedorBoton.appendChild(boton);
        gridProductos.appendChild(tarjeta);
    });
}

function obtenerCarrito(idNegocio) {
    if (!idNegocio) return [];
    return JSON.parse(localStorage.getItem(`juanfax_carrito_${idNegocio}`)) || [];
}

// SE MODIFICÓ: Candado de validación estricta de stock sin alterar la UI global de productos
function agregarAlCarrito(idProducto, nombre, precio) {
    if (!idNegocioActual) {
        alert("Error de consistencia: ID del establecimiento ausente.");
        return;
    }

    // 1. Validar que el producto realmente exista en el catálogo cargado
    const productoCatalogo = productosCargados.find(p => Number(p.idProducto) === Number(idProducto));
    
    if (!productoCatalogo) {
        alert("El producto seleccionado no se encuentra disponible en el catálogo actual.");
        return; // Evita el error de leer .stock de un undefined
    }

    // 2. LEER usando la llave con ID único del negocio
    let carrito = obtenerCarrito(idNegocioActual); 

    const itemExistente = carrito.find(item => Number(item.idProducto) === Number(idProducto));
    const cantidadActualEnCarrito = itemExistente ? itemExistente.cantidad : 0;
    const nuevaCantidadPropuesta = cantidadActualEnCarrito + 1;

    // 3. Validar Stock de forma segura
    if (nuevaCantidadPropuesta > Number(productoCatalogo.stock)) {
        alert(`No puedes agregar más unidades de "${nombre}". El stock máximo disponible es de ${productoCatalogo.stock} unidades.`);
        return; 
    }

    console.log("todo bien hasta aca");

    if (itemExistente) {
        itemExistente.cantidad = nuevaCantidadPropuesta;
    } else {
        carrito.push({
            idProducto: Number(idProducto),
            nombreProducto: nombre, 
            precioUnitario: Number(precio), 
            cantidad: 1
        });
    }
    
    // 4. GUARDAR usando exactamente la misma llave con ID único del negocio 🌟
    localStorage.setItem(`juanfax_carrito_${idNegocioActual}`, JSON.stringify(carrito));

    actualizarInterfazCarrito();
}

function actualizarInterfazCarrito() {
    // 🌟 CLAVE: Trae solo lo que pertenece al negocio actual
    const carrito = obtenerCarrito(idNegocioActual); 
    const contador = document.getElementById("contadorCarrito");
    const cuerpoCarrito = document.getElementById("elementosCarrito");
    const totalContador = document.getElementById("totalCarrito");

    if (!contador || !cuerpoCarrito || !totalContador) return;

    const totalItems = carrito.reduce((sum, item) => sum + item.cantidad, 0);
    const montoTotal = carrito.reduce((sum, item) => sum + (item.cantidad * item.precioUnitario), 0);

    contador.textContent = totalItems;
    totalContador.textContent = montoTotal.toLocaleString('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 });

    if (carrito.length === 0) {
        cuerpoCarrito.innerHTML = "<p style='color:#94a3b8; text-align:center; padding:20px;'>El carrito está vacío para este negocio.</p>";
        return;
    }

    cuerpoCarrito.innerHTML = carrito.map(item => `
        <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #334155; padding:8px 0;">
            <div>
                <h4 style="margin:0; font-size:0.95rem; color:#f8fafc;">${item.nombreProducto}</h4>
                <span style="font-size:0.85rem; color:#94a3b8;">${item.cantidad} x ${item.precioUnitario.toLocaleString('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 })}</span>
            </div>
            <button style="background:none; border:none; cursor:pointer; font-size:1.1rem;" onclick="eliminarDelCarrito(${item.idProducto})">🗑️</button>
        </div>
    `).join('');
}

function eliminarDelCarrito(idProducto) {
    let carrito = obtenerCarrito(idNegocioActual);

    carrito = carrito.filter(item => Number(item.idProducto) !== Number(idProducto));
    
    // 🌟 CLAVE: Actualiza la persistencia usando la llave del negocio
    localStorage.setItem(`juanfax_carrito_${idNegocioActual}`, JSON.stringify(carrito));
    
    actualizarInterfazCarrito();
}

function alternarModalCarrito() {
    const modal = document.getElementById("modalCarrito");
    if (modal) modal.classList.toggle("container-oculto");
    actualizarInterfazCarrito();
}

function enviarPedidoAlServidor() {
    // 🌟 CLAVE: Mandamos al backend solo el carrito de este establecimiento
    const carrito = obtenerCarrito(idNegocioActual); 

    if (carrito.length === 0) return alert("Agrega productos al carrito primero.");
    if (!idNegocioActual) return alert("Error de consistencia: ID del establecimiento ausente.");

    const payload = {
        idNegocio: parseInt(idNegocioActual),
        items: carrito
    };

    fetch("../PedidoServlet?accion=registrarPedido", {
        method: "POST",
        headers: {
            "Content-Type": "application/json; charset=UTF-8"
        },
        body: JSON.stringify(payload)
    })
    .then(response => {
        if (!response.ok) throw new Error("Error en el procesamiento del servidor.");
        return response.json();
    })
    .then(data => {
        if (data.success) {
            alert(data.message);
            
            // 🌟 CLAVE: Limpiamos ÚNICAMENTE el carrito de este negocio. Los demás quedan intactos.
            localStorage.removeItem(`juanfax_carrito_${idNegocioActual}`); 
            
            alternarModalCarrito();
            cargarHistorialUsuario(); 
            cargarProductosParaUsuario(idNegocioActual);
            
        } else {
            alert("Error: " + data.message);
        }
    })
    .catch(error => {
        console.error("Error transaccional en fetch POST:", error);
        alert("Error de red: No se pudo conectar con el endpoint del PedidoServlet.");
    });
}   

function cargarHistorialUsuario() {
    const contenedor = document.getElementById("contenedorHistorial");
    if (!contenedor) return;

    contenedor.innerHTML = "<p class='cargando-texto'>Buscando transacciones en base de datos...</p>";

    // 🌟 REPARADO: Ahora sí viaja el idNegocio dinámicamente si existe en la interfaz
    const urlHistorial = idNegocioActual 
        ? `../PedidoServlet?accion=historialPedidos&idNegocio=${idNegocioActual}`
        : "../PedidoServlet?accion=historialPedidos";

    fetch(urlHistorial)
    .then(response => {
        if (!response.ok) throw new Error("Error al consultar API de historial.");
        return response.json();
    })
    .then(pedidos => {
        if (pedidos.length === 0) {
            contenedor.innerHTML = "<p style='color:#94a3b8; font-style:italic;'>No registras compras previas en tu cuenta.</p>";
            return;
        }

        contenedor.innerHTML = pedidos.map(pedido => `
            <div class="comentarioCard" style="margin-bottom:15px; border-left:4px solid #10b981;">
                <div style="display:flex; justify-content:space-between; margin-bottom:5px; border-bottom:1px dashed #334155; padding-bottom:5px;">
                    <span><strong>Orden N°:</strong> ${pedido.idPedido}</span>
                    <span style="color: #60a5fa; font-weight: 600;">🛒 ${pedido.nombreNegocio || 'Establecimiento'}</span>
                    <span style="font-size:0.85rem; color:#94a3b8;">${pedido.fechaCompra}</span>
                </div>
                <div style="margin-left:10px; font-size:0.9rem; color:#cbd5e1;">
                    <ul style="padding-left:15px; margin:5px 0;">
                        ${pedido.items.map(det => `
                            <li>${det.cantidad} x ${det.nombreProducto} — Subtotal: ${(det.cantidad * det.precioUnitario).toLocaleString('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 })}</li>
                        `).join('')}
                    </ul>
                </div>
                <div style="text-align:right; margin-top:8px; font-weight:bold; color:#10b981;">
                    Total Pagado: ${pedido.total.toLocaleString('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 })}
                </div>
            </div>
        `).join('');
    })
    .catch(error => {
        console.error("Error consumiendo historial GET:", error);
        contenedor.innerHTML = "<p class='error-texto'>Fallo de comunicación con el servicio de registros.</p>";
    });
}

function inicializarMapaGoogle(lat, lng, nombre) {
    const contenedorMapa = document.getElementById("showMap");
    if (!contenedorMapa) return;

    const ubicacion = { lat: lat, lng: lng };

    const map = new google.maps.Map(contenedorMapa, {
        zoom: 17,
        center: ubicacion,
        styles: [
            { elementType: "geometry", stylers: [{ color: "#0f172a" }] },
            { elementType: "labels.text.fill", stylers: [{ color: "#94a3b8" }] },
            { featureType: "water", stylers: [{ color: "#020617" }] }
        ]
    });

    new google.maps.Marker({
        position: ubicacion,
        map: map,
        title: nombre
    });

    contenedorMapa.addEventListener("click", () => {
        if (!contenedorMapa.dataset.clicRegistrado && idNegocioActual) {
            registrarMetricaSilenciosa(idNegocioActual, "registrarClic");
            contenedorMapa.dataset.clicRegistrado = "true";
        }
    });
}

async function registrarMetricaSilenciosa(idNegocio, accionMetrica) {
    try {
        const url = `../MetricasServlet?accion=${accionMetrica}&idNegocio=${idNegocio}`;
        const response = await fetch(url, { method: "POST" });
        if (response.ok) {
            console.log(`✅ Métrica '${accionMetrica}' guardada silenciosamente para el negocio #${idNegocio}`);
        }
    } catch (error) {
        console.error("❌ Error de red reportando métrica:", error);
    }
}

function initMap() {}