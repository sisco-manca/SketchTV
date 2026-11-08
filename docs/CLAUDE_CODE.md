# Usare Sketch TV con Claude Code

Apri il checkout in Claude Code dopo aver installato e autenticato la CLI secondo [la documentazione ufficiale](https://code.claude.com/docs/en/overview). Il progetto non configura account, chiavi, MCP o modelli.

```sh
cd SketchTV
claude
```

`CLAUDE.md` importa `AGENTS.md`: una sola base di istruzioni per architettura, privacy, comandi e limiti delle operazioni sulla TV. Le skill di progetto in `.claude/skills/` sono invocabili dall'utente; `disable-model-invocation: true` impedisce l'attivazione automatica. Non sono inclusi hook eseguibili, auto-approvazioni o permessi per saltare le conferme.

## Comandi di progetto

- `/sketch-tv Aggiungi una scorciatoia e verifica il focus`: guida la modifica del JSON/codice e i relativi test.
- `/sketch-tv Implementa un widget con fonte e comportamento offline`: usa il contratto dei provider e richiede test del parser.
- `/device-audit Prepara una valutazione, senza collegarti a un dispositivo`: produce il percorso di ispezione e piano, usando dati simulati finché non è autorizzato un target reale.

I comandi sono istruzioni per l'assistente, non un'ulteriore applicazione o un motore capace di leggere automaticamente uno sketch. Il disegno si confronta con lo studio locale.

## Ciclo rapido, senza hardware

```sh
python3 tools/check.py
python3 tools/preview.py
```

Per i controlli opzionali:

```sh
JSON_JAR=/percorso/json-20240303.jar python3 tools/check.py --parsers
CHROMIUM=/percorso/chromium python3 tools/check.py --browser
python3 tools/check.py --build
```

Servono JDK 17 per i test Java; usa JAVA_HOME o PATH. Parser, browser e build hanno dipendenze documentate in VALIDATION.md. Il checker non scarica strumenti e non chiama ADB reale. I test del dispositivo usano solo un fake; eseguire un test non equivale a verificare una TV.

Conserva preferenze personali in `CLAUDE.local.md` e impostazioni CLI locali in `.claude/settings.local.json`, entrambi ignorati. Non inserirvi autorizzazioni permanenti alle scritture sul dispositivo. Evita di dare all'assistente contenuti di keystore o token. I dati di ispezione restano in `.local/` e non devono essere inseriti nel contesto remoto senza una scelta esplicita del proprietario.

Per operazioni reali l'assistente deve distinguere autorizzazione a leggere da autorizzazione a scrivere. Un piano non dà consenso alle modifiche. L'utente conferma ogni intervento nel tool interattivo; Claude non deve generare le risposte SI al suo posto. Le regole sono istruzioni di progetto, non una sandbox: restano necessari i normali controlli dei permessi della CLI.
