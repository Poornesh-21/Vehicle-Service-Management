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
    document.getElementById('paymentMethod').addEventListener('change', function() {
        const transactionIdGroup = document.getElementById('transactionIdGroup');
        // Hide transaction ID field when Cash is selected
        if (this.value === 'Cash') {
            transactionIdGroup.style.display = 'none';
            document.getElementById('transactionId').value = 'CASH-' + Date.now().toString().slice(-6);
            document.getElementById('transactionId').disabled = true;
        } else {
            transactionIdGroup.style.display = 'block';
            document.getElementById('transactionId').value = '';
            document.getElementById('transactionId').disabled = false;
        }
    });
    document.getElementById('homeDelivery').addEventListener('change', function() {
        if (this.checked) {
            document.getElementById('pickupFields').style.display = 'none';
            document.getElementById('deliveryFields').style.display = 'block';
            document.getElementById('confirmDeliveryBtnText').textContent = 'Confirm Delivery';

            // Auto-fill customer address if available
            if (currentService) {
                const customerAddress = getCustomerAddress(currentService);
                if (customerAddress) {
                    document.getElementById('deliveryAddress').value = customerAddress;
                }
            }
        }
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

    console.log("Populating labor charges table with:", laborCharges);

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

        // Get hours
        const hours = parseFloatSafe(charge.hours, 0);

        // IMPORTANT: Use 'rate' to match backend field name
        // Fall back to 'ratePerHour' for backward compatibility
        const ratePerHour = parseFloatSafe(charge.rate || charge.ratePerHour, 0);

        // Get or calculate total
        let total = parseFloatSafe(charge.total, hours * ratePerHour);

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
    console.log("Updating invoice summary with data:", data);

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

    // Materials total
    const materialsTotal = parseFloatSafe(
        data.materialsTotal || data.calculatedMaterialsTotal || data.total_material_cost,
        0
    );

    // Labor total
    const laborTotal = parseFloatSafe(
        data.laborTotal || data.calculatedLaborTotal || data.labor_cost,
        0
    );

    console.log(`Materials total: ${materialsTotal}, Labor total: ${laborTotal}`);

    // Calculate premium discount (30% of labor only)
    let discount = 0;

    if (data.membershipStatus &&
        (data.membershipStatus.toLowerCase() === 'premium' ||
            data.membershipStatus.toLowerCase().includes('premium'))) {
        // Apply 30% discount to labor only
        discount = parseFloat((laborTotal * 0.3).toFixed(2));
        console.log(`Applied premium discount: ${discount} (30% of labor ${laborTotal})`);
    }

    // Calculate subtotal
    const subtotal = parseFloat((materialsTotal + laborTotal - discount).toFixed(2));

    // Calculate tax (18% GST)
    const tax = parseFloat((subtotal * 0.18).toFixed(2));

    // Calculate grand total
    const grandTotal = parseFloat((subtotal + tax).toFixed(2));

    // Update UI
    document.getElementById('summaryMaterialsTotal').textContent = formatter.format(materialsTotal);
    document.getElementById('summaryLaborTotal').textContent = formatter.format(laborTotal);

    if (discount > 0) {
        // Update discount label
        if (document.getElementById('premiumDiscountLabel')) {
            document.getElementById('premiumDiscountLabel').textContent = 'Premium Discount (30% off labor)';
        }

        document.getElementById('summaryDiscount').textContent = `-${formatter.format(discount)}`;
        document.getElementById('premiumDiscountRow').style.display = '';
    } else {
        document.getElementById('premiumDiscountRow').style.display = 'none';
    }

    document.getElementById('summarySubtotal').textContent = formatter.format(subtotal);
    document.getElementById('summaryGST').textContent = formatter.format(tax);
    document.getElementById('summaryGrandTotal').textContent = formatter.format(grandTotal);

    // Store calculated values for later use
    data.calculatedMaterialsTotal = materialsTotal;
    data.calculatedLaborTotal = laborTotal;
    data.calculatedDiscount = discount;
    data.calculatedSubtotal = subtotal;
    data.calculatedTax = tax;
    data.calculatedTotal = grandTotal;
}

function calculateMaterialsTotal(materials) {
    if (!materials || !Array.isArray(materials)) return 0;

    return materials.reduce((total, material) => {
        if (!material) return total;

        const quantity = parseFloatSafe(material.quantity, 1);
        const unitPrice = parseFloatSafe(material.unitPrice, 0);
        const itemTotal = parseFloatSafe(material.total, quantity * unitPrice);

        return total + itemTotal;
    }, 0);
}

function calculateLaborTotal(laborCharges) {
    if (!laborCharges || !Array.isArray(laborCharges)) return 0;

    return laborCharges.reduce((total, charge) => {
        if (!charge) return total;

        const hours = parseFloatSafe(charge.hours, 0);
        const rate = parseFloatSafe(charge.ratePerHour || charge.rate, 0);
        const chargeTotal = parseFloatSafe(charge.total, hours * rate);

        return total + chargeTotal;
    }, 0);
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

function processLaborData(data, serviceId) {
    // Store original data for debugging
    const originalData = JSON.parse(JSON.stringify(data));

    // Check if we have labor_minutes and labor_cost from DB
    if (data.labor_minutes !== undefined && data.labor_cost !== undefined) {
        console.log(`Using DB values: labor_minutes=${data.labor_minutes}, labor_cost=${data.labor_cost}`);

        // Convert minutes to hours (ensure floating point division)
        const laborHours = data.labor_minutes / 60;

        // Calculate hourly rate (avoid division by zero)
        const hourlyRate = laborHours > 0 ? parseFloat((data.labor_cost / laborHours).toFixed(2)) : 0;

        // Create labor charge object with correct values from DB
        const laborCharge = {
            description: data.work_description || data.serviceType || 'Service Labor',
            hours: laborHours,
            ratePerHour: hourlyRate,
            total: parseFloat(data.labor_cost)
        };

        console.log("Created labor charge from DB data:", laborCharge);

        // Override any existing labor charges
        data.laborCharges = [laborCharge];
        data.laborTotal = parseFloat(data.labor_cost);

        // Make sure values are properly set for summary calculation
        data.calculatedLaborTotal = data.laborTotal;
    }
    // Check for existing labor charges
    else if (data.laborCharges && data.laborCharges.length > 0) {
        console.log("Using existing labor charges:", data.laborCharges);

        // Ensure labor total is calculated correctly
        let total = 0;
        data.laborCharges.forEach(charge => {
            // Make sure each labor charge has a total
            if (!charge.total && charge.hours && charge.ratePerHour) {
                charge.total = charge.hours * charge.ratePerHour;
            }
            total += parseFloat(charge.total || 0);
        });

        data.laborTotal = total;
        data.calculatedLaborTotal = total;
    }
    // Use default values as last resort
    else if (data.serviceType) {
        console.log("No labor data found, creating default labor charge");

        // Much more conservative defaults - 1 hour at ₹100
        data.laborCharges = [{
            description: `Service: ${data.serviceType}`,
            hours: 3, // Default to 3 hours based on original data
            ratePerHour: 65, // Calculate from 195 ÷ 3
            total: 195 // Default total based on original data
        }];

        data.laborTotal = 195;
        data.calculatedLaborTotal = 195;
    }

    // Check if the data was modified during processing
    if (JSON.stringify(data) !== JSON.stringify(originalData)) {
        console.log("Data was modified during labor processing");
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

    // Only use endpoints that actually exist in the backend
    const invoiceUrls = [
        `/admin/api/completed-services/${serviceId}/invoice-details`,
        `/admin/api/vehicle-tracking/service-request/${serviceId}`
    ];

    console.log(`Loading invoice data for service ID: ${serviceId}`);

    tryFetchUrls(invoiceUrls, token)
        .then(data => {
            console.log("Raw data from API:", data);

            if (!data) {
                data = createDefaultInvoiceData(serviceId);
            }

            // Check for labor_minutes and labor_cost in the response
            if (data.labor_minutes !== undefined && data.labor_cost !== undefined) {
                console.log(`Found labor data: ${data.labor_minutes} minutes, ₹${data.labor_cost}`);

                // Convert minutes to hours
                const laborHours = data.labor_minutes / 60;

                // Calculate hourly rate
                const hourlyRate = laborHours > 0 ? parseFloat((data.labor_cost / laborHours).toFixed(2)) : 0;

                // Create labor charge with correct values
                const laborCharge = {
                    description: data.work_description || data.serviceType || 'Service Labor',
                    hours: laborHours,
                    rate: hourlyRate, // Use 'rate' to match backend field name
                    total: parseFloat(data.labor_cost)
                };

                console.log("Created labor charge:", laborCharge);

                // Override existing labor charges
                data.laborCharges = [laborCharge];
                data.laborTotal = parseFloat(data.labor_cost);
            }
            else if (data.laborCharges && data.laborCharges.length > 0) {
                console.log("Using existing labor charges:", data.laborCharges);

                // Calculate labor total
                let total = 0;
                data.laborCharges.forEach(charge => {
                    // Important: use 'rate' (backend field) instead of 'ratePerHour'
                    const hours = parseFloatSafe(charge.hours, 0);
                    const rate = parseFloatSafe(charge.rate, 0);

                    // Ensure each charge has a total
                    if (charge.total === undefined) {
                        charge.total = hours * rate;
                    }

                    total += parseFloatSafe(charge.total, 0);
                });

                data.laborTotal = total;
            }

            const materials = data.materials || [];
            populateMaterialsTable(materials);
            populateLaborChargesTable(data.laborCharges || []);
            updateInvoiceSummary(data);
        })
        .catch(error => {
            console.error("Error loading invoice data:", error);
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
function extractLaborData(data) {
    if (!data) return null;

    // Check direct properties first (most common case)
    if (data.labor_minutes !== undefined && data.labor_cost !== undefined) {
        return {
            labor_minutes: data.labor_minutes,
            labor_cost: data.labor_cost
        };
    }

    // Check for data in serviceTracking object
    if (data.serviceTracking &&
        data.serviceTracking.labor_minutes !== undefined &&
        data.serviceTracking.labor_cost !== undefined) {
        return {
            labor_minutes: data.serviceTracking.labor_minutes,
            labor_cost: data.serviceTracking.labor_cost
        };
    }

    // Check for data in serviceTrackings array (most recent entry)
    if (data.serviceTrackings && Array.isArray(data.serviceTrackings) &&
        data.serviceTrackings.length > 0) {
        const tracking = data.serviceTrackings[0]; // Usually the most recent
        if (tracking.labor_minutes !== undefined && tracking.labor_cost !== undefined) {
            return {
                labor_minutes: tracking.labor_minutes,
                labor_cost: tracking.labor_cost
            };
        }
    }

    // Deep scan for any object containing both labor_minutes and labor_cost
    for (const key in data) {
        if (typeof data[key] === 'object' && data[key] !== null) {
            const obj = data[key];
            if (obj.labor_minutes !== undefined && obj.labor_cost !== undefined) {
                return {
                    labor_minutes: obj.labor_minutes,
                    labor_cost: obj.labor_cost
                };
            }
        }
    }

    // No labor data found in any expected location
    return null;
}


function downloadInvoice(serviceId) {
    const token = getAuthToken();
    const downloadUrl = `/admin/api/completed-services/${serviceId}/invoice/download?token=${token}`;
    window.open(downloadUrl, '_blank');
}

function openGenerateInvoiceModal() {
    if (!currentService) {
        showToast('Service data not available', 'error');
        return;
    }

    // Format currency for display
    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

    // Set service and customer information
    document.getElementById('invoiceServiceId').textContent = `REQ-${currentService.requestId || currentService.serviceId}`;
    document.getElementById('invoiceCustomerName').textContent = currentService.customerName || 'Unknown Customer';

    // Pre-fill customer email if available
    if (currentService.customerEmail) {
        document.getElementById('customerEmail').value = currentService.customerEmail;
    } else if (currentService.enhancedCustomerData && currentService.enhancedCustomerData.email) {
        document.getElementById('customerEmail').value = currentService.enhancedCustomerData.email;
    } else {
        // Try to get email from different locations in the object
        const email = findEmailInObject(currentService);
        document.getElementById('customerEmail').value = email || '';
    }

    // Set invoice amounts
    let materialsTotal = currentService.calculatedMaterialsTotal || 0;
    let laborTotal = currentService.calculatedLaborTotal || 0;
    let discount = currentService.calculatedDiscount || 0;
    let subtotal = currentService.calculatedSubtotal || 0;
    let tax = currentService.calculatedTax || 0;
    let total = currentService.calculatedTotal || 0;

    // Handle premium status and discount display
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

    // Set amount displays
    document.getElementById('invoiceMaterialsTotal').textContent = formatter.format(materialsTotal);
    document.getElementById('invoiceLaborTotal').textContent = formatter.format(laborTotal);
    document.getElementById('invoiceSubtotal').textContent = formatter.format(subtotal);
    document.getElementById('invoiceGST').textContent = formatter.format(tax);
    document.getElementById('invoiceGrandTotal').textContent = formatter.format(total);

    // Check if send email checkbox should be checked by default
    const emailInput = document.getElementById('customerEmail');
    const sendEmailCheckbox = document.getElementById('sendInvoiceEmail');
    sendEmailCheckbox.checked = emailInput.value.trim() !== '';

    // Close service details modal if open
    const serviceDetailsModal = bootstrap.Modal.getInstance(document.getElementById('viewServiceDetailsModal'));
    if (serviceDetailsModal) {
        serviceDetailsModal.hide();
    }

    // Show invoice generation modal
    const invoiceModal = new bootstrap.Modal(document.getElementById('generateInvoiceModal'));
    invoiceModal.show();
}

function getCustomerAddress(service) {
    let address = '';

    // Try to get address from different possible locations in the service object
    if (service.enhancedCustomerData) {
        const customer = service.enhancedCustomerData;
        const addressParts = [];

        if (customer.street) addressParts.push(customer.street);
        if (customer.city) addressParts.push(customer.city);
        if (customer.state) addressParts.push(customer.state);
        if (customer.postalCode) addressParts.push(customer.postalCode);

        if (addressParts.length > 0) {
            return addressParts.join(', ');
        }
    }

    // Try alternate locations for address
    if (service.customer) {
        const customer = service.customer;
        const addressParts = [];

        if (customer.street) addressParts.push(customer.street);
        if (customer.city) addressParts.push(customer.city);
        if (customer.state) addressParts.push(customer.state);
        if (customer.postalCode) addressParts.push(customer.postalCode);

        if (addressParts.length > 0) {
            return addressParts.join(', ');
        }
    }

    // If no address found, return empty string
    return address;
}
function showSuccessMessage(title, message, onClose) {
    // First ensure any existing modals are properly closed
    const existingModals = document.querySelectorAll('.modal.show');
    existingModals.forEach(modal => {
        const instance = bootstrap.Modal.getInstance(modal);
        if (instance) instance.hide();
    });

    // Clean up any lingering backdrops
    const modalBackdrops = document.querySelectorAll('.modal-backdrop');
    modalBackdrops.forEach(backdrop => {
        backdrop.remove();
    });

    // Reset modal-related classes on body
    document.body.classList.remove('modal-open');
    document.body.style.removeProperty('padding-right');

    // Get the success modal element
    const successModal = document.getElementById('successModal');

    // If it doesn't exist, create it
    if (!successModal) {
        const modalDiv = document.createElement('div');
        modalDiv.id = 'successModal';
        modalDiv.className = 'modal fade';
        modalDiv.setAttribute('tabindex', '-1');
        modalDiv.setAttribute('aria-hidden', 'true');
        modalDiv.innerHTML = `
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header bg-success text-white">
                        <h5 class="modal-title" id="successTitle">${title}</h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body text-center py-4">
                        <div class="mb-3">
                            <i class="fas fa-check-circle fa-3x text-success"></i>
                        </div>
                        <p id="successMessage">${message}</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(modalDiv);

        // Initialize the new modal
        const newModal = new bootstrap.Modal(modalDiv);

        // Setup listeners for modal events
        modalDiv.addEventListener('hidden.bs.modal', function() {
            if (typeof onClose === 'function') {
                setTimeout(onClose, 100); // Small delay to ensure modal is fully closed
            }
        });

        // Show the modal
        newModal.show();
    } else {
        // Update existing modal content
        document.getElementById('successTitle').textContent = title;
        document.getElementById('successMessage').textContent = message;

        // Get or create modal instance
        let modalInstance = bootstrap.Modal.getInstance(successModal);
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(successModal);
        }

        // Setup listeners for modal events
        successModal.addEventListener('hidden.bs.modal', function() {
            if (typeof onClose === 'function') {
                setTimeout(onClose, 100); // Small delay to ensure modal is fully closed
            }
        });

        // Show the modal
        modalInstance.show();
    }
}

function generateInvoice() {
    if (!currentService) {
        showToast('Service data not available', 'error');
        return;
    }

    // Get email address from input field
    const email = document.getElementById('customerEmail').value.trim();

    // Validate email - this is just basic validation
    if (!email) {
        showToast('Please enter customer email', 'error');
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showToast('Please enter a valid email address', 'error');
        return;
    }

    // Get whether to send the email or not
    const sendEmail = document.getElementById('sendInvoiceEmail').checked;

    // Disable button to prevent double-clicks
    const confirmBtn = document.getElementById('confirmGenerateInvoiceBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<div class="spinner-border spinner-border-sm me-2" role="status"></div> Generating...';

    // Close the modal
    const invoiceModal = bootstrap.Modal.getInstance(document.getElementById('generateInvoiceModal'));
    invoiceModal.hide();

    // Prepare request data
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

    // Get authentication token
    const token = getAuthToken();

    // Make API call to generate invoice
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
                return response.json().then(data => {
                    throw new Error(data.error || 'Failed to generate invoice');
                });
            }
            return response.json();
        })
        .then(data => {
            // Reset button state
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="fas fa-file-invoice"></i> Generate & Send';

            // Update local service data
            if (currentService) {
                currentService.hasInvoice = true;
                currentService.invoiceId = data.invoiceId;
            }

            // Refresh the service list
            loadCompletedServices();

            // Show success message
            const successModal = new bootstrap.Modal(document.getElementById('successModal'));
            document.getElementById('successTitle').textContent = 'Invoice Generated';
            document.getElementById('successMessage').textContent = sendEmail ?
                `Invoice has been generated and sent to ${email}` :
                'Invoice has been generated successfully';
            successModal.show();

            // Automatically proceed to payment after short delay
            setTimeout(() => {
                successModal.hide();
                openPaymentModal();
            }, 2000);
        })
        .catch(error => {
            // Reset button state
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="fas fa-file-invoice"></i> Generate & Send';

            // Show error message
            showToast('Error: ' + error.message, 'error');

            // Log detailed error for debugging
            console.error('Invoice generation failed:', error);
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

function openPaymentModal() {
    if (!currentService) return;
    document.getElementById('paymentServiceId').textContent = `REQ-${currentService.requestId || currentService.serviceId}`;
    document.getElementById('paymentCustomerName').textContent = currentService.customerName;
    const total = currentService.calculatedTotal || 0;
    document.getElementById('paidAmount').value = total.toFixed(2);
    const paymentModal = new bootstrap.Modal(document.getElementById('paymentModal'));
    paymentModal.show();
}

// Update the openDeliveryModal function to prepare for address auto-fill
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

    // Pre-fetch customer address for later use
    if (currentService) {
        currentService.customerAddress = getCustomerAddress(currentService);
    }

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