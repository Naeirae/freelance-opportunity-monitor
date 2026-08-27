const SheetsClient = (() => {
  const API = 'https://sheets.googleapis.com/v4/spreadsheets';

  function getToken(interactive = true) {
    return new Promise((resolve, reject) => {
      chrome.identity.getAuthToken({ interactive }, (token) => {
        if (chrome.runtime.lastError) {
          reject(new Error(chrome.runtime.lastError.message));
          return;
        }
        if (!token) {
          reject(new Error('Google OAuth token was not returned'));
          return;
        }
        resolve(token);
      });
    });
  }

  async function apiFetch(url, options = {}, retryAuth = true) {
    const token = await getToken(true);
    const response = await fetch(url, {
      ...options,
      headers: {
        ...(options.headers || {}),
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    if (response.status === 401 && retryAuth) {
      await new Promise((resolve) => chrome.identity.removeCachedAuthToken({ token }, resolve));
      return apiFetch(url, options, false);
    }

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      const message = body?.error?.message || `${response.status} ${response.statusText}`;
      throw new Error(message);
    }
    return body;
  }

  async function getHeader(spreadsheetId, sheetName) {
    const range = encodeURIComponent(`'${sheetName.replaceAll("'", "''")}'!1:1`);
    const data = await apiFetch(`${API}/${encodeURIComponent(spreadsheetId)}/values/${range}`);
    return data.values?.[0] || [];
  }

  async function appendRow(spreadsheetId, sheetName, row) {
    const range = encodeURIComponent(`'${sheetName.replaceAll("'", "''")}'!A:Z`);
    const url = `${API}/${encodeURIComponent(spreadsheetId)}/values/${range}:append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS&includeValuesInResponse=true`;
    return apiFetch(url, {
      method: 'POST',
      body: JSON.stringify({ majorDimension: 'ROWS', values: [row] })
    });
  }

  async function readRange(spreadsheetId, a1Range) {
    const range = encodeURIComponent(a1Range);
    return apiFetch(`${API}/${encodeURIComponent(spreadsheetId)}/values/${range}`);
  }

  function normalizeValue(value) {
    if (value === null || value === undefined) return '';
    return String(value);
  }

  async function appendAndVerify(spreadsheetId, sheetName, row) {
    const result = await appendRow(spreadsheetId, sheetName, row);
    const updatedRange = result?.updates?.updatedRange;
    if (!updatedRange) throw new Error('Sheets API did not return updatedRange');

    const reread = await readRange(spreadsheetId, updatedRange);
    const actual = (reread.values && reread.values[0]) || [];
    const expected = row.map(normalizeValue);
    const normalizedActual = actual.map(normalizeValue);

    const width = Math.max(expected.length, normalizedActual.length);
    for (let i = 0; i < width; i += 1) {
      if ((expected[i] || '') !== (normalizedActual[i] || '')) {
        throw new Error(`Verification failed at column ${i + 1}: expected "${expected[i] || ''}", got "${normalizedActual[i] || ''}"`);
      }
    }

    return { ok: true, updatedRange, actual: normalizedActual };
  }

  return { getToken, getHeader, appendAndVerify };
})();
