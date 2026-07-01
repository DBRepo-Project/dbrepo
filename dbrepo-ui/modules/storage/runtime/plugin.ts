import redis from "unstorage/drivers/redis";

function booleanValue(value: string | boolean | undefined, fallback: boolean): boolean {
  if (typeof value === "boolean") {
    return value;
  }
  if (value === undefined) {
    return fallback;
  }
  return value === "true";
}

function numberValue(value: string | number | null | undefined, fallback: number | null): number | null {
  if (typeof value === "number") {
    return value;
  }
  if (value === undefined || value === null) {
    return fallback;
  }
  return Number(value);
}

export default defineNitroPlugin(() => {
  const storage = useStorage();
  const config = useRuntimeConfig();
  const oidc = config.storage.oidc;
  storage.mount("oidc", redis({
    host: process.env.NUXT_STORAGE_OIDC_HOST ?? oidc.host,
    port: numberValue(process.env.NUXT_STORAGE_OIDC_PORT, oidc.port),
    username: process.env.NUXT_STORAGE_OIDC_USERNAME ?? oidc.username,
    password: process.env.NUXT_STORAGE_OIDC_PASSWORD ?? oidc.password,
    tls: booleanValue(process.env.NUXT_STORAGE_OIDC_TLS, oidc.tls) as any,
    base: process.env.NUXT_STORAGE_OIDC_BASE ?? oidc.base,
    ttl: numberValue(process.env.NUXT_STORAGE_OIDC_TTL, oidc.ttl),
  }));
});
