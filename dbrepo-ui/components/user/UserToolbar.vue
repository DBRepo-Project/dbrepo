<template>
  <div
    v-if="loggedIn">
    <v-toolbar
      title="Settings"
      flat>
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
        </v-tabs>
      </template>
    </v-toolbar>
  </div>
</template>

<script setup>
const { loggedIn, user, login, logout } = useOidcAuth()
</script>
<script>
export default {
  data () {
    return {
      tab: null,
    }
  }
}
</script>
