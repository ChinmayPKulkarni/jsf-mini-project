// ========================================================
// BorrowBox – Frontend Client Application (Shadcn-styled)
// Lucide Icons Integration (No Emojis)
// ========================================================

const state = {
  currentUser: null,
  activeTab: 'catalog',
  categoryFilter: '',
  conditionFilter: '',
  statusFilter: '',
  searchKeyword: '',
  catalogItems: [],
  searchDebounceTimer: null
};

// Helper to refresh Lucide SVG icons whenever DOM updates
function refreshIcons() {
  if (window.lucide && typeof window.lucide.createIcons === 'function') {
    window.lucide.createIcons();
  }
}

// --- Initialization ---
document.addEventListener('DOMContentLoaded', () => {
  initApp();
});

async function initApp() {
  await checkAuthState();
  loadCatalog();
  setMinDates();
  refreshIcons();
}

function setMinDates() {
  const todayStr = new Date().toISOString().split('T')[0];
  const startDateInput = document.getElementById('borrow-start-date');
  const endDateInput = document.getElementById('borrow-end-date');
  if (startDateInput) startDateInput.min = todayStr;
  if (endDateInput) endDateInput.min = todayStr;
}

function handleStartDateChange() {
  const startDateInput = document.getElementById('borrow-start-date');
  const endDateInput = document.getElementById('borrow-end-date');
  if (startDateInput && endDateInput && startDateInput.value) {
    endDateInput.min = startDateInput.value;
    if (endDateInput.value && endDateInput.value < startDateInput.value) {
      endDateInput.value = startDateInput.value;
    }
  }
}

// --- Authentication & Session State ---
async function checkAuthState() {
  try {
    const res = await fetch('/api/auth/me');
    if (res.ok) {
      state.currentUser = await res.json();
    } else {
      state.currentUser = null;
    }
  } catch (err) {
    state.currentUser = null;
  }
  renderNavbar();
  if (state.currentUser) {
    updateBadges();
  }
}

function renderNavbar() {
  const container = document.getElementById('nav-user-container');
  if (!container) return;

  if (state.currentUser) {
    const initials = state.currentUser.fullName
      .split(' ')
      .map(n => n[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();

    const ratingVal = state.currentUser.averageRating ? state.currentUser.averageRating.toFixed(1) : '0.0';
    const ratingCount = state.currentUser.ratingCount || 0;

    container.innerHTML = `
      <div class="user-badge">
        <div class="user-avatar">${initials}</div>
        <div style="display: flex; flex-direction: column; line-height: 1.2;">
          <span style="font-weight: 600;">${escapeHtml(state.currentUser.fullName)}</span>
          <span style="font-size: 0.7rem; color: var(--muted-foreground);">${escapeHtml(state.currentUser.usn)}</span>
        </div>
        <div class="rating-pill" title="Community Trust Rating">
          <i data-lucide="star" class="icon-xs fill-amber"></i> ${ratingVal} (${ratingCount})
        </div>
      </div>
      <button class="btn btn-outline btn-sm" onclick="handleLogout()">Sign Out</button>
    `;
  } else {
    container.innerHTML = `
      <button class="btn btn-outline btn-sm" onclick="openAuthModal('login')">Sign In</button>
      <button class="btn btn-primary btn-sm" onclick="openAuthModal('register')">Register</button>
    `;
  }
  refreshIcons();
}

async function updateBadges() {
  if (!state.currentUser) return;
  try {
    const [incomingRes, borrowsRes] = await Promise.all([
      fetch('/api/requests/incoming'),
      fetch('/api/requests/my-borrows')
    ]);

    if (incomingRes.ok) {
      const incoming = await incomingRes.json();
      const pendingCount = incoming.filter(r => r.status === 'PENDING').length;
      const badge = document.getElementById('badge-incoming-count');
      if (badge) {
        badge.textContent = pendingCount;
        badge.style.display = pendingCount > 0 ? 'inline-block' : 'none';
      }
    }

    if (borrowsRes.ok) {
      const borrows = await borrowsRes.json();
      const activeCount = borrows.filter(r => r.status === 'ACTIVE' || r.status === 'OVERDUE').length;
      const badge = document.getElementById('badge-borrows-count');
      if (badge) {
        badge.textContent = activeCount;
        badge.style.display = activeCount > 0 ? 'inline-block' : 'none';
      }
    }
  } catch (err) {
    console.error('Failed to update badges', err);
  }
}

// --- Navigation Tabs ---
function switchTab(tabName) {
  if (['incoming', 'myborrows', 'mylistings'].includes(tabName) && !state.currentUser) {
    showToast('Please sign in to access this section', 'error');
    openAuthModal('login');
    return;
  }

  state.activeTab = tabName;

  const tabs = ['catalog', 'incoming', 'myborrows', 'mylistings'];
  tabs.forEach(t => {
    const btn = document.getElementById(`tab-btn-${t}`);
    const sec = document.getElementById(`view-${t}`);
    if (btn) btn.classList.toggle('active', t === tabName);
    if (sec) sec.style.display = t === tabName ? 'block' : 'none';
  });

  if (tabName === 'catalog') loadCatalog();
  else if (tabName === 'incoming') loadIncomingRequests();
  else if (tabName === 'myborrows') loadMyBorrows();
  else if (tabName === 'mylistings') loadMyListings();

  refreshIcons();
}

// --- Auth Modal & Logic ---
function openAuthModal(mode = 'login') {
  toggleAuthMode(mode);
  openModal('modal-auth');
}

function closeAuthModal() {
  closeModal('modal-auth');
}

function toggleAuthMode(mode) {
  const isLogin = mode === 'login';
  document.getElementById('auth-modal-title').textContent = isLogin ? 'Sign In to BorrowBox' : 'Create Student Account';
  document.getElementById('form-login').style.display = isLogin ? 'flex' : 'none';
  document.getElementById('form-register').style.display = isLogin ? 'none' : 'flex';
  refreshIcons();
}

function quickFillLogin(email, password) {
  document.getElementById('login-identifier').value = email;
  document.getElementById('login-password').value = password;
}

async function handleLoginSubmit(event) {
  event.preventDefault();
  const username = document.getElementById('login-identifier').value.trim();
  const password = document.getElementById('login-password').value;

  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || 'Invalid credentials or user not found', 'error');
      return;
    }

    state.currentUser = await res.json();
    renderNavbar();
    closeAuthModal();
    showToast(`Welcome back, ${state.currentUser.fullName}!`, 'success');
    updateBadges();
    if (state.activeTab === 'catalog') loadCatalog();
    else switchTab(state.activeTab);
  } catch (err) {
    showToast('Failed to sign in. Please try again.', 'error');
  }
}

