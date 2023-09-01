from flask import Flask, request

from forecast import predict

app = Flask(__name__)


@app.route("/")
def hello_world():
    return "traffic-light phase-prediction is up"


@app.route("/tlp-prediction/<trafficLightId>", methods=["POST"])
def add_message(trafficLightId: str):
    req_data = request.json
    return predict(trafficLightId, req_data)


if __name__ == "__main__":
    app.run(port=5001)
