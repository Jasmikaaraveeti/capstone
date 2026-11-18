# INTELLIROUTE EXPLORER
### Intelligent Multi-Factor Route Optimization System for Andhra Pradesh, India

A comprehensive, production-grade web application engineered for intelligent multi-factor route planning across **Andhra Pradesh, India**. Powered by Spring Boot, Spring Security, BCrypt, JWT, MySQL, Leaflet, OpenStreetMap, Nominatim, OSRM, TomTom Traffic API, and Open-Meteo.

---

## 1. Project Overview

IntelliRoute Explorer is a full-stack engineering solution that evaluates genuine route corridors in Andhra Pradesh using multi-factor telemetry:
- **Distance** (OSRM calculated distance)
- **Transit Duration** (OSRM calculated duration calibrated to vehicle profile)
- **Live Traffic Flow** (TomTom Traffic Flow Segment API)
- **Atmospheric Weather Telemetry** (Open-Meteo live corridor forecast)
- **Multi-Factor Scoring** (0–100 dynamic normalization according to user preference)
- **Interactive Geospatial Visualization** (Leaflet map with genuine GeoJSON polylines)

> **Important Boundary Rule**: The application strictly enforces Andhra Pradesh boundaries. Requests for locations outside Andhra Pradesh (such as Hyderabad, Chennai, Bengaluru, etc.) are rejected by both frontend filters and backend polygon boundaries. Road conditions are intentionally excluded as per system specifications.

---

## 2. Technology Stack

### Backend
- **Java**: 21 (LTS)
- **Framework**: Spring Boot 3.3.4
- **Security**: Spring Security 6.3 with stateless JWT authentication & BCrypt password hashing
- **Persistence**: Spring Data JPA & Hibernate
- **Database**: MySQL 8.0
- **Build System**: Maven (with included `mvnw.cmd` wrapper)

### Frontend
- **Languages**: HTML5, CSS3, Modern JavaScript (ES6+)
- **Mapping & GIS**: Leaflet 1.9.4 & OpenStreetMap tile layer
- **Architecture**: Single Page Architecture with decoupled REST API communication

### External Telemetry & Routing APIs
1. **OpenStreetMap**: Geographic base map tiles
2. **Nominatim**: OpenStreetMap geocoding service with custom User-Agent and AP boundary constraint
3. **OSRM (Open Source Routing Machine)**: Genuine multi-alternative route geometry, distances, and duration (`alternatives=true`, `overview=full`, `geometries=geojson`)
4. **TomTom Traffic Flow API**: Live corridor traffic flow segment queries (`currentSpeed`, `freeFlowSpeed`)
5. **Open-Meteo Forecast API**: Real-time corridor meteorological telemetry (Temperature, Rain, Wind speed, WMO weather codes)

---

## 3. System Architecture & Project Structure

```text
vinayak/
│
├── pom.xml                                      # Maven dependencies & build configuration
├── README.md                                    # Comprehensive system documentation
├── .gitignore                                   # Ignore build artifacts & secrets
├── mvnw.cmd / mvnw                              # Maven wrapper executables
├── .mvn/                                        # Maven wrapper settings
│
└── src/
    ├── main/
    │   ├── java/com/intelliroute/
    │   │   ├── IntelliRouteApplication.java     # Spring Boot application entry point
    │   │   ├── controller/
    │   │   │   ├── AuthController.java          # Register, Login, Me endpoints
    │   │   │   ├── RouteController.java         # Status, AP locations, Route optimization endpoints
    │   │   │   └── GlobalExceptionHandler.java  # Centralized REST error handler
    │   │   ├── model/
    │   │   │   ├── User.java                    # JPA User entity (MySQL)
    │   │   │   ├── Coordinate.java              # AP Location data model
    │   │   │   ├── RouteRequest.java            # Route optimization request DTO
    │   │   │   ├── RouteResponse.java           # Route optimization response DTO
    │   │   │   ├── RouteInfo.java               # Single route details with GeoJSON path
    │   │   │   ├── TrafficInfo.java             # Live traffic telemetry model
    │   │   │   ├── WeatherInfo.java             # Live weather telemetry model
    │   │   │   ├── VehicleType.java             # CAR, BUS, AUTO, BICYCLE enum
    │   │   │   ├── PreferenceType.java          # RECOMMENDED, FASTEST, SHORTEST, WEATHER_SAFE enum
    │   │   │   ├── AuthRequest.java             # Login request DTO
    │   │   │   ├── RegisterRequest.java         # Registration request DTO
    │   │   │   └── AuthResponse.java            # JWT authentication response DTO
    │   │   ├── repository/
    │   │   │   └── UserRepository.java          # JPA Repository for User entity
    │   │   ├── security/
    │   │   │   ├── SecurityConfig.java          # Spring Security 6 stateless filter chain & CORS
    │   │   │   ├── JwtService.java              # JJWT 0.12.5 token generation & validation
    │   │   │   └── JwtAuthenticationFilter.java # Header bearer token filter
    │   │   └── service/
    │   │       ├── AuthService.java             # BCrypt registration & authentication
    │   │       ├── GeocodingService.java        # 60+ AP locations dataset, Nominatim & polygon boundary check
    │   │       ├── RoutingService.java          # OSRM genuine alternative route generator
    │   │       ├── TrafficService.java          # TomTom Traffic Flow API integration
    │   │       ├── WeatherService.java          # Open-Meteo live corridor weather integration
    │   │       ├── RouteScoringService.java     # Multi-factor normalized scoring & explanations
    │   │       └── RouteOptimizationService.java# End-to-end orchestration pipeline
    │   │
    │   └── resources/
    │       ├── application.properties           # Spring Boot configuration
    │       └── static/
    │           ├── index.html                   # Landing page with auth redirect
    │           ├── login.html                   # Clean, modern login page
    │           ├── register.html                # Account registration page
    │           ├── dashboard.html               # Main Route Planning & Leaflet Dashboard
    │           ├── css/
    │           │   ├── style.css                # Base design system & typography
    │           │   ├── auth.css                 # Login / Register styles
    │           │   └── dashboard.css            # Sidebar, map layout, route cards & badges
    │           └── js/
    │               ├── auth.js                  # JWT token storage & authentication handling
    │               └── dashboard.js             # AP autocomplete, map rendering & animation
    │
    └── test/
        └── java/com/intelliroute/
            └── IntelliRouteExplorerApplicationTests.java # Full integration test suite (10 test cases)
```

