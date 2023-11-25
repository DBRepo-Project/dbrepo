<template>
  <div>
    <v-card flat tile>
      <v-card-text v-if="infoLinks && infoLinks.length > 0">
        <div class="mb-2">Important Links</div>
        <div class="text--primary">
          <ul>
            <li v-for="(link, i) in infoLinks" :key="i">
              <a :href="link.href" :target="link.blank ? '_blank' : 'self'">
                {{ link.text }} <sup v-if="link.blank"><v-icon color="primary" x-small>mdi-open-in-new</v-icon></sup>
              </a>
            </li>
          </ul>
        </div>
      </v-card-text>
      <v-divider class="mx-4" />
    </v-card>
    <v-toolbar flat>
      <v-toolbar-title v-text="$t('databases.recent', { name: 'vue-i18n' })" />
    </v-toolbar>
    <DatabaseList ref="databases" />
  </div>
</template>

<script>
import DatabaseList from '@/components/DatabaseList'

export default {
  components: {
    DatabaseList
  },
  computed: {
    infoLinks () {
      const infoLinks = this.$config.infoLinks
      console.debug('info links', infoLinks)
      return infoLinks
    }
  }
}
</script>
