#!/usr/bin/env node
// render_diagrams.mjs — Node v24, zero deps.
// Emits 40 SVG pages to ../exports/svg/ matching the BetterMe_UML.drawio source.
// Strict academic Vietnamese style: white background, Arial, black/gray strokes only.

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const OUT_DIR = resolve(__dirname, '../exports/svg');
mkdirSync(OUT_DIR, { recursive: true });

// ---------- Style primitives ----------
const FONT = 'Arial, Helvetica, sans-serif';
const STROKE = '#000000';
const DASH = '#666666';
const HEADER_FILL = '#F0F0F0';

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function svgHeader(w, h, title) {
  return `<?xml version="1.0" encoding="UTF-8"?>\n` +
    `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" font-family="${FONT}">\n` +
    `<rect x="0" y="0" width="${w}" height="${h}" fill="#FFFFFF" stroke="none"/>\n` +
    `<text x="${w/2}" y="28" text-anchor="middle" font-size="16" font-weight="bold" fill="${STROKE}">${esc(title)}</text>\n`;
}
const svgFooter = () => `</svg>\n`;

// ---------- Primitive shape helpers ----------
function rect(x, y, w, h, opts = {}) {
  const fill = opts.fill || 'none';
  const sw = opts.sw || 1.5;
  const rx = opts.rx || 0;
  const dash = opts.dash ? `stroke-dasharray="6 4"` : '';
  const stroke = opts.stroke || STROKE;
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}" ry="${rx}" fill="${fill}" stroke="${stroke}" stroke-width="${sw}" ${dash}/>`;
}
function ellipse(cx, cy, rx, ry, opts = {}) {
  const fill = opts.fill || 'none';
  const sw = opts.sw || 1.5;
  return `<ellipse cx="${cx}" cy="${cy}" rx="${rx}" ry="${ry}" fill="${fill}" stroke="${STROKE}" stroke-width="${sw}"/>`;
}
function diamond(cx, cy, w, h) {
  const pts = `${cx},${cy-h/2} ${cx+w/2},${cy} ${cx},${cy+h/2} ${cx-w/2},${cy}`;
  return `<polygon points="${pts}" fill="none" stroke="${STROKE}" stroke-width="1.5"/>`;
}
function txt(x, y, s, opts = {}) {
  const size = opts.size || 12;
  const anchor = opts.anchor || 'middle';
  const weight = opts.bold ? 'bold' : 'normal';
  const fill = opts.fill || STROKE;
  const style = opts.italic ? 'italic' : 'normal';
  return `<text x="${x}" y="${y}" text-anchor="${anchor}" font-size="${size}" font-weight="${weight}" font-style="${style}" fill="${fill}">${esc(s)}</text>`;
}
function multiline(x, y, lines, opts = {}) {
  const lh = opts.lh || 14;
  return lines.map((ln, i) => txt(x, y + i * lh, ln, opts)).join('\n');
}
function line(x1, y1, x2, y2, opts = {}) {
  const sw = opts.sw || 1.5;
  const stroke = opts.stroke || STROKE;
  const dash = opts.dash ? `stroke-dasharray="6 4"` : '';
  const marker = opts.arrow ? `marker-end="url(#arr)"` : (opts.openArrow ? `marker-end="url(#oar)"` : '');
  return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${stroke}" stroke-width="${sw}" ${dash} ${marker}/>`;
}
function arrowDefs() {
  return `<defs>
    <marker id="arr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10 z" fill="${STROKE}"/>
    </marker>
    <marker id="oar" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10" fill="none" stroke="${STROKE}" stroke-width="1.2"/>
    </marker>
    <marker id="oarG" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10" fill="none" stroke="${DASH}" stroke-width="1.2"/>
    </marker>
  </defs>\n`;
}

// ---------- Use-case actor (stick figure) ----------
function actor(x, y, name) {
  // x,y = head center
  const head = `<circle cx="${x}" cy="${y}" r="10" fill="none" stroke="${STROKE}" stroke-width="1.5"/>`;
  const body = `<line x1="${x}" y1="${y+10}" x2="${x}" y2="${y+45}" stroke="${STROKE}" stroke-width="1.5"/>`;
  const arms = `<line x1="${x-15}" y1="${y+22}" x2="${x+15}" y2="${y+22}" stroke="${STROKE}" stroke-width="1.5"/>`;
  const legL = `<line x1="${x}" y1="${y+45}" x2="${x-12}" y2="${y+68}" stroke="${STROKE}" stroke-width="1.5"/>`;
  const legR = `<line x1="${x}" y1="${y+45}" x2="${x+12}" y2="${y+68}" stroke="${STROKE}" stroke-width="1.5"/>`;
  const label = txt(x, y+85, name, { size: 12, bold: true });
  return [head, body, arms, legL, legR, label].join('\n');
}

// UML lifeline header + dashed lifeline body
function lifeline(cx, top, bottom, label, type = 'boundary') {
  // type: actor | boundary | control | entity
  const headW = 110;
  const headH = 50;
  const x = cx - headW / 2;
  let icon = '';
  if (type === 'actor') {
    icon = `<g transform="translate(${cx-7},${top-32})">` +
      `<circle cx="7" cy="6" r="5" fill="none" stroke="${STROKE}" stroke-width="1.2"/>` +
      `<line x1="7" y1="11" x2="7" y2="24" stroke="${STROKE}" stroke-width="1.2"/>` +
      `<line x1="0" y1="17" x2="14" y2="17" stroke="${STROKE}" stroke-width="1.2"/>` +
      `<line x1="7" y1="24" x2="0" y2="34" stroke="${STROKE}" stroke-width="1.2"/>` +
      `<line x1="7" y1="24" x2="14" y2="34" stroke="${STROKE}" stroke-width="1.2"/>` +
      `</g>`;
  } else if (type === 'boundary') {
    icon = `<g transform="translate(${cx-14},${top-22})">` +
      `<line x1="0" y1="0" x2="0" y2="20" stroke="${STROKE}" stroke-width="1.4"/>` +
      `<line x1="0" y1="10" x2="8" y2="10" stroke="${STROKE}" stroke-width="1.4"/>` +
      `<circle cx="16" cy="10" r="8" fill="none" stroke="${STROKE}" stroke-width="1.4"/>` +
      `</g>`;
  } else if (type === 'control') {
    icon = `<g transform="translate(${cx-10},${top-22})">` +
      `<circle cx="10" cy="10" r="9" fill="none" stroke="${STROKE}" stroke-width="1.4"/>` +
      `<polygon points="14,2 18,4 14,6" fill="${STROKE}"/>` +
      `</g>`;
  } else if (type === 'entity') {
    icon = `<g transform="translate(${cx-9},${top-22})">` +
      `<circle cx="9" cy="9" r="9" fill="none" stroke="${STROKE}" stroke-width="1.4"/>` +
      `<line x1="0" y1="20" x2="18" y2="20" stroke="${STROKE}" stroke-width="1.4"/>` +
      `</g>`;
  }
  const head = rect(x, top, headW, headH, { fill: HEADER_FILL, sw: 1.5 });
  const lab = txt(cx, top + headH/2 + 4, label, { size: 11, bold: true });
  const ll = `<line x1="${cx}" y1="${top+headH}" x2="${cx}" y2="${bottom}" stroke="${STROKE}" stroke-width="1.2" stroke-dasharray="6 4"/>`;
  return [icon, head, lab, ll].join('\n');
}

// activation bar
function activation(cx, y1, y2) {
  return rect(cx-5, y1, 10, y2-y1, { fill: '#FFFFFF', sw: 1 });
}

// horizontal message arrow
function msg(x1, x2, y, label, opts = {}) {
  const dash = !!opts.ret;
  const stroke = STROKE;
  const dir = x2 > x1 ? 1 : -1;
  const arrowMarker = opts.ret ? 'oar' : 'arr';
  const ln = `<line x1="${x1}" y1="${y}" x2="${x2}" y2="${y}" stroke="${stroke}" stroke-width="1.4" ${dash?'stroke-dasharray="5 4"':''} marker-end="url(#${arrowMarker})"/>`;
  const lx = (x1 + x2) / 2;
  const ly = y - 6;
  const lab = txt(lx, ly, label, { size: 10, italic: opts.ret });
  return [ln, lab].join('\n');
}

// alt frame
function altFrame(x, y, w, h, label) {
  const r = rect(x, y, w, h, { sw: 1.2 });
  const tab = `<polygon points="${x},${y} ${x+50},${y} ${x+58},${y+8} ${x+58},${y+18} ${x},${y+18}" fill="#FFFFFF" stroke="${STROKE}" stroke-width="1.2"/>`;
  const tabLab = txt(x+5, y+13, 'alt', { size: 10, bold: true, anchor: 'start' });
  const titleLab = txt(x+65, y+13, `[${label}]`, { size: 10, anchor: 'start', italic: true });
  return [r, tab, tabLab, titleLab].join('\n');
}

// ---------- Activity (BDHD) helpers — swimlane "Người dùng | Hệ thống" ----------
function activityHeader(w, top = 50) {
  const half = w / 2;
  return [
    rect(40, top, w-80, 32, { fill: HEADER_FILL }),
    line(40 + half - 40, top, 40 + half - 40, top+32),
    txt(40 + (half-40)/2, top+21, 'Người dùng', { bold: true, size: 13 }),
    txt(40 + half - 40 + (w-80)/2 - (half-40)/2 + (half-40)/2, top+21, 'Hệ thống', { bold: true, size: 13 }),
  ].join('\n');
}

function startNode(cx, cy) {
  return `<circle cx="${cx}" cy="${cy}" r="9" fill="${STROKE}" stroke="${STROKE}"/>`;
}
function endNode(cx, cy) {
  return `<circle cx="${cx}" cy="${cy}" r="11" fill="none" stroke="${STROKE}" stroke-width="1.5"/>` +
         `<circle cx="${cx}" cy="${cy}" r="6" fill="${STROKE}"/>`;
}
function actionBox(cx, cy, w, h, label) {
  const x = cx - w/2;
  const y = cy - h/2;
  const r = rect(x, y, w, h, { rx: 14 });
  const lines = String(label).split('\n');
  const startY = cy - ((lines.length - 1) * 7);
  const t = lines.map((ln, i) => txt(cx, startY + i*14 + 4, ln, { size: 11 })).join('\n');
  return [r, t].join('\n');
}
function decision(cx, cy, w, h, label) {
  const d = diamond(cx, cy, w, h);
  const t = txt(cx, cy+4, label, { size: 11 });
  return [d, t].join('\n');
}
function vArrow(x, y1, y2, label) {
  const ln = `<line x1="${x}" y1="${y1}" x2="${x}" y2="${y2}" stroke="${STROKE}" stroke-width="1.4" marker-end="url(#arr)"/>`;
  const lab = label ? txt(x+8, (y1+y2)/2, label, { size: 10, anchor: 'start' }) : '';
  return [ln, lab].join('\n');
}
function hArrow(x1, x2, y, label) {
  const ln = `<line x1="${x1}" y1="${y}" x2="${x2}" y2="${y}" stroke="${STROKE}" stroke-width="1.4" marker-end="url(#arr)"/>`;
  const lab = label ? txt((x1+x2)/2, y-6, label, { size: 10 }) : '';
  return [ln, lab].join('\n');
}

