(() => {
    const reorderState = {
        activePlaylist: null,
        originalOrder: [],
        workingOrder: []
    };

    document.addEventListener('DOMContentLoaded', () => {
        const scriptTag = document.getElementById('profile-playlists-module');
        if (!scriptTag) {
            return;
        }

        document.body.classList.add('profile-reorder-ready');

        document.addEventListener('click', event => {
            const reorderTrigger = event.target.closest('[data-action="open-playlist-reorder"]');
            if (reorderTrigger) {
                const playlistId = reorderTrigger.dataset.playlistId;
                openReorderModal(playlistId);
                return;
            }

            const closeTrigger = event.target.closest('[data-action="close-reorder"]');
            if (closeTrigger) {
                closeReorderModal();
                return;
            }
        });
    });

    async function openReorderModal(playlistId) {
        if (!playlistId) {
            window.showAlert?.('Playlist information missing.', 'error');
            return;
        }

        try {
            const response = await fetch(`/api/playlists/${playlistId}`, { credentials: 'include' });
            if (!response.ok) {
                const message = await response.text();
                throw new Error(message || 'Unable to load playlist');
            }

            const playlist = await response.json();
            reorderState.activePlaylist = playlist;
            reorderState.originalOrder = Array.isArray(playlist.videos)
                ? playlist.videos.map(video => video.videoId)
                : [];
            reorderState.workingOrder = [...reorderState.originalOrder];

            renderReorderModal(playlist);
        } catch (error) {
            console.error('Error loading playlist for reorder:', error);
            window.showAlert?.(error.message || 'Failed to open reorder dialog.', 'error');
        }
    }

    function renderReorderModal(playlist) {
        const modal = ensureModalSkeleton();
        const listContainer = modal.querySelector('.reorder-list');
        const titleEl = modal.querySelector('.reorder-modal-title');
        const countEl = modal.querySelector('.reorder-count');

        titleEl.textContent = `Reorder “${playlist.name || 'Untitled playlist'}”`;
        const videos = Array.isArray(playlist.videos) ? playlist.videos : [];
        countEl.textContent = `${videos.length} video${videos.length === 1 ? '' : 's'}`;

        if (videos.length === 0) {
            listContainer.innerHTML = '<div class="empty-state">No videos in this playlist yet.</div>';
        } else {
            listContainer.innerHTML = '';
            videos.forEach((video, index) => {
                const item = document.createElement('div');
                item.className = 'reorder-item';
                item.dataset.videoId = video.videoId;
                item.innerHTML = `
                    <div class="reorder-handle" title="Drag to reorder">☰</div>
                    <div class="reorder-thumbnail">
                        <img src="${video.thumbnailPath || '/images/default-thumbnail.jpg'}" alt="${escapeHtml(video.title || '')}" onerror="this.src='/images/default-thumbnail.jpg'">
                    </div>
                    <div class="reorder-info">
                        <h4>${escapeHtml(video.title || 'Untitled video')}</h4>
                        <p>${formatDuration(video.duration)} • ${formatNumber(video.viewCount || 0)} views</p>
                    </div>
                    <button type="button" class="btn btn-link" data-action="remove-from-reorder">Remove</button>
                `;

                listContainer.appendChild(item);
            });

            enableDragAndDrop(listContainer);
        }

        modal.style.display = 'flex';
    }

    function ensureModalSkeleton() {
        let modal = document.getElementById('playlistReorderModal');
        if (modal) {
            return modal;
        }

        modal = document.createElement('div');
        modal.id = 'playlistReorderModal';
        modal.className = 'modal reorder-modal';
        modal.innerHTML = `
            <div class="modal-content reorder-modal-content">
                <span class="close" data-action="close-reorder">&times;</span>
                <h2 class="reorder-modal-title">Reorder playlist</h2>
                <p class="reorder-count">0 videos</p>
                <div class="reorder-list"></div>
                <div class="modal-actions">
                    <button class="btn btn-secondary" type="button" data-action="close-reorder">Cancel</button>
                    <button class="btn" type="button" id="saveReorderBtn">Save order</button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        modal.addEventListener('click', event => {
            if (event.target === modal) {
                closeReorderModal();
            }
        });

        modal.querySelector('#saveReorderBtn').addEventListener('click', submitReorderChanges);
        modal.querySelector('.reorder-list').addEventListener('click', handleListClick);

        return modal;
    }

    function enableDragAndDrop(listContainer) {
        let dragSrcEl = null;

        const handleDragStart = event => {
            const item = event.currentTarget;
            dragSrcEl = item;
            item.classList.add('dragging');
            event.dataTransfer.effectAllowed = 'move';
            event.dataTransfer.setData('text/plain', item.dataset.videoId || '');
        };

        const handleDragOver = event => {
            event.preventDefault();
            event.dataTransfer.dropEffect = 'move';
            const target = event.target.closest('.reorder-item');
            if (!target || target === dragSrcEl) {
                return;
            }

            const rect = target.getBoundingClientRect();
            const shouldInsertAfter = (event.clientY - rect.top) / (rect.bottom - rect.top) > 0.5;
            if (shouldInsertAfter) {
                target.after(dragSrcEl);
            } else {
                target.before(dragSrcEl);
            }
        };

        const handleDragEnd = event => {
            const item = event.currentTarget;
            item.classList.remove('dragging');
            dragSrcEl = null;
            syncWorkingOrder(listContainer);
        };

        listContainer.querySelectorAll('.reorder-item').forEach(item => {
            item.setAttribute('draggable', 'true');
            item.addEventListener('dragstart', handleDragStart);
            item.addEventListener('dragover', handleDragOver);
            item.addEventListener('dragend', handleDragEnd);
        });
    }

    function handleListClick(event) {
        const removeButton = event.target.closest('[data-action="remove-from-reorder"]');
        if (!removeButton) {
            return;
        }

        const item = removeButton.closest('.reorder-item');
        if (!item) {
            return;
        }

        item.remove();
        syncWorkingOrder(item.parentElement);
    }

    function syncWorkingOrder(listContainer) {
        reorderState.workingOrder = Array.from(listContainer.querySelectorAll('.reorder-item'))
            .map(item => Number(item.dataset.videoId))
            .filter(Boolean);
    }

    async function submitReorderChanges() {
        const playlist = reorderState.activePlaylist;
        if (!playlist) {
            closeReorderModal();
            return;
        }

        const modal = document.getElementById('playlistReorderModal');
        const saveButton = modal?.querySelector('#saveReorderBtn');
        if (saveButton) {
            saveButton.disabled = true;
            saveButton.textContent = 'Saving…';
        }

        try {
            const payload = { videoIds: reorderState.workingOrder };
            const response = await fetch(`/api/playlists/${playlist.id}/videos/reorder`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const message = await response.text();
                throw new Error(message || 'Failed to save new order.');
            }

            window.showAlert?.('Playlist order updated successfully.', 'success');
            closeReorderModal();
            document.dispatchEvent(new CustomEvent('playlist-reordered', {
                detail: {
                    playlistId: playlist.id,
                    videoIds: reorderState.workingOrder
                }
            }));
        } catch (error) {
            console.error('Error saving playlist order:', error);
            window.showAlert?.(error.message || 'Failed to save playlist order.', 'error');
        } finally {
            if (saveButton) {
                saveButton.disabled = false;
                saveButton.textContent = 'Save order';
            }
        }
    }

    function closeReorderModal() {
        const modal = document.getElementById('playlistReorderModal');
        if (modal) {
            modal.style.display = 'none';
        }
        reorderState.activePlaylist = null;
        reorderState.originalOrder = [];
        reorderState.workingOrder = [];
    }

    function escapeHtml(value) {
        if (value == null) {
            return '';
        }
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatDuration(durationInSeconds) {
        const seconds = Number(durationInSeconds || 0);
        const minutes = Math.floor(seconds / 60);
        const secondsPart = Math.floor(seconds % 60).toString().padStart(2, '0');
        return `${minutes}:${secondsPart}`;
    }

    function formatNumber(value) {
        const number = Number(value || 0);
        return Number.isNaN(number) ? '0' : number.toLocaleString();
    }
})();