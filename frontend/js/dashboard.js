/* =============================================================================
 * dashboard.js — Metrics, severity/status charts, and recent activity.
 * ============================================================================= */
(function () {
    'use strict';

    if (!window.UI.requireAuth()) {
        return;
    }

    var content = window.UI.renderShell({
        active: 'dashboard',
        title: 'Engineering Dashboard',
        subtitle: 'Real-time bug lifecycle, severity distribution, and workload.',
        actions: [
            window.UI.el('button.btn.btn-secondary', {
                type: 'button',
                onclick: loadDashboard
            }, [window.UI.icon('clock'), window.UI.el('span', { text: 'Refresh' })]),
            window.UI.el('a.btn.btn-primary', {
                href: 'bug-form.html'
            }, [window.UI.icon('plus'), window.UI.el('span', { text: 'Report Bug' })])
        ]
    });

    function statCard(label, value, tone, sub) {
        return window.UI.el('div.stat-card', { dataset: { tone: tone || 'neutral' } }, [
            window.UI.el('div.stat-label', { text: label }),
            window.UI.el('div.stat-value', { text: String(value !== undefined && value !== null ? value : 0) }),
            sub ? window.UI.el('div.stat-sub', { text: sub }) : null
        ]);
    }

    function renderDistribution(title, map, badgeFn) {
        var card = window.UI.el('div.card', {}, [
            window.UI.el('div.card-header', {}, [
                window.UI.el('h2', { text: title })
            ])
        ]);

        var body = window.UI.el('div.card-body');
        var keys = Object.keys(map || {});
        var total = keys.reduce(function (sum, key) {
            return sum + (map[key] || 0);
        }, 0);

        if (!keys.length || total === 0) {
            body.appendChild(window.UI.el('p.text-muted', { text: 'No bug data recorded yet.' }));
            card.appendChild(body);
            return card;
        }

        var list = window.UI.el('div.chart-list');
        keys.forEach(function (key) {
            var count = map[key] || 0;
            var pct = total > 0 ? Math.round((count / total) * 100) : 0;

            var row = window.UI.el('div.chart-row', {}, [
                window.UI.el('div.chart-label', {}, [
                    badgeFn ? badgeFn(key) : window.UI.el('span', { text: window.UI.label(key) })
                ]),
                window.UI.el('div.chart-bar-bg', {}, [
                    window.UI.el('div.chart-bar-fill', {
                        style: 'width: ' + pct + '%;'
                    })
                ]),
                window.UI.el('div.chart-count', { text: count + ' (' + pct + '%)' })
            ]);
            list.appendChild(row);
        });

        body.appendChild(list);
        card.appendChild(body);
        return card;
    }

    function renderRecentTable(bugs) {
        var card = window.UI.el('div.card', {}, [
            window.UI.el('div.card-header', {}, [
                window.UI.el('h2', { text: 'Recent Bug Activity' }),
                window.UI.el('a.btn.btn-sm.btn-secondary', {
                    href: 'bugs.html',
                    text: 'View all →'
                })
            ])
        ]);

        if (!bugs || !bugs.length) {
            card.appendChild(window.UI.el('div.card-body', {}, [
                window.UI.emptyState('No bugs reported yet', 'Be the first to submit a new issue ticket.',
                    window.UI.el('a.btn.btn-primary', { href: 'bug-form.html', text: 'Report Bug' }))
            ]));
            return card;
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
                window.UI.el('th', { text: 'Updated' })
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
                window.UI.el('td', { text: bug.assignedDeveloperName || 'Unassigned' }),
                window.UI.el('td.text-muted.text-small', { text: window.UI.relativeTime(bug.updatedAt) })
            ]);
            tbody.appendChild(tr);
        });

        table.appendChild(thead);
        table.appendChild(tbody);
        card.appendChild(window.UI.el('div.table-responsive', {}, [table]));
        return card;
    }

    function loadDashboard() {
        window.UI.clear(content);
        content.appendChild(window.UI.loading('Loading dashboard metrics…'));

        Promise.all([
            window.API.statistics(),
            window.API.bugs({ page: 0, size: 8, sortBy: 'updatedAt', sortDirection: 'desc' })
        ]).then(function (results) {
            var stats = results[0];
            var bugsPage = results[1];
            window.UI.clear(content);

            var statGrid = window.UI.el('div.stat-grid', {}, [
                statCard('Total Bugs', stats.totalBugs, 'neutral', 'All recorded tickets'),
                statCard('Open & In Progress', (stats.openBugs || 0) + (stats.inProgressBugs || 0), 'info',
                    (stats.openBugs || 0) + ' open · ' + (stats.inProgressBugs || 0) + ' in progress'),
                statCard('Critical Severity', stats.criticalBugs, 'danger',
                    (stats.urgentPriorityBugs || 0) + ' marked urgent'),
                statCard('Resolved / Closed', (stats.resolvedBugs || 0) + (stats.closedBugs || 0), 'success',
                    (stats.closedBugs || 0) + ' closed · ' + (stats.resolvedBugs || 0) + ' resolved'),
                statCard('Assigned to Me', stats.assignedToMe, 'warning', 'Action items on your desk'),
                statCard('Unassigned', stats.unassignedBugs, 'neutral', 'Awaiting triage or assignment')
            ]);
            content.appendChild(statGrid);

            var chartsGrid = window.UI.el('div.grid-2', {}, [
                renderDistribution('Status Breakdown', stats.statusDistribution, window.UI.statusBadge),
                renderDistribution('Severity Breakdown', stats.severityDistribution, window.UI.severityBadge)
            ]);
            content.appendChild(chartsGrid);

            var recentItems = (bugsPage && bugsPage.content) || [];
            content.appendChild(renderRecentTable(recentItems));
        }).catch(function (error) {
            window.UI.clear(content);
            content.appendChild(window.UI.errorState(error, loadDashboard));
        });
    }

    loadDashboard();
})();

