/* =============================================================================
 * auth.js — Login, registration, tabs, and quick-fill credentials.
 * ============================================================================= */
(function () {
    'use strict';

    if (window.Session.isAuthenticated()) {
        window.UI.navigate('dashboard.html');
        return;
    }

    var tabLogin = document.getElementById('tab-login');
    var tabRegister = document.getElementById('tab-register');
    var formLogin = document.getElementById('login-form');
    var formRegister = document.getElementById('register-form');

    function selectTab(showLogin) {
        tabLogin.classList.toggle('active', showLogin);
        tabLogin.setAttribute('aria-selected', showLogin ? 'true' : 'false');
        tabRegister.classList.toggle('active', !showLogin);
        tabRegister.setAttribute('aria-selected', !showLogin ? 'true' : 'false');
        formLogin.classList.toggle('hidden', !showLogin);
        formRegister.classList.toggle('hidden', showLogin);
        window.UI.clearFieldErrors(formLogin);
        window.UI.clearFieldErrors(formRegister);
    }

    tabLogin.addEventListener('click', function () {
        selectTab(true);
    });

    tabRegister.addEventListener('click', function () {
        selectTab(false);
    });

    // Quick-fill demo account buttons
    Array.prototype.forEach.call(document.querySelectorAll('[data-demo]'), function (btn) {
        btn.addEventListener('click', function () {
            selectTab(true);
            var parts = (btn.getAttribute('data-demo') || '').split(':');
            formLogin.elements.email.value = parts[0] || '';
            formLogin.elements.password.value = parts[1] || '';
            formLogin.elements.email.focus();
        });
    });

    // Handle Login submission
    formLogin.addEventListener('submit', function (event) {
        event.preventDefault();
        window.UI.clearFieldErrors(formLogin);

        var outcome = window.UI.validate(formLogin, {
            email: [window.UI.validators.required, window.UI.validators.email],
            password: [window.UI.validators.required]
        });

        if (Object.keys(outcome.errors).length > 0) {
            window.UI.showFieldErrors(formLogin, outcome.errors);
            return;
        }

        var submitBtn = formLogin.querySelector('button[type="submit"]');
        window.UI.pending(submitBtn, window.API.login(outcome.values)).then(function (session) {
            window.UI.toast('Welcome back, ' + (session.user.fullName || session.user.email) + '!', 'success');
            globalThis.setTimeout(function () {
                window.UI.navigate('dashboard.html');
            }, 350);
        }).catch(function (error) {
            var msg = error.message || 'Invalid email or password';
            var general = formLogin.querySelector('[data-error-for="general"]');
            if (general) {
                general.textContent = msg;
            } else {
                window.UI.toast(msg, 'error');
            }
        });
    });

    // Handle Registration submission
    formRegister.addEventListener('submit', function (event) {
        event.preventDefault();
        window.UI.clearFieldErrors(formRegister);

        var outcome = window.UI.validate(formRegister, {
            fullName: [
                window.UI.validators.required,
                window.UI.validators.minLength(3),
                window.UI.validators.maxLength(100)
            ],
            email: [
                window.UI.validators.required,
                window.UI.validators.email
            ],
            password: [
                window.UI.validators.password
            ],
            role: [
                window.UI.validators.required
            ]
        });

        if (Object.keys(outcome.errors).length > 0) {
            window.UI.showFieldErrors(formRegister, outcome.errors);
            return;
        }

        var submitBtn = formRegister.querySelector('button[type="submit"]');
        window.UI.pending(submitBtn, window.API.register(outcome.values)).then(function (session) {
            window.UI.toast('Account created! Welcome, ' + session.user.fullName + '.', 'success');
            globalThis.setTimeout(function () {
                window.UI.navigate('dashboard.html');
            }, 350);
        }).catch(function (error) {
            var general = formRegister.querySelector('[data-error-for="general"]');
            if (error.fieldErrors && Object.keys(error.fieldErrors).length) {
                window.UI.showFieldErrors(formRegister, error.fieldErrors);
            } else if (general) {
                general.textContent = error.message || 'Registration failed.';
            } else {
                window.UI.toast(error.message || 'Registration failed.', 'error');
            }
        });
    });
})();
