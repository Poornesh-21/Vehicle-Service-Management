document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
    setupEventListeners();
    loadInventoryItems();
});

let inventoryItems = [];
let currentPage = 1;
const itemsPerPage = 8;
let currentCategory = 'all';

function initializeApp() {
    setupMobileMenu();
    setupLogout();
    setupAuthentication();
    setupNavigation();
}

function setupMobileMenu() {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', function() {
            document.getElementById('sidebar').classList.toggle('active');
        });
    }
}

function setupLogout() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function() {
            if (confirm('Are you sure you want to logout?')) {
                localStorage.removeItem("jwt-token");
                sessionStorage.removeItem("jwt-token");
                localStorage.removeItem("user-role");
                localStorage.removeItem("user-name");
                sessionStorage.removeItem("user-role");
                sessionStorage.removeItem("user-name");
                window.location.href = '/admin/logout';
            }
        });
    }
}

function setupAuthentication() {
    const token = getToken();
    if (!token) {
        window.location.href = '/admin/login?error=session_expired';
        return;
    }
}

function setupNavigation() {
    const currentPath = window.location.pathname;
    document.querySelectorAll('.sidebar-menu-link').forEach(link => {
        const href = link.getAttribute('href');
        const baseHref = href ? href.split('?')[0] : '';

        if (baseHref && currentPath.includes(baseHref)) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });
}

function setupEventListeners() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keyup', function() {
            filterInventoryItems(this.value);
        });
    }

    const tableSearchInput = document.querySelector('.search-input-sm');
    if (tableSearchInput) {
        tableSearchInput.addEventListener('keyup', function() {
            filterGridItems(this.value);
        });
    }

    document.querySelectorAll('#categoryTabs .category-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            document.querySelectorAll('#categoryTabs .category-tab').forEach(t => {
                t.classList.remove('active');
            });

            this.classList.add('active');

            currentCategory = this.getAttribute('data-category');
            document.getElementById('gridTitle').textContent = this.textContent + ' Inventory';

            currentPage = 1;

            renderInventoryItems();
        });
    });

    const prevBtn = document.getElementById('prevBtn');
    if (prevBtn) {
        prevBtn.addEventListener('click', function(e) {
            e.preventDefault();
            if (currentPage > 1) {
                changePage(currentPage - 1);
            }
        });
    }

    const nextBtn = document.getElementById('nextBtn');
    if (nextBtn) {
        nextBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const totalPages = Math.ceil(getFilteredItems().length / itemsPerPage);
            if (currentPage < totalPages) {
                changePage(currentPage + 1);
            }
        });
    }

    const saveInventoryBtn = document.getElementById('saveInventoryBtn');
    if (saveInventoryBtn) {
        saveInventoryBtn.addEventListener('click', saveInventoryItem);
    }

    const updateInventoryBtn = document.getElementById('updateInventoryBtn');
    if (updateInventoryBtn) {
        updateInventoryBtn.addEventListener('click', updateInventoryItem);
    }

    const editInventoryBtn = document.getElementById('editInventoryBtn');
    if (editInventoryBtn) {
        editInventoryBtn.addEventListener('click', function() {
            const viewModal = bootstrap.Modal.getInstance(document.getElementById('viewInventoryModal'));
            if (viewModal) {
                viewModal.hide();
            }

            const itemId = document.getElementById('viewItemId').textContent.replace('ITEM-', '');
            showEditModal(itemId);
        });
    }

    const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', performDeleteItem);
    }

    setupFormValidation('addInventoryForm', {
        'itemName': 'itemNameError',
        'itemCategory': 'itemCategoryError',
        'currentStock': 'currentStockError',
        'unitPrice': 'unitPriceError',
        'reorderLevel': 'reorderLevelError'
    });

    setupFormValidation('editInventoryForm', {
        'editItemName': 'editItemNameError',
        'editItemCategory': 'editItemCategoryError',
        'editCurrentStock': 'editCurrentStockError',
        'editUnitPrice': 'editUnitPriceError',
        'editReorderLevel': 'editReorderLevelError'
    });
}

