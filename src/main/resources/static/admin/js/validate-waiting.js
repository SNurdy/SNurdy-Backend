/* ============================================
   SNurdy CMS — 검증/반영대기 (validate-waiting.js)
   ============================================ */

document.addEventListener("DOMContentLoaded", () => {
    Auth.requireAuth();
    ValidateWaitingPage.init();
});

/* --- 상수 --- */
const API_BASE = "/api/v1/admin/workflows/change-requests";

const STATUS_MAP = {
    REQUESTED:   { label: "요청됨",   cls: "badge--requested" },
    VALIDATED:   { label: "검증완료", cls: "badge--validated" },
    APPLIED:     { label: "반영완료", cls: "badge--applied" },
    FAILED:      { label: "검증실패", cls: "badge--failed" },
    CANCELED:    { label: "취소됨",   cls: "badge--canceled" },
    ROLLED_BACK: { label: "롤백됨",   cls: "badge--rolledback" }
};

const OP_LABEL   = { CREATE: "생성", UPDATE: "수정", DELETE: "삭제" };
const TYPE_LABEL = { TERM: "용어", MORPHEME: "어근어미" };

const FIELD_LABELS = {
    spelling: "영문 용어", meaning: "뜻", abbreviation: "약어",
    explanation: "설명", morphemes: "어근어미 ID", glossaryId: "용어집 ID"
};

/* --- DOM --- */
const DOM = {
    statusInput: $("status"),
    sizeInput: $("size"),
    rowsEl: $("rows"),
    logEl: $("log")
};

/* --- 상태 --- */
let openDetailId = null;

/* --- 로그 헬퍼 --- */
function log(msg) { writeLog(DOM.logEl, msg); }

/* ============================================
   포맷 유틸
   ============================================ */

function statusBadge(s) {
    const m = STATUS_MAP[s] || { label: s, cls: "" };
    return `<span class="badge ${m.cls}">${m.label}</span>`;
}

function fieldLabel(k) { return FIELD_LABELS[k] || k; }

function safeJson(v) {
    if (!v) return null;
    try { return JSON.parse(v); } catch { return null; }
}

function displayValue(v) {
    if (v == null || v === "") return "-";
    if (Array.isArray(v)) return v.length ? v.join(", ") : "(없음)";
    return String(v);
}

function formatTime(iso) {
    if (!iso) return "-";
    try {
        return new Date(iso).toLocaleString("ko-KR", {
            year: "numeric", month: "2-digit", day: "2-digit",
            hour: "2-digit", minute: "2-digit"
        });
    } catch { return iso; }
}

/* ============================================
   제목 하이라이팅
   ============================================ */

function highlightTitle(title, entityType) {
    const typePrefix = entityType === "TERM" ? "용어 " : entityType === "MORPHEME" ? "어근어미 " : "";
    const patterns = [
        { re: /^(생성):\s*/,        cls: "action-tag--create" },
        { re: /^(어근어미 추가):\s*/, cls: "action-tag--morpheme" },
        { re: /^(수정):\s*/,        cls: "action-tag--update" },
        { re: /^(삭제):\s*/,        cls: "action-tag--delete" }
    ];

    for (const p of patterns) {
        const m = title.match(p.re);
        if (m) {
            const label = m[1] === "어근어미 추가" ? m[1] : typePrefix + m[1];
            return `<span class="action-tag ${p.cls}">${esc(label)}</span>${esc(title.slice(m[0].length))}`;
        }
    }
    return esc(title);
}

/* ============================================
   Diff 렌더링
   ============================================ */

function renderDiff(before, after, operation) {
    if (operation === "DELETE" && before && !after) {
        return renderSingleTable("삭제 대상 데이터", before, "diff-table__removed");
    }

    if (operation === "CREATE" && !before && after) {
        return renderSingleTable("생성할 데이터", after, "diff-table__added");
    }

    if (before && after) {
        return renderComparisonTable(before, after);
    }

    return "";
}

