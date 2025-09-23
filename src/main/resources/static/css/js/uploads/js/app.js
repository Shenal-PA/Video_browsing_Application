// Global variables
let currentUser = null;
let guestViewCount = 0;
const MAX_GUEST_VIEWS = 5;

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    checkCurrentUser();
    loadCategories();
    loadLatestVideos();
    loadTopRatedVideos();
});

// User Authentication Functions
async function checkCurrentUser() {
    try {
        const response = await fetch('/api/users/current');
        if (response.ok) {
            currentUser = await response.json();
            updateNavigation();
        } else {
            currentUser = null;
            updateNavigation();
        }
    } catch (error) {
        console.error('Error checking current user:', error);
        currentUser = null;
        updateNavigation();
    }
}

function updateNavigation() {
    const guestNav = document.getElementById('guestNav');
    const userNav = document.getElementById('userNav');
    const welcomeUser = document.getElementById('welcomeUser');

    if (currentUser) {
        guestNav.style.display = 'none';
        userNav.style.display = 'flex';
        welcomeUser.textContent = `Welcome, ${currentUser.username}`;
    } else {
        guestNav.style.display = 'flex';
        userNav.style.display = 'none';
    }
}

async function login(event) {
    event.preventDefault();

    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const response = await fetch('/api/users/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ username, password })
        });

        if (response.ok) {
            closeModal('loginModal');
            checkCurrentUser();
            showAlert('Login successful!', 'success');
        } else {
            const error = await response.text();
            showAlert(error, 'error');
        }
    } catch (error) {
        console.error('Login error:', error);
        showAlert('Login failed. Please try again.', 'error');
    }
}

async function register(event) {
    event.preventDefault();

    const username = document.getElementById('regUsername').value;
    const email = document.getElementById('regEmail').value;
    const password = document.getElementById('regPassword').value;
    const firstName = document.getElementById('regFirstName').value;
    const lastName = document.getElementById('regLastName').value;
    const phone = document.getElementById('regPhone').value;
    const role = document.getElementById('regRole').value;
    const termsAgreed = document.getElementById('agreeTerms').checked;

    if (!termsAgreed) {
        showAlert('You must agree to the Terms and Conditions', 'error');
        return;
    }

    try {
        const response = await fetch('/api/users/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                username,
                email,
                password,
                firstName,
                lastName,
                phone,
                role,
                termsAgreed
            })
        });

        if (response.ok) {
            closeModal('registerModal');
            showAlert('Registration successful! Please login.', 'success');
            showLogin();
        } else {
            const error = await response.text();
            showAlert(error, 'error');
        }
    } catch (error) {
        console.error('Registration error:', error);
        showAlert('Registration failed. Please try again.', 'error');
    }
}

async function logout() {
    try {
        await fetch('/api/users/logout', { method: 'POST' });
        currentUser = null;
        updateNavigation();
        showAlert('Logged out successfully!', 'success');
        // Reload page to reset state
        window.location.reload();
    } catch (error) {
        console.error('Logout error:', error);
    }
}

// Video Functions
async function loadLatestVideos() {
    try {
        const response = await fetch('/api/videos/latest');
        if (response.ok) {
            const videos = await response.json();
            displayVideos(videos, 'latestVideos');
        }
    } catch (error) {
        console.error('Error loading latest videos:', error);
    }
}

async function loadTopRatedVideos() {
    try {
        const response = await fetch('/api/videos/top-rated');
        if (response.ok) {
            const videos = await response.json();
            displayVideos(videos, 'topRatedVideos');
        }
    } catch (error) {
        console.error('Error loading top rated videos:', error);
    }
}

async function searchVideos() {
    const keyword = document.getElementById('searchInput').value;
    const categoryId = document.getElementById('categoryFilter').value;

    let url = '/api/videos/search';
    const params = new URLSearchParams();

    if (keyword) {
        params.append('keyword', keyword);
    }
    if (categoryId) {
        params.append('categoryId', categoryId);
    }

    if (params.toString()) {
        url += '?' + params.toString();
    } else {
        url = '/api/videos/public';
    }

    try {
        const response = await fetch(url);
        if (response.ok) {
            const videos = await response.json();
            displayVideos(videos, 'searchResults');
            document.getElementById('searchResults').style.display = 'block';
        }
    } catch (error) {
        console.error('Error searching videos:', error);
    }
}