function setupFormValidation(formId, fieldMap) {
    const form = document.getElementById(formId);
    if (!form) return;

    for (const fieldId in fieldMap) {
        const field = document.getElementById(fieldId);
        const errorId = fieldMap[fieldId];

        if (field) {
            field.addEventListener('blur', function() {
                let errorMessage = '';
                if (!this.value) {
                    errorMessage = 'This field is required';
                } else if (this.type === 'number' && parseFloat(this.value) < 0) {
                    errorMessage = 'Value must be a positive number';
                }

                validateField(this, document.getElementById(errorId), errorMessage);
            });

            if (field.tagName === 'SELECT') {
                field.addEventListener('change', function() {
                    validateField(this, document.getElementById(errorId),
                        this.value ? '' : 'Please select an option');
                });
            }
        }
    }

    const modalId = formId === 'addInventoryForm' ? 'addInventoryModal' : 'editInventoryModal';
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.addEventListener('hidden.bs.modal', function() {
            form.reset();

            for (const fieldId in fieldMap) {
                const field = document.getElementById(fieldId);
                if (field) {
                    field.classList.remove('is-valid', 'is-invalid');
                }

                const errorElement = document.getElementById(fieldMap[fieldId]);
                if (errorElement) {
                    errorElement.textContent = '';
                }
            }

            const formErrorMessage = document.getElementById(formId === 'addInventoryForm' ? 'formErrorMessage' : 'editFormErrorMessage');
            if (formErrorMessage) {
                formErrorMessage.style.display = 'none';
            }
        });
    }
}

function getToken() {
    return localStorage.getItem('jwt-token') || sessionStorage.getItem('jwt-token');
}

function showSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
        spinnerOverlay.classList.add('show');
    }
}

function hideSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
        spinnerOverlay.classList.remove('show');
    }
}

function showSuccessModal(title, message) {
    document.getElementById('successTitle').textContent = title;
    document.getElementById('successMessage').textContent = message;
    const successModal = new bootstrap.Modal(document.getElementById('successModal'));
    successModal.show();
}

function loadInventoryItems() {
    showSpinner();

    fetch('/admin/inventory/api/items', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        }
    })
        .then(response => {
            if (!response.ok) {
                if (response.status === 401) {
                    window.location.href = '/admin/login?error=session_expired';
                    throw new Error('Session expired');
                }
                throw new Error(`Server responded with status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();

            document.getElementById('loading-grid').style.display = 'none';

            inventoryItems = data;

            if (inventoryItems.length > 0) {
                document.getElementById('inventoryGrid').style.display = 'grid';
                document.getElementById('pagination-container').style.display = 'flex';

                renderInventoryItems();
                setupPagination();
                updateStats();
            } else {
                document.getElementById('empty-grid').style.display = 'flex';
            }
        })
        .catch(error => {
            hideSpinner();

            document.getElementById('loading-grid').style.display = 'none';

            if (error.message !== 'Session expired') {
                const emptyGrid = document.getElementById('empty-grid');
                if (emptyGrid) {
                    emptyGrid.style.display = 'flex';
                    emptyGrid.querySelector('.empty-title').textContent = 'Failed to load inventory items';
                    emptyGrid.querySelector('.empty-message').textContent = 'Please try refreshing the page';
                }
            }
        });
}

function updateStats() {
    fetch('/admin/inventory/api/stats', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server responded with status: ${response.status}`);
            }
            return response.json();
        })
        .then(stats => {
            document.getElementById('totalItems').textContent = stats.totalItems;
            document.getElementById('lowStockItems').textContent = stats.lowStockItems;
        })
        .catch(error => {
            document.getElementById('totalItems').textContent = inventoryItems.length;
            const lowStockItems = inventoryItems.filter(item =>
                parseFloat(item.currentStock) <= parseFloat(item.reorderLevel)
            ).length;
            document.getElementById('lowStockItems').textContent = lowStockItems;
        });
}