async function handleRegisterSubmit(event) {
  event.preventDefault();
  const fullName = document.getElementById('reg-name').value.trim();
  const usn = document.getElementById('reg-usn').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const phone = document.getElementById('reg-phone').value.trim();
  const password = document.getElementById('reg-password').value;

  try {
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fullName, usn, email, phone, password })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || 'Registration failed. Check your details.', 'error');
      return;
    }

    showToast('Account created successfully! Logging you in...', 'success');
    document.getElementById('login-identifier').value = email;
    document.getElementById('login-password').value = password;
    await handleLoginSubmit(new Event('submit'));
  } catch (err) {
    showToast('Network error during registration', 'error');
  }
}

async function handleLogout() {
  try {
    await fetch('/api/auth/logout', { method: 'POST' });
  } catch (err) {}
  state.currentUser = null;
  renderNavbar();
  showToast('Logged out successfully', 'success');
  switchTab('catalog');
}

// --- Catalog Browsing, Search, and Filtering ---
function handleSearch() {
  clearTimeout(state.searchDebounceTimer);
  state.searchDebounceTimer = setTimeout(() => {
    state.searchKeyword = document.getElementById('search-keyword').value.trim();
    loadCatalog();
  }, 300);
}

function setCategoryFilter(category, buttonEl) {
  state.categoryFilter = category;
  const pills = document.querySelectorAll('#category-pills-list .pill');
  pills.forEach(p => p.classList.remove('active'));
  if (buttonEl) buttonEl.classList.add('active');
  loadCatalog();
}

function applyFilters() {
  state.conditionFilter = document.getElementById('filter-condition').value;
  state.statusFilter = document.getElementById('filter-status').value;
  loadCatalog();
}

