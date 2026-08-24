from __future__ import annotations

from io import BytesIO
from pathlib import Path

import pandas as pd
import streamlit as st

from reconcile import reconcile


st.set_page_config(page_title="Excel Reconciler", page_icon="📊", layout="wide")


def load_uploaded_table(uploaded_file) -> pd.DataFrame:
    suffix = Path(uploaded_file.name).suffix.lower()
    if suffix == ".csv":
        return pd.read_csv(uploaded_file)
    if suffix in {".xlsx", ".xls"}:
        return pd.read_excel(uploaded_file)
    raise ValueError(f"Неподдерживаемый формат: {suffix}")


def result_to_excel(result: dict[str, pd.DataFrame]) -> bytes:
    buffer = BytesIO()
    with pd.ExcelWriter(buffer, engine="openpyxl") as writer:
        for sheet_name, frame in result.items():
            frame.to_excel(writer, sheet_name=sheet_name[:31], index=False)
    buffer.seek(0)
    return buffer.getvalue()


st.title("Excel / CSV Reconciler")
st.caption("Сверка двух таблиц: пропуски, новые записи, изменения и дубли.")

left_col, right_col = st.columns(2)
with left_col:
    first_file = st.file_uploader("Первый файл", type=["csv", "xlsx", "xls"], key="first")
with right_col:
    second_file = st.file_uploader("Второй файл", type=["csv", "xlsx", "xls"], key="second")

if first_file and second_file:
    try:
        first = load_uploaded_table(first_file)
        second = load_uploaded_table(second_file)
    except Exception as exc:
        st.error(f"Не удалось прочитать файл: {exc}")
        st.stop()

    common_columns = [column for column in first.columns if column in second.columns]
    if not common_columns:
        st.error("В файлах нет общих столбцов. Нужен хотя бы один общий столбец для ключа сверки.")
        st.stop()

    key = st.selectbox(
        "По какому столбцу сопоставлять строки?",
        options=common_columns,
        help="Например: id, номер заказа, артикул, email или другой уникальный идентификатор.",
    )

    preview_left, preview_right = st.columns(2)
    with preview_left:
        st.subheader("Первый файл")
        st.dataframe(first.head(20), use_container_width=True)
        st.caption(f"Строк: {len(first):,} · Столбцов: {len(first.columns)}")
    with preview_right:
        st.subheader("Второй файл")
        st.dataframe(second.head(20), use_container_width=True)
        st.caption(f"Строк: {len(second):,} · Столбцов: {len(second.columns)}")

    if st.button("Сверить файлы", type="primary", use_container_width=True):
        try:
            result = reconcile(first, second, key)
        except Exception as exc:
            st.error(f"Не удалось выполнить сверку: {exc}")
            st.stop()

        st.session_state["reconciliation_result"] = result
        st.session_state["reconciliation_key"] = key

if "reconciliation_result" in st.session_state:
    result = st.session_state["reconciliation_result"]
    key = st.session_state["reconciliation_key"]

    summary_values = dict(zip(result["summary"]["metric"], result["summary"]["value"]))

    st.divider()
    st.subheader("Результат")
    metric_cols = st.columns(5)
    metric_cols[0].metric("Только в первом", int(summary_values.get("only_first", 0)))
    metric_cols[1].metric("Только во втором", int(summary_values.get("only_second", 0)))
    metric_cols[2].metric("Изменены", int(summary_values.get("changed_common_keys", 0)))
    metric_cols[3].metric("Дубли в первом", int(summary_values.get("duplicate_rows_first", 0)))
    metric_cols[4].metric("Дубли во втором", int(summary_values.get("duplicate_rows_second", 0)))

    tab_summary, tab_first, tab_second, tab_changed, tab_duplicates = st.tabs(
        ["Сводка", "Только в первом", "Только во втором", "Изменённые", "Дубли"]
    )

    with tab_summary:
        st.dataframe(result["summary"], use_container_width=True, hide_index=True)

    with tab_first:
        st.dataframe(result["only_first"], use_container_width=True, hide_index=True)

    with tab_second:
        st.dataframe(result["only_second"], use_container_width=True, hide_index=True)

    with tab_changed:
        if result["changed"].empty:
            st.success("Совпадающие по ключу строки не отличаются.")
        else:
            st.caption(f"Ключ сопоставления: {key}")
            st.dataframe(result["changed"], use_container_width=True, hide_index=True)

    with tab_duplicates:
        left_dup, right_dup = st.columns(2)
        with left_dup:
            st.markdown("**Первый файл**")
            st.dataframe(result["duplicates_first"], use_container_width=True, hide_index=True)
        with right_dup:
            st.markdown("**Второй файл**")
            st.dataframe(result["duplicates_second"], use_container_width=True, hide_index=True)

    report_bytes = result_to_excel(result)
    st.download_button(
        "Скачать Excel-отчёт",
        data=report_bytes,
        file_name="reconciliation_report.xlsx",
        mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        type="primary",
        use_container_width=True,
    )

st.divider()
st.caption("Файлы обрабатываются в памяти приложения. Для публичного размещения нужно отдельно настроить политику хранения и конфиденциальности.")
