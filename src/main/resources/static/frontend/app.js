// ==================== CONFIG ====================
const API_BASE = 'http://localhost:8082/api';
let currentPage = 0;
let pageSize = 10;
let totalClients = 0;
let currentSort = { column: 'name', order: 'asc' };
let currentSearch = '';
let currentStatusFilter = '';
let selectedIds = new Set();
let undoTimeout = null;
let lastDeleted = null;

// DOM elements
let searchInput, addClientBtn, clientBody, modalBackdrop, modalTitle, modalClose, cancelBtn, saveBtn, formError, toast;
let fId, fName, fEmail, fPhone, fCompany, fAddress, fStatus;
let selectAllCheckbox, bulkBar, selectedCountSpan, statusFilter, applyFilterBtn, exportBtn, themeToggle;
let prevPageBtn, nextPageBtn, pageInfoSpan, pageSizeSelect;

// ==================== API HELPERS ====================
async function apiFetch(url, options = {}) {
    const res = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...options });
    if (options.method === 'DELETE' && res.status === 204) return { success: true };
    return res.json();
}

async function loadClients() {
    let url = `${API_BASE}/clients/paginated?page=${currentPage}&size=${pageSize}&sort=${currentSort.column}&order=${currentSort.order}`;
    if (currentSearch) url += `&q=${encodeURIComponent(currentSearch)}`;
    try {
        const data = await fetch(url).then(r => r.json());
        totalClients = data.total;
        renderTable(data.data);
        renderPagination();
        updateStats();
        updateRecentCount();
        document.getElementById('totalCount').textContent = totalClients;
    } catch (e) {
        showToast('Failed to load clients', 'error');
    }
}

async function updateStats() {
    try {
        const stats = await fetch(`${API_BASE}/stats`).then(r => r.json());
        document.getElementById('activeCount').textContent = stats.active || 0;
        document.getElementById('inactiveCount').textContent = stats.inactive || 0;
        document.getElementById('totalCount').textContent = stats.total || 0;
    } catch (e) {}
}

async function updateRecentCount() {
    try {
        const recent = await fetch(`${API_BASE}/clients/recent?limit=5`).then(r => r.json());
        document.getElementById('recentCount').textContent = recent.length;
    } catch (e) {}
}

async function saveClient() {
    const name = fName.value.trim();
    const email = fEmail.value.trim();
    if (!name) { showFieldError('nameError', 'Name is required'); return; }
    if (!email) { showFieldError('emailError', 'Email is required'); return; }
    if (!validateEmail(email)) { showFieldError('emailError', 'Invalid email format'); return; }
    hideFieldErrors();

    const payload = {
        name, email,
        phone: fPhone.value.trim(),
        company: fCompany.value.trim(),
        address: fAddress.value.trim(),
        status: fStatus.value
    };
    const editingId = fId.value;
    try {
        let data;
        if (editingId) {
            data = await apiFetch(`${API_BASE}/clients/${editingId}`, { method: 'PUT', body: JSON.stringify(payload) });
        } else {
            data = await apiFetch(`${API_BASE}/clients`, { method: 'POST', body: JSON.stringify(payload) });
        }
        if (data.success) {
            closeModal();
            showToast(data.message, 'success');
            loadClients();
        } else {
            showFormError(data.message);
        }
    } catch (e) { showFormError('Network error'); }
}

async function deleteClient(id) {
    if (confirm('Delete this client? This cannot be undone.')) {
        try {
            const data = await apiFetch(`${API_BASE}/clients/${id}`, { method: 'DELETE' });
            if (data.success) {
                showToast(data.message, 'success');
                loadClients();
            } else { showToast('Delete failed', 'error'); }
        } catch (e) { showToast('Network error', 'error'); }
    }
}

async function bulkDelete() {
    if (selectedIds.size === 0) return;
    if (confirm(`Delete ${selectedIds.size} client(s)?`)) {
        const ids = Array.from(selectedIds);
        const data = await apiFetch(`${API_BASE}/clients/bulk/delete`, { method: 'POST', body: JSON.stringify(ids) });
        if (data.success) {
            showToast(`Deleted ${ids.length} clients`, 'success');
            clearSelection();
            loadClients();
        } else { showToast('Bulk delete failed', 'error'); }
    }
}

