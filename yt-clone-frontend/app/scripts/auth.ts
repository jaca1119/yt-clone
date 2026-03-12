const AUTH_SERVER = "http://localhost:9090";
const CLIENT_ID = "react-spa";

export function generateRandomString(length = 43) {
  const array = new Uint8Array(length);
  crypto.getRandomValues(array);
  return btoa(String.fromCharCode(...array))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

export async function generateCodeChallange(codeVerifier: string) {
  const encoder = new TextEncoder();
  const data = encoder.encode(codeVerifier);
  const hash = await crypto.subtle.digest("SHA-256", data);
  const base64 = btoa(String.fromCharCode(...new Uint8Array(hash)));
  return base64.replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
}

export async function redirectToOauth2Authorization() {
  const state = generateRandomString(32);
  const verifier = generateRandomString(64);
  sessionStorage.setItem(
    "auth",
    JSON.stringify({
      state: state,
      verifier: verifier,
    }),
  );
  const codeChallange = await generateCodeChallange(verifier);
  const params = new URLSearchParams({
    response_type: "code",
    client_id: CLIENT_ID,
    redirect_uri: "http://localhost:5173/callback",
    scope: "openid profile",
    state: state,
    code_challenge: codeChallange,
    code_challenge_method: "S256",
  });
  window.location.href = `${AUTH_SERVER}/oauth2/authorize?${params.toString()}`;
}

export async function handleCallback() {
  const authItem = sessionStorage.getItem("auth");
  sessionStorage.removeItem("auth");
  if (!authItem) {
    console.error("no auth saved");
    return;
  }
  const auth: { state: string; verifier: string } = JSON.parse(authItem);
  const params = new URLSearchParams(window.location.search);
  const code = params.get("code");
  const state = params.get("state");

  if (!code || state !== auth.state || !auth.verifier) {
    console.error("Wrong callback");
    return;
  }

  const tokenResponse = await fetch(`${AUTH_SERVER}/oauth2/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "authorization_code",
      client_id: CLIENT_ID,
      code: code,
      redirect_uri: "http://localhost:5173/callback",
      code_verifier: auth.verifier,
    }),
  });

  if (!tokenResponse.ok) {
    console.error("Token request error");
    return;
  }

  const tokens = await tokenResponse.json();
  //TODO this should be in BFF with httpOnly cookie, or Inmemory with state, but for dev it's ok
  sessionStorage.setItem("access_token", tokens.access_token);
  sessionStorage.setItem("id_token", tokens.id_token);
  if (tokens.refresh_token) {
    //spring authorization server don't send refresh_token when client is public
    sessionStorage.setItem("refresh_token", tokens.refresh_token);
  }

  window.history.replaceState({}, document.title, "/");
  window.location.href = "/";
}

export function isLoggedin() {
  return !!sessionStorage.getItem("access_token");
}

export function getAccessToken() {
  return sessionStorage.getItem("access_token") || "";
}

export function logout() {
  sessionStorage.removeItem("access_token");
  sessionStorage.removeItem("id_token");
  sessionStorage.removeItem("refresh_token");
}
