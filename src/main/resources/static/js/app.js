// VideoHub - Main JavaScript File

// Global variables
let currentUser = null;
let userAuthenticated = false;
let currentVideoId = null;
let currentVideoData = null;
let currentComments = [];
let userPlaylists = [];
let playlistsLoaded = false;
let privateVideoPlayHandler = null;
let pendingRatingScore = 0;
let commentWordLimitWarned = false;
const serverProvidedVideo = typeof window !== 'undefined' ? window.__INITIAL_VIDEO__ : null;
const serverProvidedVideoId = typeof window !== 'undefined' ? window.__REQUESTED_VIDEO_ID__ : null;

function normalizeVideoId(value) {
    if (value == null) {
        return null;
    }

    if (typeof value === 'number') {
        return Number.isFinite(value) ? value.toString() : null;
    }

    if (typeof value === 'string') {
        const trimmed = value.trim();
        if (!trimmed || trimmed.toLowerCase() === 'null' || trimmed.toLowerCase() === 'undefined') {
            return null;
        }
        return trimmed;
    }

    try {
        return normalizeVideoId(String(value));
    } catch (error) {
        return null;
    }
}

function collectDomVideoIds() {
    return Array.from(document.querySelectorAll('[data-video-id]'))
        .map(element => normalizeVideoId(element?.dataset?.videoId ?? element?.getAttribute?.('data-video-id')))
        .filter(Boolean);
}

function ensureCurrentVideoId({ refreshDom = false } = {}) {
    const normalized = normalizeVideoId(currentVideoId);
    if (!refreshDom && normalized) {
        currentVideoId = normalized;
        return currentVideoId;
    }

    const candidates = [
        normalizeVideoId(getVideoIdFromUrl()),
        normalizeVideoId(serverProvidedVideoId),
        normalizeVideoId(serverProvidedVideo?.id)
    ];

    const domCandidates = collectDomVideoIds();
    if (refreshDom) {
        candidates.push(...domCandidates);
    } else {
        candidates.push(...domCandidates.filter(Boolean));
    }

    const discovered = candidates.find(Boolean);
    if (discovered) {
        currentVideoId = discovered;
        return currentVideoId;
    }

    currentVideoId = null;
    return null;
}

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
});

window.addEventListener('auth-state-change', handleAuthStateChange);

async function initializeApp() {
    try {
        await checkAuthStatus();
        setupEventListeners();
        initializePageSpecificFeatures();
    } catch (error) {
        console.error('Error initializing app:', error);
    }
}

async function checkAuthStatus() {
    try {
        const response = await fetch('/api/users/me', {
            credentials: 'include'
        });

        if (response.ok) {
            currentUser = await response.json();
            userAuthenticated = true;
            if (window.setAuthState) {
                setAuthState(true, currentUser);
            }
            updateUIForAuthenticatedUser();
        } else {
            userAuthenticated = false;
            if (window.setAuthState) {
                setAuthState(false, null);
            }
            updateUIForUnauthenticatedUser();
        }
    } catch (error) {
        console.error('Error checking auth status:', error);
        userAuthenticated = false;
        if (window.setAuthState) {
            setAuthState(false, null);
        }
    }
}

function updateUIForAuthenticatedUser() {
    // Update navigation and user interface for logged-in users
    const userElements = document.querySelectorAll('.auth-required');
    userElements.forEach(el => el.style.display = 'block');

    const guestElements = document.querySelectorAll('.guest-only');
    guestElements.forEach(el => el.style.display = 'none');

    if (currentUser) {
        const welcomeUser = document.getElementById('welcomeUser');
        const avatarImg = document.getElementById('navProfileAvatar');
        if (welcomeUser) {
            const name = currentUser.firstName || currentUser.firstname || currentUser.username || 'User';
            welcomeUser.textContent = name.length > 18 ? name.substring(0, 15) + '…' : name;
        }
        if (avatarImg) {
            avatarImg.src = currentUser.profilePicture || '/images/default-avatar.jpg';
        }
    }
}

function updateUIForUnauthenticatedUser() {
    // Update navigation and user interface for guests
    const userElements = document.querySelectorAll('.auth-required');
    userElements.forEach(el => el.style.display = 'none');

    const guestElements = document.querySelectorAll('.guest-only');
    guestElements.forEach(el => el.style.display = 'block');

    const welcomeUser = document.getElementById('welcomeUser');
    if (welcomeUser) {
        welcomeUser.textContent = '';
    }
    const avatarImg = document.getElementById('navProfileAvatar');
    if (avatarImg) {
        avatarImg.src = '/images/default-avatar.jpg';
    }
}

function setupEventListeners() {
    // Search functionality
    const searchInputs = new Set();

    ['searchInput', 'mainSearch', 'headerSearchInput'].forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            searchInputs.add(element);
        }
    });

    document
        .querySelectorAll('.nav-search input[type="search"], .nav-search input[type="text"]')
        .forEach(input => searchInputs.add(input));

    searchInputs.forEach(input => {
        if (!input) {
            return;
        }
        input.dataset.globalSearch = 'true';
        input.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchVideos(e);
            }
        });
    });

    document
        .querySelectorAll('form.nav-search')
        .forEach(form => {
            form.addEventListener('submit', handleNavSearchSubmit);
        });

    // Modal close functionality
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('modal')) {
            e.target.style.display = 'none';
};
    });
}

function initializePageSpecificFeatures() {
    if (document.getElementById('videoPlayer')) {
        initializeVideoDetailPage();
    }
}

function handleAuthStateChange(event) {
    if (event?.detail) {
        userAuthenticated = !!event.detail.authenticated;
        currentUser = event.detail.user || null;
    }

    updateCommentAuthVisibility();

    if (currentVideoData) {
        updateVideoActionButtons({
            liked: !!currentVideoData.likedByCurrentUser,
            disliked: !!currentVideoData.dislikedByCurrentUser,
            likeCount: currentVideoData.likeCount ?? 0,
            dislikeCount: currentVideoData.dislikeCount ?? 0
        });
    }

    const overlay = document.getElementById('videoOverlay');
    if (overlay) {
        overlay.style.display = isAuthenticated ? 'none' : overlay.style.display;
    }

    configureVideoAccess(currentVideoData);
}

    // Search functionality
function searchVideos(source) {
    if (source instanceof Event && typeof source.preventDefault === 'function') {
        source.preventDefault();
    }
    const searchInput = resolveSearchInput(source);
    const query = searchInput?.value?.trim() || '';
    const form = searchInput?.form;
    const baseAction = form?.getAttribute?.('action') || '/search';

    const params = new URLSearchParams();
    if (query) {
        params.set('keyword', query);
    }

    const category = searchInput?.dataset?.category?.trim();
    if (category) {
        params.set('category', category);
    }

    const destination = params.toString()
        ? `${baseAction}${baseAction.includes('?') ? '&' : '?'}${params.toString()}`
        : baseAction;
    window.location.href = destination;
}

function displaySearchResults() {
    searchVideos();
}

function resolveSearchInput(source) {
    if (source instanceof Event) {
        const eventTarget = source.target instanceof HTMLElement ? source.target : null;
        const fromForm = eventTarget?.closest('form');
        const formInput = fromForm?.querySelector('input[type="search"], input[type="text"], input[name="keyword"]');
        if (formInput) {
            return formInput;
        }
        if (eventTarget?.matches('input[type="search"], input[type="text"]')) {
            return eventTarget;
        }
        if (source.currentTarget instanceof HTMLElement && source.currentTarget.matches('form')) {
            const directInput = source.currentTarget.querySelector('input[type="search"], input[type="text"], input[name="keyword"]');
            if (directInput) {
                return directInput;
            }
        }
    } else if (source instanceof HTMLElement) {
        if (source.matches('input[type="search"], input[type="text"]')) {
            return source;
        }
        const attachedInput = source.querySelector?.('input[type="search"], input[type="text"], input[name="keyword"]');
        if (attachedInput) {
            return attachedInput;
        }
        const parentInput = source.closest?.('form')?.querySelector('input[type="search"], input[type="text"], input[name="keyword"]');
        if (parentInput) {
            return parentInput;
        }
    } else if (typeof source === 'string') {
        const byId = document.getElementById(source);
        if (byId) {
            return byId;
        }
    }

    const activeElement = document.activeElement;
    if (activeElement?.matches?.('input[type="search"], input[type="text"]')) {
        return activeElement;
    }

    const preferredSelectors = [
        '.nav-search input[type="search"]',
        '.nav-search input[type="text"]',
        '.nav-search input[name="keyword"]',
        'input[data-global-search="true"]'
    ];

    for (const selector of preferredSelectors) {
        const input = document.querySelector(selector);
        if (input) {
            return input;
        }
    }

    const fallbackIds = ['searchInput', 'mainSearch', 'headerSearchInput'];
    for (const id of fallbackIds) {
        const element = document.getElementById(id);
        if (element) {
            return element;
        }
    }

    return null;
}

function handleNavSearchSubmit(event) {
    searchVideos(event);
    return false;
}

// Authentication functions
async function logout() {
    try {
        const response = await fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'include'
        });

        if (response.ok) {
            window.location.href = '/login.html';
        } else {
            showAlert('Logout failed', 'error');
        }
    } catch (error) {
        console.error('Logout error:', error);
        showAlert('Logout failed', 'error');
    }
}

// Video functions
async function addToWatchLater(videoId) {
    if (!requireAuth(null, 'add videos to Watch Later')) {
        return;
    }

    if (!videoId) {
        showAlert('Video is still loading. Please try again in a moment.', 'warning');
        return;
    }

    try {
        const response = await fetch(`/api/watch-later/${videoId}`, {
            method: 'POST',
            credentials: 'include'
        });

        if (response.ok) {
            let payload = null;
            try {
                payload = await response.json();
            } catch (err) {
                payload = null;
            }
            document.dispatchEvent(new CustomEvent('watch-later-updated', {
                detail: { action: 'add', videoId, item: payload }
            }));
            showAlert('Added to Watch Later', 'success');
            if (currentVideoData && String(currentVideoId) === String(videoId)) {
                currentVideoData.addedToWatchLater = true;
            }
        } else {
            if (response.status === 401) {
                requireAuth(null, 'add videos to Watch Later');
                return;
            }
            if (response.status === 409) {
                const message = await response.text();
                showAlert(message || 'Video is already in your Watch Later list.', 'info');
                return;
            }
            const message = await response.text();
            showAlert(message || 'Failed to add to Watch Later', 'error');
        }
    } catch (error) {
        console.error('Error adding to watch later:', error);
        showAlert('Failed to add to Watch Later', 'error');
    }
}

