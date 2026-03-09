/* ============================================
   SNurdy CMS — 요청 페이지 (request.js)
   ============================================ */

document.addEventListener("DOMContentLoaded", () => {
    Auth.requireAuth();
    RequestPage.init();
});

/* --- API 경로 --- */
const API = {
    dictionary: (tab) => `/api/v1/admin/dictionary/${tab}`,
    termDetail: (id) => `/api/v1/admin/dictionary/terms/${id}`,
    changeRequests: `/api/v1/admin/workflows/change-requests`
};

/* --- DOM 참조 --- */
const DOM = {
    listTitle: $("listTitle"),
    keyword: $("keyword"),
    sizeInput: $("size"),
    listHead: $("listHead"),
    listBody: $("listBody"),
    pageInfo: $("pageInfo"),
    countInfo: $("countInfo"),
    logBox: $("log"),
    spelling: $("spelling"),
    meaning: $("meaning"),
    glossaryPicker: $("glossaryPicker"),
    glossaryChecklist: $("glossaryChecklist"),
    abbreviation: $("abbreviation"),
    explanation: $("explanation"),
    selectedIdInput: $("selectedId"),
    morphemeBox: $("morphemeBox"),
    morphemeKeyword: $("morphemeKeyword"),
    morphemeResults: $("morphemeResults"),
    selectedMorphemesEl: $("selectedMorphemes"),
    createBtn: $("createBtn"),
    updateBtn: $("updateBtn"),
    deleteBtn: $("deleteBtn")
};

/* --- 상태 --- */
const State = {
    type: "terms",
    page: 0,
    totalPages: 1,
    selectedTermId: null,
    rows: [],
    selectedMorphemes: [],
    glossaryItems: []
};

/* --- 로그 헬퍼 --- */
function log(msg) {
    writeLog(DOM.logBox, msg);
}

/* ============================================
   용어집 선택
   ============================================ */

const GlossaryPicker = {
    getSelectedIds() {
        return Array.from(DOM.glossaryChecklist.querySelectorAll('input[type="checkbox"]:checked'))
            .map(input => Number(input.value))
            .filter(Number.isFinite);
    },

    setSelectedIds(ids) {
        const selected = new Set((ids || []).map(Number).filter(Number.isFinite));
        DOM.glossaryChecklist.querySelectorAll('input[type="checkbox"]').forEach(input => {
            input.checked = selected.has(Number(input.value));
        });
    },

    render() {
        if (!State.glossaryItems.length) {
            DOM.glossaryChecklist.innerHTML = `<div class="text--muted">용어집 목록이 없습니다.</div>`;
            return;
        }
        DOM.glossaryChecklist.innerHTML = State.glossaryItems.map(g => `
            <label class="glossary-picker__item">
                <input type="checkbox" value="${g.id}" />
                <span>${esc(g.category)} / ${esc(g.title)}</span>
            </label>
        `).join("");
    }
};

/* ============================================
   어근어미 선택 (드래그앤드롭)
   ============================================ */

