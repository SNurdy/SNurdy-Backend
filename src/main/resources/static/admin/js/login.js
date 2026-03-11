/* ============================================
   SNurdy CMS — 로그인 페이지 (login.js)
   ============================================ */

document.addEventListener("DOMContentLoaded", () => {
    const emailInput = $("email");
    const passwordInput = $("password");
    const loginBtn = $("loginBtn");
    const errorEl = $("error");

    async function doLogin() {
        errorEl.style.display = "none";
        loginBtn.disabled = true;

        try {
            const res = await fetch("/api/v1/admin/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    email: emailInput.value.trim(),
                    password: passwordInput.value
                })
            });

            const payload = await res.json();
            if (!res.ok || !payload.success) {
                throw new Error(payload.message || "로그인 실패");
            }

            sessionStorage.setItem("admin_token", payload.data.accessToken);
            window.location.href = "/api/admin/cms.html";
        } catch (e) {
            errorEl.textContent = e.message;
            errorEl.style.display = "block";
        } finally {
            loginBtn.disabled = false;
        }
    }

    loginBtn.addEventListener("click", doLogin);
    passwordInput.addEventListener("keydown", e => { if (e.key === "Enter") doLogin(); });
    emailInput.addEventListener("keydown", e => { if (e.key === "Enter") passwordInput.focus(); });
});