/* ============================================
   SNurdy CMS — 변경이력 페이지 (audit-log.js)
   ============================================ */

document.addEventListener("DOMContentLoaded", () => {
    Auth.requireAuth();
    AuditLogPage.init();
});

/* --- 상수 --- */
const API_BASE = "/api/v1/admin/audit-logs";

const ACTION_BADGE_MAP = {
    REQUEST: "badge--request",
    UPDATE: "badge--update",
    VALIDATE: "badge--validate",
    APPLY: "badge--apply",
    ROLLBACK: "badge--rollback",
    CANCEL: "badge--cancel"
};

const FIELD_LABELS = {
    spelling: "영문", meaning: "뜻", abbreviation: "약어", explanation: "설명",
    morphemes: "어근어미 ID", glossaryId: "용어집 ID", glossaryIds: "용어집 ID",
    title: "제목", status: "상태", entityType: "유형", operation: "작업", entityPk: "대상 PK",
    errorCode: "에러코드", errorMessage: "에러메시지", warningMessage: "경고",
    createdAt: "생성시각", updatedAt: "수정시각", id: "ID"
};

const SKIP_FIELDS = new Set([
    "isEditable", "isApplicable", "isCancelable", "isValidatable",
    "isRollbackable", "requestedBy", "appliedBy", "description"
]);

/* --- DOM 참조 --- */
const DOM = {
    actionInput: $("action"),
    sizeInput: $("size"),
    rowsEl: $("rows"),
    logEl: $("log"),
    pageInfoEl: $("pageInfo"),
    countInfoEl: $("countInfo"),
    modalOverlay: $("modalOverlay"),
    modalContent: $("modalContent"),
    modalClose: $("modalClose")
};

/* --- 상태 --- */
const State = {
    page: 0,
    totalPages: 1,
    allItems: []
};

/* --- 로그 헬퍼 --- */
function log(msg) {
    writeLog(DOM.logEl, msg);
}

/* ============================================
   포맷 유틸
   ============================================ */

function formatTime(iso) {
    if (!iso) return "-";
    try {
        const d = new Date(iso);
        const y = d.getFullYear();
        const mo = String(d.getMonth() + 1).padStart(2, "0");
        const da = String(d.getDate()).padStart(2, "0");
        const h = String(d.getHours()).padStart(2, "0");
        const mi = String(d.getMinutes()).padStart(2, "0");
        const s = String(d.getSeconds()).padStart(2, "0");
        return `${y}년 ${mo}월 ${da}일 ${h}:${mi}:${s}`;
    } catch { return iso; }
}

function safeJson(v) {
    if (!v) return null;
    try { return JSON.parse(v); } catch { return null; }
}

function fieldLabel(k) { return FIELD_LABELS[k] || k; }

function displayValue(v) {
    if (v == null || v === "") return "-";
    if (Array.isArray(v)) return v.length ? v.join(", ") : "(없음)";
    if (typeof v === "object") return JSON.stringify(v);
    return String(v);
}

function formatFieldValue(key, val) {
    if (typeof val === "string" && (key.endsWith("At") || key === "createdAt" || key === "updatedAt")) {
        return formatTime(val);
    }
    return displayValue(val);
}

function actionBadge(action) {
    const cls = ACTION_BADGE_MAP[action] || "";
    return `<span class="badge ${cls}">${action}</span>`;
}

/* ============================================
   변경 내용 요약 (테이블 컬럼용)
   ============================================ */

function summarizeChange(item) {
    const after = safeJson(item.afterJson);
    if (!after) return "-";

    const parts = [];
    if (after.spelling) parts.push(after.spelling);
    if (after.meaning) parts.push(after.meaning);
    if (after.title) parts.push(after.title);
    if (after.status) parts.push(after.status);
    if (parts.length === 0) return "-";

    const text = parts.join(" / ");
    return text.length > 50 ? esc(text.substring(0, 50)) + "…" : esc(text);
}

/* ============================================
   모달
   ============================================ */

const Modal = {
    open(html) {
        DOM.modalContent.innerHTML = html;
        DOM.modalOverlay.classList.add("modal-overlay--open");
    },

    close() {
        DOM.modalOverlay.classList.remove("modal-overlay--open");
        DOM.modalContent.innerHTML = "";
    }
};

/* ============================================
   렌더링 — 데이터 테이블 / Diff
   ============================================ */

function renderDataTable(title, obj, cssClass) {
    if (!obj || typeof obj !== "object") return "";

    const keys = Object.keys(obj).filter(k => !SKIP_FIELDS.has(k));
    if (!keys.length) return "";

    const rows = keys.map(k => {
        const val = formatFieldValue(k, obj[k]);
        return `<tr>
            <td class="diff-table__field">${esc(fieldLabel(k))}</td>
            <td class="${cssClass || ""}">${esc(val)}</td>
        </tr>`;
    }).join("");

    return `
        <div class="detail-section">
            <div class="detail-section__title">${title}</div>
            <table class="diff-table">
                <thead><tr><th>필드</th><th>값</th></tr></thead>
                <tbody>${rows}</tbody>
            </table>
        </div>`;
}

