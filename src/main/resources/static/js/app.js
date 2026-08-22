(function () {
  'use strict';

  var root = document.getElementById('app');

  var state = {
    view: 'login',
    booting: true,
    bootError: '',
    users: [],
    transactions: [],
    currentUserId: null,
    loginEmail: '',
    loginPassword: '',
    loginError: '',
    loginLoading: false,
    signup: {
      firstName: '', lastName: '', document: '', email: '', password: '', balance: '',
      userType: 'COMMON', error: '', loading: false
    },
    transfer: { payeeId: '', amount: '', step: 'form', error: '', result: null }
  };

  function setState(patch) {
    Object.assign(state, patch);
    render();
  }

  // ---------- API layer ----------

  function extractErrorMessage(body, status) {
    if (!body) return 'Erro ' + status + ' ao comunicar com a API.';
    if (Array.isArray(body.details) && body.details.length) {
      return body.details.map(function (d) { return d.field + ': ' + d.message; }).join(' · ');
    }
    return body.message || ((body.error || 'Erro') + ' (' + status + ')');
  }

  function apiRequest(path, options) {
    return fetch(path, options).then(function (res) {
      return res.text().then(function (text) {
        var body = null;
        try { body = text ? JSON.parse(text) : null; } catch (e) { /* non-JSON body */ }
        if (!res.ok) {
          var err = new Error(extractErrorMessage(body, res.status));
          err.status = res.status;
          err.body = body;
          throw err;
        }
        return body;
      });
    });
  }

  function fetchUsers() { return apiRequest('/users'); }
  function fetchTransactions() { return apiRequest('/transactions'); }
  function createUser(payload) {
    return apiRequest('/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
  }
  function postTransfer(payload) {
    return apiRequest('/transfer', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
  }
  function login(payload) {
    return apiRequest('/users/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
  }

  // ---------- formatting ----------

  function toNumber(str) {
    if (str == null) return NaN;
    var s = String(str).trim();
    if (s === '') return NaN;
    if (s.indexOf(',') !== -1) {
      s = s.replace(/\./g, '').replace(',', '.');
    }
    return parseFloat(s);
  }

  function fmtMoney(v) {
    var n = Number(v || 0);
    return 'R$ ' + n.toFixed(2).replace('.', ',').replace(/\B(?=(\d{3})+(?!\d)(?=[,.]))/g, '.');
  }

  function fmtDate(iso) {
    var d = new Date(iso);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleDateString('pt-BR') + ' às ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  }

  function initials(u) {
    if (!u) return '?';
    return ((u.firstName ? u.firstName[0] : '') + (u.lastName ? u.lastName[0] : '')).toUpperCase();
  }

  function fullName(u) { return u ? (u.firstName + ' ' + u.lastName) : '—'; }

  function findUser(id) {
    var idNum = Number(id);
    return state.users.filter(function (u) { return u.id === idNum; })[0] || null;
  }

  // ---------- icons ----------

  var ICON = {
    logo: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M4 14L12 6L20 14" stroke="#0a0f0d" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M12 6V20" stroke="#0a0f0d" stroke-width="2.5" stroke-linecap="round"/></svg>',
    transfer: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M7 7H17M17 7L13 3M17 7L13 11" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M17 17H7M7 17L11 13M7 17L11 21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    users: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none"><circle cx="9" cy="8" r="3" stroke="currentColor" stroke-width="2"/><path d="M3 20c0-3 2.5-5 6-5s6 2 6 5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><circle cx="17" cy="9" r="2.3" stroke="currentColor" stroke-width="1.8"/><path d="M15 20c0-2.2 1-4 3-4.6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
    info: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/><path d="M12 11v5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><circle cx="12" cy="8" r="1.2" fill="currentColor"/></svg>',
    check: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none"><path d="M4 12.5L9.5 18L20 6" stroke="oklch(0.78 0.19 152)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    alert: '<svg width="26" height="26" viewBox="0 0 24 24" fill="none"><path d="M12 3L22 20H2L12 3Z" stroke="oklch(0.66 0.21 25)" stroke-width="2" stroke-linejoin="round"/><path d="M12 10v4" stroke="oklch(0.66 0.21 25)" stroke-width="2" stroke-linecap="round"/><circle cx="12" cy="17" r="1" fill="oklch(0.66 0.21 25)"/></svg>',
    spinner: '<svg class="spinner" width="40" height="40" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="var(--border)" stroke-width="3"/><path d="M21 12a9 9 0 0 0-9-9" stroke="oklch(0.78 0.19 152)" stroke-width="3" stroke-linecap="round"/></svg>'
  };

  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  // ---------- view fragments ----------

  function headerHtml() {
    var user = findUser(state.currentUserId);
    if (!user) return '';
    var isMerchant = user.userType === 'MERCHANT';
    var avatarBg = isMerchant ? 'var(--accent-2)' : 'var(--accent)';
    return '' +
      '<header class="header">' +
      '  <div style="display:flex; align-items:center; gap:36px;">' +
      '    <div class="brand"><div class="logo-mark">' + ICON.logo + '</div><span class="display" style="font-size:18px; font-weight:700; letter-spacing:-0.02em;">PicPay Simplificado</span></div>' +
      '    <nav class="nav">' +
      '      <button type="button" class="btn-nav ' + (state.view === 'dashboard' ? 'active' : '') + '" data-action="go-dashboard">Dashboard</button>' +
      (isMerchant ? '' : '      <button type="button" class="btn-nav ' + (state.view === 'transfer' ? 'active' : '') + '" data-action="go-transfer">Transferir</button>') +
      '      <button type="button" class="btn-nav ' + (state.view === 'users' ? 'active' : '') + '" data-action="go-users">Usuários</button>' +
      '    </nav>' +
      '  </div>' +
      '  <div style="display:flex; align-items:center; gap:14px;">' +
      '    <div class="user-chip">' +
      '      <div class="avatar" style="background:' + avatarBg + ';">' + esc(initials(user)) + '</div>' +
      '      <div style="display:flex; flex-direction:column; line-height:1.25;">' +
      '        <span style="font-size:13px; font-weight:600;">' + esc(fullName(user)) + '</span>' +
      '        <span style="font-size:11px; color:var(--text-dim);">' + (isMerchant ? 'Lojista' : 'Comum') + '</span>' +
      '      </div>' +
      '    </div>' +
      '    <button type="button" class="btn btn-outline" style="padding:8px 14px; font-size:13px;" data-action="logout">Sair</button>' +
      '  </div>' +
      '</header>';
  }

  function loginView() {
    return '' +
      '<div class="center-screen" style="background: radial-gradient(circle at 18% 20%, oklch(0.78 0.19 152 / 0.08), transparent 60%), radial-gradient(circle at 82% 78%, oklch(0.72 0.17 292 / 0.08), transparent 60%), var(--bg);">' +
      '  <div class="auth-card" style="max-width:400px;">' +
      '    <div style="display:flex; flex-direction:column; align-items:center; gap:14px; text-align:center;">' +
      '      <div class="logo-mark" style="width:44px; height:44px; border-radius:12px;"><svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M4 14L12 6L20 14" stroke="#0a0f0d" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M12 6V20" stroke="#0a0f0d" stroke-width="2.5" stroke-linecap="round"/></svg></div>' +
      '      <div><div class="display" style="font-size:22px; font-weight:700;">PicPay Simplificado</div><div style="font-size:13px; color:var(--text-dim); margin-top:4px;">Entre para acessar sua carteira</div></div>' +
      '    </div>' +
      '    <div style="display:flex; flex-direction:column; gap:12px;">' +
      '      <div class="field"><label>E-mail</label><input type="email" data-field="login-email" value="' + esc(state.loginEmail) + '" placeholder="joao@email.com" /></div>' +
      '      <div class="field"><label>Senha</label><input type="password" data-field="login-password" value="' + esc(state.loginPassword) + '" placeholder="••••••••" /></div>' +
      (state.loginError ? '      <div class="alert alert-danger">' + esc(state.loginError) + '</div>' : '') +
      '      <button type="button" class="btn btn-primary" data-action="login-submit" ' + (state.loginLoading ? 'disabled' : '') + '>' + (state.loginLoading ? 'Entrando...' : 'Entrar') + '</button>' +
      '    </div>' +
      '    <div class="divider"><div class="line"></div><span>ou</span><div class="line"></div></div>' +
      '    <div style="text-align:center; font-size:13px; color:var(--text-dim);">Não tem conta? <span class="link-action" data-action="go-signup">Criar conta</span></div>' +
      '  </div>' +
      '</div>';
  }

  function signupView() {
    var s = state.signup;
    return '' +
      '<div class="center-screen" style="background: radial-gradient(circle at 82% 20%, oklch(0.72 0.17 292 / 0.08), transparent 60%), radial-gradient(circle at 18% 80%, oklch(0.78 0.19 152 / 0.08), transparent 60%), var(--bg);">' +
      '  <div class="auth-card" style="max-width:460px;">' +
      '    <div><div class="display" style="font-size:20px; font-weight:700;">Criar conta</div><div style="font-size:13px; color:var(--text-dim); margin-top:4px;">Comece a usar sua carteira digital</div></div>' +
      '    <div class="toggle-group">' +
      '      <button type="button" class="toggle-btn ' + (s.userType === 'COMMON' ? 'active-common' : '') + '" data-action="signup-type-common">Pessoa comum</button>' +
      '      <button type="button" class="toggle-btn ' + (s.userType === 'MERCHANT' ? 'active-merchant' : '') + '" data-action="signup-type-merchant">Lojista</button>' +
      '    </div>' +
      '    <div style="display:flex; gap:10px;">' +
      '      <div class="field" style="flex:1;"><label>Nome</label><input data-field="signup-firstName" value="' + esc(s.firstName) + '" placeholder="João" /></div>' +
      '      <div class="field" style="flex:1;"><label>Sobrenome</label><input data-field="signup-lastName" value="' + esc(s.lastName) + '" placeholder="Silva" /></div>' +
      '    </div>' +
      '    <div class="field"><label>Documento (CPF: 11 dígitos · CNPJ: 14 dígitos)</label><input data-field="signup-document" value="' + esc(s.document) + '" placeholder="12345678901" /></div>' +
      '    <div class="field"><label>E-mail</label><input type="email" data-field="signup-email" value="' + esc(s.email) + '" placeholder="voce@email.com" /></div>' +
      '    <div style="display:flex; gap:10px;">' +
      '      <div class="field" style="flex:1;"><label>Senha (mín. 6 caracteres)</label><input type="password" data-field="signup-password" value="' + esc(s.password) + '" placeholder="••••••••" /></div>' +
      '      <div class="field" style="flex:1;"><label>Saldo inicial</label><input data-field="signup-balance" value="' + esc(s.balance) + '" placeholder="0.00" /></div>' +
      '    </div>' +
      (s.error ? '    <div class="alert alert-danger">' + esc(s.error) + '</div>' : '') +
      '    <button type="button" class="btn btn-primary" data-action="signup-submit" ' + (s.loading ? 'disabled' : '') + '>' + (s.loading ? 'Criando...' : 'Criar conta') + '</button>' +
      '    <div style="text-align:center; font-size:13px; color:var(--text-dim);">Já tem conta? <span class="link-action" data-action="go-login">Entrar</span></div>' +
      '  </div>' +
      '</div>';
  }

  function dashboardView() {
    var user = findUser(state.currentUserId);
    if (!user) return '';
    var isMerchant = user.userType === 'MERCHANT';
    var myTx = state.transactions
      .filter(function (t) { return t.payerId === user.id || t.payeeId === user.id; })
      .slice()
      .sort(function (a, b) { return new Date(b.timestamp) - new Date(a.timestamp); });

    var txRowsHtml = myTx.map(function (t) {
      var isOutgoing = t.payerId === user.id;
      var otherId = isOutgoing ? t.payeeId : t.payerId;
      var other = findUser(otherId) || { firstName: (isOutgoing ? t.payeeName : t.payerName) || '—', lastName: '' };
      var amountLabel = (isOutgoing ? '- ' : '+ ') + fmtMoney(t.amount);
      var amountColor = isOutgoing ? '#f3f5f4' : 'var(--accent)';
      var otherName = isOutgoing ? (t.payeeName || fullName(other)) : (t.payerName || fullName(other));
      return '' +
        '<div class="tx-row">' +
        '  <div style="display:flex; align-items:center; gap:12px;">' +
        '    <div class="tx-avatar">' + esc(initials(other)) + '</div>' +
        '    <div style="display:flex; flex-direction:column;"><span style="font-size:14px; font-weight:600;">' + esc(otherName) + '</span><span style="font-size:12px; color:var(--text-faint);">' + fmtDate(t.timestamp) + '</span></div>' +
        '  </div>' +
        '  <div style="display:flex; flex-direction:column; align-items:flex-end; gap:4px;">' +
        '    <span style="font-size:14px; font-weight:700; color:' + amountColor + ';">' + amountLabel + '</span>' +
        '    <span style="font-size:11px; color:var(--text-faint);">' + (t.status === 'COMPLETED' ? 'Concluída' : esc(t.status)) + '</span>' +
        '  </div>' +
        '</div>';
    }).join('');

    return '' +
      '<div class="page"><div class="page-inner">' +
      '  <div class="balance-card">' +
      '    <div class="balance-glow"></div>' +
      '    <div style="position:relative; display:flex; flex-direction:column; gap:20px;">' +
      '      <div style="display:flex; justify-content:space-between; align-items:flex-start;">' +
      '        <div><div style="font-size:13px; color:var(--text-dim); font-weight:600;">Saldo disponível</div><div class="display balance-amount">' + fmtMoney(user.balance) + '</div><div style="font-size:12px; color:var(--text-faint); margin-top:6px;">Documento: ' + esc(user.document) + '</div></div>' +
      '        <div class="badge ' + (isMerchant ? 'badge-merchant' : 'badge-common') + '">' + (isMerchant ? 'Lojista' : 'Comum') + '</div>' +
      '      </div>' +
      '      <div style="display:flex; gap:12px; flex-wrap:wrap;">' +
      (isMerchant ? '' : '        <button type="button" class="btn btn-primary" style="padding:12px 20px;" data-action="go-transfer">' + ICON.transfer + ' Transferir</button>') +
      '        <button type="button" class="btn btn-outline" style="padding:12px 20px;" data-action="go-users">' + ICON.users + ' Ver usuários</button>' +
      '      </div>' +
      (isMerchant ? '      <div style="display:flex; align-items:center; gap:8px; font-size:12px; color:var(--text-dim); background:var(--surface-2); padding:10px 14px; border-radius:10px; border:1px solid var(--border); width:fit-content;">' + ICON.info + ' Contas lojista apenas recebem transferências.</div>' : '') +
      '    </div>' +
      '  </div>' +
      '  <div class="tx-card">' +
      '    <div style="padding:20px 24px 12px; font-size:15px; font-weight:700;">Últimas transferências</div>' +
      (myTx.length ? txRowsHtml : '    <div style="padding:32px 24px; text-align:center; font-size:13px; color:var(--text-faint);">Nenhuma transação ainda.</div>') +
      '  </div>' +
      '</div></div>';
  }

  function transferView() {
    var user = findUser(state.currentUserId);
    if (!user) return '';
    var t = state.transfer;
    var body = '';

    if (t.step === 'form') {
      var options = state.users
        .filter(function (u) { return u.id !== user.id; })
        .map(function (u) {
          return '<option value="' + u.id + '" ' + (String(t.payeeId) === String(u.id) ? 'selected' : '') + '>' + esc(fullName(u)) + (u.userType === 'MERCHANT' ? ' · Lojista' : '') + '</option>';
        }).join('');
      body = '' +
        '<div class="transfer-card">' +
        '  <div><div class="display" style="font-size:20px; font-weight:700;">Nova transferência</div><div style="font-size:13px; color:var(--text-dim); margin-top:4px;">De ' + esc(fullName(user)) + ' · ' + fmtMoney(user.balance) + ' disponível</div></div>' +
        '  <div class="field"><label>Para quem</label><select data-field="transfer-payee"><option value="">Selecione um destinatário</option>' + options + '</select></div>' +
        '  <div class="field"><label>Valor (R$)</label><input data-field="transfer-amount" value="' + esc(t.amount) + '" placeholder="0.00" style="font-size:16px; font-weight:700;" /></div>' +
        (t.error ? '  <div class="alert alert-danger">' + esc(t.error) + '</div>' : '') +
        '  <button type="button" class="btn btn-primary" data-action="transfer-review">Revisar transferência</button>' +
        '</div>';
    } else if (t.step === 'confirm') {
      var payee = findUser(t.payeeId);
      body = '' +
        '<div class="transfer-card">' +
        '  <div class="display" style="font-size:20px; font-weight:700;">Confirmar transferência</div>' +
        '  <div class="transfer-summary">' +
        '    <div class="transfer-summary-row"><span style="color:var(--text-dim); font-size:13px;">De</span><span style="font-size:13px; font-weight:600;">' + esc(fullName(user)) + '</span></div>' +
        '    <div class="transfer-summary-row"><span style="color:var(--text-dim); font-size:13px;">Para</span><span style="font-size:13px; font-weight:600;">' + esc(fullName(payee)) + '</span></div>' +
        '    <div class="transfer-summary-divider"></div>' +
        '    <div class="transfer-summary-row" style="align-items:center;"><span style="color:var(--text-dim); font-size:13px;">Valor</span><span class="display" style="font-size:24px; font-weight:700;">' + fmtMoney(t.amount) + '</span></div>' +
        '  </div>' +
        '  <div style="display:flex; gap:12px;">' +
        '    <button type="button" class="btn btn-outline" style="flex:1;" data-action="transfer-back">Voltar</button>' +
        '    <button type="button" class="btn btn-primary" style="flex:1;" data-action="transfer-confirm">Confirmar</button>' +
        '  </div>' +
        '</div>';
    } else if (t.step === 'authorizing') {
      body = '' +
        '<div class="transfer-card" style="align-items:center; text-align:center; padding:48px 32px;">' +
        ICON.spinner +
        '  <div style="font-size:14px; color:var(--text-dim); font-weight:600; margin-top:18px;">Autorizando transação...</div>' +
        '</div>';
    } else if (t.step === 'success') {
      var r = t.result;
      body = '' +
        '<div class="transfer-card" style="align-items:center; text-align:center; padding:40px 32px;">' +
        '  <div class="status-circle" style="background:var(--accent-soft);">' + ICON.check + '</div>' +
        '  <div class="display" style="font-size:20px; font-weight:700; margin-top:16px;">Transferência concluída</div>' +
        '  <div style="font-size:13px; color:var(--text-dim); margin-top:8px;">ID da transação: ' + esc(r ? r.transactionId : '') + '</div>' +
        '  <div class="display" style="font-size:32px; font-weight:700; margin-top:8px;">' + fmtMoney(r ? r.amount : 0) + '</div>' +
        '  <div style="display:flex; gap:12px; width:100%; margin-top:16px;">' +
        '    <button type="button" class="btn btn-outline" style="flex:1;" data-action="transfer-reset">Nova transferência</button>' +
        '    <button type="button" class="btn btn-primary" style="flex:1;" data-action="go-dashboard">Ir para dashboard</button>' +
        '  </div>' +
        '</div>';
    } else if (t.step === 'error') {
      body = '' +
        '<div class="transfer-card" style="align-items:center; text-align:center; padding:40px 32px; border-color:var(--danger);">' +
        '  <div class="status-circle" style="background:var(--danger-soft);">' + ICON.alert + '</div>' +
        '  <div class="display" style="font-size:20px; font-weight:700; margin-top:16px;">Não foi possível transferir</div>' +
        '  <div style="font-size:13px; color:var(--text-dim); margin-top:8px;">' + esc(t.error) + '</div>' +
        '  <button type="button" class="btn btn-primary" style="width:100%; margin-top:16px;" data-action="transfer-back">Tentar novamente</button>' +
        '</div>';
    }

    return '<div class="page" style="display:flex; justify-content:center;"><div style="width:100%; max-width:480px;">' + body + '</div></div>';
  }

  function usersView() {
    var rows = state.users.map(function (u) {
      var isMerchant = u.userType === 'MERCHANT';
      return '' +
        '<div class="table-row">' +
        '  <span style="font-weight:600;">' + esc(fullName(u)) + '</span>' +
        '  <span style="color:var(--text-dim);">' + esc(u.document) + '</span>' +
        '  <span style="color:var(--text-dim);">' + esc(u.email) + '</span>' +
        '  <span class="badge badge-sm ' + (isMerchant ? 'badge-merchant' : 'badge-common') + '">' + (isMerchant ? 'Lojista' : 'Comum') + '</span>' +
        '  <span class="text-right" style="font-weight:700;">' + fmtMoney(u.balance) + '</span>' +
        '</div>';
    }).join('');

    return '' +
      '<div class="page"><div class="page-inner">' +
      '  <div><div class="display" style="font-size:22px; font-weight:700;">Usuários cadastrados</div><div style="font-size:13px; color:var(--text-dim); margin-top:4px;">' + state.users.length + ' contas na plataforma</div></div>' +
      '  <div class="table-card">' +
      '    <div class="table-header"><span>Nome</span><span>Documento</span><span>E-mail</span><span>Tipo</span><span class="text-right">Saldo</span></div>' +
      rows +
      '  </div>' +
      '</div></div>';
  }

  // ---------- render ----------

  function render() {
    var bootBanner = state.bootError ?
      '<div class="boot-banner"><span>' + esc(state.bootError) + '</span><button type="button" class="btn btn-outline" style="padding:8px 14px; font-size:12px;" data-action="retry-boot">Tentar novamente</button></div>' : '';

    var body;
    if (state.booting) {
      body = '<div class="center-screen">' + ICON.spinner + '</div>';
    } else if (!state.currentUserId) {
      body = state.view === 'signup' ? signupView() : loginView();
    } else if (state.view === 'transfer') {
      body = transferView();
    } else if (state.view === 'users') {
      body = usersView();
    } else {
      body = dashboardView();
    }

    root.innerHTML = '<div class="app-shell">' + headerHtml() + bootBanner + '<div style="flex:1; display:flex; flex-direction:column;">' + body + '</div></div>';
  }

  // ---------- actions ----------

  function goTo(view) { setState({ view: view }); }

  function goToTransferFresh() {
    setState({ view: 'transfer', transfer: { payeeId: '', amount: '', step: 'form', error: '', result: null } });
  }

  function refreshUsersAndTransactions() {
    return Promise.all([fetchUsers(), fetchTransactions()]).then(function (results) {
      setState({ users: results[0] || [], transactions: results[1] || [] });
    });
  }

  function loginSubmit() {
    var email = state.loginEmail.trim();
    var password = state.loginPassword.trim();
    if (!email && !password) {
      setState({ loginError: 'Informe e-mail e senha.' });
      return;
    }
    if (!email) {
      setState({ loginError: 'Informe o e-mail.' });
      return;
    }
    if (!password) {
      setState({ loginError: 'Informe a senha.' });
      return;
    }
    var knownUser = state.users.filter(function (u) { return u.email.toLowerCase() === email.toLowerCase(); })[0];
    if (!knownUser) {
      setState({ loginError: 'E-mail não encontrado.' });
      return;
    }
    setState({ loginLoading: true, loginError: '' });
    login({ email: email, password: state.loginPassword })
      .then(function (user) {
        return refreshUsersAndTransactions().then(function () {
          setState({
            currentUserId: user.id,
            view: 'dashboard',
            loginEmail: '',
            loginPassword: '',
            loginLoading: false,
            loginError: ''
          });
        });
      })
      .catch(function (err) {
        setState({ loginLoading: false, loginError: 'Senha incorreta.' });
      });
  }

  function logout() {
    setState({
      currentUserId: null,
      view: 'login',
      loginEmail: '',
      loginPassword: '',
      loginError: '',
      loginLoading: false,
      transfer: { payeeId: '', amount: '', step: 'form', error: '', result: null },
      signup: { firstName: '', lastName: '', document: '', email: '', password: '', balance: '', userType: 'COMMON', error: '', loading: false }
    });
  }

  function signupSubmit() {
    var s = state.signup;
    if (!s.firstName.trim() || !s.lastName.trim() || !s.document.trim() || !s.email.trim() || !s.password.trim()) {
      setState({ signup: Object.assign({}, s, { error: 'Preencha todos os campos obrigatórios.' }) });
      return;
    }
    setState({ signup: Object.assign({}, s, { loading: true, error: '' }) });
    createUser({
      firstName: s.firstName.trim(),
      lastName: s.lastName.trim(),
      document: s.document.trim(),
      email: s.email.trim(),
      password: s.password,
      balance: toNumber(s.balance) || 0,
      userType: s.userType
    }).then(function (created) {
      return refreshUsersAndTransactions().then(function () {
        setState({
          currentUserId: created.id,
          view: 'dashboard',
          signup: { firstName: '', lastName: '', document: '', email: '', password: '', balance: '', userType: 'COMMON', error: '', loading: false }
        });
      });
    }).catch(function (err) {
      setState({ signup: Object.assign({}, state.signup, { loading: false, error: err.message }) });
    });
  }

  function transferReview() {
    var t = state.transfer;
    if (!t.payeeId) { setState({ transfer: Object.assign({}, t, { error: 'Selecione para quem transferir.' }) }); return; }
    var amount = toNumber(t.amount);
    if (!amount || amount <= 0) { setState({ transfer: Object.assign({}, t, { error: 'Informe um valor válido.' }) }); return; }
    setState({ transfer: Object.assign({}, t, { step: 'confirm', error: '' }) });
  }

  function transferBack() {
    setState({ transfer: Object.assign({}, state.transfer, { step: 'form', error: '' }) });
  }

  function transferConfirm() {
    var t = state.transfer;
    setState({ transfer: Object.assign({}, t, { step: 'authorizing' }) });
    postTransfer({ value: toNumber(t.amount), payer: state.currentUserId, payee: Number(t.payeeId) })
      .then(function (result) {
        return refreshUsersAndTransactions().then(function () {
          setState({ transfer: Object.assign({}, state.transfer, { step: 'success', result: result }) });
        });
      })
      .catch(function (err) {
        setState({ transfer: Object.assign({}, state.transfer, { step: 'error', error: err.message }) });
      });
  }

  function transferReset() {
    setState({ transfer: { payeeId: '', amount: '', step: 'form', error: '', result: null } });
  }

  function bootApp() {
    setState({ booting: true, bootError: '' });
    refreshUsersAndTransactions()
      .then(function () { setState({ booting: false }); })
      .catch(function (err) {
        setState({
          booting: false,
          bootError: 'Não foi possível conectar à API (' + err.message + '). Verifique se o backend está rodando em http://localhost:8080.'
        });
      });
  }

  // ---------- event delegation ----------

  root.addEventListener('click', function (e) {
    var el = e.target.closest('[data-action]');
    if (!el) return;
    var action = el.dataset.action;
    if (action === 'go-dashboard') goTo('dashboard');
    else if (action === 'go-transfer') goToTransferFresh();
    else if (action === 'go-users') goTo('users');
    else if (action === 'go-login') goTo('login');
    else if (action === 'go-signup') goTo('signup');
    else if (action === 'logout') logout();
    else if (action === 'login-submit') loginSubmit();
    else if (action === 'signup-type-common') setState({ signup: Object.assign({}, state.signup, { userType: 'COMMON' }) });
    else if (action === 'signup-type-merchant') setState({ signup: Object.assign({}, state.signup, { userType: 'MERCHANT' }) });
    else if (action === 'signup-submit') signupSubmit();
    else if (action === 'transfer-review') transferReview();
    else if (action === 'transfer-back') transferBack();
    else if (action === 'transfer-confirm') transferConfirm();
    else if (action === 'transfer-reset') transferReset();
    else if (action === 'retry-boot') bootApp();
  });

  root.addEventListener('input', function (e) {
    var field = e.target.dataset.field;
    if (!field) return;
    if (field === 'login-email') { state.loginEmail = e.target.value; state.loginError = ''; }
    else if (field === 'login-password') { state.loginPassword = e.target.value; state.loginError = ''; }
    else if (field === 'signup-firstName') state.signup.firstName = e.target.value;
    else if (field === 'signup-lastName') state.signup.lastName = e.target.value;
    else if (field === 'signup-document') state.signup.document = e.target.value;
    else if (field === 'signup-email') state.signup.email = e.target.value;
    else if (field === 'signup-password') state.signup.password = e.target.value;
    else if (field === 'signup-balance') state.signup.balance = e.target.value;
    else if (field === 'transfer-amount') { state.transfer.amount = e.target.value; state.transfer.error = ''; }
    // Intentionally not re-rendering on every keystroke to keep input focus stable.
  });

  root.addEventListener('change', function (e) {
    var field = e.target.dataset.field;
    if (field === 'transfer-payee') {
      setState({ transfer: Object.assign({}, state.transfer, { payeeId: e.target.value, error: '' }) });
    }
  });

  render();
  bootApp();
})();
