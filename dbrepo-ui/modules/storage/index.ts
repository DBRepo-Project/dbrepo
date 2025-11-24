import { addServerPlugin, createResolver, defineNuxtModule } from '@nuxt/kit'

export default defineNuxtModule({
  setup () {
    const { resolve } = createResolver(import.meta.url)
    addServerPlugin(resolve('./runtime/plugin.ts'))
  },
})
