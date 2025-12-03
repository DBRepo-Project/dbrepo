import redis from "unstorage/drivers/redis";

export default defineNitroPlugin(() => {
  const storage = useStorage();
  const config = useRuntimeConfig();
  storage.mount("oidc", redis({
    host: config.storage.oidc.host,
    port: config.storage.oidc.port,
    username: config.storage.oidc.username,
    password: config.storage.oidc.password,
    tls: config.storage.oidc.tls as any,
    base: config.storage.oidc.base,
    ttl: config.storage.oidc.ttl,
  }));
});
