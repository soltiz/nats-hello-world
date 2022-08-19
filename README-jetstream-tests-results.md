# Tests de performance via serialisation d'objet java


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



