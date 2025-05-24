$(document).ready(function() {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has("error")) {
        const errorType = urlParams.get("error");

        if (errorType === "invalid_grant" || errorType === "bad_credentials") {
            $("#alertMessage").text("Invalid password. Please check your password and try again.");
        } else if (errorType === "invalid_token" || errorType === "invalid_client") {
            $("#alertMessage").text("Email not found. Please check your email address.");
        } else if (errorType === "access_denied") {
            $("#alertMessage").text("Access denied. Your account may not have appropriate permissions.");
        } else if (errorType === "unauthorized") {
            $("#alertMessage").text("Your account is not authorized to access this system.");
        } else if (errorType === "session_expired") {
            $("#alertMessage").text("Your session has expired. Please log in again.");
        } else {
            $("#alertMessage").text("Authentication failed. Please verify your email and password.");
        }

        $("#loginAlert").show();
    }

    $("#togglePassword").click(function() {
        const passwordField = $("#password");
        const eyeIcon = $("#eyeIcon");

        if (passwordField.attr("type") === "password") {
            passwordField.attr("type", "text");
            eyeIcon.removeClass("fa-eye").addClass("fa-eye-slash");
        } else {
            passwordField.attr("type", "password");
            eyeIcon.removeClass("fa-eye-slash").addClass("fa-eye");
        }
    });

    localStorage.removeItem("jwt-token");
    sessionStorage.removeItem("jwt-token");

    $("#loginForm").submit(function(e) {
        e.preventDefault();

        const email = $("#email").val();
        const password = $("#password").val();
        const rememberMe = $("#rememberMe").is(":checked");

        if (!isValidEmail(email)) {
            $("#alertMessage").text("Please enter a valid email address.");
            $("#loginAlert").show();
            return false;
        }

        if (password.length < 6) {
            $("#alertMessage").text("Password must be at least 6 characters long.");
            $("#loginAlert").show();
            return false;
        }

        $(".btn-login").html('<span class="spinner-border spinner-border-sm mr-2" role="status" aria-hidden="true"></span> Signing in...');
        $(".btn-login").prop("disabled", true);

        const apiUrl = "http://localhost:8080/admin/api/login";

        $.ajax({
            url: apiUrl,
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                email: email,
                password: password
            }),
            success: function(response) {
                const storage = rememberMe ? localStorage : sessionStorage;

                storage.setItem("jwt-token", response.token);
                storage.setItem("user-role", response.role);
                storage.setItem("user-name", response.firstName + " " + response.lastName);
                storage.setItem("user-email", response.email);

                sessionStorage.setItem("jwt-token", response.token);

                window.location.href = "/admin/dashboard";
            },
            error: function(xhr) {
                let errorMsg = "Authentication failed. Please check your credentials.";

                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorMsg = xhr.responseJSON.message;

                    if (errorMsg.includes("Invalid email/password combination")) {
                        errorMsg = "Invalid email or password. Please try again.";
                    }
                } else if (xhr.responseText) {
                    try {
                        const responseObj = JSON.parse(xhr.responseText);

                        if (responseObj.message) {
                            if (responseObj.message.includes("{")) {
                                try {
                                    const innerJson = JSON.parse(responseObj.message.substring(
                                        responseObj.message.indexOf("{"),
                                        responseObj.message.lastIndexOf("}") + 1
                                    ));

                                    if (innerJson.message) {
                                        errorMsg = innerJson.message;

                                        if (errorMsg === "Invalid email/password combination") {
                                            errorMsg = "Invalid email or password. Please try again.";
                                        }
                                    }
                                } catch (e) {
                                    errorMsg = responseObj.message;
                                }
                            } else {
                                errorMsg = responseObj.message;
                            }
                        }
                    } catch (e) {
                        if (xhr.responseText.includes("Invalid email/password combination")) {
                            errorMsg = "Invalid email or password. Please try again.";
                        }
                    }
                }

                $("#alertMessage").text(errorMsg);
                $("#loginAlert").show();

                $(".btn-login").html('<span class="btn-text"><span>Sign in to Dashboard</span><i class="fas fa-arrow-right"></i></span>');
                $(".btn-login").prop("disabled", false);
            }
        });
    });

    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }
});