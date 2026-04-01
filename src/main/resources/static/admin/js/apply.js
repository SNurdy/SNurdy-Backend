/* ============================================
   SNurdy CMS — 반영 페이지 (apply.js)
   ============================================ */

document.addEventListener("DOMContentLoaded", () => {
    Auth.requireAuth();
    ApplyPage.init();
});

const API_BASE = "/api/v1/admin/workflows/change-requests";

const DOM = {
    sizeInput: $("size"),
    rowsEl: $("rows"),
    logEl: $("log")
};

function log(msg) { writeLog(DOM.logEl, msg); }

async function loadList() {
    const size = Number(DOM.sizeInput.value || 20);
    const params = new URLSearchParams({ page: "0", size: String(size), status: "VALIDATED" });
    const data = await callApi(`${API_BASE}?${params}`);
    const items = data.items || [];

    if (items.length === 0) {
        DOM.rowsEl.innerHTML = `<tr><td colspan="7" class="text--muted">반영 대기 데이터가 없습니다.</td></tr>`;
        return;
    }

    DOM.rowsEl.innerHTML = items.map(it => `
        <tr>
            <td>${it.id}</td>
            <td>${esc(it.title)}</td>
            <td>${it.entityType}</td>
            <td>${it.operation}</td>
            <td><span class="badge badge--validated">검증완료</span></td>
            <td>${it.warningMessage ? `<span class="warn-text">⚠️ ${esc(it.warningMessage)}</span>` : "-"}</td>
            <td>
                <button class="btn btn--apply btn--sm" data-apply-id="${it.id}">반영</button>
                <button class="btn btn--danger btn--sm" data-cancel-id="${it.id}" style="margin-left:6px;">취소</button>
            </td>
        </tr>
    `).join("");

    bindActions();
}

function bindActions() {
    DOM.rowsEl.querySelectorAll("button[data-apply-id]").forEach(btn => {
        btn.addEventListener("click", async () => {
            const id = Number(btn.dataset.applyId);
            if (!confirm(`요청ID ${id}를 반영하시겠습니까?`)) return;
            try {
                const r = await callApi(`${API_BASE}/${id}/apply`, "POST");
                log(`반영 완료: 요청ID=${id}, 상태=${r.status}`);
                await loadList();
            } catch (e) { log(`반영 실패: ${e.message}`); }
        });
    });

    DOM.rowsEl.querySelectorAll("button[data-cancel-id]").forEach(btn => {
        btn.addEventListener("click", async () => {
            const id = Number(btn.dataset.cancelId);
            if (!confirm(`요청ID ${id}를 취소하시겠습니까?`)) return;
            try {
                const r = await callApi(`${API_BASE}/${id}/cancel`, "POST");
                log(`취소 완료: 요청ID=${id}, 상태=${r.status}`);
                await loadList();
            } catch (e) { log(`취소 실패: ${e.message}`); }
        });
    });
}

function doSearch() { loadList().catch(e => log(`조회 실패: ${e.message}`)); }

function bindEvents() {
    $("searchBtn").addEventListener("click", doSearch);
    DOM.sizeInput.addEventListener("keydown", e => { if (e.key === "Enter") doSearch(); });
}

const ApplyPage = {
    init() {
        bindEvents();
        loadList().catch(e => log(`초기 조회 실패: ${e.message}`));
    }
};