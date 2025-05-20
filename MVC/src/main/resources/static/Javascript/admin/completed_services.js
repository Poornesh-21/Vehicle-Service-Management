let completedServices = [];
let currentServiceId = null;
let currentService = null;
document.addEventListener('DOMContentLoaded', function() {
    initializeEventListeners();
    loadCompletedServices();
});

function isMembershipPremium(service) {
    if (service.membershipStatus) {
        const status = String(service.membershipStatus).toLowerCase().trim();
        if (status.includes('premium')) {
            return true;
        }
    }
    if (service.customer && service.customer.membershipStatus) {
        const status = String(service.customer.membershipStatus).toLowerCase().trim();
        if (status.includes('premium')) {
            return true;
        }
    }
    if (service.isPremium || service.premium || service.isPremiumMember) {
        return true;
    }
    return false;
}

function getMembershipStatus(service) {
    const isPremium = isMembershipPremium(service);
    return isPremium ? 'Premium' : 'Standard';
}

function initializeEventListeners() {
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('view-service-btn') || e.target.closest('.view-service-btn')) {
            const btn = e.target.classList.contains('view-service-btn') ? e.target : e.target.closest('.view-service-btn');
            const serviceId = btn.getAttribute('data-id');
            viewServiceDetails(serviceId);
        }
    });
    document.getElementById('generateInvoiceBtn').addEventListener('click', function() {
        openGenerateInvoiceModal();
    });
    document.getElementById('confirmGenerateInvoiceBtn').addEventListener('click', function() {
        generateInvoice();
    });
    document.getElementById('confirmPaymentBtn').addEventListener('click', function() {
        processPayment();
    });
    document.getElementById('customerPickup').addEventListener('change', function() {
        if (this.checked) {
            document.getElementById('pickupFields').style.display = 'block';
            document.getElementById('deliveryFields').style.display = 'none';
            document.getElementById('confirmDeliveryBtnText').textContent = 'Confirm Pickup';
        }
    });
    document.getElementById('homeDelivery').addEventListener('change', function() {
        if (this.checked) {
            document.getElementById('pickupFields').style.display = 'none';
            document.getElementById('deliveryFields').style.display = 'block';
            document.getElementById('confirmDeliveryBtnText').textContent = 'Confirm Delivery';
        }
    });
    document.getElementById('confirmDeliveryBtn').addEventListener('click', function() {
        processDelivery();
    });
}

function normalizeServiceData(service) {
    if (!service) return;
    const isPremium = isMembershipPremium(service);
    service.membershipStatus = isPremium ? 'Premium' : 'Standard';

    if (!service.customerName || service.customerName === 'Unknown Customer') {
        if (service.customer) {
            if (typeof service.customer === 'object') {
                if (service.customer.firstName && service.customer.lastName) {
                    service.customerName = `${service.customer.firstName} ${service.customer.lastName}`;
                }
                else if (service.customer.user && service.customer.user.firstName && service.customer.user.lastName) {
                    service.customerName = `${service.customer.user.firstName} ${service.customer.user.lastName}`;
                }
                else if (service.customer.name) {
                    service.customerName = service.customer.name;
                }
            }
        }
        else if (service.firstName && service.lastName) {
            service.customerName = `${service.firstName} ${service.lastName}`;
        }
        else if (service.user && service.user.firstName && service.user.lastName) {
            service.customerName = `${service.user.firstName} ${service.user.lastName}`;
        }
    }

    if (!service.registrationNumber) {
        if (service.vehicleRegistration) {
            service.registrationNumber = service.vehicleRegistration;
        } else if (service.vehicle && service.vehicle.registrationNumber) {
            service.registrationNumber = service.vehicle.registrationNumber;
        }
    }
}

