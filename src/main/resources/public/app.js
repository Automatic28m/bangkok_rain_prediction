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
    live: { source: "กรมอุตุนิยมวิทยา", window: "24 ชั่วโมงข้างหน้า", meta: "24 ชั่วโมงข้างหน้า · พยากรณ์จากกรมอุตุนิยมวิทยา", places: PROVINCES },
    mock: { source: "ข้อมูลจำลอง", window: "ชุดตัวอย่างคงที่", meta: "ข้อมูลจำลองสำหรับทดสอบ", places: MOCK_PLACES }
};

/** Thai names for the API's risk levels. */
const LEVEL_TH = { SAFE: "ปลอดภัย", WARNING: "เฝ้าระวัง", DANGER: "อันตราย" };

/** Server messages are English; show them in Thai, falling back to the original text. */
const ERROR_TH = [
    [/^Missing location$/, () => "กรุณาระบุสถานที่"],
    [/^Location not found: (.*)$/, m => "ไม่พบสถานที่: " + m[1] + " (พิมพ์ชื่อจังหวัดภาษาอังกฤษ หรือพิกัด lat,lon)"],
    [/^TMD_API_TOKEN is not set/, () => "เซิร์ฟเวอร์ยังไม่ได้ตั้งค่า TMD_API_TOKEN ใส่ token ในไฟล์ .env แล้วรีสตาร์ต หรือเลือกแท็บข้อมูลจำลอง"],
    [/^Invalid or missing API token$/, () => "API token ไม่ถูกต้อง ตรวจสอบค่า TMD_API_TOKEN ในไฟล์ .env"],
    [/^Too many requests/, () => "เรียกใช้บริการพยากรณ์ถี่เกินไป กรุณารอประมาณ 1 นาทีแล้วลองใหม่"],
    [/^Cannot reach weather service \(timeout\)$/, () => "ติดต่อบริการพยากรณ์ไม่ได้ (หมดเวลารอ)"],
    [/^Cannot reach weather service$/, () => "ติดต่อบริการพยากรณ์ไม่ได้"],
    [/^Request was interrupted$/, () => "คำขอถูกยกเลิก กรุณาลองใหม่"],
    [/^Unexpected response from weather service$/, () => "ข้อมูลจากบริการพยากรณ์ผิดรูปแบบ"],
    [/^Weather service returned HTTP (\d+)$/, m => "บริการพยากรณ์ตอบกลับผิดปกติ (HTTP " + m[1] + ")"]
];

const REASON_TH = [
    [/^Rain ([\d.]+) mm >= ([\d.]+) mm and rain chance (\d+) % >= (\d+) %$/,
        m => "ฝน " + m[1] + " มม. ≥ " + m[2] + " มม. และโอกาสฝน " + m[3] + "% ≥ " + m[4] + "%"],
    [/^Rain ([\d.]+) mm >= ([\d.]+) mm$/, m => "ฝน " + m[1] + " มม. ≥ เกณฑ์ " + m[2] + " มม."],
    [/^Rain ([\d.]+) mm and rain chance (\d+) % are below warning thresholds$/,
        m => "ฝน " + m[1] + " มม. และโอกาสฝน " + m[2] + "% ต่ำกว่าเกณฑ์เฝ้าระวัง"]
];

function translate(text, table) {
    for (const [pattern, toThai] of table) {
        const match = pattern.exec(text);
        if (match) {
            return toThai(match);
        }
    }
    return text;
}

/* Status icons: shape differs per level, so state never relies on colour alone. */
const STATUS = {
    SAFE: { color: "var(--good)", icon: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="11" fill="var(--good)"/><path d="m7 12.5 3.2 3.2L17 9" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>' },
    WARNING: { color: "var(--warning)", icon: '<svg viewBox="0 0 24 24"><path d="M12 1.8 23 21.2H1Z" fill="var(--warning)" stroke-linejoin="round"/><path d="M12 9v5.5" stroke="#111313" stroke-width="2.2" stroke-linecap="round"/><circle cx="12" cy="17.7" r="1.3" fill="#111313"/></svg>' },
    DANGER: { color: "var(--critical)", icon: '<svg viewBox="0 0 24 24"><path d="M7.4 1h9.2L23 7.4v9.2L16.6 23H7.4L1 16.6V7.4Z" fill="var(--critical)"/><path d="M12 6.5v7" stroke="#fff" stroke-width="2.4" stroke-linecap="round"/><circle cx="12" cy="17.3" r="1.4" fill="#fff"/></svg>' }
};

const $ = id => document.getElementById(id);
const form = $("risk-form");
const locationInput = $("location");
const submitButton = $("submit");
const submitText = $("submit-text");
const stage = $("stage");
const errorBox = $("dash-error");
const tooltip = $("tooltip");
const tabs = Array.from(document.querySelectorAll(".tab"));

let mode = "live";
let current = null;          // last successful response, re-drawn on resize
let currentMode = "live";    // mode that produced `current`

/* ======================= Tabs ======================= */

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
    $("source-value").textContent = MODES[next].source;
    $("window-value").textContent = MODES[next].window;
    $("provinces").replaceChildren(...MODES[next].places.map(name =>
        Object.assign(document.createElement("option"), { value: name })));
}

