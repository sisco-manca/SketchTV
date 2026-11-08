# Ottimizzazione del dispositivo

Gli strumenti sono opzionali e si eseguono sul computer del proprietario. La configurazione della Home e la modifica della TV sono attività separate. Nessun dispositivo viene cercato o collegato automaticamente.

## Cosa può migliorare

Ridurre le animazioni può rendere i passaggi più immediati; non aumenta la potenza del processore. Un timeout di sospensione può ridurre il tempo trascorso acceso. Per la memoria occorre misurare anche PSS della Home e condizioni delle app aperte: MemFree da solo non misura velocità o lavoro risparmiato.

Non ci sono task killer, svuotamento forzato delle cache, limiti ai processi in background, modifiche CPU, root o liste automatiche di debloat. Queste operazioni possono peggiorare riaperture, streaming, ingressi e avvio. Il tool non disabilita alcuna app.

## 1. Ispezione autorizzata

Installa ADB dagli strumenti ufficiali Android. Abilita il debugging solo per il tempo necessario e autorizza il tuo computer. Individua il seriale con `adb devices -l`. Non pubblicarlo e non sostituirlo con indirizzi di altre persone.

```sh
python3 tools/device.py --serial SERIAL inspect --output .local/device/before.json
```

Sola lettura sulla TV: RAM, spazio /data, uptime, versione/modello, impostazioni rilevanti e dati meminfo/gfxinfo di Sketch TV. Non avvia app e non azzera statistiche. I dati sono scritti privatamente in `.local/`, ignorata da Git, con file a permessi proprietario. Il tool rifiuta output dentro altre cartelle del checkout. Sono ammessi percorsi esterni al checkout.

## 2. Piano senza modifiche

```sh
python3 tools/device.py plan --animations-off --sleep-five-minutes
```

Legge lo snapshot e stampa le singole proposte. Senza opzioni non propone nulla. Controlla `.local/device/plan.json`: include valori originali e obiettivo. Gli unici interventi supportati sono tre scale di animazione globali a zero e `secure:sleep_timeout` a 300000 ms. La sospensione effettiva dipende dal firmware e deve essere provata con inattività reale.

Screensaver, Home, Accessibilità, tastiera, Bluetooth, Google Play/services, framework ingressi/tuner/avvio e app restano invariati. Non serve dedurre la tecnologia del pannello: lo screensaver non viene toccato.

## 3. Conferma per ogni impostazione

```sh
python3 tools/device.py --serial SERIAL apply
```

Chiede `SI` separatamente per ogni intervento; EOF o qualsiasi altra risposta rifiutano quella modifica. Non usare pipe o automazioni per confermare al posto della persona. Prima della scrittura salva e sincronizza il registro, poi verifica il risultato. Un valore originariamente assente è conservato come null e viene eliminato correttamente al ripristino.

Il piano è legato a seriale ADB, identificativo univoco del dispositivo (seriale hardware oppure Android ID) e fingerprint del firmware, conservati soltanto sotto forma di hash. Se manca un identificativo univoco il tool si ferma. Cambiare indirizzo, collegamento o firmware richiede una nuova valutazione: il tool si ferma, non assume che sia lo stesso dispositivo. Se lo stato è cambiato dopo l'ispezione si ferma; un registro esistente non viene sovrascritto. Dopo un'interruzione usa il registro per il ripristino prima di un nuovo piano.

## 4. Confronto e ripristino

Ripeti le stesse app e sorgenti, attendi che il sistema si stabilizzi e raccogli più osservazioni, distinguendo avvio freddo, ritorno dalle app e standby. Il tool non riavvia il dispositivo.

```sh
python3 tools/device.py --serial SERIAL inspect --output .local/device/after.json
python3 tools/device.py compare .local/device/before.json .local/device/after.json
python3 tools/device.py --serial SERIAL undo
```

Il confronto mostra anche una diminuzione della memoria disponibile: non la presenta come miglioramento. Le misure istantanee non sono un benchmark di latenza. Per tempi reali ripeti lo stesso percorso e riporta mediana e intervallo; i raw meminfo/gfxinfo restano nei file locali.

Undo esamina il registro al contrario e chiede conferma per ogni ripristino. Se il valore è già originale non scrive; se qualcuno lo ha cambiato nel frattempo si ferma invece di sovrascriverlo. Gestisce anche una scrittura interrotta prima della verifica.

Al termine verifica manualmente Home, app, ingressi e sospensione, poi spegni **debugging USB, di rete e wireless**. Se lasciati attivi, altri dispositivi possono tentare di chiedere accesso. Riattivali soltanto per manutenzione autorizzata. Non inviare snapshot, registri o IP in issue, PR o artifact CI.
