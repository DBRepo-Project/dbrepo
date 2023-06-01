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
      <v-tab v-if="canHandleMessages" to="/user/developer">
        Developer
      </v-tab>
    </v-tabs>
  </div>
</template>

<script>

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
    roles () {
      return this.$store.state.roles
    },
    canCreateMessage () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-maintenance-message')
    },
    canModifyMessage () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('modify-maintenance-message')
    },
    canHandleMessages () {
      return this.canCreateMessage || this.canModifyMessage
    }
  }
}
</script>