tabs.forEach((tab, index) => {
    tab.addEventListener("click", () => selectMode(tab.dataset.mode, false));
    tab.addEventListener("keydown", event => {
        if (event.key === "ArrowRight" || event.key === "ArrowLeft") {
            const step = event.key === "ArrowRight" ? 1 : -1;
            selectMode(tabs[(index + step + tabs.length) % tabs.length].dataset.mode, true);
            event.preventDefault();
        }
    });
});

/* ======================= Search ======================= */

form.addEventListener("submit", event => {
    event.preventDefault();
    check(locationInput.value.trim());
});

async function check(location) {
    const params = new URLSearchParams({ location });
    if (mode === "mock") {
        params.set("mock", "true");
    }
    const requestMode = mode;

    submitButton.disabled = true;
    submitText.textContent = "กำลังตรวจสอบ…";
    stage.setAttribute("aria-busy", "true");   // keeps the previous render, dimmed
    try {
        const response = await fetch("/api/risk?" + params);
        const body = await response.json();
        if (!response.ok) {
            showError(body.error ? translate(body.error, ERROR_TH) : "เกิดข้อผิดพลาด (HTTP " + response.status + ")");
            return;
        }
        errorBox.hidden = true;
        current = body;
        currentMode = requestMode;
        render();
    } catch (e) {
        showError("ติดต่อเซิร์ฟเวอร์ไม่ได้");
    } finally {
        submitButton.disabled = false;
        submitText.textContent = "ตรวจสอบ";
        stage.setAttribute("aria-busy", "false");
    }
}

function showError(message) {
    errorBox.textContent = message;
    errorBox.hidden = false;
}

/* ======================= Dashboard ======================= */

function render() {
    if (!current) {
        return;
    }
    renderSummary(current);
    renderRules(current);
    drawHourly($("chart-hourly"), current);
    drawCumulative($("chart-cumulative"), current);
    drawMatrix($("chart-matrix"), current);
    renderTable($("table-hourly"), current);
}

function renderSummary(d) {
    const t = d.thresholds;
    $("status-icon").innerHTML = STATUS[d.level].icon;
    $("status-label").textContent = LEVEL_TH[d.level];
    $("place").textContent = d.location;
    $("meta").textContent = MODES[currentMode].meta;
    $("total").textContent = fmt(d.rainMm);
    $("chance").textContent = d.rainChancePercent + "%";
    $("reason").textContent = translate(d.reason, REASON_TH);

    const hours = d.hourly;
    if (hours.length) {
        const peak = hours.reduce((a, b) => (b.rainMm > a.rainMm ? b : a));
        const rainy = hours.filter(h => h.rainMm >= 0.1).length;
        $("peak").replaceChildren(peak.rainMm > 0 ? fmt(peak.rainMm) + " มม." : "ไม่มี",
            small(peak.rainMm > 0 ? "เวลา " + hourOf(peak.time) + " น." : "ไม่คาดว่าจะมีฝน"));
        $("rainy").replaceChildren(rainy + " / " + hours.length, small("ชั่วโมงที่ฝน ≥ 0.1 มม."));
    } else {
        $("peak").textContent = "–";
        $("rainy").textContent = "–";
    }

    // Meter: total rain on a scale that always shows the danger level.
    const scaleMax = niceMax(Math.max(d.rainMm, t.dangerRainMm) * 1.15);
    const fill = $("meter-fill");
    fill.style.width = Math.min(100, d.rainMm / scaleMax * 100) + "%";
    fill.style.background = STATUS[d.level].color;
    $("meter").setAttribute("aria-label", "ฝนรวม " + fmt(d.rainMm) + " มม. เกณฑ์เฝ้าระวัง "
        + t.warningRainMm + " มม. เกณฑ์อันตราย " + t.dangerRainMm + " มม.");
    $("meter-ticks").replaceChildren(
        tick(0, "0", scaleMax),
        tick(t.warningRainMm, "เฝ้าระวัง " + t.warningRainMm, scaleMax),
        tick(t.dangerRainMm, "อันตราย " + t.dangerRainMm, scaleMax),
        tick(scaleMax, scaleMax + " มม.", scaleMax, "end"));
}

