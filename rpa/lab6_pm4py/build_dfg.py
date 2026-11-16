import pandas as pd
import pm4py
import matplotlib.pyplot as plt

log_path = r"D:\7 семестр\КУРСАЧ_ДИПЛОМ\DocFlowContract\rpa\Logs\lab6_event_log.csv"
output_path = r"D:\7 семестр\КУРСАЧ_ДИПЛОМ\DocFlowContract\rpa\lab6_pm4py\lab6_dfg_clean.png"

df = pd.read_csv(
    log_path,
    sep=";",
    encoding="utf-8-sig"
)

df["timestamp"] = pd.to_datetime(
    df["timestamp"],
    format="%d.%m.%Y %H:%M:%S"
)

event_log = pm4py.format_dataframe(
    df,
    case_id="case_id",
    activity_key="activity",
    timestamp_key="timestamp"
)

dfg, start_activities, end_activities = pm4py.discover_dfg(event_log)

fig, ax = plt.subplots(figsize=(14, 6))

positions = {
    "FILE_DETECTED": (0.10, 0.50),
    "UPLOAD_STARTED": (0.32, 0.50),
    "UPLOAD_SUCCESS": (0.57, 0.72),
    "FILE_MOVED_PROCESSED": (0.84, 0.72),
    "UPLOAD_ERROR": (0.57, 0.28),
    "FILE_MOVED_ERROR": (0.84, 0.28),
}

for activity, (x, y) in positions.items():
    ax.text(
        x, y,
        activity,
        ha="center",
        va="center",
        fontsize=10,
        bbox=dict(
            boxstyle="round,pad=0.6"
        )
    )

for (source, target), count in dfg.items():
    x1, y1 = positions[source]
    x2, y2 = positions[target]

    ax.annotate(
        "",
        xy=(x2 - 0.07, y2),
        xytext=(x1 + 0.07, y1),
        arrowprops=dict(
            arrowstyle="->",
            linewidth=1.8
        )
    )

    mx = (x1 + x2) / 2
    my = (y1 + y2) / 2

    if source == "UPLOAD_STARTED" and target == "UPLOAD_SUCCESS":
        my += 0.035

    if source == "UPLOAD_STARTED" and target == "UPLOAD_ERROR":
        my -= 0.035

    ax.text(
        mx,
        my,
        str(count),
        ha="center",
        va="center",
        fontsize=11,
        bbox=dict(boxstyle="round,pad=0.2")
    )

ax.set_title(
    "Фактическая модель выполнения RPA-процесса (DFG)",
    fontsize=14
)

ax.set_xlim(0, 1)
ax.set_ylim(0, 1)
ax.axis("off")

plt.tight_layout()
plt.savefig(
    output_path,
    dpi=250,
    bbox_inches="tight"
)

print("[OK] Модель сохранена:")
print(output_path)
