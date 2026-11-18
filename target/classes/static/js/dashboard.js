/**
 * IntelliRoute Explorer - Dashboard Controller
 * India-Wide Intelligent Multi-Factor Route Optimization
 */

// State
let popularLocations = [];
let sourceLocation = null;
let destLocation = null;
let map = null;
let routeLayers = [];
let segmentLayers = [];
let sourceMarker = null;
let destMarker = null;
let currentRoutes = [];
let activeRouteIndex = 0;

// Color palette for candidate routes
const ROUTE_STYLES = [
  { color: '#10b981', weight: 6, opacity: 0.95, dashArray: null },     // Recommended: Vibrant Emerald
  { color: '#6366f1', weight: 5, opacity: 0.85, dashArray: '8, 6' },  // Alternative 1: Indigo
  { color: '#f59e0b', weight: 5, opacity: 0.85, dashArray: '6, 6' }   // Alternative 2: Amber
];

// Traffic segment colors based on congestion ratio
const TRAFFIC_COLORS = {
  FREE_FLOW: '#10b981',    // Green
  MODERATE: '#eab308',     // Yellow
  HEAVY: '#f97316',        // Orange
  SEVERE: '#ef4444',       // Red
  UNAVAILABLE: '#94a3b8'   // Slate
};

document.addEventListener('DOMContentLoaded', async () => {
  // Ensure authentication
  Auth.requireAuth();

  // Populate user header
  const user = Auth.getUser();
  if (user && user.name) {
    document.getElementById('userNameDisplay').innerText = user.name;
    document.getElementById('userAvatarLetter').innerText = user.name.charAt(0).toUpperCase();
  }

  // Logout button
  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => Auth.logout());
  }

  // Initialize Leaflet Map centered on India
  initMap();

  // Load pan-India hubs for autocomplete
  await loadPopularLocations();

  // Setup Autocomplete inputs
  setupAutocomplete('sourceInput', 'sourceDropdown', (loc) => {
    sourceLocation = loc;
  });

  setupAutocomplete('destInput', 'destDropdown', (loc) => {
    destLocation = loc;
  });

  // Optimize form submit
  const routeForm = document.getElementById('routeForm');
  if (routeForm) {
    routeForm.addEventListener('submit', handleOptimizeRoute);
  }

  // Quick preset chips
  setupQuickPresets();

  // TomTom API Key Modal Setup
  setupApiKeyModal();
});

function setupApiKeyModal() {
  const modal = document.getElementById('apiKeyModal');
  const openBtn = document.getElementById('trafficKeyBtn');
  const closeBtn = document.getElementById('closeModalBtn');
  const saveBtn = document.getElementById('saveKeyBtn');
  const input = document.getElementById('modalApiKeyInput');
  const modalAlert = document.getElementById('modalAlert');

  if (!modal || !openBtn) return;

  openBtn.addEventListener('click', async () => {
    modalAlert.style.display = 'none';
    modal.style.display = 'flex';
    try {
      const res = await fetch('/api/routes/status');
      if (res.ok) {
        const status = await res.json();
        if (status.trafficKeyConfigured) {
          modalAlert.className = 'alert alert-success';
          modalAlert.innerText = 'Current Status: TomTom Live API Key is configured and active.';
          modalAlert.style.display = 'block';
        } else {
          modalAlert.className = 'alert alert-info';
          modalAlert.innerText = 'Current Status: Corridor Segment Traffic Engine is active. Enter a TomTom key below to switch to live queries.';
          modalAlert.style.display = 'block';
        }
      }
    } catch (e) {}
  });

  closeBtn.addEventListener('click', () => {
    modal.style.display = 'none';
  });

  saveBtn.addEventListener('click', async () => {
    const key = input.value.trim();
    try {
      const res = await fetch('/api/routes/traffic-key', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ apiKey: key })
      });
      const data = await res.json();
      modalAlert.className = 'alert alert-success';
      modalAlert.innerText = data.message;
      modalAlert.style.display = 'block';
      setTimeout(() => {
        modal.style.display = 'none';
      }, 1200);
    } catch (e) {
      modalAlert.className = 'alert alert-danger';
      modalAlert.innerText = 'Failed to update API key.';
      modalAlert.style.display = 'block';
    }
  });
}

/**
 * Initialize Leaflet Map centered on India
 */
