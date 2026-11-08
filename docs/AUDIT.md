# Revisione tecnica — versione 0.3

Analisi di sorgenti Android, generazione/configurazione, anteprima, build/CI, test, licenze, installazione e documentazione. Le conclusioni derivano da lettura e test locali/CI; nessun dispositivo personale è stato collegato. Non è una certificazione di compatibilità o sicurezza universale.

## Difetti individuati e interventi

| Priorità | Riscontro | Intervento e verifica |
| --- | --- | --- |
| Alta | CI ferma in setup-android: richiesta del pacchetto SDK obsoleto `tools` | Installazione esplicita di platform-tools e runner Ubuntu 24.04 stabile (Playwright fissato non supporta il più recente ubuntu-latest); test e build su GitHub Actions |
| Alta | Qualunque file in assets poteva finire nell'APK | Staging di font/licenza e sole immagini referenziate, limiti dimensionali e test di esclusione |
| Media | Controlli widget ogni 30 secondi, anche con scadenze molto più lunghe | Scheduler alla scadenza più vicina, nessun timer per provider disabilitati, test delle scadenze e retry |
| Media | Bitmap lette durante costruzione della Home | Worker locale e budget decodificato totale 4 MiB, fallback a etichette |
| Media | Fitting testo con diminuzioni lineari e lavoro ripetuto per frame | Ricerca binaria, LRU limitata, test su larghezza e Unicode |
| Media | Geometrie icone e citazioni ricostruite in disegno | Preparazione una volta/all'aggiornamento dei dati |
| Media | Catalogo ricostruito a ogni ritorno e icone potenzialmente obsolete | Firma dei dati, invalidazione eventi pacchetto, stato dirty conservato se un caricamento termina a schermata nascosta |
| Media | Schermata rete dichiarata offline su API precedenti a 23 | Fallback NetworkInfo con guardie API |
| Media | Setup assente interpretato come completato | Guardie provisioning/setup con default incompleto; eventuale limite OEM documentato |
| Media | Temi con proprietà sconosciute potevano generare Java non valido; riquadri widget troppo bassi | Allowlist tema, minimi per tipo di contenuto e vincolo clock/weather, test negativi |
| Media | Valori meteo numericamente non validi potevano diventare numeri plausibili | Range, NaN/infinito, pioggia e ordine min/max controllati; test sintetici |
| Media | Anteprima solo su loopback ma Host non verificato | Host e Origin espliciti, CSP e no-referrer; test browser |
| Media | Mancava un flusso misurabile/reversibile per il dispositivo | CLI inspect/plan/apply/undo/compare, allowlist ridotta, conferme individuali, registro atomico prima della scrittura, drift/wrong-device e crash recovery testati con fake |
| Miglioramento | Contesto di sviluppo distribuito nelle guide | AGENTS.md condiviso, import CLAUDE.md, skill manuali e checker offline unico |

## Cosa non dichiarare come risolto

- Non esistono benchmark controllati su hardware in questa revisione: non si dichiarano percentuali di accelerazione o RAM guadagnata.
- La Home originale può comparire brevemente su alcuni firmware; il servizio opzionale non modifica il comportamento interno del sistema.
- Gli endpoint ESPN possono cambiare senza contratto stabile. Le condizioni dei dati vanno verificate per il proprio utilizzo.
- Lo studio è una simulazione: non replica esattamente font metrics o prestazioni Android e non riconosce automaticamente le immagini.
- Non vengono effettuati debloat, modifiche CPU/processi o operazioni sullo screensaver.

## Prossime verifiche prioritarie

1. Matrice di dispositivi Android TV/API, con avvio, standby, ingressi e ripristino; risultati pubblici aggregati senza modelli/identificativi personali non autorizzati.
2. Test strumentati Android per focus, lifecycle, icone aggiornate, servizio Home e limiti bitmap; i test JVM non sostituiscono il framework.
3. Trasporto HTTPS sostituibile con test di timeout, risposte troppo grandi e cancellazione di socket realmente bloccati. Ora i timeout limitano le richieste e la visibilità si controlla fra le letture; la cancellazione non è istantanea.
4. Layout visuale con trascinamento, anteprime/font metrics più vicine al renderer e localizzazione oltre l'italiano dell'interfaccia di base.
5. Gestione schematica delle versioni dei provider e migrazione/pulizia della cache delle vecchie configurazioni.