function renderInventoryItems() {
    const gridContainer = document.getElementById('inventoryGrid');
    if (!gridContainer) return;

    gridContainer.innerHTML = '';

    const filteredItems = getFilteredItems();

    if (filteredItems.length === 0) {
        document.getElementById('empty-grid').style.display = 'flex';
        gridContainer.style.display = 'none';
        document.getElementById('pagination-container').style.display = 'none';
        return;
    }

    document.getElementById('empty-grid').style.display = 'none';
    gridContainer.style.display = 'grid';
    document.getElementById('pagination-container').style.display = 'flex';

    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, filteredItems.length);
    const displayedItems = filteredItems.slice(startIndex, endIndex);

    displayedItems.forEach(item => {
        let stockStatus, stockClass, stockIcon;
        if (item.stockStatus === "Low") {
            stockStatus = "Low";
            stockClass = "stock-low";
            stockIcon = "exclamation-circle";
        } else if (item.stockStatus === "Medium") {
            stockStatus = "Medium";
            stockClass = "stock-medium";
            stockIcon = "exclamation-triangle";
        } else {
            stockStatus = "Good";
            stockClass = "stock-good";
            stockIcon = "check-circle";
        }

        let categoryIcon;
        if (item.category === "Spare Parts") {
            categoryIcon = "fa-cogs";
        } else if (item.category === "Fluids & Lubricants") {
            categoryIcon = "fa-oil-can";
        } else {
            categoryIcon = "fa-tools";
        }

        const cardDiv = document.createElement('div');
        cardDiv.className = 'inventory-card';
        cardDiv.innerHTML = `
                <div class="card-header">
                    <span class="item-id">ITEM-${item.itemId}</span>
                    <span class="stock-badge ${stockClass}">
                        <i class="fas fa-${stockIcon}"></i> ${stockStatus}
                    </span>
                </div>
                <div class="card-body">
                    <h4 class="item-name">${item.name}</h4>
                    <div class="item-category">
                        <i class="fas ${categoryIcon}"></i> ${item.category}
                    </div>
                    <div class="item-details">
                        <div class="detail-group">
                            <div class="detail-label">Current Stock</div>
                            <div class="detail-value">${parseFloat(item.currentStock).toFixed(2)}</div>
                        </div>
                        <div class="detail-group">
                            <div class="detail-label">Reorder Level</div>
                            <div class="detail-value">${parseFloat(item.reorderLevel).toFixed(2)}</div>
                        </div>
                        <div class="detail-group">
                            <div class="detail-label">Unit Price</div>
                            <div class="detail-value price-value">₹${parseFloat(item.unitPrice).toFixed(2)}</div>
                        </div>
                        <div class="detail-group">
                            <div class="detail-label">Total Value</div>
                            <div class="detail-value">₹${parseFloat(item.totalValue).toFixed(2)}</div>
                        </div>
                    </div>
                </div>
                <div class="card-footer">
                    <span class="stock-indicator"></span>
                    <div class="card-actions">
                        <button class="btn-card-action view-btn" title="View Details" onclick="showInventoryDetails(${item.itemId})">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn-card-action edit-btn" title="Edit Item" onclick="showEditModal(${item.itemId})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn-card-action delete-btn" title="Delete Item" onclick="deleteInventoryItem(${item.itemId})">
                            <i class="fas fa-trash-alt"></i>
                        </button>
                    </div>
                </div>
            `;

        gridContainer.appendChild(cardDiv);
    });

    setupPagination();
}