// ---------- Page builders ----------

// Use case: TỔNG QUÁT
function pageUseCaseTongQuat() {
  const w = 1654, h = 1169;
  let out = svgHeader(w, h, 'Hình 2.1 — Sơ đồ Use Case tổng quát hệ thống BetterMe');
  out += arrowDefs();
  // system boundary
  out += rect(380, 90, 900, 970, { sw: 2 });
  out += txt(830, 115, 'Hệ thống BetterMe', { size: 14, bold: true });
  // primary actor
  out += actor(180, 540, 'Người dùng');
  // external actors right
  const exts = [
    [1480, 200, 'Firebase Auth'],
    [1480, 360, 'Firestore'],
    [1480, 520, 'OpenRouter API'],
    [1480, 680, 'Gemini API'],
    [1480, 840, 'WorkManager'],
    [1480, 1000, 'AlarmManager'],
  ];
  for (const [x, y, n] of exts) out += actor(x, y, n);
  // ellipses (use cases)
  const ucs = [
    [560, 200, 'Đăng nhập / Xác thực'],
    [560, 290, 'Onboarding & chọn nhóm'],
    [560, 380, 'Quản lý thói quen'],
    [560, 470, 'Check-in & ảnh'],
    [560, 560, 'Nhận thông báo nhắc'],
    [560, 650, 'Trò chuyện AI'],
    [560, 740, 'Phân tích & gợi ý AI'],
    [560, 830, 'Xem biểu đồ thống kê'],
    [560, 920, 'Tham gia thử thách'],
    [560, 1010, 'Nhận huy hiệu thành tích'],
  ];
  for (const [x, y, l] of ucs) {
    out += ellipse(x, y, 150, 30);
    out += txt(x, y+4, l, { size: 12 });
    // actor → uc
    out += line(225, 555, x-150, y, { arrow: true });
  }
  // system-side ext links
  out += line(710, 200, 1440, 200, { dash: true, openArrow: true, stroke: DASH });
  out += txt(1075, 195, '«include»', { size: 10, italic: true, fill: DASH });
  out += line(710, 380, 1440, 360, { dash: true, openArrow: true, stroke: DASH });
  out += line(710, 470, 1440, 360, { dash: true, openArrow: true, stroke: DASH });
  out += line(710, 650, 1440, 520, { dash: true, openArrow: true, stroke: DASH });
  out += line(710, 740, 1440, 680, { dash: true, openArrow: true, stroke: DASH });
  out += line(710, 560, 1440, 1000, { dash: true, openArrow: true, stroke: DASH });
  out += line(710, 920, 1440, 840, { dash: true, openArrow: true, stroke: DASH });
  return out + svgFooter();
}

// Use case: NGƯỜI DÙNG (extends)
function pageUseCaseNguoiDung() {
  const w = 1654, h = 1169;
  let out = svgHeader(w, h, 'Hình 2.2 — Sơ đồ Use Case người dùng (chi tiết)');
  out += arrowDefs();
  out += rect(220, 90, 1380, 1050, { sw: 2 });
  out += txt(910, 115, 'Phạm vi: Người dùng cuối', { size: 13, bold: true });
  out += actor(110, 600, 'Người dùng');

  // core ucs (left column)
  const core = [
    [430, 200, 'Đăng nhập'],
    [430, 290, 'Xem onboarding'],
    [430, 390, 'Quản lý thói quen'],
    [430, 540, 'Check-in thói quen'],
    [430, 660, 'AI hỗ trợ'],
    [430, 800, 'Xem thống kê'],
    [430, 940, 'Thử thách'],
  ];
  for (const [x, y, l] of core) {
    out += ellipse(x, y, 130, 28);
    out += txt(x, y+4, l, { size: 12 });
    out += line(155, 600, x-130, y, { arrow: true });
  }

  // extends right column
  const ext = [
    // quan ly thoi quen extends
    [430, 390, 760, 330, 'Tạo thói quen'],
    [430, 390, 760, 390, 'Sửa thói quen'],
    [430, 390, 760, 450, 'Xóa thói quen'],
    [430, 390, 760, 510, 'Xem chi tiết'],
    // check-in
    [430, 540, 760, 570, 'Chụp ảnh'],
    [430, 540, 760, 630, 'Chọn nhóm thói quen'],
    // AI
    [430, 660, 760, 690, 'AI trò chuyện'],
    [430, 660, 760, 750, 'AI phân tích'],
    [430, 660, 760, 810, 'AI gợi ý'],
    // statistics
    [430, 800, 1050, 800, 'Biểu đồ tuần/tháng'],
    [430, 800, 1050, 860, 'Hoàn thành / thất bại'],
    // challenge
    [430, 940, 1050, 920, 'Tham gia thử thách'],
    [430, 940, 1050, 980, 'Theo dõi tiến độ'],
    [430, 940, 1050, 1040, 'Nhận huy hiệu'],
  ];
  for (const [fx, fy, x, y, l] of ext) {
    out += ellipse(x, y, 120, 26);
    out += txt(x, y+4, l, { size: 11 });
    out += line(fx+130, fy, x-120, y, { dash: true, openArrow: true, stroke: DASH });
    out += txt((fx+130+x-120)/2, (fy+y)/2 - 6, '«extend»', { size: 9, italic: true, fill: DASH });
  }
  // notifications extra
  out += ellipse(430, 1080, 130, 28);
  out += txt(430, 1084, 'Nhận thông báo', { size: 12 });
  out += line(155, 600, 300, 1080, { arrow: true });
  return out + svgFooter();
}

// Use case: HỆ THỐNG / AI
function pageUseCaseHeThongAI() {
  const w = 1654, h = 1169;
  let out = svgHeader(w, h, 'Hình 2.3 — Sơ đồ Use Case hệ thống / AI / dịch vụ ngoài');
  out += arrowDefs();
  out += rect(380, 90, 900, 970, { sw: 2 });
  out += txt(830, 115, 'Hệ thống nền (background + AI)', { size: 14, bold: true });
  const ucs = [
    [560, 200, 'Đồng bộ Firestore (SyncWorker)'],
    [560, 290, 'Dọn dẹp dữ liệu (MidnightCleanup)'],
    [560, 380, 'Nhắc thử thách (ChallengeReminder)'],
    [560, 470, 'Đặt lịch alarm (AlarmManager)'],
    [560, 560, 'Khôi phục alarm sau reboot'],
    [560, 650, 'Gọi AI Gemini (native)'],
    [560, 740, 'Gọi AI OpenRouter (7-model)'],
    [560, 830, 'Cache AI (12-24h)'],
    [560, 920, 'Dedup single-flight'],
    [560, 1010, 'Đánh giá strict-daily thử thách'],
  ];
  for (const [x, y, l] of ucs) {
    out += ellipse(x, y, 160, 30);
    out += txt(x, y+4, l, { size: 11 });
  }
  // external actors right
  const exts = [
    [1480, 180, 'Firebase Auth'],
    [1480, 320, 'Firestore'],
    [1480, 470, 'OpenRouter API'],
    [1480, 620, 'Gemini API'],
    [1480, 770, 'WorkManager'],
    [1480, 920, 'AlarmManager'],
  ];
  for (const [x, y, n] of exts) {
    out += actor(x, y, n);
    out += txt(x, y-15, '«external»', { size: 9, italic: true, fill: DASH });
  }
  // user actor
  out += actor(180, 540, 'Người dùng');
  // user → some ucs
  for (const [x, y] of [[560,200],[560,650],[560,740],[560,1010]]) {
    out += line(225, 555, x-160, y, { arrow: true });
  }
  // external links
  const links = [
    [1440, 320, 710, 200], // firestore - sync
    [1440, 320, 710, 290], // firestore - cleanup
    [1440, 770, 710, 290], // wm - cleanup
    [1440, 770, 710, 380], // wm - challenge
    [1440, 920, 710, 470], // alarm
    [1440, 920, 710, 560], // alarm boot
    [1440, 620, 710, 650], // gemini
    [1440, 470, 710, 740], // openrouter
  ];
  for (const [a, b, c, d] of links) {
    out += line(a, b, c, d, { dash: true, openArrow: true, stroke: DASH });
  }
  return out + svgFooter();
}

// ---------- Activity (BDHD) builder ----------
// items is array of {kind, ...}. We provide a small DSL.
// kinds: start, end, act:{lane,label,cy}, dec:{lane,label,cy}, arrow:{from,to,label}
// To keep code compact, each BDHD page is described declaratively below.

function buildBDHD(num, title, lanes, nodes, arrows) {
  const w = 827, h = 1169;
  let out = svgHeader(w, h, `Hình ${num} — BĐHĐ ${title}`);
  out += arrowDefs();
  const topHeader = 60;
  out += rect(40, topHeader, w-80, 36, { fill: HEADER_FILL });
  out += line((40+w-40)/2, topHeader, (40+w-40)/2, topHeader+36);
  out += txt(40 + (w-80)/4, topHeader+23, lanes[0] || 'Người dùng', { bold: true, size: 12 });
  out += txt(40 + (w-80)*3/4, topHeader+23, lanes[1] || 'Hệ thống', { bold: true, size: 12 });
  // swimlane verticals down to bottom
  out += rect(40, topHeader, w-80, h-topHeader-50, { sw: 1.5 });
  out += line((40+w-40)/2, topHeader+36, (40+w-40)/2, h-50);
  // nodes
  const left = 40 + (w-80)/4;
  const right = 40 + (w-80)*3/4;
  const laneX = (l) => (l === 0 ? left : right);
  const byId = {};
  for (const n of nodes) {
    const x = laneX(n.lane);
    const y = n.y;
    byId[n.id] = { x, y, w: n.w || 200, h: n.h || 50, kind: n.kind };
    if (n.kind === 'start') out += startNode(x, y);
    else if (n.kind === 'end') out += endNode(x, y);
    else if (n.kind === 'act') out += actionBox(x, y, n.w || 220, n.h || 46, n.label);
    else if (n.kind === 'dec') out += decision(x, y, n.w || 120, n.h || 70, n.label);
  }
  // arrows
  for (const a of arrows) {
    const A = byId[a.from], B = byId[a.to];
    if (!A || !B) continue;
    if (A.x === B.x) {
      // vertical
      const y1 = A.y + ((A.kind === 'start') ? 9 : (A.kind === 'dec' ? (A.h/2) : (A.h/2)));
      const y2 = B.y - ((B.kind === 'end') ? 11 : (B.kind === 'dec' ? (B.h/2) : (B.h/2)));
      out += vArrow(A.x, y1, y2, a.label);
    } else {
      // horizontal-ish: route via midpoint
      const fromR = A.kind === 'dec' ? A.w/2 : A.w/2;
      const toR = B.kind === 'dec' ? B.w/2 : B.w/2;
      const x1 = A.x + (B.x > A.x ? fromR : -fromR);
      const x2 = B.x + (B.x > A.x ? -toR : toR);
      const midY = (A.y + B.y) / 2;
      // polyline: A.x,A.y → mid → B.x,B.y
      out += `<polyline points="${x1},${A.y} ${(x1+x2)/2},${A.y} ${(x1+x2)/2},${B.y} ${x2},${B.y}" fill="none" stroke="${STROKE}" stroke-width="1.4" marker-end="url(#arr)"/>`;
      if (a.label) out += txt((x1+x2)/2, midY, a.label, { size: 10 });
    }
  }
  return out + svgFooter();
}