function renderDiff(before, after) {
    if (!before || !after) return "";

    const allKeys = [...new Set([...Object.keys(before), ...Object.keys(after)])]
        .filter(k => !SKIP_FIELDS.has(k));

    const changed = [];
    const unchanged = [];
    allKeys.forEach(k => {
        displayValue(before[k]) === displayValue(after[k])
            ? unchanged.push(k)
            : changed.push(k);
    });

    if (!changed.length) {
        return `<div class="detail-section text--muted">변경 사항 없음</div>`;
    }

    const changedRows = changed.map(k => {
        const bv = formatFieldValue(k, before[k]);
        const av = formatFieldValue(k, after[k]);
        return `<tr>
            <td class="diff-table__field">${esc(fieldLabel(k))}</td>
            <td class="diff-table__removed">${esc(bv)}</td>
            <td class="diff-table__added">${esc(av)}</td>
        </tr>`;
    }).join("");

    const unchangedRow = unchanged.length > 3
        ? `<tr><td class="text--muted" colspan="3">외 ${unchanged.length}개 필드 변경 없음</td></tr>`
        : "";

    return `
        <div class="detail-section">
            <div class="detail-section__title">변경 내역</div>
            <table class="diff-table">
                <thead><tr><th>필드</th><th>변경 전</th><th>변경 후</th></tr></thead>
                <tbody>${changedRows}${unchangedRow}</tbody>
            </table>
        </div>`;
}

/* ============================================
   모달 상세 렌더
   ============================================ */

function renderDetail(item) {
    const before = safeJson(item.beforeJson);
    const after = safeJson(item.afterJson);

    let html = `<div class="modal__title">${actionBadge(item.action)} 상세</div>`;
    html += `<div style="font-size:13px;color:var(--muted);margin-bottom:12px;">${formatTime(item.createdAt)}</div>`;

    if (before && after) {
        html += renderDiff(before, after);
    } else {
        if (before) html += renderDataTable("변경 전 데이터", before, "diff-table__removed");
        if (after) html += renderDataTable("변경 후 데이터", after, "diff-table__added");
    }

    return html;
}

/* ============================================
   목록 조회
   ============================================ */

async function loadList() {
    const size = Number(DOM.sizeInput.value || 20);
    const params = new URLSearchParams({ page: String(State.page), size: String(size) });

    const action = DOM.actionInput.value;
    if (action) params.set("action", action);

    const data = await callApi(`${API_BASE}?${params}`);

    State.allItems = data.items || [];
    State.totalPages = Math.max(data.totalPages || 1, 1);
    DOM.pageInfoEl.textContent = `페이지 ${(data.page || 0) + 1} / ${State.totalPages}`;
    DOM.countInfoEl.textContent = `총 ${data.totalElements || 0}건`;

    if (!State.allItems.length) {
        DOM.rowsEl.innerHTML = `<tr><td colspan="5" class="text--muted">데이터가 없습니다.</td></tr>`;
        return;
    }

    DOM.rowsEl.innerHTML = State.allItems.map(it => `
        <tr>
            <td>${it.id}</td>
            <td>${actionBadge(it.action)}</td>
            <td>${summarizeChange(it)}</td>
            <td>${formatTime(it.createdAt)}</td>
            <td><button class="btn btn--sm" data-id="${it.id}">상세</button></td>
        </tr>
    `).join("");

    DOM.rowsEl.querySelectorAll("button").forEach(btn => {
        btn.addEventListener("click", () => {
            const item = State.allItems.find(i => i.id === Number(btn.dataset.id));
            if (!item) return;
            Modal.open(renderDetail(item));
            log(`상세 조회: 로그ID=${item.id}`);
        });
    });
}

/* ============================================
   검색 & 페이징
   ============================================ */

function doSearch() {
    State.page = 0;
    loadList().catch(e => log(`조회 실패: ${e.message}`));
}

function prevPage() {
    if (State.page > 0) {
        State.page--;
        loadList().catch(e => log(`조회 실패: ${e.message}`));
    }
}

function nextPage() {
    if (State.page + 1 < State.totalPages) {
        State.page++;
        loadList().catch(e => log(`조회 실패: ${e.message}`));
    }
}

/* ============================================
   이벤트 바인딩
   ============================================ */

function bindEvents() {
    $("searchBtn").addEventListener("click", doSearch);
    $("prevBtn").addEventListener("click", prevPage);
    $("nextBtn").addEventListener("click", nextPage);

    DOM.sizeInput.addEventListener("keydown", e => { if (e.key === "Enter") doSearch(); });

    // 모달
    DOM.modalClose.addEventListener("click", () => Modal.close());
    DOM.modalOverlay.addEventListener("click", e => {
        if (e.target === DOM.modalOverlay) Modal.close();
    });
    document.addEventListener("keydown", e => {
        if (e.key === "Escape") Modal.close();
    });
}

/* ============================================
   페이지 초기화
   ============================================ */

const AuditLogPage = {
    init() {
        bindEvents();
        loadList().catch(e => log(`초기 조회 실패: ${e.message}`));
    }
};