async function loadCatalog() {
  const grid = document.getElementById('catalog-items-grid');
  if (!grid) return;

  grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 2rem; color: var(--muted-foreground);">Loading items...</div>`;

  const params = new URLSearchParams();
  if (state.searchKeyword) params.append('keyword', state.searchKeyword);
  if (state.categoryFilter) params.append('category', state.categoryFilter);
  if (state.conditionFilter) params.append('condition', state.conditionFilter);
  if (state.statusFilter) params.append('status', state.statusFilter);

  try {
    const res = await fetch(`/api/items?${params.toString()}`);
    if (!res.ok) throw new Error('Failed to load items');
    const items = await res.json();
    state.catalogItems = items;
    renderCatalogGrid(items);
  } catch (err) {
    grid.innerHTML = `
      <div class="empty-state" style="grid-column: 1/-1;">
        <div class="empty-state-icon"><i data-lucide="alert-triangle" class="icon-lg"></i></div>
        <div class="empty-state-title">Unable to load catalog</div>
        <p>${escapeHtml(err.message)}</p>
      </div>
    `;
    refreshIcons();
  }
}

function getCategoryIconName(category) {
  const map = {
    'TEXTBOOKS': 'book-open',
    'ELECTRONICS': 'cpu',
    'LAB_EQUIPMENT': 'flask-conical',
    'CHARGERS': 'zap',
    'SPORTS': 'trophy',
    'OTHER': 'package'
  };
  return map[category] || 'package';
}

function renderCatalogGrid(items) {
  const grid = document.getElementById('catalog-items-grid');
  if (!grid) return;

  if (items.length === 0) {
    grid.innerHTML = `
      <div class="empty-state" style="grid-column: 1/-1;">
        <div class="empty-state-icon"><i data-lucide="search-x" class="icon-lg"></i></div>
        <div class="empty-state-title">No items found</div>
        <p>Try adjusting your search keywords or removing filters.</p>
      </div>
    `;
    refreshIcons();
    return;
  }

  grid.innerHTML = items.map(item => {
    const isOwner = state.currentUser && state.currentUser.id === item.ownerId;
    const isAvailable = item.status === 'AVAILABLE';

    const statusBadgeClass = item.status === 'AVAILABLE' ? 'badge-available' :
      item.status === 'BORROWED' ? 'badge-borrowed' : 'badge-unavailable';

    const iconName = getCategoryIconName(item.category);
    const ratingVal = item.ownerAverageRating ? item.ownerAverageRating.toFixed(1) : '0.0';
    const ratingCount = item.ownerRatingCount || 0;

    return `
      <div class="card">
        <div class="card-image-box">
          ${item.imageUrl ? `
            <img class="card-image" src="${escapeHtml(item.imageUrl)}" alt="${escapeHtml(item.title)}" onerror="this.outerHTML='<div class=\\'card-image-fallback\\'><i data-lucide=\\'${iconName}\\' class=\\'icon-lg\\'></i></div>'; refreshIcons();">
          ` : `
            <div class="card-image-fallback"><i data-lucide="${iconName}" class="icon-lg"></i></div>
          `}
        </div>

        <div class="card-header">
          <div class="card-top-badges">
            <span class="badge badge-outline">${formatEnum(item.category)}</span>
            <span class="badge ${statusBadgeClass}">${formatEnum(item.status)}</span>
          </div>
          <h3 class="card-title" title="${escapeHtml(item.title)}">${escapeHtml(item.title)}</h3>
        </div>

        <div class="card-content">
          <p class="card-desc">${escapeHtml(item.description || 'No detailed description provided.')}</p>
          <div style="display: flex; gap: 0.5rem; align-items: center; font-size: 0.75rem;">
            <span style="color: var(--muted-foreground);">Condition:</span>
            <span class="badge badge-outline">${formatEnum(item.itemCondition)}</span>
          </div>
          <div class="card-owner-info">
            <span>Owner: <strong>${escapeHtml(item.ownerName || 'Student')}</strong></span>
            <span style="display: inline-flex; align-items: center; gap: 0.2rem;" title="Owner rating">
              <i data-lucide="star" class="icon-xs fill-amber"></i> ${ratingVal} (${ratingCount})
            </span>
          </div>
        </div>

        <div class="card-footer">
          <button class="btn ${isAvailable ? 'btn-primary' : 'btn-outline'} btn-sm" style="width: 100%;" onclick="openItemDetail(${item.id})">
            ${isOwner ? 'Manage My Item' : (isAvailable ? 'View & Borrow' : 'View Availability')}
          </button>
        </div>
      </div>
    `;
  }).join('');

  refreshIcons();
}

