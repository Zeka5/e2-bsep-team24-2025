# Log Server

Minimalistički Node.js server za prijem i čuvanje logova iz Spring Boot aplikacije.

## Instalacija

```bash
npm install
```

## Pokretanje

```bash
npm start
```

Server se pokreće na `http://localhost:3001`

## Endpoints

- `POST /api/logs` - Prima logove iz Spring Boot aplikacije
- `GET /health` - Health check endpoint

## Logovi

Logovi se čuvaju u `logs/` folderu sa nazivom `app-YYYY-MM-DD.log`
