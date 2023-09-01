# otel

<https://opentelemetry.io/docs/instrumentation/java/>

```sh
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```

(gradle build)

```sh
source activate_otel.sh
# source otel_logging.sh # default otlp
java -jar ../build/libs/glosa-0.0.1-SNAPSHOT.jar
```
