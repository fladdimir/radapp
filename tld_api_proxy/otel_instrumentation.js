import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';
import {
    OTLPTraceExporter,
} from "@opentelemetry/exporter-trace-otlp-grpc";
import { Resource } from '@opentelemetry/resources';
import { NodeSDK } from '@opentelemetry/sdk-node';
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions';

const sdk = new NodeSDK({
    resource: new Resource({
        [SemanticResourceAttributes.SERVICE_NAME]: 'tld_api',
    }),
    traceExporter: new OTLPTraceExporter({ url: "http://localhost:4317" }),
    instrumentations: [getNodeAutoInstrumentations()]
});

sdk.start();
