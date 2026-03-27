import { User } from "oidc-client-ts";

const AUTH_SERVER = "http://localhost:9090";
const CLIENT_ID = "react-spa";

export function getAccessToken() {
  return getUser()?.access_token;
}

function getUser() {
  const oidcStorage = sessionStorage.getItem(
    `oidc.user:${AUTH_SERVER}:${CLIENT_ID}`,
  );
  if (!oidcStorage) {
    return null;
  }

  return User.fromStorageString(oidcStorage);
}
