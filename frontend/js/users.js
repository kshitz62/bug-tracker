/* =============================================================================
 * users.js — Administrator directory, role assignment, active toggling.
 * ============================================================================= */
(function () {
    'use strict';

    if (!window.UI.requireAuth(['ADMIN'])) {
        return;
    }

    var content = window.UI.renderShell({
        active: 'users',
        title: 'User Management',
        subtitle: 'Administer member accounts, adjust roles, and toggle access.'
    });

    var card = window.UI.el('div.card');
    content.appendChild(card);

    function renderTable(users) {
        window.UI.clear(card);

        var header = window.UI.el('div.card-header', {}, [
            window.UI.el('h2', { text: 'All Registered Members (' + users.length + ')' })
        ]);
        card.appendChild(header);

        var table = window.UI.el('table.table');
        var thead = window.UI.el('thead', {}, [
            window.UI.el('tr', {}, [
                window.UI.el('th', { text: 'User' }),
                window.UI.el('th', { text: 'Email' }),
                window.UI.el('th', { text: 'Role' }),
                window.UI.el('th', { text: 'Status' }),
                window.UI.el('th', { text: 'Created' }),
                window.UI.el('th', { text: 'Actions' })
            ])
        ]);

        var currentUserId = window.Session.user ? window.Session.user.id : null;
        var tbody = window.UI.el('tbody');

        users.forEach(function (user) {
            var isSelf = currentUserId && String(currentUserId) === String(user.id);

            // Role selection
            var roleSelect = window.UI.el('select', {
                disabled: isSelf,
                onchange: function () {
                    var newRole = roleSelect.value;
                    window.API.updateUserRole(user.id, newRole).then(function () {
                        window.UI.toast(user.fullName + ' updated to ' + window.UI.roleLabel(newRole), 'success');
                        loadUsers();
                    }).catch(function (err) {
                        window.UI.toast(err.message || 'Failed to update role.', 'error');
                        loadUsers();
                    });
                }
            }, [
                window.UI.el('option', { value: 'ROLE_ADMIN', text: 'Admin', selected: user.role === 'ROLE_ADMIN' }),
                window.UI.el('option', { value: 'ROLE_DEVELOPER', text: 'Developer', selected: user.role === 'ROLE_DEVELOPER' }),
                window.UI.el('option', { value: 'ROLE_TESTER', text: 'QA Tester', selected: user.role === 'ROLE_TESTER' })
            ]);

            // Status toggle button
            var statusBtn = window.UI.el('button.btn.btn-sm', {
                type: 'button',
                class: user.enabled ? 'btn-danger' : 'btn-primary',
                disabled: isSelf,
                text: user.enabled ? 'Deactivate' : 'Activate',
                onclick: function () {
                    var targetState = !user.enabled;
                    var promptTitle = targetState ? 'Activate user?' : 'Deactivate user?';
                    var promptMsg = targetState
                        ? 'Allow ' + user.fullName + ' to sign in again?'
                        : 'Prevent ' + user.fullName + ' from accessing the platform?';

                    window.UI.confirmDialog({
                        title: promptTitle,
                        message: promptMsg,
                        tone: targetState ? 'info' : 'danger'
                    }).then(function (yes) {
                        if (yes) {
                            window.API.updateUserStatus(user.id, targetState).then(function () {
                                window.UI.toast('Account status updated.', 'success');
                                loadUsers();
                            }).catch(function (err) {
                                window.UI.toast(err.message || 'Failed to update user status.', 'error');
                            });
                        }
                    });
                }
            });

            var statusBadge = window.UI.el('span.badge', {
                dataset: { tone: user.enabled ? 'success' : 'neutral' },
                text: user.enabled ? 'Active' : 'Disabled'
            });

            var tr = window.UI.el('tr', {}, [
                window.UI.el('td', {}, [
                    window.UI.el('div', { style: 'display:flex; align-items:center; gap:10px;' }, [
                        window.UI.el('span.avatar', { text: window.UI.initials(user) }),
                        window.UI.el('div', {}, [
                            window.UI.el('div', { style: 'font-weight:600;', text: user.fullName }),
                            isSelf ? window.UI.el('div.text-muted.text-small', { text: '(You)' }) : null
                        ])
                    ])
                ]),
                window.UI.el('td.text-muted', { text: user.email }),
                window.UI.el('td', {}, [roleSelect]),
                window.UI.el('td', {}, [statusBadge]),
                window.UI.el('td.text-muted.text-small', { text: window.UI.formatDate(user.createdAt) }),
                window.UI.el('td', {}, [statusBtn])
            ]);

            tbody.appendChild(tr);
        });

        table.appendChild(thead);
        table.appendChild(tbody);
        card.appendChild(window.UI.el('div.table-responsive', {}, [table]));
    }

    function loadUsers() {
        window.UI.clear(card);
        card.appendChild(window.UI.loading('Loading user directory…'));

        window.API.users().then(function (users) {
            renderTable(users);
        }).catch(function (err) {
            window.UI.clear(card);
            card.appendChild(window.UI.errorState(err, loadUsers));
        });
    }

    loadUsers();
})();