function tick(value, label, max, extra) {
    const el = document.createElement("span");
    el.className = "meter-tick" + (extra ? " " + extra : "");
    el.style.left = (value / max * 100) + "%";
    el.textContent = label;
    if (value === 0) {
        el.style.transform = "none";
        el.classList.add("start");
    }
    return el;
}

function renderRules(d) {
    const t = d.thresholds;
    const rules = [
        ["DANGER", "ฝน ≥ " + t.dangerRainMm + " มม. หรือ ≥ " + t.dangerCombinedRainMm
            + " มม. และโอกาสฝน ≥ " + t.dangerCombinedChancePercent + "%"],
        ["WARNING", "ฝน ≥ " + t.warningRainMm + " มม. หรือ ≥ " + t.warningCombinedRainMm
            + " มม. และโอกาสฝน ≥ " + t.warningCombinedChancePercent + "%"],
        ["SAFE", "ต่ำกว่าเกณฑ์เฝ้าระวังทั้งสองข้อ"]
    ];
    $("rules").replaceChildren(...rules.map(([level, text]) => {
        const li = document.createElement("li");
        const icon = document.createElement("span");
        icon.className = "status-icon";
        icon.innerHTML = STATUS[level].icon;
        const body = document.createElement("span");
        body.append(Object.assign(document.createElement("strong"), { textContent: LEVEL_TH[level] }), text);
        li.append(icon, body);
        if (level === d.level) {
            li.classList.add("is-current");
            li.append(Object.assign(document.createElement("span"),
                { className: "current-tag", textContent: "ผลครั้งนี้" }));
        }
        return li;
    }));
}

function renderTable(container, d) {
    let running = 0;
    const rows = d.hourly.map(h => {
        running += h.rainMm;
        return "<tr><td>" + hourOf(h.time) + "</td><td>" + fmt(h.rainMm) + "</td><td>"
            + fmt(running) + "</td></tr>";
    }).join("");
    container.innerHTML = d.hourly.length
        ? "<table><thead><tr><th>เวลา</th><th>ฝน (มม.)</th><th>สะสม (มม.)</th></tr></thead><tbody>"
            + rows + "</tbody></table>"
        : '<p class="empty-note">แหล่งข้อมูลนี้ไม่มีข้อมูลรายชั่วโมง</p>';
}

$("table-toggle").addEventListener("click", event => {
    const showTable = event.currentTarget.getAttribute("aria-pressed") !== "true";
    event.currentTarget.setAttribute("aria-pressed", String(showTable));
    $("table-hourly").hidden = !showTable;
    $("chart-hourly").hidden = showTable;
    if (!showTable) {
        render();
    }
});

/* ======================= Charts (plain SVG) ======================= */

const SVG_NS = "http://www.w3.org/2000/svg";

function svg(tag, attrs, parent) {
    const node = document.createElementNS(SVG_NS, tag);
    for (const [key, value] of Object.entries(attrs || {})) {
        if (key === "text") {
            node.textContent = value;
        } else {
            node.setAttribute(key, value);
        }
    }
    if (parent) {
        parent.append(node);
    }
    return node;
}

function frame(container, margin) {
    container.replaceChildren();
    const width = container.clientWidth;
    const height = container.clientHeight;
    const root = svg("svg", { width, height, viewBox: "0 0 " + width + " " + height, role: "img" }, container);
    const plot = svg("g", { transform: "translate(" + margin.left + "," + margin.top + ")" }, root);
    return {
        root, plot,
        w: Math.max(10, width - margin.left - margin.right),
        h: Math.max(10, height - margin.top - margin.bottom)
    };
}

function emptyChart(container, message) {
    container.replaceChildren(Object.assign(document.createElement("p"),
        { className: "empty-note", textContent: message }));
}

