from fom.kwork import parse_projects_from_html


def test_parse_projects_from_state_data():
    html = '''
    <html><head><script>
    window.ORIGIN_URL = "https://kwork.ru";
    window.stateData = {"wantsListData":{"wants":[
      {"id":123,"name":"Python Excel automation","description":"Сверить две таблицы CSV","priceLimit":"5000","dateCreate":"2026-08-25 03:00:00"},
      {"id":124,"name":"Telegram bot","description":"Нужен бот с API","priceLimit":10000}
    ]}};
    </script></head></html>
    '''

    items = parse_projects_from_html(html)

    assert len(items) == 2
    assert items[0].source == "kwork"
    assert items[0].title == "Python Excel automation"
    assert items[0].budget_rub == 5000
    assert items[0].url == "https://kwork.ru/projects/123/view"
    assert items[0].published_at == "2026-08-25 03:00:00"
    assert items[1].budget_rub == 10000


def test_parser_skips_projects_without_id_or_title():
    html = '''<script>window.stateData = {"wantsListData":{"wants":[
      {"id":null,"name":"No id"},
      {"id":555,"name":""},
      {"id":556,"name":"Valid","description":"x","priceLimit":null}
    ]}};</script>'''

    items = parse_projects_from_html(html)

    assert [item.url for item in items] == ["https://kwork.ru/projects/556/view"]
    assert items[0].budget_rub is None
