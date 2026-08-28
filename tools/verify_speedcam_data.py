"""Checks the government speed camera data and writes the part fit to ship.

The alert is only as good as the data behind it, so rows that would make the app
warn in the wrong place are dropped: coordinates outside Taiwan, missing
positions, or speed limits that cannot be real. A few bad rows are expected in
the published data and get dropped with a note; if a large share fails, the data
itself is suspect and this exits non-zero so the build cannot ship it.

Usage: python tools/verify_speedcam_data.py data/speedcam/nationwide.csv [out.csv]
"""

import sys
import pandas as pd

# Taiwan proper plus the outlying counties of Kinmen, Matsu and Penghu
LON_MIN, LON_MAX = 118.0, 122.2
LAT_MIN, LAT_MAX = 21.7, 26.5
SPEED_MIN, SPEED_MAX = 20, 120


def load(path):
    # the file carries an English header row followed by a Chinese one
    frame = pd.read_csv(path, encoding="utf-8-sig", skiprows=[1])
    frame.columns = [c.strip() for c in frame.columns]
    return frame


# more than this share of unusable rows means the source itself changed shape
MAX_DROPPED_SHARE = 0.01


def check(frame):
    failures = []
    notes = []

    required = ["CityName", "Address", "Longitude", "Latitude", "direct", "limit"]
    missing = [c for c in required if c not in frame.columns]
    if missing:
        failures.append("missing columns: %s" % ", ".join(missing))
        return frame.iloc[0:0], failures, notes

    total = len(frame)
    notes.append("rows published: %d" % total)

    lon = pd.to_numeric(frame["Longitude"], errors="coerce")
    lat = pd.to_numeric(frame["Latitude"], errors="coerce")
    limit = pd.to_numeric(frame["limit"], errors="coerce")

    unparsable = lon.isna() | lat.isna()
    outside = (lon < LON_MIN) | (lon > LON_MAX) | (lat < LAT_MIN) | (lat > LAT_MAX)
    bad_limit = (limit < SPEED_MIN) | (limit > SPEED_MAX)

    if unparsable.any():
        notes.append("dropped, no usable coordinate: %d" % int(unparsable.sum()))
    if outside.any():
        for _, row in frame[outside.fillna(False)].iterrows():
            notes.append(
                "dropped, coordinate not in Taiwan: %s %s at %s, %s"
                % (row["CityName"], row["Address"], row["Longitude"], row["Latitude"])
            )
    if bad_limit.any():
        notes.append("dropped, impossible speed limit: %d" % int(bad_limit.sum()))

    dropped = unparsable | outside.fillna(False) | bad_limit.fillna(False)
    kept = frame[~dropped].copy()

    share = float(dropped.sum()) / total if total else 1.0
    if share > MAX_DROPPED_SHARE:
        failures.append(
            "%.1f%% of rows unusable, over the %.1f%% the source normally has"
            % (share * 100, MAX_DROPPED_SHARE * 100)
        )

    kept_lon = pd.to_numeric(kept["Longitude"])
    kept_lat = pd.to_numeric(kept["Latitude"])
    notes.append("rows kept: %d" % len(kept))
    notes.append(
        "longitude %.5f..%.5f, latitude %.5f..%.5f"
        % (kept_lon.min(), kept_lon.max(), kept_lat.min(), kept_lat.max())
    )
    notes.append("missing speed limit: %d (alert still fires, without the number)"
                 % int(pd.to_numeric(kept["limit"], errors="coerce").isna().sum()))
    notes.append("duplicate positions: %d" % int(kept.duplicated(subset=["Longitude", "Latitude"]).sum()))
    notes.append("distinct areas named: %d" % kept["CityName"].nunique())
    notes.append(
        "camera directions: %s"
        % ", ".join("%s=%d" % (k, v) for k, v in kept["direct"].value_counts().head(8).items())
    )

    return kept, failures, notes


def write_asset(kept, path):
    """Writes the smallest form the app needs: where, how fast, which way."""
    asset = pd.DataFrame({
        "lat": pd.to_numeric(kept["Latitude"]).round(6),
        "lon": pd.to_numeric(kept["Longitude"]).round(6),
        "limit": pd.to_numeric(kept["limit"], errors="coerce").fillna(0).astype(int),
        "direct": kept["direct"].astype(str).str.strip(),
    })
    asset = asset.drop_duplicates(subset=["lat", "lon", "direct"])
    asset.to_csv(path, index=False, encoding="utf-8", lineterminator="\n")
    return len(asset)


def main():
    if len(sys.argv) not in (2, 3, 4):
        print(__doc__)
        return 2

    frame = load(sys.argv[1])
    kept, failures, notes = check(frame)

    for note in notes:
        print("  %s" % note)

    if failures:
        print("\nFAIL")
        for failure in failures:
            print("  %s" % failure)
        return 1

    if len(sys.argv) >= 3:
        kept.to_csv(sys.argv[2], index=False, encoding="utf-8")
        print("\nwrote %s" % sys.argv[2])

    if len(sys.argv) == 4:
        print("wrote %s (%d cameras)" % (sys.argv[3], write_asset(kept, sys.argv[3])))

    print("\nPASS: the data can be built into the app")
    return 0


if __name__ == "__main__":
    sys.exit(main())
