<template>
  <div>
    <v-card flat>
      <v-card-text>
        <div class="mb-2">Important Links</div>
        <div class="text--primary">
          <ul>
            <li>
              <a href="https://dbrepo-docs.ossdip.at/" target="_blank">Online Documentation</a>
            </li>
            <li>
              <a href="https://doi.org/10.2218/ijdc.v17i1.825" target="_blank">System Description</a>
            </li>
            <li>
              <a href="https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services" target="_blank">Sourcecode
                Documentation</a>
            </li>
            <li>
              <a href="https://hub.docker.com/u/dbrepo" target="_blank">Docker Images</a>
            </li>
            <li>
              <a href="https://dbrepo2.ec.tuwien.ac.at/" target="_blank">Demo Instance</a>
            </li>
          </ul>
        </div>
      </v-card-text>
      <v-card-actions>
        <v-btn
          class="ml-2 mb-2"
          color="primary"
          to="/container">
          Get Started
        </v-btn>
      </v-card-actions>
    </v-card>
    <v-card flat class="mt-4">
      <v-card-text>
        <div>Release Notes</div>
        <p class="text-h4 text--primary">
          1.2
        </p>
        <p>
          <a href="https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/master/CHANGELOG.md">Detailed
            Changelog</a>
        </p>
        <div class="text--primary">
          This release adds semantic information to sub- and supersets, time-versioned metadata database, added access
          model for giving users access to databases, improved the query store handling.
        </div>
      </v-card-text>
      <v-divider class="mx-4"/>
      <v-card-text>
        <div>Release Notes</div>
        <p class="text-h4 text--primary">
          1.1
        </p>
        <p>
          <a href="https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/master/CHANGELOG.md">Detailed
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
  components: {}
}
</script>
