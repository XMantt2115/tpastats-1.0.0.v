# TPA Stats (Fabric 1.21.4)

Klientowy mod: gdy ktoś wyśle Ci /tpa, pojawia się na liście. Naciskasz **X**, wybierasz gracza i mod wysyła `/stats <nick>`.

## Budowanie przez GitHub
1. Utwórz repo na GitHubie i wrzuć do niego całą zawartość tego folderu (git add . && git commit && git push).
2. Wejdź w zakładkę **Actions** → przebieg "Build mod" → na dole **Artifacts** → pobierz `tpastats-mod` (jar).
3. Wrzuć jar do `.minecraft/mods` razem z Fabric API 0.119.2+1.21.4 (Fabric Loader 0.16+, Java 21).

## Config: `.minecraft/config/tpastats.json`
- `patterns` – regexy wykrywające prośbę TPA (grupa 1 = nick). Dopasuj do prawdziwej wiadomości z serwera.
- `debugLogMessages` – true = każda wiadomość systemowa trafia do `logs/latest.log`.
- `serverFilter` – mod działa tylko na tym serwerze ("" = wszędzie, np. do testów).
- `autoOpen`, `expireSeconds`, `requireOnline`, `statsCommand`.
