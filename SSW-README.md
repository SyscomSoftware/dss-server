## Konfigurace projektu DSS

Projekt DSS ( https://ec.europa.eu/digital-building-blocks/wikis/display/DIGITAL/eSignature ) pro použití v prostředí Syscom softwware 


# Změny oproti původní verzi projektu


* Z dockerfile byly odstraněny všechny konfigurace kromě dss-demo-webapp. 
* Odkazy z aplikace na dokumentaci jsou přesměrovány na originální projekt
* Nová třída cz.ssw.dss.SswTspSource (extends OnlineTSPSource implements TSPSource)
  * byly přidány dva konstruktory pro konfiguraci přístupu k TSP serverům POSTSIGNUM
  * použití-viz. "/src/main/resources/config/tsp-config.xml"

## Soubor konfigurace "tsp-config.xml"

#### Pro přihlášení uživatelským jménem a heslem

> __POZOR__ - adresu serveru (tspServer) je třeba zadat  **VČETNĚ KONCOVÉHO LOMÍTKA !**. Pokud tam není, dochází k cyklickému přesměrování - CIRCULAR REDIRECT

```xml
<bean id="tspSource" class="cz.ssw.dss.SswTspSource">
    <constructor-arg name="tspServer"  value="https://tsa.postsignum.cz:444/TSS/HttpTspServer/" />
    <constructor-arg name="username"  value="......." />
    <constructor-arg name="password"  value="......." />
</bean>
```

#### Přihlášení certifikátem
Je možné použít libovolný komerční certifikát vydaný Postsignum.cz svázaný s účtem pro odběr časových razítek

Klíč (certifikát) musí být uložen v *keystore* 

Pro vytvoření keystore lze použít [KeyStore Explorer](https://keystore-explorer.org/)

Výsledný soubor musí být v adresáři */src/main/resources/*

Konfigurace v souboru *tsp-config.xml* :

```xml
<bean id="tspSource" class="cz.ssw.dss.SswTspSource">
	<constructor-arg name="tspServer"  value="https://tsa.postsignum.cz/TSS/HttpTspServer/" />
	<constructor-arg name="sslKeystoreType"  value="PKCS12" />
	<constructor-arg name="sslKeystoreResource"  value="postsignum_client.p12" />
	<constructor-arg name="sslKeystorePassword"  value="AVISME" />
	<constructor-arg name="useKeyAsTrustMaterial"  value="false" />
</bean>
```



## Ostatní změny



# Spuštění v prostředí Dockeru

## Vytvoření docker image

V adresáři se souborem *dockerfile* spustit
```cmd
docker build -t dss-ssw .
```

## Spuštění serveru

```cmd
 docker run --name SSW-DSS-SERVER  --publish 8080:8080 dss-ssw
```

nebo v *detached* módu

```cmd
 docker run -d -p 8080:8080 dss-ssw
```