// --- Item Detail & Booking Modal ---
async function openItemDetail(itemId) {
  try {
    const [itemRes, requestsRes] = await Promise.all([
      fetch(`/api/items/${itemId}`),
      fetch(`/api/requests/item/${itemId}`)
    ]);

    if (!itemRes.ok) throw new Error('Could not fetch item');
    const item = await itemRes.json();
    const requests = requestsRes.ok ? await requestsRes.json() : [];

    document.getElementById('detail-title').textContent = item.title;
    document.getElementById('detail-desc').textContent = item.description || 'No description provided.';
    document.getElementById('borrow-item-id').value = item.id;

    // Badges
    const statusClass = item.status === 'AVAILABLE' ? 'badge-available' :
      item.status === 'BORROWED' ? 'badge-borrowed' : 'badge-unavailable';

    document.getElementById('detail-badges').innerHTML = `
      <span class="badge badge-outline">${formatEnum(item.category)}</span>
      <span class="badge badge-outline">${formatEnum(item.itemCondition)}</span>
      <span class="badge ${statusClass}">${formatEnum(item.status)}</span>
    `;

    // Image
    const imgEl = document.getElementById('detail-img');
    const imgBox = document.getElementById('detail-img-box');
    const iconName = getCategoryIconName(item.category);

    if (item.imageUrl) {
      imgBox.innerHTML = `<img id="detail-img" src="${escapeHtml(item.imageUrl)}" alt="${escapeHtml(item.title)}" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.outerHTML='<div class=\\'card-image-fallback\\' style=\\'height: 100%; display: flex; align-items: center; justify-content: center;\\'><i data-lucide=\\'${iconName}\\' class=\\'icon-lg\\'></i></div>'; refreshIcons();">`;
    } else {
      imgBox.innerHTML = `<div class="card-image-fallback" style="height: 100%; display: flex; align-items: center; justify-content: center;"><i data-lucide="${iconName}" class="icon-lg"></i></div>`;
    }

    // Owner info
    document.getElementById('detail-owner-name').textContent = `Owner: ${item.ownerName} (${item.ownerUsn})`;
    document.getElementById('detail-owner-reputation').innerHTML = `
      <i data-lucide="star" class="icon-xs fill-amber"></i> ${item.ownerAverageRating.toFixed(1)} rating (${item.ownerRatingCount} completed reviews)
    `;

    // Existing bookings
    const reservedSlotsBox = document.getElementById('detail-reserved-slots');
    const slotsList = document.getElementById('detail-slots-list');
    const activeBookings = requests.filter(r => ['APPROVED', 'ACTIVE', 'OVERDUE'].includes(r.status));

    if (activeBookings.length > 0) {
      reservedSlotsBox.style.display = 'block';
      slotsList.innerHTML = activeBookings.map(b => `
        <li>${b.startDate} to ${b.endDate} (${formatEnum(b.status)})</li>
      `).join('');
    } else {
      reservedSlotsBox.style.display = 'none';
      slotsList.innerHTML = '';
    }

    // Booking form setup
    const isOwner = state.currentUser && state.currentUser.id === item.ownerId;
    const submitBtn = document.getElementById('btn-submit-borrow');

    setMinDates();

    if (isOwner) {
      submitBtn.disabled = true;
      submitBtn.textContent = 'You own this item';
      submitBtn.className = 'btn btn-outline';
    } else if (item.status === 'UNAVAILABLE') {
      submitBtn.disabled = true;
      submitBtn.textContent = 'Item is marked Unavailable';
      submitBtn.className = 'btn btn-outline';
    } else {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Submit Borrow Request';
      submitBtn.className = 'btn btn-primary';
    }

    openModal('modal-detail');
    refreshIcons();
  } catch (err) {
    showToast('Failed to load item details: ' + err.message, 'error');
  }
}

async function handleBorrowSubmit(event) {
  event.preventDefault();
  if (!state.currentUser) {
    closeModal('modal-detail');
    showToast('Please sign in to submit a borrow request', 'error');
    openAuthModal('login');
    return;
  }

  const itemId = document.getElementById('borrow-item-id').value;
  const startDate = document.getElementById('borrow-start-date').value;
  const endDate = document.getElementById('borrow-end-date').value;

  if (!startDate || !endDate) {
    showToast('Please select both start and return dates', 'error');
    return;
  }

  try {
    const res = await fetch('/api/requests', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ itemId: parseInt(itemId), startDate, endDate })
    });

    const data = await res.json().catch(() => ({}));

    if (!res.ok) {
      showToast(data.message || 'Borrow request failed. Dates may conflict.', 'error');
      return;
    }

    showToast('Borrow request submitted successfully! Awaiting owner approval.', 'success');
    closeModal('modal-detail');
    updateBadges();
    switchTab('myborrows');
  } catch (err) {
    showToast('Failed to submit borrow request', 'error');
  }
}

