[![Build Status](https://travis-ci.org/totomz/chabotto.svg?branch=master)](https://travis-ci.org/totomz/chabotto)


[Chabotto](https://en.wikipedia.org/wiki/John_Cabot) is a simple and light "framework" to easily implement service discovery in Java.

**NEW VERSION** This version requires SkyDNS and etcd; this is nothing more than a test now, and will be merged in porketta.


Info:

Chabotto è una API per interagire con etcd e skydns. 

Per usarla devi
* includerla nel classpath
* avviare la tua app con `-Dsun.net.spi.nameservice.nameservers=127.0.0.1 -Dsun.net.spi.nameservice.provider.1=dns,dnsjava`

Se i flag non sono presenti, chabotto non parte

