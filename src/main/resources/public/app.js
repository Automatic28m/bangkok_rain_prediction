"use strict";

const PROVINCES = [
    "Bangkok", "Kamphaeng Phet", "Chai Nat", "Nakhon Nayok", "Nakhon Pathom", "Nakhon Sawan",
    "Nonthaburi", "Pathum Thani", "Phra Nakhon Si Ayutthaya", "Phichit", "Phitsanulok", "Lop Buri",
    "Samut Prakan", "Samut Songkhram", "Samut Sakhon", "Saraburi", "Sing Buri", "Suphan Buri",
    "Sukhothai", "Uthai Thani", "Ang Thong", "Phetchabun",
    "Nan", "Phayao", "Lampang", "Lamphun", "Uttaradit", "Chiang Rai", "Chiang Mai", "Phrae",
    "Mae Hong Son",
    "Kalasin", "Khon Kaen", "Chaiyaphum", "Nakhon Phanom", "Nakhon Ratchasima", "Bueng Kan",
    "Buri Ram", "Maha Sarakham", "Mukdahan", "Yasothon", "Roi Et", "Si Sa Ket", "Sakon Nakhon",
    "Surin", "Nong Khai", "Nong Bua Lam Phu", "Amnat Charoen", "Udon Thani", "Ubon Ratchathani",
    "Loei",
    "Chanthaburi", "Chachoengsao", "Chon Buri", "Trat", "Prachin Buri", "Rayong", "Sa Kaeo",
    "Krabi", "Chumphon", "Trang", "Nakhon Si Thammarat", "Narathiwat", "Pattani", "Phang Nga",
    "Phatthalung", "Phuket", "Yala", "Ranong", "Songkhla", "Satun", "Surat Thani",
    "Kanchanaburi", "Tak", "Prachuap Khiri Khan", "Ratchaburi", "Phetchaburi",
    "Ayutthaya", "Korat", "Pattaya", "Hat Yai"
];

/** Places that exist in the server's mock data set. */
const MOCK_PLACES = ["Bangkok", "Chiangmai", "Phuket"];

const MODES = {
    live: { source: "TMD live data", window: "Next 24 hours", meta: "Next 24 hours · TMD", places: PROVINCES },
    mock: { source: "Mock sample", window: "Fixed sample", meta: "Mock sample data", places: MOCK_PLACES }
};

const form = document.getElementById("risk-form");
const locationInput = document.getElementById("location");
const submitButton = document.getElementById("submit");
const submitText = document.getElementById("submit-text");
const sourceValue = document.getElementById("source-value");
const windowValue = document.getElementById("window-value");
const datalist = document.getElementById("provinces");
const resultBox = document.getElementById("result");
const tabs = Array.from(document.querySelectorAll(".tab"));

let mode = "live";

/* ---------- Tabs: switch between live and mock data ---------- */

function selectMode(next, focus) {
    mode = next;
    tabs.forEach(tab => {
        const active = tab.dataset.mode === next;
        tab.setAttribute("aria-selected", String(active));
        tab.tabIndex = active ? 0 : -1;
        if (active && focus) {
            tab.focus();
        }
    });
    sourceValue.textContent = MODES[next].source;
    windowValue.textContent = MODES[next].window;
    datalist.replaceChildren(...MODES[next].places.map(name =>
        Object.assign(document.createElement("option"), { value: name })));
    resultBox.replaceChildren();
}

tabs.forEach((tab, index) => {
    tab.addEventListener("click", () => selectMode(tab.dataset.mode, false));
    tab.addEventListener("keydown", event => {
        if (event.key === "ArrowRight" || event.key === "ArrowLeft") {
            const step = event.key === "ArrowRight" ? 1 : -1;
            const next = tabs[(index + step + tabs.length) % tabs.length];
            selectMode(next.dataset.mode, true);
            event.preventDefault();
        }
    });
});

selectMode("live", false);

/* ---------- Search: call the risk API ---------- */

form.addEventListener("submit", async event => {
    event.preventDefault();
    const params = new URLSearchParams({ location: locationInput.value.trim() });
    if (mode === "mock") {
        params.set("mock", "true");
    }

    submitButton.disabled = true;
    submitText.textContent = "Checking…";
    try {
        const response = await fetch("/api/risk?" + params);
        const body = await response.json();
        if (response.ok) {
            renderResult(body);
        } else {
            renderError(body.error || "Something went wrong (HTTP " + response.status + ")");
        }
    } catch (e) {
        renderError("Cannot reach the server");
    } finally {
        submitButton.disabled = false;
        submitText.textContent = "Check risk";
    }
});

function renderResult(risk) {
    resultBox.replaceChildren(element("article", "card " + risk.level, [
        element("div", "card-top", [
            element("span", "badge " + risk.level, risk.level),
            element("span", "card-meta", MODES[mode].meta)
        ]),
        element("h2", "card-location", risk.location),
        element("dl", "stats", [
            stat("Rain", risk.rainMm.toFixed(1) + " mm"),
            stat("Rain chance", risk.rainChancePercent + " %")
        ]),
        element("p", "reason", risk.reason)
    ]));
}

function renderError(message) {
    resultBox.replaceChildren(element("div", "card error", message));
}

function stat(label, value) {
    return element("div", null, [element("dt", null, label), element("dd", null, value)]);
}

/** Builds an element with textContent (never innerHTML), so server text cannot inject markup. */
function element(tag, className, content) {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }
    if (Array.isArray(content)) {
        node.append(...content);
    } else {
        node.textContent = content;
    }
    return node;
}

/* ---------- Fjord video: load only when motion is allowed, fade in on first frame ---------- */

const video = document.getElementById("fjord-video");
if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    const reveal = () => video.classList.add("is-ready");
    video.addEventListener("playing", reveal, { once: true });
    video.src = video.dataset.src;
    video.play().catch(() => { /* autoplay blocked: the still photo stays */ });
}
