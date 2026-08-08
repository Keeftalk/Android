/**
 * Keeftalk Website Interactive Logic
 * Handles Mobile Menu and Theme Switching.
 * (Static Header/Footer are now hardcoded in HTML for crawler visibility).
 */

document.addEventListener('DOMContentLoaded', () => {
    // Mobile menu toggle
    const menuBtn = document.getElementById('mobile-menu-btn');
    const menu = document.getElementById('mobile-menu');
    if (menuBtn && menu) {
        menuBtn.addEventListener('click', () => {
            menu.classList.toggle('hidden');
        });
    }

    // Theme Switch Logic
    const toggleTheme = () => {
        if (document.documentElement.classList.contains('dark')) {
            document.documentElement.classList.remove('dark');
            localStorage.setItem('theme', 'light');
        } else {
            document.documentElement.classList.add('dark');
            localStorage.setItem('theme', 'dark');
        }
    };

    document.getElementById('theme-toggle')?.addEventListener('click', toggleTheme);
    document.getElementById('theme-toggle-mobile')?.addEventListener('click', toggleTheme);
});