async function removeFromWatchLater(videoId, { silent = false } = {}) {
    if (!isAuthenticated) {
        if (!silent) {
            requireAuth(null, 'manage Watch Later');
        }
        return;
    }

    try {
        const response = await fetch(`/api/watch-later/${videoId}`, {
            method: 'DELETE',
            credentials: 'include'
        });

        if (response.status === 401) {
            if (!silent) {
                requireAuth(null, 'manage Watch Later');
            }
            return;
        }

        if (response.ok || response.status === 204) {
            document.dispatchEvent(new CustomEvent('watch-later-updated', {
                detail: { action: 'remove', videoId }
            }));
            if (!silent) {
                showAlert('Removed from Watch Later', 'success');
            }
            if (currentVideoData && String(currentVideoId) === String(videoId)) {
                currentVideoData.addedToWatchLater = false;
            }
        } else if (!silent) {
            const message = await response.text();
            showAlert(message || 'Failed to remove from Watch Later', 'error');
        }
    } catch (error) {
        console.error('Error removing from watch later:', error);
        if (!silent) {
            showAlert('Failed to remove from Watch Later', 'error');
        }
    }
}

function goToReportPage() {
    if (!currentVideoId) {
        showAlert('Video is still loading. Please try again in a moment.', 'warning');
        return;
    }

    const title = currentVideoData?.title || document.getElementById('videoTitle')?.textContent || '';
    const shareUrl = `${window.location.origin}/video-show?id=${currentVideoId}`;

    const params = new URLSearchParams();
    params.set('videoId', currentVideoId);
    if (title) {
        params.set('videoTitle', title);
    }
    params.set('videoUrl', shareUrl);

    window.location.href = `/report-issue.html?${params.toString()}`;
}

async function toggleLike(videoId = currentVideoId) {
    if (!requireAuth(null, 'like videos')) {
        return;
    }

    if (!videoId) {
        console.warn('toggleLike called without a videoId');
        return;
    }

    try {
        const response = await fetch(`/api/videos/${videoId}/like`, {
            method: 'POST',
            credentials: 'include'
        });

        if (response.status === 401) {
            requireAuth(null, 'like videos');
            return;
        }

        if (response.ok) {
            const result = await response.json();
            updateRatingButtons(videoId, result);
            if (currentVideoId === videoId && currentVideoData) {
                currentVideoData.likeCount = result.likeCount;
                currentVideoData.dislikeCount = result.dislikeCount;
                currentVideoData.likedByCurrentUser = result.liked;
                currentVideoData.dislikedByCurrentUser = result.disliked;
            }
        } else {
            const message = await response.text();
            showAlert(message || 'Failed to update like status', 'error');
        }
    } catch (error) {
        console.error('Error toggling like:', error);
        showAlert('Failed to update like status', 'error');
    }
}

async function toggleDislike(videoId = currentVideoId) {
    if (!requireAuth(null, 'dislike videos')) {
        return;
    }

    if (!videoId) {
        console.warn('toggleDislike called without a videoId');
        return;
    }

    try {
        const response = await fetch(`/api/videos/${videoId}/dislike`, {
            method: 'POST',
            credentials: 'include'
        });

        if (response.status === 401) {
            requireAuth(null, 'dislike videos');
            return;
        }

        if (response.ok) {
            const result = await response.json();
            updateRatingButtons(videoId, result);
            if (currentVideoId === videoId && currentVideoData) {
                currentVideoData.likeCount = result.likeCount;
                currentVideoData.dislikeCount = result.dislikeCount;
                currentVideoData.likedByCurrentUser = result.liked;
                currentVideoData.dislikedByCurrentUser = result.disliked;
            }
        } else {
            const message = await response.text();
            showAlert(message || 'Failed to update dislike status', 'error');
        }
    } catch (error) {
        console.error('Error toggling dislike:', error);
        showAlert('Failed to update dislike status', 'error');
    }
}

function updateLikeButton(videoId, isLiked, likeCount) {
    const likeBtn = document.querySelector(`[data-video-id="${videoId}"] .like-btn`);
    const likeCountSpan = document.querySelector(`[data-video-id="${videoId}"] .like-count`);

    if (likeBtn) {
        likeBtn.textContent = isLiked ? '👍 Liked' : '👍 Like';
        likeBtn.classList.toggle('liked', isLiked);
    }

    if (likeCountSpan) {
        likeCountSpan.textContent = likeCount;
    }
}

function fallbackShare(url, title) {
    // Copy to clipboard as fallback
    navigator.clipboard.writeText(url).then(() => {
        showAlert('Video link copied to clipboard', 'success');
    }).catch(() => {
        // If clipboard API fails, show the URL in a prompt
        prompt('Copy this link to share:', url);
    });
}

async function getVideoInfo(videoId) {
    if (currentVideoData && String(currentVideoData.id) === String(videoId)) {
        return currentVideoData;
    }

    try {
        const response = await fetch(`/api/videos/${videoId}`, {
            credentials: 'include'
        });

        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error('Error getting video info:', error);
    }
    return null;
}

async function initializeVideoDetailPage() {
    const discoveredVideoId = ensureCurrentVideoId({ refreshDom: true });

    if (!discoveredVideoId) {
        console.warn('Video ID not found in URL or initial data.');
        showAlert('We could not determine which video to display. Please select a video again.', 'error');
        return;
    }

    currentVideoId = discoveredVideoId;

    const normalizedServerId = normalizeVideoId(serverProvidedVideo?.id);
    if (serverProvidedVideo && normalizedServerId === currentVideoId && !currentVideoData) {
        currentVideoData = serverProvidedVideo;
        applyVideoDetails(serverProvidedVideo);
    }

    updateCommentAuthVisibility();

    const commentInput = document.getElementById('commentInput');
    if (commentInput) {
        commentInput.addEventListener('input', updateCommentWordCount);
        updateCommentWordCount();
    }

    const commentSort = document.getElementById('commentSort');
    if (commentSort) {
        commentSort.addEventListener('change', () => renderComments());
    }

    attachRatingStarListeners();

    // Attach event listeners for video action buttons
    const likeBtn = document.getElementById('likeBtn');
    if (likeBtn) {
        likeBtn.addEventListener('click', () => {
            alert('Like button clicked');
            requireAuth(() => toggleLike(), 'like videos');
        });
    }

    const dislikeBtn = document.getElementById('dislikeBtn');
    if (dislikeBtn) {
        dislikeBtn.addEventListener('click', () => {
            alert('Dislike button clicked');
            requireAuth(() => toggleDislike(), 'dislike videos');
        });
    }

    const shareBtn = document.getElementById('shareBtn');
    if (shareBtn) {
        shareBtn.addEventListener('click', () => {
            alert('Share button clicked');
            shareVideo();
        });
    }

    const watchLaterBtn = document.getElementById('watchLaterBtn');
    if (watchLaterBtn) {
        watchLaterBtn.addEventListener('click', () => {
            alert('Watch later button clicked');
            requireAuth(() => addToWatchLater(currentVideoId), 'add to Watch Later');
        });
    }

    const playlistBtn = document.getElementById('playlistBtn');
    if (playlistBtn) {
        playlistBtn.addEventListener('click', () => {
            alert('Playlist button clicked');
            requireAuth(() => showAddToPlaylist(), 'manage playlists');
        });
    }

    const reportBtn = document.getElementById('reportBtn');
    if (reportBtn) {
        reportBtn.addEventListener('click', () => {
            alert('Report button clicked');
            requireAuth(() => goToReportPage(), 'report videos');
        });
    }

    const details = await loadVideoDetails(currentVideoId, {
        fallback: currentVideoData,
        suppressAlert: !!currentVideoData
    });
    loadComments(currentVideoId);
    loadRelatedVideos(currentVideoId);
    loadTrendingVideos(details?.categoryId ?? currentVideoData?.categoryId ?? null);
    loadLatestVideos();

    if (isAuthenticated) {
        loadUserPlaylists();
    }
}

function getVideoIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    if (params.has('id')) {
        return params.get('id');
    }

    const segments = window.location.pathname.split('/').filter(Boolean);
    const lastSegment = segments.pop();
    return lastSegment && /^\d+$/.test(lastSegment) ? lastSegment : null;
}

function updateCommentAuthVisibility() {
    const commentForm = document.getElementById('commentForm');
    const loginPrompt = document.getElementById('loginPrompt');
    const avatarImg = document.getElementById('userAvatar');

    const authenticated = !!isAuthenticated;

    // Don't override server-side rendering - form visibility is controlled by Spring Security
    // Just update the avatar if the form exists
    if (avatarImg && authenticated && currentUser) {
        const avatarSource = sanitizeAvatar(currentUser.profilePicture);
        avatarImg.src = avatarSource;
    }
}

async function loadVideoDetails(videoId = currentVideoId, options = {}) {
    const fallback = options?.fallback ?? null;
    const suppressAlert = options?.suppressAlert ?? false;

    if (!videoId) {
        return fallback;
    }

    try {
        const response = await fetch(`/api/videos/${videoId}`, {
            credentials: 'include'
        });

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || 'Failed to load video details');
        }

        const data = await response.json();
        currentVideoData = data;
        currentVideoId = data.id ? data.id.toString() : currentVideoId;
        applyVideoDetails(data);
        return data;
    } catch (error) {
        console.error('Error loading video details:', error);
        if (!suppressAlert) {
            showAlert('Unable to load video details right now.', 'error');
        }
        return fallback;
    }
}

