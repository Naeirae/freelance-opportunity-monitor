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


def metric_value(result: dict[str, pd.DataFrame], name: str) -> int:
    summary = result["summary"]
    match = summary.loc[summary["metric"] == name, "value"]
    return int(match.iloc[0]) if not match.empty else 0


st.title("Сверка Excel / CSV")
st.caption("Загрузите две таблицы, выберите ключ и укажите, какие поля действительно нужно сравнить.")

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
        st.error("В файлах нет общих столбцов. Нужен хотя бы один общий столбец для сопоставления строк.")
        st.stop()

    st.subheader("1. Настройка сверки")
    key = st.selectbox(
        "Какой столбец идентифицирует одну и ту же запись?",
        options=common_columns,
        help="Например: order_id, номер заказа, артикул, email или ID клиента.",
    )

    compare_options = [column for column in common_columns if column != key]
    compare_columns = st.multiselect(
        "Какие поля сравнивать?",
        options=compare_options,
        default=compare_options,
        help="Снимите галочку с технических полей, изменения которых вам не важны.",
    )

    preview_left, preview_right = st.columns(2)
    with preview_left:
        st.markdown("**Первый файл**")
        st.dataframe(first.head(20), use_container_width=True, hide_index=True)
        st.caption(f"Строк: {len(first):,} · Столбцов: {len(first.columns)}")
    with preview_right:
        st.markdown("**Второй файл**")
        st.dataframe(second.head(20), use_container_width=True, hide_index=True)
        st.caption(f"Строк: {len(second):,} · Столбцов: {len(second.columns)}")

    if not compare_columns:
        st.warning("Вы не выбрали ни одного поля для сравнения. Будут найдены только новые/пропавшие записи и дубли.")

    if st.button("Сверить файлы", type="primary", use_container_width=True):
        try:
            result = reconcile(first, second, key, compare_columns=compare_columns)
        except Exception as exc:
            st.error(f"Не удалось выполнить сверку: {exc}")
            st.stop()

        st.session_state["reconciliation_result"] = result
        st.session_state["reconciliation_key"] = key
        st.session_state["reconciliation_compare_columns"] = compare_columns

if "reconciliation_result" in st.session_state:
    result = st.session_state["reconciliation_result"]
    key = st.session_state["reconciliation_key"]
    compare_columns = st.session_state.get("reconciliation_compare_columns", [])

    only_first_count = metric_value(result, "only_first")
    only_second_count = metric_value(result, "only_second")
    changed_count = metric_value(result, "changed_common_keys")
    changed_fields_count = metric_value(result, "changed_fields")
    dup_first_count = metric_value(result, "duplicate_rows_first")
    dup_second_count = metric_value(result, "duplicate_rows_second")

    st.divider()
    st.subheader("2. Что найдено")
    st.write(
        f"Сверка по **{key}**. Сравнивались поля: "
        + (", ".join(compare_columns) if compare_columns else "никакие — только наличие записей и дубли")
        + "."
    )

    metric_cols = st.columns(5)
    metric_cols[0].metric("Пропали из второго", only_first_count)
    metric_cols[1].metric("Появились во втором", only_second_count)
    metric_cols[2].metric("Изменённые записи", changed_count)
    metric_cols[3].metric("Изменённые поля", changed_fields_count)
    metric_cols[4].metric("Строк-дублей", dup_first_count + dup_second_count)

    if all(value == 0 for value in [only_first_count, only_second_count, changed_count, dup_first_count, dup_second_count]):
        st.success("По выбранным правилам расхождений не найдено.")
    else:
        st.info(
            f"Итог: {only_first_count} записей есть только в первом файле, "
            f"{only_second_count} — только во втором, "
            f"{changed_count} записей изменились, "
            f"обнаружено {dup_first_count + dup_second_count} строк-дублей."
        )

    tab_changes, tab_first, tab_second, tab_duplicates, tab_summary = st.tabs(
        ["Что изменилось", "Только в первом", "Только во втором", "Дубли", "Сводка"]
    )

    with tab_changes:
        changes = result["changes"]
        if changes.empty:
            st.success("В выбранных полях изменений нет.")
        else:
            visible_changes = changes.rename(
                columns={key: "Ключ", "field": "Поле", "before": "Было", "after": "Стало"}
            )
            st.dataframe(visible_changes, use_container_width=True, hide_index=True)

    with tab_first:
        if result["only_first"].empty:
            st.success("Нет записей, которые присутствуют только в первом файле.")
        else:
            st.caption("Эти записи есть в первом файле, но отсутствуют во втором.")
            st.dataframe(result["only_first"], use_container_width=True, hide_index=True)

    with tab_second:
        if result["only_second"].empty:
            st.success("Нет записей, которые появились только во втором файле.")
        else:
            st.caption("Эти записи есть во втором файле, но отсутствуют в первом.")
            st.dataframe(result["only_second"], use_container_width=True, hide_index=True)

    with tab_duplicates:
        left_dup, right_dup = st.columns(2)
        with left_dup:
            st.markdown("**Дубли в первом файле**")
            if result["duplicates_first"].empty:
                st.success("Нет")
            else:
                st.dataframe(result["duplicates_first"], use_container_width=True, hide_index=True)
        with right_dup:
            st.markdown("**Дубли во втором файле**")
            if result["duplicates_second"].empty:
                st.success("Нет")
            else:
                st.dataframe(result["duplicates_second"], use_container_width=True, hide_index=True)

    with tab_summary:
        st.dataframe(result["human_summary"], use_container_width=True, hide_index=True)

    st.subheader("3. Скачать результат")
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
st.caption("Файлы обрабатываются в памяти приложения. Для публичного размещения нужно отдельно настроить хранение, удаление и конфиденциальность клиентских данных.")