const MorphemeSelector = {
    renderSelected() {
        const el = DOM.selectedMorphemesEl;

        if (State.selectedMorphemes.length === 0) {
            el.innerHTML = `<div class="text--muted">선택된 어근어미가 없습니다.</div>`;
            return;
        }

        el.innerHTML = State.selectedMorphemes.map((m, idx) => `
            <div class="morpheme-row" draggable="true" data-idx="${idx}">
                <div>${idx + 1}. ${esc(m.spelling)} (${esc(m.meaning)})</div>
                <span></span>
                <span></span>
                <button class="morpheme-row__btn" data-action="remove" data-idx="${idx}">제거</button>
            </div>
        `).join("");

        this._bindRemoveButtons(el);
        this._bindDragAndDrop(el);
    },

    renderResults(items) {
        const el = DOM.morphemeResults;

        if (!items?.length) {
            el.innerHTML = `<div class="text--muted">검색 결과가 없습니다.</div>`;
            return;
        }

        el.innerHTML = items.map(m => `
            <div class="morpheme-row">
                <div>${esc(m.spelling)} (${esc(m.meaning)})</div>
                <button class="morpheme-row__btn" data-id="${m.id}">추가</button>
                <span></span><span></span>
            </div>
        `).join("");

        el.querySelectorAll("button").forEach(btn => {
            btn.addEventListener("click", () => {
                const id = Number(btn.dataset.id);
                const found = items.find(i => i.id === id);
                if (!found) return;
                if (State.selectedMorphemes.some(m => m.id === id)) {
                    log("이미 추가된 어근어미입니다.");
                    return;
                }
                State.selectedMorphemes.push({
                    id: found.id,
                    spelling: found.spelling,
                    meaning: found.meaning
                });
                this.renderSelected();
            });
        });
    },

    _bindRemoveButtons(el) {
        el.querySelectorAll("button[data-action='remove']").forEach(btn => {
            btn.addEventListener("click", () => {
                State.selectedMorphemes.splice(Number(btn.dataset.idx), 1);
                this.renderSelected();
            });
        });
    },

    _bindDragAndDrop(el) {
        let dragIdx = null;

        el.querySelectorAll(".morpheme-row").forEach(row => {
            row.addEventListener("dragstart", (e) => {
                dragIdx = Number(row.dataset.idx);
                row.classList.add("morpheme-row--dragging");
                e.dataTransfer.effectAllowed = "move";
            });

            row.addEventListener("dragend", () => {
                row.classList.remove("morpheme-row--dragging");
                el.querySelectorAll(".morpheme-row").forEach(r =>
                    r.classList.remove("morpheme-row--drag-over")
                );
            });

            row.addEventListener("dragover", (e) => {
                e.preventDefault();
                e.dataTransfer.dropEffect = "move";
                el.querySelectorAll(".morpheme-row").forEach(r =>
                    r.classList.remove("morpheme-row--drag-over")
                );
                row.classList.add("morpheme-row--drag-over");
            });

            row.addEventListener("dragleave", () => {
                row.classList.remove("morpheme-row--drag-over");
            });

            row.addEventListener("drop", (e) => {
                e.preventDefault();
                const dropIdx = Number(row.dataset.idx);
                if (dragIdx === null || dragIdx === dropIdx) return;
                const item = State.selectedMorphemes.splice(dragIdx, 1)[0];
                State.selectedMorphemes.splice(dropIdx, 0, item);
                MorphemeSelector.renderSelected();
            });
        });
    }
};

/* ============================================
   폼
   ============================================ */

const Form = {
    clear() {
        State.selectedTermId = null;
        DOM.selectedIdInput.value = "";
        DOM.spelling.value = "";
        DOM.meaning.value = "";
        GlossaryPicker.setSelectedIds([]);
        DOM.abbreviation.value = "";
        DOM.explanation.value = "";
        State.selectedMorphemes = [];
        MorphemeSelector.renderSelected();
    },

    isTermTab() { return State.type === "terms"; },
    isMorphemeTab() { return State.type === "morphemes"; },

    updateMode() {
        if (this.isTermTab()) {
            DOM.createBtn.style.display = "";
            DOM.createBtn.textContent = "생성 요청";
            DOM.updateBtn.style.display = "";
            DOM.deleteBtn.style.display = "";
            [DOM.spelling, DOM.meaning, DOM.abbreviation, DOM.explanation, DOM.selectedIdInput]
                .forEach(el => el.style.display = "");
            DOM.glossaryPicker.style.display = "";
            DOM.morphemeBox.style.display = "";
            return;
        }

        if (this.isMorphemeTab()) {
            DOM.createBtn.style.display = "";
            DOM.createBtn.textContent = "어근어미 추가 요청";
            DOM.updateBtn.style.display = "none";
            DOM.deleteBtn.style.display = "none";
            DOM.spelling.style.display = "";
            DOM.meaning.style.display = "";
            DOM.glossaryPicker.style.display = "none";
            DOM.abbreviation.style.display = "none";
            DOM.explanation.style.display = "none";
            DOM.selectedIdInput.style.display = "none";
            DOM.morphemeBox.style.display = "none";
            return;
        }
    }
};

/* ============================================
   목록 조회
   ============================================ */

async function loadList() {
    const size = toInt(DOM.sizeInput.value, 20);
    const q = qs({ keyword: DOM.keyword.value.trim() || null, page: State.page, size });
    const data = await callApi(`${API.dictionary(State.type)}?${q}`);

    State.rows = data.items || [];
    State.totalPages = Math.max(data.totalPages || 1, 1);
    DOM.pageInfo.textContent = `페이지 ${(data.page || 0) + 1} / ${State.totalPages}`;
    DOM.countInfo.textContent = `총 ${data.totalElements || 0}건`;

    if (State.type === "terms") {
        renderTermList();
        return;
    }

    if (State.type === "morphemes") {
        renderMorphemeList();
        return;
    }
}

