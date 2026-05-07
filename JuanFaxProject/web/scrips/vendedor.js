const BttnesNav = document.querySelectorAll('.BttNavegacion');


BttnesNav.forEach((bttn) => {
    bttn.addEventListener('click', () => {
        BttnesNav.forEach((btn) => btn.classList.remove('BttNavegacionFocus'));
        bttn.classList.add('BttNavegacionFocus');
    });
});