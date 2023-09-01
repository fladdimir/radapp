# notes

## otel

```sh
pip install -r requirements.txt
opentelemetry-bootstrap -a install
```

```sh
OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317" \
OTEL_EXPORTER_OTLP_PROTOCOL=grpc \
OTEL_EXPORTER_OTLP_INSECURE=true \
OTEL_SERVICE_NAME=tld_forecast \
opentelemetry-instrument \
    --traces_exporter otlp \
    --metrics_exporter none \
    --logs_exporter none \
    python app.py
```