function renderTermList() {
    DOM.listTitle.textContent = "용어 목록";
    DOM.listHead.innerHTML = "<tr><th>ID</th><th>용어</th><th>약어</th><th>뜻</th></tr>";
    DOM.listBody.innerHTML = State.rows.map(r => `
        <tr data-id="${r.id}">
            <td>${r.id}</td>
            <td>${esc(r.spelling)}</td>
            <td>${esc(r.abbreviation || "-")}</td>
            <td>${esc(r.meaning)}</td>
        </tr>
    `).join("");

    DOM.listBody.querySelectorAll("tr").forEach(tr => {
        tr.addEventListener("click", () => selectTerm(Number(tr.dataset.id), tr));
    });
}

function renderMorphemeList() {
    DOM.listTitle.textContent = "어근어미 목록";
    DOM.listHead.innerHTML = "<tr><th>ID</th><th>어근어미</th><th>뜻</th></tr>";
    DOM.listBody.innerHTML = State.rows.map(r => `
        <tr>
            <td>${r.id}</td>
            <td>${esc(r.spelling)}</td>
            <td>${esc(r.meaning)}</td>
        </tr>
    `).join("");
}

async function selectTerm(termId, tr) {
    DOM.listBody.querySelectorAll("tr").forEach(r => r.classList.remove("selected"));
    tr.classList.add("selected");
    State.selectedTermId = termId;
    DOM.selectedIdInput.value = String(termId);

    try {
        const d = await callApi(API.termDetail(termId));
        DOM.spelling.value = d.spelling || "";
        DOM.meaning.value = d.meaning || "";
        DOM.abbreviation.value = d.abbreviation || "";
        DOM.explanation.value = d.explanation || "";
        State.selectedMorphemes = (d.morphemes || []).map(m => ({
            id: m.morphemeId,
            spelling: m.spelling,
            meaning: m.meaning
        }));
        GlossaryPicker.setSelectedIds(d.glossaryIds || []);
        MorphemeSelector.renderSelected();
    } catch (e) {
        log(`상세 조회 실패: ${e.message}`);
    }
}

/* ============================================
   워크플로우 요청 생성
   ============================================ */

function buildTitle(prefix) {
    return `${prefix}: ${DOM.spelling.value.trim() || "데이터"}`;
}

async function createRequest() {
    try {
        let body;

        if (Form.isTermTab()) {
            if (!DOM.spelling.value.trim() || !DOM.meaning.value.trim()) {
                log("영문 용어와 뜻은 필수입니다.");
                return;
            }
            const glossaryIds = GlossaryPicker.getSelectedIds();
            if (glossaryIds.length === 0) {
                log("용어집을 1개 이상 선택하세요.");
                return;
            }
            body = {
                title: buildTitle("생성"),
                description: "CMS 요청 페이지에서 용어 생성",
                entityType: "TERM",
                operation: "CREATE",
                spelling: DOM.spelling.value.trim(),
                meaning: DOM.meaning.value.trim(),
                glossaryIds,
                abbreviation: DOM.abbreviation.value.trim() || null,
                explanation: DOM.explanation.value.trim() || null,
                morphemes: State.selectedMorphemes.map(m => m.id)
            };
        } else if (Form.isMorphemeTab()) {
            if (!DOM.spelling.value.trim() || !DOM.meaning.value.trim()) {
                log("어근어미와 뜻은 필수입니다.");
                return;
            }
            body = {
                title: buildTitle("어근어미 추가"),
                description: "CMS 요청 페이지에서 어근어미 추가",
                entityType: "MORPHEME",
                operation: "CREATE",
                spelling: DOM.spelling.value.trim(),
                meaning: DOM.meaning.value.trim()
            };
        } else {
            log("현재 탭에서는 요청 생성을 지원하지 않습니다.");
            return;
        }

        const res = await callApi(API.changeRequests, "POST", body);
        log(`생성 요청 등록: 요청ID=${res.id}, 상태=${res.status}`);
        Form.clear();
    } catch (e) {
        log(`생성 요청 실패: ${e.message}`);
    }
}