function renderSingleTable(title, obj, cssClass) {
    const keys = Object.keys(obj);
    if (!keys.length) return "";

    const rows = keys.map(k =>
        `<tr><td class="diff-table__field">${esc(fieldLabel(k))}</td><td class="${cssClass}">${esc(displayValue(obj[k]))}</td></tr>`
    ).join("");

    return `<div class="diff-section">
        <div class="diff-section__title">${title}</div>
        <table class="diff-table"><thead><tr><th>필드</th><th>값</th></tr></thead><tbody>${rows}</tbody></table>
    </div>`;
}

function renderComparisonTable(before, after) {
    const allKeys = [...new Set([...Object.keys(before), ...Object.keys(after)])];
    const changed = [], unchanged = [];
    allKeys.forEach(k => {
        displayValue(before[k]) === displayValue(after[k]) ? unchanged.push(k) : changed.push(k);
    });

    if (!changed.length) return `<div class="diff-section diff-section__title">변경 사항 없음</div>`;

    let rows = changed.map(k =>
        `<tr><td class="diff-table__field">${esc(fieldLabel(k))}</td><td class="diff-table__removed">${esc(displayValue(before[k]))}</td><td class="diff-table__added">${esc(displayValue(after[k]))}</td></tr>`
    ).join("");

    rows += unchanged.map(k =>
        `<tr><td class="diff-table__field">${esc(fieldLabel(k))}</td><td class="diff-table__unchanged" colspan="2">${esc(displayValue(before[k]))}</td></tr>`
    ).join("");

    return `<div class="diff-section">
        <div class="diff-section__title">변경 내역</div>
        <table class="diff-table"><thead><tr><th>필드</th><th>변경 전</th><th>변경 후</th></tr></thead><tbody>${rows}</tbody></table>
    </div>`;
}

/* ============================================
   수정 폼 (FAILED/REQUESTED)
   ============================================ */

function renderEditForm(d) {
    const after = safeJson(d.afterJson) || {};
    const isTerm = d.entityType === "TERM";

    let html = `<div class="edit-section"><div class="edit-section__title">데이터 수정 후 재검증</div><div class="edit-grid">`;
    html += `<input class="input edit-grid__full edit-title" placeholder="요청 제목" value="${esc(d.title || "")}" />`;
    html += `<input class="input edit-grid__full edit-spelling" placeholder="용어/어근어미" value="${esc(after.spelling || "")}" />`;
    html += `<input class="input edit-grid__full edit-meaning" placeholder="뜻" value="${esc(after.meaning || "")}" />`;

    if (isTerm) {
        html += `<input class="input edit-abbreviation" placeholder="약어" value="${esc(after.abbreviation || "")}" />`;
        html += `<input class="input edit-glossaryId" placeholder="용어집 ID" value="${esc(after.glossaryId || "")}" />`;
        html += `<textarea class="input textarea edit-grid__full edit-explanation" placeholder="설명">${esc(after.explanation || "")}</textarea>`;
    }

    html += `</div><div class="edit-actions">
        <button class="btn btn--primary edit-save-btn" data-id="${d.id}" data-type="${d.entityType}">수정 저장 (재검증 대기)</button>
    </div></div>`;

    return html;
}

/* ============================================
   상세 렌더
   ============================================ */

