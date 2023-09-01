import './otel_instrumentation.js';

import express from 'express';
import { getData, shutdown } from './tld_api_proxy.js';


/**
 * tld_api_proxy - cache for data from the iot-backend, always providing the latest n values for a datastream
 * 
 * input:
 *      datastreamId (url path parameter)
 *      + optionally: nvalues=10 (url query parameter)
 *      http://host:port/tld/50850?nvalues=10
 * output: 
 * [
 *      {
 *          "phenomenonTime": "2023-05-20T18:07:56Z",
 *          "resultTime": "2023-05-20T18:07:58.200155Z",
 *          "result": 2,
 *      },
 *      // [...] + 9 more
 * ]
 *  
*/
const app = express();
const port = 3000;

app.get('/', (req, res) => {
    res.send('Hello from tld_api_proxy!');
});

app.get('/tld/:datastreamId', async (req, res) => {
    const datastreamId = req.params.datastreamId;
    const nvalues = req.query.nvalues;
    const data = await getData(datastreamId, nvalues);
    res.json(data);
});

process.on('SIGINT', () => {
    shutdown();
    console.log("shutdown complete.");
    process.exit(0);
});

app.listen(port, () => {
    console.log(`tld_api_proxy listening on port ${port}`);
});