function loadCompletedServices() {
    const tableBody = document.getElementById('completedServicesTableBody');
    tableBody.innerHTML = `
        <tr>
            <td colspan="10" class="text-center py-4">
                <div class="spinner-border text-wine" role="status"></div>
                <p class="mt-2">Loading completed services...</p>
            </td>
        </tr>
    `;
    const token = getAuthToken();
    completedServices = [];
    fetch('/admin/api/completed-services', {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to fetch completed services: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (!Array.isArray(data)) {
                throw new Error('Invalid data format received from API');
            }
            completedServices = data;

            return Promise.all(completedServices.map(service => {
                normalizeServiceData(service);
                return enhanceCustomerInfo(service);
            }));
        })
        .then(() => {
            renderCompletedServicesTable();
        })
        .catch(error => {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="10" class="text-center py-4">
                        <div class="text-danger mb-3">
                            <i class="fas fa-exclamation-circle fa-2x"></i>
                        </div>
                        <p>Error loading completed services: ${error.message}</p>
                        <button class="btn-premium primary mt-3" onclick="loadCompletedServices()">
                            <i class="fas fa-sync-alt"></i> Try Again
                        </button>
                    </td>
                </tr>
            `;
        });
}

function enhanceCustomerInfo(service) {
    return new Promise((resolve, reject) => {
        try {
            normalizeServiceData(service);
            let customerId = service.customerId;
            if (!customerId) {
                if (service.customer && service.customer.customerId) {
                    customerId = service.customer.customerId;
                } else if (service.vehicle && service.vehicle.customer && service.vehicle.customer.customerId) {
                    customerId = service.vehicle.customer.customerId;
                } else if (service.userId) {
                    customerId = service.userId;
                }
            }

            if (!customerId) {
                const isPremium = isMembershipPremium(service);
                service.membershipStatus = isPremium ? 'Premium' : 'Standard';
                resolve(service);
                return;
            }

            const token = getAuthToken();
            fetch(`/admin/customers/api/${customerId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            })
                .then(response => {
                    if (response.ok) return response.json();
                    throw new Error('Failed to fetch customer details');
                })
                .then(customerData => {
                    if (customerData) {
                        if (customerData.firstName && customerData.lastName) {
                            service.customerName = `${customerData.firstName} ${customerData.lastName}`;
                        }
                        if (customerData.email) {
                            service.customerEmail = customerData.email;
                        }
                        if (customerData.phoneNumber) {
                            service.customerPhone = customerData.phoneNumber;
                        }
                        if (customerData.membershipStatus) {
                            service.rawCustomerMembershipStatus = customerData.membershipStatus;
                            const status = String(customerData.membershipStatus).trim().toLowerCase();
                            if (status.includes('premium')) {
                                service.membershipStatus = 'Premium';
                            }
                            else if (service.membershipStatus !== 'Premium') {
                                service.membershipStatus = 'Standard';
                            }
                        }
                        service.enhancedCustomerData = customerData;
                    }
                    const isPremium = isMembershipPremium(service);
                    service.membershipStatus = isPremium ? 'Premium' : 'Standard';
                    resolve(service);
                })
                .catch(error => {
                    const isPremium = isMembershipPremium(service);
                    service.membershipStatus = isPremium ? 'Premium' : 'Standard';
                    resolve(service);
                });
        } catch (error) {
            if (service) {
                const isPremium = isMembershipPremium(service);
                service.membershipStatus = isPremium ? 'Premium' : 'Standard';
            }
            resolve(service);
        }
    });
}

function renderCompletedServicesTable() {
    const tableBody = document.getElementById('completedServicesTableBody');
    tableBody.innerHTML = '';
    if (!completedServices || completedServices.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="10" class="text-center py-5">
                    <div class="my-4">
                        <i class="fas fa-check-circle fa-3x text-muted mb-3" style="opacity: 0.3;"></i>
                        <h5>No Completed Services</h5>
                        <p class="text-muted">Completed vehicle services will appear here once they're finished.</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    completedServices.forEach(service => {
        const row = createServiceTableRow(service);
        tableBody.appendChild(row);
    });
}

function createServiceTableRow(service) {
    const row = document.createElement('tr');
    const completionDate = new Date(service.completionDate || service.completedDate || service.updatedAt);
    const formattedDate = completionDate.toLocaleDateString();
    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });
    const invoiceStatus = service.hasInvoice ?
        `<span class="status-badge status-completed"><i class="fas fa-check-circle"></i> Generated</span>` :
        `<span class="status-badge status-pending"><i class="fas fa-clock"></i> Pending</span>`;
    const paymentStatus = service.isPaid || service.paid ?
        `<span class="status-badge status-paid"><i class="fas fa-check-circle"></i> Paid</span>` :
        `<span class="status-badge status-pending"><i class="fas fa-clock"></i> Pending</span>`;
    const deliveryStatus = service.isDelivered || service.delivered ?
        `<span class="status-badge status-completed"><i class="fas fa-check-circle"></i> Completed</span>` :
        `<span class="status-badge status-pending"><i class="fas fa-clock"></i> Pending</span>`;
    const vehicleType = (service.vehicleType || service.category || '').toString().toLowerCase();
    const vehicleIcon = vehicleType.includes('bike') || vehicleType === 'bike' ?
        'fas fa-motorcycle' :
        vehicleType.includes('truck') || vehicleType === 'truck' ?
            'fas fa-truck' :
            'fas fa-car';
    const vehicleName = service.vehicleName ||
        (service.vehicleBrand && service.vehicleModel ?
            `${service.vehicleBrand} ${service.vehicleModel}` :
            'Unknown Vehicle');

    const isPremium = isMembershipPremium(service);
    const membershipStatus = isPremium ? 'Premium' : 'Standard';
    const membershipClass = isPremium ? 'membership-premium' : 'membership-standard';

    const customerName = service.customerName || 'Unknown Customer';
    row.innerHTML = `
        <td>REQ-${service.requestId || service.serviceId}</td>
        <td>
            <div class="vehicle-info">
                <div class="vehicle-icon">
                    <i class="${vehicleIcon}"></i>
                </div>
                <div class="vehicle-details">
                    <h5>${vehicleName}</h5>
                    <p>${service.registrationNumber || 'Unknown'}</p>
                </div>
            </div>
        </td>
        <td>${customerName}</td>
        <td>${formattedDate}</td>
        <td>${formatter.format(service.totalAmount || service.totalCost || service.calculatedTotal || 0)}</td>
        <td>${invoiceStatus}</td>
        <td>${paymentStatus}</td>
        <td>${deliveryStatus}</td>
        <td>
            <div class="table-actions-cell">
                <button class="btn-table-action view-service-btn" data-id="${service.requestId || service.serviceId}">
                    <i class="fas fa-eye"></i>
                </button>
            </div>
        </td>
    `;
    return row;
}

