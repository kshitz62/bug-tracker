/* =============================================================================
 * bug-detail.js — Full view, workflow transitions, comments, attachments & audit.
 * ============================================================================= */
(function () {
    'use strict';

    if (!window.UI.requireAuth()) {
        return;
    }

    var params = new URLSearchParams(window.location.search);
    var bugId = params.get('id');

    if (!bugId) {
        window.UI.navigate('bugs.html');
        return;
    }

    var currentBug = null;
    var currentRole = window.Session.role;
    var currentUserId = window.Session.user ? window.Session.user.id : null;

    var content = window.UI.renderShell({
        active: 'bugs',
        title: 'Bug Details',
        subtitle: 'Inspecting ticket and lifecycle transitions.',
        actions: [
            window.UI.el('a.btn.btn-secondary', {
                href: 'bugs.html'
            }, [window.UI.icon('back'), window.UI.el('span', { text: 'Back to list' })])
        ]
    });

    function isAssignedToMe(bug) {
        return bug.assignedDeveloperId && String(bug.assignedDeveloperId) === String(currentUserId);
    }

    function canEdit(bug) {
        if (currentRole === 'ADMIN' || currentRole === 'TESTER') { return true; }
        if (currentRole === 'DEVELOPER' && isAssignedToMe(bug)) { return true; }
        return false;
    }

    function canDelete() {
        return currentRole === 'ADMIN';
    }

    function canAssign() {
        return currentRole === 'ADMIN';
    }

    function canChangePriorityOrSeverity() {
        return currentRole === 'ADMIN' || currentRole === 'TESTER';
    }
    function getAvailableTransitions(bug) {
        var status = bug.status;
        var transitions = [];

        if (currentRole === 'ADMIN') {
            if (status === 'OPEN') {
                transitions.push({ to: 'IN_PROGRESS', label: 'Start Work' });
                transitions.push({ to: 'RESOLVED', label: 'Mark Resolved', needsResolution: true });
                transitions.push({ to: 'CLOSED', label: 'Close Ticket', needsResolution: true, tone: 'danger' });
            } else if (status === 'IN_PROGRESS') {
                transitions.push({ to: 'OPEN', label: 'Revert to Open' });
                transitions.push({ to: 'RESOLVED', label: 'Mark Resolved', needsResolution: true });
            } else if (status === 'RESOLVED') {
                transitions.push({ to: 'CLOSED', label: 'Verify & Close', tone: 'success' });
                transitions.push({ to: 'REOPENED', label: 'Reopen Bug', tone: 'warning' });
            } else if (status === 'REOPENED') {
                transitions.push({ to: 'IN_PROGRESS', label: 'Restart Work' });
                transitions.push({ to: 'RESOLVED', label: 'Mark Resolved', needsResolution: true });
            } else if (status === 'CLOSED') {
                transitions.push({ to: 'REOPENED', label: 'Reopen Bug', tone: 'warning' });
            }
        } else if (currentRole === 'DEVELOPER' && isAssignedToMe(bug)) {
            if (status === 'OPEN' || status === 'REOPENED') {
                transitions.push({ to: 'IN_PROGRESS', label: 'Start Progress' });
                transitions.push({ to: 'RESOLVED', label: 'Mark Resolved', needsResolution: true });
            } else if (status === 'IN_PROGRESS') {
                transitions.push({ to: 'OPEN', label: 'Move to Open' });
                transitions.push({ to: 'RESOLVED', label: 'Mark Resolved', needsResolution: true });
            }
        } else if (currentRole === 'TESTER') {
            if (status === 'RESOLVED') {
                transitions.push({ to: 'CLOSED', label: 'Verify & Close', tone: 'success' });
                transitions.push({ to: 'REOPENED', label: 'Reopen Bug', tone: 'warning' });
            }
        }

        return transitions;
    }

    function doTransition(targetStatus, needsResolution) {
        if (needsResolution && !currentBug.resolution) {
            promptResolution(targetStatus);
            return;
        }

        window.UI.confirmDialog({
            title: 'Change status to ' + window.UI.label(targetStatus) + '?',
            message: 'Are you sure you want to transition this bug?'
        }).then(function (confirmed) {
            if (!confirmed) { return; }
            window.API.changeStatus(bugId, {
                status: targetStatus,
                resolution: currentBug.resolution || null
            }).then(function (updated) {
                window.UI.toast('Bug moved to ' + window.UI.label(targetStatus), 'success');
                loadBug();
            }).catch(function (error) {
                window.UI.toast(error.message || 'Status transition failed.', 'error');
            });
        });
    }

    function promptResolution(targetStatus) {
        var textarea = window.UI.el('textarea', {
            rows: 4,
            style: 'width:100%; margin-top: 10px;',
            placeholder: 'Provide a root-cause explanation and fix description (required to resolve)...'
        });

        window.UI.openModal({
            title: 'Resolution Required',
            message: 'Please provide a resolution summary before marking as ' + window.UI.label(targetStatus) + ':',
            body: textarea,
            confirmLabel: 'Save & Transition',
            onConfirm: function (closeModal) {
                var notes = textarea.value.trim();
                if (!notes) {
                    window.UI.toast('Resolution summary cannot be blank.', 'error');
                    return;
                }
                closeModal();
                window.API.changeStatus(bugId, {
                    status: targetStatus,
                    resolution: notes
                }).then(function () {
                    window.UI.toast('Bug marked ' + window.UI.label(targetStatus), 'success');
                    loadBug();
                }).catch(function (error) {
                    window.UI.toast(error.message || 'Failed to update status.', 'error');
                });
            }
        });
    }
    function renderDetail(bug) {
        currentBug = bug;
        window.UI.clear(content);

        var transitions = getAvailableTransitions(bug);
        var workflowButtons = transitions.map(function (tr) {
            return window.UI.el('button.btn.btn-sm', {
                type: 'button',
                class: tr.tone === 'danger' ? 'btn-danger' : (tr.tone === 'success' ? 'btn-primary' : 'btn-secondary'),
                text: tr.label,
                onclick: function () {
                    doTransition(tr.to, tr.needsResolution);
                }
            });
        });

        if (canEdit(bug)) {
            workflowButtons.push(window.UI.el('a.btn.btn-sm.btn-secondary', {
                href: 'bug-form.html?id=' + bug.id,
                text: 'Edit Ticket'
            }));
        }

        if (canDelete()) {
            workflowButtons.push(window.UI.el('button.btn.btn-sm.btn-danger', {
                type: 'button',
                text: 'Delete Bug',
                onclick: function () {
                    window.UI.confirmDialog({
                        title: 'Delete ' + (bug.bugCode || 'Bug') + '?',
                        message: 'This will permanently delete this bug and all attached files.',
                        tone: 'danger',
                        confirmLabel: 'Delete'
                    }).then(function (confirmed) {
                        if (confirmed) {
                            window.API.deleteBug(bug.id).then(function () {
                                window.UI.toast('Bug deleted.', 'success');
                                window.UI.navigate('bugs.html');
                            }).catch(function (err) {
                                window.UI.toast(err.message || 'Delete failed.', 'error');
                            });
                        }
                    });
                }
            }));
        }

        // Topbar header box
        var headerCard = window.UI.el('div.card', { style: 'margin-bottom: var(--space-4)' }, [
            window.UI.el('div.card-body', {}, [
                window.UI.el('div', { style: 'display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:16px;' }, [
                    window.UI.el('div', {}, [
                        window.UI.el('div', { style: 'display:flex; align-items:center; gap:8px; margin-bottom:8px;' }, [
                            window.UI.el('span.mono', { style: 'font-weight:700; font-size:1.1rem; color:var(--text-muted);', text: bug.bugCode || ('#' + bug.id) }),
                            window.UI.statusBadge(bug.status),
                            window.UI.severityBadge(bug.severity),
                            window.UI.priorityBadge(bug.priority)
                        ]),
                        window.UI.el('h1', { style: 'font-size:1.5rem; margin:0 0 8px 0;', text: bug.title }),
                        window.UI.el('div.text-muted.text-small', {
                            text: 'Reported by ' + (bug.reporterName || '—') + ' · ' +
                                window.UI.formatDateTime(bug.createdAt) +
                                (bug.updatedAt ? ' (Updated ' + window.UI.relativeTime(bug.updatedAt) + ')' : '')
                        })
                    ]),
                    window.UI.el('div', { style: 'display:flex; gap:8px; flex-wrap:wrap;' }, workflowButtons)
                ])
            ])
        ]);
        content.appendChild(headerCard);

        var layout = window.UI.el('div.grid-3', { style: 'grid-template-columns: 2fr 1fr; gap: var(--space-4);' });
        var leftCol = window.UI.el('div', { style: 'display:flex; flex-direction:column; gap:var(--space-4);' });
        var rightCol = window.UI.el('div', { style: 'display:flex; flex-direction:column; gap:var(--space-4);' });
        layout.appendChild(leftCol);
        layout.appendChild(rightCol);
        content.appendChild(layout);

        renderLeftColumn(leftCol, bug);
        renderRightColumn(rightCol, bug);
    }
    function renderLeftColumn(parent, bug) {
        // Description Card
        var descCard = window.UI.el('div.card', {}, [
            window.UI.el('div.card-header', {}, [window.UI.el('h2', { text: 'Description' })]),
            window.UI.el('div.card-body', {}, [
                window.UI.el('p', { style: 'white-space: pre-wrap; line-height: 1.6;', text: bug.description || 'No description provided.' })
            ])
        ]);
        parent.appendChild(descCard);

        // Reproduction Steps Card
        var stepsCard = window.UI.el('div.card', {}, [
            window.UI.el('div.card-header', {}, [window.UI.el('h2', { text: 'Steps to Reproduce' })]),
            window.UI.el('div.card-body', {}, [
                window.UI.el('pre', { style: 'white-space: pre-wrap; font-family: inherit; line-height: 1.5;', text: bug.stepsToReproduce || '—' }),
                window.UI.el('hr', { style: 'border:none; border-top:1px solid var(--border-color); margin:16px 0;' }),
                window.UI.el('div.grid-2', {}, [
                    window.UI.el('div', {}, [
                        window.UI.el('strong', { text: 'Expected Result:' }),
                        window.UI.el('p', { style: 'white-space: pre-wrap; margin-top:4px;', text: bug.expectedResult || '—' })
                    ]),
                    window.UI.el('div', {}, [
                        window.UI.el('strong', { text: 'Actual Result:' }),
                        window.UI.el('p', { style: 'white-space: pre-wrap; margin-top:4px;', text: bug.actualResult || '—' })
                    ])
                ])
            ])
        ]);
        parent.appendChild(stepsCard);

        // Resolution Notes Card (if present)
        if (bug.resolution) {
            var resCard = window.UI.el('div.card', {}, [
                window.UI.el('div.card-header', {}, [window.UI.el('h2', { text: 'Resolution Notes' })]),
                window.UI.el('div.card-body', {}, [
                    window.UI.el('p', { style: 'white-space: pre-wrap; line-height:1.5;', text: bug.resolution })
                ])
            ]);
            parent.appendChild(resCard);
        }

        // Comments Stream Card
        renderCommentsCard(parent, bug);

        // Audit History Timeline Card
        renderAuditTimeline(parent, bug);
    }
    function renderRightColumn(parent, bug) {
        // Ticket Meta & Attributes Card
        var metaCard = window.UI.el('div.card', {}, [
            window.UI.el('div.card-header', {}, [window.UI.el('h2', { text: 'Attributes' })]),
            window.UI.el('div.card-body', {}, [
                window.UI.el('div.field', {}, [
                    window.UI.el('label', { text: 'Assignee' }),
                    canAssign() ? createAssigneePicker(bug) : window.UI.el('p', {
                        text: bug.assignedDeveloperName ? (bug.assignedDeveloperName + ' (' + bug.assignedDeveloperEmail + ')') : 'Unassigned'
                    })
                ]),
                window.UI.el('div.field', {}, [
                    window.UI.el('label', { text: 'Severity' }),
                    canChangePriorityOrSeverity() ? createSeverityPicker(bug) : window.UI.severityBadge(bug.severity)
                ]),
                window.UI.el('div.field', {}, [
                    window.UI.el('label', { text: 'Priority' }),
                    canChangePriorityOrSeverity() ? createPriorityPicker(bug) : window.UI.priorityBadge(bug.priority)
                ]),
                window.UI.el('div.field', {}, [
                    window.UI.el('label', { text: 'Environment' }),
                    window.UI.el('p', { text: bug.environment || 'None specified' })
                ]),
                window.UI.el('div.field', {}, [
                    window.UI.el('label', { text: 'Reporter' }),
                    window.UI.el('p', { text: (bug.reporterName || '—') + (bug.reporterEmail ? ' (' + bug.reporterEmail + ')' : '') })
                ])
            ])
        ]);
        parent.appendChild(metaCard);

        // Attachments Card
        renderAttachmentsCard(parent, bug);
    }

    function createAssigneePicker(bug) {
        var select = window.UI.el('select', { style: 'width: 100%' }, [
            window.UI.el('option', { value: '', text: 'Unassigned' })
        ]);
        window.API.assignableUsers().then(function (users) {
            users.forEach(function (user) {
                var opt = window.UI.el('option', {
                    value: user.id,
                    text: user.fullName + ' (' + window.UI.roleLabel(user.role) + ')'
                });
                if (bug.assignedDeveloperId && String(bug.assignedDeveloperId) === String(user.id)) {
                    opt.selected = true;
                }
                select.appendChild(opt);
            });
        });
        select.addEventListener('change', function () {
            var devId = select.value ? Number(select.value) : null;
            window.API.assign(bug.id, devId).then(function () {
                window.UI.toast('Assignee updated.', 'success');
                loadBug();
            }).catch(function (err) {
                window.UI.toast(err.message || 'Failed to update assignee.', 'error');
            });
        });
        return select;
    }

    function createSeverityPicker(bug) {
        var select = window.UI.el('select', { style: 'width: 100%' }, [
            window.UI.el('option', { value: 'LOW', text: 'Low' }),
            window.UI.el('option', { value: 'MEDIUM', text: 'Medium' }),
            window.UI.el('option', { value: 'HIGH', text: 'High' }),
            window.UI.el('option', { value: 'CRITICAL', text: 'Critical' })
        ]);
        select.value = bug.severity;
        select.addEventListener('change', function () {
            window.API.changeSeverity(bug.id, select.value).then(function () {
                window.UI.toast('Severity updated.', 'success');
                loadBug();
            }).catch(function (err) {
                window.UI.toast(err.message || 'Failed to change severity.', 'error');
            });
        });
        return select;
    }

    function createPriorityPicker(bug) {
        var select = window.UI.el('select', { style: 'width: 100%' }, [
            window.UI.el('option', { value: 'LOW', text: 'Low' }),
            window.UI.el('option', { value: 'MEDIUM', text: 'Medium' }),
            window.UI.el('option', { value: 'HIGH', text: 'High' }),
            window.UI.el('option', { value: 'URGENT', text: 'Urgent' })
        ]);
        select.value = bug.priority;
        select.addEventListener('change', function () {
            window.API.changePriority(bug.id, select.value).then(function () {
                window.UI.toast('Priority updated.', 'success');
                loadBug();
            }).catch(function (err) {
                window.UI.toast(err.message || 'Failed to change priority.', 'error');
            });
        });
        return select;
    }
    function renderAttachmentsCard(parent, bug) {
        var card = window.UI.el('div.card');
        var header = window.UI.el('div.card-header', {}, [
            window.UI.el('h2', { text: 'Attachments (' + (bug.attachments ? bug.attachments.length : 0) + ')' })
        ]);
        var body = window.UI.el('div.card-body');
        card.appendChild(header);
        card.appendChild(body);

        var list = window.UI.el('div.attachment-list', { style: 'display:flex; flex-direction:column; gap:8px;' });
        (bug.attachments || []).forEach(function (att) {
            var isImage = att.contentType && att.contentType.indexOf('image/') === 0;
            var downloadUrl = window.API.attachmentUrl(bug.id, att.id, false);
            var previewUrl = window.API.attachmentUrl(bug.id, att.id, true);

            var item = window.UI.el('div.attachment-item', {
                style: 'display:flex; align-items:center; justify-content:space-between; padding:8px 12px; background:var(--bg-subtle); border-radius:var(--radius-md); border:1px solid var(--border-color);'
            }, [
                window.UI.el('div', { style: 'overflow:hidden; text-overflow:ellipsis; white-space:nowrap; max-width:180px;' }, [
                    window.UI.el('a', {
                        href: isImage ? previewUrl : downloadUrl,
                        target: '_blank',
                        style: 'font-weight:500; text-decoration:none;',
                        text: att.originalFileName
                    }),
                    window.UI.el('div.text-muted.text-small', { text: window.UI.formatBytes(att.fileSize) })
                ]),
                window.UI.el('div', { style: 'display:flex; gap:6px;' }, [
                    window.UI.el('a.btn.btn-sm.btn-secondary', {
                        href: downloadUrl,
                        target: '_blank',
                        text: '↓'
                    }),
                    (currentRole === 'ADMIN' || (att.uploadedByName && att.uploadedByName === (window.Session.user && window.Session.user.fullName))) ? window.UI.el('button.btn.btn-sm.btn-danger', {
                        type: 'button',
                        text: '×',
                        onclick: function () {
                            window.UI.confirmDialog({
                                title: 'Remove attachment?',
                                message: 'Delete ' + att.originalFileName + '?',
                                tone: 'danger'
                            }).then(function (yes) {
                                if (yes) {
                                    window.API.deleteAttachment(bug.id, att.id).then(function () {
                                        window.UI.toast('Attachment deleted.', 'success');
                                        loadBug();
                                    }).catch(function (err) {
                                        window.UI.toast(err.message || 'Failed to delete attachment.', 'error');
                                    });
                                }
                            });
                        }
                    }) : null
                ])
            ]);
            list.appendChild(item);
        });

        if (!bug.attachments || !bug.attachments.length) {
            list.appendChild(window.UI.el('p.text-muted.text-small', { text: 'No files attached yet.' }));
        }
        body.appendChild(list);

        // Upload input box
        var uploadArea = window.UI.el('div', { style: 'margin-top:16px;' }, [
            window.UI.el('input', {
                type: 'file',
                id: 'file-upload',
                style: 'display:none',
                onchange: function (event) {
                    var file = event.target.files[0];
                    if (!file) { return; }
                    window.UI.toast('Uploading ' + file.name + '…', 'info');
                    window.API.uploadAttachment(bug.id, file).then(function () {
                        window.UI.toast('File attached.', 'success');
                        loadBug();
                    }).catch(function (err) {
                        window.UI.toast(err.message || 'Upload failed.', 'error');
                    });
                }
            }),
            window.UI.el('button.btn.btn-secondary.btn-block', {
                type: 'button',
                onclick: function () {
                    var input = document.getElementById('file-upload');
                    if (input) { input.click(); }
                }
            }, [window.UI.icon('paperclip'), window.UI.el('span', { text: 'Attach File' })])
        ]);
        body.appendChild(uploadArea);
        parent.appendChild(card);
    }
    function renderCommentsCard(parent, bug) {
        var card = window.UI.el('div.card');
        var header = window.UI.el('div.card-header', {}, [
            window.UI.el('h2', { text: 'Comments (' + (bug.comments ? bug.comments.length : 0) + ')' })
        ]);
        var body = window.UI.el('div.card-body');
        card.appendChild(header);
        card.appendChild(body);

        var list = window.UI.el('div.comment-list', { style: 'display:flex; flex-direction:column; gap:16px;' });
        (bug.comments || []).forEach(function (c) {
            var item = window.UI.el('div.comment-item', {
                style: 'border-bottom:1px solid var(--border-color); padding-bottom:12px;'
            }, [
                window.UI.el('div', { style: 'display:flex; justify-content:space-between; margin-bottom:6px;' }, [
                    window.UI.el('span', { style: 'font-weight:600;', text: c.authorName || 'User' }),
                    window.UI.el('div', { style: 'display:flex; gap:8px; align-items:center;' }, [
                        window.UI.el('span.text-muted.text-small', { text: window.UI.relativeTime(c.createdAt) }),
                        (currentRole === 'ADMIN' || (c.authorId && String(c.authorId) === String(currentUserId))) ? window.UI.el('button.btn.btn-sm.btn-danger', {
                            type: 'button',
                            text: '×',
                            onclick: function () {
                                window.UI.confirmDialog({
                                    title: 'Delete comment?',
                                    message: 'This will delete this comment permanently.',
                                    tone: 'danger'
                                }).then(function (yes) {
                                    if (yes) {
                                        window.API.deleteComment(bug.id, c.id).then(function () {
                                            window.UI.toast('Comment deleted.', 'success');
                                            loadBug();
                                        }).catch(function (err) {
                                            window.UI.toast(err.message || 'Failed to delete comment.', 'error');
                                        });
                                    }
                                });
                            }
                        }) : null
                    ])
                ]),
                window.UI.el('p', { style: 'white-space: pre-wrap; margin:0;', text: c.content })
            ]);
            list.appendChild(item);
        });

        if (!bug.comments || !bug.comments.length) {
            list.appendChild(window.UI.el('p.text-muted', { text: 'No comments yet. Join the conversation below.' }));
        }
        body.appendChild(list);

        // Add Comment Form
        var form = window.UI.el('form', {
            style: 'margin-top:20px;',
            onsubmit: function (event) {
                event.preventDefault();
                var txt = form.elements.content.value.trim();
                if (!txt) { return; }
                var btn = form.querySelector('button[type="submit"]');
                window.UI.pending(btn, window.API.addComment(bug.id, txt)).then(function () {
                    form.elements.content.value = '';
                    window.UI.toast('Comment posted.', 'success');
                    loadBug();
                }).catch(function (err) {
                    window.UI.toast(err.message || 'Failed to post comment.', 'error');
                });
            }
        }, [
            window.UI.el('div.field', {}, [
                window.UI.el('textarea', {
                    name: 'content',
                    rows: 3,
                    required: true,
                    placeholder: 'Leave a comment, investigation note, or update...'
                })
            ]),
            window.UI.el('div', { style: 'display:flex; justify-content:flex-end;' }, [
                window.UI.el('button.btn.btn-primary', { type: 'submit', text: 'Post Comment' })
            ])
        ]);
        body.appendChild(form);
        parent.appendChild(card);
    }
    function renderAuditTimeline(parent, bug) {
        var card = window.UI.el('div.card');
        var header = window.UI.el('div.card-header', {}, [
            window.UI.el('h2', { text: 'Audit History Timeline' })
        ]);
        var body = window.UI.el('div.card-body');
        card.appendChild(header);
        card.appendChild(body);

        var history = bug.history || [];
        if (!history.length) {
            body.appendChild(window.UI.el('p.text-muted', { text: 'No changes recorded for this bug yet.' }));
            parent.appendChild(card);
            return;
        }

        var timeline = window.UI.el('div.timeline');
        history.forEach(function (h) {
            var item = window.UI.el('div.timeline-item', {}, [
                window.UI.el('div.timeline-dot'),
                window.UI.el('div.timeline-content', {}, [
                    window.UI.el('div', { style: 'font-weight:600; margin-bottom:2px;' }, [
                        window.UI.el('span', { text: h.changedByName || 'System' }),
                        window.UI.el('span.text-muted', { text: ' changed ' }),
                        window.UI.el('strong', { text: h.fieldName })
                    ]),
                    window.UI.el('div.text-small', {}, [
                        window.UI.el('span.text-muted', { text: 'From ' }),
                        window.UI.el('code', { text: h.oldValue || 'none' }),
                        window.UI.el('span.text-muted', { text: ' → ' }),
                        window.UI.el('code', { text: h.newValue || 'none' })
                    ]),
                    window.UI.el('div.text-muted.text-small', {
                        style: 'margin-top:4px;',
                        text: window.UI.formatDateTime(h.changedAt)
                    })
                ])
            ]);
            timeline.appendChild(item);
        });
        body.appendChild(timeline);
        parent.appendChild(card);
    }

    function loadBug() {
        window.UI.clear(content);
        content.appendChild(window.UI.loading('Loading bug details…'));

        window.API.bug(bugId).then(function (bug) {
            renderDetail(bug);
        }).catch(function (error) {
            window.UI.clear(content);
            content.appendChild(window.UI.errorState(error, loadBug));
        });
    }

    loadBug();
})();
