/* =============================================================================
 * api.js — session storage + typed wrapper around the REST API.
 * Exposes window.Session and window.API.
 * ============================================================================= */
(function (global) {
    'use strict';

    var API_BASE = '/api';
    var TOKEN_KEY = 'bugtracker.token';
    var USER_KEY = 'bugtracker.user';

    /* ---------------------------------------------------------------- Session */

    var Session = {
        get token() {
            return localStorage.getItem(TOKEN_KEY);
        },
        get user() {
            try {
                return JSON.parse(localStorage.getItem(USER_KEY) || 'null');
            } catch (err) {
                return null;
            }
        },
        /**
         * Canonical role of the signed-in user, normalised to the short form used
         * by the UI ('ADMIN' | 'TESTER' | 'DEVELOPER'). The REST API serialises
         * the RoleName enum as 'ROLE_ADMIN', so the Spring prefix is stripped once
         * here and every permission check can compare short names.
         */
        get role() {
            var role = Session.user && Session.user.role;
            role = typeof role === 'string' ? role : (role && role.name) || null;
            return role ? role.replace(/^ROLE_/, '') : null;
        },
        isAuthenticated: function () {
            return Boolean(Session.token);
        },
        hasRole: function () {
            var role = Session.role;
            return Array.prototype.slice.call(arguments).indexOf(role) !== -1;
        },
        save: function (authResponse) {
            localStorage.setItem(TOKEN_KEY, authResponse.token);
            localStorage.setItem(USER_KEY, JSON.stringify(authResponse.user || null));
        },
        setUser: function (user) {
            localStorage.setItem(USER_KEY, JSON.stringify(user || null));
        },
        clear: function () {
            localStorage.removeItem(TOKEN_KEY);
            localStorage.removeItem(USER_KEY);
        }
    };

    /* ------------------------------------------------------------ API client */

    function ApiError(status, message, fieldErrors) {
        var error = new Error(message || 'Request failed');
        error.name = 'ApiError';
        error.status = status;
        error.fieldErrors = fieldErrors || {};
        return error;
    }

    /**
     * Thin fetch wrapper: injects the JWT, normalises Spring's error payloads into
     * a single ApiError shape and bounces the browser to sign-in on 401.
     */
    function request(path, options) {
        var settings = options || {};
        var headers = settings.headers || {};
        var body;

        if (settings.auth !== false && Session.token) {
            headers.Authorization = 'Bearer ' + Session.token;
        }

        if (settings.form) {
            body = settings.form;
        } else if (settings.body !== undefined) {
            headers['Content-Type'] = 'application/json';
            body = JSON.stringify(settings.body);
        }

        return fetch(API_BASE + path, {
            method: settings.method || 'GET',
            headers: headers,
            body: body
        }).then(function (response) {
            if (response.status === 401 && settings.auth !== false && !settings.silent401) {
                Session.clear();
                global.location.href = 'index.html?expired=1';
            }
            if (response.status === 204) {
                return null;
            }
            return response.text().then(function (text) {
                var payload = null;
                if (text) {
                    try {
                        payload = JSON.parse(text);
                    } catch (err) {
                        payload = { message: text };
                    }
                }
                if (!response.ok) {
                    throw ApiError(response.status,
                        (payload && (payload.message || payload.error)) ||
                        'Unexpected server response (' + response.status + ')',
                        (payload && payload.fieldErrors) || {});
                }
                return settings.raw ? response : payload;
            });
        }, function () {
            throw ApiError(0, 'Cannot reach the Bug Tracker API. Is the Spring Boot server running?');
        });
    }

    function query(params) {
        var search = new URLSearchParams();
        Object.keys(params || {}).forEach(function (key) {
            var value = params[key];
            if (value !== null && value !== undefined && value !== '') {
                search.set(key, value);
            }
        });
        var str = search.toString();
        return str ? '?' + str : '';
    }

    global.Session = Session;
    global.API = {
        request: request,
        query: query,

        login: function (credentials) {
            return request('/auth/login', { method: 'POST', body: credentials, auth: false, silent401: true });
        },
        register: function (payload) {
            return request('/auth/register', { method: 'POST', body: payload, auth: false, silent401: true });
        },
        me: function () {
            return request('/auth/me', { silent401: true });
        },
        logout: function () {
            return request('/auth/logout', { method: 'POST', silent401: true })
                .catch(function () { /* stateless JWT: advisory only */ });
        },

        bugs: function (filters) {
            return request('/bugs' + query(filters));
        },
        bug: function (id) {
            return request('/bugs/' + id);
        },
        createBug: function (payload) {
            return request('/bugs', { method: 'POST', body: payload });
        },
        updateBug: function (id, payload) {
            return request('/bugs/' + id, { method: 'PUT', body: payload });
        },
        deleteBug: function (id) {
            return request('/bugs/' + id, { method: 'DELETE' });
        },
        changeStatus: function (id, payload) {
            return request('/bugs/' + id + '/status', { method: 'PATCH', body: payload });
        },
        changePriority: function (id, priority) {
            return request('/bugs/' + id + '/priority', { method: 'PATCH', body: { priority: priority } });
        },
        changeSeverity: function (id, severity) {
            return request('/bugs/' + id + '/severity', { method: 'PATCH', body: { severity: severity } });
        },
        assign: function (id, developerId) {
            return request('/bugs/' + id + '/assign', {
                method: 'PATCH',
                body: { assignedDeveloperId: developerId }
            });
        },

        addComment: function (bugId, content) {
            return request('/bugs/' + bugId + '/comments', {
                method: 'POST',
                body: { content: content }
            });
        },
        deleteComment: function (bugId, commentId) {
            return request('/bugs/' + bugId + '/comments/' + commentId, { method: 'DELETE' });
        },

        uploadAttachment: function (bugId, file) {
            var form = new FormData();
            form.append('file', file);
            return request('/bugs/' + bugId + '/attachments', { method: 'POST', form: form });
        },
        deleteAttachment: function (bugId, attachmentId) {
            return request('/bugs/' + bugId + '/attachments/' + attachmentId, { method: 'DELETE' });
        },
        attachmentUrl: function (bugId, attachmentId, inline) {
            return API_BASE + '/bugs/' + bugId + '/attachments/' + attachmentId + '/download' +
                (inline ? '?inline=true&' : '?') + 'token=' + encodeURIComponent(Session.token || '');
        },

        statistics: function () {
            return request('/dashboard/statistics');
        },

        users: function (filters) {
            return request('/users' + query(filters));
        },
        assignableUsers: function () {
            return request('/users/assignable');
        },
        updateUserRole: function (id, role) {
            return request('/users/' + id + '/role', { method: 'PATCH', body: { role: role } });
        },
        updateUserStatus: function (id, enabled) {
            return request('/users/' + id + '/status', { method: 'PATCH', body: { enabled: enabled } });
        }
    };
})(window);