function applyVideoDetails(data) {
    const player = document.getElementById('videoPlayer');
    const source = document.getElementById('videoSource');
    const videoUrl = data.videoUrl || data.filePath || '';
    if (player) {
        player.dataset.videoSrc = videoUrl;
        if (data.id) {
            player.dataset.videoId = data.id;
        }

        if (isAuthenticated && videoUrl) {
            if (source) {
                source.src = videoUrl;
            }
            player.src = videoUrl;
            player.load();
        } else {
            if (source) {
                source.removeAttribute('src');
            }
            player.removeAttribute('src');
            player.src = '';
            player.load();
        }

        player.poster = data.thumbnailUrl || data.thumbnailPath || '';
    }

    const titleEl = document.getElementById('videoTitle');
    if (titleEl) {
        titleEl.textContent = data.title || 'Untitled Video';
    }

    const viewsEl = document.getElementById('videoViews');
    if (viewsEl) {
        viewsEl.textContent = `${formatViewCount(data.viewCount ?? 0)} views`;
    }

    const dateEl = document.getElementById('videoDate');
    if (dateEl) {
        dateEl.textContent = data.createdAt ? formatDate(data.createdAt) : '';
    }

    const descriptionEl = document.getElementById('videoDescription');
    if (descriptionEl) {
        descriptionEl.textContent = data.description || 'No description provided.';
    }

    renderTags(parseTags(data.tags));

    const channelName = document.getElementById('channelName');
    if (channelName) {
        channelName.textContent = data.uploaderName || 'Video Creator';
    }

    const channelAvatar = document.getElementById('channelAvatar');
    if (channelAvatar) {
        channelAvatar.src = sanitizeAvatar(data.uploaderAvatar);
    }

    const subtitleEl = document.getElementById('channelSubtitle');
    if (subtitleEl) {
        const roleLabel = formatRoleLabel(data.uploaderRole);
        const joinLabel = formatJoinedDateLabel(data.uploaderJoinDate);
        const pieces = [roleLabel, joinLabel].filter(Boolean);
        subtitleEl.textContent = pieces.length > 0 ? pieces.join(' · ') : 'Sharing knowledge with the world';
    }

    updateSubscribeSection(data);

    updateVideoActionButtons({
        liked: !!data.likedByCurrentUser,
        disliked: !!data.dislikedByCurrentUser,
        likeCount: data.likeCount ?? 0,
        dislikeCount: data.dislikeCount ?? 0
    });

    if (data.title) {
        document.title = `${data.title} — VideoHub`;
    }

    updateRatingSummary(data);
    updateNowPlayingCard(data);
    configureVideoAccess(data);
}

function updateSubscribeSection(video = {}) {
    const btn = document.getElementById('subscribeBtn');
    const countEl = document.getElementById('channelSubscribers');
    const creatorId = video.uploadedById;

    if (countEl) {
        const count = Number(video.uploaderSubscriberCount ?? 0);
        countEl.textContent = formatSubscriberLabel(count);
    }

    if (!btn) {
        return;
    }

    if (!creatorId) {
        btn.style.display = 'none';
        return;
    }

    btn.dataset.creatorId = creatorId;

    const isSelf = currentUser && String(currentUser.id) === String(creatorId);
    if (isSelf) {
        btn.style.display = 'none';
        return;
    }

    btn.style.display = 'inline-flex';
    const isSubscribed = !!video.subscribedToUploader;
    btn.classList.toggle('subscribed', isSubscribed);
    btn.classList.remove('loading');
    btn.disabled = false;
    btn.setAttribute('aria-pressed', String(isSubscribed));
    btn.innerHTML = isSubscribed ? '✓ Subscribed' : '🔔 Subscribe';
}

function configureVideoAccess(videoData) {
    const player = document.getElementById('videoPlayer');
    const overlay = document.getElementById('videoOverlay');

    if (!player || !overlay) {
        return;
    }

    const isPrivate = (videoData?.privacy || '').toUpperCase() === 'PRIVATE';
    const requiresAuth = !isAuthenticated;
    const shouldBlockPlayback = requiresAuth || (isPrivate && !isAuthenticated);

    const source = document.getElementById('videoSource');

    if (shouldBlockPlayback) {
        overlay.classList.remove('hidden');
        overlay.style.display = 'flex';
        player.pause();
        player.controls = false;

        if (source) {
            source.removeAttribute('src');
        }
        player.removeAttribute('src');
        player.src = '';
        player.load();

        if (!privateVideoPlayHandler) {
            privateVideoPlayHandler = function(event) {
                if (event) {
                    event.preventDefault();
                }
                player.pause();
                showAlert('Please log in to watch this video.', 'warning');
                requireAuth(null, 'watch videos');
            };
            player.addEventListener('play', privateVideoPlayHandler);
        }
    } else {
        overlay.classList.add('hidden');
        overlay.style.display = 'none';
        player.controls = true;

        if (privateVideoPlayHandler) {
            player.removeEventListener('play', privateVideoPlayHandler);
            privateVideoPlayHandler = null;
        }

        const storedSrc = player.dataset.videoSrc;
        if (storedSrc) {
            if (source) {
                source.src = storedSrc;
            }
            if (player.src !== storedSrc) {
                player.src = storedSrc;
            }
            player.load();
        }
    }
}

function updateRatingSummary(video) {
    if (!video) {
        return;
    }

    const summaryEl = document.getElementById('ratingSummary');
    const averageEl = document.getElementById('ratingAverage');
    const countEl = document.getElementById('ratingCountLabel');
    const buttonLabelEl = document.getElementById('ratingButtonLabel');

    const average = typeof video.averageRating === 'number' ? video.averageRating : null;
    const ratingCount = Number(video.ratingCount || 0);
    const userRating = Number(video.userRating || 0);

    if (averageEl) {
        averageEl.textContent = ratingCount > 0 && Number.isFinite(average)
            ? average.toFixed(1)
            : '—';
    }

    if (countEl) {
        if (ratingCount <= 0) {
            countEl.textContent = 'No ratings yet';
        } else if (ratingCount === 1) {
            countEl.textContent = '1 rating';
        } else {
            countEl.textContent = `${ratingCount} ratings`;
        }
    }

    if (buttonLabelEl) {
        buttonLabelEl.textContent = userRating > 0
            ? `Your rating: ${userRating}★`
            : 'Rate this video';
    }

    if (summaryEl) {
        summaryEl.classList.remove('hidden');
    }

    pendingRatingScore = userRating || 0;
}

function updateNowPlayingCard(video) {
    const card = document.getElementById('nowPlayingCard');
    if (!card || !video) {
        return;
    }

    const thumbnailEl = document.getElementById('nowPlayingThumbnail');
    const titleEl = document.getElementById('nowPlayingTitle');
    const viewsEl = document.getElementById('nowPlayingViews');
    const dateEl = document.getElementById('nowPlayingDate');
    const categoryEl = document.getElementById('nowPlayingCategory');
    const ratingEl = document.getElementById('nowPlayingRating');

    const thumbnail = video.thumbnailUrl || video.thumbnailPath || buildMediaPath('thumbnails', video.thumbnail);
    if (thumbnailEl) {
        thumbnailEl.src = thumbnail || '/images/default-thumbnail.jpg';
        thumbnailEl.alt = `${video.title || 'Video thumbnail'}`;
    }

    if (titleEl) {
        titleEl.textContent = video.title || 'Untitled video';
    }

    if (viewsEl) {
        viewsEl.textContent = `${formatViewCount(Number(video.viewCount || 0))} views`;
    }

    const hasValidDate = video.createdAt && !Number.isNaN(Date.parse(video.createdAt));
    if (dateEl) {
        dateEl.textContent = hasValidDate ? formatDate(video.createdAt) : '—';
    }

    if (categoryEl) {
        categoryEl.textContent = video.categoryName || 'Uncategorized';
    }

    if (ratingEl) {
        const average = typeof video.averageRating === 'number' ? video.averageRating : null;
        const ratingCount = Number(video.ratingCount || 0);
        ratingEl.textContent = ratingCount > 0 && Number.isFinite(average)
            ? `${average.toFixed(1)}★ (${ratingCount})`
            : 'No ratings yet';
    }

    card.dataset.videoId = video.id || '';
}

function renderTags(tags) {
    const tagsContainer = document.getElementById('videoTags');
    if (!tagsContainer) {
        return;
    }

    tagsContainer.innerHTML = '';
    if (!tags || tags.length === 0) {
        return;
    }

    tags.forEach(tag => {
        const span = document.createElement('span');
        span.className = 'tag';
        span.textContent = tag;
        tagsContainer.appendChild(span);
    });
}

function parseTags(rawTags) {
    if (!rawTags) {
        return [];
    }

    try {
        const parsed = JSON.parse(rawTags);
        if (Array.isArray(parsed)) {
            return parsed;
        }
    } catch (error) {
        // Not JSON, fall back to comma separated list
    }

    if (typeof rawTags === 'string') {
        return rawTags.split(',').map(tag => tag.trim()).filter(Boolean);
    }

    return [];
}

function sanitizeAvatar(avatarPath) {
    if (!avatarPath) {
        return '/images/default-avatar.jpg';
    }
    if (avatarPath.startsWith('http') || avatarPath.startsWith('/')) {
        return avatarPath;
    }
    return `/uploads/avatars/${avatarPath}`;
}

function escapeHtml(text) {
    if (text == null) {
        return '';
    }
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

async function loadComments(videoId = currentVideoId) {
    const commentsList = document.getElementById('commentsList');
    const resolvedVideoId = normalizeVideoId(videoId) || ensureCurrentVideoId();
    if (!commentsList) {
        return;
    }

    if (!resolvedVideoId) {
        commentsList.innerHTML = '<p class="error">Comments will appear once the video finishes loading.</p>';
        return;
    }

    commentsList.innerHTML = '<div class="loading">Loading comments...</div>';

    try {
        const response = await fetch(`/api/videos/${resolvedVideoId}/comments`, {
            credentials: 'include'
        });

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || 'Failed to load comments');
        }

        const data = await response.json();
        currentVideoId = resolvedVideoId;
        currentComments = sanitizeComments(Array.isArray(data) ? data : []);
        renderComments();
    } catch (error) {
        console.error('Error loading comments:', error);
        commentsList.innerHTML = '<p class="error">Unable to load comments right now.</p>';
    }
}