// ---------- Sequence (BDTT) builder ----------
function buildBDTT(num, title, lifelines, messages, altBlocks = []) {
  const isWide = lifelines.length > 5;
  const w = isWide ? 1654 : 1100;
  const h = 1169;
  let out = svgHeader(w, h, `Hình ${num} — BĐTT ${title}`);
  out += arrowDefs();
  // distribute lifelines
  const top = 120;
  const bottom = h - 70;
  const margin = 90;
  const span = w - 2*margin;
  const step = lifelines.length > 1 ? span / (lifelines.length - 1) : 0;
  const lifelineX = (i) => margin + i * step;
  // draw lifelines
  for (let i = 0; i < lifelines.length; i++) {
    const ll = lifelines[i];
    out += lifeline(lifelineX(i), top, bottom, ll.label, ll.type);
  }
  // alt frames
  for (const a of altBlocks) {
    const x1 = lifelineX(a.from) - 60;
    const x2 = lifelineX(a.to) + 60;
    out += altFrame(x1, a.y, x2 - x1, a.h, a.label);
  }
  // messages
  for (const m of messages) {
    const y = m.y;
    const x1 = lifelineX(m.from);
    const x2 = lifelineX(m.to);
    if (m.self) {
      // self message loop
      out += `<polyline points="${x1},${y} ${x1+30},${y} ${x1+30},${y+18} ${x1+4},${y+18}" fill="none" stroke="${STROKE}" stroke-width="1.4" marker-end="url(#arr)"/>`;
      out += txt(x1+34, y-2, m.label, { size: 10, anchor: 'start' });
    } else {
      out += msg(x1, x2, y, m.label, { ret: !!m.ret });
    }
  }
  return out + svgFooter();
}

// ---------- BDHD/BDTT declarative definitions for 17 use cases ----------