window.showEditModal = function(itemId) {
    showSpinner();

    const formErrorMessage = document.getElementById('editFormErrorMessage');
    if (formErrorMessage) {
        formErrorMessage.style.display = 'none';
    }

    fetch(`/admin/inventory/api/items/${itemId}`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server responded with status: ${response.status}`);
            }
            return response.json();
        })
        .then(item => {
            hideSpinner();

            document.getElementById('editItemId').value = item.itemId;
            document.getElementById('editItemName').value = item.name;
            document.getElementById('editItemCategory').value = item.category;
            document.getElementById('editCurrentStock').value = parseFloat(item.currentStock).toFixed(2);
            document.getElementById('editUnitPrice').value = parseFloat(item.unitPrice).toFixed(2);
            document.getElementById('editReorderLevel').value = parseFloat(item.reorderLevel).toFixed(2);

            const form = document.getElementById('editInventoryForm');
            if (form) {
                form.querySelectorAll('.is-valid, .is-invalid').forEach(el => {
                    el.classList.remove('is-valid', 'is-invalid');
                });
            }

            const modal = new bootstrap.Modal(document.getElementById('editInventoryModal'));
            modal.show();
        })
        .catch(error => {
            hideSpinner();

            if (formErrorMessage) {
                formErrorMessage.textContent = 'Failed to load item details for editing. Please try again.';
                formErrorMessage.style.display = 'block';

                const modal = new bootstrap.Modal(document.getElementById('editInventoryModal'));
                modal.show();
            }
        });
};

function showDeleteConfirmModal(itemId) {
    document.getElementById('deleteItemId').value = itemId;
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
    deleteModal.show();
}

function performDeleteItem() {
    const itemId = document.getElementById('deleteItemId').value;
    if (!itemId) return;

    const deleteModal = bootstrap.Modal.getInstance(document.getElementById('deleteConfirmModal'));
    deleteModal.hide();

    showSpinner();

    fetch(`/admin/inventory/${itemId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server responded with status: ${response.status}`);
            }

            hideSpinner();
            showSuccessModal('Item Deleted', 'The inventory item has been deleted successfully.');
            loadInventoryItems();
        })
        .catch(error => {
            hideSpinner();

            const successTitle = document.getElementById('successTitle');
            const successMessage = document.getElementById('successMessage');

            if (successTitle && successMessage) {
                successTitle.textContent = 'Error';
                successTitle.className = 'mb-2 text-danger';
                successMessage.textContent = 'Failed to delete inventory item. Please try again.';

                const successModal = new bootstrap.Modal(document.getElementById('successModal'));
                successModal.show();

                document.getElementById('successModal').addEventListener('hidden.bs.modal', function() {
                    successTitle.textContent = 'Success!';
                    successTitle.className = 'mb-2';
                }, { once: true });
            }
        });
}

window.deleteInventoryItem = function(itemId) {
    showDeleteConfirmModal(itemId);
};

function getFilteredItems() {
    if (currentCategory !== 'all') {
        const categoryMap = {
            'spare-parts': 'Spare Parts',
            'fluids': 'Fluids & Lubricants',
            'tools': 'Tools & Equipment'
        };

        return inventoryItems.filter(item =>
            item.category === categoryMap[currentCategory]
        );
    }

    return [...inventoryItems];
}

function filterInventoryItems(searchTerm) {
    if (!searchTerm) {
        renderInventoryItems();
        return;
    }

    searchTerm = searchTerm.toLowerCase();

    const filteredItems = getFilteredItems().filter(item => {
        return (
            `item-${item.itemId}`.toLowerCase().includes(searchTerm) ||
            item.name.toLowerCase().includes(searchTerm) ||
            item.category.toLowerCase().includes(searchTerm)
        );
    });

    const gridContainer = document.getElementById('inventoryGrid');

    if (filteredItems.length === 0) {
        document.getElementById('empty-grid').style.display = 'flex';
        document.getElementById('empty-grid').querySelector('.empty-title').textContent = 'No items found';
        document.getElementById('empty-grid').querySelector('.empty-message').textContent = 'Try a different search term';

        gridContainer.style.display = 'none';
        document.getElementById('pagination-container').style.display = 'none';
    } else {
        currentPage = 1;
        renderFilteredItems(filteredItems);
    }
}

function renderFilteredItems(filteredItems) {
    const gridContainer = document.getElementById('inventoryGrid');
    if (!gridContainer) return;

    gridContainer.innerHTML = '';

    document.getElementById('empty-grid').style.display = 'none';
    gridContainer.style.display = 'grid';
    document.getElementById('pagination-container').style.display = 'flex';

    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, filteredItems.length);
    const displayedItems = filteredItems.slice(startIndex, endIndex);

    displayedItems.forEach(item => {
        let stockStatus, stockClass, stockIcon;
        if (item.stockStatus === "Low") {
            stockStatus = "Low";
            stockClass = "stock-low";
            stockIcon = "exclamation-circle";
        } else if (item.stockStatus === "Medium") {
            stockStatus = "Medium";
            stockClass = "stock-medium";
            stockIcon = "exclamation-triangle";
        } else {
            stockStatus = "Good";
            stockClass = "stock-good";
            stockIcon = "check-circle";
        }

        let categoryIcon;
        if (item.category === "Spare Parts") {
            categoryIcon = "fa-cogs";
        } else if (item.category === "Fluids & Lubricants") {
            categoryIcon = "fa-oil-can";
        } else {
            categoryIcon = "fa-tools";
        }

        const cardDiv = document.createElement('div');
        cardDiv.className = 'inventory-card';
        cardDiv.innerHTML = `
                <div class="card-header">
                    <span class="item-id">ITEM-${item.itemId}</span>
                    <span class="stock-badge ${stockClass}">
                        <i class="fas fa-${stockIcon}"></i> ${stockStatus}
                    </span>
                </div>
                <div class="card-body">
                    <h4 class="item-name">${item.name}</h4>
                    <div class="item-category">
                        <i class="fas ${categoryIcon}"></i> ${item.category}
                    </div>
                    <div class="item-details">
                        <div class="detail-group">
                            <div class="detail-label">Current Stock</div>
                            <div class="detail-value">${parseFloat(item.currentStock).toFixed(2)}</div>
                        </div>
                        <div class="detail-group">
                            <div class="detail-label">Reorder Level</div>
                            <div class="detail-value">${parseFloat(item.reorderLevel).toFixed(2)}</div>
                        </div>
                        <div class="detail-group">
                            <div class="detail-label">Unit Price</div>
                            <div class="detail-value price-value">₹${parseFloat(item.unitPrice).toFixed(2)}</div>
                        </div>
                        <div class="detail-group">
                            <div class="detail-label">Total Value</div>
                            <div class="detail-value">₹${parseFloat(item.totalValue).toFixed(2)}</div>
                        </div>
                    </div>
                </div>
                <div class="card-footer">
                    <span class="stock-indicator"></span>
                    <div class="card-actions">
                        <button class="btn-card-action view-btn" title="View Details" onclick="showInventoryDetails(${item.itemId})">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn-card-action edit-btn" title="Edit Item" onclick="showEditModal(${item.itemId})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn-card-action delete-btn" title="Delete Item" onclick="deleteInventoryItem(${item.itemId})">
                            <i class="fas fa-trash-alt"></i>
                        </button>
                    </div>
                </div>
            `;

        gridContainer.appendChild(cardDiv);
    });

    setupPaginationForItems(filteredItems);
}

function filterGridItems(searchTerm) {
    if (!searchTerm) {
        document.querySelectorAll('.inventory-card').forEach(card => {
            card.style.display = '';
        });
        return;
    }

    searchTerm = searchTerm.toLowerCase();

    document.querySelectorAll('.inventory-card').forEach(card => {
        const text = card.textContent.toLowerCase();
        if (text.includes(searchTerm)) {
            card.style.display = '';
        } else {
            card.style.display = 'none';
        }
    });
}

function changePage(page) {
    currentPage = page;
    renderInventoryItems();
    updatePaginationUI();
}

function setupPagination() {
    const filteredItems = getFilteredItems();
    setupPaginationForItems(filteredItems);
}

function setupPaginationForItems(items) {
    const totalPages = Math.ceil(items.length / itemsPerPage);
    const pagination = document.getElementById('pagination');
    if (!pagination) return;

    const pageLinks = pagination.querySelectorAll('.page-link:not(#prevBtn):not(#nextBtn)');
    pageLinks.forEach(link => link.parentElement.remove());

    const prevBtn = document.getElementById('prevBtn');
    const prevBtnParent = prevBtn ? prevBtn.parentElement : null;
    const nextBtn = document.getElementById('nextBtn');
    const nextBtnParent = nextBtn ? nextBtn.parentElement : null;

    if (prevBtnParent && nextBtnParent) {
        for (let i = 1; i <= totalPages; i++) {
            const li = document.createElement('li');
            li.className = `page-item ${i === currentPage ? 'active' : ''}`;
            li.innerHTML = `<a class="page-link" href="#" data-page="${i}">${i}</a>`;

            li.querySelector('.page-link').addEventListener('click', function(e) {
                e.preventDefault();
                changePage(i);
            });

            pagination.insertBefore(li, nextBtnParent);
        }

        prevBtnParent.classList.toggle('disabled', currentPage === 1);
        nextBtnParent.classList.toggle('disabled', currentPage === totalPages || totalPages === 0);
    }
}

function updatePaginationUI() {
    const filteredItems = getFilteredItems();
    const totalPages = Math.ceil(filteredItems.length / itemsPerPage);
    const pagination = document.getElementById('pagination');
    if (!pagination) return;

    const pageItems = pagination.querySelectorAll('.page-item');
    pageItems.forEach(item => {
        const pageLink = item.querySelector('.page-link');
        if (pageLink && pageLink.id !== 'prevBtn' && pageLink.id !== 'nextBtn') {
            const page = parseInt(pageLink.getAttribute('data-page'));
            item.classList.toggle('active', page === currentPage);
        }
    });

    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');

    if (prevBtn) {
        prevBtn.parentElement.classList.toggle('disabled', currentPage === 1);
    }

    if (nextBtn) {
        nextBtn.parentElement.classList.toggle('disabled', currentPage === totalPages || totalPages === 0);
    }
}

function validateField(field, errorElement, errorMessage) {
    if (!field.checkValidity()) {
        field.classList.add('is-invalid');
        errorElement.textContent = errorMessage || field.validationMessage;
        return false;
    } else {
        field.classList.remove('is-invalid');
        field.classList.add('is-valid');
        errorElement.textContent = '';
        return true;
    }
}

function validateAddForm() {
    const itemName = document.getElementById('itemName');
    const itemCategory = document.getElementById('itemCategory');
    const currentStock = document.getElementById('currentStock');
    const unitPrice = document.getElementById('unitPrice');
    const reorderLevel = document.getElementById('reorderLevel');

    const nameValid = validateField(itemName, document.getElementById('itemNameError'),
        itemName.value ? '' : 'Item name is required');

    const categoryValid = validateField(itemCategory, document.getElementById('itemCategoryError'),
        itemCategory.value ? '' : 'Please select a category');

    const stockValid = validateField(currentStock, document.getElementById('currentStockError'),
        currentStock.value && parseFloat(currentStock.value) >= 0 ? '' : 'Current stock must be a positive number');

    const priceValid = validateField(unitPrice, document.getElementById('unitPriceError'),
        unitPrice.value && parseFloat(unitPrice.value) >= 0 ? '' : 'Unit price must be a positive number');

    const reorderValid = validateField(reorderLevel, document.getElementById('reorderLevelError'),
        reorderLevel.value && parseFloat(reorderLevel.value) >= 0 ? '' : 'Reorder level must be a positive number');

    return nameValid && categoryValid && stockValid && priceValid && reorderValid;
}

function saveInventoryItem() {
    const form = document.getElementById('addInventoryForm');
    if (!form) return;

    const formErrorMessage = document.getElementById('formErrorMessage');
    formErrorMessage.style.display = 'none';

    if (!validateAddForm()) {
        return;
    }

    const itemData = {
        name: document.getElementById('itemName').value,
        category: document.getElementById('itemCategory').value,
        currentStock: parseFloat(document.getElementById('currentStock').value),
        unitPrice: parseFloat(document.getElementById('unitPrice').value),
        reorderLevel: parseFloat(document.getElementById('reorderLevel').value)
    };

    showSpinner();

    fetch('/admin/inventory', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        },
        body: JSON.stringify(itemData)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server responded with status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();

            const modal = bootstrap.Modal.getInstance(document.getElementById('addInventoryModal'));
            if (modal) {
                modal.hide();
            }

            form.reset();

            form.querySelectorAll('.is-valid, .is-invalid').forEach(el => {
                el.classList.remove('is-valid', 'is-invalid');
            });

            showSuccessModal('Item Added', 'The inventory item has been added successfully.');

            loadInventoryItems();
        })
        .catch(error => {
            hideSpinner();

            formErrorMessage.textContent = 'Failed to create inventory item. Please try again.';
            formErrorMessage.style.display = 'block';
        });
}

function validateEditForm() {
    const itemName = document.getElementById('editItemName');
    const itemCategory = document.getElementById('editItemCategory');
    const currentStock = document.getElementById('editCurrentStock');
    const unitPrice = document.getElementById('editUnitPrice');
    const reorderLevel = document.getElementById('editReorderLevel');

    const nameValid = validateField(itemName, document.getElementById('editItemNameError'),
        itemName.value ? '' : 'Item name is required');

    const categoryValid = validateField(itemCategory, document.getElementById('editItemCategoryError'),
        itemCategory.value ? '' : 'Please select a category');

    const stockValid = validateField(currentStock, document.getElementById('editCurrentStockError'),
        currentStock.value && parseFloat(currentStock.value) >= 0 ? '' : 'Current stock must be a positive number');

    const priceValid = validateField(unitPrice, document.getElementById('editUnitPriceError'),
        unitPrice.value && parseFloat(unitPrice.value) >= 0 ? '' : 'Unit price must be a positive number');

    const reorderValid = validateField(reorderLevel, document.getElementById('editReorderLevelError'),
        reorderLevel.value && parseFloat(reorderLevel.value) >= 0 ? '' : 'Reorder level must be a positive number');

    return nameValid && categoryValid && stockValid && priceValid && reorderValid;
}

function updateInventoryItem() {
    const form = document.getElementById('editInventoryForm');
    if (!form) return;

    const formErrorMessage = document.getElementById('editFormErrorMessage');
    formErrorMessage.style.display = 'none';

    if (!validateEditForm()) {
        return;
    }

    const itemId = parseInt(document.getElementById('editItemId').value);
    const itemData = {
        name: document.getElementById('editItemName').value,
        category: document.getElementById('editItemCategory').value,
        currentStock: parseFloat(document.getElementById('editCurrentStock').value),
        unitPrice: parseFloat(document.getElementById('editUnitPrice').value),
        reorderLevel: parseFloat(document.getElementById('editReorderLevel').value)
    };

    showSpinner();

    fetch(`/admin/inventory/${itemId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        },
        body: JSON.stringify(itemData)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server responded with status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();

            const modal = bootstrap.Modal.getInstance(document.getElementById('editInventoryModal'));
            if (modal) {
                modal.hide();
            }

            form.querySelectorAll('.is-valid, .is-invalid').forEach(el => {
                el.classList.remove('is-valid', 'is-invalid');
            });

            showSuccessModal('Item Updated', 'The inventory item has been updated successfully.');

            loadInventoryItems();
        })
        .catch(error => {
            hideSpinner();

            formErrorMessage.textContent = 'Failed to update inventory item. Please try again.';
            formErrorMessage.style.display = 'block';
        });
}