async function updateRequest() {
    if (!State.selectedTermId) {
        log("수정할 용어를 목록에서 선택하세요.");
        return;
    }
    if (!DOM.spelling.value.trim() || !DOM.meaning.value.trim()) {
        log("영문 용어와 뜻은 필수입니다.");
        return;
    }

    try {
        const glossaryIds = GlossaryPicker.getSelectedIds();
        const body = {
            title: buildTitle("수정"),
            description: `기존 용어(ID=${State.selectedTermId}) 수정 요청`,
            entityType: "TERM",
            operation: "UPDATE",
            termId: State.selectedTermId,
            spelling: DOM.spelling.value.trim(),
            meaning: DOM.meaning.value.trim(),
            abbreviation: DOM.abbreviation.value.trim() || null,
            explanation: DOM.explanation.value.trim() || null,
            morphemes: State.selectedMorphemes.map(m => m.id),
            ...(glossaryIds.length > 0 ? { glossaryIds } : {})
        };
        const res = await callApi(API.changeRequests, "POST", body);
        log(`수정 요청 등록: 요청ID=${res.id}, 상태=${res.status}`);
    } catch (e) {
        log(`수정 요청 실패: ${e.message}`);
    }
}

async function deleteRequest() {
    if (!State.selectedTermId) {
        log("삭제할 용어를 목록에서 선택하세요.");
        return;
    }
    if (!confirm(`ID ${State.selectedTermId} 용어를 삭제하시겠습니까?`)) return;

    try {
        const body = {
            title: `삭제: ID=${State.selectedTermId}`,
            description: `기존 용어(ID=${State.selectedTermId}) 삭제 요청`,
            entityType: "TERM",
            operation: "DELETE",
            termId: State.selectedTermId
        };
        const res = await callApi(API.changeRequests, "POST", body);
        log(`삭제 요청 등록: 요청ID=${res.id}, 상태=${res.status}`);
    } catch (e) {
        log(`삭제 요청 실패: ${e.message}`);
    }
}

async function searchMorphemes() {
    const q = DOM.morphemeKeyword.value.trim();
    if (!q) {
        log("어근어미 검색어를 입력하세요.");
        return;
    }

    try {
        const data = await callApi(
            `${API.dictionary("morphemes")}?${qs({ keyword: q, page: 0, size: 20 })}`
        );
        MorphemeSelector.renderResults(data.items || []);
        log(`어근어미 검색: ${data.totalElements || 0}건`);
    } catch (e) {
        log(`어근어미 검색 실패: ${e.message}`);
    }
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
   탭 전환
   ============================================ */

function switchTab(tabBtn) {
    document.querySelectorAll(".tabs__btn").forEach(t => t.classList.remove("tabs__btn--active"));
    tabBtn.classList.add("tabs__btn--active");
    State.type = tabBtn.dataset.type;
    State.page = 0;
    Form.clear();
    Form.updateMode();
    loadList().catch(e => log(`조회 실패: ${e.message}`));
}

/* ============================================
   이벤트 바인딩
   ============================================ */

function bindEvents() {
    $("refreshBtn").addEventListener("click", doSearch);
    $("searchBtn").addEventListener("click", doSearch);
    $("prevBtn").addEventListener("click", prevPage);
    $("nextBtn").addEventListener("click", nextPage);
    $("newBtn").addEventListener("click", () => Form.clear());
    $("createBtn").addEventListener("click", createRequest);
    $("updateBtn").addEventListener("click", updateRequest);
    $("deleteBtn").addEventListener("click", deleteRequest);
    $("searchMorphemeBtn").addEventListener("click", searchMorphemes);

    DOM.keyword.addEventListener("keydown", (e) => { if (e.key === "Enter") doSearch(); });
    DOM.sizeInput.addEventListener("keydown", (e) => { if (e.key === "Enter") doSearch(); });
    DOM.morphemeKeyword.addEventListener("keydown", (e) => { if (e.key === "Enter") searchMorphemes(); });

    $("tabs").querySelectorAll(".tabs__btn").forEach(tab => {
        tab.addEventListener("click", () => switchTab(tab));
    });
}

/* ============================================
   페이지 초기화
   ============================================ */

const RequestPage = {
    async init() {
        bindEvents();

        try {
            const data = await callApi(`${API.dictionary("glossaries")}?page=0&size=100`);
            State.glossaryItems = data.items || [];
            GlossaryPicker.render();
        } catch (e) {
            log(`용어집 목록 로드 실패: ${e.message}`);
        }

        MorphemeSelector.renderSelected();
        Form.updateMode();
        await loadList().catch(e => log(`초기 조회 실패: ${e.message}`));
    }
};