<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        Settings
      </v-toolbar-title>
    </v-toolbar>
    <v-tabs v-model="tab" color="primary">
      <v-tab to="/user/info">
        Info
      </v-tab>
      <v-tab to="/user/authentication">
        Authentication
      </v-tab>
      <v-tab v-if="false" to="/user/developer">
        Developer
      </v-tab>
    </v-tabs>
  </div>
</template>

<script>
import { isDeveloper, isResearcher } from '@/utils'

export default {
  data () {
    return {
      tab: null
    }
  },
  computed: {
    user () {
      return this.$store.state.user
    },
    isDeveloper () {
      return isDeveloper(this.user)
    },
    isResearcher () {
      return isResearcher(this.user)
    }
  }
}
</script>
