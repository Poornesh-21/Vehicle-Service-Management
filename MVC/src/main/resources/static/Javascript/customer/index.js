/**
 * Customer Authentication Script for Index Page
 * Handles UI changes based on authentication status
 */
document.addEventListener('DOMContentLoaded', function() {
    // Initialize AOS animation library if available
    if (typeof AOS !== 'undefined') {
        AOS.init({
            duration: 800,
            easing: 'ease-in-out',
            once: true
        });
    }

    // Check auth status and update UI immediately
    updateAuthUI();

    // Handle URL parameters (e.g., messages from redirects)
    handleUrlParams();

    // Set active navbar item based on current URL
    setActiveNavItem();

    // Navbar scroll effect
    window.addEventListener('scroll', handleNavbarScroll);
    handleNavbarScroll();

    // Handle book service buttons and membership links
    updateBookServiceButtons();
    updateMembershipLinks();
});

/**
 * Set active state for navbar items based on current URL
 */
function setActiveNavItem() {
    // Get current path
    const currentPath = window.location.pathname;

    // Get all nav links
    const navLinks = document.querySelectorAll('.navbar-nav .nav-link');

    // Remove active class from all links
    navLinks.forEach(link => {
        link.classList.remove('active');
    });

    // Set active class based on current path
    navLinks.forEach(link => {
        const href = link.getAttribute('href');

        // Skip hash links when on homepage
        if (href && href.startsWith('#') && currentPath === '/') {
            // For homepage, leave hash links as is
            return;
        }

        // For absolute paths, check if current path matches
        if (href && href.startsWith('/')) {
            if (currentPath === href || currentPath.startsWith(href) && href !== '/') {
                link.classList.add('active');
            }
        }
    });

    // Special case for homepage
    if (currentPath === '/' || currentPath === '') {
        const homeLink = document.querySelector('.navbar-nav .nav-link[href="/"]');
        if (homeLink) {
            homeLink.classList.add('active');
        }
    }
}

/**
 * Main function to update UI based on authentication state
 */
function updateAuthUI() {
    console.log("Checking authentication status...");

    const token = sessionStorage.getItem('authToken');
    const userInfoStr = sessionStorage.getItem('userInfo');

    console.log("Auth token exists:", !!token);
    console.log("User info exists:", !!userInfoStr);

    if (token && userInfoStr) {
        try {
            const userInfo = JSON.parse(userInfoStr);
            console.log("User authenticated:", userInfo.firstName);

            // Update navbar with user info
            updateNavbarForLoggedInUser(userInfo);

            // Verify token in background
            validateToken(token);
        } catch (error) {
            console.error("Error processing auth data:", error);
            sessionStorage.removeItem('authToken');
            sessionStorage.removeItem('userInfo');
            updateNavbarForLoggedOutUser();
        }
    } else {
        console.log("No authentication data found");
        updateNavbarForLoggedOutUser();
    }
}

/**
 * Update navbar for logged in user
 */
function updateNavbarForLoggedInUser(userInfo) {
    // Get navigation elements
    const loginBtn = document.querySelector('.btn-login');
    const signupBtn = document.querySelector('.btn-signup');
    const authContainer = loginBtn ? loginBtn.parentElement : document.querySelector('.ms-lg-3');

    if (!authContainer) {
        console.error("Auth container not found");
        return;
    }

    // Create user dropdown HTML
    const dropdownHtml = `
        <div class="dropdown">
            <button class="btn btn-primary dropdown-toggle" type="button" id="userDropdown" data-bs-toggle="dropdown" aria-expanded="false">
                <i class="bi bi-person-circle me-1"></i> ${userInfo.firstName}
                ${userInfo.membershipType === 'PREMIUM' ? '<span class="badge bg-warning text-dark ms-1">PREMIUM</span>' : ''}
            </button>
            <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="userDropdown">
                <li><a class="dropdown-item" href="/customer/profile">My Profile</a></li>
                <li><a class="dropdown-item" href="/customer/myVehicles">My Vehicles</a></li>
                <li><a class="dropdown-item" href="/customer/serviceHistory">Service History</a></li>
                <li><hr class="dropdown-divider"></li>
                <li><a class="dropdown-item" href="#" id="logoutBtn">Logout</a></li>
            </ul>
        </div>
    `;

    // Replace auth container content
    authContainer.innerHTML = dropdownHtml;

    // Add logout handler
    document.getElementById('logoutBtn').addEventListener('click', function(e) {
        e.preventDefault();
        logout();
    });
}

/**
 * Update navbar for logged out user
 */
