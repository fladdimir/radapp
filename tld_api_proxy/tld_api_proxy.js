import { initialRequest, Subscription } from "./tld_api_client.js";
import log from "./util.js";

const N_VALUES_DEFAULT = 10;

// returns the most current n values
export async function getData(datastreamId, nvalues) {

    nvalues = nvalues ?? N_VALUES_DEFAULT;

    let observer = OBSERVER_MAP.get(datastreamId);
    if (!observer) {
        observer = new DatastreamObserver(datastreamId, nvalues);
        OBSERVER_MAP.set(datastreamId, observer);
    } else if (observer.nvalues < nvalues) {
        observer.evict(); // too few values, create new
        observer = new DatastreamObserver(datastreamId, nvalues);
        OBSERVER_MAP.set(datastreamId, observer);
    }
    const values = await observer.getValues();
    // may be more than requested
    return values.length == nvalues ? values : values.slice(0, nvalues);
}

export function shutdown() {
    OBSERVER_MAP.forEach(observer => observer.evict());
}

const OBSERVER_MAP = new Map();

const EVICTION_TIMEOUT_MS = 1 * 60 * 1000;

// efficiently provides the newest N values
// the cache is initialized via an initial http request,
// subsequent updates are received via ws / mqtt
class DatastreamObserver {

    values = []; // new values first

    constructor(datastreamId, nvalues) {
        log(`new observer for datastreamId: ${datastreamId}, nvalues: ${nvalues}`);
        this.datastreamId = datastreamId;
        this.nvalues = nvalues;
        this.scheduleEviction();
    }

    async getValues() {
        this.clearEvictionTimeout();

        if (!this.values.length) { // first call
            log("initial request for datastreamId: " + this.datastreamId);

            const result = await initialRequest(this.datastreamId, this.nvalues);
            this.values.push(...result);

            this.subscribe();
        }

        this.scheduleEviction();
        return this.values;
    }

    subscribe() {
        this.subscription = new Subscription(
            this.datastreamId,
            () => { },
            data => this.onNewData(data),
            () => this.evict());
    }

    onNewData(data) {
        log(`new data received for datastreamId ${this.datastreamId}: ${data.phenomenonTime}`);
        while (this.values.length >= this.nvalues) this.values.pop();
        this.values.unshift(data);
    }

    scheduleEviction() {
        this.evictionTimeout = setTimeout(() => this.evict(), EVICTION_TIMEOUT_MS);
    }

    clearEvictionTimeout() {
        clearTimeout(this.evictionTimeout);
    }

    evict() {
        log("evicting observer for datastreamId: " + this.datastreamId);
        if (this.evictionTimeout) this.clearEvictionTimeout();
        if (this.subscription) this.subscription.cancel();
        OBSERVER_MAP.delete(this.datastreamId);
    }
}