function displayVideos(videos, containerId) {
    const container = document.getElementById(containerId);
    container.innerHTML = '';

    if (videos.length === 0) {
        container.innerHTML = '<p class="text-center">No videos found.</p>';
        return;
    }

    videos.forEach(video => {
        const videoCard = createVideoCard(video);
        container.appendChild(videoCard);
    });
}

function createVideoCard(video) {
    const card = document.createElement('div');
    card.className = 'video-card';
    card.onclick = () => openVideoModal(video.id);

    const thumbnail = video.thumbnailPath || '/images/default-thumbnail.jpg';
    const duration = formatDuration(video.duration);
    const views = formatViews(video.viewCount);

    card.innerHTML = `
        <img src="${thumbnail}" alt="${video.title}" class="video-thumbnail"
             onerror="this.src='/images/default-thumbnail.jpg'">
        <div class="video-info">
            <div class="video-title">${escapeHtml(video.title)}</div>
            <div class="video-meta">
                <span class="video-uploader">${escapeHtml(video.uploadedBy?.username || 'Unknown')}</span>
                <span>${views} views</span>
            </div>
            <div class="video-meta">
                <span>${duration}</span>
                <span>${formatDate(video.createdAt)}</span>
            </div>
        </div>
    `;

    return card;
}

async function openVideoModal(videoId) {
    // Check guest user limitations
    if (!currentUser) {
        guestViewCount++;
        if (guestViewCount > MAX_GUEST_VIEWS) {
            showAlert('You have reached the maximum number of videos for guest users. Please register to continue watching.', 'error');
            showRegister();
            return;
        }
        if (guestViewCount === MAX_GUEST_VIEWS) {
            showAlert('This is your last free video as a guest. Register to continue watching unlimited videos!', 'warning');
        }
    }

    try {
        const response = await fetch(`/api/videos/${videoId}`);
        if (response.ok) {
            const video = await response.json();
            displayVideoModal(video);
        } else {
            showAlert('Video not found or unavailable.', 'error');
        }
    } catch (error) {
        console.error('Error loading video:', error);
        showAlert('Error loading video.', 'error');
    }
}

async function displayVideoModal(video) {
    const modal = document.getElementById('videoModal');
    const content = document.getElementById('videoContent');

    const canInteract = currentUser !== null;
    const isOwner = currentUser && video.uploadedBy.id === currentUser.id;

    content.innerHTML = `
        <div class="video-player-container">
            <video controls class="video-player" src="${video.filePath}">
                Your browser does not support the video tag.
            </video>
        </div>

        <div class="video-details">
            <h2 class="video-title">${escapeHtml(video.title)}</h2>
            <div class="video-meta">
                <span class="video-uploader">By: ${escapeHtml(video.uploadedBy.username)}</span>
                <span>${formatViews(video.viewCount)} views</span>
                <span>Uploaded: ${formatDate(video.createdAt)}</span>
            </div>

            ${canInteract ? `
                <div class="video-actions">
                    <button class="action-btn" onclick="toggleLike(${video.id}, 'LIKE')">
                        👍 Like (${video.likeCount})
                    </button>
                    <button class="action-btn" onclick="toggleLike(${video.id}, 'DISLIKE')">
                        👎 Dislike (${video.dislikeCount})
                    </button>
                    <button class="action-btn" onclick="addToWatchLater(${video.id})">
                        📋 Watch Later
                    </button>
                    <button class="action-btn" onclick="showReportForm(${video.id})">
                        🚨 Report
                    </button>
                </div>
            ` : ''}

            <div class="video-description">
                <h3>Description</h3>
                <p>${escapeHtml(video.description || 'No description available.')}</p>
            </div>

            <div class="video-category">
                <strong>Category:</strong> ${video.category ? escapeHtml(video.category.name) : 'Uncategorized'}
            </div>
        </div>

        <div class="comments-section">
            <h3>Comments</h3>
            ${canInteract ? `
                <div class="comment-form">
                    <textarea id="commentText" placeholder="Add a comment..." rows="3"></textarea>
                    <button onclick="addComment(${video.id})">Post Comment</button>
                </div>
            ` : '<p class="text-center">Please <a href="#" onclick="showLogin()">login</a> to comment.</p>'}
            <div id="commentsContainer">Loading comments...</div>
        </div>
    `;

    modal.style.display = 'flex';

    // Load comments
    if (canInteract) {
        loadComments(video.id);
    }
}

