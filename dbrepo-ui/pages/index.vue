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
      <v-card-actions>
        <v-btn
          class="ml-2 mb-2"
          color="primary"
          to="/database">
          Get Started
        </v-btn>
      </v-card-actions>
    </v-card>
    <v-card class="mt-4" flat tile>
      <v-card-text>
        <div>Release Notes</div>
        <p class="text-h4 text--primary">
          1.3
        </p>
        <p>
          <a href="https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/e4c6fdc36b3ceaa9f912cf284052459963d96076/CHANGELOG.md">Detailed
            Changelog</a>
        </p>
        <div class="text--primary">
          This release fixes bugs related to versioning, increases tasks for developers and data stewards. It enables
          organizations to federate identities with their own identity provider through Keycloak.
        </div>
      </v-card-text>
      <v-divider class="mx-4" />
      <v-card-text>
        <div>Release Notes</div>
        <p class="text-h4 text--primary">
          1.2
        </p>
        <p>
          <a
            href="https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/ac45842d27fcce47a99683330dc8ea1b8321c3be/CHANGELOG.md">Detailed
            Changelog</a>
        </p>
        <div class="text--primary">
          This release adds semantic information to sub- and supersets, time-versioned metadata database, added access
          model for giving users access to databases, improved the query store handling.
        </div>
      </v-card-text>
      <v-divider class="mx-4" />
      <v-card-text>
        <div>Release Notes</div>
        <p class="text-h4 text--primary">
          1.1
        </p>
        <p>
          <a
            href="https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/70b4d1e6398934d88e0080cb89f8cf87c68bf6eb/CHANGELOG">Detailed
            Changelog</a>
        </p>
        <div class="text--primary">
          In this release we added long-overdue features like user management so each user has their own database (which
          can also be private) as well as data-insert features directly in the portal.
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
export default {
  computed: {
    infoLinks () {
      const infoLinks = this.$config.infoLinks
      console.debug('info links', infoLinks)
      return infoLinks
    }
  }
}
</script>
