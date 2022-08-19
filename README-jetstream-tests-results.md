# Tests de performance via serialisation d'objet java

## Synthese


Sauf mention contraire, 
- les écritures se font avec 1 seul pod groupant ses attentes d'acquittement par lot de 200 messages (asynchrone par lot)
- les lectures se font avec 1 seul pod, en mode push
- le régime établi est observé par des sessions d'écritures de nombre de messages croissantss (10K, 10K, 10k, 100k, 300k, 1M), avec 6 à 10s entre chaque session
  d'écriture.
- l'écrivain java n'a pas de test permettant de mesurer le "régime établi" ce qui peut expliquer une latence max (phase de chauffage de l'écrivain) importante, meme en régime établi
  ==> amélioration possible : fournir les sessions d'écriture à 1 meme process écrivain


* Sans réplication, sur cluster K8s

  - En régime établi: pour 47 kMsgs / s => 3.6 ms de latence, déviation standard de 5.6 ms . Latence max pouvant aller jusqu'à 80 ms
      
  - Pendant la phase de préchauffage java (10000 evénements, traités sur la premiere seconde), des latences bien supérieures : 300 ms.

* Avec replication 2, sur cluster K8s

  - En régime établi: pour 26 kMsgs / s => 6.6 ms de latence, déviation standard de 3.6 ms . Latence max pouvant aller jusqu'à 100 ms
      
  - Pendant la phase de préchauffage java (10000 evénements, traités sur la premiere seconde), des latences bien supérieures : 300 ms.








## Tests locaux en mode Write Synchrone

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


## Tests locaux en mode Write ASynchrone (ack / 5000)

Moteur Jetstream sous Docker
Stream in memory
Ecriture 5000 message à la fois avant d'attendre l'ack des 5000

Debit max écriture (1000 kMsg, 13s): 78.4 kMsg/s

### Client en mode push

Meme débit que l'émetteur.

Min latency: 0 ms
Average end-to-end latency: 0.092015 ms
Max latency: 7 ms
Standard deviation of end-to-end latency: 0.031899 ms

### Client en mode pull (Batch 10000)

Meme débit que l'émetteur.

Min latency: 0 ms
Average end-to-end latency: 1.964032 ms
Max latency: 67 ms
Standard deviation of end-to-end latency: 4.275415 ms



## Links

### Java compiler


https://stackoverflow.com/questions/53885832/why-is-server-option-there-when-server-vm-is-the-default-option

-client and -server are ignored on modern JVMs, as easy as that. There are two JITcompilers C1 and C2, but there are 5 tiers, all the details in the entire glory are here in the comments.

These flags used to control how C1 and C2 would act - disable or not; this is now controlled by two other flags : XX:-TieredCompilation -XX:TieredStopAtLevel=1



