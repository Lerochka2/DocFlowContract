import pandas as pd
import pm4py

log_path = r"D:\7 семестр\КУРСАЧ_ДИПЛОМ\DocFlowContract\rpa\Logs\lab6_event_log.csv"

print("PM4Py version:", pm4py.__version__)

# Загружаем event log
df = pd.read_csv(
    log_path,
    sep=";",
    encoding="utf-8-sig"
)

# Преобразуем дату и время
df["timestamp"] = pd.to_datetime(
    df["timestamp"],
    format="%d.%m.%Y %H:%M:%S"
)

# Подготавливаем данные для Process Mining
event_log = pm4py.format_dataframe(
    df,
    case_id="case_id",
    activity_key="activity",
    timestamp_key="timestamp"
)

# Строим Directly-Follows Graph
dfg, start_activities, end_activities = pm4py.discover_dfg(event_log)

print()
print("=== EVENT LOG ===")
print("Событий:", len(event_log))
print("Экземпляров процесса:", event_log["case:concept:name"].nunique())

print()
print("=== НАЧАЛЬНЫЕ ОПЕРАЦИИ ===")
print(start_activities)

print()
print("=== КОНЕЧНЫЕ ОПЕРАЦИИ ===")
print(end_activities)

print()
print("=== ПЕРЕХОДЫ DFG ===")
for transition, count in sorted(dfg.items()):
    print(f"{transition[0]} -> {transition[1]} : {count}")

print()
print("=== ВАРИАНТЫ ПРОЦЕССА ===")

variants = (
    event_log
    .sort_values(["case:concept:name", "time:timestamp"])
    .groupby("case:concept:name")["concept:name"]
    .apply(lambda x: " -> ".join(x))
    .value_counts()
)

for variant, count in variants.items():
    print(f"{count} x {variant}")
