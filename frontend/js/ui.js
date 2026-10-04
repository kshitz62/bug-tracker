/* =============================================================================
 * ui.js — DOM builders, formatters, toasts, modals, validation helpers and the
 * shared application shell (sidebar + topbar). Exposes window.UI.
 * ============================================================================= */
(function (global) {
    'use strict';

    /* ----------------------------------------------------------- DOM helpers */

    function el(selector, options, children) {
        var tokens = String(selector || 'div').split(/(?=[.#])/);
        var tag = 'div';

        if (tokens.length && /^[a-z][a-z0-9]*$/i.test(tokens[0])) {
            tag = tokens[0];
            tokens = tokens.slice(1);
        }

        var node = document.createElement(tag);

        tokens.forEach(function (token) {
            if (token.charAt(0) === '#') {
                node.id = token.slice(1);
            } else if (token.charAt(0) === '.') {
                node.classList.add(token.slice(1));
            }
        });

        Object.keys(options || {}).forEach(function (key) {
            var value = options[key];
            if (value === null || value === undefined || value === false) {
                return;
            }
            if (key.slice(0, 2) === 'on' && typeof value === 'function') {
                node.addEventListener(key.slice(2).toLowerCase(), value);
            } else if (key === 'class') {
                String(value).split(/\s+/).filter(Boolean).forEach(function (cls) {
                    node.classList.add(cls);
                });
            } else if (key === 'dataset') {
                Object.keys(value).forEach(function (dataKey) {
                    node.dataset[dataKey] = value[dataKey];
                });
            } else if (key === 'text') {
                node.textContent = value;
            } else if (key === 'html') {
                node.innerHTML = value;
            } else if (value === true) {
                node.setAttribute(key, '');
            } else {
                node.setAttribute(key, value);
            }
        });

        append(node, children);
        return node;
    }

    function append(parent, children) {
        if (children === null || children === undefined || children === false) {
            return parent;
        }
        if (!Array.isArray(children)) {
            children = [children];
        }
        children.forEach(function (child) {
            if (child === null || child === undefined || child === false) {
                return;
            }
            parent.appendChild(child.nodeType ? child : document.createTextNode(String(child)));
        });
        return parent;
    }

    function clear(node) {
        while (node && node.firstChild) {
            node.removeChild(node.firstChild);
        }
        return node;
    }

    var ICON_PATHS = {
        dashboard: 'M3 13h8V3H3v10zm10 8h8v-6h-8v6zM3 21h8v-6H3v6zm10-18v6h8V3h-8z',
        bugs: 'M8 2h8M9 7h6M6 7a6 6 0 0 1 12 0v5a6 6 0 0 1-12 0V7zM3 10h3M18 10h3M4 16l3-1M20 16l-3-1',
        plus: 'M12 5v14M5 12h14',
        users: 'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z',
        back: 'M19 12H5M12 19l-7-7 7-7',
        logout: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
        check: 'M20 6L9 17l-5-5',
        trash: 'M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 11v6M14 11v6',
        paperclip: 'M21.44 11.05l-9.19 9.19a5 5 0 0 1-7.07-7.07l9.19-9.19a3 3 0 0 1 4.24 4.24',
        comment: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10z',
        clock: 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20zM12 6v6l4 2'
    };

    function icon(name) {
        return el('svg.nav-icon', {
            viewBox: '0 0 24 24',
            'aria-hidden': 'true',
            html: '<path d="' + (ICON_PATHS[name] || ICON_PATHS.bugs) + '"></path>'
        });
    }
    /* ------------------------------------------------------------ Formatting */

    var STATUS_TONE = {
        OPEN: 'info',
        IN_PROGRESS: 'info',
        REOPENED: 'warning',
        RESOLVED: 'success',
        CLOSED: 'neutral'
    };

    function label(value) {
        if (value === null || value === undefined || value === '') {
            return '—';
        }
        return String(value).replace(/_/g, ' ').toLowerCase()
            .replace(/^./, function (character) {
                return character.toUpperCase();
            });
    }

    function badge(value, prefix) {
        return el('span.badge', {
            class: prefix + '-' + String(value || 'none').toLowerCase(),
            dataset: { tone: prefix === 'status' ? (STATUS_TONE[value] || 'neutral') : 'neutral' },
            text: label(value)
        });
    }

    function statusBadge(value) {
        return badge(value, 'status');
    }

    function severityBadge(value) {
        return badge(value, 'severity');
    }

    function priorityBadge(value) {
        return badge(value, 'priority');
    }

    var ROLE_LABELS = {
        ADMIN: 'Admin',
        TESTER: 'QA Tester',
        DEVELOPER: 'Developer'
    };

    /**
     * Human readable role name. Accepts either the short form ('ADMIN') or the
     * Spring authority form ('ROLE_ADMIN') returned by the REST API.
     */
    function roleLabel(role) {
        if (!role) {
            return '—';
        }
        var key = String(role).toUpperCase().replace(/^ROLE_/, '');
        return ROLE_LABELS[key] || label(key);
    }

    function initials(user) {
        if (!user) {
            return '?';
        }
        return String(user.fullName || user.email || '?').trim().split(/\s+/)
            .map(function (part) {
                return part.charAt(0);
            }).slice(0, 2).join('').toUpperCase();
    }

    function formatDate(value) {
        var date = new Date(value);
        if (!value || isNaN(date.getTime())) {
            return '—';
        }
        return date.toLocaleDateString(undefined, {
            day: '2-digit', month: 'short', year: 'numeric'
        });
    }

    function formatDateTime(value) {
        var date = new Date(value);
        if (!value || isNaN(date.getTime())) {
            return '—';
        }
        return formatDate(value) + ' · ' + date.toLocaleTimeString(undefined, {
            hour: '2-digit', minute: '2-digit'
        });
    }

    function relativeTime(value) {
        var then = new Date(value).getTime();
        if (!value || isNaN(then)) {
            return '—';
        }
        var diff = Date.now() - then;
        var units = [
            ['y', 31536000000], ['mo', 2592000000], ['d', 86400000],
            ['h', 3600000], ['m', 60000]
        ];
        for (var index = 0; index < units.length; index += 1) {
            var amount = Math.floor(diff / units[index][1]);
            if (amount >= 1) {
                return amount + units[index][0] + ' ago';
            }
        }
        return 'just now';
    }

    function formatBytes(bytes) {
        if (bytes === null || bytes === undefined) {
            return '—';
        }
        var units = ['B', 'KB', 'MB', 'GB'];
        var value = bytes;
        var index = 0;
        while (value >= 1024 && index < units.length - 1) {
            value /= 1024;
            index += 1;
        }
        return (index === 0 ? String(value) : value.toFixed(1)) + ' ' + units[index];
    }

    function pluralise(count, singular, plural) {
        return count + ' ' + (count === 1 ? singular : (plural || singular + 's'));
    }

    /* ---------------------------------------------------------------- Toasts */

    function toastRegion() {
        var region = document.getElementById('toast-region');
        if (!region) {
            region = el('div.toast-region#toast-region', {
                role: 'status',
                'aria-live': 'polite'
            });
            document.body.appendChild(region);
        }
        return region;
    }

    function dismiss(node) {
        node.style.opacity = '0';
        node.style.transform = 'translateY(6px)';
        global.setTimeout(function () {
            if (node.parentNode) {
                node.parentNode.removeChild(node);
            }
        }, 180);
    }

    function toast(message, tone) {
        var node = el('div.toast', { dataset: { tone: tone || 'info' } }, [
            el('span', { text: message }),
            el('button.toast-close', {
                type: 'button',
                'aria-label': 'Dismiss notification',
                text: '×',
                onclick: function () {
                    dismiss(node);
                }
            })
        ]);
        toastRegion().appendChild(node);
        global.setTimeout(function () {
            if (node.parentNode) {
                dismiss(node);
            }
        }, tone === 'error' ? 6500 : 4000);
        return node;
    }
    /* ---------------------------------------------------------------- Modals */

    function openModal(config) {
        var backdrop = el('div.modal-backdrop', { role: 'presentation' });
        var closed = false;

        function close() {
            if (closed) {
                return;
            }
            closed = true;
            document.removeEventListener('keydown', onKeydown);
            if (backdrop.parentNode) {
                backdrop.parentNode.removeChild(backdrop);
            }
            if (typeof config.onClose === 'function') {
                config.onClose();
            }
        }

        function onKeydown(event) {
            if (event.key === 'Escape') {
                close();
            }
        }

        var confirmButton = el('button.btn', {
            type: 'button',
            class: config.tone === 'danger' ? 'btn-danger' : 'btn-primary',
            text: config.confirmLabel || 'Confirm'
        });

        var body = el('div.modal-body', {}, [
            el('h2', { text: config.title || 'Are you sure?' }),
            config.message ? el('p.text-muted', { text: config.message }) : null,
            config.body || null,
            config.form || null
        ]);

        confirmButton.addEventListener('click', function () {
            var result = config.onConfirm ? config.onConfirm(close) : close();
            if (result && typeof result.then === 'function') {
                confirmButton.disabled = true;
                result.then(function () {
                    confirmButton.disabled = false;
                }, function () {
                    confirmButton.disabled = false;
                });
            }
        });

        backdrop.addEventListener('click', function (event) {
            if (event.target === backdrop) {
                close();
            }
        });
        document.addEventListener('keydown', onKeydown);

        backdrop.appendChild(el('div.modal', { role: 'dialog', 'aria-modal': 'true' }, [
            body,
            el('div.modal-footer', {}, [
                el('button.btn.btn-secondary', {
                    type: 'button',
                    text: config.cancelLabel || 'Cancel',
                    onclick: close
                }),
                confirmButton
            ])
        ]));

        document.body.appendChild(backdrop);
        global.setTimeout(function () {
            var focusable = backdrop.querySelector('input, select, textarea, button');
            if (focusable) {
                focusable.focus();
            }
        }, 30);
        return close;
    }

    function confirmDialog(config) {
        return new Promise(function (resolve) {
            var confirmed = false;
            openModal({
                title: config.title,
                message: config.message,
                confirmLabel: config.confirmLabel,
                tone: config.tone,
                onConfirm: function (close) {
                    confirmed = true;
                    close();
                    resolve(true);
                },
                onClose: function () {
                    if (!confirmed) {
                        resolve(false);
                    }
                }
            });
        });
    }

    /* --------------------------------------------------------- State renders */

    function loading(text) {
        return el('div.loading-block', {}, [
            el('span.spinner.spinner-lg'),
            el('span', { text: text || 'Loading…' })
        ]);
    }

    function skeletonRows(count) {
        var rows = [];
        for (var index = 0; index < (count || 5); index += 1) {
            rows.push(el('div', { style: 'padding:14px 16px' }, [
                el('div.skeleton', { style: 'width:' + (55 + (index % 4) * 12) + '%' })
            ]));
        }
        return rows;
    }

    function emptyState(title, hint, action) {
        return el('div.empty-state', {}, [
            el('h3', { text: title }),
            hint ? el('p', { text: hint }) : null,
            action || null
        ]);
    }

    function errorState(error, retry) {
        var status = error && error.status;
        var title = status === 403 ? 'You do not have access to this'
            : status === 404 ? 'We could not find that'
                : 'Something went wrong';
        return el('div.empty-state', {}, [
            el('h3', { text: title }),
            el('p', { text: (error && error.message) || 'Unexpected error.' }),
            retry ? el('button.btn.btn-secondary', {
                type: 'button', text: 'Try again', onclick: retry
            }) : null
        ]);
    }
    /* ----------------------------------------------------- Form validation */

    function fieldError(form, name) {
        return form.querySelector('[data-error-for="' + name + '"]');
    }

    function showFieldErrors(form, errors) {
        clearFieldErrors(form);
        Object.keys(errors || {}).forEach(function (name) {
            var input = form.querySelector('[name="' + name + '"]');
            var holder = fieldError(form, name);
            if (input) {
                input.setAttribute('aria-invalid', 'true');
            }
            if (holder) {
                holder.textContent = errors[name];
            }
        });
    }

    function clearFieldErrors(form) {
        Array.prototype.forEach.call(form.querySelectorAll('[aria-invalid]'), function (input) {
            input.removeAttribute('aria-invalid');
        });
        Array.prototype.forEach.call(form.querySelectorAll('[data-error-for]'), function (holder) {
            holder.textContent = '';
        });
    }

    function readForm(form) {
        var values = {};
        Array.prototype.forEach.call(form.elements, function (element) {
            var name = element.name;
            if (!name || element.type === 'button' || element.type === 'submit') {
                return;
            }
            var value = element.type === 'checkbox' ? element.checked : element.value;
            if (typeof value === 'string') {
                value = value.trim();
                if (element.dataset.cast === 'number') {
                    value = value === '' ? null : Number(value);
                }
            }
            values[name] = value === '' ? null : value;
        });
        return values;
    }

    var validators = {
        required: function (value) {
            return value === null || value === undefined || value === '' ? 'This field is required' : null;
        },
        minLength: function (min) {
            return function (value) {
                if (value === null || value === undefined || value === '') {
                    return null;
                }
                return String(value).length < min ? 'Must be at least ' + min + ' characters' : null;
            };
        },
        maxLength: function (max) {
            return function (value) {
                if (value === null || value === undefined || value === '') {
                    return null;
                }
                return String(value).length > max ? 'Must be ' + max + ' characters or fewer' : null;
            };
        },
        email: function (value) {
            if (!value) {
                return null;
            }
            return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) ? null : 'Enter a valid email address';
        },
        password: function (value) {
            if (!value) {
                return 'Password is required';
            }
            if (value.length < 8) {
                return 'Use at least 8 characters';
            }
            if (!/[A-Za-z]/.test(value) || !/[0-9]/.test(value)) {
                return 'Include at least one letter and one number';
            }
            return null;
        }
    };

    function validate(form, rules) {
        var values = readForm(form);
        var errors = {};
        Object.keys(rules).forEach(function (name) {
            var list = rules[name] || [];
            for (var index = 0; index < list.length; index += 1) {
                var message = list[index](values[name], values);
                if (message) {
                    errors[name] = message;
                    break;
                }
            }
        });
        return { values: values, errors: errors };
    }

    function pending(button, task) {
        var original = button.textContent;
        button.disabled = true;
        button.textContent = 'Working…';
        return task.then(function (result) {
            button.disabled = false;
            button.textContent = original;
            return result;
        }, function (error) {
            button.disabled = false;
            button.textContent = original;
            throw error;
        });
    }
    /* ---------------------------------------------------- Application shell */

    var NAV_ITEMS = [
        { key: 'dashboard', label: 'Dashboard', href: 'dashboard.html', icon: 'dashboard' },
        { key: 'bugs', label: 'Bugs', href: 'bugs.html', icon: 'bugs' },
        { key: 'new-bug', label: 'Report Bug', href: 'bug-form.html', icon: 'plus' },
        { key: 'users', label: 'Users', href: 'users.html', icon: 'users', roles: ['ADMIN'] }
    ];

    function navigate(href) {
        global.location.href = href;
    }

    function logout() {
        Promise.resolve(window.API.logout()).then(function () {
            window.Session.clear();
            navigate('index.html');
        });
    }

    function buildSidebar(activeKey) {
        var role = window.Session.role;
        var user = window.Session.user || {};

        var nav = el('nav.nav', { 'aria-label': 'Primary' });
        NAV_ITEMS.forEach(function (item) {
            if (item.roles && item.roles.indexOf(role) === -1) {
                return;
            }
            nav.appendChild(el('a.nav-link', {
                href: item.href,
                class: activeKey === item.key ? 'active' : null,
                'aria-current': activeKey === item.key ? 'page' : null
            }, [icon(item.icon), el('span', { text: item.label })]));
        });

        var sidebar = el('aside.sidebar', {}, [
            el('a.brand', { href: 'dashboard.html' }, [
                el('span.brand-mark', { 'aria-hidden': 'true', text: '🐞' }),
                el('span', { text: 'Bug Tracker' })
            ]),
            nav,
            el('div.sidebar-footer', {}, [
                el('div.user-chip', {}, [
                    el('span.avatar', { text: initials(user) }),
                    el('div', {}, [
                        el('div.user-chip-name', { text: user.fullName || 'Signed in' }),
                        el('div.user-chip-role', { text: role ? roleLabel(role) : '' })
                    ])
                ]),
                el('button.btn.btn-secondary.btn-block', {
                    type: 'button',
                    onclick: logout
                }, [icon('logout'), el('span', { text: 'Sign out' })])
            ])
        ]);

        sidebar.insertBefore(el('button.nav-toggle', {
            type: 'button',
            'aria-label': 'Toggle navigation',
            text: '☰',
            onclick: function () {
                sidebar.classList.toggle('nav-open');
            }
        }), nav);
        return sidebar;
    }

    function renderShell(config) {
        var options = config || {};

        var actions = el('div.topbar-actions');
        append(actions, options.actions || null);

        var content = el('div.content', { id: 'content' });
        var main = el('main.main', {}, [
            el('header.topbar', {}, [
                el('div.topbar-title', {}, [
                    el('h1', { text: options.title || 'Bug Tracker' }),
                    options.subtitle ? el('p', { text: options.subtitle }) : null
                ]),
                actions
            ]),
            content
        ]);

        var mount = document.getElementById('app') || document.body;
        clear(mount);
        mount.appendChild(buildSidebar(options.active));
        mount.appendChild(main);
        document.title = (options.title ? options.title + ' · ' : '') + 'Bug Tracker';
        return content;
    }

    function requireAuth(roles) {
        if (!window.Session.isAuthenticated()) {
            navigate('index.html');
            return false;
        }
        if (roles && roles.length && roles.indexOf(window.Session.role) === -1) {
            toast('Your role does not have access to that page.', 'error');
            navigate('dashboard.html');
            return false;
        }
        return true;
    }
    /* ------------------------------------------------------------ Pagination */

    function pagination(meta, onChange) {
        var bar = el('div.pagination', { 'aria-label': 'Pagination' });

        function pageButton(text, target, options) {
            return el('button.page-btn', {
                type: 'button',
                text: text,
                disabled: Boolean(options && options.disabled),
                'aria-current': options && options.current ? 'page' : null,
                onclick: function () {
                    onChange(target);
                }
            });
        }

        // PageResponse exposes `first` / `last` flags; fall back to page maths for
        // safety so the arrows stay usable no matter which envelope is supplied.
        var hasPrevious = meta.page > 0 && !meta.first;
        var hasNext = meta.page < meta.totalPages - 1 && !meta.last;

        bar.appendChild(pageButton('‹', meta.page - 1, { disabled: !hasPrevious }));

        var start = Math.max(0, Math.min(meta.page - 2, Math.max(0, meta.totalPages - 5)));
        for (var index = start; index < Math.min(meta.totalPages, start + 5); index += 1) {
            bar.appendChild(pageButton(String(index + 1), index, { current: index === meta.page }));
        }

        bar.appendChild(pageButton('›', meta.page + 1, { disabled: !hasNext }));
        return bar;
    }

    function resultsSummary(meta) {
        if (!meta.totalElements) {
            return 'No results';
        }
        var from = meta.page * meta.size + 1;
        var to = Math.min((meta.page + 1) * meta.size, meta.totalElements);
        return 'Showing ' + from + '–' + to + ' of ' + meta.totalElements +
            ' · page ' + (meta.page + 1) + ' of ' + meta.totalPages;
    }

    /* -------------------------------------------------------------- Exports */

    global.UI = {
        el: el,
        append: append,
        clear: clear,
        icon: icon,
        label: label,
        badge: badge,
        statusBadge: statusBadge,
        severityBadge: severityBadge,
        priorityBadge: priorityBadge,
        roleLabel: roleLabel,
        initials: initials,
        formatDate: formatDate,
        formatDateTime: formatDateTime,
        relativeTime: relativeTime,
        formatBytes: formatBytes,
        pluralise: pluralise,
        toast: toast,
        openModal: openModal,
        confirmDialog: confirmDialog,
        loading: loading,
        skeletonRows: skeletonRows,
        emptyState: emptyState,
        errorState: errorState,
        showFieldErrors: showFieldErrors,
        clearFieldErrors: clearFieldErrors,
        readForm: readForm,
        validate: validate,
        validators: validators,
        pending: pending,
        renderShell: renderShell,
        requireAuth: requireAuth,
        navigate: navigate,
        logout: logout,
        pagination: pagination,
        resultsSummary: resultsSummary
    };
})(window);