// --- Post & Edit Item ---
function openPostItemModal() {
  if (!state.currentUser) {
    showToast('Please sign in to post an item', 'error');
    openAuthModal('login');
    return;
  }

  document.getElementById('item-modal-title').textContent = 'List an Item for Borrowing';
  document.getElementById('item-edit-id').value = '';
  document.getElementById('item-form-title').value = '';
  document.getElementById('item-form-category').value = 'TEXTBOOKS';
  document.getElementById('item-form-condition').value = 'GOOD';
  document.getElementById('item-form-description').value = '';
  document.getElementById('item-form-image').value = '';
  document.getElementById('item-form-submit-btn').textContent = 'Publish Listing';

  openModal('modal-item');
}

async function openEditItemModal(itemId) {
  try {
    const res = await fetch(`/api/items/${itemId}`);
    if (!res.ok) throw new Error('Item not found');
    const item = await res.json();

    document.getElementById('item-modal-title').textContent = 'Edit Item Listing';
    document.getElementById('item-edit-id').value = item.id;
    document.getElementById('item-form-title').value = item.title;
    document.getElementById('item-form-category').value = item.category;
    document.getElementById('item-form-condition').value = item.itemCondition;
    document.getElementById('item-form-description').value = item.description || '';
    document.getElementById('item-form-image').value = item.imageUrl || '';
    document.getElementById('item-form-submit-btn').textContent = 'Save Changes';

    openModal('modal-item');
  } catch (err) {
    showToast('Unable to edit item: ' + err.message, 'error');
  }
}

async function handleItemFormSubmit(event) {
  event.preventDefault();
  const editId = document.getElementById('item-edit-id').value;
  const title = document.getElementById('item-form-title').value.trim();
  const category = document.getElementById('item-form-category').value;
  const itemCondition = document.getElementById('item-form-condition').value;
  const description = document.getElementById('item-form-description').value.trim();
  const imageUrl = document.getElementById('item-form-image').value.trim();

  const payload = { title, category, itemCondition, description, imageUrl };

  try {
    const url = editId ? `/api/items/${editId}` : '/api/items';
    const method = editId ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      showToast(data.message || 'Operation failed', 'error');
      return;
    }

    showToast(editId ? 'Item updated successfully!' : 'Item listed for borrowing!', 'success');
    closeModal('modal-item');
    if (state.activeTab === 'mylistings') loadMyListings();
    else loadCatalog();
  } catch (err) {
    showToast('Failed to save item', 'error');
  }
}