function initMap() {
  // Center of India: Lat 22.5, Lon 82.0, Zoom 5
  map = L.map('map', {
    zoomControl: false
  }).setView([22.5, 82.0], 5);

  // OpenStreetMap Tile Layer
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
  }).addTo(map);

  // Custom Zoom Control at top-right
  L.control.zoom({ position: 'topright' }).addTo(map);
}

/**
 * Fetch initial popular Indian hubs from backend API
 */
async function loadPopularLocations() {
  try {
    const res = await fetch('/api/routes/locations');
    if (res.ok) {
      popularLocations = await res.json();
    }
  } catch (err) {
    console.error('Failed to load locations:', err);
  }
}

/**
 * Setup searchable dropdown autocomplete
 */
function setupAutocomplete(inputId, dropdownId, onSelect) {
  const input = document.getElementById(inputId);
  const dropdown = document.getElementById(dropdownId);

  input.addEventListener('input', async () => {
    const query = input.value.trim().toLowerCase();
    dropdown.innerHTML = '';

    if (!query) {
      dropdown.classList.remove('open');
      return;
    }

    // Local matches first
    let matches = popularLocations.filter(loc =>
      loc.name.toLowerCase().includes(query) ||
      (loc.district && loc.district.toLowerCase().includes(query)) ||
      (loc.state && loc.state.toLowerCase().includes(query))
    ).slice(0, 8);

    if (matches.length === 0) {
      // Dynamic search via API if query is at least 3 chars
      if (query.length >= 3) {
        try {
          const res = await fetch(`/api/routes/locations?q=${encodeURIComponent(query)}`);
          if (res.ok) {
            matches = await res.json();
          }
        } catch (e) {
          // Ignore
        }
      }
    }

    if (matches.length === 0) {
      const emptyItem = document.createElement('div');
      emptyItem.className = 'autocomplete-item';
      emptyItem.innerHTML = `<span class="item-sub">Press enter to geocode "${escapeHtml(query)}" across India</span>`;
      dropdown.appendChild(emptyItem);
      dropdown.classList.add('open');
      return;
    }

    matches.slice(0, 8).forEach(loc => {
      const item = document.createElement('div');
      item.className = 'autocomplete-item';
      const subLabel = [loc.district, loc.state].filter(Boolean).join(', ') || 'India';
      item.innerHTML = `
        <div>
          <div class="item-main">${escapeHtml(loc.name)}</div>
          <div class="item-sub">${escapeHtml(subLabel)}, India</div>
        </div>
        <span class="item-tag">${escapeHtml(loc.state || 'IN')}</span>
      `;
      item.addEventListener('click', () => {
        input.value = loc.name;
        onSelect(loc);
        dropdown.classList.remove('open');
      });
      dropdown.appendChild(item);
    });

    dropdown.classList.add('open');
  });

  // Close dropdown on outside click
  document.addEventListener('click', (e) => {
    if (!input.contains(e.target) && !dropdown.contains(e.target)) {
      dropdown.classList.remove('open');
    }
  });

  // Focus event: show initial prominent hubs
  input.addEventListener('focus', () => {
    if (!input.value.trim() && popularLocations.length > 0) {
      dropdown.innerHTML = '';
      popularLocations.slice(0, 7).forEach(loc => {
        const item = document.createElement('div');
        item.className = 'autocomplete-item';
        const subLabel = [loc.district, loc.state].filter(Boolean).join(', ') || 'India';
        item.innerHTML = `
          <div>
            <div class="item-main">${escapeHtml(loc.name)}</div>
            <div class="item-sub">${escapeHtml(subLabel)}, India</div>
          </div>
          <span class="item-tag">${escapeHtml(loc.state || 'IN')}</span>
        `;
        item.addEventListener('click', () => {
          input.value = loc.name;
          onSelect(loc);
          dropdown.classList.remove('open');
        });
        dropdown.appendChild(item);
      });
      dropdown.classList.add('open');
    }
  });
}

/**
 * Quick Preset Buttons for India-wide Benchmarks
 */
