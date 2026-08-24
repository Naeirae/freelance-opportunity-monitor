from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd


def load_table(path: Path) -> pd.DataFrame:
    suffix = path.suffix.lower()
    if suffix == ".csv":
        return pd.read_csv(path)
    if suffix in {".xlsx", ".xls"}:
        return pd.read_excel(path)
    raise ValueError(f"Unsupported file type: {suffix}")


def normalize_key(series: pd.Series) -> pd.Series:
    return series.astype("string").str.strip()


def reconcile(first: pd.DataFrame, second: pd.DataFrame, key: str) -> dict[str, pd.DataFrame]:
    if key not in first.columns:
        raise KeyError(f"Column '{key}' is missing in the first file")
    if key not in second.columns:
        raise KeyError(f"Column '{key}' is missing in the second file")

    left = first.copy()
    right = second.copy()
    left[key] = normalize_key(left[key])
    right[key] = normalize_key(right[key])

    left_duplicates = left[left.duplicated(key, keep=False)].sort_values(key)
    right_duplicates = right[right.duplicated(key, keep=False)].sort_values(key)

    left_unique = left.drop_duplicates(key, keep="first")
    right_unique = right.drop_duplicates(key, keep="first")

    left_keys = set(left_unique[key].dropna())
    right_keys = set(right_unique[key].dropna())

    only_first = left_unique[left_unique[key].isin(left_keys - right_keys)].copy()
    only_second = right_unique[right_unique[key].isin(right_keys - left_keys)].copy()

    common_columns = [column for column in left_unique.columns if column in right_unique.columns and column != key]
    merged = left_unique.merge(
        right_unique,
        how="inner",
        on=key,
        suffixes=("__first", "__second"),
    )

    difference_flags = []
    for column in common_columns:
        first_col = f"{column}__first"
        second_col = f"{column}__second"
        first_values = merged[first_col].astype("string").fillna("").str.strip()
        second_values = merged[second_col].astype("string").fillna("").str.strip()
        difference_flags.append(first_values.ne(second_values))

    if difference_flags:
        differs = difference_flags[0]
        for flag in difference_flags[1:]:
            differs = differs | flag
        changed = merged[differs].copy()
    else:
        changed = merged.iloc[0:0].copy()

    summary = pd.DataFrame(
        [
            {"metric": "rows_first", "value": len(first)},
            {"metric": "rows_second", "value": len(second)},
            {"metric": "unique_keys_first", "value": len(left_unique)},
            {"metric": "unique_keys_second", "value": len(right_unique)},
            {"metric": "only_first", "value": len(only_first)},
            {"metric": "only_second", "value": len(only_second)},
            {"metric": "changed_common_keys", "value": len(changed)},
            {"metric": "duplicate_rows_first", "value": len(left_duplicates)},
            {"metric": "duplicate_rows_second", "value": len(right_duplicates)},
        ]
    )

    return {
        "summary": summary,
        "only_first": only_first,
        "only_second": only_second,
        "changed": changed,
        "duplicates_first": left_duplicates,
        "duplicates_second": right_duplicates,
    }


def write_report(result: dict[str, pd.DataFrame], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with pd.ExcelWriter(output, engine="openpyxl") as writer:
        for sheet_name, frame in result.items():
            frame.to_excel(writer, sheet_name=sheet_name[:31], index=False)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Compare two CSV/XLSX files and create an Excel discrepancy report.")
    parser.add_argument("first", type=Path)
    parser.add_argument("second", type=Path)
    parser.add_argument("--key", required=True, help="Column used as the record key")
    parser.add_argument("--output", type=Path, default=Path("reconciliation_report.xlsx"))
    return parser


def main() -> None:
    args = build_parser().parse_args()
    first = load_table(args.first)
    second = load_table(args.second)
    result = reconcile(first, second, args.key)
    write_report(result, args.output)
    print(f"Report written to {args.output}")


if __name__ == "__main__":
    main()