async function toggleItemAvailability(itemId, currentStatus) {
  const newStatus = currentStatus === 'AVAILABLE' ? 'UNAVAILABLE' : 'AVAILABLE';
  try {
    const res = await fetch(`/api/items/${itemId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || 'Could not update status', 'error');
      return;
    }

    showToast(`Item marked as ${newStatus}`, 'success');
    loadMyListings();
  } catch (err) {
    showToast('Failed to toggle status', 'error');
  }
}

async function deleteItem(itemId) {
  if (!confirm('Are you sure you want to delete this listing?')) return;
  try {
    const res = await fetch(`/api/items/${itemId}`, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || 'Cannot delete item with active or approved borrows', 'error');
      return;
    }

    showToast('Item deleted successfully', 'success');
    loadMyListings();
  } catch (err) {
    showToast('Failed to delete item', 'error');
  }
}

// --- My Listings View ---
async function loadMyListings() {
  const tbody = document.getElementById('my-listings-tbody');
  if (!tbody) return;

  tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--muted-foreground);">Loading your items...</td></tr>`;

  try {
    const res = await fetch('/api/items/my');
    if (!res.ok) throw new Error('Could not fetch listings');
    const items = await res.json();

    if (items.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="6">
            <div class="empty-state">
              <div class="empty-state-icon"><i data-lucide="package" class="icon-lg"></i></div>
              <div class="empty-state-title">No items listed yet</div>
              <p>Have an extra calculator, textbook, or charger? Post it to help college peers.</p>
              <button class="btn btn-primary btn-sm" style="margin-top: 0.75rem;" onclick="openPostItemModal()">
                <i data-lucide="plus" class="icon-xs"></i> Post Your First Item
              </button>
            </div>
          </td>
        </tr>
      `;
      refreshIcons();
      return;
    }

    tbody.innerHTML = items.map(item => {
      const statusClass = item.status === 'AVAILABLE' ? 'badge-available' :
        item.status === 'BORROWED' ? 'badge-borrowed' : 'badge-unavailable';

      const toggleAction = item.status === 'AVAILABLE' ? 'Mark Unavailable' : 'Mark Available';

      return `
        <tr>
          <td>
            <div style="font-weight: 600; color: var(--foreground);">${escapeHtml(item.title)}</div>
            <div style="font-size: 0.75rem; color: var(--muted-foreground);">${escapeHtml(item.description ? item.description.substring(0, 60) + '...' : '')}</div>
          </td>
          <td><span class="badge badge-outline">${formatEnum(item.category)}</span></td>
          <td><span class="badge badge-outline">${formatEnum(item.itemCondition)}</span></td>
          <td><span class="badge ${statusClass}">${formatEnum(item.status)}</span></td>
          <td style="font-size: 0.75rem; color: var(--muted-foreground);">${item.createdAt ? item.createdAt.substring(0, 10) : ''}</td>
          <td style="text-align: right; white-space: nowrap;">
            ${item.status !== 'BORROWED' ? `
              <button class="btn btn-outline btn-sm" onclick="toggleItemAvailability(${item.id}, '${item.status}')">${toggleAction}</button>
            ` : ''}
            <button class="btn btn-outline btn-sm" onclick="openEditItemModal(${item.id})">Edit</button>
            <button class="btn btn-destructive btn-sm" onclick="deleteItem(${item.id})">Delete</button>
          </td>
        </tr>
      `;
    }).join('');

    refreshIcons();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--destructive);">${escapeHtml(err.message)}</td></tr>`;
  }
}

// --- Incoming Requests View (For Item Owners) ---
async function loadIncomingRequests() {
  const tbody = document.getElementById('incoming-requests-tbody');
  if (!tbody) return;

  tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--muted-foreground);">Loading incoming requests...</td></tr>`;

  try {
    const res = await fetch('/api/requests/incoming');
    if (!res.ok) throw new Error('Failed to load incoming requests');
    const requests = await res.json();
    updateBadges();

    if (requests.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="5">
            <div class="empty-state">
              <div class="empty-state-icon"><i data-lucide="inbox" class="icon-lg"></i></div>
              <div class="empty-state-title">No incoming requests</div>
              <p>When students submit borrow requests for your items, they will appear here.</p>
            </div>
          </td>
        </tr>
      `;
      refreshIcons();
      return;
    }

    tbody.innerHTML = requests.map(req => {
      const badgeClass = getRequestBadgeClass(req.status, req.overdue);

      return `
        <tr>
          <td>
            <div style="font-weight: 600;">${escapeHtml(req.itemTitle)}</div>
            <span class="badge badge-outline" style="font-size: 0.7rem;">${formatEnum(req.itemCategory)}</span>
          </td>
          <td>
            <div style="font-weight: 600;">${escapeHtml(req.borrowerName)}</div>
            <div style="font-size: 0.75rem; color: var(--muted-foreground); display: flex; align-items: center; gap: 0.25rem;">
              <span>USN: ${escapeHtml(req.borrowerUsn)}</span> |
              <i data-lucide="phone" class="icon-xs"></i> <span>${escapeHtml(req.borrowerPhone || 'N/A')}</span>
            </div>
          </td>
          <td>
            <div style="font-weight: 500;">${req.startDate} to ${req.endDate}</div>
            <div style="font-size: 0.75rem; color: var(--muted-foreground);">${calculateDays(req.startDate, req.endDate)} days</div>
          </td>
          <td>
            <span class="badge ${badgeClass}">${req.overdue ? 'OVERDUE' : formatEnum(req.status)}</span>
          </td>
          <td style="text-align: right; white-space: nowrap;">
            ${renderOwnerActions(req)}
          </td>
        </tr>
      `;
    }).join('');

    refreshIcons();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--destructive);">${escapeHtml(err.message)}</td></tr>`;
  }
}

function renderOwnerActions(req) {
  if (req.status === 'PENDING') {
    return `
      <button class="btn btn-primary btn-sm" onclick="updateRequestStatus(${req.id}, 'APPROVE')">Approve</button>
      <button class="btn btn-outline btn-sm" onclick="updateRequestStatus(${req.id}, 'REJECT')">Reject</button>
    `;
  } else if (req.status === 'APPROVED') {
    return `
      <button class="btn btn-primary btn-sm" onclick="updateRequestStatus(${req.id}, 'HANDOVER')">Mark Handed Over</button>
      <button class="btn btn-outline btn-sm" onclick="updateRequestStatus(${req.id}, 'REJECT')">Cancel</button>
    `;
  } else if (req.status === 'ACTIVE' || req.status === 'OVERDUE') {
    return `
      <button class="btn btn-primary btn-sm" onclick="updateRequestStatus(${req.id}, 'RETURN')">Confirm Returned</button>
    `;
  } else if (req.status === 'RETURNED') {
    if (!req.reviewedByMe) {
      return `
        <button class="btn btn-outline btn-sm" onclick="openRatingModal(${req.id}, '${escapeHtml(req.borrowerName)}')">
          <i data-lucide="star" class="icon-xs"></i> Rate Borrower
        </button>
      `;
    } else {
      return `
        <span style="font-size: 0.75rem; color: #15803d; display: inline-flex; align-items: center; gap: 0.2rem;">
          <i data-lucide="check" class="icon-xs"></i> Rated
        </span>
      `;
    }
  }
  return `<span style="font-size: 0.75rem; color: var(--muted-foreground);">-</span>`;
}

// --- My Borrows View (For Borrowers) ---
async function loadMyBorrows() {
  const tbody = document.getElementById('my-borrows-tbody');
  const overdueBannerContainer = document.getElementById('overdue-banner-container');
  if (!tbody) return;

  tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--muted-foreground);">Loading your borrow history...</td></tr>`;

  try {
    const res = await fetch('/api/requests/my-borrows');
    if (!res.ok) throw new Error('Failed to load borrows');
    const borrows = await res.json();
    updateBadges();

    // Check for overdue items
    const overdues = borrows.filter(b => b.overdue || b.status === 'OVERDUE');
    if (overdueBannerContainer) {
      if (overdues.length > 0) {
        overdueBannerContainer.innerHTML = `
          <div class="overdue-alert">
            <i data-lucide="alert-circle" class="icon-sm"></i>
            <div>
              <strong>Action Required:</strong> You have <strong>${overdues.length}</strong> overdue item(s).
              Please return them to the owner promptly to preserve your community trust rating!
            </div>
          </div>
        `;
      } else {
        overdueBannerContainer.innerHTML = '';
      }
    }

    if (borrows.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="5">
            <div class="empty-state">
              <div class="empty-state-icon"><i data-lucide="clock" class="icon-lg"></i></div>
              <div class="empty-state-title">No borrow requests yet</div>
              <p>Explore the catalog to borrow textbooks, calculators, and lab tools from peers.</p>
              <button class="btn btn-primary btn-sm" style="margin-top: 0.75rem;" onclick="switchTab('catalog')">
                <i data-lucide="search" class="icon-xs"></i> Browse Items
              </button>
            </div>
          </td>
        </tr>
      `;
      refreshIcons();
      return;
    }

    tbody.innerHTML = borrows.map(req => {
      const badgeClass = getRequestBadgeClass(req.status, req.overdue);

      return `
        <tr>
          <td>
            <div style="font-weight: 600;">${escapeHtml(req.itemTitle)}</div>
            <span class="badge badge-outline" style="font-size: 0.7rem;">${formatEnum(req.itemCategory)}</span>
          </td>
          <td>
            <div style="font-weight: 600;">${escapeHtml(req.ownerName)}</div>
            <div style="font-size: 0.75rem; color: var(--muted-foreground); display: flex; align-items: center; gap: 0.25rem;">
              <i data-lucide="phone" class="icon-xs"></i>
              <span>${escapeHtml(req.ownerPhone || req.ownerEmail || 'Campus Contact')}</span>
            </div>
          </td>
          <td>
            <div style="font-weight: 500;">${req.startDate} to ${req.endDate}</div>
            <div style="font-size: 0.75rem; color: var(--muted-foreground);">${calculateDays(req.startDate, req.endDate)} days</div>
          </td>
          <td>
            <span class="badge ${badgeClass}">${req.overdue ? 'OVERDUE' : formatEnum(req.status)}</span>
          </td>
          <td style="text-align: right; white-space: nowrap;">
            ${renderBorrowerActions(req)}
          </td>
        </tr>
      `;
    }).join('');

    refreshIcons();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--destructive);">${escapeHtml(err.message)}</td></tr>`;
  }
}

function renderBorrowerActions(req) {
  if (req.status === 'PENDING') {
    return `
      <button class="btn btn-destructive btn-sm" onclick="updateRequestStatus(${req.id}, 'CANCEL')">Cancel</button>
    `;
  } else if (req.status === 'RETURNED') {
    if (!req.reviewedByMe) {
      return `
        <button class="btn btn-outline btn-sm" onclick="openRatingModal(${req.id}, '${escapeHtml(req.ownerName)}')">
          <i data-lucide="star" class="icon-xs"></i> Rate Lender
        </button>
      `;
    } else {
      return `
        <span style="font-size: 0.75rem; color: #15803d; display: inline-flex; align-items: center; gap: 0.2rem;">
          <i data-lucide="check" class="icon-xs"></i> Rated
        </span>
      `;
    }
  }
  return `<span style="font-size: 0.75rem; color: var(--muted-foreground);">-</span>`;
}

// --- Request Actions Execution ---
async function updateRequestStatus(requestId, action) {
  try {
    const res = await fetch(`/api/requests/${requestId}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action })
    });

    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      showToast(data.message || `Failed to perform ${action}`, 'error');
      return;
    }

    showToast(`Request updated: ${action}`, 'success');
    updateBadges();
    if (state.activeTab === 'incoming') loadIncomingRequests();
    else if (state.activeTab === 'myborrows') loadMyBorrows();
  } catch (err) {
    showToast('Failed to update request', 'error');
  }
}

// --- Rating System Modal & Logic ---
function openRatingModal(requestId, targetName) {
  document.getElementById('rating-request-id').value = requestId;
  document.getElementById('rating-target-desc').textContent = `Rate your experience with ${targetName}.`;
  document.getElementById('rating-comment').value = '';
  setStarScore(5);
  openModal('modal-rating');
  refreshIcons();
}

function setStarScore(score) {
  document.getElementById('rating-score-val').value = score;
  const buttons = document.querySelectorAll('#star-selector .star-btn');
  buttons.forEach((btn, index) => {
    btn.classList.toggle('active', index < score);
  });
}

async function handleRatingSubmit(event) {
  event.preventDefault();
  const borrowRequestId = parseInt(document.getElementById('rating-request-id').value);
  const score = parseInt(document.getElementById('rating-score-val').value);
  const comment = document.getElementById('rating-comment').value.trim();

  try {
    const res = await fetch('/api/ratings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ borrowRequestId, score, comment })
    });

    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      showToast(data.message || 'Could not submit rating', 'error');
      return;
    }

    showToast('Rating submitted! Thank you for building campus trust.', 'success');
    closeModal('modal-rating');
    await checkAuthState();
    if (state.activeTab === 'incoming') loadIncomingRequests();
    else if (state.activeTab === 'myborrows') loadMyBorrows();
  } catch (err) {
    showToast('Failed to submit rating', 'error');
  }
}

// --- Utility Functions ---
function getRequestBadgeClass(status, isOverdue) {
  if (isOverdue || status === 'OVERDUE') return 'badge-overdue';
  switch (status) {
    case 'PENDING': return 'badge-pending';
    case 'APPROVED': return 'badge-approved';
    case 'ACTIVE': return 'badge-active';
    case 'RETURNED': return 'badge-returned';
    case 'REJECTED': return 'badge-rejected';
    default: return 'badge-outline';
  }
}

function calculateDays(startDate, endDate) {
  if (!startDate || !endDate) return 1;
  const start = new Date(startDate);
  const end = new Date(endDate);
  const diffTime = Math.abs(end - start);
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
  return diffDays || 1;
}

function formatEnum(val) {
  if (!val) return '';
  return val.replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, c => c.toUpperCase());
}

function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.add('open');
  refreshIcons();
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.remove('open');
}

function showToast(message, type = 'default') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const iconName = type === 'error' ? 'alert-circle' : (type === 'success' ? 'check-circle-2' : 'info');

  const toast = document.createElement('div');
  toast.className = `toast ${type === 'error' ? 'toast-error' : (type === 'success' ? 'toast-success' : '')}`;
  toast.innerHTML = `
    <i data-lucide="${iconName}" class="icon-sm"></i>
    <span style="flex: 1;">${escapeHtml(message)}</span>
  `;

  container.appendChild(toast);
  refreshIcons();

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(1rem)';
    toast.style.transition = 'all 0.2s ease';
    setTimeout(() => toast.remove(), 200);
  }, 3500);
}