function setupQuickPresets() {
  const presets = [
    { label: 'Delhi ➔ Mumbai', src: 'Delhi', dst: 'Mumbai', veh: 'CAR', pref: 'FASTEST' },
    { label: 'Bengaluru ➔ Chennai', src: 'Bengaluru', dst: 'Chennai', veh: 'CAR', pref: 'RECOMMENDED' },
    { label: 'Vijayawada ➔ Hyderabad', src: 'Vijayawada', dst: 'Hyderabad', veh: 'CAR', pref: 'RECOMMENDED' },
    { label: 'Kolkata ➔ Visakhapatnam', src: 'Kolkata', dst: 'Visakhapatnam', veh: 'CAR', pref: 'SHORTEST' },
    { label: 'Jaipur ➔ Ahmedabad', src: 'Jaipur', dst: 'Ahmedabad', veh: 'CAR', pref: 'RECOMMENDED' },
    { label: 'VIT-AP ➔ Tirupati', src: 'VIT-AP University', dst: 'Tirupati', veh: 'CAR', pref: 'RECOMMENDED' },
    { label: 'Vijayawada ➔ Guntur (Cycle)', src: 'Vijayawada', dst: 'Guntur', veh: 'BICYCLE', pref: 'RECOMMENDED' }
  ];

  const container = document.getElementById('presetsContainer');
  if (!container) return;

  presets.forEach(p => {
    const chip = document.createElement('button');
    chip.type = 'button';
    chip.className = 'btn btn-outline';
    chip.style.cssText = 'padding: 4px 10px; font-size: 0.75rem; text-transform: none; border-radius: 9999px;';
    chip.innerText = p.label;
    chip.addEventListener('click', () => {
      document.getElementById('sourceInput').value = p.src;
      document.getElementById('destInput').value = p.dst;
      document.getElementById('vehicleSelect').value = p.veh;
      document.getElementById('preferenceSelect').value = p.pref;

      sourceLocation = popularLocations.find(l => l.name.toLowerCase() === p.src.toLowerCase()) || null;
      destLocation = popularLocations.find(l => l.name.toLowerCase() === p.dst.toLowerCase()) || null;
    });
    container.appendChild(chip);
  });
}

/**
 * Handle Find Best Routes Submission
 */
async function handleOptimizeRoute(e) {
  e.preventDefault();

  const sourceName = document.getElementById('sourceInput').value.trim();
  const destName = document.getElementById('destInput').value.trim();
  const vehicle = document.getElementById('vehicleSelect').value;
  const preference = document.getElementById('preferenceSelect').value;

  const errorAlert = document.getElementById('errorAlert');
  const loadingBox = document.getElementById('loadingBox');
  const resultsContainer = document.getElementById('resultsContainer');
  const submitBtn = document.getElementById('optimizeBtn');

  errorAlert.style.display = 'none';

  if (!sourceName || !destName) {
    showError('Please enter both source and destination locations in India.');
    return;
  }

  submitBtn.disabled = true;
  loadingBox.classList.add('active');
  resultsContainer.innerHTML = '';

  const stepItems = [
    document.getElementById('step-1'), // Validating Indian locations...
    document.getElementById('step-2'), // Calculating genuine OSRM routes...
    document.getElementById('step-3'), // Analyzing TomTom segment traffic...
    document.getElementById('step-4'), // Fetching time-aware Open-Meteo forecasts...
    document.getElementById('step-5'), // Multi-factor scoring & ranking...
    document.getElementById('step-6')  // Complete
  ];

  resetSteps(stepItems);

  // Step 1: Validating locations
  setStepState(stepItems, 0, 'running');
  await sleep(200);
  setStepState(stepItems, 0, 'completed');

  // Step 2: Routing
  setStepState(stepItems, 1, 'running');

  const payload = {
    source: sourceName,
    destination: destName,
    sourceLat: sourceLocation ? sourceLocation.latitude : null,
    sourceLon: sourceLocation ? sourceLocation.longitude : null,
    destLat: destLocation ? destLocation.latitude : null,
    destLon: destLocation ? destLocation.longitude : null,
    vehicle: vehicle,
    preference: preference
  };

  try {
    const token = Auth.getToken();
    const response = await fetch('/api/routes/optimize', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(payload)
    });

    setStepState(stepItems, 1, 'completed');
    setStepState(stepItems, 2, 'running');
    await sleep(250);
    setStepState(stepItems, 2, 'completed');

    setStepState(stepItems, 3, 'running');
    await sleep(250);
    setStepState(stepItems, 3, 'completed');

    setStepState(stepItems, 4, 'running');
    await sleep(200);
    setStepState(stepItems, 4, 'completed');

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Failed to optimize route.');
    }

    setStepState(stepItems, 5, 'completed');
    await sleep(300);
    loadingBox.classList.remove('active');

    // Render Routes on UI and Map
    renderRouteResults(data);

  } catch (err) {
    loadingBox.classList.remove('active');
    showError(err.message || 'An error occurred during route optimization.');
  } finally {
    submitBtn.disabled = false;
  }
}

