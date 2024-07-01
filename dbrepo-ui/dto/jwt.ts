interface Token {
  exp: number;
  iat: number;
  jti: string;
  iss: string;
  aud: string;
  sub: string;
  typ: string;
  azp: string;
  session_state: string;
  realm_access: RealmAccess;
  scope: string;
  sid: string;
  uid: string;
  preferred_username: string;
}

interface RealmAccess {
  roles: string[];
}