function sanitizeComments(comments) {
    if (!Array.isArray(comments)) {
        return [];
    }

    return comments
        .map(sanitizeComment)
        .filter(Boolean);
}

function sanitizeComment(comment) {
    if (!comment || comment.isDisabled) {
        return null;
    }

    return {
        ...comment,
        likeCount: Number(comment.likeCount || 0),
        likedByCurrentUser: !!comment.likedByCurrentUser,
        replies: sanitizeComments(comment.replies || [])
    };
}

function renderComments(comments = currentComments) {
    console.log('[renderComments] Called with', comments?.length || 0, 'comments');
    const commentsList = document.getElementById('commentsList');
    const commentCountEl = document.getElementById('commentCount');

    if (!commentsList) {
        console.error('[renderComments] commentsList element not found!');
        return;
    }

    commentsList.innerHTML = '';

    if (!Array.isArray(comments) || comments.length === 0) {
        console.log('[renderComments] No comments to display');
        commentsList.innerHTML = '<p class="no-comments">No comments yet. Be the first to comment!</p>';
        if (commentCountEl) {
            commentCountEl.textContent = '0';
        }
        return;
    }

    const sortValue = document.getElementById('commentSort')?.value || 'newest';
    const sorted = sortCommentCollection(comments, sortValue);

    console.log('[renderComments] Rendering', sorted.length, 'comment threads');
    sorted.forEach(comment => {
        const thread = renderCommentThread(comment, sortValue);
        console.log('[renderComments] Appending thread for comment', comment.id);
        commentsList.appendChild(thread);
    });

    if (commentCountEl) {
        commentCountEl.textContent = String(getTotalCommentCount(comments));
    }
    console.log('[renderComments] Rendering complete');
}

function sortCommentCollection(comments, sortValue) {
    const sorted = [...comments];
    sorted.sort((a, b) => {
        const pinnedA = a.isPinned ? 1 : 0;
        const pinnedB = b.isPinned ? 1 : 0;
        if (pinnedB !== pinnedA) {
            return pinnedB - pinnedA;
        }

        const dateA = new Date(a.createdAt || 0);
        const dateB = new Date(b.createdAt || 0);

        switch (sortValue) {
            case 'oldest':
                return dateA - dateB;
            case 'popular': {
                const likesA = Number(a.likeCount || 0);
                const likesB = Number(b.likeCount || 0);
                if (likesB !== likesA) {
                    return likesB - likesA;
                }
                const repliesA = Array.isArray(a.replies) ? a.replies.length : 0;
                const repliesB = Array.isArray(b.replies) ? b.replies.length : 0;
                if (repliesB !== repliesA) {
                    return repliesB - repliesA;
                }
                return dateB - dateA;
            }
            case 'newest':
            default:
                return dateB - dateA;
        }
    });
    return sorted;
}

function renderCommentThread(comment, sortValue) {
    const thread = document.createElement('div');
    thread.className = 'comment-thread';
    thread.dataset.commentId = comment.id;
    thread.dataset.createdAt = comment.createdAt || new Date().toISOString();

    const commentElement = createCommentElement(comment);
    thread.appendChild(commentElement);

    const replies = Array.isArray(comment.replies) ? comment.replies : [];
    if (replies.length > 0) {
        const repliesContainer = document.createElement('div');
        repliesContainer.className = 'comment-replies';
        const replySortOrder = sortValue === 'popular' ? 'popular' : 'oldest';
        const sortedReplies = sortCommentCollection(replies, replySortOrder);
        sortedReplies.forEach(reply => {
            repliesContainer.appendChild(renderCommentThread(reply, sortValue));
        });
        thread.appendChild(repliesContainer);
    }

    return thread;
}

function createCommentElement(comment) {
    const element = document.createElement('div');
    element.className = 'comment';
    element.dataset.commentId = comment.id;
    element.dataset.createdAt = comment.createdAt || new Date().toISOString();
    
    // Check if current user is the comment author
    const isAuthor = currentUser && comment.userId && (currentUser.id === comment.userId);
    
    element.innerHTML = `
        <div class="comment-avatar">
            <img src="${sanitizeAvatar(comment.userProfilePicture)}" alt="${escapeHtml(comment.username || 'User')}">
        </div>
        <div class="comment-content">
            <div class="comment-header">
                <span class="comment-author">${escapeHtml(comment.username || 'User')}</span>
                <span class="comment-date">${comment.createdAt ? formatDate(comment.createdAt) : 'Just now'}</span>
                ${isAuthor ? `
                <div class="comment-actions">
                    <button class="comment-action-btn" type="button" data-comment-id="${comment.id}" data-action="edit" title="Edit comment">
                        ✏️
                    </button>
                    <button class="comment-action-btn" type="button" data-comment-id="${comment.id}" data-action="delete" title="Delete comment">
                        🗑️
                    </button>
                </div>
                ` : ''}
            </div>
            <div class="comment-body" data-comment-id="${comment.id}">${escapeHtml(comment.content)}</div>
            <div class="comment-footer">
                <button class="comment-like-btn${comment.likedByCurrentUser ? ' liked' : ''}" type="button" data-comment-id="${comment.id}" aria-pressed="${comment.likedByCurrentUser ? 'true' : 'false'}">
                    <span class="comment-like-icon">${comment.likedByCurrentUser ? '❤️' : '♡'}</span>
                    <span class="comment-like-text">Like</span>
                    <span class="comment-like-count">${Number(comment.likeCount || 0)}</span>
                </button>
                <span class="comment-footer-separator">•</span>
                <button class="comment-reply-btn" type="button" data-comment-id="${comment.id}">Reply</button>
            </div>
        </div>
    `;

    const likeBtn = element.querySelector('.comment-like-btn');
    if (likeBtn) {
        likeBtn.addEventListener('click', event => {
            event.preventDefault();
            requireAuth(() => toggleCommentLike(comment.id), 'like comments');
        });
    }

    const replyBtn = element.querySelector('.comment-reply-btn');
    if (replyBtn) {
        replyBtn.addEventListener('click', event => {
            event.preventDefault();
            requireAuth(() => toggleReplyForm(comment.id), 'replying to comments');
        });
    }
    
    // Add event listeners for edit and delete buttons
    const editBtn = element.querySelector('[data-action="edit"]');
    if (editBtn) {
        editBtn.addEventListener('click', event => {
            event.preventDefault();
            editComment(comment.id);
        });
    }
    
    const deleteBtn = element.querySelector('[data-action="delete"]');
    if (deleteBtn) {
        deleteBtn.addEventListener('click', event => {
            event.preventDefault();
            deleteComment(comment.id);
        });
    }

    return element;
}

function toggleReplyForm(commentId) {
    const thread = document.querySelector(`.comment-thread[data-comment-id="${commentId}"]`);
    if (!thread) {
        return;
    }

    const existingForm = thread.querySelector('.reply-form');
    if (existingForm) {
        existingForm.remove();
        return;
    }

    const form = document.createElement('form');
    form.className = 'reply-form';
    form.innerHTML = `
        <textarea rows="3" maxlength="500" placeholder="Write a reply..."></textarea>
        <div class="reply-actions">
            <button type="button" class="btn btn-secondary">Cancel</button>
            <button type="submit" class="btn">Reply</button>
        </div>
    `;

    const textarea = form.querySelector('textarea');
    const cancelBtn = form.querySelector('.btn-secondary');
    if (textarea) {
    textarea.dataset.wordLimit = '150';
    textarea.addEventListener('input', () => enforceWordLimit(textarea, 150));
    }

    cancelBtn?.addEventListener('click', () => form.remove());
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (!requireAuth(null, 'reply to comments')) {
            return;
        }

        const content = textarea.value.trim();
        if (!content) {
            showAlert('Reply cannot be empty.', 'warning');
            return;
        }

    const limit = Number(textarea.dataset.wordLimit) || 150;
        const wordCount = countWords(content);
        if (wordCount > limit) {
            showAlert(`Replies are limited to ${limit} words.`, 'warning');
            enforceWordLimit(textarea, limit);
            return;
        }

        await submitReply(commentId, content, form);
    });

    const repliesContainer = thread.querySelector('.comment-replies');
    if (repliesContainer) {
        repliesContainer.prepend(form);
    } else {
        const newContainer = document.createElement('div');
        newContainer.className = 'comment-replies';
        newContainer.appendChild(form);
        thread.appendChild(newContainer);
    }

    textarea.focus();
}

async function submitReply(parentCommentId, content, form) {
    let shouldRemoveForm = true;
    try {
        const response = await fetch(`/api/videos/${currentVideoId}/comments`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ content, parentCommentId })
        });

        if (response.status === 401) {
            requireAuth(null, 'reply to comments');
            shouldRemoveForm = false;
            return;
        }

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || 'Failed to post reply');
        }

        const reply = sanitizeComment(await response.json());
        if (!reply) {
            throw new Error('Failed to post reply');
        }

        if (!insertComment(currentComments, reply)) {
            await loadComments(currentVideoId);
        } else {
            renderComments();
        }

        showAlert('Reply posted successfully!', 'success');
    } catch (error) {
        console.error('Error submitting reply:', error);
        showAlert(error.message || 'Failed to post reply', 'error');
    } finally {
        if (shouldRemoveForm) {
            form?.remove();
        }
    }
}

