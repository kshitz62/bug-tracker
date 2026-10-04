/* =============================================================================
 * bug-form.js — Create and update bug tickets with full validation.
 * ============================================================================= */
(function () {
    'use strict';

    if (!window.UI.requireAuth()) {
        return;
    }

    var params = new URLSearchParams(window.location.search);
    var bugId = params.get('id');
    var isEdit = Boolean(bugId);

    var content = window.UI.renderShell({
        active: isEdit ? 'bugs' : 'new-bug',
        title: isEdit ? 'Edit Bug Ticket' : 'Report a New Bug',
        subtitle: isEdit
            ? 'Update bug details, expectations, or assign to an engineer.'
            : 'Fill in details below with steps to reproduce and system environment.',
        actions: [
            window.UI.el('a.btn.btn-secondary', {
                href: isEdit ? 'bug-detail.html?id=' + bugId : 'bugs.html'
            }, [window.UI.icon('back'), window.UI.el('span', { text: 'Back' })])
        ]
    });

    var card = window.UI.el('div.card', { style: 'max-width: 900px; margin: 0 auto;' });
    var form = window.UI.el('form', { novalidate: true, style: 'padding: var(--space-6)' });
    card.appendChild(form);
    content.appendChild(card);

    form.innerHTML = [
        '<div class="field">',
        '  <label class="required" for="f-title">Bug Title</label>',
        '  <input type="text" id="f-title" name="title" required maxlength="200" placeholder="e.g. Broken authentication redirect on safari mobile">',
        '  <div class="field-error" data-error-for="title"></div>',
        '</div>',
        '<div class="form-row">',
        '  <div class="field">',
        '    <label class="required" for="f-severity">Severity</label>',
        '    <select id="f-severity" name="severity" required>',
        '      <option value="LOW">Low</option>',
        '      <option value="MEDIUM" selected>Medium</option>',
        '      <option value="HIGH">High</option>',
        '      <option value="CRITICAL">Critical</option>',
        '    </select>',
        '    <div class="field-error" data-error-for="severity"></div>',
        '  </div>',
        '  <div class="field">',
        '    <label class="required" for="f-priority">Priority</label>',
        '    <select id="f-priority" name="priority" required>',
        '      <option value="LOW">Low</option>',
        '      <option value="MEDIUM" selected>Medium</option>',
        '      <option value="HIGH">High</option>',
        '      <option value="URGENT">Urgent</option>',
        '    </select>',
        '    <div class="field-error" data-error-for="priority"></div>',
        '  </div>',
        '  <div class="field">',
        '    <label for="f-assignee">Assignee</label>',
        '    <select id="f-assignee" name="assignedDeveloperId" data-cast="number">',
        '      <option value="">Unassigned</option>',
        '    </select>',
        '    <div class="field-error" data-error-for="assignedDeveloperId"></div>',
        '  </div>',
        '</div>',
        '<div class="field">',
        '  <label for="f-env">Environment</label>',
        '  <input type="text" id="f-env" name="environment" maxlength="200" placeholder="e.g. macOS Sonoma, Chrome 124, Production cluster">',
        '  <div class="field-error" data-error-for="environment"></div>',
        '</div>',
        '<div class="field">',
        '  <label class="required" for="f-desc">Description</label>',
        '  <textarea id="f-desc" name="description" rows="4" required maxlength="5000" placeholder="Summarize the observed problem and symptoms..."></textarea>',
        '  <div class="field-error" data-error-for="description"></div>',
        '</div>',
        '<div class="field">',
        '  <label class="required" for="f-steps">Steps to Reproduce</label>',
        '  <textarea id="f-steps" name="stepsToReproduce" rows="4" required maxlength="5000" placeholder="1. Log into portal&#10;2. Click on payments tab&#10;3. Submit invalid card..."></textarea>',
        '  <div class="field-error" data-error-for="stepsToReproduce"></div>',
        '</div>',
        '<div class="form-row">',
        '  <div class="field">',
        '    <label class="required" for="f-expected">Expected Result</label>',
        '    <textarea id="f-expected" name="expectedResult" rows="3" required maxlength="3000" placeholder="What should have happened..."></textarea>',
        '    <div class="field-error" data-error-for="expectedResult"></div>',
        '  </div>',
        '  <div class="field">',
        '    <label class="required" for="f-actual">Actual Result</label>',
        '    <textarea id="f-actual" name="actualResult" rows="3" required maxlength="3000" placeholder="What actually happened..."></textarea>',
        '    <div class="field-error" data-error-for="actualResult"></div>',
        '  </div>',
        '</div>',
        isEdit ? [
            '<div class="field">',
            '  <label for="f-resolution">Resolution Notes</label>',
            '  <textarea id="f-resolution" name="resolution" rows="3" maxlength="3000" placeholder="Describe root cause and fix details..."></textarea>',
            '  <div class="field-error" data-error-for="resolution"></div>',
            '</div>'
        ].join('') : '',
        '<div class="form-actions" style="margin-top: var(--space-6); display: flex; justify-content: flex-end; gap: var(--space-3)">',
        '  <a class="btn btn-secondary" href="' + (isEdit ? 'bug-detail.html?id=' + bugId : 'bugs.html') + '">Cancel</a>',
        '  <button type="submit" class="btn btn-primary">' + (isEdit ? 'Update Bug' : 'Create Bug') + '</button>',
        '</div>'
    ].join('');

    var assigneeSelect = form.querySelector('[name="assignedDeveloperId"]');

    // Populate assignees
    window.API.assignableUsers().then(function (users) {
        users.forEach(function (user) {
            var opt = document.createElement('option');
            opt.value = user.id;
            opt.textContent = user.fullName + ' (' + window.UI.roleLabel(user.role) + ')';
            assigneeSelect.appendChild(opt);
        });
    }).then(function () {
        if (isEdit) {
            return window.API.bug(bugId).then(function (bug) {
                form.elements.title.value = bug.title || '';
                form.elements.severity.value = bug.severity || 'MEDIUM';
                form.elements.priority.value = bug.priority || 'MEDIUM';
                form.elements.assignedDeveloperId.value = bug.assignedDeveloperId || '';
                form.elements.environment.value = bug.environment || '';
                form.elements.description.value = bug.description || '';
                form.elements.stepsToReproduce.value = bug.stepsToReproduce || '';
                form.elements.expectedResult.value = bug.expectedResult || '';
                form.elements.actualResult.value = bug.actualResult || '';
                if (form.elements.resolution) {
                    form.elements.resolution.value = bug.resolution || '';
                }
            });
        }
    }).catch(function (error) {
        window.UI.toast(error.message || 'Failed to load ticket details.', 'error');
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        window.UI.clearFieldErrors(form);

        var outcome = window.UI.validate(form, {
            title: [window.UI.validators.required, window.UI.validators.maxLength(200)],
            description: [window.UI.validators.required, window.UI.validators.maxLength(5000)],
            stepsToReproduce: [window.UI.validators.required, window.UI.validators.maxLength(5000)],
            expectedResult: [window.UI.validators.required, window.UI.validators.maxLength(3000)],
            actualResult: [window.UI.validators.required, window.UI.validators.maxLength(3000)],
            severity: [window.UI.validators.required],
            priority: [window.UI.validators.required]
        });

        if (Object.keys(outcome.errors).length > 0) {
            window.UI.showFieldErrors(form, outcome.errors);
            window.UI.toast('Please correct marked validation errors.', 'error');
            return;
        }

        var submitBtn = form.querySelector('button[type="submit"]');
        var payload = outcome.values;

        var requestPromise = isEdit
            ? window.API.updateBug(bugId, payload)
            : window.API.createBug(payload);

        window.UI.pending(submitBtn, requestPromise).then(function (res) {
            window.UI.toast(isEdit ? 'Bug successfully updated!' : 'Bug successfully reported!', 'success');
            var targetId = (res && res.id) ? res.id : bugId;
            globalThis.setTimeout(function () {
                window.UI.navigate('bug-detail.html?id=' + targetId);
            }, 300);
        }).catch(function (error) {
            if (error.fieldErrors && Object.keys(error.fieldErrors).length) {
                window.UI.showFieldErrors(form, error.fieldErrors);
            }
            window.UI.toast(error.message || 'Save failed.', 'error');
        });
    });
})();
