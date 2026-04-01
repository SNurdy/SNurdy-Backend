/* ============================================
   SNurdy CMS — 공통 JavaScript (common.js)
   ============================================ */

const Auth = {
    getToken() {
        return sessionStorage.getItem("admin_token");
    },

    requireAuth() {
        if (!this.getToken()) {
            window.location.href = "/api/admin/login.html";
            return false;
        }
        return true;
    },

    clearAndRedirect() {
        sessionStorage.removeItem("admin_token");
        window.location.href = "/api/admin/login.html";
    }
};

/**
 * API 호출 유틸리티
 * @param {string} path - API 경로
 * @param {string} method - HTTP 메서드
 * @param {object|null} body - 요청 바디
 * @returns {Promise<any>} 응답 data 필드
 */
async function callApi(path, method = "GET", body = null) {
    const token = Auth.getToken();
    if (!token) {
        Auth.clearAndRedirect();
        throw new Error("인증 필요");
    }

    const opts = {
        method,
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + token,
            "X-Request-Id": "cms-" + Date.now()
        }
    };

    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(path, opts);

    if (res.status === 401 || res.status === 403) {
        Auth.clearAndRedirect();
        throw new Error("인증 만료");
    }

    const payload = await res.json();
    if (!res.ok || payload.success === false) {
        throw new Error(payload.message || "HTTP " + res.status);
    }

    return payload.data;
}

/* --- DOM 유틸 --- */

function $(id) {
    return document.getElementById(id);
}

function esc(v) {
    if (v == null) return "";
    return String(v)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function toInt(raw, fallback = 20) {
    const v = String(raw || "").trim();
    return /^\d+$/.test(v) ? Number(v) : fallback;
}

function qs(params) {
    const q = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
        if (v != null && String(v) !== "") q.set(k, String(v));
    });
    return q.toString();
}

function writeLog(logEl, msg) {
    logEl.textContent = `[${new Date().toLocaleTimeString()}] ${msg}\n` + logEl.textContent;
}