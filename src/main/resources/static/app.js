// Daily Budget web dashboard. Plain JavaScript, no libraries.
//
// How it works:
// 1. Log in with POST /api/auth/login and keep the token in sessionStorage (gone when the tab closes).
// 2. Load everything with GET /api/sync, and the selected month's categories with
//    GET /api/expenses/months/{month}?zone=<browser time zone>.
// 3. Work out today's limit with the same rule as the iPhone app, in the browser's time zone.
//
// Safety: text from the database (notes, bill names) is only ever put on the page with
// textContent, never innerHTML, so it can't turn into HTML or script.
'use strict';

(function () {
  const TOKEN_KEY = 'dailyBudget.token';
  const EXPIRES_KEY = 'dailyBudget.expiresAt';
  const RECENT_LIMIT = 30;

  // Used only if the account has no saved plan yet. Same as the app's defaults.
  const DEFAULT_PLAN = {
    startMonth: '2026-11', monthCount: 6, livingMonthlyCents: 9200,
    totalCents: 470000, engagementCents: 150000,
  };

  const CATEGORY_NAMES = {
    food: 'Food', coffee: 'Coffee', transport: 'Transport', sport: 'Sport', skinCare: 'Skin care',
    phone: 'Phone', shopping: 'Shopping', health: 'Health', fun: 'Fun', bills: 'Bills',
    engagement: 'Engagement', other: 'Other',
  };

  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

  const $ = (id) => document.getElementById(id);

  let state = null;       // { plan, planMonths, bills, expenses, subscriptions } after loading
  let resizeTimer = null;
  let recentPart = 'all'; // which pill is on above Recent expenses

  // ---------- small helpers ----------

  function money(cents) {
    return usd.format(cents / 100);
  }

  function isLiving(category) {
    return category !== 'bills' && category !== 'engagement';
  }

  function pad(n) {
    return String(n).padStart(2, '0');
  }

  /** "2026-11" for a Date, in the browser's time zone. */
  function monthKeyOf(date) {
    return date.getFullYear() + '-' + pad(date.getMonth() + 1);
  }

  /** "2026-11" + 2 → "2027-01" */
  function addMonths(key, n) {
    const [y, m] = key.split('-').map(Number);
    const d = new Date(y, m - 1 + n, 1);
    return monthKeyOf(d);
  }

  function monthLabel(key, withYear) {
    const [y, m] = key.split('-').map(Number);
    return new Date(y, m - 1, 1).toLocaleDateString('en-US',
      withYear ? { month: 'long', year: 'numeric' } : { month: 'short' });
  }

  function sameLocalDay(a, b) {
    return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
  }

  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  const SVG_NS = 'http://www.w3.org/2000/svg';
  function svgEl(tag, attrs, text) {
    const node = document.createElementNS(SVG_NS, tag);
    for (const [k, v] of Object.entries(attrs || {})) node.setAttribute(k, String(v));
    if (text !== undefined) node.textContent = text;
    return node;
  }

  /** A progress bar. The width is set through the style object, which the CSP allows. */
  function bar(cents, ofCents, fillClass) {
    const outer = el('div', 'bar');
    const inner = el('span', cents > ofCents && ofCents > 0 ? 'fill-over' : fillClass);
    const pct = ofCents > 0 ? Math.min(100, (cents / ofCents) * 100) : (cents > 0 ? 100 : 0);
    inner.style.width = pct.toFixed(1) + '%';
    outer.appendChild(inner);
    return outer;
  }

  // ---------- token and API ----------

  function getToken() {
    const token = sessionStorage.getItem(TOKEN_KEY);
    const expires = sessionStorage.getItem(EXPIRES_KEY);
    if (!token) return null;
    if (expires && Date.parse(expires) <= Date.now()) {
      clearToken();
      return null;
    }
    return token;
  }

  function clearToken() {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(EXPIRES_KEY);
  }

  class LoggedOut extends Error {}

  async function api(path) {
    const token = getToken();
    if (!token) {
      showLogin('Please sign in.');
      throw new LoggedOut();
    }
    const res = await fetch(path, { headers: { Authorization: 'Bearer ' + token, Accept: 'application/json' } });
    if (res.status === 401) {
      clearToken();
      showLogin('Your session ended. Please sign in again.');
      throw new LoggedOut();
    }
    if (!res.ok) throw new Error('Server answered ' + res.status + ' for ' + path);
    return res.json();
  }

  // ---------- views ----------

  function showLogin(message) {
    state = null;
    $('dash-view').hidden = true;
    $('login-view').hidden = false;
    const err = $('login-error');
    err.textContent = message || '';
    err.hidden = !message;
    $('password').value = '';
    $('email').focus();
  }

  function showDashboard() {
    $('login-view').hidden = true;
    $('dash-view').hidden = false;
  }

  async function onLogin(event) {
    event.preventDefault();
    const button = $('login-button');
    const err = $('login-error');
    err.hidden = true;
    button.disabled = true;
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ email: $('email').value.trim(), password: $('password').value }),
      });
      if (!res.ok) {
        err.textContent = res.status === 401 ? 'Wrong email or password.'
          : res.status === 400 ? 'Check the email, and that the password has at least 10 characters.'
          : 'Could not sign in (error ' + res.status + ').';
        err.hidden = false;
        return;
      }
      const body = await res.json();
      sessionStorage.setItem(TOKEN_KEY, body.token);
      if (body.expiresAt) sessionStorage.setItem(EXPIRES_KEY, body.expiresAt);
      $('password').value = '';
      showDashboard();
      await load();
    } catch (e) {
      if (!(e instanceof LoggedOut)) {
        err.textContent = 'Could not reach the server. Try again.';
        err.hidden = false;
      }
    } finally {
      button.disabled = false;
    }
  }

  function onLogout() {
    clearToken();
    showLogin();
  }

  // Sign out on every device: the server raises a number that every older token carries,
  // so all of them (the phone's too) stop working. Then this tab signs out as well.
  async function onLogoutAll() {
    const ok = window.confirm("Sign out on every device, including your phone? You'll need to sign in again on each.");
    if (!ok) return;
    const token = getToken();
    if (!token) {
      showLogin('Please sign in.');
      return;
    }
    const button = $('logout-all-button');
    button.disabled = true;
    try {
      const res = await fetch('/api/auth/logout-all', {
        method: 'POST',
        headers: { Authorization: 'Bearer ' + token },
      });
      if (res.status === 204 || res.status === 401) {
        // 401: this token had already stopped working, so the result is the same.
        clearToken();
        showLogin('Signed out on every device.');
        return;
      }
      $('dash-status').textContent = 'Could not sign out everywhere (error ' + res.status + ').';
    } catch (e) {
      $('dash-status').textContent = 'Could not reach the server. Try again.';
    } finally {
      button.disabled = false;
    }
  }

  // ---------- loading ----------

  async function load() {
    const status = $('dash-status');
    status.textContent = 'Loading…';
    try {
      const sync = await api('/api/sync');
      const plan = sync.settings || DEFAULT_PLAN;
      const planMonths = [];
      for (let i = 0; i < plan.monthCount; i++) planMonths.push(addMonths(plan.startMonth, i));
      state = {
        plan,
        planMonths,
        bills: (sync.bills || []).filter((b) => !b.deleted),
        expenses: (sync.expenses || []).filter((e) => !e.deleted)
          .map((e) => Object.assign({}, e, { at: new Date(e.spentAt) }))
          .sort((a, b) => b.at - a.at),
        // Only active, not deleted subscriptions, soonest renewal first.
        subscriptions: (sync.subscriptions || []).filter((s) => !s.deleted && s.active)
          .map((s) => Object.assign({}, s, { renews: new Date(s.nextRenewal) }))
          .sort((a, b) => a.renews - b.renews),
      };
      renderAll();
      status.textContent = (sync.settings ? '' : 'No plan saved yet, showing the default plan. ')
        + 'Updated ' + new Date().toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' })
        + ' · ' + zone;
      await loadCategories();
    } catch (e) {
      if (e instanceof LoggedOut) return;
      status.textContent = 'Could not load your data: ' + e.message;
    }
  }

  async function loadCategories() {
    const month = $('category-month').value;
    const list = $('category-list');
    if (!month) return;
    try {
      const data = await api('/api/expenses/months/' + encodeURIComponent(month) + '?zone=' + encodeURIComponent(zone));
      renderCategories(data);
    } catch (e) {
      if (e instanceof LoggedOut) return;
      list.replaceChildren(el('p', 'empty', 'Could not load categories.'));
    }
  }

  // ---------- calculations ----------

  /** Spent per part (living / bills / engagement) in one month, in the browser's time zone. */
  function monthTotals(monthKey) {
    const t = { living: 0, bills: 0, engagement: 0 };
    for (const e of state.expenses) {
      if (monthKeyOf(e.at) !== monthKey) continue;
      if (e.category === 'bills') t.bills += e.amountCents;
      else if (e.category === 'engagement') t.engagement += e.amountCents;
      else t.living += e.amountCents;
    }
    return t;
  }

  function billsDueIn(monthKey) {
    return state.bills.filter((b) => (b.dueMonths || []).includes(monthKey))
      .reduce((sum, b) => sum + b.amountCents, 0);
  }

  /**
   * The app's rule: daily limit = living money left this month ÷ days left in the month,
   * counting today, in whole cents rounded down. "Left this month" is counted before today's
   * spending, so the limit stays the same all day and "left" goes down as you spend.
   */
  function today() {
    const now = new Date();
    const key = monthKeyOf(now);
    const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
    const daysLeft = daysInMonth - now.getDate() + 1;

    let spentBeforeToday = 0;
    let spentToday = 0;
    for (const e of state.expenses) {
      if (!isLiving(e.category) || monthKeyOf(e.at) !== key) continue;
      if (sameLocalDay(e.at, now)) spentToday += e.amountCents;
      else if (e.at < now) spentBeforeToday += e.amountCents;
    }
    const leftThisMonth = state.plan.livingMonthlyCents - spentBeforeToday;
    const limit = Math.max(0, Math.floor(leftThisMonth / daysLeft));
    return { now, limit, spentToday, left: limit - spentToday, daysLeft, leftThisMonth };
  }

  // ---------- rendering ----------

  function renderAll() {
    renderToday();
    renderMonth();
    renderChart();
    fillMonthSelect();
    renderSubscriptions();
    renderRecent();
  }

  function renderToday() {
    const t = today();
    const hour = t.now.getHours();
    $('greeting').textContent = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
    $('today-date').textContent = t.now.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })
      + ' · ' + t.daysLeft + (t.daysLeft === 1 ? ' day' : ' days') + ' left this month';
    $('today-limit').textContent = money(t.limit);
    $('today-spent').textContent = money(t.spentToday);

    let count = 0;
    for (const e of state.expenses) if (isLiving(e.category) && sameLocalDay(e.at, t.now)) count++;
    $('today-count').textContent = count === 0 ? 'nothing yet' : count + (count === 1 ? ' expense' : ' expenses');

    const left = $('today-left');
    left.textContent = money(t.left);
    left.classList.toggle('negative', t.left < 0);
    const chip = $('today-chip');
    chip.className = 'chip ' + (t.left < 0 ? 'over' : 'good');
    chip.textContent = t.left < 0 ? 'over today' : 'on budget';

    const monthLeft = $('month-left');
    monthLeft.textContent = money(t.leftThisMonth);
    monthLeft.classList.toggle('negative', t.leftThisMonth < 0);
    $('today-note').textContent = 'for ' + t.daysLeft + (t.daysLeft === 1 ? ' day' : ' days') + ', today included';
  }

  function splitRow(label, spent, plan, fillClass, detail) {
    const row = el('div', 'split-row');
    const top = el('div', 'split-top');
    top.appendChild(el('span', '', label));
    top.appendChild(el('span', 'num', money(spent) + ' of ' + money(plan)));
    row.appendChild(top);
    row.appendChild(bar(spent, plan, fillClass));
    if (detail) row.appendChild(el('span', 'muted small', detail));
    return row;
  }

  function renderMonth() {
    const key = monthKeyOf(new Date());
    $('month-name').textContent = monthLabel(key, true);
    const t = monthTotals(key);
    const plan = state.plan;
    const billsPlan = billsDueIn(key);

    // Engagement money is one amount for the whole plan, so compare everything spent on it so far.
    let engagementSoFar = 0;
    for (const m of state.planMonths) engagementSoFar += monthTotals(m).engagement;
    if (!state.planMonths.includes(key)) engagementSoFar += t.engagement;

    const livingLeft = plan.livingMonthlyCents - t.living;
    const box = $('month-split');
    box.replaceChildren(
      splitRow('Living', t.living, plan.livingMonthlyCents, 'fill-living',
        livingLeft >= 0 ? money(livingLeft) + ' left' : money(-livingLeft) + ' over'),
      splitRow('Bills', t.bills, billsPlan, 'fill-bills',
        billsPlan === 0 && t.bills === 0 ? 'No bills due this month' : 'Due this month: ' + money(billsPlan)),
      splitRow('Engagement (whole plan)', engagementSoFar, plan.engagementCents, 'fill-engagement',
        'This month: ' + money(t.engagement)),
    );
    renderDonut(t);

    // Side menu: how much of this month's living money is used, and a few counts.
    const used = plan.livingMonthlyCents > 0 ? Math.min(100, (t.living / plan.livingMonthlyCents) * 100) : 0;
    const fill = $('side-plan-fill');
    fill.style.width = used.toFixed(1) + '%';
    fill.className = t.living > plan.livingMonthlyCents ? 'fill-over' : 'fill-sun';
    $('side-plan-text').textContent = money(t.living) + ' of ' + money(plan.livingMonthlyCents) + ' spent';
    $('nav-expenses').textContent = state.expenses.length ? String(state.expenses.length) : '';
    $('nav-subs').textContent = state.subscriptions.length ? String(state.subscriptions.length) : '';
  }

  /** A ring showing living / bills / engagement spent this month, with the total in the middle. */
  function renderDonut(t) {
    const total = t.living + t.bills + t.engagement;
    const r = 15.915;   // circumference ≈ 100, so lengths are percentages
    const svg = svgEl('svg', { viewBox: '0 0 42 42', role: 'img',
      'aria-label': 'Spent this month: ' + money(total) });
    svg.appendChild(svgEl('circle', { cx: 21, cy: 21, r, fill: 'none', stroke: 'rgba(15,26,46,0.08)', 'stroke-width': 5 }));
    let start = 0;
    const colors = { living: '#7FB6E0', bills: '#F5C542', engagement: '#3E8E63' };
    for (const part of ['living', 'bills', 'engagement']) {
      if (total <= 0 || t[part] <= 0) continue;
      const pct = (t[part] / total) * 100;
      svg.appendChild(svgEl('circle', { cx: 21, cy: 21, r, fill: 'none', stroke: colors[part], 'stroke-width': 5,
        'stroke-dasharray': pct.toFixed(2) + ' ' + (100 - pct).toFixed(2),
        'stroke-dashoffset': (25 - start).toFixed(2) }))
        .appendChild(svgEl('title', {}, part + ': ' + money(t[part])));
      start += pct;
    }
    svg.appendChild(svgEl('text', { x: 21, y: 22, 'text-anchor': 'middle', 'font-size': total >= 100000 ? 5 : 6,
      'font-weight': 700, fill: '#0F1A2E' }, shortMoney(total)));
    svg.appendChild(svgEl('text', { x: 21, y: 27, 'text-anchor': 'middle', 'font-size': 3, fill: '#4E6580' }, 'spent'));
    $('month-donut').replaceChildren(svg);
  }

  /** Stacked bars per plan month (living, bills, engagement) with a dashed line for the plan (living + bills due). */
  function renderChart() {
    const box = $('month-chart');
    const width = Math.max(280, Math.floor(box.clientWidth || 600));
    const height = width < 480 ? 220 : 260;
    const pad = { top: 22, right: 8, bottom: 30, left: width < 480 ? 44 : 56 };
    const innerW = width - pad.left - pad.right;
    const innerH = height - pad.top - pad.bottom;

    const rows = state.planMonths.map((m) => {
      const t = monthTotals(m);
      return { m, ...t, total: t.living + t.bills + t.engagement, plan: state.plan.livingMonthlyCents + billsDueIn(m) };
    });
    const maxValue = Math.max(1, ...rows.map((r) => Math.max(r.total, r.plan)));
    const niceMax = niceCeil(maxValue);
    const y = (cents) => pad.top + innerH - (cents / niceMax) * innerH;

    const svg = svgEl('svg', {
      viewBox: '0 0 ' + width + ' ' + height, width, height, role: 'img',
      'aria-label': 'Spending per month of the plan',
    });

    // Grid lines and y labels
    for (let i = 0; i <= 4; i++) {
      const v = (niceMax / 4) * i;
      const yy = y(v);
      svg.appendChild(svgEl('line', { x1: pad.left, x2: width - pad.right, y1: yy, y2: yy,
        stroke: 'rgba(15,26,46,0.08)', 'stroke-width': 1 }));
      svg.appendChild(svgEl('text', { x: pad.left - 6, y: yy + 4, 'text-anchor': 'end',
        'font-size': 11, fill: '#4E6580' }, shortMoney(v)));
    }

    const slot = innerW / rows.length;
    const barW = Math.min(46, slot * 0.6);
    const thisMonth = monthKeyOf(new Date());
    const colors = { living: '#7FB6E0', bills: '#F5C542', engagement: '#3E8E63' };

    rows.forEach((r, i) => {
      const cx = pad.left + slot * i + slot / 2;
      const x = cx - barW / 2;
      let base = 0;
      for (const part of ['living', 'bills', 'engagement']) {
        const v = r[part];
        if (v <= 0) continue;
        const top = y(base + v);
        const h = y(base) - top;
        svg.appendChild(svgEl('rect', { x, y: top, width: barW, height: Math.max(1, h), rx: 3,
          fill: colors[part], opacity: 0.9 }, undefined))
          .appendChild(svgEl('title', {}, monthLabel(r.m, true) + ' ' + part + ': ' + money(v)));
        base += v;
      }
      // Plan marker
      const py = y(r.plan);
      svg.appendChild(svgEl('line', { x1: x - 4, x2: x + barW + 4, y1: py, y2: py,
        stroke: 'rgba(15,26,46,0.6)', 'stroke-width': 1.5, 'stroke-dasharray': '4 3' }))
        .appendChild(svgEl('title', {}, monthLabel(r.m, true) + ' plan: ' + money(r.plan)));
      // Total above the bar
      if (r.total > 0) {
        svg.appendChild(svgEl('text', { x: cx, y: Math.min(y(r.total), py) - 6, 'text-anchor': 'middle',
          'font-size': 11, fill: '#0F1A2E' }, shortMoney(r.total)));
      }
      // Month label
      svg.appendChild(svgEl('text', { x: cx, y: height - 10, 'text-anchor': 'middle', 'font-size': 12,
        fill: r.m === thisMonth ? '#0F1A2E' : '#4E6580',
        'font-weight': r.m === thisMonth ? 700 : 400 }, monthLabel(r.m, false)));
    });

    box.replaceChildren(svg);
  }

  /** Round up to 1, 2, 2.5 or 5 × a power of ten, so the axis has tidy numbers. */
  function niceCeil(cents) {
    const dollars = cents / 100;
    const p = Math.pow(10, Math.floor(Math.log10(dollars)));
    for (const s of [1, 2, 2.5, 5, 10]) if (s * p >= dollars) return s * p * 100;
    return 10 * p * 100;
  }

  function shortMoney(cents) {
    const d = cents / 100;
    if (d >= 1000) return '$' + (d / 1000).toFixed(d >= 10000 ? 0 : 1).replace(/\.0$/, '') + 'k';
    return '$' + Math.round(d);
  }

  function fillMonthSelect() {
    const select = $('category-month');
    const previous = select.value;
    const months = state.planMonths.slice();
    const current = monthKeyOf(new Date());
    if (!months.includes(current)) months.push(current);
    months.sort();
    select.replaceChildren(...months.map((m) => {
      const o = el('option', '', monthLabel(m, true));
      o.value = m;
      return o;
    }));
    select.value = months.includes(previous) ? previous : current;
  }

  function renderCategories(data) {
    const list = $('category-list');
    const entries = Object.entries(data.byCategory || {})
      .filter(([, cents]) => cents > 0)
      .sort((a, b) => b[1] - a[1]);
    if (entries.length === 0) {
      list.replaceChildren(el('p', 'empty', 'Nothing spent in ' + monthLabel(data.month, true) + '.'));
      return;
    }
    const max = entries[0][1];
    const rows = entries.map(([category, cents]) => {
      const row = el('div', 'cat-row');
      const top = el('div', 'split-top');
      top.appendChild(el('span', '', CATEGORY_NAMES[category] || category));
      const pct = data.totalCents > 0 ? Math.round((cents / data.totalCents) * 100) : 0;
      top.appendChild(el('span', 'num', money(cents) + ' · ' + pct + '%'));
      row.appendChild(top);
      const fill = category === 'bills' ? 'fill-bills' : category === 'engagement' ? 'fill-engagement' : 'fill-living';
      row.appendChild(bar(cents, max, fill));
      return row;
    });
    const total = el('div', 'split-top');
    total.appendChild(el('span', 'muted', 'Living ' + money(data.livingCents) + ' · Bills ' + money(data.billsCents)
      + ' · Engagement ' + money(data.engagementCents)));
    total.appendChild(el('span', 'num', money(data.totalCents)));
    list.replaceChildren(...rows, total);
  }

  /**
   * Active subscriptions with price, cycle and next renewal (in the browser's time zone),
   * plus roughly what they cost per month: a yearly one counts as its price ÷ 12.
   */
  function renderSubscriptions() {
    const list = $('subs-list');
    const subs = state.subscriptions;
    let perMonth = 0;   // not rounded yet: rounded to whole cents once, at the end
    for (const s of subs) perMonth += s.cycle === 'yearly' ? s.amountCents / 12 : s.amountCents;
    $('subs-total').textContent = subs.length ? '≈ ' + money(Math.round(perMonth)) + ' / month' : '';
    if (subs.length === 0) {
      list.replaceChildren(el('p', 'empty', 'No active subscriptions.'));
      return;
    }
    list.replaceChildren(...subs.map((s) => {
      const row = el('div', 'sub-row');
      const left = el('div', 'sub-main');
      left.appendChild(el('span', 'sub-name', s.name));   // textContent: names can never become HTML
      left.appendChild(el('span', 'muted small',
        (s.cycle === 'yearly' ? 'Yearly' : 'Monthly') + ' · renews '
        + s.renews.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })));
      row.appendChild(left);
      row.appendChild(el('span', 'num', money(s.amountCents)));
      return row;
    }));
  }

  function renderRecent() {
    const body = $('expense-rows');
    // The search box and the All / Living / Bills / Engagement pills narrow the list.
    const query = $('search').value.trim().toLowerCase();
    const matches = state.expenses.filter((e) => {   // already newest first, deleted ones removed
      if (recentPart === 'living' && !isLiving(e.category)) return false;
      if ((recentPart === 'bills' || recentPart === 'engagement') && e.category !== recentPart) return false;
      if (!query) return true;
      const name = (CATEGORY_NAMES[e.category] || e.category).toLowerCase();
      return name.includes(query) || (e.note || '').toLowerCase().includes(query);
    });
    const recent = matches.slice(0, RECENT_LIMIT);
    if (recent.length === 0) {
      const tr = el('tr');
      const filtered = query || recentPart !== 'all';
      const td = el('td', 'empty', filtered ? 'No expenses match.' : 'No expenses yet.');
      td.colSpan = 4;
      tr.appendChild(td);
      body.replaceChildren(tr);
      return;
    }
    body.replaceChildren(...recent.map((e) => {
      const tr = el('tr');
      tr.appendChild(el('td', '', e.at.toLocaleString('en-US',
        { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })));
      const cat = el('td');
      cat.appendChild(el('span', 'chip ' + (isLiving(e.category) ? 'good' : 'plain'),
        CATEGORY_NAMES[e.category] || e.category));
      tr.appendChild(cat);
      tr.appendChild(el('td', '', e.note || ''));   // textContent: notes can never become HTML
      tr.appendChild(el('td', 'num', money(e.amountCents)));
      return tr;
    }));
  }

  // ---------- start ----------

  document.addEventListener('DOMContentLoaded', () => {
    $('login-form').addEventListener('submit', onLogin);
    $('logout-button').addEventListener('click', onLogout);
    $('logout-all-button').addEventListener('click', onLogoutAll);
    $('refresh-button').addEventListener('click', load);
    $('category-month').addEventListener('change', loadCategories);
    $('search').addEventListener('input', () => { if (state) renderRecent(); });
    $('part-tabs').addEventListener('click', (event) => {
      const button = event.target.closest('button[data-part]');
      if (!button) return;
      recentPart = button.dataset.part;
      for (const b of $('part-tabs').querySelectorAll('button')) {
        const on = b === button;
        b.classList.toggle('on', on);
        b.setAttribute('aria-pressed', String(on));
      }
      if (state) renderRecent();
    });
    // Side menu: mark the link you clicked (the page scrolls to that section by itself).
    for (const link of document.querySelectorAll('.nav a')) {
      link.addEventListener('click', () => {
        for (const other of document.querySelectorAll('.nav a')) other.classList.toggle('on', other === link);
      });
    }
    window.addEventListener('resize', () => {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(() => { if (state) renderChart(); }, 150);
    });

    if (getToken()) {
      showDashboard();
      load();
    } else {
      showLogin();
    }
  });
})();