---

## 4. Environment Variables & Configuration

The application uses environment variables with safe defaults:

| Variable | Description | Default in application.properties |
| :--- | :--- | :--- |
| `DB_PASSWORD` | MySQL root user password | `Jyothi@123` (configured for local machine) |
| `TOMTOM_API_KEY` | TomTom Traffic API key | *Empty* (Gracefully returns "Traffic data unavailable") |
| `JWT_SECRET` | Secret key for signing JWT tokens | Preconfigured 256-bit secure key |

### Configuring on Windows (Command Prompt or PowerShell)

**PowerShell:**
```powershell
$env:DB_PASSWORD="your_mysql_password"
$env:TOMTOM_API_KEY="your_tomtom_api_key"
```

**Command Prompt:**
```cmd
set DB_PASSWORD=your_mysql_password
set TOMTOM_API_KEY=your_tomtom_api_key
```

---

## 5. Database Setup (MySQL)

1. Open MySQL Command Line or MySQL Workbench:
```sql
CREATE DATABASE IF NOT EXISTS intelliroute;
```
2. Hibernate automatically manages the schema with `spring.jpa.hibernate.ddl-auto=update`.
3. The `users` table contains:
   - `id`: BIGINT AUTO_INCREMENT PRIMARY KEY
   - `name`: VARCHAR(255) NOT NULL
   - `email`: VARCHAR(255) UNIQUE NOT NULL
   - `password`: VARCHAR(255) NOT NULL (BCrypt hashed, 60 chars)
   - `created_at`: DATETIME NOT NULL

---

## 6. How to Build and Run

### Step 1: Compile and Run Tests
Using the included Maven wrapper in the project root:
```powershell
.\mvnw.cmd clean test
```
All 10 integration and unit tests will run and pass.

### Step 2: Run the Spring Boot Server
```powershell
.\mvnw.cmd spring-boot:run
```

### Step 3: Open in Browser
Visit:
```text
http://localhost:8080
```
- New users can register at `http://localhost:8080/register.html`.
- Existing users log in at `http://localhost:8080/login.html`.
- The dashboard is accessible at `http://localhost:8080/dashboard.html`.

---

## 7. REST API Endpoints

### Public Endpoints:
- `POST /api/auth/register`: Register new user with full name, email, password.
- `POST /api/auth/login`: Authenticate and receive JWT token.
- `GET /api/routes/status`: System health check and telemetry provider status.
- `GET /api/routes/locations?q={query}`: Search and retrieve Andhra Pradesh locations.

### Authenticated Endpoints (`Authorization: Bearer <token>`):
- `GET /api/auth/me`: Get details of the current logged-in user.
- `POST /api/routes/optimize`: Submit route optimization request:
  ```json
  {
    "source": "VIT-AP University",
    "destination": "Thullur",
    "vehicle": "CAR",
    "preference": "RECOMMENDED"
  }
  ```

---

## 8. Multi-Factor Scoring Algorithm

All individual factors are normalized onto a clean scale of **0 to 100**:

### Normalization Formula (Lower = Better for Distance & Time)
$$\text{Score} = \frac{\text{Max} - \text{Current}}{\text{Max} - \text{Min}} \times 100$$
*(If all alternatives have identical metrics or only 1 route is returned, the normalized score is set to 100.0 without division by zero).*

