const EXPECTED_HEADERS = [
  'signal_id', 'captured_at', 'query', 'market', 'language', 'url', 'title', 'domain',
  'signal_type', 'visible_price', 'problem', 'gap', 'ai_resistance', 'commercial_intent'
];

const statusEl = document.getElementById('status');
const spreadsheetIdEl = document.getElementById('spreadsheetId');
const sheetNameEl = document.getElementById('sheetName');

function setStatus(message) {
  statusEl.textContent = message;
}

function uuid() {
  return crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function storageGet(keys) {
  return new Promise((resolve) => chrome.storage.local.get(keys, resolve));
}

function storageSet(value) {
  return new Promise((resolve) => chrome.storage.local.set(value, resolve));
}

async function loadSettings() {
  const data = await storageGet(['spreadsheetId', 'sheetName']);
  spreadsheetIdEl.value = data.spreadsheetId || '';
  sheetNameEl.value = data.sheetName || 'Sheet1';
}

async function saveSettings() {
  await storageSet({
    spreadsheetId: spreadsheetIdEl.value.trim(),
    sheetName: sheetNameEl.value.trim() || 'Sheet1'
  });
  setStatus('Настройки сохранены.');
}

async function currentTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab?.id) throw new Error('Active tab not found');
  return tab;
}

async function pageData(tabId) {
  const [{ result }] = await chrome.scripting.executeScript({
    target: { tabId },
    func: () => ({ title: document.title, url: location.href, domain: location.hostname })
  });
  return result;
}

function signalToRow(s) {
  return EXPECTED_HEADERS.map((key) => s[key] ?? '');
}

async function ensureHeader(spreadsheetId, sheetName) {
  const header = await SheetsClient.getHeader(spreadsheetId, sheetName);
  if (!header.length) {
    throw new Error(`Лист ${sheetName} пуст. Сначала создайте заголовки:\n${EXPECTED_HEADERS.join(' | ')}`);
  }
  const mismatch = EXPECTED_HEADERS.find((name, index) => (header[index] || '') !== name);
  if (mismatch) {
    throw new Error(`Схема таблицы не совпадает. Ожидался столбец ${mismatch}.`);
  }
}

async function enqueue(signal) {
  const { outbox = [] } = await storageGet(['outbox']);
  outbox.push(signal);
  await storageSet({ outbox });
}

async function flushOutbox() {
  const settings = await storageGet(['spreadsheetId', 'sheetName', 'outbox']);
  const spreadsheetId = settings.spreadsheetId;
  const sheetName = settings.sheetName || 'Sheet1';
  const outbox = settings.outbox || [];
  if (!spreadsheetId) throw new Error('Не задан Spreadsheet ID.');
  if (!outbox.length) {
    setStatus('Очередь пуста.');
    return;
  }

  await ensureHeader(spreadsheetId, sheetName);
  const remaining = [];
  for (const signal of outbox) {
    try {
      const verified = await SheetsClient.appendAndVerify(spreadsheetId, sheetName, signalToRow(signal));
      setStatus(`Записано и проверено: ${signal.signal_id}\n${verified.updatedRange}`);
    } catch (error) {
      remaining.push(signal);
      await storageSet({ outbox: remaining.concat(outbox.slice(outbox.indexOf(signal) + 1)) });
      throw error;
    }
  }
  await storageSet({ outbox: [] });
}

async function captureSignal() {
  const tab = await currentTab();
  const page = await pageData(tab.id);
  const signal = {
    signal_id: uuid(),
    captured_at: new Date().toISOString(),
    query: document.getElementById('query').value.trim(),
    market: document.getElementById('market').value.trim(),
    language: document.getElementById('language').value.trim(),
    url: page.url,
    title: page.title,
    domain: page.domain,
    signal_type: document.getElementById('signalType').value,
    visible_price: document.getElementById('visiblePrice').value.trim(),
    problem: document.getElementById('problem').value.trim(),
    gap: document.getElementById('gap').value.trim(),
    ai_resistance: document.getElementById('aiResistance').value.trim(),
    commercial_intent: document.getElementById('commercialIntent').value.trim()
  };
  await enqueue(signal);
  setStatus(`Сигнал сохранён локально: ${signal.signal_id}\nОтправляю…`);
  await flushOutbox();
}

document.getElementById('saveSettings').addEventListener('click', () => saveSettings().catch((e) => setStatus(e.message)));
document.getElementById('auth').addEventListener('click', () => SheetsClient.getToken(true).then(() => setStatus('Google подключён.')).catch((e) => setStatus(e.message)));
document.getElementById('capture').addEventListener('click', () => captureSignal().catch((e) => setStatus(`Ошибка: ${e.message}\nСигнал оставлен в локальной очереди.`)));
document.getElementById('flush').addEventListener('click', () => flushOutbox().catch((e) => setStatus(`Ошибка: ${e.message}`)));

loadSettings().catch((e) => setStatus(e.message));
