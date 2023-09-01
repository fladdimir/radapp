#!/bin/bash

nginx() {
    docker-compose up
}

tld_api_proxy() {
    cd ./tld_api_proxy && node index.js
}

tld_forecast() {
    cd ./tld_analysis && source venv/bin/activate &&
        ./start_with_otel.sh
}

be_db() {
    cd be && docker-compose up
}

otel() {
    cd be/otel && docker-compose up
}

nginx &
tld_api_proxy &
tld_forecast &
be_db &
otel