function updateNavbarForLoggedOutUser() {
    const authContainer = document.querySelector('.ms-lg-3') || document.querySelector('.dropdown')?.parentElement;

    if (!authContainer) {
        console.error("Auth container not found");
        return;
    }

    authContainer.innerHTML = `
        <a href="/authentication/login" class="btn btn-login me-2">Login</a>
        <a href="/authentication/login" class="btn btn-signup">Signup</a>
    `;
}

/**
 * Update book service buttons based on auth status
 */
function updateBookServiceButtons() {
    const isLoggedIn = sessionStorage.getItem('authToken') !== null;
    const buttons = document.querySelectorAll('.btn-hero, .btn-cta');

    buttons.forEach(btn => {
        const newBtn = btn.cloneNode(true);
        if (btn.parentNode) {
            btn.parentNode.replaceChild(newBtn, btn);

            newBtn.addEventListener('click', function(e) {
                e.preventDefault();

                if (isLoggedIn) {
                    window.location.href = '/customer/bookService';
                } else {
                    window.location.href = '/authentication/login?message=' +
                        encodeURIComponent('Please login to book a service') +
                        '&type=info&redirect=/customer/bookService';
                }
            });
        }
    });
}

/**
 * Update membership links to preserve auth state
 */
function updateMembershipLinks() {
    const membershipLinks = document.querySelectorAll('a[href="/customer/membership"], a[href*="membership"]');
    const token = sessionStorage.getItem('authToken');

    membershipLinks.forEach(link => {
        // Skip if already processed
        if (link.dataset.processed) return;

        link.dataset.processed = "true";

        // Store original click handler if it exists
        const originalClickHandler = link.onclick;

        link.addEventListener('click', function(e) {
            e.preventDefault();

            // If user is logged in, send directly to membership page
            if (token) {
                // Create a form to maintain the token through the request
                const form = document.createElement('form');
                form.method = 'GET';
                form.action = '/customer/membership';

                // Add the auth token as a hidden field
                const authInput = document.createElement('input');
                authInput.type = 'hidden';
                authInput.name = 'auth';
                authInput.value = 'true';
                form.appendChild(authInput);

                // Add to body and submit
                document.body.appendChild(form);

                // Set Authorization header in session storage for the next request
                if (window.sessionStorage) {
                    console.log("Redirecting to membership page with token");
                    // No need to add auth token here as it's already in sessionStorage
                    window.location.href = '/customer/membership';
                } else {
                    // Submit form if sessionStorage is not available
                    form.submit();
                }
            } else {
                // If not logged in, redirect to login with return to membership
                window.location.href = '/authentication/login?message=' +
                    encodeURIComponent('Please login to view membership options') +
                    '&type=info&redirect=/customer/membership';
            }

            // Call original handler if it exists
            if (originalClickHandler) {
                originalClickHandler.call(this, e);
            }
        });
    });
}

/**
 * Validate token with backend
 */
function validateToken(token) {
    fetch('/authentication/validate-token?token=' + token)
        .then(response => response.json())
        .then(data => {
            if (!data.valid) {
                console.warn("Token validation failed");
                logout(false);
            }
        })
        .catch(error => {
            console.error("Token validation error:", error);
        });
}

/**
 * Handle user logout
 */
function logout(showMessage = true) {
    sessionStorage.removeItem('authToken');
    sessionStorage.removeItem('userInfo');

    if (showMessage) {
        showToast('You have been logged out successfully', 'success');
    }

    updateNavbarForLoggedOutUser();

    if (showMessage) {
        setTimeout(() => {
            window.location.href = '/';
        }, 1000);
    }
}

/**
 * Show toast notification
 */
function showToast(message, type = 'info') {
    let toastContainer = document.querySelector('.toast-container');

    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        document.body.appendChild(toastContainer);
    }

    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white bg-${type === 'error' ? 'danger' : type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;

    toastContainer.insertAdjacentHTML('beforeend', toastHtml);

    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, {
        autohide: true,
        delay: 5000
    });

    toast.show();

    toastElement.addEventListener('hidden.bs.toast', function() {
        toastElement.remove();
    });
}

/**
 * Handle navbar scroll effect
 */
function handleNavbarScroll() {
    const navbar = document.querySelector('.navbar');
    if (navbar) {
        navbar.classList.toggle('scrolled', window.scrollY > 50);
    }
}

/**
 * Handle URL parameters (like message from redirects)
 */
function handleUrlParams() {
    const urlParams = new URLSearchParams(window.location.search);
    const messageParam = urlParams.get('message');
    const messageType = urlParams.get('type') || 'info';

    if (messageParam) {
        showToast(decodeURIComponent(messageParam), messageType);

        // Remove the parameters from URL
        window.history.replaceState({}, document.title, window.location.pathname);
    }
}