<template>
  <div>
    <v-toolbar title="Settings" flat>
      <template v-slot:extension>
        <v-tabs
          v-model="tab"
          color="primary">
          <v-tab
            :text="$t('toolbars.user.info')"
            to="/user/info" />
          <v-tab
            :text="$t('toolbars.user.authentication')"
            to="/user/authentication" />
          <v-tab
            :text="$t('toolbars.user.developer')"
            to="/user/developer" />
        </v-tabs>
      </template>
    </v-toolbar>
  </div>
</template>

<script>
import { useUserStore } from '@/stores/user.js'

export default {
  data () {
    return {
      tab: null,
      userStore: useUserStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    roles () {
      return this.userStore.getRoles
    }
  }
}
</script>