// Category Functions
async function loadCategories() {
    try {
        const response = await fetch('/api/search/categories');
        if (response.ok) {
            const categories = await response.json();
            const select = document.getElementById('categoryFilter');

            categories.forEach(category => {
                const option = document.createElement('option');
                option.value = category.id;
                option.textContent = category.name;
                select.appendChild(option);
            });
        }
    } catch (error) {
        console.error('Error loading categories:', error);
    }
}

// Comment Functions
async function loadComments(videoId) {
    try {
        const response = await fetch(`/api/comments/video/${videoId}`);
        if (response.ok) {
            const comments = await response.json();
            displayComments(comments);
        }
    } catch (error) {
        console.error('Error loading comments:', error);
    }
}

function displayComments(comments) {
    const container = document.getElementById('commentsContainer');

    if (comments.length === 0) {
        container.innerHTML = '<p class="text-center">No comments yet. Be the first to comment!</p>';
        return;
    }

    container.innerHTML = comments.map(comment => `
        <div class="comment">
            <div class="comment-author">${escapeHtml(comment.user.username)}</div>
            <div class="comment-content">${escapeHtml(comment.content)}</div>
            <div class="comment-meta">
                <small>${formatDate(comment.createdAt)}</small>
                ${currentUser && currentUser.role === 'ADMIN' ? `
                    <button onclick="deleteComment(${comment.id})" class="action-btn" style="font-size: 0.8rem;">Delete</button>
                ` : ''}
            </div>
        </div>
    `).join('');
}

async function addComment(videoId) {
    const commentText = document.getElementById('commentText').value.trim();

    if (!commentText) {
        showAlert('Please enter a comment.', 'error');
        return;
    }

    try {
        const response = await fetch('/api/comments', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                videoId: videoId,
                content: commentText
            })
        });

        if (response.ok) {
            document.getElementById('commentText').value = '';
            loadComments(videoId);
            showAlert('Comment added successfully!', 'success');
        } else {
            const error = await response.text();
            showAlert(error, 'error');
        }
    } catch (error) {
        console.error('Error adding comment:', error);
        showAlert('Error adding comment.', 'error');
    }
}

// Modal Functions
function showLogin() {
    closeAllModals();
    document.getElementById('loginModal').style.display = 'flex';
}

function showRegister() {
    closeAllModals();
    document.getElementById('registerModal').style.display = 'flex';
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

function closeAllModals() {
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => modal.style.display = 'none');
}