async function bulkStatus(status) {
    if (selectedIds.size === 0) return;
    const ids = Array.from(selectedIds);
    const payload = { ids, status };
    const data = await apiFetch(`${API_BASE}/clients/bulk/status`, { method: 'PUT', body: JSON.stringify(payload) });
    if (data.success) {
        showToast(`Status updated to ${status}`, 'success');
        clearSelection();
        loadClients();
    } else { showToast('Update failed', 'error'); }
}

async function exportCSV() {
    window.open(`${API_BASE}/clients/export/csv`, '_blank');
}

// ==================== RENDER ====================
function renderTable(clients) {
    if (!clients.length) {
        clientBody.innerHTML = '<tr><td colspan="8" class="empty">No clients found.</td></tr>';
        return;
    }
    clientBody.innerHTML = clients.map(c => `
        <tr data-id="${c.id}">
            <td><input type="checkbox" class="row-checkbox" value="${c.id}" ${selectedIds.has(c.id) ? 'checked' : ''}></td>
            <td>${c.id}</td>
            <td><strong>${esc(c.name)}</strong></td>
            <td>${esc(c.email)}</td>
            <td>${esc(c.phone || '—')}</td>
            <td>${esc(c.company || '—')}</td>
            <td><span class="badge ${c.status}">${c.status}</span></td>
            <td>
                <div class="actions">
                    <button class="btn-icon edit" onclick="window.openEdit(${c.id})">✎ Edit</button>
                    <button class="btn-icon delete" onclick="window.deleteClient(${c.id})">✕ Delete</button>
                </div>
            </td>
        </tr>
    `).join('');
    attachCheckboxEvents();
    updateBulkBar();
}

function attachCheckboxEvents() {
    document.querySelectorAll('.row-checkbox').forEach(cb => {
        cb.addEventListener('change', (e) => {
            const id = parseInt(e.target.value);
            if (e.target.checked) selectedIds.add(id);
            else selectedIds.delete(id);
            updateBulkBar();
            updateSelectAll();
        });
    });
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', (e) => {
            document.querySelectorAll('.row-checkbox').forEach(cb => {
                cb.checked = e.target.checked;
                const id = parseInt(cb.value);
                if (e.target.checked) selectedIds.add(id);
                else selectedIds.delete(id);
            });
            updateBulkBar();
        });
    }
}

function updateBulkBar() {
    const count = selectedIds.size;
    if (count > 0) bulkBar.classList.remove('hidden');
    else bulkBar.classList.add('hidden');
    selectedCountSpan.textContent = count;
}

function updateSelectAll() {
    if (!selectAllCheckbox) return;
    const allCheckboxes = document.querySelectorAll('.row-checkbox');
    const allChecked = allCheckboxes.length > 0 && Array.from(allCheckboxes).every(cb => cb.checked);
    selectAllCheckbox.checked = allChecked;
}

function clearSelection() {
    selectedIds.clear();
    updateBulkBar();
    if (selectAllCheckbox) selectAllCheckbox.checked = false;
    loadClients(); // reload to refresh checkboxes
}

function renderPagination() {
    const totalPages = Math.ceil(totalClients / pageSize);
    pageInfoSpan.textContent = `Page ${currentPage+1} of ${totalPages || 1}`;
    prevPageBtn.disabled = currentPage === 0;
    nextPageBtn.disabled = currentPage >= totalPages - 1;
}

// ==================== MODAL / FORM ====================
function openAdd() {
    fId.value = '';
    modalTitle.textContent = 'Add Client';
    clearForm();
    openModal();
}

function openEdit(id) {
    const row = document.querySelector(`tr[data-id="${id}"]`);
    if (!row) return;
    fId.value = id;
    fName.value = row.cells[2].innerText;
    fEmail.value = row.cells[3].innerText;
    fPhone.value = row.cells[4].innerText !== '—' ? row.cells[4].innerText : '';
    fCompany.value = row.cells[5].innerText !== '—' ? row.cells[5].innerText : '';
    fAddress.value = '';
    const statusSpan = row.cells[6].querySelector('.badge');
    fStatus.value = statusSpan ? statusSpan.innerText : 'active';
    modalTitle.textContent = 'Edit Client';
    openModal();
}

