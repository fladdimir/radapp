import fetch from "node-fetch";

import pahoMqtt from 'paho-mqtt';
const { Client } = pahoMqtt;

import WebSocket from "ws";
global.WebSocket = WebSocket;

import log from "./util.js";


const BASE_URL = "https://tld.iot.hamburg.de/v1.1/";

export async function initialRequest(datastreamId, nValues) {

    const url = BASE_URL + "Datastreams(" + datastreamId + ")/"
        + "Observations?$top=" + nValues
        + "&$orderby=phenomenonTime+desc&$select=phenomenonTime,resultTime,result";

    const response = await fetch(url);

    const parsed = await response.json();

    if (!parsed.value) { console.log(response); console.log(parsed); }

    return parsed.value;
}

const HOST = "tld.iot.hamburg.de"
const PORT = 443;
const PATH = "/mqtt";
// v1.0/Datastreams(datastreamId)/Observations


export class Subscription {

    constructor(datastreamId, onOpenCb, onMessageCb, onFailureCb) {
        this.datastreamId = datastreamId;
        this.onOpenCb = onOpenCb;
        this.onMessageCb = onMessageCb;
        this.onFailureCb = onFailureCb;
        this.subscribe();
    }

    subscribe() {

        const topic = "v1.0/Datastreams(" + this.datastreamId + ")/Observations";

        // todo: client singleton for multiple subscriptions
        const client = new Client(HOST, PORT, PATH, "undefined_" + this.datastreamId);
        this.connection = client;

        client.onMessageArrived = (message) => this._onMessage(message.payloadString);
        client.onConnectionLost = (error) => this._onFailure(error.errorCode, error.errorMessage);

        client.connect({
            useSSL: true,
            onSuccess: (o) => client.subscribe(topic, {
                onSuccess: success => this._onOpen(),
                onFailure: (error) => this._onFailure(error.errorCode, error.errorMessage),

            }),
            onFailure: (error) => this._onFailure(error.errorCode, error.errorMessage),
        });
    }

    cancel() {
        if (this.connection.isConnected()) {
            log("disconnecting for datastreamId: " + this.datastreamId);
            this.onFailureCb = () => { /* don't invoke actual onFailureCb on a self-initiated connection-close */ };
            this.connection.disconnect();
        }
    }

    _onOpen() {
        log("subsription created for datastreamId: " + this.datastreamId);
        this.onOpenCb();
    }

    _onMessage(data) {
        log("message for datastreamId: " + this.datastreamId);
        const parsed = JSON.parse(data);
        this.onMessageCb(parsed);
    }

    _onFailure(code, reason) {
        log(`subsription closed for datastreamId: ${this.datastreamId}, code: ${code}, reason: ${reason}`);
        this.onFailureCb();
    }

}