function viewServiceDetails(serviceId) {
    const modal = new bootstrap.Modal(document.getElementById('viewServiceDetailsModal'));
    modal.show();
    loadBasicServiceDetails(serviceId);
    loadInvoiceData(serviceId);
}

document.addEventListener('DOMContentLoaded', function() {
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('view-service-btn') || e.target.closest('.view-service-btn')) {
            const btn = e.target.classList.contains('view-service-btn') ? e.target : e.target.closest('.view-service-btn');
            const serviceId = btn.getAttribute('data-id');
            viewServiceDetails(serviceId);
        }
    });
});

function loadBasicServiceDetails(serviceId) {
    currentServiceId = serviceId;
    document.getElementById('viewServiceId').textContent = `REQ-${serviceId}`;
    document.getElementById('viewVehicleName').textContent = 'Loading...';
    document.getElementById('viewRegistrationNumber').textContent = 'Loading...';
    document.getElementById('viewCustomerName').textContent = 'Loading...';
    document.getElementById('viewMembership').textContent = 'Loading...';
    document.getElementById('viewCompletionDate').textContent = 'Loading...';
    const token = getAuthToken();
    const serviceUrls = [
        `/admin/api/completed-services/${serviceId}`,
        `/admin/api/services/${serviceId}/details`,
        `/admin/api/service-details/${serviceId}`
    ];
    tryFetchUrls(serviceUrls, token)
        .then(service => {
            currentService = service;
            document.getElementById('viewServiceId').textContent = `REQ-${service.requestId || service.serviceId || serviceId}`;
            document.getElementById('viewVehicleName').textContent = getVehicleName(service);
            document.getElementById('viewRegistrationNumber').textContent = getRegistrationNumber(service);
            document.getElementById('viewCustomerName').textContent = service.customerName || 'Unknown Customer';
            document.getElementById('viewMembership').textContent = getMembershipStatus(service);
            document.getElementById('viewCompletionDate').textContent = getFormattedDate(service);
            updateWorkflowSteps(service);
            updateFooterButtons(service);
        })
        .catch(error => {
            document.getElementById('viewServiceId').textContent = `REQ-${serviceId}`;
            document.getElementById('viewVehicleName').textContent = 'Error loading details';
            document.getElementById('viewRegistrationNumber').textContent = 'Error loading details';
            document.getElementById('viewCustomerName').textContent = 'Error loading details';
            document.getElementById('viewMembership').textContent = 'Error loading details';
            document.getElementById('viewCompletionDate').textContent = 'Error loading details';
            showToast('Error loading service details: ' + error.message, 'error');
        });
}

function getRegistrationNumber(service) {
    if (service.registrationNumber) {
        return service.registrationNumber;
    }
    if (service.vehicleRegistration) {
        return service.vehicleRegistration;
    }
    if (service.vehicle && service.vehicle.registrationNumber) {
        return service.vehicle.registrationNumber;
    }
    return 'Unknown';
}

function tryFetchUrls(urls, token) {
    return urls.reduce((promise, url) => {
        return promise.catch(() => {
            return fetch(url, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            }).then(response => {
                if (!response.ok) {
                    throw new Error(`Failed to fetch from ${url}: ${response.status}`);
                }
                return response.json();
            });
        });
    }, Promise.reject(new Error('Starting URL chain')));
}

