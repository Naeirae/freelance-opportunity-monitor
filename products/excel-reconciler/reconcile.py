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


def _display_value(value) -> str:
    if pd.isna(value):
        return ""
    return str(value).strip()


def reconcile(
    first: pd.DataFrame,
    second: pd.DataFrame,
    key: str,
    compare_columns: list[str] | None = None,
) -> dict[str, pd.DataFrame]:
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

    available_compare_columns = [
        column
        for column in left_unique.columns
        if column in right_unique.columns and column != key
    ]
    if compare_columns is None:
        compare_columns = available_compare_columns
    else:
        invalid = [column for column in compare_columns if column not in available_compare_columns]
        if invalid:
            raise KeyError(f"Columns are not available in both files: {', '.join(invalid)}")

    merged = left_unique.merge(
        right_unique,
        how="inner",
        on=key,
        suffixes=("__first", "__second"),
    )

    change_rows: list[dict[str, str]] = []
    changed_keys: set[str] = set()
    for _, row in merged.iterrows():
        row_key = _display_value(row[key])
        for column in compare_columns:
            first_col = f"{column}__first"
            second_col = f"{column}__second"
            before = _display_value(row[first_col])
            after = _display_value(row[second_col])
            if before != after:
                changed_keys.add(row_key)
                change_rows.append(
                    {
                        key: row_key,
                        "field": column,
                        "before": before,
                        "after": after,
                    }
                )

    changed_details = pd.DataFrame(change_rows, columns=[key, "field", "before", "after"])
    if changed_keys:
        changed = merged[merged[key].astype("string").isin(changed_keys)].copy()
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
            {"metric": "changed_common_keys", "value": len(changed_keys)},
            {"metric": "changed_fields", "value": len(changed_details)},
            {"metric": "duplicate_rows_first", "value": len(left_duplicates)},
            {"metric": "duplicate_rows_second", "value": len(right_duplicates)},
        ]
    )

    human_summary = pd.DataFrame(
        [
            {"Показатель": "Записей только в первом файле", "Количество": len(only_first)},
            {"Показатель": "Записей только во втором файле", "Количество": len(only_second)},
            {"Показатель": "Изменённых записей", "Количество": len(changed_keys)},
            {"Показатель": "Изменённых полей", "Количество": len(changed_details)},
            {"Показатель": "Строк-дублей в первом файле", "Количество": len(left_duplicates)},
            {"Показатель": "Строк-дублей во втором файле", "Количество": len(right_duplicates)},
        ]
    )

    return {
        "human_summary": human_summary,
        "changes": changed_details,
        "only_first": only_first,
        "only_second": only_second,
        "duplicates_first": left_duplicates,
        "duplicates_second": right_duplicates,
        # Technical sheets are kept for debugging/backward compatibility.
        "summary": summary,
        "changed": changed,
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
    parser.add_argument(
        "--compare",
        nargs="*",
        default=None,
        help="Columns to compare. If omitted, all common non-key columns are compared.",
    )
    parser.add_argument("--output", type=Path, default=Path("reconciliation_report.xlsx"))
    return parser


def main() -> None:
    args = build_parser().parse_args()
    first = load_table(args.first)
    second = load_table(args.second)
    result = reconcile(first, second, args.key, compare_columns=args.compare)
    write_report(result, args.output)
    print(f"Report written to {args.output}")


if __name__ == "__main__":
    main()