function renderDetail(d) {
    const before = safeJson(d.beforeJson);
    const after = safeJson(d.afterJson);
    const typeLabel = TYPE_LABEL[d.entityType] || d.entityType;
    const opLabel = OP_LABEL[d.operation] || d.operation;

    let html = `<div class="inline-detail__inner">`;
    html += `<div class="detail-header">
        <p class="detail-header__title">${highlightTitle(d.title, d.entityType)}</p>
        ${d.description ? `<p class="detail-header__desc">${esc(d.description)}</p>` : ""}
    </div>`;

    if (d.errorMessage) html += `<div class="alert alert--error">❌ ${esc(d.errorCode ? `[${d.errorCode}] ` : "")}${esc(d.errorMessage)}</div>`;
    if (d.warningMessage) html += `<div class="alert alert--warning">⚠️ ${esc(d.warningMessage)}</div>`;

    html += `<div class="detail-meta">`;
    html += `<div class="detail-meta__item"><div class="detail-meta__label">유형</div><div class="detail-meta__value">${typeLabel} / ${opLabel}</div></div>`;
    if (d.entityPk && d.entityPk !== "NEW") html += `<div class="detail-meta__item"><div class="detail-meta__label">대상 ID</div><div class="detail-meta__value">${esc(d.entityPk)}</div></div>`;
    html += `<div class="detail-meta__item"><div class="detail-meta__label">상태</div><div class="detail-meta__value">${statusBadge(d.status)}</div></div>`;
    html += `<div class="detail-meta__item"><div class="detail-meta__label">요청 시각</div><div class="detail-meta__value">${formatTime(d.createdAt)}</div></div>`;
    if (d.appliedAt) html += `<div class="detail-meta__item"><div class="detail-meta__label">반영 시각</div><div class="detail-meta__value">${formatTime(d.appliedAt)}</div></div>`;
    if (d.rolledBackAt) html += `<div class="detail-meta__item"><div class="detail-meta__label">롤백 시각</div><div class="detail-meta__value">${formatTime(d.rolledBackAt)}</div></div>`;
    html += `</div>`;

    html += renderDiff(before, after, d.operation);
    if (d.status === "FAILED" || d.status === "REQUESTED") html += renderEditForm(d);

    html += `</div>`;
    return html;
}

/* ============================================
   인라인 상세 토글
   ============================================ */

function closeOpenDetail() {
    const el = DOM.rowsEl.querySelector(".inline-detail");
    if (el) el.remove();
    openDetailId = null;
}

async function toggleDetail(id, afterTr) {
    if (openDetailId === id) { closeOpenDetail(); return; }
    closeOpenDetail();

    try {
        const d = await callApi(`${API_BASE}/${id}`);
        const tr = document.createElement("tr");
        tr.className = "inline-detail";
        tr.dataset.detailId = id;
        tr.innerHTML = `<td colspan="7">${renderDetail(d)}</td>`;
        afterTr.after(tr);
        openDetailId = id;

        bindEditSaveButton(tr);
        log(`상세 조회: 요청ID=${id}`);
    } catch (e) {
        log(`상세 조회 실패: ${e.message}`);
    }
}

function bindEditSaveButton(tr) {
    const saveBtn = tr.querySelector(".edit-save-btn");
    if (!saveBtn) return;

    saveBtn.addEventListener("click", async () => {
        const crId = Number(saveBtn.dataset.id);
        const isTerm = saveBtn.dataset.type === "TERM";

        const body = {
            title: tr.querySelector(".edit-title")?.value?.trim() || null,
            spelling: tr.querySelector(".edit-spelling")?.value?.trim() || null,
            meaning: tr.querySelector(".edit-meaning")?.value?.trim() || null
        };

        if (isTerm) {
            body.abbreviation = tr.querySelector(".edit-abbreviation")?.value?.trim() || null;
            body.explanation = tr.querySelector(".edit-explanation")?.value?.trim() || null;
            const gid = tr.querySelector(".edit-glossaryId")?.value?.trim();
            if (gid) body.glossaryId = Number(gid);
        }

        try {
            const r = await callApi(`${API_BASE}/${crId}`, "PUT", body);
            log(`수정 저장 완료: 요청ID=${r.id}, 상태=${r.status}`);
            await loadList();
        } catch (e) {
            log(`수정 저장 실패: ${e.message}`);
        }
    });
}

/* ============================================
   목록 조회
   ============================================ */

function summaryText(item) {
    if (item.errorMessage) return `❌ ${item.errorMessage}`;
    if (item.warningMessage) return `⚠️ ${item.warningMessage}`;
    return "-";
}

