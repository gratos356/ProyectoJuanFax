const botones = document.querySelectorAll('.botones');
const botonesNavMovil = document.querySelectorAll('.BttNavMovil');
const botonesAdministradorMovile = document.querySelectorAll('.contenedorAccionesRapidas')


botones.forEach((bttn) => {
    bttn.addEventListener('click', () => {
        botones.forEach((btn) => btn.classList.remove('botonesFocus'));
        bttn.classList.add('botonesFocus');
    });
});
botonesNavMovil.forEach((bttn) => {
    bttn.addEventListener('click', () => {
        botonesNavMovil.forEach((btn) => btn.classList.remove('BttNavMovilFocus'));
        bttn.classList.add('BttNavMovilFocus');
    });
});
botonesAdministradorMovile.forEach((bttn) => {
    bttn.addEventListener('click', () => {
        botonesAdministradorMovile.forEach((btn) => btn.classList.remove('contenedorAccionesRapidasFocus'));
        bttn.classList.add('contenedorAccionesRapidasFocus');
    });
});