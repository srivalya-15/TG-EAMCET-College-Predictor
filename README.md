# TG EAPCET College Predictor

A web app that helps TG EAPCET (Telangana State Engineering, Agriculture and Pharmacy Common Entrance Test) candidates predict which engineering colleges and branches they're likely to get admission into, based on their rank, category, and gender.

**Live demo:** https://tg-eamcet-college-predictor.onrender.com
> Hosted on Render's free tier — the first request after a period of inactivity may take 30-50s to wake up.

## What it does

Enter your EAPCET rank, gender, and reservation category, and the predictor returns a ranked list of colleges and branches you're eligible for, based on the official cutoff-rank data (last rank statement).

- Filter results by specific branches (CSE, ECE, AI & ML, Civil, Mechanical, and 40+ others)
- Supports all reservation categories: OC, BC-A/B/C/D/E, SC-I/II/III, ST, and EWS
- Applies a rank buffer so you also see "reach" colleges near your rank, not only guaranteed admits
- Results are sorted by closing rank, closest matches first
- Includes each college's place, district, and college-type code

## How it works

Cutoff-rank data is loaded from a bundled Excel sheet at application startup into an in-memory H2 database. When you submit a prediction request, the app filters colleges by branch, then checks whether your rank clears the cutoff for your specific gender/category combination, and returns the matches sorted by cutoff rank.

## Tech stack

- **Backend:** Java 17, Spring Boot, Spring Data JPA / Hibernate
- **Database:** H2 (in-memory)
- **Data ingestion:** Apache POI (reads cutoff data from `.xlsx` files)
- **Frontend:** HTML, CSS, Bootstrap, vanilla JavaScript
- **Deployment:** Docker, Render

## Running locally

Requires Java 17+.

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8082` by default (or `$PORT` if set).

## API

| Endpoint | Method | Description |
|---|---|---|
| `/api/branches` | GET | List all branch codes and names |
| `/api/predict` | POST | Predict colleges — params: `rank`, `gender`, `category`, `branches` (optional, repeatable) |

## Deployment

The repo includes a `Dockerfile` and `render.yaml`, so it deploys directly to [Render](https://render.com) as a Blueprint — no manual configuration needed beyond connecting the GitHub repo.
