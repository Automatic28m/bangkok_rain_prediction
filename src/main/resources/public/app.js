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

const form = document.getElementById("risk-form");
const locationInput = document.getElementById("location");
const mockInput = document.getElementById("mock");
const submitButton = document.getElementById("submit");
const resultBox = document.getElementById("result");

document.getElementById("provinces").append(
    ...PROVINCES.map(name => Object.assign(document.createElement("option"), { value: name }))
);

form.addEventListener("submit", async event => {
    event.preventDefault();
    const params = new URLSearchParams({ location: locationInput.value.trim() });
    if (mockInput.checked) {
        params.set("mock", "true");
    }

    submitButton.disabled = true;
    submitButton.textContent = "กำลังโหลด…";
    try {
        const response = await fetch("/api/risk?" + params);
        const body = await response.json();
        if (response.ok) {
            renderResult(body);
        } else {
            renderError(body.error || "เกิดข้อผิดพลาด (HTTP " + response.status + ")");
        }
    } catch (e) {
        renderError("ติดต่อเซิร์ฟเวอร์ไม่ได้");
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = "ตรวจสอบ";
    }
});

function renderResult(risk) {
    resultBox.replaceChildren(element("div", "card result " + risk.level, [
        element("h2", null, [
            element("span", "badge " + risk.level, risk.level),
            element("span", null, risk.location)
        ]),
        element("dl", "stats", [
            stat("ปริมาณฝน", risk.rainMm.toFixed(1) + " มม."),
            stat("โอกาสฝนตก", risk.rainChancePercent + " %")
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