function getVehicleName(service) {
    if (service.vehicleName) {
        return service.vehicleName;
    }
    if (service.vehicleBrand && service.vehicleModel) {
        return `${service.vehicleBrand} ${service.vehicleModel}`;
    }
    if (service.vehicle) {
        const vehicle = service.vehicle;
        if (vehicle.brand && vehicle.model) {
            return `${vehicle.brand} ${vehicle.model}`;
        }
    }
    return 'Unknown Vehicle';
}

function getFormattedDate(service) {
    const dateFields = [
        'completionDate', 'completedDate', 'updatedAt',
        'formattedCompletedDate', 'formattedCompletionDate'
    ];
    let dateValue = null;
    for (const field of dateFields) {
        if (service[field]) {
            dateValue = service[field];
            break;
        }
    }
    if (!dateValue) {
        return new Date().toLocaleDateString();
    }
    if (typeof dateValue === 'string' && dateValue.includes(',')) {
        return dateValue;
    }
    try {
        const date = new Date(dateValue);
        if (!isNaN(date.getTime())) {
            return date.toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric'
            });
        }
    } catch (e) {
    }
    return dateValue;
}

function populateMaterialsTable(materials) {
    const tableBody = document.getElementById('materialsTableBody');
    tableBody.innerHTML = '';
    if (!materials || materials.length === 0) {
        tableBody.innerHTML = `
            <tr><td colspan="4" class="text-center">No materials used in this service</td></tr>
        `;
        return;
    }
    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });
    let materialTotal = 0;
    materials.forEach(material => {
        if (!material) return;
        const row = document.createElement('tr');
        const itemName = material.name || 'Unknown Item';
        const quantity = parseFloatSafe(material.quantity, 1);
        const unitPrice = parseFloatSafe(material.unitPrice, 0);
        let total;
        if (material.total) {
            total = parseFloatSafe(material.total, 0);
        } else {
            total = quantity * unitPrice;
        }
        materialTotal += total;
        row.innerHTML = `
            <td>${itemName}</td>
            <td>${quantity}</td>
            <td>${formatter.format(unitPrice)}</td>
            <td>${formatter.format(total)}</td>
        `;
        tableBody.appendChild(row);
    });
    if (materials.length > 1) {
        const totalRow = document.createElement('tr');
        totalRow.innerHTML = `
            <td colspan="3" class="text-end fw-bold">Total</td>
            <td class="fw-bold">${formatter.format(materialTotal)}</td>
        `;
        tableBody.appendChild(totalRow);
    }
}

function getAuthToken() {
    const urlParams = new URLSearchParams(window.location.search);
    const tokenParam = urlParams.get('token');
    if (tokenParam) return tokenParam;
    const sessionToken = sessionStorage.getItem('jwt-token');
    if (sessionToken) return sessionToken;
    const localToken = localStorage.getItem('jwt-token');
    if (localToken) return localToken;
    if (typeof token !== 'undefined') return token;
    return '';
}

function parseFloatSafe(value, defaultValue) {
    if (value === null || value === undefined) {
        return defaultValue;
    }
    if (typeof value === 'number') {
        return value;
    }
    try {
        const parsed = parseFloat(value);
        return isNaN(parsed) ? defaultValue : parsed;
    } catch (e) {
        return defaultValue;
    }
}

function populateLaborChargesTable(laborCharges) {
    const tableBody = document.getElementById('laborChargesTableBody');
    tableBody.innerHTML = '';
    if (!laborCharges || laborCharges.length === 0) {
        tableBody.innerHTML = `
            <tr><td colspan="4" class="text-center">No labor charges recorded for this service</td></tr>
        `;
        return;
    }
    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });
    let laborTotal = 0;
    laborCharges.forEach(charge => {
        if (!charge) return;
        const row = document.createElement('tr');
        const description = charge.description || 'Service Labor';
        const hours = parseFloatSafe(charge.hours, 0);
        const ratePerHour = parseFloatSafe(charge.ratePerHour, 0);
        let total;
        if (charge.total) {
            total = parseFloatSafe(charge.total, 0);
        } else {
            total = hours * ratePerHour;
        }
        laborTotal += total;
        row.innerHTML = `
            <td>${description}</td>
            <td>${hours.toFixed(2)}</td>
            <td>${formatter.format(ratePerHour)}/hr</td>
            <td>${formatter.format(total)}</td>
        `;
        tableBody.appendChild(row);
    });
    if (laborCharges.length > 1) {
        const totalRow = document.createElement('tr');
        totalRow.innerHTML = `
            <td colspan="3" class="text-end fw-bold">Total</td>
            <td class="fw-bold">${formatter.format(laborTotal)}</td>
        `;
        tableBody.appendChild(totalRow);
    }
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    }).format(amount);
}