/** Column per hour, rounded data-end, peak labelled. */
function drawHourly(container, d) {
    if (container.hidden) {
        return;
    }
    const hours = d.hourly;
    if (!hours.length) {
        emptyChart(container, "แหล่งข้อมูลนี้ไม่มีข้อมูลรายชั่วโมง");
        return;
    }
    const { root, plot, w, h } = frame(container, { top: 18, right: 4, bottom: 22, left: 34 });
    const max = niceMax(Math.max(...hours.map(x => x.rainMm), 1));
    const y = v => h - v / max * h;
    const band = w / hours.length;
    const barWidth = Math.max(2, Math.min(24, band - 2));

    yGrid(plot, ticksFor(max), y, w, v => fmt(v, true));

    const peakIndex = hours.reduce((best, x, i) => (x.rainMm > hours[best].rainMm ? i : best), 0);
    const bars = hours.map((x, i) => {
        const x0 = i * band + (band - barWidth) / 2;
        return svg("path", { class: "bar", d: columnPath(x0, y(x.rainMm), barWidth, h - y(x.rainMm)) }, plot);
    });
    svg("line", { class: "axis", x1: 0, x2: w, y1: h, y2: h }, plot);

    hours.forEach((x, i) => {
        if (i % 3 === 0) {
            svg("text", { x: i * band + band / 2, y: h + 16, "text-anchor": "middle", text: hourOf(x.time) }, plot);
        }
    });
    if (hours[peakIndex].rainMm > 0) {
        svg("text", {
            class: "value", x: peakIndex * band + band / 2, y: y(hours[peakIndex].rainMm) - 6,
            "text-anchor": "middle", text: fmt(hours[peakIndex].rainMm)
        }, plot);
    }

    // hit targets: the whole column band, taller than the bar
    hours.forEach((x, i) => {
        const hit = svg("rect", { class: "hit", x: i * band, y: 0, width: band, height: h }, plot);
        hit.addEventListener("mouseenter", () => {
            bars[i].classList.add("is-active");
            showTip(container, i * band + band / 2 + 34, y(x.rainMm) + 18,
                "<strong>" + hourOf(x.time) + " น.</strong> · ฝน " + fmt(x.rainMm) + " มม.");
        });
        hit.addEventListener("mouseleave", () => {
            bars[i].classList.remove("is-active");
            hideTip();
        });
    });

    root.setAttribute("aria-label", "ฝนรายชั่วโมง สูงสุด " + fmt(hours[peakIndex].rainMm)
        + " มม. เวลา " + hourOf(hours[peakIndex].time) + " น. กดปุ่มตารางเพื่อดูทุกค่า");
}

