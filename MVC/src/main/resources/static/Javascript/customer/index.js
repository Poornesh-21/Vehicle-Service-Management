 // Customer Authentication Script for Index Page
    AOS.init();
    document.addEventListener('DOMContentLoaded', function() {
    // Check if user is logged in
    const token = sessionStorage.getItem('authToken');
    const userInfo = sessionStorage.getItem('userInfo');

    // Elements in the header
    const loginBtn = document.querySelector('a.btn-login');
    const signupBtn = document.querySelector('a.btn-signup');

    if (token && userInfo) {
    // User is logged in
    const user = JSON.parse(userInfo);

    // Replace login/signup buttons with user info
    if (loginBtn && signupBtn) {
    // Hide signup button
    signupBtn.style.display = 'none';

    // Change login button to display user name
    loginBtn.innerHTML = `<i class="bi bi-person-circle me-1"></i> ${user.firstName}`;
    loginBtn.href = '/customer/profile';
    loginBtn.classList.remove('btn-login');
    loginBtn.classList.add('btn-user');

    // Add a dropdown menu
    const dropdownHtml = `
                <div class="dropdown">
                    <button class="btn btn-user dropdown-toggle" type="button" id="userDropdown" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="bi bi-person-circle me-1"></i> ${user.firstName}
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

    // Replace login button with dropdown
    loginBtn.parentNode.innerHTML = dropdownHtml;

    // Add logout functionality
    document.getElementById('logoutBtn').addEventListener('click', function(e) {
    e.preventDefault();
    logout();
});
}
}

    // Logout function
    function logout() {
    // Clear session storage
    sessionStorage.removeItem('authToken');
    sessionStorage.removeItem('userInfo');

    // Redirect to home page
    window.location.href = '/';
}

    // Book Service button functionality
    const bookServiceBtns = document.querySelectorAll('button.btn-hero, button.btn-cta');
    bookServiceBtns.forEach(btn => {
    btn.addEventListener('click', function(e) {
    e.preventDefault();

    // Check if user is logged in
    if (token && userInfo) {
    // Redirect to book service page
    window.location.href = '/customer/bookService';
} else {
    // Redirect to login page
    window.location.href = '/authentication/login';
}
});
});
});
