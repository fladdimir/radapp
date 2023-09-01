import datetime
from os import listdir
from os.path import isfile, join

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from geopy import distance

data_dir = "./data"

csv_files = [
    join(data_dir, f)
    for f in listdir(data_dir)
    if isfile(join(data_dir, f)) and f.endswith(".csv")
]

print("analysing: " + str(csv_files))

dfs = [pd.read_csv(csv) for csv in csv_files]
# id, insert_time, accuracy, alt, alt_accuracy, heading, lat, lng, measurement_time, speed, trip_id
df = pd.concat(dfs)
df = df[["id", "trip_id", "measurement_time", "speed", "lat", "lng"]]
df["measurement_time"] = pd.to_datetime(df["measurement_time"])

print(df.dtypes)
print(df)


def analyse_trip(df: pd.DataFrame, trip_id: int):
    print("\ntrip " + str(trip_id) + ":\n")
    df = df[df["trip_id"] == trip_id].copy()
    df.drop_duplicates("measurement_time", inplace=True)
    df.sort_values("measurement_time", inplace=True)

    # basic info
    start_time = min(df["measurement_time"])
    stop_time = max(df["measurement_time"])
    duration: datetime.timedelta = stop_time - start_time
    print("start_time: " + str(start_time))
    print("stop_time: " + str(stop_time))
    print("duration: " + str(duration))
    num_samples = len(df)
    print("# samples: " + str(num_samples))

    # distance
    df["next_lat"] = df["lat"].shift(-1)
    df["next_lng"] = df["lng"].shift(-1)
    # tbd: vectorize, with geopandas
    df["distance_m_next"] = df.apply(
        lambda row: distance.distance(
            (row["lat"], row["lng"]), (row["next_lat"], row["next_lng"])
        ).m
        if not np.isnan(row["next_lat"])
        else 0,
        axis=1,
    )
    distance_m = sum(df["distance_m_next"])
    print("length (m): " + str(round(distance_m, 2)))

    # average speed
    speed_ms_distance_based = distance_m / duration.seconds
    print("speed (m/s) distance-based: " + str(speed_ms_distance_based))
    print("km/h: " + str(speed_ms_distance_based * 3.6))
    speed_ms_distance_based_10_700 = 10_700 / duration.seconds
    print("speed (m/s) distance-based (10.7km): " + str(speed_ms_distance_based_10_700))
    print("km/h: " + str(speed_ms_distance_based_10_700 * 3.6))
    speed_ms_sample_based = sum(df["speed"]) / len(df["speed"])
    print("speed (m/s) sample-based: " + str(speed_ms_sample_based))
    print("km/h: " + str(speed_ms_sample_based * 3.6))

    max_speed = max(df["speed"])
    print("max speed (m/s): " + str(max_speed))
    print("km/h: " + str(max_speed * 3.6))

    # stops
    def calc_share(threshold: float, df: pd.DataFrame) -> float:
        num_samples = len(df[df["speed"] < threshold])
        return num_samples / len(df)

    thresholds = np.linspace(0, max_speed, 50)
    shares = [calc_share(t, df) * 100 for t in thresholds]
    fig, ax = plt.subplots()
    ax.plot(thresholds, shares)
    # plt.show()
    # approx. 10% of the samples are below 0.2 m/s, thereafter the share does not increase as much
    # (derivative local max) # so that might be a good threshold to detect stops

    STOP_THRESHOLD_M_S = 1 / 3.6  # 1 km/h
    num_samples_stopped = len(df[df["speed"] < STOP_THRESHOLD_M_S])
    print("# samples below standing threshold: " + str(num_samples_stopped))
    print(str(round(num_samples_stopped / num_samples, 3)))


analyse_trip(df, 14156)
analyse_trip(df, 14164)


def plot_speeds(df: pd.DataFrame):
    df = df.copy()
    df.drop_duplicates("measurement_time", inplace=True)
    df.sort_values("measurement_time", inplace=True)

    fig, ax = plt.subplots()

    trips = sorted(df["trip_id"].unique())
    for trip_id in trips:
        trip_df = df[df["trip_id"] == trip_id]
        trip_df = trip_df.copy()
        start_time = min(trip_df["measurement_time"])
        trip_df["time"] = trip_df["measurement_time"] - start_time
        times = trip_df["time"]
        speeds = trip_df["speed"]
        ax.plot(times, speeds)
    # plt.show()


plot_speeds(df)
