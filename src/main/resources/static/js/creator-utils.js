(function() {
    async function fetchCurrentUser() {
        try {
            const response = await fetch('/api/users/me', {
                credentials: 'include'
            });

            if (!response.ok) {
                if (typeof window.setAuthState === 'function') {
                    window.setAuthState(false, null);
                }
                return null;
            }

            const user = await response.json();
            if (typeof window.setAuthState === 'function') {
                window.setAuthState(true, user);
            }
            window.currentUserProfile = user;
            return user;
        } catch (error) {
            console.error('Failed to retrieve current user:', error);
            return null;
        }
    }

    async function handleBecomeCreatorCTA() {
        const goToUpgrade = () => {
            window.location.href = '/become-creator';
        };

        if (typeof window.isAuthenticated === 'function' && window.isAuthenticated()) {
            goToUpgrade();
            return;
        }

        const user = await fetchCurrentUser();
        if (user) {
            goToUpgrade();
            return;
        }

        if (typeof window.redirectToRegister === 'function') {
            window.redirectToRegister();
        } else {
            window.location.href = '/register';
        }
    }

    if (typeof window.handleBecomeCreatorCTA !== 'function') {
        window.handleBecomeCreatorCTA = handleBecomeCreatorCTA;
    }
})();