// Edit Comment Function
function editComment(commentId) {
    console.log('=== EDIT COMMENT CALLED ===');
    console.log('Comment ID:', commentId);
    
    const commentElement = document.querySelector(`.comment[data-comment-id="${commentId}"]`);
    if (!commentElement) {
        console.error('Comment element not found for ID:', commentId);
        return;
    }
    
    const commentBodyEl = commentElement.querySelector('.comment-body');
    if (!commentBodyEl) {
        console.error('Comment body element not found');
        return;
    }
    
    // Get current comment text
    const currentText = commentBodyEl.textContent;
    console.log('Current text:', currentText);
    
    // Check if already editing
    if (commentElement.querySelector('.edit-comment-form')) {
        console.log('Already editing this comment');
        return;
    }
    
    // Hide the comment body
    commentBodyEl.style.display = 'none';
    
    // Create edit form
    const editForm = document.createElement('form');
    editForm.className = 'edit-comment-form';
    editForm.innerHTML = `
        <textarea rows="3" maxlength="500" class="edit-comment-textarea">${currentText}</textarea>
        <div class="edit-comment-actions">
            <button type="button" class="btn btn-secondary cancel-edit-btn">Cancel</button>
            <button type="submit" class="btn btn-primary save-edit-btn">Save</button>
        </div>
    `;
    
    const textarea = editForm.querySelector('textarea');
    textarea.dataset.wordLimit = '150';
    textarea.addEventListener('input', () => enforceWordLimit(textarea, 150));
    
    // Insert after comment body
    commentBodyEl.after(editForm);
    textarea.focus();
    textarea.setSelectionRange(textarea.value.length, textarea.value.length);
    
    // Cancel button
    const cancelBtn = editForm.querySelector('.cancel-edit-btn');
    cancelBtn.addEventListener('click', () => {
        editForm.remove();
        commentBodyEl.style.display = '';
    });
    
    // Submit edit
    editForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        
        const newContent = textarea.value.trim();
        if (!newContent) {
            showAlert('Comment cannot be empty.', 'warning');
            return;
        }
        
        const limit = Number(textarea.dataset.wordLimit) || 150;
        const wordCount = countWords(newContent);
        if (wordCount > limit) {
            showAlert(`Comments are limited to ${limit} words.`, 'warning');
            enforceWordLimit(textarea, limit);
            return;
        }
        
        if (newContent === currentText) {
            editForm.remove();
            commentBodyEl.style.display = '';
            return;
        }
        
        try {
            console.log('=== SUBMITTING EDIT ===');
            console.log('Comment ID:', commentId);
            console.log('New content:', newContent);
            console.log('URL:', `/api/comments/${commentId}`);
            
            const response = await fetch(`/api/comments/${commentId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ content: newContent })
            });
            
            console.log('Response status:', response.status);
            console.log('Response ok:', response.ok);
            
            if (response.status === 401) {
                requireAuth(null, 'edit comments');
                return;
            }
            
            if (!response.ok) {
                const message = await response.text();
                console.error('Error response:', message);
                throw new Error(message || 'Failed to edit comment');
            }
            
            const result = await response.json();
            console.log('Update successful:', result);
            
            // Update the comment body
            commentBodyEl.textContent = newContent;
            commentBodyEl.style.display = '';
            editForm.remove();
            
            showAlert('Comment updated successfully!', 'success');
            
            // Refresh comments to sync with server
            await loadComments(currentVideoId);
        } catch (error) {
            console.error('Error editing comment:', error);
            showAlert(error.message || 'Failed to edit comment', 'error');
        }
    });
}

// Delete Comment Function
async function deleteComment(commentId) {
    if (!confirm('Are you sure you want to delete this comment? This action cannot be undone.')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/comments/${commentId}`, {
            method: 'DELETE',
            credentials: 'include'
        });
        
        if (response.status === 401) {
            requireAuth(null, 'delete comments');
            return;
        }
        
        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || 'Failed to delete comment');
        }
        
        showAlert('Comment deleted successfully!', 'success');
        
        // Reload comments
        await loadComments(currentVideoId);
    } catch (error) {
        console.error('Error deleting comment:', error);
        showAlert(error.message || 'Failed to delete comment', 'error');
    }
}

function insertComment(comments, comment) {
    console.log('[insertComment] Inserting comment:', comment?.id, 'parentId:', comment?.parentCommentId);
    console.log('[insertComment] Current comments array length:', comments?.length || 0);
    
    if (!Array.isArray(comments) || !comment) {
        console.error('[insertComment] Invalid parameters');
        return false;
    }

    if (!comment.parentCommentId) {
        comment.replies = Array.isArray(comment.replies) ? comment.replies : [];
        comments.unshift(comment);
        console.log('[insertComment] Added top-level comment. New length:', comments.length);
        return true;
    }

    for (const node of comments) {
        if (String(node.id) === String(comment.parentCommentId)) {
            if (!Array.isArray(node.replies)) {
                node.replies = [];
            }
            comment.replies = Array.isArray(comment.replies) ? comment.replies : [];
            node.replies.push(comment);
            console.log('[insertComment] Added reply to comment', node.id);
            return true;
        }

        if (Array.isArray(node.replies) && insertComment(node.replies, comment)) {
            return true;
        }
    }

    console.warn('[insertComment] Parent comment not found');
    return false;
}

function findCommentById(comments, commentId) {
    if (!Array.isArray(comments) || commentId == null) {
        return null;
    }

    for (const comment of comments) {
        if (!comment) {
            continue;
        }
        if (String(comment.id) === String(commentId)) {
            return comment;
        }
        const nested = findCommentById(comment.replies, commentId);
        if (nested) {
            return nested;
        }
    }
    return null;
}

function updateCommentLikeState(commentId, likeCount, liked) {
    const comment = findCommentById(currentComments, commentId);
    if (comment) {
        comment.likeCount = Number.isFinite(likeCount) ? likeCount : 0;
        comment.likedByCurrentUser = !!liked;
    }
}

function updateCommentLikeButton(commentId, likeCount, liked) {
    const buttons = document.querySelectorAll(`.comment-like-btn[data-comment-id="${commentId}"]`);
    buttons.forEach(btn => {
        btn.classList.toggle('liked', !!liked);
        btn.setAttribute('aria-pressed', liked ? 'true' : 'false');
        if (btn.classList.contains('loading')) {
            btn.classList.remove('loading');
        }
        const iconEl = btn.querySelector('.comment-like-icon');
        if (iconEl) {
            iconEl.textContent = liked ? '❤️' : '♡';
        }
        const countEl = btn.querySelector('.comment-like-count');
        if (countEl) {
            countEl.textContent = Number.isFinite(likeCount) ? likeCount : 0;
        }
    });
}

async function toggleCommentLike(commentId) {
    if (!requireAuth(null, 'like comments')) {
        return;
    }

    const videoId = ensureCurrentVideoId();
    if (!videoId) {
        showAlert('Video is still loading. Try again shortly.', 'warning');
        return;
    }

    const buttons = Array.from(document.querySelectorAll(`.comment-like-btn[data-comment-id="${commentId}"]`));
    buttons.forEach(btn => btn.classList.add('loading'));

    try {
        const response = await fetch(`/api/videos/${videoId}/comments/${commentId}/like`, {
            method: 'POST',
            credentials: 'include'
        });

        if (response.status === 401) {
            requireAuth(null, 'like comments');
            buttons.forEach(btn => btn.classList.remove('loading'));
            return;
        }

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || 'Failed to update comment like.');
        }

        const result = await response.json();
        const likeCount = Number(result.likeCount || 0);
        const liked = !!result.liked;
        updateCommentLikeState(commentId, likeCount, liked);
        updateCommentLikeButton(commentId, likeCount, liked);
    } catch (error) {
        console.error('Error toggling comment like:', error);
        showAlert(error.message || 'Failed to update comment like.', 'error');
        buttons.forEach(btn => btn.classList.remove('loading'));
    }
}

function getTotalCommentCount(comments) {
    if (!Array.isArray(comments)) {
        return 0;
    }

    return comments.reduce((total, comment) => {
        const replies = Array.isArray(comment.replies) ? comment.replies : [];
        return total + 1 + getTotalCommentCount(replies);
    }, 0);
}

function extractWords(text) {
    if (!text) {
        return [];
    }
    return text.trim().split(/\s+/).filter(Boolean);
}

function countWords(text) {
    return extractWords(text).length;
}

function enforceWordLimit(textarea, limit = 150) {
    if (!textarea) {
        return 0;
    }

    const words = extractWords(textarea.value);
    if (words.length > limit) {
        textarea.value = words.slice(0, limit).join(' ');
        if (!commentWordLimitWarned) {
            showAlert(`Comments are limited to ${limit} words.`, 'warning');
            commentWordLimitWarned = true;
        }
        return limit;
    }

    if (commentWordLimitWarned && words.length < limit) {
        commentWordLimitWarned = false;
    }

    return words.length;
}

function updateCommentWordCount() {
    const input = document.getElementById('commentInput');
    const counter = document.getElementById('commentWordCount');
    if (!input || !counter) {
        return;
    }

    const limit = Number(input.dataset.wordLimit) || 150;
    const wordsUsed = enforceWordLimit(input, limit);
    counter.textContent = wordsUsed;
}

async function submitComment() {
    if (!requireAuth(null, 'comment on videos')) {
        return;
    }

    const input = document.getElementById('commentInput');
    const videoId = ensureCurrentVideoId();
    if (!input) {
        return;
    }

    if (!videoId) {
        showAlert('Video is still loading. Please try again in a moment.', 'warning');
        return;
    }

    const content = input.value.trim();
    if (!content) {
        showAlert('Comment cannot be empty.', 'warning');
        return;
    }

    const limit = Number(input.dataset.wordLimit) || 150;
    const wordCount = countWords(content);
    if (wordCount > limit) {
        showAlert(`Comments are limited to ${limit} words.`, 'warning');
        enforceWordLimit(input, limit);
        updateCommentWordCount();
        return;
    }

    try {
        const response = await fetch(`/api/videos/${videoId}/comments`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ content })
        });

        if (response.status === 401) {
            requireAuth(null, 'comment on videos');
            return;
        }

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || 'Failed to post comment');
        }

        const newComment = sanitizeComment(await response.json());
        input.value = '';
        updateCommentWordCount();
        currentVideoId = videoId;
        if (!newComment) {
            await loadComments(videoId);
        } else {
            console.log('[submitComment] New comment received:', newComment);
            insertComment(currentComments, newComment);
            console.log('[submitComment] currentComments after insert:', currentComments.length);
            renderComments();
            showAlert('Comment posted successfully!', 'success');
            // Refresh from server to sync metadata and counts after a small delay
            setTimeout(() => loadComments(videoId), 500);
        }
    } catch (error) {
        console.error('Error submitting comment:', error);
        showAlert(error.message || 'Failed to post comment', 'error');
    }
}

