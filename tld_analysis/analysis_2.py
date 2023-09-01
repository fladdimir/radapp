import datetime

import pandas as pd


# ,phenomenonTime,result,result_text,next_phenomenonTime,duration
dataset = "50850_18052023_08_10"
df = pd.read_csv("data/" + dataset + ".csv")
df["phenomenonTime"] = pd.to_datetime(df["phenomenonTime"])

print(df)


def duration(start: datetime, end: datetime) -> float:
    d: datetime.timedelta = end - start
    return d.total_seconds()


LAST_N = 16


def predict_all(predict, df: pd.DataFrame):
    for index in df.index[LAST_N:]:
        # print("index: " + str(index))
        last_n = df[index - LAST_N : index].copy()
        last_n = last_n.sort_values(by="phenomenonTime", ascending=False).reset_index(
            drop=True
        )
        last_n.sort_values(by="phenomenonTime", ascending=False, inplace=True)
        last_n.loc[0, "duration"] = None  # to be predicted

        ppt = predict(last_n)
        df.loc[index, "predicted_phenomenonTime"] = ppt

    df["error"] = df.apply(
        lambda row: abs(
            (row["phenomenonTime"] - row["predicted_phenomenonTime"]).total_seconds()
        ),
        axis=1,
    )
    error = df["error"].mean()
    print("error: " + str(error))


# just last_tsp + avg duration of signal phase
def naive(last_n: pd.DataFrame) -> datetime.datetime:
    last_n.sort_values(by="phenomenonTime", ascending=False, inplace=True)
    mean_dur_red = last_n[last_n["result_text"] == "red"]["duration"].mean()
    mean_dur_green = last_n[last_n["result_text"] == "green"]["duration"].mean()

    current_phase = last_n["result_text"].iloc[0]
    mean_dur = mean_dur_red if current_phase == "red" else mean_dur_green

    last_tsp = last_n["phenomenonTime"].iloc[0]
    predicted = last_tsp + datetime.timedelta(seconds=mean_dur)
    return predicted


predict_all(naive, df.copy())

# todo: plot + actual
