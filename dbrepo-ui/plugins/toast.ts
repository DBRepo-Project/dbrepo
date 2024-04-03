import VueToast, {useToast} from 'vue-toast-notification';
import 'vue-toast-notification/dist/theme-default.css';

const config: any = {
  position: 'top-right',
  duration: 6000,
  dismissible: false /* allow copy of error message */
}

export default defineNuxtPlugin((app) => {
  app.vueApp.use(VueToast, config)
})

/* export composition API https://www.npmjs.com/package/vue-toast-notification#composition-api-usage */
export {
  useToast,
  config
}
