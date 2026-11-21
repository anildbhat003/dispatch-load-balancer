/*
 * Dispatch Control Room: a thin visual client for the Dispatch Load Balancer REST API.
 * It only calls the public endpoints (/api/dispatch/...). All the optimisation happens on the server.
 */
(() => {
    "use strict";

    const API = "/api/dispatch";
    const ROUTE_COLOURS = ["#0e4d8f", "#9b2c8c", "#1f7a4d", "#c2410c", "#5b4b8a", "#0f766e", "#a16207", "#be123c"];
    const PRIORITY_COLOUR = { HIGH: "var(--high)", MEDIUM: "var(--medium)", LOW: "var(--low)" };

    const DELHI_SAMPLE = {orders: [{"orderId":"ORD001","latitude":28.6139,"longitude":77.209,"address":"Connaught Place, Delhi, India","packageWeight":15,"priority":"HIGH"},{"orderId":"ORD002","latitude":28.6139,"longitude":77.209,"address":"Connaught Place, Delhi, India","packageWeight":10,"priority":"MEDIUM"},{"orderId":"ORD003","latitude":28.7041,"longitude":77.1025,"address":"Karol Bagh, Delhi, India","packageWeight":20,"priority":"LOW"},{"orderId":"ORD004","latitude":28.5355,"longitude":77.391,"address":"Sector 18, Noida, Uttar Pradesh, India","packageWeight":25,"priority":"HIGH"},{"orderId":"ORD005","latitude":28.4595,"longitude":77.0266,"address":"Cyber Hub, Gurgaon, Haryana, India","packageWeight":30,"priority":"MEDIUM"},{"orderId":"ORD006","latitude":28.6139,"longitude":77.209,"address":"Connaught Place, Delhi, India","packageWeight":40,"priority":"LOW"},{"orderId":"ORD007","latitude":28.7041,"longitude":77.1025,"address":"Karol Bagh, Delhi, India","packageWeight":20,"priority":"HIGH"},{"orderId":"ORD008","latitude":28.5355,"longitude":77.391,"address":"Sector 18, Noida, Uttar Pradesh, India","packageWeight":25,"priority":"HIGH"},{"orderId":"ORD009","latitude":28.4595,"longitude":77.0266,"address":"Cyber Hub, Gurgaon, Haryana, India","packageWeight":15,"priority":"MEDIUM"},{"orderId":"ORD010","latitude":28.6139,"longitude":77.209,"address":"Connaught Place, Delhi, India","packageWeight":30,"priority":"LOW"},{"orderId":"ORD011","latitude":28.7041,"longitude":77.1025,"address":"Karol Bagh, Delhi, India","packageWeight":20,"priority":"HIGH"},{"orderId":"ORD012","latitude":28.5355,"longitude":77.391,"address":"Sector 18, Noida, Uttar Pradesh, India","packageWeight":10,"priority":"MEDIUM"},{"orderId":"ORD013","latitude":28.4595,"longitude":77.0266,"address":"Cyber Hub, Gurgaon, Haryana, India","packageWeight":25,"priority":"HIGH"},{"orderId":"ORD014","latitude":28.6139,"longitude":77.209,"address":"Connaught Place, Delhi, India","packageWeight":15,"priority":"LOW"},{"orderId":"ORD015","latitude":28.7041,"longitude":77.1025,"address":"Karol Bagh, Delhi, India","packageWeight":20,"priority":"HIGH"},{"orderId":"ORD016","latitude":28.5355,"longitude":77.391,"address":"Sector 18, Noida, Uttar Pradesh, India","packageWeight":30,"priority":"LOW"},{"orderId":"ORD017","latitude":28.4595,"longitude":77.0266,"address":"Cyber Hub, Gurgaon, Haryana, India","packageWeight":15,"priority":"MEDIUM"},{"orderId":"ORD018","latitude":28.6139,"longitude":77.209,"address":"Connaught Place, Delhi, India","packageWeight":10,"priority":"HIGH"},{"orderId":"ORD019","latitude":28.7041,"longitude":77.1025,"address":"Karol Bagh, Delhi, India","packageWeight":20,"priority":"LOW"},{"orderId":"ORD020","latitude":28.5355,"longitude":77.391,"address":"Sector 18, Noida, Uttar Pradesh, India","packageWeight":30,"priority":"HIGH"},{"orderId":"ORD021","latitude":28.6139,"longitude":77.209,"address":"Connaught Place, Delhi, India","packageWeight":25,"priority":"MEDIUM"},{"orderId":"ORD022","latitude":28.7041,"longitude":77.1025,"address":"Karol Bagh, Delhi, India","packageWeight":20,"priority":"LOW"},{"orderId":"ORD023","latitude":28.5355,"longitude":77.391,"address":"Sector 18, Noida, Uttar Pradesh, India","packageWeight":30,"priority":"HIGH"},{"orderId":"ORD024","latitude":28.4595,"longitude":77.0266,"address":"Cyber Hub, Gurgaon, Haryana, India","packageWeight":15,"priority":"LOW"},{"orderId":"ORD025","latitude":28.6139,"longitude":77.209,"address":"Connaught Place, Delhi, India","packageWeight":20,"priority":"MEDIUM"},{"orderId":"ORD026","latitude":28.7041,"longitude":77.1025,"address":"Karol Bagh, Delhi, India","packageWeight":25,"priority":"HIGH"},{"orderId":"ORD027","latitude":28.5355,"longitude":77.391,"address":"Sector 18, Noida, Uttar Pradesh, India","packageWeight":20,"priority":"MEDIUM"},{"orderId":"ORD028","latitude":28.4595,"longitude":77.0266,"address":"Cyber Hub, Gurgaon, Haryana, India","packageWeight":15,"priority":"LOW"},{"orderId":"ORD029","latitude":28.6139,"longitude":77.209,"address":"Connaught Place, Delhi, India","packageWeight":30,"priority":"HIGH"},{"orderId":"ORD030","latitude":28.7041,"longitude":77.1025,"address":"Karol Bagh, Delhi, India","packageWeight":20,"priority":"LOW"}],
      vehicles: [{"vehicleId":"VEH001","capacity":100,"currentLatitude":28.7041,"currentLongitude":77.1025,"currentAddress":"Karol Bagh, Delhi, India"},{"vehicleId":"VEH002","capacity":80,"currentLatitude":28.5355,"currentLongitude":77.391,"currentAddress":"Sector 18, Noida, Uttar Pradesh, India"},{"vehicleId":"VEH003","capacity":120,"currentLatitude":28.4595,"currentLongitude":77.0266,"currentAddress":"Cyber Hub, Gurgaon, Haryana, India"},{"vehicleId":"VEH004","capacity":90,"currentLatitude":28.6139,"currentLongitude":77.209,"currentAddress":"Connaught Place, Delhi, India"},{"vehicleId":"VEH005","capacity":110,"currentLatitude":28.7041,"currentLongitude":77.1025,"currentAddress":"Karol Bagh, Delhi, India"}]};

    /** Real neighbourhoods per city, used to build believable random scenarios. */
    const CITIES = {
        blr: { name: "Bengaluru", areas: [
            ["Koramangala", 12.9352, 77.6245], ["Indiranagar", 12.9784, 77.6408], ["Whitefield", 12.9698, 77.7500],
            ["Jayanagar", 12.9250, 77.5938], ["Hebbal", 13.0358, 77.5970], ["Electronic City", 12.8452, 77.6602],
            ["Malleshwaram", 13.0031, 77.5643], ["Yelahanka", 13.1007, 77.5963], ["HSR Layout", 12.9116, 77.6474],
            ["Rajajinagar", 12.9910, 77.5550]] },
        mum: { name: "Mumbai", areas: [
            ["Andheri", 19.1136, 72.8697], ["Bandra", 19.0596, 72.8295], ["Powai", 19.1176, 72.9060],
            ["Dadar", 19.0178, 72.8478], ["Colaba", 18.9067, 72.8147], ["Thane", 19.2183, 72.9781],
            ["Borivali", 19.2307, 72.8567], ["Chembur", 19.0522, 72.9005], ["Lower Parel", 18.9953, 72.8302],
            ["Vashi", 19.0771, 72.9986]] },
        del: { name: "Delhi NCR", areas: [
            ["Connaught Place", 28.6315, 77.2167], ["Karol Bagh", 28.6519, 77.1909], ["Saket", 28.5245, 77.2066],
            ["Dwarka", 28.5921, 77.0460], ["Sector 18, Noida", 28.5708, 77.3260], ["Cyber Hub, Gurgaon", 28.4950, 77.0895],
            ["Rohini", 28.7495, 77.0565], ["Lajpat Nagar", 28.5677, 77.2433], ["Chandni Chowk", 28.6506, 77.2303],
            ["Vasant Kunj", 28.5200, 77.1590]] },
        che: { name: "Chennai", areas: [
            ["T Nagar", 13.0418, 80.2341], ["Anna Nagar", 13.0850, 80.2101], ["Adyar", 13.0012, 80.2565],
            ["Velachery", 12.9815, 80.2180], ["Guindy", 13.0067, 80.2206], ["Tambaram", 12.9249, 80.1000],
            ["Mylapore", 13.0368, 80.2676], ["Porur", 13.0382, 80.1565], ["Egmore", 13.0732, 80.2609],
            ["Sholinganallur", 12.9010, 80.2279]] }
    };

    const state = { vehicles: [], orders: [], plans: { smart: null, naive: null }, comparison: null, view: "smart",
        get plan() { return this.plans[this.view]; }, selected: null, layers: [], fitNext: true };
    const $ = (sel) => document.querySelector(sel);

    // ---------------------------------------------------------------- API
    async function call(method, path, body) {
        const res = await fetch(API + path, {
            method,
            headers: body ? { "Content-Type": "application/json" } : {},
            body: body ? JSON.stringify(body) : undefined
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok) {
            const err = new Error(data.message || `Request failed (${res.status})`);
            err.details = data.errors || [];
            throw err;
        }
        return data;
    }

    async function refresh() {
        const [vehicles, orders, smart, naive, comparison] = await Promise.all([
            call("GET", "/vehicles"), call("GET", "/orders"), call("GET", "/plan"),
            call("GET", "/plan?strategy=naive"), call("GET", "/plan/comparison")]);
        Object.assign(state, { vehicles, orders, comparison });
        state.plans = { smart, naive };
        if (state.selected && !vehicles.some((v) => v.vehicleId === state.selected)) state.selected = null;
        render();
    }

    async function run(button, task, successMessage) {
        if (button) button.disabled = true;
        try {
            await task();
            await refresh();
            if (successMessage) toast(successMessage);
        } catch (e) {
            toast(e.message, e.details, true);
        } finally {
            if (button) button.disabled = false;
        }
    }

    async function replaceData(vehicles, orders) {
        await call("DELETE", "/orders");
        await call("DELETE", "/vehicles");
        await call("POST", "/vehicles", { vehicles });
        await call("POST", "/orders", { orders });
        state.selected = null;
        state.fitNext = true;
    }

    // ---------------------------------------------------------------- scenarios
    const rand = (min, max) => min + Math.random() * (max - min);
    const pick = (arr) => arr[Math.floor(Math.random() * arr.length)];
    const round = (n, dp) => Number(n.toFixed(dp));
    const pad = (n) => String(n).padStart(3, "0");

    function randomScenario(cityKey) {
        const city = CITIES[cityKey];
        const jitter = () => rand(-0.008, 0.008);
        const vehicles = Array.from({ length: 5 }, (_, i) => {
            const [area, lat, lon] = pick(city.areas);
            return { vehicleId: `VEH${pad(i + 1)}`, capacity: pick([80, 100, 120, 150]),
                currentLatitude: round(lat + jitter(), 4), currentLongitude: round(lon + jitter(), 4),
                currentAddress: `${area}, ${city.name}` };
        });
        const orders = Array.from({ length: 26 }, (_, i) => {
            const [area, lat, lon] = pick(city.areas);
            return { orderId: `ORD${pad(i + 1)}`, latitude: round(lat + jitter(), 4), longitude: round(lon + jitter(), 4),
                address: `${area}, ${city.name}`, packageWeight: Math.round(rand(5, 40)),
                priority: pick(["HIGH", "HIGH", "MEDIUM", "MEDIUM", "LOW", "LOW", "LOW"]) };
        });
        return { vehicles, orders, name: city.name };
    }

    function nextId(prefix, existing) {
        let n = existing.length + 1;
        while (existing.includes(`${prefix}${pad(n)}`)) n++;
        return `${prefix}${pad(n)}`;
    }

    // ---------------------------------------------------------------- map
    /** Keeps routes clear of the floating "add" box (top right) and the legend (bottom). */
    const FIT = { paddingTopLeft: [40, 150], paddingBottomRight: [40, 60], maxZoom: 14 };
    let map = null;
    if (window.L) {
        map = L.map("map", { zoomControl: true }).setView([22.5, 79], 5);
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 18,
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);
        map.on("click", onMapClick);
    } else {
        $("#map").innerHTML = '<p style="padding:24px">The map library could not load. The fleet panel still works.</p>';
    }

    /** Markers at the same coordinate are fanned out in a small ring so none hide behind another. */
    function fanOut(items) {
        const groups = new Map();
        items.forEach((it) => {
            const key = `${it.lat.toFixed(5)},${it.lon.toFixed(5)}`;
            if (!groups.has(key)) groups.set(key, []);
            groups.get(key).push(it);
        });
        groups.forEach((group) => {
            const radius = group.length > 1 ? 12 + group.length * 2 : 0;
            group.forEach((it, i) => {
                const angle = (2 * Math.PI * i) / group.length - Math.PI / 2;
                it.dx = Math.round(Math.cos(angle) * radius);
                it.dy = Math.round(Math.sin(angle) * radius);
            });
        });
        return items;
    }

    function icon(html, w, h, dx = 0, dy = 0, anchorY = h / 2) {
        return L.divIcon({ html, className: "", iconSize: [w, h], iconAnchor: [w / 2 - dx, anchorY - dy], popupAnchor: [dx, dy - anchorY] });
    }

    function renderMap(colours, capacities) {
        if (!map) return;
        state.layers.forEach((l) => l.layer.remove());
        state.layers = [];
        const bounds = [];
        const plan = state.plan || { dispatchPlan: [], unassignedOrders: [] };

        const markers = [];
        plan.dispatchPlan.forEach((vp) => {
            const v = state.vehicles.find((x) => x.vehicleId === vp.vehicleId);
            if (!v) return;
            const colour = colours[vp.vehicleId];
            const path = [[v.currentLatitude, v.currentLongitude], ...vp.assignedOrders.map((o) => [o.latitude, o.longitude])];
            path.forEach((p) => bounds.push(p));
            if (path.length > 1) {
                const line = L.polyline(path, { color: colour, weight: 4, opacity: 0.85, lineJoin: "round" }).addTo(map);
                state.layers.push({ layer: line, vehicle: vp.vehicleId, kind: "line" });
            }
            markers.push({ kind: "vehicle", lat: v.currentLatitude, lon: v.currentLongitude, v, vp, colour });
            vp.assignedOrders.forEach((o, i) =>
                markers.push({ kind: "stop", lat: o.latitude, lon: o.longitude, o, seq: i + 1, vp, colour }));
        });
        plan.unassignedOrders.forEach((u) => {
            const o = state.orders.find((x) => x.orderId === u.orderId);
            if (o) {
                markers.push({ kind: "left", lat: o.latitude, lon: o.longitude, o, u });
                bounds.push([o.latitude, o.longitude]);
            }
        });

        fanOut(markers).forEach((m) => {
            let marker;
            if (m.kind === "vehicle") {
                const label = m.v.vehicleId.replace(/^VEH0*/, "V");
                marker = L.marker([m.lat, m.lon], {
                    icon: icon(`<div class="mk-shield" style="--route:${m.colour}">${esc(label)}</div>`, 34, 38, m.dx, m.dy, 38),
                    zIndexOffset: 1000, keyboard: true, title: m.v.vehicleId
                }).bindPopup(`<strong>${esc(m.v.vehicleId)}</strong><br>${esc(m.v.currentAddress)}<br>`
                    + `Load ${m.vp.totalLoad} / ${capacities[m.v.vehicleId]} kg · ${esc(m.vp.totalDistance)}`);
                marker.on("click", () => select(m.v.vehicleId, false));
            } else if (m.kind === "stop") {
                marker = L.marker([m.lat, m.lon], {
                    icon: icon(`<div class="mk-stop" style="--p:${PRIORITY_COLOUR[m.o.priority]};--route:${m.colour}">${m.seq}</div>`, 24, 24, m.dx, m.dy),
                    title: m.o.orderId
                }).bindPopup(`<strong>${esc(m.o.orderId)}</strong><br>${esc(m.o.address)}<br>`
                    + `${m.o.packageWeight} kg · ${m.o.priority}<br>Stop ${m.seq} for ${esc(m.vp.vehicleId)}`);
            } else {
                marker = L.marker([m.lat, m.lon], {
                    icon: icon('<div class="mk-left"></div>', 18, 18, m.dx, m.dy), title: m.o.orderId
                }).bindPopup(`<strong>${esc(m.o.orderId)}</strong><br>${esc(m.o.address)}<br>`
                    + `${m.o.packageWeight} kg · ${m.o.priority}<br>Not dispatched: ${esc(m.u.reason)}`);
            }
            marker.addTo(map);
            state.layers.push({ layer: marker, vehicle: m.vp ? m.vp.vehicleId : null, kind: "marker" });
        });

        if (state.fitNext && bounds.length) {
            map.fitBounds(bounds, FIT);
            state.fitNext = false;
        }
        applySelection();
    }

    function applySelection() {
        state.layers.forEach(({ layer, vehicle, kind }) => {
            const faded = state.selected && vehicle !== state.selected;
            if (kind === "line") {
                layer.setStyle({ opacity: faded ? 0.12 : 0.85, weight: state.selected === vehicle ? 6 : 4 });
                if (!faded && state.selected) layer.bringToFront();
            } else {
                const el = layer.getElement();
                if (el) el.classList.toggle("is-faded", Boolean(faded));
            }
        });
        document.querySelectorAll(".truck").forEach((b) =>
            b.setAttribute("aria-pressed", String(b.dataset.vehicle === state.selected)));
    }

    function select(vehicleId, zoom = true) {
        state.selected = state.selected === vehicleId ? null : vehicleId;
        applySelection();
        if (zoom && map && state.selected) {
            const pts = state.layers.filter((l) => l.vehicle === state.selected && l.kind === "line").flatMap((l) => l.layer.getLatLngs());
            if (pts.length) map.fitBounds(pts, { ...FIT, maxZoom: 15 });
        }
    }

    function onMapClick(e) {
        const mode = document.querySelector('input[name="mode"]:checked').value;
        const lat = round(e.latlng.lat, 4);
        const lon = round(e.latlng.lng, 4);
        const where = `Dropped pin ${lat}, ${lon}`;
        if (mode === "order") {
            const id = nextId("PIN", state.orders.map((o) => o.orderId));
            const order = { orderId: id, latitude: lat, longitude: lon, address: where,
                packageWeight: Number($("#drop-weight").value), priority: $("#drop-priority").value };
            run(null, () => call("POST", "/orders", { orders: [order] }), `${id} added. The plan has been recalculated.`);
        } else {
            const id = nextId("VAN", state.vehicles.map((v) => v.vehicleId));
            const vehicle = { vehicleId: id, capacity: Number($("#drop-capacity").value),
                currentLatitude: lat, currentLongitude: lon, currentAddress: where };
            run(null, () => call("POST", "/vehicles", { vehicles: [vehicle] }), `${id} added to the fleet. The plan has been recalculated.`);
        }
    }

    // ---------------------------------------------------------------- panel
    function render() {
        const plan = state.plan || { dispatchPlan: [], unassignedOrders: [] };
        const colours = {};
        const capacities = {};
        [...state.vehicles].sort((a, b) => a.vehicleId.localeCompare(b.vehicleId)).forEach((v, i) => {
            colours[v.vehicleId] = ROUTE_COLOURS[i % ROUTE_COLOURS.length];
            capacities[v.vehicleId] = v.capacity;
        });

        const assigned = plan.dispatchPlan.reduce((n, vp) => n + vp.assignedOrders.length, 0);
        const distance = plan.dispatchPlan.reduce((n, vp) => n + parseFloat(vp.totalDistance), 0);
        const load = plan.dispatchPlan.reduce((n, vp) => n + vp.totalLoad, 0);
        const capacity = state.vehicles.reduce((n, v) => n + v.capacity, 0);
        const leftKg = plan.unassignedOrders.reduce((n, u) => n + u.packageWeight, 0);

        $("#kpi-assigned").textContent = `${assigned}/${state.orders.length}`;
        $("#kpi-distance").textContent = `${distance.toFixed(1)} km`;
        $("#kpi-util").textContent = capacity ? `${Math.round((load / capacity) * 100)}%` : "0%";
        $("#kpi-left").textContent = `${trim(leftKg)} kg`;
        $("#kpi-left-wrap").classList.toggle("is-active", leftKg > 0);

        const fleet = $("#fleet");
        fleet.replaceChildren(...plan.dispatchPlan.map((vp) => truckCard(vp, colours[vp.vehicleId], capacities[vp.vehicleId])));
        $("#fleet-hint").textContent = plan.dispatchPlan.length
            ? "Click a truck to follow its route. Numbers show the delivery order."
            : "No trucks yet. Add some with a scenario, or switch the map tool to “a truck”.";

        const left = $("#leftover");
        left.replaceChildren(...plan.unassignedOrders.map((u) => {
            const li = document.createElement("li");
            li.innerHTML = `<strong>${esc(u.orderId)}</strong> · ${trim(u.packageWeight)} kg · ${u.priority}<br>${esc(u.reason)}`;
            return li;
        }));
        $("#left-section").hidden = plan.unassignedOrders.length === 0;
        $("#empty").hidden = state.orders.length > 0 || state.vehicles.length > 0;

        renderComparison();
        renderMap(colours, capacities);
    }

    function renderComparison() {
        const c = state.comparison;
        const section = $("#versus");
        section.hidden = !c || c.smart.ordersTotal === 0 || state.vehicles.length === 0;
        if (section.hidden) return;
        const s = c.smart.kmPerDispatchedOrder;
        const n = c.naive.kmPerDispatchedOrder;
        const max = Math.max(s, n) || 1;
        $("#versus-headline").textContent = c.distanceSavedPercent > 0
            ? `${c.distanceSavedPercent.toFixed(1)}% less driving per delivery`
            : c.distanceSavedPercent < 0
                ? `${(-c.distanceSavedPercent).toFixed(1)}% more driving per delivery, but more urgent orders delivered`
                : "Same driving per delivery";
        $("#val-smart").textContent = `${s.toFixed(2)} km / delivery`;
        $("#val-naive").textContent = `${n.toFixed(2)} km / delivery`;
        requestAnimationFrame(() => {
            $("#bar-smart").style.width = `${(s / max) * 100}%`;
            $("#bar-naive").style.width = `${(n / max) * 100}%`;
        });
        $("#versus-high").textContent = `HIGH-priority orders dispatched: smart ${c.smart.highPriorityDispatched}/${c.smart.highPriorityTotal}, `
            + `naive ${c.naive.highPriorityDispatched}/${c.naive.highPriorityTotal}.`;
    }

    function truckCard(vp, colour, capacity) {
        const v = state.vehicles.find((x) => x.vehicleId === vp.vehicleId) || {};
        const pct = capacity ? Math.min(100, (vp.totalLoad / capacity) * 100) : 0;
        const li = document.createElement("li");
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "truck" + (vp.assignedOrders.length ? "" : " truck--idle");
        btn.dataset.vehicle = vp.vehicleId;
        btn.style.setProperty("--route", colour);
        btn.setAttribute("aria-pressed", "false");
        btn.innerHTML = `
            <div class="truck__top"><span class="truck__id">${esc(vp.vehicleId)}</span><span class="truck__dist">${esc(vp.totalDistance)}</span></div>
            <p class="truck__from">From ${esc(v.currentAddress || "")}</p>
            <div class="load" role="img" aria-label="${trim(vp.totalLoad)} of ${trim(capacity)} kg loaded"><div class="load__fill"></div></div>
            <div class="load__text"><span>${trim(vp.totalLoad)} / ${trim(capacity)} kg</span><span>${Math.round(pct)}% full</span></div>
            <ol class="stops">${vp.assignedOrders.map((o, i) =>
                `<li class="stop-chip" style="--p:${PRIORITY_COLOUR[o.priority]}" title="${esc(o.address)}">${i + 1}. ${esc(o.orderId)}</li>`).join("")}</ol>`;
        btn.addEventListener("click", () => select(vp.vehicleId));
        requestAnimationFrame(() => { btn.querySelector(".load__fill").style.width = `${pct}%`; });
        li.append(btn);
        return li;
    }

    // ---------------------------------------------------------------- helpers
    function esc(s) {
        return String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
    }

    function trim(n) {
        return Number.isInteger(n) ? String(n) : Number(n).toFixed(1);
    }

    let toastTimer;
    function toast(message, details = [], isError = false) {
        const el = $("#toast");
        el.className = "toast" + (isError ? " toast--error" : "");
        el.innerHTML = esc(message) + (details.length ? `<ul>${details.map((d) => `<li>${esc(d)}</li>`).join("")}</ul>` : "");
        el.hidden = false;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => { el.hidden = true; }, isError ? 7000 : 3500);
    }

    // ---------------------------------------------------------------- wiring
    document.querySelectorAll('input[name="mode"]').forEach((r) => r.addEventListener("change", () => {
        const mode = document.querySelector('input[name="mode"]:checked').value;
        document.querySelectorAll(".dropper__fields").forEach((f) => { f.hidden = f.dataset.for !== mode; });
    }));
    $("#dropper").addEventListener("submit", (e) => e.preventDefault());

    document.querySelectorAll('input[name="view"]').forEach((r) => r.addEventListener("change", (e) => {
        state.view = e.target.value;
        state.selected = null;
        document.body.classList.toggle("view-naive", state.view === "naive");
        render();
    }));

    document.querySelector('[data-action="sample"]').addEventListener("click", (e) =>
        run(e.currentTarget, () => replaceData(DELHI_SAMPLE.vehicles, DELHI_SAMPLE.orders), "Loaded the assignment's Delhi sample: 30 orders, 5 trucks."));

    document.querySelector('[data-action="random"]').addEventListener("click", (e) => {
        const s = randomScenario($("#city").value);
        run(e.currentTarget, () => replaceData(s.vehicles, s.orders), `New ${s.name} scenario: ${s.orders.length} orders, ${s.vehicles.length} trucks.`);
    });

    document.querySelector('[data-action="reset"]').addEventListener("click", (e) =>
        run(e.currentTarget, async () => {
            await call("DELETE", "/orders");
            await call("DELETE", "/vehicles");
            state.selected = null;
        }, "Cleared all orders and trucks."));

    refresh().catch((e) => toast(e.message, e.details, true));
})();