function updateInvoiceSummary(data) {
    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });
    if (!data) {
        document.getElementById('summaryMaterialsTotal').textContent = formatter.format(0);
        document.getElementById('summaryLaborTotal').textContent = formatter.format(0);
        document.getElementById('premiumDiscountRow').style.display = 'none';
        document.getElementById('summarySubtotal').textContent = formatter.format(0);
        document.getElementById('summaryGST').textContent = formatter.format(0);
        document.getElementById('summaryGrandTotal').textContent = formatter.format(0);
        return;
    }

    const materialsTotal = parseFloatSafe(data.materialsTotal, 0);
    const laborTotal = parseFloatSafe(data.laborTotal, 0);
    const discount = parseFloatSafe(data.discount, 0);
    let subtotal = parseFloatSafe(data.subtotal, 0);
    if (subtotal === 0) {
        subtotal = materialsTotal + laborTotal - discount;
    }
    let tax = parseFloatSafe(data.tax, 0);
    if (tax === 0) {
        tax = subtotal * 0.18;
    }
    let grandTotal = parseFloatSafe(data.grandTotal, 0);
    if (grandTotal === 0) {
        grandTotal = subtotal + tax;
    }

    document.getElementById('summaryMaterialsTotal').textContent = formatter.format(materialsTotal);
    document.getElementById('summaryLaborTotal').textContent = formatter.format(laborTotal);
    if (discount > 0) {
        document.getElementById('summaryDiscount').textContent = `-${formatter.format(discount)}`;
        document.getElementById('premiumDiscountRow').style.display = '';
    } else {
        document.getElementById('premiumDiscountRow').style.display = 'none';
    }
    document.getElementById('summarySubtotal').textContent = formatter.format(subtotal);
    document.getElementById('summaryGST').textContent = formatter.format(tax);
    document.getElementById('summaryGrandTotal').textContent = formatter.format(grandTotal);
}

function updateWorkflowSteps(service) {
    document.querySelectorAll('.workflow-step').forEach(step => {
        step.classList.remove('active', 'completed');
    });
    const hasInvoice = service.hasInvoice || service.invoiceId ||
        (service.invoice && service.invoice.invoiceId) || false;
    const isPaid = service.isPaid || service.paid ||
        (service.payment && service.payment.status === 'Completed') || false;
    const isDelivered = service.isDelivered || service.delivered || false;
    if (hasInvoice) {
        document.getElementById('stepInvoice').classList.add('completed');
        if (isPaid) {
            document.getElementById('stepPayment').classList.add('completed');
            if (isDelivered) {
                document.getElementById('stepDelivery').classList.add('completed');
            } else {
                document.getElementById('stepDelivery').classList.add('active');
            }
        } else {
            document.getElementById('stepPayment').classList.add('active');
        }
    } else {
        document.getElementById('stepInvoice').classList.add('active');
    }
    service.hasInvoice = hasInvoice;
    service.isPaid = isPaid;
    service.isDelivered = isDelivered;
}

function updateFooterButtons(service) {
    const footer = document.getElementById('serviceDetailsFooter');
    const actionButtons = footer.querySelectorAll('button:not([data-bs-dismiss="modal"])');
    actionButtons.forEach(button => button.remove());
    if (!service.hasInvoice) {
        const generateInvoiceBtn = document.createElement('button');
        generateInvoiceBtn.type = 'button';
        generateInvoiceBtn.className = 'btn-premium primary';
        generateInvoiceBtn.innerHTML = '<i class="fas fa-file-invoice"></i> Generate Invoice';
        generateInvoiceBtn.addEventListener('click', openGenerateInvoiceModal);
        footer.appendChild(generateInvoiceBtn);
    } else if (!service.isPaid) {
        const processPaymentBtn = document.createElement('button');
        processPaymentBtn.type = 'button';
        processPaymentBtn.className = 'btn-premium primary';
        processPaymentBtn.innerHTML = '<i class="fas fa-money-bill-wave"></i> Process Payment';
        processPaymentBtn.addEventListener('click', openPaymentModal);
        footer.appendChild(processPaymentBtn);
    } else if (!service.isDelivered) {
        const deliveryBtn = document.createElement('button');
        deliveryBtn.type = 'button';
        deliveryBtn.className = 'btn-premium primary';
        deliveryBtn.innerHTML = '<i class="fas fa-truck"></i> Schedule Delivery';
        deliveryBtn.addEventListener('click', openDeliveryModal);
        footer.appendChild(deliveryBtn);
    }
    if (service.hasInvoice) {
        const downloadInvoiceBtn = document.createElement('button');
        downloadInvoiceBtn.type = 'button';
        downloadInvoiceBtn.className = 'btn-premium secondary me-2';
        downloadInvoiceBtn.innerHTML = '<i class="fas fa-download"></i> Download Invoice';
        downloadInvoiceBtn.addEventListener('click', () => downloadInvoice(service.requestId || service.serviceId));
        footer.insertBefore(downloadInvoiceBtn, footer.firstChild);
    }
}

