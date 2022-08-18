# Tests de performance via serialisation d'objet java


## Tests locaux

Moteur Jetstream sous Docker
Stream in memory
Ecriture 1 message à la fois

Debit max écriture (300 kMsg, 28s): 10.7 kMsg/s
A titre de contrôle, meme code java, sans l'appel à la publication: 460 kMsg/s

### Client en mode push

Meme débit que l'émetteur.

Min latency: 0 ms
Average end-to-end latency: 0.092015 ms
Max latency: 7 ms
Standard deviation of end-to-end latency: 0.031899 ms

### Client en mode pull (Batch 10000)

Meme débit que l'émetteur.

Min latency: 0 ms
Average end-to-end latency: 0.382976 ms
Max latency: 20 ms
Standard deviation of end-to-end latency: 1.969993 ms