/** Running total with warning/danger reference lines and a crosshair. */
function drawCumulative(container, d) {
    const hours = d.hourly;
    if (!hours.length) {
        emptyChart(container, "แหล่งข้อมูลนี้ไม่มีข้อมูลรายชั่วโมง");
        return;
    }
    const t = d.thresholds;
    const margin = { top: 14, right: 12, bottom: 22, left: 34 };
    const { root, plot, w, h } = frame(container, margin);

    let sum = 0;
    const totals = hours.map(x => (sum += x.rainMm));
    const max = niceMax(Math.max(sum, t.dangerRainMm) * 1.08);
    const x = i => (hours.length === 1 ? w / 2 : i / (hours.length - 1) * w);
    const y = v => h - v / max * h;

    yGrid(plot, ticksFor(max), y, w, v => fmt(v, true));
    svg("line", { class: "axis", x1: 0, x2: w, y1: h, y2: h }, plot);

    // reference lines (solid, status colour) with a key + text label
    [["warning", t.warningRainMm, "เฝ้าระวัง"], ["danger", t.dangerRainMm, "อันตราย"]].forEach(([kind, v, name]) => {
        svg("line", { class: "ref-" + kind, x1: 0, x2: w, y1: y(v), y2: y(v) }, plot);
        svg("rect", { class: "key-" + kind, x: 6, y: y(v) - 13, width: 8, height: 8, rx: 2 }, plot);
        svg("text", { class: "ref", x: 19, y: y(v) - 5, text: name + " " + v + " มม." }, plot);
    });

    const points = totals.map((v, i) => [x(i), y(v)]);
    svg("path", { class: "area", d: "M" + points.map(p => p.join(",")).join("L") + "L" + x(hours.length - 1) + "," + h + "L0," + h + "Z" }, plot);
    svg("path", { class: "line", d: "M" + points.map(p => p.join(",")).join("L") }, plot);

    hours.forEach((hr, i) => {
        if (i % 6 === 0) {
            svg("text", { x: x(i), y: h + 16, "text-anchor": i === 0 ? "start" : "middle", text: hourOf(hr.time) }, plot);
        }
    });

    const last = points[points.length - 1];
    svg("circle", { class: "dot", cx: last[0], cy: last[1], r: 4.5 }, plot);
    svg("text", { class: "value", x: last[0] - 8, y: last[1] - 9, "text-anchor": "end", text: fmt(sum) + " มม." }, plot);

    // crosshair
    const cross = svg("line", { class: "crosshair", y1: 0, y2: h, visibility: "hidden" }, plot);
    const marker = svg("circle", { class: "dot", r: 4.5, visibility: "hidden" }, plot);
    const hit = svg("rect", { class: "hit", x: 0, y: 0, width: w, height: h }, plot);
    hit.addEventListener("mousemove", event => {
        const box = hit.getBoundingClientRect();
        const i = Math.max(0, Math.min(hours.length - 1,
            Math.round((event.clientX - box.left) / box.width * (hours.length - 1))));
        cross.setAttribute("x1", x(i));
        cross.setAttribute("x2", x(i));
        marker.setAttribute("cx", x(i));
        marker.setAttribute("cy", y(totals[i]));
        cross.setAttribute("visibility", "visible");
        marker.setAttribute("visibility", "visible");
        showTip(container, x(i) + margin.left, y(totals[i]) + margin.top,
            "<strong>" + hourOf(hours[i].time) + " น.</strong> · สะสม " + fmt(totals[i]) + " มม.");
    });
    hit.addEventListener("mouseleave", () => {
        cross.setAttribute("visibility", "hidden");
        marker.setAttribute("visibility", "hidden");
        hideTip();
    });

    root.setAttribute("aria-label", "ฝนสะสมถึง " + fmt(sum) + " มม. ระดับเฝ้าระวัง "
        + t.warningRainMm + " มม. ระดับอันตราย " + t.dangerRainMm + " มม.");
}

/** Rain total x rain chance with the risk zones shaded and this forecast plotted. */
function drawMatrix(container, d) {
    const t = d.thresholds;
    const margin = { top: 8, right: 10, bottom: 34, left: 34 };
    const { root, plot, w, h } = frame(container, margin);

    const xMax = Math.max(120, Math.ceil(d.rainMm * 1.1 / 10) * 10);
    const x = v => Math.min(v, xMax) / xMax * w;
    const y = v => h - v / 100 * h;
    const zone = (cls, x0, x1, y0, y1) =>
        svg("rect", { class: cls, x: x(x0), y: y(y1), width: x(x1) - x(x0), height: y(y0) - y(y1) }, plot);

    zone("zone-safe", 0, xMax, 0, 100);
    zone("zone-warning", t.warningRainMm, xMax, 0, 100);
    zone("zone-warning", t.warningCombinedRainMm, t.warningRainMm, t.warningCombinedChancePercent, 100);
    zone("zone-danger", t.dangerRainMm, xMax, 0, 100);
    zone("zone-danger", t.dangerCombinedRainMm, t.dangerRainMm, t.dangerCombinedChancePercent, 100);

    svg("text", { class: "zone", x: 8, y: h - 8, text: LEVEL_TH.SAFE }, plot);
    svg("text", { class: "zone", x: x((t.warningRainMm + t.dangerRainMm) / 2), y: h - 8, "text-anchor": "middle", text: LEVEL_TH.WARNING }, plot);
    svg("text", { class: "zone", x: x((t.dangerRainMm + xMax) / 2), y: h - 8, "text-anchor": "middle", text: LEVEL_TH.DANGER }, plot);

    svg("line", { class: "axis", x1: 0, x2: w, y1: h, y2: h }, plot);
    svg("line", { class: "axis", x1: 0, x2: 0, y1: 0, y2: h }, plot);
    [0, t.warningCombinedRainMm, t.warningRainMm, t.dangerCombinedRainMm, t.dangerRainMm, xMax].forEach(v => {
        svg("text", { x: x(v), y: h + 15, "text-anchor": v === 0 ? "start" : v === xMax ? "end" : "middle", text: v }, plot);
    });
    svg("text", { x: w, y: h + 29, "text-anchor": "end", text: "ปริมาณฝนรวม (มม.)" }, plot);
    [0, t.warningCombinedChancePercent, t.dangerCombinedChancePercent, 100].forEach(v => {
        svg("text", { x: -6, y: y(v) + 4, "text-anchor": "end", text: v + "%" }, plot);
    });
    svg("text", { x: 0, y: h + 29, text: "↑ โอกาสฝน" }, plot);

    const px = x(d.rainMm);
    const py = y(d.rainChancePercent);
    const dot = svg("circle", { class: "dot ink", cx: px, cy: py, r: 6 }, plot);
    const labelLeft = px > w * 0.6;
    svg("text", {
        class: "value", x: labelLeft ? px - 11 : px + 11, y: py + 4,
        "text-anchor": labelLeft ? "end" : "start", text: d.location
    }, plot);
    const hit = svg("circle", { class: "hit", cx: px, cy: py, r: 14 }, plot);
    hit.addEventListener("mouseenter", () => showTip(container, px + margin.left, py + margin.top,
        "<strong>" + escapeHtml(d.location) + "</strong> · " + fmt(d.rainMm) + " มม. · " + d.rainChancePercent + "% → " + LEVEL_TH[d.level]));
    hit.addEventListener("mouseleave", hideTip);
    dot.setAttribute("aria-hidden", "true");

    root.setAttribute("aria-label", d.location + " ฝน " + fmt(d.rainMm) + " มม. โอกาสฝน "
        + d.rainChancePercent + "% อยู่ในโซน" + LEVEL_TH[d.level]);
}