function cancelComment() {
    const input = document.getElementById('commentInput');
    if (input) {
        input.value = '';
        updateCommentWordCount();
    }
}

function sortComments() {
    renderComments();
}

function updateRatingButtons(videoId, ratingState) {
    if (!ratingState) {
        return;
    }

    updateLikeButton(videoId, !!ratingState.liked, ratingState.likeCount ?? 0);

    const card = document.querySelector(`[data-video-id="${videoId}"]`);
    if (card) {
        const dislikeBtn = card.querySelector('.dislike-btn');
        const dislikeCountSpan = card.querySelector('.dislike-count');
        if (dislikeBtn) {
            dislikeBtn.classList.toggle('disliked', !!ratingState.disliked);
        }
        if (dislikeCountSpan && typeof ratingState.dislikeCount !== 'undefined') {
            dislikeCountSpan.textContent = ratingState.dislikeCount;
        }
    }

    if (currentVideoId && String(currentVideoId) === String(videoId)) {
        updateVideoActionButtons(ratingState);
    }
}

function updateVideoActionButtons(ratingState) {
    const likeBtn = document.getElementById('likeBtn');
    const dislikeBtn = document.getElementById('dislikeBtn');
    const likeCountEl = document.getElementById('likeCount');
    const dislikeCountEl = document.getElementById('dislikeCount');

    if (likeBtn) {
        likeBtn.classList.toggle('liked', !!ratingState.liked);
    }
    if (dislikeBtn) {
        dislikeBtn.classList.toggle('disliked', !!ratingState.disliked);
    }
    if (likeCountEl && typeof ratingState.likeCount !== 'undefined') {
        likeCountEl.textContent = ratingState.likeCount;
    }
    if (dislikeCountEl && typeof ratingState.dislikeCount !== 'undefined') {
        dislikeCountEl.textContent = ratingState.dislikeCount;
    }
}

function attachRatingStarListeners() {
    const stars = document.querySelectorAll('#ratingModal .rating-star');
    if (!stars.length) {
        return;
    }

    stars.forEach(star => {
        if (star.dataset.listenerAttached === 'true') {
            return;
        }

        star.addEventListener('mouseenter', () => {
            const score = Number(star.dataset.score || 0);
            highlightRatingStars(score);
        });

        star.addEventListener('mouseleave', () => {
            const fallback = pendingRatingScore || Number(currentVideoData?.userRating || 0);
            highlightRatingStars(fallback);
        });

        star.addEventListener('click', () => {
            const score = Number(star.dataset.score || 0);
            selectRatingStar(score);
        });

        star.dataset.listenerAttached = 'true';
    });
}

function highlightRatingStars(score) {
    const container = document.querySelector('#ratingModal .rating-modal-stars');
    if (!container) {
        return;
    }

    const normalizedScore = Number(score || 0);
    container.dataset.activeScore = normalizedScore > 0 ? String(normalizedScore) : '0';

    const stars = container.querySelectorAll('.rating-star');
    stars.forEach(star => {
        const starScore = Number(star.dataset.score || 0);
        star.classList.toggle('active', starScore <= normalizedScore && normalizedScore > 0);
    });
}

function selectRatingStar(score) {
    pendingRatingScore = score;
    highlightRatingStars(score);
}

function openRatingModal() {
    if (!requireAuth(null, 'rate this video')) {
        return;
    }

    if (!currentVideoId) {
        showAlert('Video is still loading. Try again in a moment.', 'warning');
        return;
    }

    const modal = document.getElementById('ratingModal');
    if (!modal) {
        return;
    }

    attachRatingStarListeners();

    pendingRatingScore = Number(currentVideoData?.userRating || 0);
    highlightRatingStars(pendingRatingScore);

    const subtitle = modal.querySelector('.rating-modal-subtitle');
    if (subtitle) {
        subtitle.textContent = pendingRatingScore
            ? `You previously rated this video ${pendingRatingScore} star${pendingRatingScore === 1 ? '' : 's'}.`
            : 'Select how helpful this video was for you.';
    }

    modal.style.display = 'flex';
}

async function submitVideoRating() {
    if (!requireAuth(null, 'rate this video')) {
        return;
    }

    if (!currentVideoId) {
        showAlert('Video is still loading. Try again shortly.', 'warning');
        return;
    }

    if (!pendingRatingScore || pendingRatingScore < 1 || pendingRatingScore > 5) {
        showAlert('Please choose a rating between 1 and 5 stars.', 'warning');
        return;
    }

    try {
        const response = await fetch(`/api/videos/${currentVideoId}/rating`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ score: pendingRatingScore })
        });

        if (response.status === 401) {
            requireAuth(null, 'rate this video');
            return;
        }

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || 'Failed to submit rating');
        }

        const summary = await response.json();
        if (!currentVideoData) {
            currentVideoData = {};
        }

        currentVideoData.averageRating = summary.averageRating;
        currentVideoData.ratingCount = summary.ratingCount;
        currentVideoData.userRating = summary.userRating;

        updateRatingSummary(currentVideoData);
        updateNowPlayingCard(currentVideoData);
    pendingRatingScore = Number(summary.userRating || 0);
    highlightRatingStars(pendingRatingScore);

        closeModal('ratingModal');
        showAlert('Thanks for rating this video!', 'success');
    } catch (error) {
        console.error('Error submitting rating:', error);
        showAlert(error.message || 'Failed to submit rating', 'error');
    }
}