function loadInvoiceData(serviceId) {
    document.getElementById('materialsTableBody').innerHTML = `
        <tr><td colspan="4" class="text-center"><div class="spinner-border spinner-border-sm text-wine" role="status"></div> Loading materials...</td></tr>
    `;
    document.getElementById('laborChargesTableBody').innerHTML = `
        <tr><td colspan="4" class="text-center"><div class="spinner-border spinner-border-sm text-wine" role="status"></div> Loading labor charges...</td></tr>
    `;
    const token = getAuthToken();
    const invoiceUrls = [
        `/admin/api/completed-services/${serviceId}/invoice-details`,
        `/admin/api/invoice-details/service-request/${serviceId}`,
        `/admin/api/invoice-details/service/${serviceId}`,
        `/admin/api/services/${serviceId}/invoice-details`,
        `/admin/api/vehicle-tracking/service-request/${serviceId}`
    ];
    tryFetchUrls(invoiceUrls, token)
        .then(data => {
            if (!data) {
                data = createDefaultInvoiceData(serviceId);
            }
            const materials = data.materials || [];
            populateMaterialsTable(materials);
            const laborCharges = data.laborCharges || [];
            populateLaborChargesTable(laborCharges);
            updateInvoiceSummary(data);
        })
        .catch(error => {
            document.getElementById('materialsTableBody').innerHTML = `
                <tr><td colspan="4" class="text-center">No materials data available</td></tr>
            `;
            document.getElementById('laborChargesTableBody').innerHTML = `
                <tr><td colspan="4" class="text-center">No labor charges data available</td></tr>
            `;
            updateInvoiceSummary(createDefaultInvoiceData(serviceId));
        });
}

function createDefaultInvoiceData(serviceId) {
    return {
        requestId: serviceId,
        serviceId: serviceId,
        materialsTotal: 0,
        laborTotal: 0,
        discount: 0,
        subtotal: 0,
        tax: 0,
        grandTotal: 0,
        materials: [],
        laborCharges: []
    };
}

function downloadInvoice(serviceId) {
    const token = getAuthToken();
    const downloadUrl = `/admin/api/completed-services/${serviceId}/invoice/download?token=${token}`;
    window.open(downloadUrl, '_blank');
}

function openGenerateInvoiceModal() {
    if (!currentService) return;
    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });
    document.getElementById('invoiceServiceId').textContent = `REQ-${currentService.requestId || currentService.serviceId}`;
    document.getElementById('invoiceCustomerName').textContent = currentService.customerName;
    document.getElementById('customerEmail').value = currentService.customerEmail || '';
    let materialsTotal = currentService.calculatedMaterialsTotal || 0;
    let laborTotal = currentService.calculatedLaborTotal || 0;
    let discount = currentService.calculatedDiscount || 0;
    let subtotal = currentService.calculatedSubtotal || 0;
    let tax = currentService.calculatedTax || 0;
    let total = currentService.calculatedTotal || 0;
    const isPremium = isMembershipPremium(currentService);
    const premiumDiscountRow = document.getElementById('invoicePremiumDiscountRow');
    const premiumBadge = document.getElementById('invoicePremiumBadge');
    if (isPremium) {
        premiumBadge.style.display = '';
        if (discount > 0) {
            document.getElementById('invoiceDiscount').textContent = `-${formatter.format(discount)}`;
            premiumDiscountRow.style.display = '';
        } else {
            premiumDiscountRow.style.display = 'none';
        }
    } else {
        premiumDiscountRow.style.display = 'none';
        premiumBadge.style.display = 'none';
    }
    document.getElementById('invoiceMaterialsTotal').textContent = formatter.format(materialsTotal);
    document.getElementById('invoiceLaborTotal').textContent = formatter.format(laborTotal);
    document.getElementById('invoiceSubtotal').textContent = formatter.format(subtotal);
    document.getElementById('invoiceGST').textContent = formatter.format(tax);
    document.getElementById('invoiceGrandTotal').textContent = formatter.format(total);
    const serviceDetailsModal = bootstrap.Modal.getInstance(document.getElementById('viewServiceDetailsModal'));
    serviceDetailsModal.hide();
    const invoiceModal = new bootstrap.Modal(document.getElementById('generateInvoiceModal'));
    invoiceModal.show();
}

