# Hackathon 2025-10

## Nödvändig Programvara

### Java 21

Du behöver Java 17 eller nyare installerad på din dator.
Om du inte redan har det, kan du installera Azul Zulu, en OpenJDK variant av Java:

[Downloads](https://www.azul.com/downloads/?package=jdk#zulu)

Var säker på att välj att sätta både PATH och JAVA_HOME variablerna vid installationen.

För mer ingående instruktioner kan du titta på [Install Azul Zulu on Windows](https://docs.azul.com/core/install/windows#install-azul-zulu-with-msi-installer) dokumentationen.

### Maven

För att kunna fokusera på Java-koden istället för att fastna i konfiguration och beroende hantering kommer vi att använda ett verktyg som heter Maven.

Maven läser en lista på vilka bibliotek (beroenden) vi behöver och hämtar dem automatiskt.
Dessutom organiserar den vårt projekt på ett standartiserat sätt, vilket gör att det blir enklare att först strukturen och bygga applikationen med ett enda kommando.

För instruktioner om hur du installerar Maven, gå till [Intallation](https://maven.apache.org/install.html)

## Camel

För att kunna börja använda Camel och Spring Boot, behöver vi ladda ned ett antal beroenden.
Som tur är, hanterar Maven detta för oss.

Du kan see vilka beroenden vi har lagt till, i `dependencies` sektionen i *./pom.xml* filen

För att explecit ladda ner dom nu, kan du använda kommandot nedan, annars kommer det även att laddas ned automatiskt, när vi bygger vårt projekt:

```bash
mvn dependency:resolve
```

Aplikationen är förberädd med två färdiga rutter:

1. En rutt som flyttar en fil från *demo/int001-in/* till *demo/int001-out/*.  
   Den plockar dock bara upp filer slutar med _*.txt_
2. En rutt för att hantera fel. Denna kommer att logga felet och *backa ut* den falerade filen till *demo/int001-backout/*.

### Uppgift 1.1 - Flytta en fil

1. Öppna en powershell eller bash terminal.  
   Ställ dig i huvud katalogen för projectet och kör följande kommando:
   
   ```bash
   mvn clean spring-boot:run
   ```

2. Kopiera nu test.txt från projektets huvudkatalog eller valfri txt fil till underkatalogen *demo/int001-in/*.
   Efter ett kort ögonblick bör du se filens innehåll i loggutskriften i fönstret där applikationen körs.   
   Om du tittar i *demo/int001-out* kan du verifiera att filen har flyttats hit.  
3. Om du kopierar in samma fil till *demo/int001-in* katalogen, kommer du se att det skapas ett fel. Filen har nu flyttasts till *demo/int001-backout* katalogen istället.

**Genomgång**

Låt oss titta på hur denna rutt är uppsatt.  
Börja med att öppna *src/main/java/se/replyto/hackathon/routbuilders/__RoutBuilder01.java__*.

* I denna fil kan du hitta rutten som användes för att flytta filen.  
  Den börjar med `from("file:demo/int001-in?antInclude=*.txt")` på rad 38.
  
  Här säger vi att vi vill hämta en fil från *demo/int001-in* och att vi bara är intereserat av filer som slutar med *.txt*

* Sedan sätter vi ett routId. Detta är helt optionalt, men kan ses som *best practice*.
* Efter detta har vi bestämmt att fel ska hanteras av `deadLetterChannelBuilder` som är definierat ovanför rutten.
* För övrigt finns det en del loggning i rutten.
* Den sista biten som är viktigt för oss just nu är på rad 50: `.to("file:demo/int001-out?fileExist=Fail")`
  
  Här skickar vi filen till *demo/int001-out*.  
  `fileExist=Fail` är anledningen, varför vi inte lyckades skicka filen andra gången vi la den i in katalogen: en fil med samma namn fanns redan i destinations katalogen!

* Därför hanterades filen av våran felhantering som är definierat på rad 28-33.  
  Här sätts ett antal optioner som omförsök med mera. Vad den även gör är att peka ut en till rutt, `direct:error-handler` som detta meddelande ska skickas till.
* `direct` är en komponent för att kunna skicka meddelanden mellan rutter i Camel.
* Rutten `direct:error-handler` kan du hitta under våran huvud rutt på rad 56.  
  I den gör loggar felet som har inträffat samt lägger meddelandet i en backout katalog: `.to("{{int001.backout.uri}}")`
* Som du ser, ser strängen i denna `.to` helt annorlunda ut än den vi har använt oss av i huvudrutten.  
  Detta är för det vi ser här, bara är en reference till en konfiguration, som finns i en egen fil.
* Om du öppnar *src/main/resources/application.yaml* kan du hitta den riktiga strängen som används i rutten på rad 10: `file:demo/int001-backout`  
  Varje punkt i referencen vi använder i java filen, blir till en hyrarkisk nivå i yaml filen.  
* I Camels fil komponent är standard beteendet att filer skrivs över.  
  Då vi inte har sagt något annat, som vi gjorde i huvud rutten, kommer filer som redan finns att skrivas över.

*Det är vanligt att lägga konfigurationen som används av `.to` och `from` för att ansluta till externa system i en extern konfigurations källa.  
Denna källa kan vara en server, eller en eller flera filer, som tillhandahåller konfiguration för min applikation.  
Detta gör det enklare att ha olika konfigurationer i olika miljöer.  
Tänk till exempel olika fil-ytor eller olika databaser i test och produktions eller medans utveklar som bör användas.*

### Uppgift 1.2 - Ändra innehållet i meddelandet

Just nu flyttar vi bara filen så som den är. Camel har möjligheten att manipulera filens innehåll.

1. Lägg till följande kod på rad 48 i *src/main/java/se/replyto/hackathon/routbuilders/__RoutBuilder01.java__*:
   
   ```java
   .setBody().simple("${bodyAs(String)} File changed by Camel Integration.")
   ```
   
   Så att rutten ser ut såhär:
   
   ```java
   // Main route ---------------------------------------------------------
   from("file:demo/int001-in?antInclude=*.txt") // look for files that end with .txt in demo/int001-in folder
     .routeId("int001-exercise-main-route")   // routId is optional, but it is good practice to set one for every route
     .errorHandler(deadLetterChannelBuilder)  // How do we handle errors in this route
   
     // File received from inbound endpoint
     .log(LoggingLevel.INFO, loggerId, "Incoming file headers: ${headers}")
     .log(LoggingLevel.INFO, loggerId, """
       Incoming file body:
       ${body}
       """)
   
     .setBody().simple("${bodyAs(String)} File changed by Camel Integration.")
   
     // Send file to outbound endpoint
     .to("file:demo/int001-out?fileExist=Fail") // put file into demo/int001-out, if the file already exists, fail.
     .log(LoggingLevel.INFO, loggerId, "Outgoing file headers: ${headers}")
     .log(LoggingLevel.INFO, loggerId, "Sent file body: " + System.lineSeparator() + "${bodyAs(String)}");
   ```
   
   Spara filen

2. Stoppa applicationen i terminalen genom att använda `Ctrl` + `c` och starta om den med:
   
   ```bash
   mvn clean spring-boot:run
   ```

3. Rensa bort alla filer från *demo/int001-out*.
4. Lägg en ny txt fil i *demo/int001-in* katalogen
5. Titta på filen som har lagts i *demo/int001-out* katalogen.

**Genomgång**

* I denna övning har vi lagt till en sträng i filens innehåll.  
  För att verkställa detta har vi använd oss av `setBody` methoden.  
  Vi skulle ha kunnat lagt in en sträng direkt som argument i denna method (`.setBody("Detta är min nya body")`).
* Istället har vi använd `.simple("${bodyAs(String)} File changed by Camel Integration.")` efter `setBody()`.
* Det vi säger i `"${bodyAs(String)} File changed by Camel Integration."` är att vi vill ha en `String` som börjar med meddelandets body.
  
  Då body:n kommer från en fil, är den av typen `GenericFileStream`.  
  Därför ber vi Simple att konvertera den till en vanlig `String`.

* Sedan har vi lagt till en egen text efter body:n.
  
*[Simple](https://camel.apache.org/components/4.14.x/languages/simple-language.html) är ett eget språk som kan användas i Camel för att hämta, sätta och evaluera en mängd med olika värden.  
Enligt Camels dokumentation:*

> The Simple Expression Language was a really simple language when it was created, but has since grown more powerful.

*Vi kommer att se fler exempel på hur __simple__ språket kan användas vid senare uppgifter.*

### Uppgift 1.3 - Parsa SCV

Vi har sett att vi kan ändra innehållet i meddelandet som körs genom rutten.

Det vi ska göra nu är att parsa en CSV fil.

1. Först behöver vi definiera en class som beskriver datat vi vill omvandla:

   Skapa en ny folder i *src/main/java/se/replyto/hackathon* med namnet *__data___*.
   Sedan skapa en ny fil med namnet *__User.java___* i *__data___* katalogen.
2. Kopiera in följande kod i *src/main/java/se/replyto/hackathon/data/__User.java__*:

   ```java
   package se.replyto.hackathon.data;
   
   import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
   import org.apache.camel.dataformat.bindy.annotation.DataField;
   
   
   @CsvRecord(separator = ",", skipFirstLine = true, generateHeaderColumns = true) // Defines a CSV record and skips the header row
   public class User {
   
       @DataField(pos = 1)
       private int userId;
   
       @DataField(pos = 2)
       private String firstName;
   
       @DataField(pos = 3)
       private String lastName;
   
       // Bindy requires standard getters and setters to populate the object
       public int getUserId() { return userId; }
       public void setUserId(int userId) { this.userId = userId; }
       public String getFirstName() { return firstName; }
       public void setFirstName(String firstName) { this.firstName = firstName.trim(); }
       public String getLastName() { return lastName; }
       public void setLastName(String lastName) { this.lastName = lastName.trim(); }
   
       @Override
       public String toString() {
           return "User{" + "userId=" + userId + ", firstName='" + firstName + '\'' + ", lastName='" + lastName + '\'' + '}';
       }
   }
   ```

3. Sedan behöver vi lägga in en Bindy data definition ovanför våran main rutt i *src/main/java/se/replyto/hackathon/routbuilders/__RoutBuilder01.java__*:
   
   ```java
   DataFormat bindy = new BindyCsvDataFormat(User.class);
   ```

4. För att kunna börja använda detta, behöver vi dessutom lägga till följande importer:
   
   ```java
   import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
   import org.apache.camel.spi.DataFormat;
   import se.replyto.hackathon.data.User;
   ```

5. Nu är allt uppsatt för att kunna börja använda våran nya Bindy definition.

   Byt ut raden med `.setBody().simple("${bodyAs(String)} File changed by Camel Integration.")` mot föjande kod rader:

   ```java
   .unmarshal(bindy)
   .log("Body Type: ${body.class.name}, Body: ${body}")
   .marshal(bindy)
   ```

6. Innan vi kan köra detta behöver vi göra ett sista tillägg: På `from` raden i våran main rutt behöver vi lägga till `*.csv` så att även CSV filer plåckas upp.

   ```java
   from("file:demo/int001-in?antInclude=*.txt,*.csv")
   ```

7. Med alla våra ändringar och tilllägg bör *src/main/java/se/replyto/hackathon/routbuilders/**RoutBuilder01.java*** nu se ut ungefär såhär:
   
   ```java
   package se.replyto.hackathon.routbuilders;
   
   import org.apache.camel.CamelContext;
   import org.apache.camel.LoggingLevel;
   import org.apache.camel.builder.RouteBuilder;
   import org.apache.camel.model.errorhandler.DefaultErrorHandlerDefinition;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.core.env.Environment;
   import org.springframework.stereotype.Component;
   import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
   import org.apache.camel.spi.DataFormat;
   import se.replyto.hackathon.data.User;
   
   @Component
   public class RoutBuilder01 extends RouteBuilder {
   
       final String loggerId = getClass().getName();
   
       @Autowired
       Environment env;
   
       @Autowired
       CamelContext context;
   
       @Override
       public void configure(){
           context.setMessageHistory(true);
           context.setSourceLocationEnabled(true);
   
           DataFormat bindy = new BindyCsvDataFormat(User.class);
   
           final DefaultErrorHandlerDefinition deadLetterChannelBuilder =
                   deadLetterChannel("direct:error-handler")
                   .retryAttemptedLogLevel(LoggingLevel.WARN)
                   .useOriginalMessage()
                   .maximumRedeliveries(1)
                   .redeliveryDelay(1000);
   
           errorHandler(noErrorHandler());
   
           // Main route ---------------------------------------------------------
           from("file:demo/int001-in?antInclude=*.txt,*.csv")
               .routeId("int001-exercise-main-route")
               .errorHandler(deadLetterChannelBuilder)
   
               // File received from inbound endpoint
               .log(LoggingLevel.INFO, loggerId, "Incoming file headers: ${headers}")
               .log(LoggingLevel.INFO, loggerId, """
                 Incoming file body:
                 ${body}
                 """)
   
               .unmarshal(bindy)
               .log("Body Type: ${body.class.name}, Body: ${body}")
               .marshal(bindy)
   
               // Send file to outbound endpoint
               .to("file:demo/int001-out?fileExist=Fail") // put file into demo/int001-out, if the file already exists, fail.
               .log(LoggingLevel.INFO, loggerId, "Outgoing file headers: ${headers}")
               .log(LoggingLevel.INFO, loggerId, "Sent file body: " + System.lineSeparator() + "${bodyAs(String)}");
   
   
           // Error handling route -----------------------------------------------
           from("direct:error-handler")
               .routeId("error-handler-route")
               .log(LoggingLevel.WARN, "Moving ${header.CamelFileName} to backout folder {{int001.backout.uri}}...")
               .to("{{int001.backout.uri}}")
               .log(LoggingLevel.WARN, "Moved ${header.CamelFileName} to backout folder {{int001.backout.uri}}")
               .log(LoggingLevel.ERROR, """
   
                   E R R O R   R E P O R T :
                   ---------------------------------------------------------------------------------------------------------------------------------------
   
                   Failure Route ID: ${exchangeProperty.CamelFailureRouteId}
                   Failure Endpoint: ${exchangeProperty.CamelFailureEndpoint}
   
                   Exception Type:   ${exception.class.name}
                   Exception Message: ${exception.message}
   
                   Stacktrace: ${exception.stacktrace}
                   ${messageHistory}
                   """);
       }
   
   }
   ```

7. Nu är vi redå att testa våra ändringar. Stoppa och starta om applikationen.

   Kopiera **test.csv** filen från projektets rutt till *demo/int001-in* katalogen.

**Genomgång**

* Vi har skapat en nu klass för atthålla våran Bindy konfiguration.
  I denna har vi med hjälp av annotationer konfigurerat hur Bindy ska mappa data till denna klass.
* Sedan har vi lagt upp ett nytt data format som använder våran User klass.
* Vi parsar inkommande CSV datat med unmarhal
* Loggar ut både body type och själva body:n.
  I denna log kunde vi se att meddelandet var en ArrayList av User objekt.
* Sedan parsar vi vårt data tillbacka till en CSV sträng igen.

### Uppgift 1.4 - Omvandla CSV till JSON data

Att parsa CSV från och till CSV data är givetvis inte särskild menigsfullt. Därav ska vi prova omvandla det till något annat: JSON data.

Vi kan använda samma User.java klass för detta. Men vi behöver sätta upp ett nytt data format.

1. Lägg till följande kod efter våran bindy definition i *RoutBuilder01.java*:

   ```java
   JacksonDataFormat jsonDataFormat = new JacksonDataFormat(User[].class);
   jsonDataFormat.setEnableFeatures("ACCEPT_SINGLE_VALUE_AS_ARRAY");
   ```

2. Ny behöver vi byta ut `.marhal(bindy)` mot följande kod:
   
   ```java
   .marshal().json(JsonLibrary.Jackson)
   .setHeader(Exchange.FILE_NAME, simple("${file:name.noext}.json"))
   ```

3. Innan vi kan använda våra ändringar behöver vi även importera dem in i klassen:
   
   ```java
   import org.apache.camel.component.jackson.JacksonDataFormat;
   import org.apache.camel.model.dataformat.JsonLibrary;
   import org.apache.camel.Exchange;
   ```

4. Med alla våra ändringar och tilllägg bör *src/main/java/se/replyto/hackathon/routbuilders/**RoutBuilder01.java*** nu se ut ungefär såhär:
   
   ```java
   package se.replyto.hackathon.routbuilders;
   
   import org.apache.camel.CamelContext;
   import org.apache.camel.LoggingLevel;
   import org.apache.camel.builder.RouteBuilder;
   import org.apache.camel.model.errorhandler.DefaultErrorHandlerDefinition;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.core.env.Environment;
   import org.springframework.stereotype.Component;
   import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
   import org.apache.camel.spi.DataFormat;
   import se.replyto.hackathon.data.User;
   import org.apache.camel.component.jackson.JacksonDataFormat;
   import org.apache.camel.model.dataformat.JsonLibrary;
   import org.apache.camel.Exchange;
   
   @Component
   public class RoutBuilder01 extends RouteBuilder {
   
       final String loggerId = getClass().getName();
   
       @Autowired
       Environment env;
   
       @Autowired
       CamelContext context;
   
       @Override
       public void configure(){
           context.setMessageHistory(true);
           context.setSourceLocationEnabled(true);
   
           DataFormat bindy = new BindyCsvDataFormat(User.class);
           JacksonDataFormat jsonDataFormat = new JacksonDataFormat(User[].class);
           jsonDataFormat.setEnableFeatures("ACCEPT_SINGLE_VALUE_AS_ARRAY");
   
           final DefaultErrorHandlerDefinition deadLetterChannelBuilder =
                   deadLetterChannel("direct:error-handler")
                   .retryAttemptedLogLevel(LoggingLevel.WARN)
                   .useOriginalMessage()
                   .maximumRedeliveries(1)
                   .redeliveryDelay(1000);
   
           errorHandler(noErrorHandler());
   
           // Main route ---------------------------------------------------------
           from("file:demo/int001-in?antInclude=*.txt,*.csv")
               .routeId("int001-exercise-main-route")
               .errorHandler(deadLetterChannelBuilder)
   
               // File received from inbound endpoint
               .log(LoggingLevel.INFO, loggerId, "Incoming file headers: ${headers}")
               .log(LoggingLevel.INFO, loggerId, """
                 Incoming file body:
                 ${body}
                 """)
   
               .unmarshal(bindy)
               .log("Body Type: ${body.class.name}, Body: ${body}")
               .marshal().json(JsonLibrary.Jackson)
               .setHeader(Exchange.FILE_NAME, simple("${file:name.noext}.json"))
   
               // Send file to outbound endpoint
               .to("file:demo/int001-out?fileExist=Fail") // put file into demo/int001-out, if the file already exists, fail.
               .log(LoggingLevel.INFO, loggerId, "Outgoing file headers: ${headers}")
               .log(LoggingLevel.INFO, loggerId, "Sent file body: " + System.lineSeparator() + "${bodyAs(String)}");
   
   
           // Error handling route -----------------------------------------------
           from("direct:error-handler")
               .routeId("error-handler-route")
               .log(LoggingLevel.WARN, "Moving ${header.CamelFileName} to backout folder {{int001.backout.uri}}...")
               .to("{{int001.backout.uri}}")
               .log(LoggingLevel.WARN, "Moved ${header.CamelFileName} to backout folder {{int001.backout.uri}}")
               .log(LoggingLevel.ERROR, """
   
                   E R R O R   R E P O R T :
                   ---------------------------------------------------------------------------------------------------------------------------------------
   
                   Failure Route ID: ${exchangeProperty.CamelFailureRouteId}
                   Failure Endpoint: ${exchangeProperty.CamelFailureEndpoint}
   
                   Exception Type:   ${exception.class.name}
                   Exception Message: ${exception.message}
   
                   Stacktrace: ${exception.stacktrace}
                   ${messageHistory}
                   """);
       }
   
   }
   ```

5. Rensa bort alla filer från *demo/int001-out*.
6. Kopiera **test.csv** filen från projektets rutt till *demo/int001-in* katalogen.
7. Kontrollera filen i *demo/int001-out*.

**Genomgång**

* Vi har lagt till ett nytt data format som använder Jackson bibliotheket för att hantera arrayer av User.class data.
* Sedan har vi även lagt till konfiguration på detta nya dataformat för att kunna hantera enskilda objekt, som om det våre en array, ungefär på samma sätt som *Varargs* hanteras i Java
* Vi ändrade marhsal att använda JSON data formatet
* Till slut ändrade vi filnamnet så att det slutade med .json istället.

### Uppgift 1.5 - Omvandla JSON till CSV data

Vi kan även omvandla JSON filer till CSV och vice versa.

1. byt ut följande kod:
   
   ```java
   .unmarshal(bindy)
   .log("Body Type: ${body.class.name}, Body: ${body}")
   .marshal().json(JsonLibrary.Jackson)
   .setHeader(Exchange.FILE_NAME, simple("${file:name.noext}.json"))
   ```
   
   Mot följande kod rader:
   
   ```java
   .choice()    
   .when(simple("${header.CamelFileName} ends with '.csv'"))
       .unmarshal(bindy)
       .marshal().json(JsonLibrary.Jackson)
       .setHeader(Exchange.FILE_NAME, simple("${file:name.noext}.json"))
   .when(simple("${header.CamelFileName} ends with '.json'"))
       .unmarshal(jsonDataFormat)
       .marshal(bindy)
       .setHeader(Exchange.FILE_NAME, simple("${file:name.noext}.csv"))
   .end()
   ```

2. Givetvis behöver vi även lägga till `*.json` i `from` för våran main rutt:
   
   ```java
   from("file:demo/int001-in?antInclude=*.txt,*.csv,*.json")
   ```

3. Med alla våra ändringar och tilllägg bör *src/main/java/se/replyto/hackathon/routbuilders/**RoutBuilder01.java*** nu se ut ungefär såhär:
   
   ```java
   package se.replyto.hackathon.routbuilders;
   
   import org.apache.camel.CamelContext;
   import org.apache.camel.LoggingLevel;
   import org.apache.camel.builder.RouteBuilder;
   import org.apache.camel.model.errorhandler.DefaultErrorHandlerDefinition;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.core.env.Environment;
   import org.springframework.stereotype.Component;
   import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
   import org.apache.camel.spi.DataFormat;
   import se.replyto.hackathon.data.User;
   import org.apache.camel.component.jackson.JacksonDataFormat;
   import org.apache.camel.model.dataformat.JsonLibrary;
   import org.apache.camel.Exchange;
   
   @Component
   public class RoutBuilder01 extends RouteBuilder {
   
       final String loggerId = getClass().getName();
   
       @Autowired
       Environment env;
   
       @Autowired
       CamelContext context;
   
       @Override
       public void configure(){
           context.setMessageHistory(true);
           context.setSourceLocationEnabled(true);
   
           DataFormat bindy = new BindyCsvDataFormat(User.class);
           JacksonDataFormat jsonDataFormat = new JacksonDataFormat(User[].class);
           jsonDataFormat.setEnableFeatures("ACCEPT_SINGLE_VALUE_AS_ARRAY");
   
           final DefaultErrorHandlerDefinition deadLetterChannelBuilder =
                   deadLetterChannel("direct:error-handler")
                   .retryAttemptedLogLevel(LoggingLevel.WARN)
                   .useOriginalMessage()
                   .maximumRedeliveries(1)
                   .redeliveryDelay(1000);
   
           errorHandler(noErrorHandler());
   
           // Main route ---------------------------------------------------------
           from("file:demo/int001-in?antInclude=*.txt,*.csv,*.json")
               .routeId("int001-exercise-main-route")
               .errorHandler(deadLetterChannelBuilder)
   
               // File received from inbound endpoint
               .log(LoggingLevel.INFO, loggerId, "Incoming file headers: ${headers}")
               .log(LoggingLevel.INFO, loggerId, """
                 Incoming file body:
                 ${body}
                 """)
   
               .choice()    
               .when(simple("${header.CamelFileName} ends with '.csv'"))
                   .unmarshal(bindy)
                   .marshal().json(JsonLibrary.Jackson)
                   .setHeader(Exchange.FILE_NAME, simple("${file:name.noext}.json"))
               .when(simple("${header.CamelFileName} ends with '.json'"))
                   .unmarshal(jsonDataFormat)
                   .marshal(bindy)
                   .setHeader(Exchange.FILE_NAME, simple("${file:name.noext}.csv"))
               .end()
   
               // Send file to outbound endpoint
               .to("file:demo/int001-out?fileExist=Fail") // put file into demo/int001-out, if the file already exists, fail.
               .log(LoggingLevel.INFO, loggerId, "Outgoing file headers: ${headers}")
               .log(LoggingLevel.INFO, loggerId, "Sent file body: " + System.lineSeparator() + "${bodyAs(String)}");
   
   
           // Error handling route -----------------------------------------------
           from("direct:error-handler")
               .routeId("error-handler-route")
               .log(LoggingLevel.WARN, "Moving ${header.CamelFileName} to backout folder {{int001.backout.uri}}...")
               .to("{{int001.backout.uri}}")
               .log(LoggingLevel.WARN, "Moved ${header.CamelFileName} to backout folder {{int001.backout.uri}}")
               .log(LoggingLevel.ERROR, """
   
                   E R R O R   R E P O R T :
                   ---------------------------------------------------------------------------------------------------------------------------------------
   
                   Failure Route ID: ${exchangeProperty.CamelFailureRouteId}
                   Failure Endpoint: ${exchangeProperty.CamelFailureEndpoint}
   
                   Exception Type:   ${exception.class.name}
                   Exception Message: ${exception.message}
   
                   Stacktrace: ${exception.stacktrace}
                   ${messageHistory}
                   """);
       }
   
   }
   ```

4. Kopiera ***test.json*** filen från *demo/int001-out* katalogen in till *demo/int001-in* katalogen.
5. Kontrollera nya CSV filen i *demo/int001-out*.

**Genomgång**

* I denna övning har använd oss av en choice tillsammans med ett Simple Language uttryck för att ändra beteended beroende på file endelsen på inkommande filen.
  * När det är en CSV fil gör vi samma som förrut och omvandlar den till JSON data.
  * När det är JSON data, skapar vi en CSV fil från denna.
  * Om det är en TXT fil, flyttar vi filen som den är, utan att ändra innehållet.

### Uppgift 2.1 - Skapa en REST tjänst

Nu ska vi ta nästa steg och skapa en REST tjänst som kan hantera användar data.
För data formatet, kommer vi att använda samma User.class som vi har använd i föregående övningar.

1. Skapa en ny fil **RoutBuilder02.java** i *src/main/java/se/replyto/hackathon/routbuilders/* katalogen.
   
   ```java
   package se.replyto.hackathon.routbuilders;
   
   import org.apache.camel.CamelContext;
   import org.apache.camel.Exchange;
   import org.apache.camel.LoggingLevel;
   import org.apache.camel.builder.RouteBuilder;
   import org.apache.camel.model.rest.RestBindingMode;
   import org.apache.camel.model.errorhandler.DefaultErrorHandlerDefinition;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.core.env.Environment;
   import org.springframework.stereotype.Component;
   import se.replyto.hackathon.data.User;
   import java.io.InputStream;
   import java.util.Map;
   import java.util.Scanner;
   import java.util.concurrent.ConcurrentHashMap;
   
   @Component
   public class RoutBuilder02 extends RouteBuilder {
   
       final String loggerId = getClass().getName();
   
       @Autowired
       Environment env;
   
       @Autowired
       CamelContext context;
   
       private final Map<Integer, User> userMap = new ConcurrentHashMap<>();
   
       @Override
       public void configure(){
   
           final DefaultErrorHandlerDefinition deadLetterChannelBuilder =
                   deadLetterChannel("direct:rest-error-handler")
                   .retryAttemptedLogLevel(LoggingLevel.WARN)
                   .useOriginalMessage()
                   .maximumRedeliveries(1)
                   .redeliveryDelay(1000);
   
           errorHandler(deadLetterChannelBuilder);
   
           // REST DSL configuration
           restConfiguration()
               .component("servlet")
               .bindingMode(RestBindingMode.json)
               .enableCORS(true);
   
           // REST route for managing users
           rest("/users")
               .post().type(User.class)
                   .to("direct:new-user")
               .get()
                   .to("direct:get-all-users")
   
               .get("/{id}")
                   .to("direct:get-user")
               .put("/{id}").type(User.class)
                   .to("direct:update-user")
               .delete("/{id}")
                   .to("direct:delete-user");
   
   
           from("direct:rest-error-handler")
               .routeId("rest-error-handler-route")
               .log(LoggingLevel.ERROR, """
   
                   E R R O R   R E P O R T :
                   ---------------------------------------------------------------------------------------------------------------------------------------
   
                   Failure Route ID: ${exchangeProperty.CamelFailureRouteId}
                   Failure Endpoint: ${exchangeProperty.CamelFailureEndpoint}
   
                   Exception Type:   ${exception.class.name}
                   Exception Message: ${exception.message}
   
                   Stacktrace: ${exception.stacktrace}
                   ${messageHistory}
                   """);
       }
   }
   ```

2. Denna fil innehåller del kod vi redan känner igen:

   * Vi har en data format definition för JSON data
   * Vi har en `DefaultErrorHandlerDefinition` med en tillhörande rutt, som denna gång bara loggar fel och inte skriver till någon fil.
   * Efter detta sätter vi ett antal konfigurationer på Camels rest komponent.
   * Slutligen har vi definitionen av våran API. Än så länge pekar den till sub rutter som inte finns än. Det är dessa som hanterar all logik för våran REST API och som vi ska bygga.
   * Vi har även en instans variabel för en map som kommer att användas för att spara användare som API:et hanterar. Dessa persisteras med andra ord bara i minnet, inte på disk. Om applikationen startas om, rensas denna map.
3. Lägg till följande kod för att hämta alla användare:
   
   ```java
   from("direct:get-all-users")
       .routeId("int002:users:get-all-users")
       .log("Retrieving all users")
       .process(exchange -> {
           exchange.getIn().setBody(userMap.values());
       });
   ```

   Här gör vi inget annat än att lägga innehållet av `userMap` i meddelandet. Detta används sedan som `Callback` för anropet till from, som i sin tur används som `Callback` av rest anropet och retuneras till klienten.
4. Låt oss nu lägga till rutten för att skapa en nu användare:
   
   ```java
   from("direct:new-user")
       .routeId("int002:users:create-user")
       .log("Received new user: ${body}")
       .process(exchange -> {
           User user = exchange.getIn().getBody(User.class);
           if (user.getUserId() == 0) {
               int count = user.getUserId() +1;
               while (userMap.containsKey(count))
                   count++;
               user.setUserId(count);
           }      
           userMap.put(user.getUserId(), user);
           exchange.getIn().setBody(user);
           exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201));
       })
       .log("User stored: ${body}");
   ```
   
   Här använder vi oss av en processor för att kunna bearbeta meddelandet efter våra behov.
   
   * Vi utgåt från att vi får in User data
   * Sedan kontrollerar vi userId. Om det är noll, så skapar vi ett nytt genom att ta det första lediga, som inte finns i `userMap` än.
   * Efter det sparar vi användaren till `userMap` och sätter användaren med det eventuellt nya Id:et till meddelande body:n
   * Till slut sätter vi rätt HTTP status kod som ska retuneras till klienten.
5. Nästa rutt vi behöver lägga till är den för att hämta en enskild användare:
   
   ```java
   from("direct:get-user")
       .routeId("int002:users:get-user")
       .log("Retrieving user with id: ${header.id}")
       .process(exchange -> {
           int id = Integer.parseInt(exchange.getIn().getHeader("id", String.class));
           User user = userMap.get(id);
           if (user != null) {
               exchange.getIn().setBody(user);
           } else {
               exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404));
               exchange.getIn().setBody("No users found.");
           }
       });
   ```
   
   Om vi inte specificerar nogån `HTTP_RESPONSE_CODE` och allt gick bra, kommer Camel att retunera en lämplig kod.
6. Nästa rutt vi lägger till är den för att ändra en befintlig användare:
   
   ```java
   from("direct:update-user")
       .routeId("int002:users:update-user")
       .log("Updating user with id: ${header.id}")
       .process(exchange -> {
           int id = Integer.parseInt(exchange.getIn().getHeader("id", String.class));
           User user = exchange.getIn().getBody(User.class);
           if( userMap.containsKey(id)) {
               user.setUserId(id); // Make sure the user's ID is set from the URL
               userMap.put(id, user);
               exchange.getIn().setBody(user);
           } else {
               exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404));
               exchange.getIn().setBody("User with id " + id + " not found.");
           }
       });
   ```
   
   Denna liknar i stort sätt det vi redan har sett i det föregående rutterna.
7. Till sist lägger vi till rutten för att radera en användare:
   
   ```java
   from("direct:delete-user")
       .routeId("int002:users:delete-user")
       .log("Deleting user with id: ${header.id}")
       .process(exchange -> {
           int id = Integer.parseInt(exchange.getIn().getHeader("id", String.class));
           if( userMap.containsKey(id)) {
               System.out.println("deleting");
               userMap.remove(id);
               exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(204));
               exchange.getIn().setBody("User with id " + id + " deleted successfully.");
           } else {
               exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404));
               exchange.getIn().setBody("User with id " + id + " not found.");
           }
       })
       .log(LoggingLevel.INFO, loggerId, "headers: ${headers}");
   ```

8. Den färdiga klassen bör se ut så här:
   *src/main/java/se/replyto/hackathon/routbuilders/__RoutBuilder02.java__*
   
   ```java
   package se.replyto.hackathon.routbuilders;
   
   import org.apache.camel.CamelContext;
   import org.apache.camel.Exchange;
   import org.apache.camel.LoggingLevel;
   import org.apache.camel.builder.RouteBuilder;
   import org.apache.camel.model.rest.RestBindingMode;
   import org.apache.camel.model.errorhandler.DefaultErrorHandlerDefinition;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.core.env.Environment;
   import org.springframework.stereotype.Component;
   import se.replyto.hackathon.data.User;
   import java.io.InputStream;
   import java.util.Map;
   import java.util.Scanner;
   import java.util.concurrent.ConcurrentHashMap;
   
   @Component
   public class RoutBuilder02 extends RouteBuilder {
   
       final String loggerId = getClass().getName();
   
       @Autowired
       Environment env;
   
       @Autowired
       CamelContext context;
   
       private final Map<Integer, User> userMap = new ConcurrentHashMap<>();
   
       @Override
       public void configure(){
   
           final DefaultErrorHandlerDefinition deadLetterChannelBuilder =
                   deadLetterChannel("direct:rest-error-handler")
                   .retryAttemptedLogLevel(LoggingLevel.WARN)
                   .useOriginalMessage()
                   .maximumRedeliveries(1)
                   .redeliveryDelay(1000);
   
           errorHandler(deadLetterChannelBuilder);
   
           // REST DSL configuration
           restConfiguration()
               .component("servlet")
               .bindingMode(RestBindingMode.json)
               .enableCORS(true);
   
           // REST route for managing users
           rest("/users")
                   .get()
                   .to("direct:get-all-users")
               .post().type(User.class)
                   .to("direct:new-user")
   
               .get("/{id}")
                   .to("direct:get-user")
               .put("/{id}").type(User.class)
                   .to("direct:update-user")
               .delete("/{id}")
                   .to("direct:delete-user");
   
           from("direct:get-all-users")
               .routeId("int002:users:get-all-users")
               .log("Retrieving all users")
               .process(exchange -> {
                   exchange.getIn().setBody(userMap.values());
               });
   
           from("direct:new-user")
               .routeId("int002:users:create-user")
               .log("Received new user: ${body}")
               .process(exchange -> {
                   User user = exchange.getIn().getBody(User.class);
                   if (user.getUserId() == 0) {
                       int count = user.getUserId() +1;
                       while (userMap.containsKey(count))
                           count++;
                       user.setUserId(count);    
                   }    
                   userMap.put(user.getUserId(), user);
                   exchange.getIn().setBody(user);
                   exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201));
               })
               .log("User stored: ${body}");
   
           from("direct:get-user")
               .routeId("int002:users:get-user")
               .log("Retrieving user with id: ${header.id}")
               .process(exchange -> {
                   int id = Integer.parseInt(exchange.getIn().getHeader("id", String.class));
                   User user = userMap.get(id);
                   if (user != null) {
                       exchange.getIn().setBody(user);
                   } else {
                       exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404));
                       exchange.getIn().setBody("No users found.");
                   }
               });
           from("direct:update-user")
               .routeId("int002:users:update-user")
               .log("Updating user with id: ${header.id}")
               .process(exchange -> {
                   int id = Integer.parseInt(exchange.getIn().getHeader("id", String.class));
                   User user = exchange.getIn().getBody(User.class);
                   if( userMap.containsKey(id)) {
                       user.setUserId(id); // Make sure the user's ID is set from the URL
                       userMap.put(id, user);
                       exchange.getIn().setBody(user);
                   } else {
                       exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404));
                       exchange.getIn().setBody("User with id " + id + " not found.");
                   }
               });
   
           from("direct:delete-user")
               .routeId("int002:users:delete-user")
               .log("Deleting user with id: ${header.id}")
               .process(exchange -> {
                   int id = Integer.parseInt(exchange.getIn().getHeader("id", String.class));
                   if( userMap.containsKey(id)) {
                       System.out.println("deleting");
                       userMap.remove(id);
                       exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(204));
                       exchange.getIn().setBody("User with id " + id + " deleted successfully.");
                   } else {
                       exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404));
                       exchange.getIn().setBody("User with id " + id + " not found.");
                   }
               })
               .log(LoggingLevel.INFO, loggerId, "headers: ${headers}");
   
           from("direct:rest-error-handler")
               .routeId("rest-error-handler-route")
               .log(LoggingLevel.ERROR, """
   
                   E R R O R   R E P O R T :
                   ---------------------------------------------------------------------------------------------------------------------------------------
   
                   Failure Route ID: ${exchangeProperty.CamelFailureRouteId}
                   Failure Endpoint: ${exchangeProperty.CamelFailureEndpoint}
   
                   Exception Type:   ${exception.class.name}
                   Exception Message: ${exception.message}
   
                   Stacktrace: ${exception.stacktrace}
                   ${messageHistory}
                   """);
       }
   }
   ```

9. Nu kan vi starta om applicationen om och testa API:et.
10. Du kan använda curl eller postman för att anropa det på `127.0.0.1:8080/camel/users/`

### Uppgift 2.2 - Användar gränssnitt

Postman ich curl fungerar, men om du tittar i *src/main/resources/static* katalogen, så hittar du en html fil, som är förbered att agera som användar gränssnitt till vårt nya API.

För att tillhandahålla det, kan vi lägga till en egen rutt.

1. Först behöver vi skapa rest definitionen.
   
   ```java
   rest("/")
       .get()
       .to("direct:get-website");
   ```

2. Och precis som vi gjorde med API:et, behöver vi skapa den korresponderande rutten:
   
   ```java
   from("direct:get-website")
       .routeId("int002:user-ui")
       .setHeader(Exchange.CONTENT_TYPE, constant("text/html"))
       .process(exchange -> {
           InputStream is = exchange.getContext().getClassResolver().loadResourceAsStream("static/users.html");
           try(Scanner sc = new Scanner(is)) {
               String html = sc.useDelimiter("\\A").next();
               exchange.getIn().setBody(html);
           }
       });
   ```

   I denna läser vi in html dokumentet som en ström.
   Avskiljaren `\\A` representerar början av strömen. Då vi använder denna, säger vi att vi i praktiken vill läsa in hela strömmen.
3. Nu ser *src/main/java/se/replyto/hackathon/routbuilders/__RoutBuilder02.java__* ut så här:
   
   ```java
   package se.replyto.hackathon.routbuilders;
   
   import org.apache.camel.CamelContext;
   import org.apache.camel.Exchange;
   import org.apache.camel.LoggingLevel;
   import org.apache.camel.builder.RouteBuilder;
   import org.apache.camel.model.rest.RestBindingMode;
   import org.apache.camel.model.errorhandler.DefaultErrorHandlerDefinition;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.core.env.Environment;
   import org.springframework.stereotype.Component;
   import se.replyto.hackathon.data.User;
   import java.io.InputStream;
   import java.util.Map;
   import java.util.Scanner;
   import java.util.concurrent.ConcurrentHashMap;
   
   @Component
   public class RoutBuilder02 extends RouteBuilder {
   
       final String loggerId = getClass().getName();
   
       @Autowired
       Environment env;
   
       @Autowired
       CamelContext context;
   
       private final Map<Integer, User> userMap = new ConcurrentHashMap<>();
   
       @Override
       public void configure(){
   
           final DefaultErrorHandlerDefinition deadLetterChannelBuilder =
                   deadLetterChannel("direct:rest-error-handler")
                   .retryAttemptedLogLevel(LoggingLevel.WARN)
                   .useOriginalMessage()
                   .maximumRedeliveries(1)
                   .redeliveryDelay(1000);
   
           errorHandler(deadLetterChannelBuilder);
   
           // REST DSL configuration
           restConfiguration()
               .component("servlet")
               .bindingMode(RestBindingMode.json)
               .enableCORS(true);
   
           // REST route for managing users
           rest("/users")
                   .get()
                   .to("direct:get-all-users")
               .post().type(User.class)
                   .to("direct:new-user")
   
               .get("/{id}")
                   .to("direct:get-user")
               .put("/{id}").type(User.class)
                   .to("direct:update-user")
               .delete("/{id}")
                   .to("direct:delete-user");
           rest("/")
               .get()
               .to("direct:get-website");
   
           from("direct:get-website")
               .routeId("int002:user-ui")
               .setHeader(Exchange.CONTENT_TYPE, constant("text/html"))
               .process(exchange -> {
                   InputStream is = exchange.getContext().getClassResolver().loadResourceAsStream("static/users.html");
                   try(Scanner sc = new Scanner(is)) {
                       String html = sc.useDelimiter("\\A").next();
                       exchange.getIn().setBody(html);
                   }
               });
   
           from("direct:get-all-users")
               .routeId("int002:users:get-all-users")
               .log("Retrieving all users")
               .process(exchange -> {
                   exchange.getIn().setBody(userMap.values());
               });
   
           from("direct:new-user")
               .routeId("int002:users:create-user")
               .log("Received new user: ${body}")
               .process(exchange -> {
                   User user = exchange.getIn().getBody(User.class);
                   if (user.getUserId() == 0) {
                       int count = user.getUserId() +1;
                       while (userMap.containsKey(count))
                           count++;
                       user.setUserId(count);    
                   }    
                   userMap.put(user.getUserId(), user);
                   exchange.getIn().setBody(user);
                   exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201));
               })
               .log("User stored: ${body}");
   
           from("direct:get-user")
               .routeId("int002:users:get-user")
               .log("Retrieving user with id: ${header.id}")
               .process(exchange -> {
                   int id = Integer.parseInt(exchange.getIn().getHeader("id", String.class));
                   User user = userMap.get(id);
                   if (user != null) {
                       exchange.getIn().setBody(user);
                   } else {
                       exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404));
                       exchange.getIn().setBody("No users found.");
                   }
               });
           from("direct:update-user")
               .routeId("int002:users:update-user")
               .log("Updating user with id: ${header.id}")
               .process(exchange -> {
                   int id = Integer.parseInt(exchange.getIn().getHeader("id", String.class));
                   User user = exchange.getIn().getBody(User.class);
                   if( userMap.containsKey(id)) {
                       user.setUserId(id); // Make sure the user's ID is set from the URL
                       userMap.put(id, user);
                       exchange.getIn().setBody(user);
                   } else {
                       exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404));
                       exchange.getIn().setBody("User with id " + id + " not found.");
                   }
               });
   
           from("direct:delete-user")
               .routeId("int002:users:delete-user")
               .log("Deleting user with id: ${header.id}")
               .process(exchange -> {
                   int id = Integer.parseInt(exchange.getIn().getHeader("id", String.class));
                   if( userMap.containsKey(id)) {
                       System.out.println("deleting");
                       userMap.remove(id);
                       exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(204));
                       exchange.getIn().setBody("User with id " + id + " deleted successfully.");
                   } else {
                       exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404));
                       exchange.getIn().setBody("User with id " + id + " not found.");
                   }
               })
               .log(LoggingLevel.INFO, loggerId, "headers: ${headers}");
   
           from("direct:rest-error-handler")
               .routeId("rest-error-handler-route")
               .log(LoggingLevel.ERROR, """
   
                   E R R O R   R E P O R T :
                   ---------------------------------------------------------------------------------------------------------------------------------------
   
                   Failure Route ID: ${exchangeProperty.CamelFailureRouteId}
                   Failure Endpoint: ${exchangeProperty.CamelFailureEndpoint}
   
                   Exception Type:   ${exception.class.name}
                   Exception Message: ${exception.message}
   
                   Stacktrace: ${exception.stacktrace}
                   ${messageHistory}
                   """);
       }
   }
   ```

4. Starta om applikationen och öppna [User Management](http://localhost:8080/camel/) i en webläsare.

### Uppgift 2.3 - Skapa en integration till User API:et

Att lägga till nya användare via web gränssnittet eller curl fungerar. Men i det flesta fallen vill man kunna skicka data från ett system till ett annat. vi kan använda oss av en anpassat rutt i RoutBuilder01.java för att plåcka upp filer med användare och sedan skicka dessa automatiskt till vårat User Rest backend.

1. Lägg till följande rutt i *src/main/java/se/replyto/hackathon/routbuilders/__RoutBuilder01.java__*:
   
   ```java
   from("file:demo/int002-in")
       .routeId("int002-in:file_to_rest")
       .log("Processing file: ${header.CamelFileName}")
       .choice()
           .when(header(Exchange.FILE_NAME).endsWith(".csv"))
               .unmarshal(bindy)
           .when(header(Exchange.FILE_NAME).endsWith(".json"))
               .unmarshal(jsonDataFormat)
           .otherwise()
               .log("Unsupported file type: ${header.CamelFileName}")
               .stop()
       .end()
       .split(body())
           .marshal().json(JsonLibrary.Jackson)
           .setHeader(Exchange.HTTP_METHOD, constant("POST"))
           .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
           .toD("http://localhost:{{server.port}}/camel/users")
       .end();
   ```
   
   Det mesta ifrån denna rutt känner du någ redan igen.
   
   * Vi parsar datat beroende på om det handlar om en CSV eller JSON fil.
   * Då API:et bara kan hantera enskilda användare, styckar vi upp datat, omvandlar det till JSON strängar och skickar in var och en användare för sig till API:et.

2. *src/main/java/se/replyto/hackathon/routbuilders/__RoutBuilder01.java__* ser nu ut såhär:
   
   ```java
   package se.replyto.hackathon.routbuilders;
   
   import org.apache.camel.CamelContext;
   import org.apache.camel.LoggingLevel;
   import org.apache.camel.builder.RouteBuilder;
   import org.apache.camel.model.errorhandler.DefaultErrorHandlerDefinition;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.core.env.Environment;
   import org.springframework.stereotype.Component;
   import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
   import org.apache.camel.spi.DataFormat;
   import se.replyto.hackathon.data.User;
   import org.apache.camel.component.jackson.JacksonDataFormat;
   import org.apache.camel.model.dataformat.JsonLibrary;
   import org.apache.camel.Exchange;
   
   @Component
   public class RoutBuilder01 extends RouteBuilder {
   
   	final String loggerId = getClass().getName();
   	
   	@Autowired
   	Environment env;
   
   	@Autowired
   	CamelContext context;
   
   	@Override
   	public void configure(){
   		context.setMessageHistory(true);
   		context.setSourceLocationEnabled(true);
   		
   		DataFormat bindy = new BindyCsvDataFormat(User.class);
   		JacksonDataFormat jsonDataFormat = new JacksonDataFormat(User[].class);
   		jsonDataFormat.setEnableFeatures("ACCEPT_SINGLE_VALUE_AS_ARRAY");
   		
   		final DefaultErrorHandlerDefinition deadLetterChannelBuilder =
   				deadLetterChannel("direct:error-handler")
   				.retryAttemptedLogLevel(LoggingLevel.WARN)
   				.useOriginalMessage()
   				.maximumRedeliveries(1)
   				.redeliveryDelay(1000);
   		
   		errorHandler(noErrorHandler());
   	    
   		// Main route ---------------------------------------------------------
   		from("file:demo/int001-in?antInclude=*.txt,*.csv,*.json")
   			.routeId("int001-exercise-main-route")
   			.errorHandler(deadLetterChannelBuilder)
   
   			// File received from inbound endpoint
   			.log(LoggingLevel.INFO, loggerId, "Incoming file headers: ${headers}")
   			.log(LoggingLevel.INFO, loggerId, """
   			  Incoming file body:
   			  ${body}
   			  """)
   			
   			.choice()	
   			.when(simple("${header.CamelFileName} ends with '.csv'"))
   			    .unmarshal(bindy)
   				.marshal().json(JsonLibrary.Jackson)
   				.setHeader(Exchange.FILE_NAME, simple("${file:name.noext}.json"))
   			.when(simple("${header.CamelFileName} ends with '.json'"))
   				.unmarshal(jsonDataFormat)
   				.marshal(bindy)
   				.setHeader(Exchange.FILE_NAME, simple("${file:name.noext}.csv"))
   			.end()
   			
   			// Send file to outbound endpoint
   			.to("file:demo/int001-out?fileExist=Fail") // put file into demo/int001-out, if the file already exists, fail.
   			.log(LoggingLevel.INFO, loggerId, "Outgoing file headers: ${headers}")
   			.log(LoggingLevel.INFO, loggerId, "Sent file body: " + System.lineSeparator() + "${bodyAs(String)}");
   		
   		from("file:demo/int002-in")
   			.routeId("int002-in:file_to_rest")
   		    .log("Processing file: ${header.CamelFileName}")
   		    .choice()
   		        .when(header(Exchange.FILE_NAME).endsWith(".csv"))
   		            .unmarshal(bindy)
   		        .when(header(Exchange.FILE_NAME).endsWith(".json"))
   		            .unmarshal(jsonDataFormat)
   		        .otherwise()
   		            .log("Unsupported file type: ${header.CamelFileName}")
   		            .stop()
   		    .end()
   		    .split(body())
   		        .marshal().json(JsonLibrary.Jackson)
   		        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
   		        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
   		        .toD("http://localhost:{{server.port}}/camel/users")
   		    .end();
   
   		// Error handling route -----------------------------------------------
   		from("direct:error-handler")
   			.routeId("error-handler-route")
   			.log(LoggingLevel.WARN, "Moving ${header.CamelFileName} to backout folder {{int001.backout.uri}}...")
   			.to("{{int001.backout.uri}}")
   			.log(LoggingLevel.WARN, "Moved ${header.CamelFileName} to backout folder {{int001.backout.uri}}")
   			.log(LoggingLevel.ERROR, """
   									
   				E R R O R   R E P O R T :
   				---------------------------------------------------------------------------------------------------------------------------------------
   				
   				Failure Route ID: ${exchangeProperty.CamelFailureRouteId}
   				Failure Endpoint: ${exchangeProperty.CamelFailureEndpoint}
   
   				Exception Type:   ${exception.class.name}
   				Exception Message: ${exception.message}
   				
   				Stacktrace: ${exception.stacktrace}
   				${messageHistory}
   				""");
   	}
   
   }
   ```

2. Om du lägger någon av ***test.json*** eller ***test.csv*** i *demo/int002-in*, kan du se hur det hanteras av vårat backend. 
3. Om du laddar om [User Management](http://localhost:8080/camel/) kan du sedan se det nya användarna som systemet har lagt till.