function yGrid(plot, ticks, y, w, label) {
    ticks.forEach(v => {
        if (v > 0) {
            svg("line", { class: "gridline", x1: 0, x2: w, y1: y(v), y2: y(v) }, plot);
        }
        svg("text", { x: -8, y: y(v) + 4, "text-anchor": "end", text: label(v) }, plot);
    });
}

/** Column path: square at the baseline, 4px rounded data-end. */
function columnPath(x, top, width, height) {
    if (height <= 0) {
        return "";
    }
    const r = Math.min(4, width / 2, height);
    const bottom = top + height;
    return "M" + x + "," + bottom + "V" + (top + r) + "A" + r + "," + r + " 0 0 1 " + (x + r) + "," + top
        + "H" + (x + width - r) + "A" + r + "," + r + " 0 0 1 " + (x + width) + "," + (top + r)
        + "V" + bottom + "Z";
}

/* ======================= Tooltip ======================= */

function showTip(container, x, y, html) {
    tooltip.innerHTML = html;
    tooltip.hidden = false;
    const stageBox = stage.getBoundingClientRect();
    const box = container.getBoundingClientRect();
    const tipWidth = tooltip.offsetWidth;
    let left = box.left - stageBox.left + x - tipWidth / 2;
    left = Math.max(0, Math.min(left, stageBox.width - tipWidth));
    tooltip.style.left = left + "px";
    tooltip.style.top = (box.top - stageBox.top + y - tooltip.offsetHeight - 12) + "px";
}

function hideTip() {
    tooltip.hidden = true;
}

/* ======================= Helpers ======================= */

function fmt(value, compact) {
    const rounded = Math.round(value * 10) / 10;
    return compact && Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1);
}

/** "2026-11-07T15:00:00+07:00" -> "15:00" (already Thai time; ignores the browser's zone). */
function hourOf(iso) {
    return iso.slice(11, 16);
}

function niceStep(max) {
    const raw = max / 4;
    const power = Math.pow(10, Math.floor(Math.log10(raw)));
    const step = [1, 2, 2.5, 5, 10].map(m => m * power).find(s => s >= raw);
    return step;
}

function niceMax(max) {
    const step = niceStep(max);
    return Math.ceil(max / step) * step;
}

function ticksFor(max) {
    const step = niceStep(max);
    const ticks = [];
    for (let v = 0; v <= max + 1e-9; v += step) {
        ticks.push(Math.round(v * 100) / 100);
    }
    return ticks;
}

function small(text) {
    return Object.assign(document.createElement("small"), { textContent: text });
}

function escapeHtml(text) {
    return text.replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

/* ======================= Start ======================= */

let resizeFrame = 0;
new ResizeObserver(() => {
    cancelAnimationFrame(resizeFrame);
    resizeFrame = requestAnimationFrame(render);
}).observe($("dash"));

selectMode("live", false);
check(locationInput.value.trim());