function openModal() { modalBackdrop.classList.add('open'); }
function closeModal() { modalBackdrop.classList.remove('open'); clearForm(); }
function clearForm() {
    fName.value = fEmail.value = fPhone.value = fCompany.value = fAddress.value = '';
    fStatus.value = 'active';
    hideFormError();
    hideFieldErrors();
}
function validateEmail(email) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email); }
function showFieldError(id, msg) { const el = document.getElementById(id); if (el) { el.textContent = msg; el.style.display = 'block'; } }
function hideFieldErrors() { document.querySelectorAll('.field-error').forEach(el => el.style.display = 'none'); }
function showFormError(msg) { formError.textContent = msg; formError.classList.remove('hidden'); }
function hideFormError() { formError.classList.add('hidden'); }

// ==================== TOAST ====================
let toastTimer;
function showToast(msg, type = 'success') {
    toast.textContent = msg;
    toast.className = `toast ${type} show`;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.remove('show'), 3000);
}

function esc(str) { if (!str) return ''; return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }

// ==================== THEME ====================
function toggleTheme() {
    document.body.classList.toggle('light');
    const btn = document.getElementById('themeToggle');
    btn.textContent = document.body.classList.contains('light') ? '☀️' : '🌙';
}

// ==================== INIT ====================
document.addEventListener('DOMContentLoaded', () => {
    // Get elements
    searchInput = document.getElementById('searchInput');
    addClientBtn = document.getElementById('addClientBtn');
    clientBody = document.getElementById('clientBody');
    modalBackdrop = document.getElementById('modalBackdrop');
    modalTitle = document.getElementById('modalTitle');
    modalClose = document.getElementById('modalClose');
    cancelBtn = document.getElementById('cancelBtn');
    saveBtn = document.getElementById('saveBtn');
    formError = document.getElementById('formError');
    toast = document.getElementById('toast');
    fId = document.getElementById('clientId');
    fName = document.getElementById('clientName');
    fEmail = document.getElementById('clientEmail');
    fPhone = document.getElementById('clientPhone');
    fCompany = document.getElementById('clientCompany');
    fAddress = document.getElementById('clientAddress');
    fStatus = document.getElementById('clientStatus');
    selectAllCheckbox = document.getElementById('selectAll');
    bulkBar = document.getElementById('bulkBar');
    selectedCountSpan = document.getElementById('selectedCount');
    statusFilter = document.getElementById('statusFilter');
    applyFilterBtn = document.getElementById('applyFilterBtn');
    exportBtn = document.getElementById('exportBtn');
    themeToggle = document.getElementById('themeToggle');
    prevPageBtn = document.getElementById('prevPage');
    nextPageBtn = document.getElementById('nextPage');
    pageInfoSpan = document.getElementById('pageInfo');
    pageSizeSelect = document.getElementById('pageSize');

    // Events
    addClientBtn.addEventListener('click', openAdd);
    modalClose.addEventListener('click', closeModal);
    cancelBtn.addEventListener('click', closeModal);
    saveBtn.addEventListener('click', saveClient);
    modalBackdrop.addEventListener('click', e => { if (e.target === modalBackdrop) closeModal(); });
    if (exportBtn) exportBtn.addEventListener('click', exportCSV);
    if (themeToggle) themeToggle.addEventListener('click', toggleTheme);
    if (applyFilterBtn) applyFilterBtn.addEventListener('click', () => { currentStatusFilter = statusFilter.value; loadClients(); });
    if (prevPageBtn) prevPageBtn.addEventListener('click', () => { if (currentPage > 0) { currentPage--; loadClients(); } });
    if (nextPageBtn) nextPageBtn.addEventListener('click', () => { currentPage++; loadClients(); });
    if (pageSizeSelect) pageSizeSelect.addEventListener('change', (e) => { pageSize = parseInt(e.target.value); currentPage = 0; loadClients(); });
    if (document.getElementById('bulkDeleteBtn')) document.getElementById('bulkDeleteBtn').addEventListener('click', bulkDelete);
    if (document.getElementById('bulkActiveBtn')) document.getElementById('bulkActiveBtn').addEventListener('click', () => bulkStatus('active'));
    if (document.getElementById('bulkInactiveBtn')) document.getElementById('bulkInactiveBtn').addEventListener('click', () => bulkStatus('inactive'));
    if (document.getElementById('clearSelectionBtn')) document.getElementById('clearSelectionBtn').addEventListener('click', clearSelection);

    let searchTimer;

    searchInput.addEventListener('input', () => {
        clearTimeout(searchTimer);

        searchTimer = setTimeout(() => {
            currentPage = 0;
            loadClients();
        }, 300);
    });

      loadClients();

      })