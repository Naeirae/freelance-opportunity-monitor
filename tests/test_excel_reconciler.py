import importlib.util
from pathlib import Path

import pandas as pd


MODULE_PATH = Path(__file__).resolve().parents[1] / "products" / "excel-reconciler" / "reconcile.py"
spec = importlib.util.spec_from_file_location("excel_reconciler", MODULE_PATH)
module = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(module)


def test_reconcile_finds_missing_changed_and_duplicates():
    first = pd.DataFrame(
        [
            {"id": "1", "name": "Alpha", "amount": 100},
            {"id": "2", "name": "Beta", "amount": 200},
            {"id": "2", "name": "Beta duplicate", "amount": 200},
            {"id": "3", "name": "Gamma", "amount": 300},
        ]
    )
    second = pd.DataFrame(
        [
            {"id": "1", "name": "Alpha", "amount": 100},
            {"id": "2", "name": "Beta", "amount": 250},
            {"id": "4", "name": "Delta", "amount": 400},
        ]
    )

    result = module.reconcile(first, second, "id")

    assert result["only_first"]["id"].tolist() == ["3"]
    assert result["only_second"]["id"].tolist() == ["4"]
    assert result["changed"]["id"].tolist() == ["2"]
    assert result["changes"].to_dict("records") == [
        {"id": "2", "field": "amount", "before": "200", "after": "250"}
    ]
    assert result["duplicates_first"]["id"].tolist() == ["2", "2"]
    assert result["duplicates_second"].empty


def test_reconcile_trims_key_whitespace():
    first = pd.DataFrame([{"id": " 42 ", "value": "x"}])
    second = pd.DataFrame([{"id": "42", "value": "x"}])

    result = module.reconcile(first, second, "id")

    assert result["only_first"].empty
    assert result["only_second"].empty
    assert result["changed"].empty


def test_reconcile_compares_only_selected_columns():
    first = pd.DataFrame([{"id": "1", "amount": 100, "manager": "A"}])
    second = pd.DataFrame([{"id": "1", "amount": 100, "manager": "B"}])

    ignored = module.reconcile(first, second, "id", compare_columns=["amount"])
    included = module.reconcile(first, second, "id", compare_columns=["manager"])

    assert ignored["changes"].empty
    assert included["changes"].to_dict("records") == [
        {"id": "1", "field": "manager", "before": "A", "after": "B"}
    ]
