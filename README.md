# Voting API

Az Országgyűlés szavazásait rögzítő és kiértékelő REST backend (Java 21, Spring Boot 4.1, H2).
A kérések és válaszok JSON formátumúak, a mezőnevek magyarok, a hibák `application/problem+json`
formájúak.

## Előfeltételek

- Java 21 (a Gradle wrapper a többit letölti)
- Docker a konténeres futtatáshoz

## Megjegyzés a feladatleíráshoz: az `eljaras` kötelező

A feladatleírás a mentési kérés és a napi lista sémájában nem sorolja az `eljaras` mezőt a kötelezők
közé, az 5.2 kimutatás válaszsémájában viszont kötelező. Az alkalmazás kötelezőként kezeli, mert:

- eljárás nélkül egy szavazás nem sorolható sem normál, sem különleges eljárásba, így az 5.2
  kimutatás nem lenne egyértelmű;
- alapértéket a leírás nem ad meg, egy feltételezett érték pedig csendben torzíthatná a kimutatást.

Hiányzó `eljaras` esetén a válasz `400 Validációs hiba`.

## Futtatás helyben

```bash
./gradlew bootRun
```

Az alkalmazás a `http://localhost:8080` címen indul, memóriabeli H2 adatbázissal: az adatok
leálláskor elvesznek. Másik porton: `./gradlew bootRun --args='--server.port=8081'`.

## Futtatás konténerben

```bash
docker compose up -d --build
```

A `docker` profil fájl alapú H2-t használ a `voting-api-data` volume-on, így az adatok újraindítás
után megmaradnak. Csak az image építése: `./gradlew dockerBuild` (`voting-api:latest`).

| Környezeti változó | Alapérték | Leírás |
|---|---|---|
| `VOTING_API_PORT` | `8080` | a kifelé publikált port, pl. `VOTING_API_PORT=8081 docker compose up -d` |
| `VOTING_TOTALMEMBERS` | `200` | az Országgyűlés teljes létszáma (minősített többség) |

Leállítás: `docker compose down` (a `-v` kapcsoló az adatokat tároló volume-ot is törli).

## Build és tesztek

```bash
./gradlew build          # fordítás, tesztek, formázás-ellenőrzés
./gradlew spotlessApply  # kódformázás
```

## Végpontok

Minden végpont a `/szavazasok` alatt van.

| Metódus és útvonal | Leírás |
|---|---|
| `POST /szavazas` | szavazás mentése a szavazatokkal |
| `GET /szavazat?szavazas={id}&kepviselo={kepviselo}` | egy képviselő szavazata |
| `GET /eredmeny?szavazas={id}` | egy szavazás eredménye |
| `GET /napi-szavazasok?nap=2023-12-13` | egy (budapesti) nap szavazásai |
| `GET /kepviselo-reszvetel-atlag?tol=2023-12-01&ig=2023-12-31` | átlagos részvétel |
| `GET /kulonleges-eljarasok-szama?tol=2023-12-01&ig=2023-12-31` | különleges eljárások száma |

Példa:

```bash
curl -X POST http://localhost:8080/szavazasok/szavazas \
  -H 'Content-Type: application/json' \
  -d '{
        "idopont": "2023-09-28T11:06:25Z",
        "targy": "Koltsegvetes",
        "tipus": "j",
        "eljaras": "n",
        "elnok": "Kepviselo1",
        "szavazatok": [
          { "kepviselo": "Kepviselo1", "szavazat": "i" },
          { "kepviselo": "Kepviselo2", "szavazat": "n" }
        ]
      }'
# {"szavazasId":"OJ7251"}

curl 'http://localhost:8080/szavazasok/eredmeny?szavazas=OJ7251'
```
