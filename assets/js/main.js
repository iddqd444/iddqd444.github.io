document.addEventListener('DOMContentLoaded', () => {
    const burger = document.getElementById('burgerBtn');
    const menu = document.getElementById('navMenu');

    if(burger && menu) {
        burger.addEventListener('click', () => {
            menu.classList.toggle('active');
            menu.classList.toggle('hidden'); // Для Tailwind
            menu.classList.toggle('flex');   // Для Tailwind
        });
    }
});