// Utility Functions
function formatDuration(seconds) {
    if (!seconds) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

function formatViews(views) {
    if (!views) return '0';
    if (views >= 1000000) {
        return (views / 1000000).toFixed(1) + 'M';
    }
    if (views >= 1000) {
        return (views / 1000).toFixed(1) + 'K';
    }
    return views.toString();
}

function formatDate(dateString) {
    if (!dateString) return 'Unknown';
    const date = new Date(dateString);
    return date.toLocaleDateString();
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showAlert(message, type = 'info') {
    // Create alert element
    const alert = document.createElement('div');
    alert.className = `alert alert-${type}`;
    alert.textContent = message;
    alert.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        z-index: 9999;
        padding: 15px;
        border-radius: 4px;
        color: white;
        font-weight: bold;
        max-width: 300px;
        ${type === 'success' ? 'background-color: #28a745;' : ''}
        ${type === 'error' ? 'background-color: #dc3545;' : ''}
        ${type === 'warning' ? 'background-color: #ffc107; color: #000;' : ''}
        ${type === 'info' ? 'background-color: #17a2b8;' : ''}
    `;

    document.body.appendChild(alert);

    // Remove after 3 seconds
    setTimeout(() => {
        if (alert.parentNode) {
            alert.parentNode.removeChild(alert);
        }
    }, 3000);
}

function goToProfile() {
    if (currentUser) {
        window.location.href = '/profile.html';
    }
}

// Video interaction functions
async function toggleLike(videoId, type) {
    try {
        const response = await fetch('/api/ratings', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                videoId: videoId,
                ratingType: type
            })
        });

        if (response.ok) {
            // Refresh video modal to show updated counts
            openVideoModal(videoId);
        } else {
            const error = await response.text();
            showAlert(error, 'error');
        }
    } catch (error) {
        console.error('Error toggling like:', error);
        showAlert('Error updating rating.', 'error');
    }
}

async function addToWatchLater(videoId) {
    try {
        const response = await fetch('/api/playlists/watch-later', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ videoId: videoId })
        });

        if (response.ok) {
            showAlert('Added to Watch Later!', 'success');
        } else {
            const error = await response.text();
            showAlert(error, 'error');
        }
    } catch (error) {
        console.error('Error adding to watch later:', error);
        showAlert('Error adding to watch later.', 'error');
    }
}

function showReportForm(videoId) {
    const reportTypes = [
        'SPAM',
        'INAPPROPRIATE_CONTENT',
        'COPYRIGHT',
        'PLAYBACK_ISSUE',
        'OTHER'
    ];

    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.style.display = 'flex';

    modal.innerHTML = `
        <div class="modal-content">
            <span class="close" onclick="this.parentElement.parentElement.remove()">&times;</span>
            <h2>Report Video</h2>
            <form onsubmit="submitReport(event, ${videoId})">
                <label>Report Type:</label>
                <select id="reportType" required>
                    ${reportTypes.map(type => `<option value="${type}">${type.replace('_', ' ')}</option>`).join('')}
                </select>
                <label>Description:</label>
                <textarea id="reportDescription" placeholder="Please describe the issue..." rows="4" required></textarea>
                <button type="submit">Submit Report</button>
                <button type="button" onclick="this.closest('.modal').remove()">Cancel</button>
            </form>
        </div>
    `;

    document.body.appendChild(modal);
}

async function submitReport(event, videoId) {
    event.preventDefault();

    const reportType = document.getElementById('reportType').value;
    const description = document.getElementById('reportDescription').value;

    try {
        const response = await fetch('/api/reports', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                videoId: videoId,
                reportType: reportType,
                description: description
            })
        });

        if (response.ok) {
            showAlert('Report submitted successfully. Thank you for helping keep our platform safe.', 'success');
            event.target.closest('.modal').remove();
        } else {
            const error = await response.text();
            showAlert(error, 'error');
        }
    } catch (error) {
        console.error('Error submitting report:', error);
        showAlert('Error submitting report.', 'error');
    }
}

// Close modals when clicking outside
window.onclick = function(event) {
    if (event.target.classList.contains('modal')) {
        event.target.style.display = 'none';
    }
}

// Keyboard shortcuts
document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        closeAllModals();
    }
});

// Profile page specific JavaScript
let currentUser = null;

document.addEventListener('DOMContentLoaded', function() {
    loadCurrentUser();
    loadCategories();
});

async function loadCurrentUser() {
    try {
        const response = await fetch('/api/users/current');
        if (response.ok) {
            currentUser = await response.json();
            displayUserProfile();
            loadUserData();
        } else {
            // Redirect to home if not logged in
            window.location.href = '/';
        }
    } catch (error) {
        console.error('Error loading user:', error);
        window.location.href = '/';
    }
}

function displayUserProfile() {
    document.getElementById('profileName').textContent =
        `${currentUser.firstName || ''} ${currentUser.lastName || ''}`.trim() || currentUser.username;
    document.getElementById('profileUsername').textContent = `@${currentUser.username}`;
    document.getElementById('profileRole').textContent = `Role: ${currentUser.role.replace('_', ' ')}`;
    document.getElementById('profileBio').textContent = currentUser.bio || 'No bio available.';

    if (currentUser.profilePicture) {
        document.getElementById('profilePicture').src = currentUser.profilePicture;
    }

    // Show content creator actions
    if (currentUser.role === 'CONTENT_CREATOR' || currentUser.role === 'ADMIN') {
        document.getElementById('creatorActions').style.display = 'block';
    }

    // Show admin tab
    if (currentUser.role === 'ADMIN') {
        document.getElementById('adminTab').style.display = 'block';
    }
}

async function loadUserData() {
    await Promise.all([
        loadMyVideos(),
        loadMyPlaylists(),
        loadWatchLater()
    ]);

    if (currentUser.role === 'ADMIN') {
        loadAdminData();
    }
}

async function loadMyVideos() {
    try {
        const response = await fetch('/api/videos/my-videos');
        if (response.ok) {
            const videos = await response.json();
            displayMyVideos(videos);
        }
    } catch (error) {
        console.error('Error loading videos:', error);
    }
}

function displayMyVideos(videos) {
    const container = document.getElementById('myVideos');

    if (videos.length === 0) {
        container.innerHTML = '<p>No videos uploaded yet.</p>';
        return;
    }

    container.innerHTML = videos.map(video => `
        <div class="video-card">
            <img src="${video.thumbnailPath || '/images/default-thumbnail.jpg'}"
                 alt="${video.title}" class="video-thumbnail">
            <div class="video-info">
                <div class="video-title">${escapeHtml(video.title)}</div>
                <div class="video-meta">
                    <span>${formatViews(video.viewCount)} views</span>
                    <span>${video.status}</span>
                </div>
                <div class="video-actions">
                    <button onclick="editVideo(${video.id})">Edit</button>
                    <button onclick="deleteVideo(${video.id})" class="danger-btn">Delete</button>
                </div>
            </div>
        </div>
    `).join('');
}

async function loadMyPlaylists() {
    try {
        const response = await fetch('/api/playlists/my-playlists');
        if (response.ok) {
            const playlists = await response.json();
            displayMyPlaylists(playlists);
        }
    } catch (error) {
        console.error('Error loading playlists:', error);
    }
}

function displayMyPlaylists(playlists) {
    const container = document.getElementById('myPlaylists');

    if (playlists.length === 0) {
        container.innerHTML = '<p>No playlists created yet.</p>';
        return;
    }

    container.innerHTML = playlists.map(playlist => `
        <div class="playlist-card">
            <div class="playlist-info">
                <h3>${escapeHtml(playlist.name)}</h3>
                <p>${escapeHtml(playlist.description || 'No description')}</p>
                <div class="playlist-meta">
                    <span>${playlist.videoCount || 0} videos</span>
                    <span>${playlist.privacy}</span>
                </div>
                <div class="playlist-actions">
                    <button onclick="viewPlaylist(${playlist.id})">View</button>
                    <button onclick="editPlaylist(${playlist.id})">Edit</button>
                    <button onclick="deletePlaylist(${playlist.id})" class="danger-btn">Delete</button>
                </div>
            </div>
        </div>
    `).join('');
}

async function loadWatchLater() {
    try {
        const response = await fetch('/api/playlists/watch-later');
        if (response.ok) {
            const watchLaterItems = await response.json();
            displayWatchLater(watchLaterItems);
        }
    } catch (error) {
        console.error('Error loading watch later:', error);
    }
}

function displayWatchLater(items) {
    const container = document.getElementById('watchLaterVideos');

    if (items.length === 0) {
        container.innerHTML = '<p>No videos in watch later.</p>';
        return;
    }

    container.innerHTML = items.map(item => `
        <div class="video-card">
            <img src="${item.video.thumbnailPath || '/images/default-thumbnail.jpg'}"
                 alt="${item.video.title}" class="video-thumbnail"
                 onclick="openVideoModal(${item.video.id})">
            <div class="video-info">
                <div class="video-title">${escapeHtml(item.video.title)}</div>
                <div class="video-meta">
                    <span>Added: ${formatDate(item.addedAt)}</span>
                </div>
                <div class="video-actions">
                    <button onclick="removeFromWatchLater(${item.video.id})" class="danger-btn">Remove</button>
                </div>
            </div>
        </div>
    `).join('');
}

// Admin functions
async function loadAdminData() {
    try {
        const [usersResponse, videosResponse, reportsResponse] = await Promise.all([
            fetch('/api/admin/stats/users'),
            fetch('/api/admin/stats/videos'),
            fetch('/api/admin/reports/pending')
        ]);

        if (usersResponse.ok) {
            const userStats = await usersResponse.json();
            document.getElementById('totalUsers').textContent = userStats.total;
            document.getElementById('totalCreators').textContent = userStats.creators;
        }

        if (videosResponse.ok) {
            const videoStats = await videosResponse.json();
            document.getElementById('totalVideos').textContent = videoStats.total;
        }

        if (reportsResponse.ok) {
            const reports = await reportsResponse.json();
            document.getElementById('pendingReports').textContent = reports.length;
            displayRecentReports(reports.slice(0, 5)); // Show first 5
        }
    } catch (error) {
        console.error('Error loading admin data:', error);
    }
}

function displayRecentReports(reports) {
    const container = document.getElementById('recentReports');

    if (reports.length === 0) {
        container.innerHTML = '<p>No pending reports.</p>';
        return;
    }

    container.innerHTML = reports.map(report => `
        <div class="report-item">
            <div class="report-info">
                <strong>${report.reportType}</strong>
                <p>${escapeHtml(report.description)}</p>
                <small>Reported by: ${report.reportedByUsername} on ${formatDate(report.createdAt)}</small>
            </div>
            <div class="report-actions">
                <button onclick="resolveReport(${report.id})">Resolve</button>
                <button onclick="dismissReport(${report.id})">Dismiss</button>
            </div>
        </div>
    `).join('');
}

// Tab management
function showTab(tabName) {
    // Hide all tab contents
    const tabContents = document.querySelectorAll('.tab-content');
    tabContents.forEach(tab => tab.style.display = 'none');

    // Remove active class from all buttons
    const tabButtons = document.querySelectorAll('.tab-button');
    tabButtons.forEach(btn => btn.classList.remove('active'));

    // Show selected tab and mark button as active
    document.getElementById(tabName + 'Tab').style.display = 'block';
    event.target.classList.add('active');
}

// Modal functions
function showEditProfile() {
    // Populate form with current data
    document.getElementById('editFirstName').value = currentUser.firstName || '';
    document.getElementById('editLastName').value = currentUser.lastName || '';
    document.getElementById('editEmail').value = currentUser.email || '';
    document.getElementById('editPhone').value = currentUser.phone || '';
    document.getElementById('editBio').value = currentUser.bio || '';

    document.getElementById('editProfileModal').style.display = 'flex';
}

async function updateProfile(event) {
    event.preventDefault();

    const profileData = {
        firstName: document.getElementById('editFirstName').value,
        lastName: document.getElementById('editLastName').value,
        email: document.getElementById('editEmail').value,
        phone: document.getElementById('editPhone').value,
        bio: document.getElementById('editBio').value
    };

    try {
        const response = await fetch('/api/users/profile', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(profileData)
        });

        if (response.ok) {
            closeModal('editProfileModal');
            showAlert('Profile updated successfully!', 'success');
            loadCurrentUser(); // Reload user data
        } else {
            const error = await response.text();
            showAlert(error, 'error');
        }
    } catch (error) {
        console.error('Error updating profile:', error);
        showAlert('Error updating profile.', 'error');
    }
}

function showUploadVideo() {
    document.getElementById('uploadVideoModal').style.display = 'flex';
}

async function uploadVideo(event) {
    event.preventDefault();

    const formData = new FormData();
    formData.append('title', document.getElementById('videoTitle').value);
    formData.append('description', document.getElementById('videoDescription').value);
    formData.append('categoryId', document.getElementById('videoCategory').value);
    formData.append('privacy', document.getElementById('videoPrivacy').value);
    formData.append('tags', document.getElementById('videoTags').value);
    formData.append('videoFile', document.getElementById('videoFile').files[0]);

    const thumbnailFile = document.getElementById('thumbnailFile').files[0];
    if (thumbnailFile) {
        formData.append('thumbnailFile', thumbnailFile);
    }

    try {
        const response = await fetch('/api/videos/upload', {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            closeModal('uploadVideoModal');
            showAlert('Video uploaded successfully!', 'success');
            loadMyVideos(); // Reload videos

            // Reset form
            event.target.reset();
        } else {
            const error = await response.text();
            showAlert(error, 'error');
        }
    } catch (error) {
        console.error('Error uploading video:', error);
        showAlert('Error uploading video.', 'error');
    }
}

// Utility functions for profile page
async function deleteVideo(videoId) {
    if (!confirm('Are you sure you want to delete this video?')) {
        return;
    }

    try {
        const response = await fetch(`/api/videos/${videoId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showAlert('Video deleted successfully!', 'success');
            loadMyVideos();
        } else {
            const error = await response.text();
            showAlert(error, 'error');
        }
    } catch (error) {
        console.error('Error deleting video:', error);
        showAlert('Error deleting video.', 'error');
    }
}

async function removeFromWatchLater(videoId) {
    try {
        const response = await fetch(`/api/playlists/watch-later/${videoId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showAlert('Removed from watch later!', 'success');
            loadWatchLater();
        } else {
            const error = await response.text();
            showAlert(error, 'error');
        }
    } catch (error) {
        console.error('Error removing from watch later:', error);
        showAlert('Error removing from watch later.', 'error');
    }
}

async function logout() {
    try {
        await fetch('/api/users/logout', { method: 'POST' });
        window.location.href = '/';
    } catch (error) {
        console.error('Logout error:', error);
        window.location.href = '/';
    }
}