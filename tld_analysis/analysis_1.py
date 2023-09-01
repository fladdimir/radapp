import datetime
import json

import pandas as pd
from bokeh.plotting import figure, show

# raw json -> preprocessing + visualization

# https://tld.iot.hamburg.de/v1.1/Datastreams(50850)/Observations?$orderby=phenomenonTime+desc&$select=phenomenonTime,result&$top=10000&$filter=phenomenonTime lt 2023-05-18T10:00:00%2B02:00 and phenomenonTime gt 2023-05-18T08:00:00%2B02:00

raw_dataset = "50850_19052023_08_10"
with open("data/" + raw_dataset + ".json") as file:
    data = json.load(file)["value"]
    print(f"n values: {len(data)}")


df = pd.DataFrame(data)
assert len(data) == len(df)

# https://tld.iot.hamburg.de/v1.1/Datastreams(50850)
# 0=dark,1=red,2=amber,3=green,4=red-amber,5=amber-flashing,6=green-flashing,9=unknown
# order: red, red-amber, green, amber / 1 - 4 - 3 - 2
color_names = {
    0: "dark",
    1: "red",
    2: "amber",
    3: "green",
    4: "red-amber",
    5: "amber-flashing",
    6: "green-flashing",
    9: "unknown",
}


df["phenomenonTime"] = pd.to_datetime(df["phenomenonTime"])
df = df.sort_values(by="phenomenonTime").reset_index(drop=True)
df["result_text"] = df.apply(lambda row: color_names[row["result"]], axis=1)


# duration
def duration(start: datetime, end: datetime) -> float:
    d: datetime.timedelta = end - start
    return d.total_seconds()


def calc_durations_until_next(df: pd.DataFrame) -> None:
    df["next_phenomenonTime"] = df["phenomenonTime"].shift(-1)
    df["duration"] = df.apply(
        lambda row: duration(row["phenomenonTime"], row["next_phenomenonTime"]), axis=1
    )


# filter red or green
df_red_green = df[df["result"].isin((1, 3))].copy()
calc_durations_until_next(df_red_green)
print(df_red_green)
df_red_green.drop(columns=["next_phenomenonTime"], inplace=True)
df_red_green.to_csv(
    "data/" + raw_dataset + ".csv", index=False
)  # save for further analysis

# cycle duration: red -> red
df_red = df[df["result"] == 1].copy()
calc_durations_until_next(df_red)
df_red["mean_duration"] = df_red["duration"].mean()
print(df_red)


# phase durations
calc_durations_until_next(df)
different_results = df["result"].unique()
different_result_means = {}
for result in different_results:
    df_filtered = df[df["result"] == result]
    mean = df_filtered["duration"].mean()
    df.loc[df["result"] == result, "mean_duration"] = mean
    different_result_means[result] = mean

print(df)

# exit()

# plot
colors = {
    1: "#ff0000",
    2: "#ffff00",
    3: "#00ff00",
    4: "#ffa500",
}
p = figure(height=500, width=1800, x_axis_type="datetime", x_axis_location="above")
for result in different_results:
    dfr = df.copy()
    dfr.loc[dfr["result"] != result, "duration"] = 0
    dfr["mean_duration"] = different_result_means[result]

    p.step(
        dfr["phenomenonTime"],
        dfr["duration"],
        mode="after",
        legend_label=color_names[result],
        line_color=colors[result],
    )
    p.line(
        dfr["phenomenonTime"],
        dfr["mean_duration"],
        line_color=colors[result],
        line_dash="dashed",
    )

# cycle_duration
p.step(
    df_red["phenomenonTime"],
    df_red["duration"],
    mode="after",
    legend_label="cycle duration\n(red -> red)",
    line_color="blue",
)
p.line(
    df_red["phenomenonTime"],
    df_red["mean_duration"],
    line_color="blue",
    line_dash="dashed",
)

p.title.text = raw_dataset
p.xaxis.axis_label = "timestamp"
p.yaxis.axis_label = "duration [s]"

show(p)
