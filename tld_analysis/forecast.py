import datetime
import pandas as pd

MAX_VARIATION_COEFFICIENT = 0.5


# TODO: tests + rewrite without pandas (speed-up ?)
def predict(trafficLightId: str, input: list[dict]) -> list[dict]:
    # print("trafficLightId: " + trafficLightId)

    if len(input) < 2:
        return []

    df = pd.DataFrame(input)
    df = df[df["result"].isin((1, 3))]  # filter red or green
    df["phenomenonTime"] = pd.to_datetime(df["phenomenonTime"])
    df = df.sort_values(by="phenomenonTime", ascending=False).reset_index(drop=True)
    calc_durations_until_next(df)
    # print(df)

    mean_dur_red = df[df["result"] == 1]["duration"].mean()
    mean_dur_green = df[df["result"] == 3]["duration"].mean()

    std_dev_red = df[df["result"] == 1]["duration"].std()
    std_dev_green = df[df["result"] == 3]["duration"].std()
    if (
        std_dev_red / mean_dur_red > MAX_VARIATION_COEFFICIENT
        or std_dev_green / mean_dur_green > MAX_VARIATION_COEFFICIENT
    ):
        print(
            "standard deviation of past durations higher than threshold, no useful prediction possible"
        )
        return []
    # TODO: further input validation for useful predictions (time since last observation / oae)

    current_phase = df["result"].iloc[0]
    mean_duration = mean_dur_red if current_phase == 1 else mean_dur_green

    last_tsp = df["phenomenonTime"].iloc[0]
    predicted: datetime.datetime = last_tsp + datetime.timedelta(seconds=mean_duration)
    if predicted < datetime.datetime.now(predicted.tzinfo):
        predicted = datetime.datetime.now(predicted.tzinfo) + datetime.timedelta(
            seconds=1
        )
    next_phase = 3 if current_phase == 1 else 1

    next_phase_2 = 3 if next_phase == 1 else 1
    mean_duration_2 = mean_dur_red if next_phase == 1 else mean_dur_green
    predicted_2: datetime.datetime = predicted + datetime.timedelta(
        seconds=mean_duration_2
    )

    prediction = [
        {
            "phenomenonTime": predicted.isoformat(),
            "result": next_phase,
        },
        {
            "phenomenonTime": predicted_2.isoformat(),
            "result": next_phase_2,
        },
    ]
    # print(prediction)
    return prediction


def calc_durations_until_next(df: pd.DataFrame) -> None:
    df["next_phenomenonTime"] = df["phenomenonTime"].shift(1)
    df["duration"] = df.apply(
        lambda row: duration(row["phenomenonTime"], row["next_phenomenonTime"]), axis=1
    )


def duration(start: datetime, end: datetime) -> float:
    d: datetime.timedelta = end - start
    return d.total_seconds()
