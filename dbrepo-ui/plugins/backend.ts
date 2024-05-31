export default defineNuxtPlugin((nuxtApp) => {
  const config = useRuntimeConfig();
  nuxtApp.provide('backendURL', () => {
    if (process.server && !process.dev) {
      return config.public.backendURLServer;
    } else {
      return config.public.backendURL;
    }
  });
})
