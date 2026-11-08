# Fonti dei widget

| Widget | Provider e documentazione | Provenienza nell'app |
| --- | --- | --- |
| Meteo | [Open-Meteo](https://open-meteo.com/) e [condizioni](https://open-meteo.com/en/terms) | Nome, URL richiesto, data download; dati CC BY 4.0 con attribuzione |
| Risultati F1 e costruttori | [Jolpica F1](https://github.com/jolpica/jolpica-f1) | Nome, endpoint, data download, stagione e gara |
| Calcio, risultati e classifica | ESPN, endpoint site.api.espn.com | Nome, URL richiesto, data download, stagione |

Gli endpoint ESPN usati non costituiscono un contratto pubblico stabile. Non è concessa qui una licenza per redistribuire i dati: verificarne condizioni e disponibilità per il proprio impiego. Anche per Jolpica controllare condizioni e limiti del servizio. Non confondere la licenza del codice del provider con quella dei dati.

Seleziona un widget per aprire provenienza e dettagli. La pagina README non sostituisce l'attribuzione nel prodotto: ogni nuovo adattatore deve mostrare almeno provider, URL, timestamp e stato degli errori. Il timestamp è quello del download, non una garanzia che il provider abbia già pubblicato il risultato più recente. La UI indica dati salvati dopo errori o se vecchi di oltre un'ora.

Clock e data provengono dall'orologio di sistema nel fuso configurato. Le tacche Wi-Fi provengono dalle API Android, possono risultare sconosciute se il firmware limita i dati. Non si avviano scansioni.