async function loadRelatedVideos(videoId = currentVideoId) {
    const container = document.getElementById('relatedVideos');
    if (!container || !videoId) {
        return;
    }

    container.innerHTML = '<div class="loading">Loading related videos...</div>';

    try {
        const response = await fetch(`/api/videos/${videoId}/related`, {
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error('Failed to fetch related videos');
        }

        const videos = await response.json();
        renderRelatedVideos(container, Array.isArray(videos) ? videos : []);
    } catch (error) {
        console.error('Error loading related videos:', error);
        container.innerHTML = '<p class="error">Unable to load related videos.</p>';
    }
}

function renderRelatedVideos(container, videos) {
    if (!videos || videos.length === 0) {
        container.innerHTML = '<p class="empty">No related videos available right now.</p>';
        return;
    }

    container.innerHTML = '';
    videos.forEach(video => {
        const thumbnail = video.thumbnailUrl || video.thumbnailPath || buildMediaPath('thumbnails', video.thumbnail);
    const item = document.createElement('a');
    item.href = `/video-show?id=${video.id}`;
        item.className = 'related-video';
        item.dataset.videoId = video.id;

        const tags = parseTags(video.tags).slice(0, 2).map(tag => `#${escapeHtml(tag)}`).join(' ');
        item.innerHTML = `
            <img src="${thumbnail}" alt="${escapeHtml(video.title || 'Video thumbnail')}" class="related-video-thumbnail" onerror="this.src='/images/default-thumbnail.jpg'">
            <div class="related-video-info">
                <h4 class="related-video-title">${escapeHtml(video.title || 'Untitled video')}</h4>
                <div class="related-video-meta">
                    <span class="related-video-views">${formatViewCount(video.viewCount ?? 0)} views</span>
                    <span class="related-video-date">${video.createdAt ? formatDate(video.createdAt) : ''}</span>
                    ${tags ? `<span class="related-video-tags">${tags}</span>` : ''}
                </div>
            </div>
        `;
        container.appendChild(item);
    });
}

async function loadTrendingVideos(categoryId = null) {
    const container = document.getElementById('trendingVideos');
    if (!container) {
        return;
    }

    container.innerHTML = '<div class="loading">Loading trending videos...</div>';

    try {
        const endpoint = categoryId ? `/api/videos/trending-by-category/${categoryId}` : '/api/videos/top-rated';
        const response = await fetch(endpoint, {
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error('Failed to fetch trending videos');
        }

        const videos = await response.json();
        renderTrendingVideos(container, Array.isArray(videos) ? videos : []);
    } catch (error) {
        console.error('Error loading trending videos:', error);
        container.innerHTML = '<p class="error">Unable to load trending videos.</p>';
    }
}

function renderTrendingVideos(container, videos) {
    if (!videos || videos.length === 0) {
        container.innerHTML = '<p class="empty">No trending videos at the moment.</p>';
        return;
    }

    container.innerHTML = '';
    videos.forEach(video => {
        const thumbnail = video.thumbnailUrl || video.thumbnailPath || buildMediaPath('thumbnails', video.thumbnail);
    const item = document.createElement('a');
    item.href = `/video-show?id=${video.id}`;
        item.className = 'trending-video';
        item.dataset.videoId = video.id;
        item.innerHTML = `
            <img src="${thumbnail}" alt="${escapeHtml(video.title || 'Video thumbnail')}" class="trending-video-thumbnail" onerror="this.src='/images/default-thumbnail.jpg'">
            <div class="trending-video-info">
                <h4 class="trending-video-title">${escapeHtml(video.title || 'Untitled video')}</h4>
                <div class="trending-video-meta">
                    <span class="trending-video-views">${formatViewCount(video.viewCount ?? 0)} views</span>
                    <span class="trending-video-date">${video.createdAt ? formatDate(video.createdAt) : ''}</span>
                </div>
            </div>
        `;
        container.appendChild(item);
    });
}

async function loadLatestVideos() {
    const container = document.getElementById('latestVideos');
    if (!container) {
        return;
    }

    container.innerHTML = '<div class="loading">Loading latest videos...</div>';

    try {
        const response = await fetch('/api/videos/latest', {
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error('Failed to fetch latest videos');
        }

        const videos = await response.json();
        renderLatestVideos(container, Array.isArray(videos) ? videos : []);
    } catch (error) {
        console.error('Error loading latest videos:', error);
        container.innerHTML = '<p class="error">Unable to load latest videos.</p>';
    }
}

function renderLatestVideos(container, videos) {
    if (!videos || videos.length === 0) {
        container.innerHTML = '<p class="empty">No recent uploads just yet.</p>';
        return;
    }

    container.innerHTML = '';
    videos.slice(0, 6).forEach(video => {
        const thumbnail = video.thumbnailUrl || video.thumbnailPath || buildMediaPath('thumbnails', video.thumbnail);
        const hasValidDate = video.createdAt && !Number.isNaN(Date.parse(video.createdAt));
        const dateLabel = hasValidDate ? formatDate(video.createdAt) : 'Just now';
    const item = document.createElement('a');
    item.href = `/video-show?id=${video.id}`;
        item.className = 'related-video latest-video';
        item.dataset.videoId = video.id;
        item.innerHTML = `
            <img src="${thumbnail}" alt="${escapeHtml(video.title || 'Video thumbnail')}" class="related-video-thumbnail" onerror="this.src='/images/default-thumbnail.jpg'">
            <div class="related-video-info">
                <h4 class="latest-video-title">${escapeHtml(video.title || 'Untitled video')}</h4>
                <div class="latest-video-meta">
                    <span>${dateLabel}</span>
                    <span>•</span>
                    <span>${formatViewCount(video.viewCount ?? 0)} views</span>
                </div>
            </div>
        `;
        container.appendChild(item);
    });
}

function buildMediaPath(subFolder, fileName) {
    if (!fileName) {
        return '/images/default-thumbnail.jpg';
    }
    if (fileName.startsWith('http') || fileName.startsWith('/')) {
        return fileName;
    }
    return `/uploads/${subFolder}/${fileName}`;
}

async function shareVideo(videoId = currentVideoId) {
    alert('shareVideo called with videoId: ' + videoId);
    const targetVideoId = videoId || currentVideoId;
    if (!targetVideoId) {
        alert('Unable to determine which video to share.');
        showAlert('Unable to determine which video to share.', 'error');
        return;
    }

    let video = currentVideoData;
    if (!video || String(video.id) !== String(targetVideoId)) {
        video = await getVideoInfo(targetVideoId);
    }

    const shareUrl = `${window.location.origin}/video-show?id=${targetVideoId}`;
    const title = video?.title || 'Check out this video on VideoHub';
    const description = video?.description || 'Watch this video on VideoHub';

    if (navigator.share) {
        try {
            await navigator.share({
                title,
                text: description,
                url: shareUrl
            });
            return;
        } catch (error) {
            if (error.name !== 'AbortError') {
                console.warn('Native share failed, falling back to modal.', error);
            }
        }
    }

    const shareModal = document.getElementById('shareModal');
    if (shareModal) {
        const urlInput = document.getElementById('videoUrl');
        const embedInput = document.getElementById('embedCode');
        if (urlInput) {
            urlInput.value = shareUrl;
        }
        if (embedInput) {
            embedInput.value = `<iframe src="${shareUrl}" frameborder="0" allowfullscreen></iframe>`;
        }
        shareModal.style.display = 'flex';
    } else {
        fallbackShare(shareUrl, title);
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'none';
    }
}

function copyLink(inputId) {
    const input = document.getElementById(inputId);
    if (!input) {
        return;
    }

    navigator.clipboard.writeText(input.value).then(() => {
        showAlert('Link copied to clipboard!', 'success');
    }).catch(() => {
        showAlert('Unable to copy link automatically. Please copy it manually.', 'warning');
        input.select();
    });
}

function shareOnFacebook() {
    const shareUrl = `${window.location.origin}/video-show?id=${currentVideoId}`;
    window.open(`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(shareUrl)}`, '_blank');
}

function shareOnTwitter() {
    const shareUrl = `${window.location.origin}/video-show?id=${currentVideoId}`;
    window.open(`https://twitter.com/intent/tweet?url=${encodeURIComponent(shareUrl)}`, '_blank');
}

function shareOnWhatsApp() {
    const shareUrl = `${window.location.origin}/video-show?id=${currentVideoId}`;
    window.open(`https://wa.me/?text=${encodeURIComponent(shareUrl)}`, '_blank');
}

function shareViaEmail() {
    const shareUrl = `${window.location.origin}/video-show?id=${currentVideoId}`;
    window.location.href = `mailto:?subject=Check out this video&body=${encodeURIComponent(shareUrl)}`;
}

async function showAddToPlaylist() {
    if (!requireAuth(null, 'manage playlists')) {
        return;
    }

    await loadUserPlaylists();

    const modal = document.getElementById('addToPlaylistModal');
    if (modal) {
        modal.style.display = 'flex';
    }
}

async function loadUserPlaylists(force = false) {
    if (!isAuthenticated) {
        userPlaylists = [];
        playlistsLoaded = false;
        populatePlaylistSelect([]);
        return [];
    }

    if (force) {
        playlistsLoaded = false;
    }

    if (playlistsLoaded && userPlaylists.length > 0) {
        populatePlaylistSelect(userPlaylists);
        return userPlaylists;
    }

    try {
        const response = await fetch('/api/playlists/my', {
            credentials: 'include'
        });

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || 'Failed to load playlists');
        }

        userPlaylists = await response.json();
        playlistsLoaded = true;
        populatePlaylistSelect(userPlaylists);
        return userPlaylists;
    } catch (error) {
        console.error('Error loading playlists:', error);
        showAlert(error.message || 'Failed to load playlists', 'error');
        userPlaylists = [];
        playlistsLoaded = false;
        populatePlaylistSelect([]);
        return [];
    }
}

function populatePlaylistSelect(playlists) {
    const select = document.getElementById('playlistSelect');
    if (!select) {
        return;
    }

    const previousValue = select.value;
    select.innerHTML = '<option value="">-- Select a playlist --</option>';

    playlists.forEach(playlist => {
        if (!playlist || typeof playlist.id === 'undefined') {
            return;
        }
        const option = document.createElement('option');
        option.value = playlist.id;
        const count = typeof playlist.videoCount === 'number'
            ? playlist.videoCount
            : (Array.isArray(playlist.videos) ? playlist.videos.length : 0);
        option.textContent = count ? `${playlist.name} (${count})` : playlist.name;
        select.appendChild(option);
    });

    if (previousValue) {
        select.value = previousValue;
    }
}

async function addToPlaylist() {
    if (!requireAuth(null, 'add videos to playlists')) {
        return;
    }

    if (!currentVideoId) {
        showAlert('Video is still loading. Please try again shortly.', 'warning');
        return;
    }

    const select = document.getElementById('playlistSelect');
    const newPlaylistInput = document.getElementById('newPlaylistName');

    let playlistId = select?.value?.trim();
    const newPlaylistName = newPlaylistInput?.value.trim();

    try {
        if (newPlaylistName) {
            const createResponse = await fetch('/api/playlists', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ name: newPlaylistName })
            });

            if (!createResponse.ok) {
                const message = await createResponse.text();
                throw new Error(message || 'Failed to create playlist');
            }

            const created = await createResponse.json();
            playlistId = String(created.id);
            if (newPlaylistInput) {
                newPlaylistInput.value = '';
            }
            userPlaylists = [created, ...userPlaylists];
            playlistsLoaded = true;
            populatePlaylistSelect(userPlaylists);
            if (select) {
                select.value = playlistId;
            }
        }

        if (!playlistId) {
            showAlert('Please select a playlist or create a new one.', 'warning');
            return;
        }

        const addResponse = await fetch(`/api/playlists/${playlistId}/videos`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ videoId: Number(currentVideoId) })
        });

        if (addResponse.status === 401) {
            requireAuth(null, 'add videos to playlists');
            return;
        }

        if (!addResponse.ok) {
            const message = await addResponse.text();
            throw new Error(message || 'Failed to add video to playlist');
        }

        showAlert('Video added to playlist!', 'success');
        document.getElementById('addToPlaylistForm')?.reset();
        await loadUserPlaylists(true);
        closeModal('addToPlaylistModal');
    } catch (error) {
        console.error('Error adding to playlist:', error);
        showAlert(error.message || 'Failed to add video to playlist', 'error');
    }
}

async function toggleSubscribe() {
    const btn = document.getElementById('subscribeBtn');
    if (!btn) {
        return;
    }

    const creatorId = btn.dataset.creatorId;
    if (!creatorId) {
        showAlert('Creator information is missing for this video.', 'error');
        return;
    }

    if (!requireAuth(null, 'subscribe to creators')) {
        return;
    }

    const isSubscribed = btn.classList.contains('subscribed');
    const method = isSubscribed ? 'DELETE' : 'POST';

    btn.disabled = true;
    btn.classList.add('loading');

    try {
        const response = await fetch(`/api/subscriptions/${creatorId}`, {
            method,
            credentials: 'include'
        });

        if (!response.ok) {
            if (response.status === 401) {
                requireAuth(null, 'subscribe to creators');
                return;
            }
            const message = await response.text();
            throw new Error(message || 'Unable to update subscription right now.');
        }

        const result = await response.json();
        const subscribed = !!result.subscribed;
        const subscriberCount = Number(result.subscriberCount || 0);

        if (currentVideoData && String(currentVideoData.uploadedById) === String(creatorId)) {
            currentVideoData.subscribedToUploader = subscribed;
            currentVideoData.uploaderSubscriberCount = subscriberCount;
        }

        if (currentUser && typeof result.subscriptionCount === 'number') {
            currentUser.subscriptionCount = result.subscriptionCount;
        }

        updateSubscribeSection({
            uploadedById: Number(creatorId),
            subscribedToUploader: subscribed,
            uploaderSubscriberCount: subscriberCount
        });

        showAlert(subscribed ? 'Subscribed to creator.' : 'Unsubscribed from creator.', 'success');
    } catch (error) {
        console.error('Error updating subscription:', error);
        showAlert(error.message || 'Failed to update subscription status.', 'error');
    } finally {
        btn.classList.remove('loading');
        btn.disabled = false;
    }
}

