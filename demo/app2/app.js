const express = require('express');
const session = require('express-session');
const { Issuer, generators } = require('openid-client');

const PORT = 8082;
const ISSUER_URL = process.env.ISSUER_URL || 'http://localhost:8080';
const CLIENT_ID = 'demo-app2';
const CLIENT_SECRET = 'demo-app2-secret';
const REDIRECT_URI = `http://localhost:${PORT}/callback`;

const app = express();

app.use(session({
  secret: 'demo-app2-session-secret',
  resave: false,
  saveUninitialized: false,
}));

let client;

async function initClient() {
  const issuer = await Issuer.discover(ISSUER_URL);
  client = new issuer.Client({
    client_id: CLIENT_ID,
    client_secret: CLIENT_SECRET,
    redirect_uris: [REDIRECT_URI],
    response_types: ['code'],
  });
  console.log(`OIDC client initialized. Issuer: ${issuer.issuer}`);
}

app.get('/', (req, res) => {
  if (!req.session.user) {
    res.send(`
      <h1>Demo App 2 (Node.js/Express)</h1>
      <p>You are not logged in.</p>
      <a href="/login">Login with TuenSSO</a>
    `);
  } else {
    const u = req.session.user;
    res.send(`
      <h1>Demo App 2 (Node.js/Express)</h1>
      <p>Logged in via TuenSSO!</p>
      <pre>${JSON.stringify(u, null, 2)}</pre>
      <a href="/logout">Logout</a>
    `);
  }
});

app.get('/login', (req, res) => {
  const nonce = generators.nonce();
  const state = generators.state();
  req.session.nonce = nonce;
  req.session.state = state;
  const url = client.authorizationUrl({ scope: 'openid profile email', state, nonce });
  res.redirect(url);
});

app.get('/callback', async (req, res) => {
  try {
    const params = client.callbackParams(req);
    const tokenSet = await client.callback(REDIRECT_URI, params, {
      nonce: req.session.nonce,
      state: req.session.state,
    });
    const userinfo = await client.userinfo(tokenSet.access_token);
    req.session.user = userinfo;
    delete req.session.nonce;
    delete req.session.state;
    res.redirect('/');
  } catch (err) {
    console.error('Callback error:', err);
    res.status(500).send('Authentication failed: ' + err.message);
  }
});

app.get('/logout', (req, res) => {
  req.session.destroy(() => res.redirect('/'));
});

initClient().then(() => {
  app.listen(PORT, () => console.log(`Demo App 2 running on http://localhost:${PORT}`));
}).catch(err => {
  console.error('Failed to initialize OIDC client:', err.message);
  console.error('Make sure TuenSSO is running on', ISSUER_URL);
  process.exit(1);
});