/**
 * Step Progress Helpers
 */
function resetSteps(stepElements) {
  stepElements.forEach(el => {
    if (el) {
      el.className = 'loading-step-item pending';
      const ind = el.querySelector('.step-indicator');
      if (ind) ind.innerHTML = '';
    }
  });
}

function setStepState(stepElements, index, state) {
  const el = stepElements[index];
  if (!el) return;

  el.className = `loading-step-item ${state}`;
  const ind = el.querySelector('.step-indicator');
  if (ind) {
    if (state === 'completed') {
      ind.innerHTML = '✓';
    } else {
      ind.innerHTML = '';
    }
  }
}

/**
 * Render Route Results in Sidebar & Draw on Leaflet Map
 */
function renderRouteResults(data) {
  const resultsContainer = document.getElementById('resultsContainer');
  resultsContainer.innerHTML = '';

  currentRoutes = data.routes || [];

  if (currentRoutes.length === 0) {
    resultsContainer.innerHTML = '<div class="alert alert-info">No viable routes found between selected locations.</div>';
    return;
  }

  // Results Header
  const header = document.createElement('div');
  header.className = 'results-header';
  header.innerHTML = `
    <div>
      <div class="results-title">Evaluated Candidate Routes (${currentRoutes.length})</div>
      <div style="font-size: 0.75rem; color: var(--text-muted); margin-top: 2px;">
        ${escapeHtml(data.source.name)} ➔ ${escapeHtml(data.destination.name)}
      </div>
    </div>
    <span class="badge badge-ap" style="background: rgba(56, 189, 248, 0.15); color: #38bdf8;">${escapeHtml(data.vehicle)}</span>
  `;
  resultsContainer.appendChild(header);

  // Render Route Cards
  currentRoutes.forEach((route, idx) => {
    const isRecommended = route.recommended;
    const card = document.createElement('div');
    card.className = `route-card ${isRecommended ? 'recommended-card' : ''} ${idx === 0 ? 'active' : ''}`;
    card.id = `route-card-${idx}`;

    // Traffic Badge Color
    let trafficBadgeClass = 'badge-success';
    if (route.traffic && (route.traffic.includes('Moderate') || route.traffic.includes('moderate'))) trafficBadgeClass = 'badge-warning';
    else if (route.traffic && (route.traffic.includes('Heavy') || route.traffic.includes('Severe'))) trafficBadgeClass = 'badge-danger';
    else if (route.traffic && route.traffic.includes('unavailable')) trafficBadgeClass = 'badge-info';

    // Weather Badge Color
    let weatherBadgeClass = 'badge-success';
    if (route.weather && (route.weather.includes('Rain') || route.weather.includes('Wind'))) weatherBadgeClass = 'badge-warning';
    else if (route.weather && (route.weather.includes('Heavy Rain') || route.weather.includes('Thunderstorm'))) weatherBadgeClass = 'badge-danger';

    // Telemetry coverage string
    let coverageHtml = '';
    if (route.dataQuality) {
      coverageHtml = `<div style="font-size: 0.72rem; color: var(--text-muted); margin-bottom: 8px;">📡 ${escapeHtml(route.dataQuality)}</div>`;
    }

    card.innerHTML = `
      <div class="card-top">
        <div class="card-route-title">
          ${isRecommended ? '⭐ RECOMMENDED ROUTE' : escapeHtml(route.routeType)}
        </div>
        <div class="card-score-pill">
          ${route.score.toFixed(1)} / 100
        </div>
      </div>

      <div style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 8px; font-weight: 600;">
        ${escapeHtml(route.routeName)}
      </div>

      ${coverageHtml}

      <div class="metrics-grid">
        <div class="metric-item">
          <div class="metric-label">📏 Distance</div>
          <div class="metric-value">${route.distanceKm.toFixed(1)} km</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">⏱️ Estimated Time</div>
          <div class="metric-value">${formatMinutes(route.estimatedTimeMinutes)}</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">🚦 Segment Traffic</div>
          <div class="metric-value" style="font-size: 0.85rem;">
            <span class="badge ${trafficBadgeClass}">${escapeHtml(route.traffic)}</span>
            ${route.trafficDetails && route.trafficDetails.currentSpeedKmh != null ? `
              <div style="font-size: 0.72rem; color: var(--text-secondary); margin-top: 3px; line-height: 1.35;">
                Speed: <b>${route.trafficDetails.currentSpeedKmh} km/h</b> <span style="color:var(--text-muted); font-size: 0.68rem;">(Free: ${route.trafficDetails.freeFlowSpeedKmh} km/h)</span>
                ${route.trafficDetails.delayMinutes > 0 ? `<br><span style="color: #f87171; font-weight: 600;">+${route.trafficDetails.delayMinutes.toFixed(0)} min delay</span>` : '<br><span style="color: #34d399; font-weight: 500;">Free-flowing corridor</span>'}
              </div>
            ` : ''}
            ${route.congestedSegmentCount > 0 ? `<div style="font-size: 0.7rem; color: #f87171; margin-top: 3px; font-weight: 600;">⚠️ ${route.congestedSegmentCount} congested section(s)</div>` : ''}
          </div>
        </div>
        <div class="metric-item">
          <div class="metric-label">🌦️ Segment Weather</div>
          <div class="metric-value" style="font-size: 0.85rem;">
            <span class="badge ${weatherBadgeClass}">${escapeHtml(route.weather)}</span>
            ${route.rainAffectedSegmentCount > 0 ? `<div style="font-size: 0.7rem; color: #fbbf24; margin-top: 3px;">${route.rainAffectedSegmentCount} rain section(s)</div>` : ''}
          </div>
        </div>
      </div>

      ${route.recommendationReason ? `
        <div class="recommendation-explanation">
          <div class="explanation-title">💡 Why this route?</div>
          <div class="explanation-text">${escapeHtml(route.recommendationReason)}</div>
        </div>
      ` : ''}
    `;

    card.addEventListener('click', () => {
      selectRoute(idx);
    });

    resultsContainer.appendChild(card);
  });

  // Draw on Leaflet Map
  drawRoutesOnMap(data.source, data.destination, currentRoutes);
}