function generateInvoice() {
    if (!currentService) return;
    const email = document.getElementById('customerEmail').value;
    if (!email) {
        showToast('Please enter customer email', 'error');
        return;
    }
    const sendEmail = document.getElementById('sendInvoiceEmail').checked;
    const confirmBtn = document.getElementById('confirmGenerateInvoiceBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<div class="spinner-border spinner-border-sm me-2" role="status"></div> Generating...';
    const invoiceModal = bootstrap.Modal.getInstance(document.getElementById('generateInvoiceModal'));
    invoiceModal.hide();
    const invoiceRequest = {
        serviceId: currentService.requestId || currentService.serviceId,
        emailAddress: email,
        sendEmail: sendEmail,
        notes: "Generated from admin portal",
        materialsTotal: currentService.calculatedMaterialsTotal,
        laborTotal: currentService.calculatedLaborTotal,
        discount: currentService.calculatedDiscount,
        subtotal: currentService.calculatedSubtotal,
        tax: currentService.calculatedTax,
        total: currentService.calculatedTotal,
        membershipStatus: getMembershipStatus(currentService)
    };
    const token = getAuthToken();
    fetch(`/admin/api/invoices/service-request/${currentService.requestId || currentService.serviceId}/generate`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(invoiceRequest)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to generate invoice');
            }
            return response.json();
        })
        .then(data => {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="fas fa-file-invoice"></i> Generate & Send';
            if (currentService) {
                currentService.hasInvoice = true;
            }
            loadCompletedServices();
            const successModal = new bootstrap.Modal(document.getElementById('successModal'));
            document.getElementById('successTitle').textContent = 'Invoice Generated';
            document.getElementById('successMessage').textContent = sendEmail ?
                `Invoice has been generated and sent to ${email}` :
                'Invoice has been generated successfully';
            successModal.show();
            setTimeout(() => {
                successModal.hide();
                openPaymentModal();
            }, 2000);
        })
        .catch(error => {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="fas fa-file-invoice"></i> Generate & Send';
            showToast('Error generating invoice: ' + error.message, 'error');
        });
}

function openPaymentModal() {
    if (!currentService) return;
    document.getElementById('paymentServiceId').textContent = `REQ-${currentService.requestId || currentService.serviceId}`;
    document.getElementById('paymentCustomerName').textContent = currentService.customerName;
    const total = currentService.calculatedTotal || 0;
    document.getElementById('paidAmount').value = total.toFixed(2);
    const paymentModal = new bootstrap.Modal(document.getElementById('paymentModal'));
    paymentModal.show();
}

function processPayment() {
    if (!currentService) return;
    const paymentMethod = document.getElementById('paymentMethod').value;
    const transactionId = document.getElementById('transactionId').value;
    const paidAmount = document.getElementById('paidAmount').value;
    if (!paymentMethod) {
        showToast('Please select a payment method', 'error');
        return;
    }
    if (!paidAmount || paidAmount <= 0) {
        showToast('Please enter a valid amount', 'error');
        return;
    }
    const confirmBtn = document.getElementById('confirmPaymentBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<div class="spinner-border spinner-border-sm me-2" role="status"></div> Processing...';
    const paymentModal = bootstrap.Modal.getInstance(document.getElementById('paymentModal'));
    paymentModal.hide();
    const paymentRequest = {
        serviceId: currentService.requestId || currentService.serviceId,
        paymentMethod: paymentMethod,
        transactionId: transactionId,
        amount: parseFloat(paidAmount),
        notes: "Payment processed by admin"
    };
    const token = getAuthToken();
    const paymentEndpoints = [
        `/admin/api/vehicle-tracking/process-payment`,
        `/admin/api/vehicle-tracking/service-request/${currentService.requestId || currentService.serviceId}/payment`,
        `/admin/api/completed-services/${currentService.requestId || currentService.serviceId}/payment`
    ];
    tryPostUrls(paymentEndpoints, paymentRequest, token)
        .then(data => {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="fas fa-check-circle"></i> Confirm Payment';
            if (currentService) {
                currentService.isPaid = true;
                currentService.paid = true;
            }
            loadCompletedServices();
            const successModal = new bootstrap.Modal(document.getElementById('successModal'));
            document.getElementById('successTitle').textContent = 'Payment Processed';
            document.getElementById('successMessage').textContent = 'Payment has been processed successfully';
            successModal.show();
            setTimeout(() => {
                successModal.hide();
                openDeliveryModal();
            }, 2000);
        })
        .catch(error => {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="fas fa-check-circle"></i> Confirm Payment';
            showToast('Error processing payment: ' + error.message, 'error');
        });
}

function tryPostUrls(urls, requestData, token) {
    return urls.reduce((promise, url) => {
        return promise.catch(() => {
            return fetch(url, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            }).then(response => {
                if (!response.ok) {
                    throw new Error(`Failed to POST to ${url}: ${response.status}`);
                }
                return response.json();
            });
        });
    }, Promise.reject(new Error('Starting POST URL chain')));
}

