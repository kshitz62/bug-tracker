/* =============================================================================
 * bugs.js — Search, multi-field filters, sorting, paginated table.
 * ============================================================================= */
(function () {
    'use strict';

    if (!window.UI.requireAuth()) {
        return;
    }

    var state = {
        search: '',
        status: '',
        severity: '',
        priority: '',
        unassigned: false,
        assigneeId: '',
        page: 0,
        size: 10,
        sortBy: 'createdAt',
        sortDirection: 'desc'
    };

    var content = window.UI.renderShell({
        active: 'bugs',
        title: 'Bug Directory',
        subtitle: 'Search, filter, inspect and manage reported issues.',
        actions: [
            window.UI.el('a.btn.btn-primary', {
                href: 'bug-form.html'
            }, [window.UI.icon('plus'), window.UI.el('span', { text: 'Report Bug' })])
        ]
    });

    var filterCard = window.UI.el('div.card', { style: 'margin-bottom: var(--space-4)' });
    var filterBody = window.UI.el('div.card-body');
    var filterForm = window.UI.el('form.filter-bar', {
        onsubmit: function (event) {
            event.preventDefault();
            applyFilters();
        }
    });

    var searchInput = window.UI.el('input', {
        type: 'search',
        placeholder: 'Search code, title, description…',
        value: state.search,
        style: 'min-width: 220px; flex: 1'
    });

    var statusSelect = window.UI.el('select', {}, [
        window.UI.el('option', { value: '', text: 'All Statuses' }),
        window.UI.el('option', { value: 'OPEN', text: 'Open' }),
        window.UI.el('option', { value: 'IN_PROGRESS', text: 'In Progress' }),
        window.UI.el('option', { value: 'RESOLVED', text: 'Resolved' }),
        window.UI.el('option', { value: 'CLOSED', text: 'Closed' }),
        window.UI.el('option', { value: 'REOPENED', text: 'Reopened' })
    ]);

    var severitySelect = window.UI.el('select', {}, [
        window.UI.el('option', { value: '', text: 'All Severities' }),
        window.UI.el('option', { value: 'LOW', text: 'Low' }),
        window.UI.el('option', { value: 'MEDIUM', text: 'Medium' }),
        window.UI.el('option', { value: 'HIGH', text: 'High' }),
        window.UI.el('option', { value: 'CRITICAL', text: 'Critical' })
    ]);

    var prioritySelect = window.UI.el('select', {}, [
        window.UI.el('option', { value: '', text: 'All Priorities' }),
        window.UI.el('option', { value: 'LOW', text: 'Low' }),
        window.UI.el('option', { value: 'MEDIUM', text: 'Medium' }),
        window.UI.el('option', { value: 'HIGH', text: 'High' }),
        window.UI.el('option', { value: 'URGENT', text: 'Urgent' })
    ]);

    var assigneeSelect = window.UI.el('select', {}, [
        window.UI.el('option', { value: '', text: 'All Assignees' })
    ]);

    var sortSelect = window.UI.el('select', {}, [
        window.UI.el('option', { value: 'createdAt,desc', text: 'Newest first' }),
        window.UI.el('option', { value: 'createdAt,asc', text: 'Oldest first' }),
        window.UI.el('option', { value: 'updatedAt,desc', text: 'Recently updated' }),
        window.UI.el('option', { value: 'priority,desc', text: 'Highest priority' }),
        window.UI.el('option', { value: 'severity,desc', text: 'Highest severity' }),
        window.UI.el('option', { value: 'title,asc', text: 'Title (A-Z)' })
    ]);

    // Optional: only show bugs that still need an owner (uses the backend
    // `unassigned` filter).
    var unassignedCheckbox = window.UI.el('input', { type: 'checkbox' });
    var unassignedToggle = window.UI.el('label', {
        style: 'display:flex; align-items:center; gap:6px; white-space:nowrap;'
    }, [unassignedCheckbox, window.UI.el('span', { text: 'Unassigned only' })]);

    var resetBtn = window.UI.el('button.btn.btn-secondary', {
        type: 'button',
        text: 'Reset',
        onclick: function () {
            searchInput.value = '';
            statusSelect.value = '';
            severitySelect.value = '';
            prioritySelect.value = '';
            assigneeSelect.value = '';
            sortSelect.value = 'createdAt,desc';
            unassignedCheckbox.checked = false;
            applyFilters();
        }
    });

    filterForm.appendChild(searchInput);
    filterForm.appendChild(statusSelect);
    filterForm.appendChild(severitySelect);
    filterForm.appendChild(prioritySelect);
    filterForm.appendChild(assigneeSelect);
    filterForm.appendChild(sortSelect);
    filterForm.appendChild(resetBtn);
    filterForm.appendChild(unassignedToggle);
    filterBody.appendChild(filterForm);

    filterCard.appendChild(filterBody);
    content.appendChild(filterCard);

    var tableCard = window.UI.el('div.card');
    content.appendChild(tableCard);

    function applyFilters() {
        state.search = searchInput.value.trim();
        state.status = statusSelect.value;
        state.severity = severitySelect.value;
        state.priority = prioritySelect.value;
        state.assigneeId = assigneeSelect.value;
        state.unassigned = Boolean(unassignedCheckbox.checked);
        var sortParts = (sortSelect.value || 'createdAt,desc').split(',');
        state.sortBy = sortParts[0];
        state.sortDirection = sortParts[1] || 'desc';
        state.page = 0;
        loadBugs();
    }

    // Auto-search on select change
    [statusSelect, severitySelect, prioritySelect, assigneeSelect, sortSelect, unassignedCheckbox].forEach(function (sel) {
        sel.addEventListener('change', applyFilters);
    });

    var searchTimer;
    searchInput.addEventListener('input', function () {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(applyFilters, 350);
    });

    function renderTable(pageData) {
        window.UI.clear(tableCard);

        var header = window.UI.el('div.card-header', {}, [
            window.UI.el('div', { text: window.UI.resultsSummary(pageData) })
        ]);
        tableCard.appendChild(header);

        var bugs = pageData.content || [];
        if (!bugs.length) {
            tableCard.appendChild(window.UI.el('div.card-body', {}, [
                window.UI.emptyState('No matching bugs found',
                    'Try adjusting your filters, clearing search criteria, or file a new bug.',
                    window.UI.el('a.btn.btn-primary', { href: 'bug-form.html', text: 'Report Bug' }))
            ]));
            return;
        }

        var table = window.UI.el('table.table');
        var thead = window.UI.el('thead', {}, [
            window.UI.el('tr', {}, [
                window.UI.el('th', { text: 'Code' }),
                window.UI.el('th', { text: 'Title' }),
                window.UI.el('th', { text: 'Status' }),
                window.UI.el('th', { text: 'Severity' }),
                window.UI.el('th', { text: 'Priority' }),
                window.UI.el('th', { text: 'Assignee' }),
                window.UI.el('th', { text: 'Reporter' }),
                window.UI.el('th', { text: 'Created' })
            ])
        ]);

        var tbody = window.UI.el('tbody');
        bugs.forEach(function (bug) {
            var tr = window.UI.el('tr', {
                style: 'cursor: pointer',
                onclick: function () {
                    window.UI.navigate('bug-detail.html?id=' + bug.id);
                }
            }, [
                window.UI.el('td.mono', { text: bug.bugCode || ('#' + bug.id) }),
                window.UI.el('td', { style: 'font-weight: 500' }, [
                    window.UI.el('span', { text: bug.title })
                ]),
                window.UI.el('td', {}, [window.UI.statusBadge(bug.status)]),
                window.UI.el('td', {}, [window.UI.severityBadge(bug.severity)]),
                window.UI.el('td', {}, [window.UI.priorityBadge(bug.priority)]),
                window.UI.el('td', { text: bug.assignedDeveloperName || '—' }),
                window.UI.el('td.text-muted', { text: bug.reporterName || '—' }),
                window.UI.el('td.text-muted.text-small', { text: window.UI.formatDate(bug.createdAt) })
            ]);
            tbody.appendChild(tr);
        });

        table.appendChild(thead);
        table.appendChild(tbody);
        tableCard.appendChild(window.UI.el('div.table-responsive', {}, [table]));

        if (pageData.totalPages > 1) {
            var footer = window.UI.el('div.card-footer', {}, [
                window.UI.pagination(pageData, function (targetPage) {
                    state.page = targetPage;
                    loadBugs();
                })
            ]);
            tableCard.appendChild(footer);
        }
    }

    function loadBugs() {
        window.UI.clear(tableCard);
        tableCard.appendChild(window.UI.loading('Loading bugs…'));

        var queryParams = {
            page: state.page,
            size: state.size,
            sortBy: state.sortBy,
            sortDirection: state.sortDirection
        };
        if (state.search) { queryParams.search = state.search; }
        if (state.status) { queryParams.status = state.status; }
        if (state.severity) { queryParams.severity = state.severity; }
        if (state.priority) { queryParams.priority = state.priority; }
        if (state.assigneeId) { queryParams.assigneeId = state.assigneeId; }
        if (state.unassigned) { queryParams.unassigned = true; }

        window.API.bugs(queryParams).then(function (pageData) {
            renderTable(pageData);
        }).catch(function (error) {
            window.UI.clear(tableCard);
            tableCard.appendChild(window.UI.errorState(error, loadBugs));
        });
    }

    // Populate assignees picker
    window.API.assignableUsers().then(function (users) {
        users.forEach(function (user) {
            assigneeSelect.appendChild(window.UI.el('option', {
                value: user.id,
                text: user.fullName + ' (' + window.UI.roleLabel(user.role) + ')'
            }));
        });
    }).catch(function () {
        // Fallback gracefully if assignables list not loaded
    });

    loadBugs();
})();