### Higher = Better for Traffic & Weather
- **Traffic**: Evaluated from TomTom speed ratio $\frac{\text{CurrentSpeed}}{\text{FreeFlowSpeed}}$:
  - Low congestion ($\ge 85\%$ free flow) = 100
  - Moderate congestion ($65\% - 85\%$) = 60
  - Heavy congestion ($40\% - 65\%$) = 20
  - Severe congestion ($< 40\%$) = 5
  - Unavailable provider = 60 (neutral baseline, clearly marked)
- **Weather**: Evaluated from Open-Meteo WMO weather codes and precipitation:
  - Clear / Sunny (WMO 0, 1) = 100
  - Cloudy / Overcast (WMO 2, 3) = 85
  - Light Rain / Drizzle (rain $> 0.1$ mm) = 65
  - Strong Wind ($> 40$ km/h) = 45
  - Heavy Rain (rain $\ge 4.0$ mm) = 25
  - Severe Thunderstorm (WMO 95–99) = 10

### Preference Weights

| Preference | Distance | Time | Traffic | Weather |
| :--- | :---: | :---: | :---: | :---: |
| **⭐ Recommended / Balanced** | 20% | 25% | 30% | 25% |
| **⚡ Fastest Time** | 15% | 50% | 30% | 5% |
| **📍 Shortest Distance** | 50% | 25% | 20% | 5% |
| **🌦 Weather Safe** | 10% | 15% | 25% | 50% |

$$\text{FinalScore} = (W_{\text{traffic}} \times S_{\text{traffic}}) + (W_{\text{weather}} \times S_{\text{weather}}) + (W_{\text{dist}} \times S_{\text{dist}}) + (W_{\text{time}} \times S_{\text{time}})$$

The route with the highest score is marked `recommended: true` and rendered with a prominent emerald glowing card and primary map polyline.

---

## 9. Real Telemetry vs Fake Fallbacks

IntelliRoute Explorer maintains strict integrity and never fabricates route data:
1. **OSRM Routes**: Genuine geometry coordinates are fetched directly from OSRM. If OSRM returns 1 route, 1 is shown; if 2, 2 are shown; if 3, 3 are shown. No straight lines or fake alternative coordinates are created.
2. **Open-Meteo Weather**: Real-time atmospheric readings (temperature, precipitation, wind speed, weather code) are queried from Open-Meteo for the corridor midpoint.
3. **TomTom Traffic**: Real traffic speed and free-flow speed are queried when `TOMTOM_API_KEY` is provided. If the key is not set or the provider is unavailable, the UI cleanly displays **"Traffic data unavailable"** without inventing fake congestion numbers.

---

## 10. Test Cases Summary

All required test scenarios were validated via automated integration tests (`IntelliRouteExplorerApplicationTests.java`):

| Test # | Route / Scenario | Vehicle | Preference | Status | Result |
| :---: | :--- | :---: | :---: | :---: | :--- |
| **1** | VIT-AP University ➔ Thullur | Car | Recommended | **PASSED** | 14.1 km, genuine OSRM geometry, clear weather |
| **2** | Nellore ➔ Tirupati | Bus | Fastest Time | **PASSED** | 136.2 km, bus time calibrated, ranked by speed |
| **3** | Vijayawada ➔ Guntur | Auto | Shortest Distance | **PASSED** | 39.5 km, auto speed calibrated, ranked by distance |
| **4** | Kadapa ➔ Tirupati | Car | Recommended | **PASSED** | 139.7 km, multi-factor scoring |
| **5** | Vijayawada ➔ Guntur | Bicycle | Recommended | **PASSED** | 39.5 km, cycling duration calibrated (~158 mins) |
| **6** | Outside AP (Hyderabad, Chennai) | Any | Any | **PASSED** | Rejected with "Location is outside Andhra Pradesh" |
| **7** | Invalid Location (`XYZABC123INVALID`) | Any | Any | **PASSED** | Rejected with "Location could not be found" |
| **8** | Scoring Algorithm Normalization | - | - | **PASSED** | 0-100 normalization and weights accurate |
| **9** | Registration & BCrypt Auth | - | - | **PASSED** | Password securely hashed, JWT generated |
| **10** | Spring Context & DB Connection | - | - | **PASSED** | MySQL80 connected via HikariCP |

---

## 11. Known Limitations & Technical Details

1. **Vehicle-Specific Routing**:
   - The public demo OSRM instance provides routing networks primarily for automobile driving and bicycle networks.
   - For **Bus** and **Auto**, genuine highway routing from OSRM is used, with honest calibration of vehicle transit durations (commercial bus governing limits ~1.25x and 3-wheeler auto-rickshaw governing limits ~1.35x).
2. **Public API Rate Limits**:
   - The demo OSRM server (`router.project-osrm.org`) and OpenStreetMap Nominatim have public usage guidelines. The application uses a local cache for geocoded locations and samples representative corridor points to respect rate limits.
3. **TomTom Traffic API**:
   - Requires a valid TomTom developer key in `TOMTOM_API_KEY`. Free tier permits up to 2,500 daily queries. When unavailable or unconfigured, the system safely reports "Traffic data unavailable" without halting optimization.