function openDeliveryModal() {
    if (!currentService) return;
    document.getElementById('deliveryServiceId').textContent = `REQ-${currentService.requestId || currentService.serviceId}`;
    document.getElementById('pickupPerson').value = '';
    document.getElementById('pickupTime').value = '';
    document.getElementById('deliveryAddress').value = '';
    document.getElementById('deliveryDate').value = '';
    document.getElementById('deliveryContact').value = '';
    document.getElementById('customerPickup').checked = true;
    document.getElementById('pickupFields').style.display = 'block';
    document.getElementById('deliveryFields').style.display = 'none';
    document.getElementById('confirmDeliveryBtnText').textContent = 'Confirm Pickup';
    const deliveryModal = new bootstrap.Modal(document.getElementById('deliveryModal'));
    deliveryModal.show();
}

function processDelivery() {
    if (!currentService) return;
    const deliveryMethod = document.querySelector('input[name="deliveryMethod"]:checked').value;
    if (deliveryMethod === 'pickup') {
        const pickupPerson = document.getElementById('pickupPerson').value;
        const pickupTime = document.getElementById('pickupTime').value;
        if (!pickupPerson) {
            showToast('Please enter pickup person name', 'error');
            return;
        }
        if (!pickupTime) {
            showToast('Please select pickup time', 'error');
            return;
        }
    } else {
        const deliveryAddress = document.getElementById('deliveryAddress').value;
        const deliveryDate = document.getElementById('deliveryDate').value;
        const deliveryContact = document.getElementById('deliveryContact').value;
        if (!deliveryAddress) {
            showToast('Please enter delivery address', 'error');
            return;
        }
        if (!deliveryDate) {
            showToast('Please select delivery date', 'error');
            return;
        }
        if (!deliveryContact) {
            showToast('Please enter contact number', 'error');
            return;
        }
    }
    const confirmBtn = document.getElementById('confirmDeliveryBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<div class="spinner-border spinner-border-sm me-2" role="status"></div> Processing...';
    const deliveryModal = bootstrap.Modal.getInstance(document.getElementById('deliveryModal'));
    deliveryModal.hide();
    const deliveryRequest = {
        serviceId: currentService.requestId || currentService.serviceId,
        deliveryType: deliveryMethod,
        notes: "Processed by admin"
    };
    if (deliveryMethod === 'pickup') {
        deliveryRequest.pickupPerson = document.getElementById('pickupPerson').value;
        deliveryRequest.pickupTime = document.getElementById('pickupTime').value;
    } else {
        deliveryRequest.deliveryAddress = document.getElementById('deliveryAddress').value;
        deliveryRequest.deliveryDate = document.getElementById('deliveryDate').value;
        deliveryRequest.contactNumber = document.getElementById('deliveryContact').value;
    }
    const token = getAuthToken();
    const deliveryEndpoints = [
        `/admin/api/vehicle-tracking/service-request/${currentService.requestId || currentService.serviceId}/dispatch`,
        `/admin/api/completed-services/${currentService.requestId || currentService.serviceId}/dispatch`,
        `/admin/api/delivery/service-request/${currentService.requestId || currentService.serviceId}`
    ];
    tryPostUrls(deliveryEndpoints, deliveryRequest, token)
        .then(data => {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = `<i class="fas fa-check-circle"></i> ${deliveryMethod === 'pickup' ? 'Confirm Pickup' : 'Confirm Delivery'}`;
            if (currentService) {
                currentService.isDelivered = true;
                currentService.delivered = true;
            }
            loadCompletedServices();
            const successModal = new bootstrap.Modal(document.getElementById('successModal'));
            document.getElementById('successTitle').textContent = 'Delivery Scheduled';
            document.getElementById('successMessage').textContent = deliveryMethod === 'pickup' ?
                'Vehicle pickup has been scheduled successfully' :
                'Vehicle delivery has been scheduled successfully';
            successModal.show();
        })
        .catch(error => {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = `<i class="fas fa-check-circle"></i> ${deliveryMethod === 'pickup' ? 'Confirm Pickup' : 'Confirm Delivery'}`;
            showToast('Error processing delivery: ' + error.message, 'error');
        });
}

function showToast(message, type = 'success') {
    const toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) return;
    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center text-white bg-${type === 'error' ? 'danger' : (type === 'warning' ? 'warning' : 'success')} border-0`;
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');
    toastEl.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    `;
    toastContainer.appendChild(toastEl);
    const toast = new bootstrap.Toast(toastEl, {
        autohide: true,
        delay: 3000
    });
    toast.show();
    toastEl.addEventListener('hidden.bs.toast', function() {
        toastEl.remove();
    });
}