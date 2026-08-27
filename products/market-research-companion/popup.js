const EXPECTED_HEADERS = [
  'Date', 'Market', 'Language', 'Query', 'Query family', 'Source', 'Result type', 'URL', 'Domain', 'Title',
  'Commercial intent 0–3', 'Pain signal 0–3', 'AI resistance 0–5', 'Competitor', 'Price', 'Key promise',
  'Gap / complaint', 'Evidence note', 'Status', 'Next check', 'Signal ID'
];

const statusEl = document.getElementById('status');
const spreadsheetIdEl = document.getElementById('spreadsheetId');
const sheetNameEl = document.getElementById('sheetName');

function setStatus(message) { statusEl.textContent = message; }
function uuid() { return crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`; }
function storageGet(keys) { return new Promise((resolve) => chrome.storage.local.get(keys, resolve)); }
function storageSet(value) { return new Promise((resolve) => chrome.storage.local.set(value, resolve)); }

async function loadSettings() {
  const data = await storageGet(['spreadsheetId', 'sheetName']);
  spreadsheetIdEl.value = data.spreadsheetId || '1sOpk8dQB9VCK4n3YK7vQ760Y-L4oxulavZHgqcdedOI';
  sheetNameEl.value = data.sheetName || 'Сигналы';
}

async function saveSettings() {
  await storageSet({ spreadsheetId: spreadsheetIdEl.value.trim(), sheetName: sheetNameEl.value.trim() || 'Сигналы' });
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
  return [
    s.date, s.market, s.language, s.query, s.queryFamily, s.source, s.resultType, s.url, s.domain, s.title,
    s.commercialIntent, s.painSignal, s.aiResistance, s.competitor, s.price, s.keyPromise, s.gap,
    s.evidenceNote, s.status, s.nextCheck, s.signalId
  ];
}

async function ensureHeader(spreadsheetId, sheetName) {
  const header = await SheetsClient.getHeader(spreadsheetId, sheetName);
  if (!header.length) throw new Error(`Лист ${sheetName} пуст.`);
  const mismatch = EXPECTED_HEADERS.find((name, index) => (header[index] || '') !== name);
  if (mismatch) throw new Error(`Схема таблицы не совпадает. Ожидался столбец: ${mismatch}`);
}

async function enqueue(signal) {
  const { outbox = [] } = await storageGet(['outbox']);
  outbox.push(signal);
  await storageSet({ outbox });
}

async function flushOutbox() {
  const settings = await storageGet(['spreadsheetId', 'sheetName', 'outbox']);
  const spreadsheetId = settings.spreadsheetId || '1sOpk8dQB9VCK4n3YK7vQ760Y-L4oxulavZHgqcdedOI';
  const sheetName = settings.sheetName || 'Сигналы';
  const outbox = settings.outbox || [];
  if (!outbox.length) { setStatus('Очередь пуста.'); return; }

  await ensureHeader(spreadsheetId, sheetName);
  for (let i = 0; i < outbox.length; i += 1) {
    const signal = outbox[i];
    try {
      const verified = await SheetsClient.appendAndVerify(spreadsheetId, sheetName, signalToRow(signal));
      await storageSet({ outbox: outbox.slice(i + 1) });
      setStatus(`Записано и проверено: ${signal.signalId}\n${verified.updatedRange}`);
    } catch (error) {
      await storageSet({ outbox: outbox.slice(i) });
      throw error;
    }
  }
}

async function captureSignal() {
  const tab = await currentTab();
  const page = await pageData(tab.id);
  const signal = {
    date: new Date().toISOString(),
    market: document.getElementById('market').value.trim(),
    language: document.getElementById('language').value.trim(),
    query: document.getElementById('query').value.trim(),
    queryFamily: document.getElementById('queryFamily').value.trim(),
    source: page.domain,
    resultType: document.getElementById('resultType').value,
    url: page.url,
    domain: page.domain,
    title: page.title,
    commercialIntent: document.getElementById('commercialIntent').value.trim(),
    painSignal: document.getElementById('painSignal').value.trim(),
    aiResistance: document.getElementById('aiResistance').value.trim(),
    competitor: document.getElementById('competitor').value.trim(),
    price: document.getElementById('price').value.trim(),
    keyPromise: document.getElementById('keyPromise').value.trim(),
    gap: document.getElementById('gap').value.trim(),
    evidenceNote: document.getElementById('evidenceNote').value.trim(),
    status: 'new',
    nextCheck: '',
    signalId: uuid()
  };

  await enqueue(signal);
  setStatus(`Сигнал сохранён локально: ${signal.signalId}\nОтправляю…`);
  await flushOutbox();
}

document.getElementById('saveSettings').addEventListener('click', () => saveSettings().catch((e) => setStatus(e.message)));
document.getElementById('auth').addEventListener('click', () => SheetsClient.getToken(true).then(() => setStatus('Google подключён.')).catch((e) => setStatus(e.message)));
document.getElementById('capture').addEventListener('click', () => captureSignal().catch((e) => setStatus(`Ошибка: ${e.message}\nСигнал оставлен в локальной очереди.`)));
document.getElementById('flush').addEventListener('click', () => flushOutbox().catch((e) => setStatus(`Ошибка: ${e.message}`)));

loadSettings().catch((e) => setStatus(e.message));