const BDHD_DEFS = [
  // 1 Đăng nhập
  {
    title: 'Đăng nhập / Xác thực người dùng',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 190, label: 'Mở ứng dụng' },
      { id: 'a2', kind: 'act', lane: 0, y: 260, label: 'Nhấn "Đăng nhập với Google"' },
      { id: 'a3', kind: 'act', lane: 1, y: 330, label: 'Hiển thị Credential Manager' },
      { id: 'a4', kind: 'act', lane: 0, y: 400, label: 'Chọn tài khoản Google' },
      { id: 'a5', kind: 'act', lane: 1, y: 470, label: 'Gọi Firebase Auth\nsignInWithCredential' },
      { id: 'd1', kind: 'dec', lane: 1, y: 560, label: 'Hợp lệ?' },
      { id: 'a6', kind: 'act', lane: 1, y: 670, label: 'Tạo/đồng bộ user\ntrong Firestore + Room' },
      { id: 'a7', kind: 'act', lane: 1, y: 750, label: 'Vào HomeScreen' },
      { id: 'a8', kind: 'act', lane: 0, y: 670, label: 'Hiển thị lỗi đăng nhập' },
      { id: 'e', kind: 'end', lane: 1, y: 830 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' },
      { from: 'a2', to: 'a3' }, { from: 'a3', to: 'a4' },
      { from: 'a4', to: 'a5' }, { from: 'a5', to: 'd1' },
      { from: 'd1', to: 'a6', label: 'đúng' },
      { from: 'd1', to: 'a8', label: 'sai' },
      { from: 'a6', to: 'a7' }, { from: 'a7', to: 'e' },
    ],
  },
  // 2 Onboarding & chọn nhóm
  {
    title: 'Onboarding & chọn nhóm thói quen',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Vào lần đầu sau đăng nhập' },
      { id: 'a2', kind: 'act', lane: 1, y: 280, label: 'Hiển thị màn 1 / 2 / 3' },
      { id: 'a3', kind: 'act', lane: 0, y: 360, label: 'Vuốt qua các slide' },
      { id: 'a4', kind: 'act', lane: 0, y: 440, label: 'Nhấn "Bắt đầu"' },
      { id: 'a5', kind: 'act', lane: 1, y: 520, label: 'Load 6 category mặc định' },
      { id: 'a6', kind: 'act', lane: 0, y: 600, label: 'Tích chọn nhóm yêu thích' },
      { id: 'a7', kind: 'act', lane: 1, y: 680, label: 'Lưu user_categories' },
      { id: 'a8', kind: 'act', lane: 1, y: 760, label: 'Đặt cờ onboardingDone' },
      { id: 'e', kind: 'end', lane: 1, y: 840 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' },
      { from: 'a3', to: 'a4' }, { from: 'a4', to: 'a5' }, { from: 'a5', to: 'a6' },
      { from: 'a6', to: 'a7' }, { from: 'a7', to: 'a8' }, { from: 'a8', to: 'e' },
    ],
  },
  // 3 Xem danh sách thói quen
  {
    title: 'Xem danh sách thói quen',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Vào tab "Thói quen"' },
      { id: 'a2', kind: 'act', lane: 1, y: 280, label: 'HabitsViewModel load danh sách' },
      { id: 'a3', kind: 'act', lane: 1, y: 360, label: 'Truy vấn Room\n(habits + habit_logs hôm nay)' },
      { id: 'd1', kind: 'dec', lane: 1, y: 460, label: 'Rỗng?' },
      { id: 'a4', kind: 'act', lane: 1, y: 570, label: 'Hiển thị empty state' },
      { id: 'a5', kind: 'act', lane: 1, y: 570, label: 'Render LazyColumn các thói quen' },
      { id: 'a6', kind: 'act', lane: 0, y: 670, label: 'Cuộn / lọc theo nhóm' },
      { id: 'a7', kind: 'act', lane: 1, y: 760, label: 'Cập nhật trạng thái UI' },
      { id: 'e', kind: 'end', lane: 1, y: 850 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' },
      { from: 'a3', to: 'd1' },
      { from: 'd1', to: 'a4', label: 'có' }, { from: 'd1', to: 'a5', label: 'không' },
      { from: 'a5', to: 'a6' }, { from: 'a6', to: 'a7' }, { from: 'a7', to: 'e' },
    ],
  },
  // 4 Tạo thói quen mới
  {
    title: 'Tạo thói quen mới',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Nhấn FAB "Thêm thói quen"' },
      { id: 'a2', kind: 'act', lane: 1, y: 280, label: 'Mở AddHabitScreen' },
      { id: 'a3', kind: 'act', lane: 0, y: 360, label: 'Nhập tên, mô tả, mục tiêu,\ntần suất, nhóm' },
      { id: 'a4', kind: 'act', lane: 0, y: 460, label: 'Nhấn "Lưu"' },
      { id: 'd1', kind: 'dec', lane: 1, y: 560, label: 'Hợp lệ?' },
      { id: 'a5', kind: 'act', lane: 0, y: 670, label: 'Hiển thị lỗi inline' },
      { id: 'a6', kind: 'act', lane: 1, y: 670, label: 'Insert habits + reminder' },
      { id: 'a7', kind: 'act', lane: 1, y: 750, label: 'Đặt AlarmManager nếu có nhắc' },
      { id: 'a8', kind: 'act', lane: 1, y: 830, label: 'Đẩy Firestore qua SyncWorker' },
      { id: 'e', kind: 'end', lane: 1, y: 910 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' },
      { from: 'a3', to: 'a4' }, { from: 'a4', to: 'd1' },
      { from: 'd1', to: 'a5', label: 'sai' }, { from: 'd1', to: 'a6', label: 'đúng' },
      { from: 'a6', to: 'a7' }, { from: 'a7', to: 'a8' }, { from: 'a8', to: 'e' },
    ],
  },
  // 5 Chỉnh sửa
  {
    title: 'Chỉnh sửa thói quen',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Mở chi tiết thói quen' },
      { id: 'a2', kind: 'act', lane: 0, y: 280, label: 'Nhấn "Chỉnh sửa"' },
      { id: 'a3', kind: 'act', lane: 1, y: 360, label: 'Hiển thị form điền sẵn' },
      { id: 'a4', kind: 'act', lane: 0, y: 440, label: 'Sửa trường cần cập nhật' },
      { id: 'a5', kind: 'act', lane: 0, y: 520, label: 'Nhấn "Lưu"' },
      { id: 'd1', kind: 'dec', lane: 1, y: 620, label: 'Hợp lệ?' },
      { id: 'a6', kind: 'act', lane: 0, y: 720, label: 'Báo lỗi' },
      { id: 'a7', kind: 'act', lane: 1, y: 720, label: 'Update Room + Firestore' },
      { id: 'a8', kind: 'act', lane: 1, y: 800, label: 'Cập nhật alarm reminder' },
      { id: 'e', kind: 'end', lane: 1, y: 880 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' },
      { from: 'a3', to: 'a4' }, { from: 'a4', to: 'a5' }, { from: 'a5', to: 'd1' },
      { from: 'd1', to: 'a6', label: 'sai' }, { from: 'd1', to: 'a7', label: 'đúng' },
      { from: 'a7', to: 'a8' }, { from: 'a8', to: 'e' },
    ],
  },
  // 6 Xóa thói quen
  {
    title: 'Xóa thói quen',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Mở chi tiết / vuốt thẻ' },
      { id: 'a2', kind: 'act', lane: 0, y: 280, label: 'Nhấn "Xóa"' },
      { id: 'a3', kind: 'act', lane: 1, y: 360, label: 'Hiển thị dialog xác nhận' },
      { id: 'd1', kind: 'dec', lane: 0, y: 460, label: 'Xác nhận?' },
      { id: 'a4', kind: 'act', lane: 0, y: 570, label: 'Hủy bỏ' },
      { id: 'a5', kind: 'act', lane: 1, y: 570, label: 'Xóa habit + habit_logs liên quan' },
      { id: 'a6', kind: 'act', lane: 1, y: 650, label: 'Hủy AlarmManager' },
      { id: 'a7', kind: 'act', lane: 1, y: 730, label: 'Đồng bộ Firestore (xóa mềm)' },
      { id: 'e', kind: 'end', lane: 1, y: 810 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' }, { from: 'a3', to: 'd1' },
      { from: 'd1', to: 'a4', label: 'không' }, { from: 'd1', to: 'a5', label: 'có' },
      { from: 'a5', to: 'a6' }, { from: 'a6', to: 'a7' }, { from: 'a7', to: 'e' },
    ],
  },
  // 7 Xem chi tiết
  {
    title: 'Xem chi tiết thói quen',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Chọn 1 thói quen từ danh sách' },
      { id: 'a2', kind: 'act', lane: 1, y: 280, label: 'HabitDetailViewModel load' },
      { id: 'a3', kind: 'act', lane: 1, y: 360, label: 'Truy vấn habits + 30 log gần nhất' },
      { id: 'a4', kind: 'act', lane: 1, y: 440, label: 'Tính streak + tỷ lệ hoàn thành' },
      { id: 'a5', kind: 'act', lane: 1, y: 520, label: 'Hiển thị biểu đồ heatmap + lịch sử' },
      { id: 'a6', kind: 'act', lane: 0, y: 600, label: 'Cuộn xem ảnh check-in' },
      { id: 'e', kind: 'end', lane: 1, y: 690 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' },
      { from: 'a3', to: 'a4' }, { from: 'a4', to: 'a5' }, { from: 'a5', to: 'a6' }, { from: 'a6', to: 'e' },
    ],
  },
  // 8 Chụp ảnh check-in
  {
    title: 'Chụp ảnh check-in mỗi ngày',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Nhấn nút "Check-in"' },
      { id: 'a2', kind: 'act', lane: 1, y: 280, label: 'Yêu cầu quyền CAMERA' },
      { id: 'd1', kind: 'dec', lane: 0, y: 380, label: 'Cấp quyền?' },
      { id: 'a3', kind: 'act', lane: 0, y: 480, label: 'Báo cần quyền' },
      { id: 'a4', kind: 'act', lane: 0, y: 480, label: 'Chụp ảnh / chọn từ thư viện' },
      { id: 'a5', kind: 'act', lane: 1, y: 560, label: 'Upload Cloudinary' },
      { id: 'a6', kind: 'act', lane: 1, y: 640, label: 'Lưu habit_logs (photoUrl, done=true)' },
      { id: 'a7', kind: 'act', lane: 1, y: 720, label: 'Đánh giá strict-daily challenge' },
      { id: 'a8', kind: 'act', lane: 1, y: 800, label: 'Hiển thị popup chúc mừng' },
      { id: 'e', kind: 'end', lane: 1, y: 880 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'd1' },
      { from: 'd1', to: 'a3', label: 'không' }, { from: 'd1', to: 'a4', label: 'có' },
      { from: 'a4', to: 'a5' }, { from: 'a5', to: 'a6' }, { from: 'a6', to: 'a7' },
      { from: 'a7', to: 'a8' }, { from: 'a8', to: 'e' },
    ],
  },
  // 9 Nhắc nhở
  {
    title: 'Nhận thông báo nhắc nhở',
    nodes: [
      { id: 's', kind: 'start', lane: 1, y: 130 },
      { id: 'a1', kind: 'act', lane: 1, y: 200, label: 'AlarmManager kích hoạt\nđúng giờ' },
      { id: 'a2', kind: 'act', lane: 1, y: 300, label: 'HabitReminderReceiver chạy' },
      { id: 'a3', kind: 'act', lane: 1, y: 380, label: 'Kiểm tra log hôm nay' },
      { id: 'd1', kind: 'dec', lane: 1, y: 470, label: 'Đã hoàn thành?' },
      { id: 'a4', kind: 'act', lane: 1, y: 580, label: 'Bỏ qua thông báo' },
      { id: 'a5', kind: 'act', lane: 1, y: 580, label: 'Đẩy notification' },
      { id: 'a6', kind: 'act', lane: 0, y: 670, label: 'Người dùng nhấn vào thông báo' },
      { id: 'a7', kind: 'act', lane: 1, y: 760, label: 'Mở HabitDetailScreen' },
      { id: 'e', kind: 'end', lane: 1, y: 850 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' }, { from: 'a3', to: 'd1' },
      { from: 'd1', to: 'a4', label: 'rồi' }, { from: 'd1', to: 'a5', label: 'chưa' },
      { from: 'a5', to: 'a6' }, { from: 'a6', to: 'a7' }, { from: 'a7', to: 'e' },
    ],
  },
  // 10 AI chat
  {
    title: 'Trò chuyện AI',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Mở tab "AI Coach"' },
      { id: 'a2', kind: 'act', lane: 0, y: 280, label: 'Nhập câu hỏi' },
      { id: 'a3', kind: 'act', lane: 1, y: 360, label: 'AiChatRouter kiểm tra cache' },
      { id: 'd1', kind: 'dec', lane: 1, y: 450, label: 'Có cache?' },
      { id: 'a4', kind: 'act', lane: 1, y: 560, label: 'Trả về câu trả lời cache' },
      { id: 'a5', kind: 'act', lane: 1, y: 560, label: 'Gọi Gemini (singleFlight)' },
      { id: 'd2', kind: 'dec', lane: 1, y: 660, label: 'Thành công?' },
      { id: 'a6', kind: 'act', lane: 1, y: 770, label: 'Fallback OpenRouter\n7-model chain' },
      { id: 'a7', kind: 'act', lane: 1, y: 770, label: 'Lưu ai_cache + ai_chat' },
      { id: 'a8', kind: 'act', lane: 1, y: 860, label: 'Stream UI tin nhắn' },
      { id: 'e', kind: 'end', lane: 1, y: 950 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' }, { from: 'a3', to: 'd1' },
      { from: 'd1', to: 'a4', label: 'có' }, { from: 'd1', to: 'a5', label: 'không' },
      { from: 'a5', to: 'd2' },
      { from: 'd2', to: 'a6', label: 'lỗi' }, { from: 'd2', to: 'a7', label: 'ok' },
      { from: 'a6', to: 'a7' }, { from: 'a7', to: 'a8' }, { from: 'a4', to: 'a8' }, { from: 'a8', to: 'e' },
    ],
  },
  // 11 AI phân tích
  {
    title: 'Nhận phân tích thói quen từ AI',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Mở Insights / Trang AI' },
      { id: 'a2', kind: 'act', lane: 0, y: 280, label: 'Nhấn "Phân tích thói quen"' },
      { id: 'a3', kind: 'act', lane: 1, y: 370, label: 'Gom log 7-30 ngày' },
      { id: 'a4', kind: 'act', lane: 1, y: 450, label: 'Tính tổng quan: streak, %\nhoàn thành theo nhóm' },
      { id: 'a5', kind: 'act', lane: 1, y: 540, label: 'AiChatRouter prompt category=ANALYSIS' },
      { id: 'a6', kind: 'act', lane: 1, y: 630, label: 'Lưu ai_cache 24h' },
      { id: 'a7', kind: 'act', lane: 1, y: 720, label: 'Render kết quả + biểu đồ' },
      { id: 'e', kind: 'end', lane: 1, y: 810 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' },
      { from: 'a3', to: 'a4' }, { from: 'a4', to: 'a5' }, { from: 'a5', to: 'a6' }, { from: 'a6', to: 'a7' }, { from: 'a7', to: 'e' },
    ],
  },
  // 12 AI gợi ý điều chỉnh
  {
    title: 'Nhận gợi ý điều chỉnh thói quen từ AI',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Mở phần đề xuất AI' },
      { id: 'a2', kind: 'act', lane: 1, y: 280, label: 'Lấy ngữ cảnh: thói quen\n+ logs gần đây' },
      { id: 'a3', kind: 'act', lane: 1, y: 370, label: 'AiChatRouter prompt category=ADVICE' },
      { id: 'd1', kind: 'dec', lane: 1, y: 460, label: 'Quota AI còn?' },
      { id: 'a4', kind: 'act', lane: 1, y: 570, label: 'Hiển thị gợi ý fallback offline' },
      { id: 'a5', kind: 'act', lane: 1, y: 570, label: 'Stream gợi ý cá nhân hóa' },
      { id: 'a6', kind: 'act', lane: 0, y: 660, label: 'Nhấn "Áp dụng"' },
      { id: 'a7', kind: 'act', lane: 1, y: 740, label: 'Tạo / sửa thói quen theo gợi ý' },
      { id: 'e', kind: 'end', lane: 1, y: 820 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' }, { from: 'a3', to: 'd1' },
      { from: 'd1', to: 'a4', label: 'hết' }, { from: 'd1', to: 'a5', label: 'còn' },
      { from: 'a5', to: 'a6' }, { from: 'a6', to: 'a7' }, { from: 'a7', to: 'e' },
    ],
  },
  // 13 Biểu đồ
  {
    title: 'Xem biểu đồ thống kê theo tuần / tháng',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Mở tab "Thống kê"' },
      { id: 'a2', kind: 'act', lane: 0, y: 280, label: 'Chọn khoảng: tuần / tháng' },
      { id: 'a3', kind: 'act', lane: 1, y: 370, label: 'ChartService gom dữ liệu logs' },
      { id: 'a4', kind: 'act', lane: 1, y: 450, label: 'Group theo ngày + nhóm' },
      { id: 'a5', kind: 'act', lane: 1, y: 530, label: 'Render BarChart + LineChart' },
      { id: 'a6', kind: 'act', lane: 0, y: 610, label: 'Vuốt sang nhóm khác' },
      { id: 'a7', kind: 'act', lane: 1, y: 690, label: 'Cập nhật chart realtime' },
      { id: 'e', kind: 'end', lane: 1, y: 780 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' },
      { from: 'a3', to: 'a4' }, { from: 'a4', to: 'a5' }, { from: 'a5', to: 'a6' }, { from: 'a6', to: 'a7' }, { from: 'a7', to: 'e' },
    ],
  },
  // 14 Theo dõi tiến trình
  {
    title: 'Theo dõi tiến trình thói quen',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Mở chi tiết thói quen' },
      { id: 'a2', kind: 'act', lane: 1, y: 280, label: 'Tính streak hiện tại' },
      { id: 'a3', kind: 'act', lane: 1, y: 360, label: 'Tính % hoàn thành 30 ngày' },
      { id: 'a4', kind: 'act', lane: 1, y: 440, label: 'Hiển thị heatmap + progress bar' },
      { id: 'd1', kind: 'dec', lane: 1, y: 540, label: 'Đạt cột mốc?' },
      { id: 'a5', kind: 'act', lane: 1, y: 650, label: 'Tiếp tục bình thường' },
      { id: 'a6', kind: 'act', lane: 1, y: 650, label: 'Trao huy hiệu (achievements)' },
      { id: 'a7', kind: 'act', lane: 1, y: 740, label: 'Popup chúc mừng' },
      { id: 'e', kind: 'end', lane: 1, y: 820 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' },
      { from: 'a3', to: 'a4' }, { from: 'a4', to: 'd1' },
      { from: 'd1', to: 'a5', label: 'chưa' }, { from: 'd1', to: 'a6', label: 'rồi' },
      { from: 'a6', to: 'a7' }, { from: 'a7', to: 'e' }, { from: 'a5', to: 'e' },
    ],
  },
  // 15 Xem hoàn thành / thất bại
  {
    title: 'Xem danh sách thói quen hoàn thành / thất bại',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Mở tab "Tổng kết"' },
      { id: 'a2', kind: 'act', lane: 0, y: 280, label: 'Chọn lọc Hoàn thành / Thất bại' },
      { id: 'a3', kind: 'act', lane: 1, y: 370, label: 'Truy vấn habit_logs\ntheo bộ lọc' },
      { id: 'a4', kind: 'act', lane: 1, y: 460, label: 'Phân loại theo nhóm' },
      { id: 'a5', kind: 'act', lane: 1, y: 540, label: 'Render danh sách kèm icon trạng thái' },
      { id: 'a6', kind: 'act', lane: 0, y: 620, label: 'Nhấn 1 mục để xem chi tiết' },
      { id: 'a7', kind: 'act', lane: 1, y: 700, label: 'Mở HabitDetailScreen' },
      { id: 'e', kind: 'end', lane: 1, y: 790 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' },
      { from: 'a3', to: 'a4' }, { from: 'a4', to: 'a5' }, { from: 'a5', to: 'a6' }, { from: 'a6', to: 'a7' }, { from: 'a7', to: 'e' },
    ],
  },
  // 16 Tham gia thử thách
  {
    title: 'Tham gia thử thách cá nhân',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Mở tab "Thử thách"' },
      { id: 'a2', kind: 'act', lane: 1, y: 280, label: 'Load danh sách challenges\nđang mở' },
      { id: 'a3', kind: 'act', lane: 0, y: 370, label: 'Chọn 1 thử thách' },
      { id: 'a4', kind: 'act', lane: 1, y: 450, label: 'Hiển thị mô tả + luật strict-daily' },
      { id: 'a5', kind: 'act', lane: 0, y: 540, label: 'Nhấn "Tham gia"' },
      { id: 'a6', kind: 'act', lane: 1, y: 620, label: 'Insert user_challenges' },
      { id: 'a7', kind: 'act', lane: 1, y: 700, label: 'Đặt ChallengeReminderWorker' },
      { id: 'a8', kind: 'act', lane: 1, y: 780, label: 'Hiển thị tiến độ ban đầu' },
      { id: 'e', kind: 'end', lane: 1, y: 870 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' },
      { from: 'a3', to: 'a4' }, { from: 'a4', to: 'a5' }, { from: 'a5', to: 'a6' },
      { from: 'a6', to: 'a7' }, { from: 'a7', to: 'a8' }, { from: 'a8', to: 'e' },
    ],
  },
  // 17 Theo dõi tiến độ thử thách + huy hiệu
  {
    title: 'Theo dõi tiến độ thử thách & nhận huy hiệu',
    nodes: [
      { id: 's', kind: 'start', lane: 0, y: 130 },
      { id: 'a1', kind: 'act', lane: 0, y: 200, label: 'Mở chi tiết thử thách đang tham gia' },
      { id: 'a2', kind: 'act', lane: 1, y: 280, label: 'EvaluateChallengeStatusUseCase' },
      { id: 'a3', kind: 'act', lane: 1, y: 360, label: 'Truy vấn challenge_logs + habit_logs' },
      { id: 'a4', kind: 'act', lane: 1, y: 440, label: 'Tính số ngày đạt strict-daily' },
      { id: 'd1', kind: 'dec', lane: 1, y: 540, label: 'Đạt 100%?' },
      { id: 'a5', kind: 'act', lane: 1, y: 650, label: 'Hiển thị tiến độ' },
      { id: 'a6', kind: 'act', lane: 1, y: 650, label: 'Trao huy hiệu (user_achievements)' },
      { id: 'a7', kind: 'act', lane: 1, y: 740, label: 'Popup + cho phép share ảnh' },
      { id: 'e', kind: 'end', lane: 1, y: 830 },
    ],
    arrows: [
      { from: 's', to: 'a1' }, { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' },
      { from: 'a3', to: 'a4' }, { from: 'a4', to: 'd1' },
      { from: 'd1', to: 'a5', label: 'chưa' }, { from: 'd1', to: 'a6', label: 'rồi' },
      { from: 'a6', to: 'a7' }, { from: 'a7', to: 'e' }, { from: 'a5', to: 'e' },
    ],
  },
];

const BDTT_DEFS = [
  // 1 Đăng nhập
  {
    title: 'Đăng nhập / Xác thực người dùng',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_DangNhap', type: 'boundary' },
      { label: 'Ctrl_Dangnhap', type: 'control' },
      { label: 'Firebase Auth', type: 'entity' },
      { label: 'E_TaiKhoan', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Mở ứng dụng' },
      { y: 270, from: 1, to: 2, label: '2. Yêu cầu đăng nhập' },
      { y: 310, from: 2, to: 3, label: '3. requestCredential()' },
      { y: 350, from: 3, to: 2, label: '4. idToken', ret: true },
      { y: 390, from: 2, to: 3, label: '5. signInWithCredential(idToken)' },
      { y: 430, from: 3, to: 2, label: '6. AuthResult(uid)', ret: true },
      { y: 470, from: 2, to: 4, label: '7. upsert(user)' },
      { y: 510, from: 4, to: 2, label: '8. ok', ret: true },
      { y: 550, from: 2, to: 1, label: '9. navigate(Home)', ret: true },
      { y: 590, from: 1, to: 0, label: '10. Hiển thị Home', ret: true },
    ],
    altBlocks: [{ from: 2, to: 3, y: 620, h: 100, label: 'Đăng nhập thất bại' }],
  },
  // 2 Onboarding
  {
    title: 'Onboarding & chọn nhóm thói quen',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_Onboarding', type: 'boundary' },
      { label: 'Ctrl_Onboarding', type: 'control' },
      { label: 'E_TaiKhoan', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Xem slide 1-3' },
      { y: 270, from: 0, to: 1, label: '2. Nhấn "Bắt đầu"' },
      { y: 310, from: 1, to: 2, label: '3. loadCategories()' },
      { y: 350, from: 2, to: 1, label: '4. List<Category>', ret: true },
      { y: 390, from: 0, to: 1, label: '5. Chọn nhóm yêu thích' },
      { y: 430, from: 1, to: 2, label: '6. saveUserCategories()' },
      { y: 470, from: 2, to: 3, label: '7. update onboardingDone=true' },
      { y: 510, from: 3, to: 2, label: '8. ok', ret: true },
      { y: 550, from: 2, to: 1, label: '9. navigate(Home)', ret: true },
    ],
  },
  // 3 Xem danh sách thói quen
  {
    title: 'Xem danh sách thói quen',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ThoiQuen', type: 'boundary' },
      { label: 'Ctrl_ThoiQuen', type: 'control' },
      { label: 'E_ThoiQuen', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Vào tab Thói quen' },
      { y: 270, from: 1, to: 2, label: '2. loadHabits()' },
      { y: 310, from: 2, to: 3, label: '3. selectAll(userId)' },
      { y: 350, from: 3, to: 2, label: '4. List<Habit>', ret: true },
      { y: 390, from: 2, to: 3, label: '5. selectTodayLogs()' },
      { y: 430, from: 3, to: 2, label: '6. List<HabitLog>', ret: true },
      { y: 470, from: 2, to: 1, label: '7. render(state)', ret: true },
      { y: 510, from: 1, to: 0, label: '8. Hiển thị danh sách', ret: true },
    ],
  },
  // 4 Tạo thói quen
  {
    title: 'Tạo thói quen mới',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ThemThoiQuen', type: 'boundary' },
      { label: 'Ctrl_ThoiQuen', type: 'control' },
      { label: 'E_ThoiQuen', type: 'entity' },
      { label: 'AlarmManager', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Nhấn FAB Thêm' },
      { y: 270, from: 1, to: 0, label: '2. Hiển thị form', ret: true },
      { y: 310, from: 0, to: 1, label: '3. Nhập + Lưu' },
      { y: 350, from: 1, to: 2, label: '4. createHabit(dto)' },
      { y: 390, from: 2, to: 2, label: '5. validate()', self: true },
      { y: 430, from: 2, to: 3, label: '6. insert(habit)' },
      { y: 470, from: 3, to: 2, label: '7. id', ret: true },
      { y: 510, from: 2, to: 4, label: '8. schedule(reminder)' },
      { y: 550, from: 2, to: 1, label: '9. ok', ret: true },
      { y: 590, from: 1, to: 0, label: '10. Hiển thị toast thành công', ret: true },
    ],
  },
  // 5 Sửa
  {
    title: 'Chỉnh sửa thói quen',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ChiTietThoiQuen', type: 'boundary' },
      { label: 'Ctrl_ThoiQuen', type: 'control' },
      { label: 'E_ThoiQuen', type: 'entity' },
      { label: 'AlarmManager', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Nhấn "Sửa"' },
      { y: 270, from: 1, to: 2, label: '2. loadDetail(id)' },
      { y: 310, from: 2, to: 3, label: '3. selectById(id)' },
      { y: 350, from: 3, to: 2, label: '4. Habit', ret: true },
      { y: 390, from: 2, to: 1, label: '5. fill form', ret: true },
      { y: 430, from: 0, to: 1, label: '6. Sửa + Lưu' },
      { y: 470, from: 1, to: 2, label: '7. updateHabit(dto)' },
      { y: 510, from: 2, to: 3, label: '8. update(habit)' },
      { y: 550, from: 2, to: 4, label: '9. reschedule(reminder)' },
      { y: 590, from: 2, to: 1, label: '10. ok', ret: true },
    ],
  },
  // 6 Xóa
  {
    title: 'Xóa thói quen',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ChiTietThoiQuen', type: 'boundary' },
      { label: 'Ctrl_ThoiQuen', type: 'control' },
      { label: 'E_ThoiQuen', type: 'entity' },
      { label: 'AlarmManager', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Nhấn "Xóa"' },
      { y: 270, from: 1, to: 0, label: '2. Hiển thị dialog xác nhận', ret: true },
      { y: 310, from: 0, to: 1, label: '3. Xác nhận' },
      { y: 350, from: 1, to: 2, label: '4. deleteHabit(id)' },
      { y: 390, from: 2, to: 3, label: '5. softDelete(id)' },
      { y: 430, from: 2, to: 4, label: '6. cancel(reminder)' },
      { y: 470, from: 2, to: 1, label: '7. ok', ret: true },
      { y: 510, from: 1, to: 0, label: '8. Quay về danh sách', ret: true },
    ],
  },
  // 7 Xem chi tiết
  {
    title: 'Xem chi tiết thói quen',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ChiTietThoiQuen', type: 'boundary' },
      { label: 'Ctrl_ThoiQuen', type: 'control' },
      { label: 'Ctrl_ChartService', type: 'control' },
      { label: 'E_ThoiQuen', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Chọn 1 thói quen' },
      { y: 270, from: 1, to: 2, label: '2. loadDetail(id)' },
      { y: 310, from: 2, to: 4, label: '3. selectByIdWithLogs(id)' },
      { y: 350, from: 4, to: 2, label: '4. HabitDetail', ret: true },
      { y: 390, from: 2, to: 3, label: '5. buildHeatmap(logs)' },
      { y: 430, from: 3, to: 2, label: '6. HeatmapData', ret: true },
      { y: 470, from: 2, to: 1, label: '7. render(detail+chart)', ret: true },
      { y: 510, from: 1, to: 0, label: '8. Hiển thị chi tiết', ret: true },
    ],
  },
  // 8 Check-in ảnh
  {
    title: 'Chụp ảnh check-in mỗi ngày',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ChiTietThoiQuen', type: 'boundary' },
      { label: 'Ctrl_ThoiQuen', type: 'control' },
      { label: 'Cloudinary', type: 'entity' },
      { label: 'E_ThoiQuen', type: 'entity' },
      { label: 'Ctrl_ThuThach', type: 'control' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Nhấn Check-in' },
      { y: 270, from: 1, to: 0, label: '2. Yêu cầu quyền camera', ret: true },
      { y: 310, from: 0, to: 1, label: '3. Chụp / chọn ảnh' },
      { y: 350, from: 1, to: 2, label: '4. checkIn(uri)' },
      { y: 390, from: 2, to: 3, label: '5. upload(file)' },
      { y: 430, from: 3, to: 2, label: '6. photoUrl', ret: true },
      { y: 470, from: 2, to: 4, label: '7. insertLog(habitId, photoUrl)' },
      { y: 510, from: 2, to: 5, label: '8. evaluateChallenges(userId, date)' },
      { y: 550, from: 5, to: 2, label: '9. updatedChallenges', ret: true },
      { y: 590, from: 2, to: 1, label: '10. ok', ret: true },
      { y: 630, from: 1, to: 0, label: '11. Popup chúc mừng', ret: true },
    ],
  },
  // 9 Nhắc nhở
  {
    title: 'Nhận thông báo nhắc nhở',
    lifelines: [
      { label: 'AlarmManager', type: 'entity' },
      { label: 'Ctrl_NotificationService', type: 'control' },
      { label: 'E_ThoiQuen', type: 'entity' },
      { label: 'GD_Notification', type: 'boundary' },
      { label: 'Người dùng', type: 'actor' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Trigger HabitReminderReceiver' },
      { y: 270, from: 1, to: 2, label: '2. getTodayLog(habitId)' },
      { y: 310, from: 2, to: 1, label: '3. log?', ret: true },
      { y: 350, from: 1, to: 1, label: '4. checkAlreadyDone()', self: true },
      { y: 390, from: 1, to: 3, label: '5. postNotification()' },
      { y: 430, from: 3, to: 4, label: '6. Hiển thị', ret: true },
      { y: 470, from: 4, to: 3, label: '7. Tap notification' },
      { y: 510, from: 3, to: 1, label: '8. openHabitDetail(id)' },
    ],
  },
  // 10 Chat AI
  {
    title: 'Trò chuyện AI',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ChatAI', type: 'boundary' },
      { label: 'Ctrl_AIService', type: 'control' },
      { label: 'Gemini API', type: 'entity' },
      { label: 'OpenRouter API', type: 'entity' },
      { label: 'E_AiCache', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Gửi câu hỏi' },
      { y: 270, from: 1, to: 2, label: '2. ask(prompt)' },
      { y: 310, from: 2, to: 5, label: '3. lookupCache(hash)' },
      { y: 350, from: 5, to: 2, label: '4. miss', ret: true },
      { y: 390, from: 2, to: 3, label: '5. generate(prompt) [singleFlight]' },
      { y: 430, from: 3, to: 2, label: '6. response', ret: true },
      { y: 470, from: 2, to: 5, label: '7. save(cache)' },
      { y: 510, from: 2, to: 1, label: '8. stream answer', ret: true },
      { y: 550, from: 1, to: 0, label: '9. Hiển thị tin nhắn', ret: true },
    ],
    altBlocks: [{ from: 2, to: 4, y: 600, h: 110, label: 'Gemini lỗi → fallback OpenRouter 7-model' }],
  },
  // 11 AI phân tích
  {
    title: 'Nhận phân tích thói quen từ AI',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ChatAI', type: 'boundary' },
      { label: 'Ctrl_AIService', type: 'control' },
      { label: 'E_ThoiQuen', type: 'entity' },
      { label: 'Gemini API', type: 'entity' },
      { label: 'E_AiCache', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Yêu cầu phân tích' },
      { y: 270, from: 1, to: 2, label: '2. analyze(userId, 30d)' },
      { y: 310, from: 2, to: 3, label: '3. selectLogs(30d)' },
      { y: 350, from: 3, to: 2, label: '4. logs', ret: true },
      { y: 390, from: 2, to: 2, label: '5. summarize(logs)', self: true },
      { y: 430, from: 2, to: 4, label: '6. callAi(category=ANALYSIS)' },
      { y: 470, from: 4, to: 2, label: '7. insight', ret: true },
      { y: 510, from: 2, to: 5, label: '8. save(cache, ttl=24h)' },
      { y: 550, from: 2, to: 1, label: '9. render(insight)', ret: true },
    ],
  },
  // 12 AI gợi ý điều chỉnh
  {
    title: 'Nhận gợi ý điều chỉnh thói quen',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ChatAI', type: 'boundary' },
      { label: 'Ctrl_AIService', type: 'control' },
      { label: 'E_ThoiQuen', type: 'entity' },
      { label: 'OpenRouter API', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Mở mục đề xuất AI' },
      { y: 270, from: 1, to: 2, label: '2. suggest(userId)' },
      { y: 310, from: 2, to: 3, label: '3. getContext(habits, logs)' },
      { y: 350, from: 3, to: 2, label: '4. ctx', ret: true },
      { y: 390, from: 2, to: 4, label: '5. callAi(category=ADVICE)' },
      { y: 430, from: 4, to: 2, label: '6. suggestionList', ret: true },
      { y: 470, from: 2, to: 1, label: '7. render(suggestions)', ret: true },
      { y: 510, from: 0, to: 1, label: '8. Nhấn Áp dụng' },
      { y: 550, from: 1, to: 2, label: '9. applySuggestion()' },
      { y: 590, from: 2, to: 3, label: '10. createOrUpdateHabit()' },
    ],
  },
  // 13 Biểu đồ
  {
    title: 'Xem biểu đồ thống kê tuần / tháng',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ThongKe', type: 'boundary' },
      { label: 'Ctrl_ThongKe', type: 'control' },
      { label: 'Ctrl_ChartService', type: 'control' },
      { label: 'E_ThoiQuen', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Chọn tab Thống kê' },
      { y: 270, from: 1, to: 2, label: '2. loadStats(period)' },
      { y: 310, from: 2, to: 4, label: '3. selectLogs(period)' },
      { y: 350, from: 4, to: 2, label: '4. logs[]', ret: true },
      { y: 390, from: 2, to: 3, label: '5. buildChartData(logs)' },
      { y: 430, from: 3, to: 2, label: '6. chartData', ret: true },
      { y: 470, from: 2, to: 1, label: '7. render(chart)', ret: true },
      { y: 510, from: 1, to: 0, label: '8. Hiển thị biểu đồ', ret: true },
    ],
  },
  // 14 Tiến trình
  {
    title: 'Theo dõi tiến trình thói quen',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ChiTietThoiQuen', type: 'boundary' },
      { label: 'Ctrl_ThoiQuen', type: 'control' },
      { label: 'E_ThoiQuen', type: 'entity' },
      { label: 'E_ThanhTich', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Mở chi tiết' },
      { y: 270, from: 1, to: 2, label: '2. getProgress(id)' },
      { y: 310, from: 2, to: 3, label: '3. selectLogs(habitId)' },
      { y: 350, from: 3, to: 2, label: '4. logs', ret: true },
      { y: 390, from: 2, to: 2, label: '5. computeStreak()', self: true },
      { y: 430, from: 2, to: 4, label: '6. checkMilestones(streak)' },
      { y: 470, from: 4, to: 2, label: '7. newBadges?', ret: true },
      { y: 510, from: 2, to: 1, label: '8. render(progress+badges)', ret: true },
    ],
  },
  // 15 Hoàn thành / thất bại
  {
    title: 'Xem danh sách hoàn thành / thất bại',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ThongKe', type: 'boundary' },
      { label: 'Ctrl_ThongKe', type: 'control' },
      { label: 'E_ThoiQuen', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Mở tab Tổng kết' },
      { y: 270, from: 0, to: 1, label: '2. Chọn bộ lọc' },
      { y: 310, from: 1, to: 2, label: '3. filter(status)' },
      { y: 350, from: 2, to: 3, label: '4. selectByStatus(status)' },
      { y: 390, from: 3, to: 2, label: '5. List<Habit>', ret: true },
      { y: 430, from: 2, to: 1, label: '6. render(list)', ret: true },
      { y: 470, from: 1, to: 0, label: '7. Hiển thị', ret: true },
    ],
  },
  // 16 Tham gia thử thách
  {
    title: 'Tham gia thử thách cá nhân',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ThuThach', type: 'boundary' },
      { label: 'Ctrl_ThuThach', type: 'control' },
      { label: 'E_ThuThach', type: 'entity' },
      { label: 'E_NguoiDungThuThach', type: 'entity' },
      { label: 'WorkManager', type: 'entity' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Mở tab Thử thách' },
      { y: 270, from: 1, to: 2, label: '2. loadActive()' },
      { y: 310, from: 2, to: 3, label: '3. selectActive()' },
      { y: 350, from: 3, to: 2, label: '4. List<Challenge>', ret: true },
      { y: 390, from: 2, to: 1, label: '5. render(list)', ret: true },
      { y: 430, from: 0, to: 1, label: '6. Chọn + Tham gia' },
      { y: 470, from: 1, to: 2, label: '7. join(challengeId)' },
      { y: 510, from: 2, to: 4, label: '8. insert(userChallenge)' },
      { y: 550, from: 2, to: 5, label: '9. enqueue(ChallengeReminder)' },
      { y: 590, from: 2, to: 1, label: '10. ok', ret: true },
    ],
  },
  // 17 Tiến độ + huy hiệu
  {
    title: 'Theo dõi tiến độ thử thách & nhận huy hiệu',
    lifelines: [
      { label: 'Người dùng', type: 'actor' },
      { label: 'GD_ThuThach', type: 'boundary' },
      { label: 'Ctrl_ThuThach', type: 'control' },
      { label: 'E_NguoiDungThuThach', type: 'entity' },
      { label: 'E_ThanhTich', type: 'entity' },
      { label: 'Ctrl_PopupService', type: 'control' },
    ],
    messages: [
      { y: 230, from: 0, to: 1, label: '1. Mở chi tiết thử thách' },
      { y: 270, from: 1, to: 2, label: '2. getProgress(challengeId)' },
      { y: 310, from: 2, to: 3, label: '3. selectByUser(userId)' },
      { y: 350, from: 3, to: 2, label: '4. logs', ret: true },
      { y: 390, from: 2, to: 2, label: '5. evaluateStrictDaily()', self: true },
      { y: 430, from: 2, to: 4, label: '6. grantBadge() (nếu hoàn thành)' },
      { y: 470, from: 4, to: 2, label: '7. achievementId', ret: true },
      { y: 510, from: 2, to: 5, label: '8. showCelebration()' },
      { y: 550, from: 5, to: 1, label: '9. popup overlay', ret: true },
      { y: 590, from: 1, to: 0, label: '10. Hiển thị + cho phép Share', ret: true },
    ],
  },
];

// ---------- Architecture: Class diagram ----------
function pageClassDiagram() {
  const w = 1654, h = 1169;
  let out = svgHeader(w, h, 'Hình 4.1 — Sơ đồ Lớp (Class Diagram) — domain entities + repositories + use cases');
  out += arrowDefs();
  // helper to draw a class box
  const cls = (x, y, w_, name, attrs, ops, stereo) => {
    const lhs = 18;
    const headerH = stereo ? 50 : 30;
    const attrH = 16;
    const opsStart = headerH + attrs.length * attrH + 6;
    const total = opsStart + ops.length * attrH + 6;
    let s = '';
    s += rect(x, y, w_, total);
    // header
    s += rect(x, y, w_, headerH, { fill: HEADER_FILL });
    if (stereo) s += txt(x + w_/2, y + 16, `«${stereo}»`, { size: 10, italic: true });
    s += txt(x + w_/2, y + (stereo ? 38 : 20), name, { size: 12, bold: true });
    // separators
    s += line(x, y + headerH, x + w_, y + headerH);
    for (let i = 0; i < attrs.length; i++) {
      s += txt(x + lhs, y + headerH + (i+1)*attrH - 3, attrs[i], { size: 10, anchor: 'start' });
    }
    s += line(x, y + opsStart - 4, x + w_, y + opsStart - 4);
    for (let i = 0; i < ops.length; i++) {
      s += txt(x + lhs, y + opsStart + (i+1)*attrH - 3, ops[i], { size: 10, anchor: 'start' });
    }
    return { svg: s, x, y, w: w_, h: total };
  };
  // Domain entities (top row)
  const user = cls(40, 60, 230, 'User', ['- id: String','- email: String','- displayName: String','- photoUrl: String','- onboardingDone: Bool'], ['+ updateProfile()'], 'entity');
  const habit = cls(300, 60, 250, 'Habit', ['- id: String','- userId: String','- name: String','- description: String','- targetPerDay: Int','- categoryId: String','- reminderTime: LocalTime?'], ['+ isActiveToday(): Bool'], 'entity');
  const habitLog = cls(580, 60, 230, 'HabitLog', ['- id: String','- habitId: String','- date: LocalDate','- done: Bool','- photoUrl: String?','- note: String?'], ['+ isCheckedIn(): Bool'], 'entity');
  const category = cls(840, 60, 220, 'Category', ['- id: String','- name: String','- icon: String','- color: String','- isDefault: Bool'], [], 'entity');
  const challenge = cls(1090, 60, 230, 'Challenge', ['- id: String','- title: String','- description: String','- durationDays: Int','- ruleStrictDaily: Bool'], ['+ isExpired(): Bool'], 'entity');
  const userChallenge = cls(1350, 60, 270, 'UserChallenge', ['- id: String','- userId: String','- challengeId: String','- startDate: LocalDate','- progress: Int','- status: Status'], ['+ percent(): Float'], 'entity');
  // second row
  const achievement = cls(40, 360, 230, 'Achievement', ['- id: String','- code: String','- title: String','- iconUrl: String','- criteria: String'], [], 'entity');
  const reminder = cls(300, 360, 250, 'Reminder', ['- id: String','- habitId: String','- timeOfDay: LocalTime','- daysOfWeek: Set<DayOfWeek>'], ['+ nextTrigger(): Instant'], 'entity');
  const aiChat = cls(580, 360, 230, 'AiChat', ['- id: String','- userId: String','- role: String','- content: String','- createdAt: Instant'], [], 'entity');
  const aiCache = cls(840, 360, 220, 'AiCache', ['- key: String','- response: String','- category: String','- ttlAt: Instant'], ['+ isExpired(): Bool'], 'entity');
  const notif = cls(1090, 360, 230, 'Notification', ['- id: String','- userId: String','- type: String','- payload: String','- read: Bool'], [], 'entity');
  const settings = cls(1350, 360, 270, 'UserSettings', ['- userId: String','- theme: Theme','- reminderEnabled: Bool','- aiEnabled: Bool','- language: Lang'], [], 'entity');
  // Repositories
  const habitRepo = cls(40, 660, 280, 'HabitRepository', [], ['+ getAll(userId): Flow<List<Habit>>','+ insert(habit): Long','+ update(habit)','+ delete(id)','+ findById(id): Habit?'], 'interface');
  const logRepo = cls(340, 660, 280, 'HabitLogRepository', [], ['+ insertLog(log)','+ logsForHabit(id): Flow<List<HabitLog>>','+ logsBetween(from,to): List<HabitLog>'], 'interface');
  const challRepo = cls(640, 660, 280, 'ChallengeRepository', [], ['+ activeChallenges(): Flow<List<Challenge>>','+ join(userId, challengeId)','+ progress(userId): UserChallenge'], 'interface');
  const aiRepo = cls(940, 660, 280, 'AiChatRepository + AiCacheRepository', [], ['+ ask(prompt): Flow<String>','+ history(userId): Flow<List<AiChat>>','+ lookup(key): AiCache?','+ save(entry)'], 'interface');
  const syncRepo = cls(1240, 660, 380, 'SyncCoordinator / SyncStatusRepository', [], ['+ enqueuePeriodic()','+ enqueueOneShot()','+ status(): Flow<SyncStatus>','+ runEntitySynchronizers()'], 'interface');
  // Use cases
  const ucCheckIn = cls(40, 940, 300, 'CheckInHabitUseCase', [], ['+ invoke(habitId, photo)'], 'usecase');
  const ucEval = cls(360, 940, 320, 'EvaluateChallengeStatusUseCase', [], ['+ invoke(userId, date)'], 'usecase');
  const ucBuildShare = cls(700, 940, 380, 'BuildChallengeProgressShareTextUseCase', [], ['+ invoke(challengeId): ShareData'], 'usecase');
  const ucAiAnalyze = cls(1100, 940, 250, 'AnalyzeHabitsUseCase', [], ['+ invoke(userId, range)'], 'usecase');
  const ucAiAdvice = cls(1370, 940, 250, 'SuggestHabitsUseCase', [], ['+ invoke(userId)'], 'usecase');

  for (const c of [user, habit, habitLog, category, challenge, userChallenge, achievement, reminder, aiChat, aiCache, notif, settings, habitRepo, logRepo, challRepo, aiRepo, syncRepo, ucCheckIn, ucEval, ucBuildShare, ucAiAnalyze, ucAiAdvice]) out += c.svg;

  // associations (simple lines)
  const assoc = (a, b, mulA, mulB, label) => {
    const x1 = a.x + a.w / 2;
    const y1 = a.y + a.h;
    const x2 = b.x + b.w / 2;
    const y2 = b.y;
    out += `<polyline points="${x1},${y1} ${x1},${(y1+y2)/2} ${x2},${(y1+y2)/2} ${x2},${y2}" fill="none" stroke="${STROKE}" stroke-width="1.2"/>`;
    if (mulA) out += txt(x1 + 6, y1 + 12, mulA, { size: 9, anchor: 'start' });
    if (mulB) out += txt(x2 + 6, y2 - 4, mulB, { size: 9, anchor: 'start' });
    if (label) out += txt((x1+x2)/2, (y1+y2)/2 - 4, label, { size: 9, italic: true });
  };
  assoc(user, habit, '1', '0..*', 'có');
  assoc(habit, habitLog, '1', '0..*', 'gồm');
  assoc(habit, category, '*', '1', 'thuộc');
  assoc(user, userChallenge, '1', '0..*', 'tham gia');
  assoc(challenge, userChallenge, '1', '0..*', '');
  assoc(user, achievement, '*', '*', 'đạt');
  assoc(habit, reminder, '1', '0..*', 'đặt');
  assoc(user, settings, '1', '1', '');
  return out + svgFooter();
}

// ---------- Component diagram ----------
function pageComponent() {
  const w = 1654, h = 1169;
  let out = svgHeader(w, h, 'Hình 4.2 — Sơ đồ Thành phần (Component) — Kiến trúc 4 tầng + dịch vụ ngoài');
  out += arrowDefs();
  // layered boxes (top to bottom)
  const layer = (x, y, w_, h_, title, items) => {
    let s = rect(x, y, w_, h_, { sw: 2 });
    s += rect(x, y, w_, 32, { fill: HEADER_FILL });
    s += txt(x + w_/2, y + 22, title, { bold: true, size: 13 });
    const cols = 4;
    const cw = (w_ - 40) / cols;
    items.forEach((it, i) => {
      const r = Math.floor(i / cols);
      const c = i % cols;
      const ix = x + 20 + c * cw;
      const iy = y + 50 + r * 70;
      s += rect(ix, iy, cw - 16, 55);
      // component icon (mini)
      s += rect(ix - 5, iy + 8, 14, 8);
      s += rect(ix - 5, iy + 26, 14, 8);
      s += txt(ix + (cw-16)/2, iy + 32, it, { size: 11 });
    });
    return s;
  };
  out += layer(60, 60, 1530, 230, 'Tầng Trình bày (Presentation — Jetpack Compose, MVI)',
    ['HomeScreen','HabitsScreen','HabitDetailScreen','AddHabitScreen','ChatAIScreen','ChallengeScreen','StatsScreen','ProfileScreen']);
  out += layer(60, 310, 1530, 230, 'Tầng Nghiệp vụ (Domain — Use Cases)',
    ['CheckInHabitUseCase','EvaluateChallengeStatusUseCase','AnalyzeHabitsUseCase','SuggestHabitsUseCase','BuildChallengeProgressShareTextUseCase','AiChatRouter','SingleFlight','EvaluateAchievementsUseCase']);
  out += layer(60, 560, 1530, 230, 'Tầng Dữ liệu (Data — Room v14 + Repositories + Sync)',
    ['HabitRepository','HabitLogRepository','ChallengeRepository','AiChatRepository','AiCacheRepository','UserRepository','SyncCoordinator','7×EntitySynchronizer']);
  out += layer(60, 810, 1530, 230, 'Tầng Nền (Background — WorkManager + AlarmManager + Receivers)',
    ['SyncWorker (30m)','MidnightCleanupWorker','ChallengeReminderWorker','HabitReminderReceiver','HabitBootReceiver','ConnectivityObserver','NotificationService','PopupService']);
  // external services
  out += rect(60, 1060, 1530, 90, { sw: 2, dash: true });
  out += txt(60 + 765, 1080, '«external»', { size: 11, italic: true, fill: DASH });
  const exts = ['Firebase Auth','Firestore','Gemini API','OpenRouter API','Cloudinary'];
  exts.forEach((e, i) => {
    const ix = 80 + i * 300;
    const iy = 1095;
    out += rect(ix, iy, 260, 40, { dash: true });
    out += txt(ix + 130, iy + 25, e, { size: 12, bold: true });
  });
  // vertical depends-on arrows between layers
  for (let i = 0; i < 4; i++) {
    const ax = 200 + i * 350;
    out += line(ax, 290, ax, 310, { dash: true, openArrow: true, stroke: DASH });
    out += line(ax, 540, ax, 560, { dash: true, openArrow: true, stroke: DASH });
    out += line(ax, 790, ax, 810, { dash: true, openArrow: true, stroke: DASH });
    out += line(ax, 1040, ax, 1060, { dash: true, openArrow: true, stroke: DASH });
  }
  return out + svgFooter();
}

// ---------- ERD ----------
function pageERD() {
  const w = 1654, h = 1169;
  let out = svgHeader(w, h, 'Hình 4.3 — Sơ đồ Thực thể-Liên kết (ERD) — 16 bảng Room v14');
  out += arrowDefs();
  const tbl = (x, y, w_, name, cols) => {
    const headerH = 30;
    const rowH = 18;
    const total = headerH + cols.length * rowH;
    let s = rect(x, y, w_, total);
    s += rect(x, y, w_, headerH, { fill: HEADER_FILL });
    s += txt(x + w_/2, y + 20, name, { bold: true, size: 12 });
    cols.forEach((c, i) => {
      if (c.startsWith('PK:')) {
        s += rect(x, y + headerH + i*rowH, w_, rowH, { fill: HEADER_FILL, sw: 0.8 });
      } else {
        s += line(x, y + headerH + i*rowH, x + w_, y + headerH + i*rowH, { sw: 0.5 });
      }
      s += txt(x + 8, y + headerH + i*rowH + 13, c, { size: 10, anchor: 'start' });
    });
    return { svg: s, x, y, w: w_, h: total };
  };
  const tabs = [];
  // Row 1
  tabs.push(tbl(40, 60, 230, 'users',
    ['PK: id : String','email : String','displayName : String','photoUrl : String','onboardingDone : Bool','createdAt : Long']));
  tabs.push(tbl(290, 60, 230, 'habits',
    ['PK: id : String','FK: userId → users','FK: categoryId → categories','name : String','description : String','targetPerDay : Int','reminderTime : LocalTime?','isArchived : Bool']));
  tabs.push(tbl(540, 60, 230, 'habit_logs',
    ['PK: id : String','FK: habitId → habits','date : LocalDate','done : Bool','photoUrl : String?','note : String?','createdAt : Long']));
  tabs.push(tbl(790, 60, 230, 'categories',
    ['PK: id : String','name : String','icon : String','color : String','isDefault : Bool']));
  tabs.push(tbl(1040, 60, 230, 'user_categories',
    ['PK: id : String','FK: userId → users','FK: categoryId → categories','priority : Int']));
  tabs.push(tbl(1290, 60, 320, 'challenges',
    ['PK: id : String','title : String','description : String','durationDays : Int','ruleStrictDaily : Bool','iconUrl : String?','createdAt : Long']));
  // Row 2
  tabs.push(tbl(40, 360, 230, 'user_challenges',
    ['PK: id : String','FK: userId → users','FK: challengeId → challenges','startDate : LocalDate','progress : Int','status : Status']));
  tabs.push(tbl(290, 360, 230, 'challenge_logs',
    ['PK: id : String','FK: userChallengeId','date : LocalDate','done : Bool','source : String']));
  tabs.push(tbl(540, 360, 230, 'reminders',
    ['PK: id : String','FK: habitId → habits','timeOfDay : LocalTime','daysOfWeek : Set','enabled : Bool']));
  tabs.push(tbl(790, 360, 230, 'achievements',
    ['PK: id : String','code : String','title : String','iconUrl : String','criteria : String']));
  tabs.push(tbl(1040, 360, 230, 'user_achievements',
    ['PK: id : String','FK: userId → users','FK: achievementId','grantedAt : Long']));
  tabs.push(tbl(1290, 360, 320, 'group_teams',
    ['PK: id : String','name : String','ownerUserId : String','memberCount : Int','createdAt : Long']));
  // Row 3
  tabs.push(tbl(40, 660, 230, 'notifications',
    ['PK: id : String','FK: userId → users','type : String','payload : String','read : Bool','createdAt : Long']));
  tabs.push(tbl(290, 660, 230, 'ai_chat',
    ['PK: id : String','FK: userId → users','role : String','content : String','createdAt : Long']));
  tabs.push(tbl(540, 660, 230, 'ai_cache',
    ['PK: key : String','response : String','category : String','ttlAt : Long','createdAt : Long']));
  tabs.push(tbl(790, 660, 230, 'user_settings',
    ['PK: userId : String','theme : Theme','reminderEnabled : Bool','aiEnabled : Bool','language : Lang']));
  for (const t of tabs) out += t.svg;
  // relationship lines (selected high-importance)
  const rel = (a, b, label) => {
    const x1 = a.x + a.w;
    const y1 = a.y + a.h/2;
    const x2 = b.x;
    const y2 = b.y + b.h/2;
    out += `<polyline points="${x1},${y1} ${(x1+x2)/2},${y1} ${(x1+x2)/2},${y2} ${x2},${y2}" fill="none" stroke="${STROKE}" stroke-width="1.2"/>`;
    if (label) out += txt((x1+x2)/2, (y1+y2)/2 - 4, label, { size: 9, italic: true });
  };
  rel(tabs[0], tabs[1], '1—N');
  rel(tabs[1], tabs[2], '1—N');
  rel(tabs[3], tabs[1], '1—N');
  rel(tabs[0], tabs[4], '1—N');
  rel(tabs[3], tabs[4], '1—N');
  rel(tabs[5], tabs[6], '1—N');
  rel(tabs[6], tabs[7], '1—N');
  rel(tabs[1], tabs[8], '1—N');
  rel(tabs[9], tabs[10], '1—N');
  rel(tabs[0], tabs[10], '1—N');
  rel(tabs[0], tabs[12], '1—N');
  rel(tabs[0], tabs[13], '1—N');
  rel(tabs[0], tabs[15], '1—1');
  return out + svgFooter();
}

// ---------- Page registry ----------
const pages = [];

// Use case (3)
pages.push({ file: 'Hinh_2.1_UseCase_TongQuat.svg', render: pageUseCaseTongQuat });
pages.push({ file: 'Hinh_2.2_UseCase_NguoiDung.svg', render: pageUseCaseNguoiDung });
pages.push({ file: 'Hinh_2.3_UseCase_HeThong_AI.svg', render: pageUseCaseHeThongAI });

// Slugify filename component
function slug(s) {
  return s.normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd').replace(/Đ/g, 'D')
    .replace(/[^A-Za-z0-9 ]/g, '')
    .trim().split(/\s+/).join('_');
}

// BDHD + BDTT for 17
const NUMS = [
  '3.1','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9',
  '3.10','3.11','3.12','3.13','3.14','3.15','3.16','3.17'
];
const FILE_KEYS = [
  'DangNhap','Onboarding','XemDSThoiQuen','TaoThoiQuen','SuaThoiQuen','XoaThoiQuen','XemChiTiet',
  'CheckIn','NhacNho','ChatAI','PhanTichAI','GoiYAI','BieuDo','TienTrinh','HoanThanhThatBai',
  'ThamGiaThuThach','TienDoThuThachHuyHieu'
];

for (let i = 0; i < 17; i++) {
  const num = NUMS[i];
  const key = FILE_KEYS[i];
  const bdhd = BDHD_DEFS[i];
  const bdtt = BDTT_DEFS[i];
  pages.push({
    file: `Hinh_${num}_BDHD_${key}.svg`,
    render: () => buildBDHD(num, bdhd.title, ['Người dùng','Hệ thống'], bdhd.nodes, bdhd.arrows),
  });
  pages.push({
    file: `Hinh_${num}_BDTT_${key}.svg`,
    render: () => buildBDTT(num, bdtt.title, bdtt.lifelines, bdtt.messages, bdtt.altBlocks || []),
  });
}

// Architecture (3)
pages.push({ file: 'Hinh_4.1_ClassDiagram.svg', render: pageClassDiagram });
pages.push({ file: 'Hinh_4.2_ComponentDiagram.svg', render: pageComponent });
pages.push({ file: 'Hinh_4.3_ERD.svg', render: pageERD });

// ---------- Write ----------
let written = 0;
for (const p of pages) {
  const svg = p.render();
  writeFileSync(resolve(OUT_DIR, p.file), svg, 'utf8');
  written++;
}
console.log(`render_diagrams.mjs: wrote ${written} SVGs to ${OUT_DIR}`);
