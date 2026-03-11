/* ============================================
   SNurdy CMS — 메인 페이지 (cms.js)
   ============================================ */

document.addEventListener("DOMContentLoaded", () => {
    Auth.requireAuth();

    $("logoutBtn").addEventListener("click", (e) => {
        e.preventDefault();
        Auth.clearAndRedirect();
    });
});