const BttnesNav = document.querySelectorAll('.BttNavegacion');
const BttSideBaar = document.querySelectorAll('.BttnSideBaar');

BttnesNav.forEach((bttn) => {
    bttn.addEventListener('click', () => {
        BttnesNav.forEach((btn) => btn.classList.remove('BttNavegacionFocus'));
        bttn.classList.add('BttNavegacionFocus');
    });
});

BttSideBaar.forEach((bttn) => {
    bttn.addEventListener('click', () => {
        BttSideBaar.forEach((btn) => btn.classList.remove('BttnSideBaarFocus'));
        bttn.classList.add('BttnSideBaarFocus');
    });
});