window.showInventoryDetails = function(itemId) {
    showSpinner();

    fetch(`/admin/inventory/api/items/${itemId}`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server responded with status: ${response.status}`);
            }
            return response.json();
        })
        .then(item => {
            document.getElementById('viewItemId').textContent = `ITEM-${item.itemId}`;
            document.getElementById('viewItemName').textContent = item.name;
            document.getElementById('viewCategory').textContent = item.category;
            document.getElementById('viewCurrentStock').textContent = `${parseFloat(item.currentStock).toFixed(2)} units`;
            document.getElementById('viewReorderLevel').textContent = `${parseFloat(item.reorderLevel).toFixed(2)} units`;
            document.getElementById('viewUnitPrice').textContent = `₹${parseFloat(item.unitPrice).toFixed(2)}`;
            document.getElementById('viewTotalValue').textContent = `₹${item.totalValue.toFixed(2)}`;

            const viewStockStatus = document.getElementById('viewStockStatus');
            let stockClass, stockIcon;

            if (item.stockStatus === "Low") {
                stockClass = "stock-low";
                stockIcon = "exclamation-circle";
            } else if (item.stockStatus === "Medium") {
                stockClass = "stock-medium";
                stockIcon = "exclamation-triangle";
            } else {
                stockClass = "stock-good";
                stockIcon = "check-circle";
            }

            viewStockStatus.innerHTML = `
                <span class="stock-badge ${stockClass}">
                    <i class="fas fa-${stockIcon}"></i> ${item.stockStatus}
                </span>
            `;

            return fetch(`/admin/inventory/api/items/${itemId}/usage-history`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + getToken()
                }
            });
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server responded with status: ${response.status}`);
            }
            return response.json();
        })
        .then(usageData => {
            const usageTableBody = document.getElementById('usageTableBody');
            if (usageTableBody) {
                if (usageData && usageData.length > 0) {
                    let usageHtml = '';
                    usageData.forEach(usage => {
                        const date = new Date(usage.usedAt).toLocaleDateString('en-US', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric'
                        });

                        usageHtml += `
                            <tr>
                                <td>${usage.requestReference}</td>
                                <td>${date}</td>
                                <td>${parseFloat(usage.quantity).toFixed(2)}</td>
                                <td>${usage.serviceAdvisorName}</td>
                            </tr>
                        `;
                    });
                    usageTableBody.innerHTML = usageHtml;
                } else {
                    usageTableBody.innerHTML = `
                        <tr>
                            <td colspan="4" class="text-center py-3">
                                <p class="text-muted mb-0">No usage history available for this item</p>
                            </td>
                        </tr>
                    `;
                }
            }

            hideSpinner();

            const modal = new bootstrap.Modal(document.getElementById('viewInventoryModal'));
            modal.show();
        })
        .catch(error => {
            hideSpinner();
            alert('Failed to load item details. Please try again.');
        });
};