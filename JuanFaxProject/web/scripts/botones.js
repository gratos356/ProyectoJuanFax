const botones = document.querySelectorAll('.botones');


botones.forEach((bttn) => {
    bttn.addEventListener('click', () => {
        botones.forEach((btn) => btn.classList.remove('botonesFocus'));
        bttn.classList.add('botonesFocus');
    });
});