async function reportVideoSubmit() {
    if (!currentVideoId) {
        showAlert('No video selected to report.', 'error');
        return;
    }

    if (!requireAuth(null, 'report videos')) {
        return;
    }

    const reason = document.getElementById('reportReason')?.value;
    const additionalInfo = document.getElementById('additionalInfo')?.value?.trim();

    if (!reason) {
        showAlert('Please select a reason for reporting.', 'warning');
        return;
    }

    try {
        const response = await fetch('/api/reports', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                videoId: Number(currentVideoId),
                reportType: reason,
                description: additionalInfo || '',
                videoTitle: currentVideoData?.title || ''
            })
        });

        if (response.status === 401) {
            requireAuth(null, 'report videos');
            return;
        }

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || 'Failed to submit report');
        }

        showAlert('Thank you for your report. Our team will review it shortly.', 'success');
        document.getElementById('reportVideoForm')?.reset();
        closeModal('reportVideoModal');
    } catch (error) {
        console.error('Error reporting video:', error);
        showAlert(error.message || 'Failed to submit report', 'error');
    }
}

function showLogin() {
    window.location.href = '/login.html';
}

function showRegister() {
    window.location.href = '/register.html';
}

function loginUser() {
    showLogin();
}

function registerUser() {
    showRegister();
}

function toggleDescription() {
    const descriptionText = document.getElementById('videoDescription');
    const showMoreBtn = document.getElementById('showMoreBtn');
    if (!descriptionText || !showMoreBtn) {
        return;
    }

    if (descriptionText.classList.contains('expanded')) {
        descriptionText.classList.remove('expanded');
        descriptionText.style.maxHeight = '100px';
        showMoreBtn.innerText = 'Show more';
    } else {
        descriptionText.classList.add('expanded');
        descriptionText.style.maxHeight = 'none';
        showMoreBtn.innerText = 'Show less';
    }
}

function loadMoreComments() {
    showAlert('All comments are currently displayed.', 'info');
}

// Playlist functions
async function toggleSavePlaylist() {
    const playlistId = getCurrentPlaylistId();
    if (!playlistId || !isAuthenticated) {
        showAlert('Please log in to save playlists', 'warning');
        return;
    }

    try {
        const response = await fetch(`/api/playlists/${playlistId}/save`, {
            method: 'POST',
            credentials: 'include'
        });

        if (response.ok) {
            const result = await response.json();
            const saveBtn = document.getElementById('savePlaylistBtn');
            if (saveBtn) {
                saveBtn.textContent = result.saved ? '💾 Saved' : '💾 Save Playlist';
                saveBtn.classList.toggle('saved', result.saved);
            }
            showAlert(result.saved ? 'Playlist saved' : 'Playlist removed from saved', 'success');
        } else {
            showAlert('Failed to save playlist', 'error');
        }
    } catch (error) {
        console.error('Error saving playlist:', error);
        showAlert('Failed to save playlist', 'error');
    }
}

function getCurrentPlaylistId() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('id') || window.location.pathname.split('/').pop();
}

// Social sharing functions
function sharePlaylistOnFacebook() {
    const url = document.getElementById('sharePlaylistUrl').value;
    const shareUrl = `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`;
    window.open(shareUrl, 'facebook-share', 'width=580,height=296');
}

function sharePlaylistOnTwitter() {
    const url = document.getElementById('sharePlaylistUrl').value;
    const text = `Check out this playlist: ${currentPlaylist?.name || 'Awesome Playlist'}`;
    const shareUrl = `https://twitter.com/intent/tweet?url=${encodeURIComponent(url)}&text=${encodeURIComponent(text)}`;
    window.open(shareUrl, 'twitter-share', 'width=550,height=235');
}

function sharePlaylistOnWhatsApp() {
    const url = document.getElementById('sharePlaylistUrl').value;
    const text = `Check out this playlist: ${currentPlaylist?.name || 'Awesome Playlist'} ${url}`;
    const shareUrl = `https://wa.me/?text=${encodeURIComponent(text)}`;
    window.open(shareUrl, 'whatsapp-share');
}

function sharePlaylistViaEmail() {
    const url = document.getElementById('sharePlaylistUrl').value;
    const subject = `Check out this playlist: ${currentPlaylist?.name || 'Awesome Playlist'}`;
    const body = `I thought you might enjoy this playlist:\n\n${currentPlaylist?.name || 'Awesome Playlist'}\n${url}`;
    const mailtoUrl = `mailto:?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
    window.location.href = mailtoUrl;
}

// Utility functions
function formatViewCount(count) {
    if (count >= 1000000) {
        return (count / 1000000).toFixed(1) + 'M';
    } else if (count >= 1000) {
        return (count / 1000).toFixed(1) + 'K';
    }
    return count.toString();
}

function formatSubscriberCount(count) {
    const num = Number(count) || 0;
    if (num >= 1_000_000) {
        return (num / 1_000_000).toFixed(1).replace(/\.0$/, '') + 'M';
    }
    if (num >= 1_000) {
        return (num / 1_000).toFixed(1).replace(/\.0$/, '') + 'K';
    }
    return num.toString();
}

function formatSubscriberLabel(count) {
    const numeric = Number(count) || 0;
    const label = formatSubscriberCount(numeric);
    const plural = numeric === 1 ? 'subscriber' : 'subscribers';
    return `${label} ${plural}`;
}

function formatRoleLabel(role) {
    if (!role) {
        return '';
    }
    return role
        .toString()
        .toLowerCase()
        .split('_')
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ');
}

function formatJoinedDateLabel(dateString) {
    if (!dateString) {
        return '';
    }
    const parsed = new Date(dateString);
    if (Number.isNaN(parsed.getTime())) {
        return '';
    }
    return `Joined ${parsed.toLocaleDateString(undefined, { year: 'numeric', month: 'short' })}`;
}

function formatDuration(seconds) {
    if (!seconds) return '0:00';

    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    if (hours > 0) {
        return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
}

function formatDate(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now - date);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 1) {
        return '1 day ago';
    } else if (diffDays < 7) {
        return `${diffDays} days ago`;
    } else if (diffDays < 30) {
        const weeks = Math.floor(diffDays / 7);
        return weeks === 1 ? '1 week ago' : `${weeks} weeks ago`;
    } else if (diffDays < 365) {
        const months = Math.floor(diffDays / 30);
        return months === 1 ? '1 month ago' : `${months} months ago`;
    } else {
        const years = Math.floor(diffDays / 365);
        return years === 1 ? '1 year ago' : `${years} years ago`;
    }
}

function showAlert(message, type = 'info') {
    // Create a simple alert system
    const alertContainer = getOrCreateAlertContainer();

    const alert = document.createElement('div');
    alert.className = `alert alert-${type}`;
    alert.innerHTML = `
        <span>${message}</span>
        <button class="alert-close" onclick="this.parentElement.remove()">&times;</button>
    `;

    alertContainer.appendChild(alert);

    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (alert.parentElement) {
            alert.remove();
        }
    }, 5000);
}

function getOrCreateAlertContainer() {
    let container = document.getElementById('alert-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'alert-container';
        container.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 10000;
            max-width: 400px;
        `;
        document.body.appendChild(container);

        // Add alert styles to head if not already present
        if (!document.getElementById('alert-styles')) {
            const styles = document.createElement('style');
            styles.id = 'alert-styles';
            styles.textContent = `
                .alert {
                    padding: 12px 16px;
                    margin-bottom: 10px;
                    border-radius: 4px;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                    animation: slideIn 0.3s ease-out;
                }
                .alert-success { background: #d4edda; color: #155724; border-left: 4px solid #28a745; }
                .alert-error { background: #f8d7da; color: #721c24; border-left: 4px solid #dc3545; }
                .alert-warning { background: #fff3cd; color: #856404; border-left: 4px solid #ffc107; }
                .alert-info { background: #d1ecf1; color: #0c5460; border-left: 4px solid #17a2b8; }
                .alert-close {
                    background: none;
                    border: none;
                    font-size: 18px;
                    cursor: pointer;
                    color: inherit;
                    opacity: 0.7;
                    margin-left: 10px;
                }
                .alert-close:hover { opacity: 1; }
                @keyframes slideIn {
                    from { transform: translateX(100%); opacity: 0; }
                    to { transform: translateX(0); opacity: 1; }
                }
            `;
            document.head.appendChild(styles);
        }
    }
    return container;
}

// Form validation helpers
function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

function validatePassword(password) {
    // At least 8 characters, 1 uppercase, 1 lowercase, 1 number
    const re = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,}$/;
    return re.test(password);
}

function validateUsername(username) {
    // 3-20 characters, alphanumeric and underscore only
    const re = /^[a-zA-Z0-9_]{3,20}$/;
    return re.test(username);
}

// Error handling
function handleApiError(error, customMessage = 'An error occurred') {
    console.error('API Error:', error);

    if (error.status === 401) {
        showAlert('Please log in to continue', 'warning');
        window.location.href = '/login.html';
    } else if (error.status === 403) {
        showAlert('You do not have permission for this action', 'error');
    } else if (error.status === 404) {
        showAlert('The requested resource was not found', 'error');
    } else if (error.status >= 500) {
        showAlert('Server error. Please try again later.', 'error');
    } else {
        showAlert(customMessage, 'error');
    }
}

// Export functions for use in other scripts
window.VideoHubApp = {
    checkAuthStatus,
    logout,
    searchVideos,
    handleNavSearchSubmit,
    addToWatchLater,
    removeFromWatchLater,
    toggleLike,
    toggleDislike,
    shareVideo,
    toggleSavePlaylist,
    toggleSubscribe,
    submitComment,
    loadVideoDetails,
    loadComments,
    showAlert,
    formatViewCount,
    formatSubscriberCount,
    formatDuration,
    formatDate,
    validateEmail,
    validatePassword,
    validateUsername,
    handleApiError,
    openRatingModal,
    submitVideoRating,
    goToReportPage
};

Object.assign(window, {
    searchVideos,
    handleNavSearchSubmit,
    toggleLike,
    toggleDislike,
    shareVideo,
    goToReportPage,
    closeModal,
    showLogin,
    showRegister,
    loginUser,
    registerUser,
    addToPlaylist,
    toggleSubscribe,
    reportVideoSubmit,
    submitComment,
    cancelComment,
    sortComments,
    copyLink,
    shareOnFacebook,
    shareOnTwitter,
    shareOnWhatsApp,
    shareViaEmail,
    toggleDescription,
    loadMoreComments,
    openRatingModal,
    submitVideoRating,
    showAlert,
    addToWatchLater,
    removeFromWatchLater
});