/**
 * Draw Routes and Markers on Leaflet Map
 */
function drawRoutesOnMap(source, dest, routes) {
  // Clear previous layers
  routeLayers.forEach(layer => map.removeLayer(layer));
  routeLayers = [];
  segmentLayers.forEach(layer => map.removeLayer(layer));
  segmentLayers = [];

  if (sourceMarker) map.removeLayer(sourceMarker);
  if (destMarker) map.removeLayer(destMarker);

  // Custom Markers
  const sourceIcon = L.divIcon({
    className: '',
    html: '<div class="custom-map-marker marker-source" title="Source">S</div>',
    iconSize: [32, 32],
    iconAnchor: [16, 16]
  });

  const destIcon = L.divIcon({
    className: '',
    html: '<div class="custom-map-marker marker-dest" title="Destination">D</div>',
    iconSize: [32, 32],
    iconAnchor: [16, 16]
  });

  sourceMarker = L.marker([source.latitude, source.longitude], { icon: sourceIcon })
    .bindPopup(`<b>Source:</b> ${escapeHtml(source.displayName || source.name)}`)
    .addTo(map);

  destMarker = L.marker([dest.latitude, dest.longitude], { icon: destIcon })
    .bindPopup(`<b>Destination:</b> ${escapeHtml(dest.displayName || dest.name)}`)
    .addTo(map);

  const bounds = L.latLngBounds([
    [source.latitude, source.longitude],
    [dest.latitude, dest.longitude]
  ]);

  // Render Polylines for each candidate route
  routes.forEach((route, idx) => {
    // GeoJSON [[lon, lat], ...] -> Leaflet [[lat, lon], ...]
    const latLngs = route.path.map(pt => [pt[1], pt[0]]);

    if (latLngs.length > 0) {
      latLngs.forEach(ll => bounds.extend(ll));

      const style = ROUTE_STYLES[idx % ROUTE_STYLES.length];
      const polyline = L.polyline(latLngs, {
        color: style.color,
        weight: (idx === 0) ? style.weight + 1 : style.weight,
        opacity: (idx === 0) ? 0.95 : 0.65,
        dashArray: style.dashArray
      }).addTo(map);

      polyline.bindPopup(`
        <div style="font-family: inherit; font-size: 0.85rem;">
          <b style="color: ${style.color}; font-size: 0.95rem;">${escapeHtml(route.routeType)} (${escapeHtml(route.routeName)})</b><br>
          <b>Distance:</b> ${route.distanceKm.toFixed(1)} km<br>
          <b>Est. Time:</b> ${formatMinutes(route.estimatedTimeMinutes)}<br>
          <b>Traffic:</b> ${escapeHtml(route.traffic)}<br>
          <b>Weather:</b> ${escapeHtml(route.weather)}<br>
          <b>Score:</b> ${route.score.toFixed(1)} / 100
        </div>
      `);

      polyline.on('click', () => {
        selectRoute(idx);
      });

      routeLayers.push(polyline);

      // Render segment traffic telemetry overlays if segments are present
      if (route.segments && route.segments.length > 0) {
        route.segments.forEach((seg) => {
          if (seg.path && seg.path.length > 0) {
            const segLatLngs = seg.path.map(pt => [pt[1], pt[0]]);
            const segColor = TRAFFIC_COLORS[seg.congestionLevel] || style.color;

            const segPolyline = L.polyline(segLatLngs, {
              color: segColor,
              weight: (idx === 0) ? 6 : 4,
              opacity: (idx === 0) ? 0.9 : 0.4
            }).addTo(map);

            let popupContent = `
              <div style="font-family: inherit; font-size: 0.82rem; min-width: 190px;">
                <b style="color: ${segColor}; font-size: 0.88rem;">Section ${seg.segmentIndex}: ~${seg.distanceKm.toFixed(1)} km</b><br>
                <b>Expected Arrival:</b> ${escapeHtml(seg.expectedArrivalTime || 'In transit')}<br>
                <hr style="border: 0; border-top: 1px solid #334155; margin: 5px 0;">
                <b>Traffic Flow:</b> <span style="font-weight:700; color:${segColor};">${escapeHtml(seg.congestionLevel || 'Available')}</span><br>
            `;
            if (seg.currentSpeedKmh != null) {
              popupContent += `Current Speed: <b>${seg.currentSpeedKmh} km/h</b><br>`;
              popupContent += `Free Flow Speed: <b>${seg.freeFlowSpeedKmh} km/h</b><br>`;
              if (seg.congestionRatio != null) {
                popupContent += `Flow Ratio: <b>${Math.round(seg.congestionRatio * 100)}%</b> of free flow<br>`;
              }
            }
            popupContent += `
                <hr style="border: 0; border-top: 1px solid #334155; margin: 5px 0;">
                <b>Weather:</b> ${escapeHtml(seg.weatherCondition || 'Unavailable')}<br>
            `;
            if (seg.temperatureCelsius != null) {
              popupContent += `Temp: <b>${seg.temperatureCelsius}°C</b> | Rain Prob: <b>${seg.precipitationProbability || 0}%</b><br>`;
            }
            if (seg.windSpeedKmh != null) {
              popupContent += `Wind: <b>${seg.windSpeedKmh} km/h</b><br>`;
            }
            popupContent += `</div>`;

            segPolyline.bindPopup(popupContent);
            segmentLayers.push(segPolyline);
          }
        });
      }
    }
  });

  // Bring recommended route layer to front
  if (routeLayers.length > 0) {
    routeLayers[0].bringToFront();
  }

  // Smooth fit bounds
  map.fitBounds(bounds, { padding: [50, 50] });
}

/**
 * Highlight Selected Route Card and Polyline
 */
function selectRoute(idx) {
  activeRouteIndex = idx;

  currentRoutes.forEach((_, i) => {
    const card = document.getElementById(`route-card-${i}`);
    if (card) {
      if (i === idx) {
        card.classList.add('active');
        card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      } else {
        card.classList.remove('active');
      }
    }
  });

  routeLayers.forEach((layer, i) => {
    if (i === idx) {
      layer.setStyle({ weight: 7, opacity: 1.0 });
      layer.bringToFront();
      layer.openPopup();
    } else {
      const origStyle = ROUTE_STYLES[i % ROUTE_STYLES.length];
      layer.setStyle({ weight: origStyle.weight, opacity: 0.35 });
    }
  });
}

function formatMinutes(mins) {
  if (mins < 60) {
    return Math.round(mins) + ' min';
  }
  const hours = Math.floor(mins / 60);
  const remainingMins = Math.round(mins % 60);
  return `${hours} hr ${remainingMins} min`;
}

function showError(msg) {
  const alert = document.getElementById('errorAlert');
  if (alert) {
    alert.innerText = msg;
    alert.style.display = 'flex';
  }
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
