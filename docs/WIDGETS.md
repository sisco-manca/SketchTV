# Estendere i widget

`WidgetProvider` è il contratto comune. Un provider dichiara:

- id e titolo;
- abilitazione e intervallo (almeno 10 minuti);
- fonti con nome, endpoint HTTPS e attribuzione/condizioni;
- parser che restituisce `Content`: titolo e righe da mostrare.

`BuiltinWidgets` registra i provider inclusi. Per un nuovo tipo aggiungi una classe che implementa il contratto e registrala qui. Se desideri parametri nel JSON, estendi anche validazione e generazione in `tools/configure.py`, quindi la presentazione dimostrativa nello studio. La Home Android, i dettagli delle fonti, il caricamento e il rendering generico non richiedono casi dedicati al nuovo formato dati.

`WidgetEngine` gestisce aggiornamenti indipendenti e cache. Riceve trasporto, archivio e segnale di attività sostituibili nei test. `WidgetRepository` lo esegue su un worker, usa HTTPS con timeout/limite risposta e conserva le risposte nelle preferenze private. Le pubblicazioni sono snapshot: il renderer non accede al worker.

Un widget con più endpoint pubblica il contenuto soltanto se tutte le risposte sono valide. I parser non devono inventare valori in presenza di campi mancanti. I provider sportivi verificano coerenza di stagione e, per F1, round. Un fallimento mantiene la precedente coppia completa di dati con la data originale, senza mescolare risposte vecchie e nuove.

Gli aggiornamenti si interrompono fra le richieste quando la Home non è visibile. Una richiesta HTTPS già in corso può terminare entro il timeout; il risultato non viene pubblicato se la Home è stata lasciata. Il ritorno può riprovare immediatamente una richiesta interrotta. I fallimenti hanno un intervallo minimo di tentativo di un minuto; il refresh manuale mantiene un debounce di cinque secondi.

Ogni riquadro mostra il nome della fonte. Selezionandolo si vedono URL, attribuzione, timestamp e stato. Il timestamp è di download, non l'ora ufficiale di pubblicazione; i dettagli ricordano questo limite. La vecchia cache viene segnalata dopo un errore o dopo il maggiore fra un'ora e due intervalli di aggiornamento.

Prima di aggiungere una fonte controlla condizioni, disponibilità e limiti. Non inserire chiavi segrete nell'APK. Aggiungi fixture inventate per risposta valida, dati assenti, formati modificati ed errori; verifica che gli errori conservino l'ultimo contenuto attendibile.

Le scadenze vengono calcolate dal motore: nessun polling fisso di 30 secondi e nessun timer se tutti i provider sono disabilitati.
