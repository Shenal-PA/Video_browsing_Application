/**
 * Authentication utility functions for VideoHub
 * Provides common functions to handle user authentication across the site
 */

const authState = {
    authenticated: false,
    user: null,
    initialized: false,
    checking: false
};

function setAuthState(authenticated, user) {
    authState.authenticated = !!authenticated;
    authState.user = user || null;
    authState.initialized = true;
    authState.checking = false;
    window.dispatchEvent(new CustomEvent('auth-state-change', { detail: authState }));
}

// Check if user is authenticated
function isAuthenticated() {
    return authState.authenticated;
}

// Redirect to registration page
function redirectToRegister() {
    window.location.href = '/login.html';
}

function notifyAuthRequired(featureName) {
    const featureLabel = featureName && featureName !== 'this feature'
        ? featureName
        : 'continue';
    const message = `Please log in to ${featureLabel}.`;

    if (typeof window.showAlert === 'function') {
        window.showAlert(message, 'warning');
    } else if (typeof window.alert === 'function') {
        window.alert(message);
    } else {
        console.warn(message);
    }

    window.dispatchEvent(new CustomEvent('auth-required', {
        detail: { feature: featureLabel }
    }));
}

// Check authentication before performing any action
function requireAuth(callback, featureName = 'this feature') {
    if (isAuthenticated()) {
        if (typeof callback === 'function') {
            callback();
        }
        return true;
    }

    if (!authState.initialized || authState.checking) {
        const handler = (event) => {
            if (!event || !event.detail) {
                return;
            }
            window.removeEventListener('auth-state-change', handler);
            if (event.detail.authenticated) {
                if (typeof callback === 'function') {
                    callback();
                }
            } else {
                notifyAuthRequired(featureName);
            }
        };

        window.addEventListener('auth-state-change', handler, { once: true });
        return false;
    }

    notifyAuthRequired(featureName);
    return false;
}

// Helper function to get cookies
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// IMMEDIATE PAGE-LEVEL AUTHENTICATION CHECK
// This runs as soon as the script loads
(function() {
    const publicPathPrefixes = [
        '/',
        '/index',
        '/search',
        '/help-center',
        '/privacy',
        '/report-issue',
        '/become-creator',
        '/all-videos',
        '/register',
        '/login',
        '/video-show',
        '/videos'
    ];

    function isPublicPath(pathname) {
        return publicPathPrefixes.some(prefix => {
            if (prefix === '/') {
                return pathname === '/' || pathname === '';
            }
            return pathname === prefix
                || pathname === `${prefix}.html`
                || pathname.startsWith(`${prefix}/`);
        });
    }

    let enforceTimer = null;

    function scheduleEnforceAuth(delay = 0) {
        if (enforceTimer) {
            clearTimeout(enforceTimer);
        }
        enforceTimer = setTimeout(enforceAuth, delay);
    }

    function enforceAuth() {
        if (isAuthenticated()) {
            return;
        }

        const path = window.location.pathname;
        if (isPublicPath(path)) {
            return;
        }

        if (!authState.initialized || authState.checking) {
            scheduleEnforceAuth(200);
            return;
        }

        redirectToRegister();
    }

    async function bootstrapAuthState() {
        authState.checking = true;

        try {
            const response = await fetch('/api/users/me', {
                credentials: 'include'
            });

            if (response.ok) {
                const user = await response.json();
                setAuthState(true, user);
            } else if (response.status === 401 || response.status === 403) {
                setAuthState(false, null);
            } else {
                console.warn('Auth bootstrap returned unexpected status:', response.status);
                setAuthState(false, null);
            }
        } catch (error) {
            console.warn('Auth bootstrap failed:', error);
            setAuthState(false, null);
        } finally {
            authState.checking = false;
            authState.initialized = true;
            scheduleEnforceAuth();
        }
    }

    window.addEventListener('auth-state-change', () => {
        authState.initialized = true;
        scheduleEnforceAuth();
    });

    bootstrapAuthState();
})();

window.setAuthState = setAuthState;
window.isAuthenticated = isAuthenticated;
window.requireAuth = requireAuth;