async function loadList() {
    const size = Number(DOM.sizeInput.value || 20);
    const status = DOM.statusInput.value;
    const params = new URLSearchParams({ page: "0", size: String(size) });
    if (status) params.set("status", status);

    const data = await callApi(`${API_BASE}?${params}`);
    const items = data.items || [];
    openDetailId = null;

    if (!items.length) {
        DOM.rowsEl.innerHTML = `<tr><td colspan="7" class="text--muted">데이터가 없습니다.</td></tr>`;
        return;
    }

    DOM.rowsEl.innerHTML = items.map(it => {
        let actions = it.status === "FAILED"
            ? `<button class="btn btn--sm btn--warn" data-action="detail" data-id="${it.id}">수정</button>`
            : `<button class="btn btn--sm" data-action="detail" data-id="${it.id}">상세</button>`;

        if (it.status === "REQUESTED") actions += `<button class="btn btn--sm btn--primary" data-action="validate" data-id="${it.id}">검증</button>`;
        if (it.status === "APPLIED") actions += `<button class="btn btn--sm btn--warn" data-action="rollback" data-id="${it.id}">롤백</button>`;
        if (["REQUESTED", "FAILED", "VALIDATED"].includes(it.status)) actions += `<button class="btn btn--sm btn--danger" data-action="cancel" data-id="${it.id}">취소</button>`;

        return `<tr data-row-id="${it.id}">
            <td>${it.id}</td>
            <td style="max-width:340px;">${highlightTitle(it.title, it.entityType)}</td>
            <td>${it.entityType}</td>
            <td>${it.operation}</td>
            <td>${statusBadge(it.status)}</td>
            <td style="max-width:320px;">${esc(summaryText(it))}</td>
            <td>${actions}</td>
        </tr>`;
    }).join("");

    bindRowActions();
}

/* ============================================
   행 액션 바인딩
   ============================================ */

function bindRowActions() {
    DOM.rowsEl.querySelectorAll("button").forEach(btn => {
        btn.addEventListener("click", async () => {
            const id = Number(btn.dataset.id);
            const act = btn.dataset.action;
            const parentTr = btn.closest("tr");

            switch (act) {
                case "detail":
                    await toggleDetail(id, parentTr);
                    break;

                case "validate":
                    try {
                        const r = await callApi(`${API_BASE}/${id}/validate`, "POST");
                        log(`검증 완료: 요청ID=${id}, 상태=${r.status}${r.warningMessage ? ", 경고=" + r.warningMessage : ""}`);
                        await loadList();
                    } catch (e) { log(`검증 실패: ${e.message}`); }
                    break;

                case "rollback":
                    if (!confirm(`요청ID ${id}를 롤백하시겠습니까?`)) return;
                    try {
                        const r = await callApi(`${API_BASE}/${id}/rollback`, "POST");
                        log(`롤백 완료: 요청ID=${id}, 상태=${r.status}`);
                        await loadList();
                    } catch (e) { log(`롤백 실패: ${e.message}`); }
                    break;

                case "cancel":
                    if (!confirm(`요청ID ${id}를 취소하시겠습니까?`)) return;
                    try {
                        const r = await callApi(`${API_BASE}/${id}/cancel`, "POST");
                        log(`취소 완료: 요청ID=${id}, 상태=${r.status}`);
                        await loadList();
                    } catch (e) { log(`취소 실패: ${e.message}`); }
                    break;
            }
        });
    });
}

/* ============================================
   이벤트 바인딩
   ============================================ */

function bindEvents() {
    $("searchBtn").addEventListener("click", doSearch);
    DOM.sizeInput.addEventListener("keydown", e => { if (e.key === "Enter") doSearch(); });
    DOM.statusInput.addEventListener("keydown", e => { if (e.key === "Enter") doSearch(); });
}

function doSearch() {
    loadList().catch(e => log(`조회 실패: ${e.message}`));
}

/* ============================================
   페이지 초기화
   ============================================ */

const ValidateWaitingPage = {
    init() {
        bindEvents();
        loadList().catch(e => log(`초기 조회 실패: ${e.message}`));
    }
};