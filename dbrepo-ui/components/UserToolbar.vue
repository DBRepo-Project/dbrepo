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
      <v-tab to="/user/developer">
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
    }
  }
}
</script>
