<template>
  <div>
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-card>
        <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
        <v-card-title>
          Database Metadata
        </v-card-title>
        <v-card-text>
          <v-checkbox
            id="public"
            v-model="modify.is_public"
            name="public"
            label="Public" />
          <v-text-field
            id="publisher"
            v-model="modify.publisher"
            name="publisher"
            label="Publisher *"
            autofocus
            :rules="[v => !!v || $t('Required')]"
            required />
          <v-textarea
            id="description"
            v-model="modify.description"
            name="description"
            rows="2"
            label="Description *"
            :rules="[v => !!v || $t('Required')]"
            required />
          <v-select
            id="language"
            v-model="modify.language"
            name="language"
            label="Language *"
            :items="languages"
            item-value="value"
            item-text="text"
            :rules="[v => !!v || $t('Required')]"
            required />
          <v-text-field
            id="publication-year"
            v-model.number="modify.publication_year"
            name="publication"
            label="Publication Year *"
            hint="e.g. 2022"
            type="number"
            :rules="[v => !!v || $t('Required')]"
            required />
          <v-select
            id="license"
            v-model="modify.license"
            name="license"
            label="License *"
            :items="licenses"
            item-value="identifier"
            item-text="identifier"
            :rules="[v => !!v || $t('Required')]"
            return-object
            required />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            class="mb-2"
            @click="cancel">
            Cancel
          </v-btn>
          <v-btn
            id="database"
            class="mb-2"
            :disabled="!valid || loading"
            color="primary"
            type="submit"
            @click="updateDatabase">
            Update
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
export default {
  props: {
    database: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
      valid: false,
      loading: false,
      error: false,
      modify: {
        is_public: null,
        publisher: null,
        description: null,
        language: null,
        publication_year: null,
        license: null
      },
      licenses: [],
      languages: [
        { text: 'aa', value: 'aa' },
        { text: 'ab', value: 'ab' },
        { text: 'ae', value: 'ae' },
        { text: 'af', value: 'af' },
        { text: 'ak', value: 'ak' },
        { text: 'am', value: 'am' },
        { text: 'an', value: 'an' },
        { text: 'ar', value: 'ar' },
        { text: 'as', value: 'as' },
        { text: 'av', value: 'av' },
        { text: 'ay', value: 'ay' },
        { text: 'az', value: 'az' },
        { text: 'ba', value: 'ba' },
        { text: 'be', value: 'be' },
        { text: 'bg', value: 'bg' },
        { text: 'bh', value: 'bh' },
        { text: 'bi', value: 'bi' },
        { text: 'bm', value: 'bm' },
        { text: 'bn', value: 'bn' },
        { text: 'bo', value: 'bo' },
        { text: 'br', value: 'br' },
        { text: 'bs', value: 'bs' },
        { text: 'ca', value: 'ca' },
        { text: 'ce', value: 'ce' },
        { text: 'ch', value: 'ch' },
        { text: 'co', value: 'co' },
        { text: 'cr', value: 'cr' },
        { text: 'cs', value: 'cs' },
        { text: 'cu', value: 'cu' },
        { text: 'cv', value: 'cv' },
        { text: 'cy', value: 'cy' },
        { text: 'da', value: 'da' },
        { text: 'de', value: 'de' },
        { text: 'dv', value: 'dv' },
        { text: 'dz', value: 'dz' },
        { text: 'ee', value: 'ee' },
        { text: 'el', value: 'el' },
        { text: 'en', value: 'en' },
        { text: 'eo', value: 'eo' },
        { text: 'es', value: 'es' },
        { text: 'et', value: 'et' },
        { text: 'eu', value: 'eu' },
        { text: 'fa', value: 'fa' },
        { text: 'ff', value: 'ff' },
        { text: 'fi', value: 'fi' },
        { text: 'fj', value: 'fj' },
        { text: 'fo', value: 'fo' },
        { text: 'fr', value: 'fr' },
        { text: 'fy', value: 'fy' },
        { text: 'ga', value: 'ga' },
        { text: 'gd', value: 'gd' },
        { text: 'gl', value: 'gl' },
        { text: 'gn', value: 'gn' },
        { text: 'gu', value: 'gu' },
        { text: 'gv', value: 'gv' },
        { text: 'ha', value: 'ha' },
        { text: 'he', value: 'he' },
        { text: 'hi', value: 'hi' },
        { text: 'ho', value: 'ho' },
        { text: 'hr', value: 'hr' },
        { text: 'ht', value: 'ht' },
        { text: 'hu', value: 'hu' },
        { text: 'hy', value: 'hy' },
        { text: 'hz', value: 'hz' },
        { text: 'ia', value: 'ia' },
        { text: 'id', value: 'id' },
        { text: 'ie', value: 'ie' },
        { text: 'ig', value: 'ig' },
        { text: 'ii', value: 'ii' },
        { text: 'ik', value: 'ik' },
        { text: 'io', value: 'io' },
        { text: 'is', value: 'is' },
        { text: 'it', value: 'it' },
        { text: 'iu', value: 'iu' },
        { text: 'ja', value: 'ja' },
        { text: 'jv', value: 'jv' },
        { text: 'ka', value: 'ka' },
        { text: 'kg', value: 'kg' },
        { text: 'ki', value: 'ki' },
        { text: 'kj', value: 'kj' },
        { text: 'kk', value: 'kk' },
        { text: 'kl', value: 'kl' },
        { text: 'km', value: 'km' },
        { text: 'kn', value: 'kn' },
        { text: 'ko', value: 'ko' },
        { text: 'kr', value: 'kr' },
        { text: 'ks', value: 'ks' },
        { text: 'ku', value: 'ku' },
        { text: 'kv', value: 'kv' },
        { text: 'kw', value: 'kw' },
        { text: 'ky', value: 'ky' },
        { text: 'la', value: 'la' },
        { text: 'lb', value: 'lb' },
        { text: 'lg', value: 'lg' },
        { text: 'li', value: 'li' },
        { text: 'ln', value: 'ln' },
        { text: 'lo', value: 'lo' },
        { text: 'lt', value: 'lt' },
        { text: 'lu', value: 'lu' },
        { text: 'lv', value: 'lv' },
        { text: 'mg', value: 'mg' },
        { text: 'mh', value: 'mh' },
        { text: 'mi', value: 'mi' },
        { text: 'mk', value: 'mk' },
        { text: 'ml', value: 'ml' },
        { text: 'mn', value: 'mn' },
        { text: 'mr', value: 'mr' },
        { text: 'ms', value: 'ms' },
        { text: 'mt', value: 'mt' },
        { text: 'my', value: 'my' },
        { text: 'na', value: 'na' },
        { text: 'nb', value: 'nb' },
        { text: 'nd', value: 'nd' },
        { text: 'ne', value: 'ne' },
        { text: 'ng', value: 'ng' },
        { text: 'nl', value: 'nl' },
        { text: 'nn', value: 'nn' },
        { text: 'no', value: 'no' },
        { text: 'nr', value: 'nr' },
        { text: 'nv', value: 'nv' },
        { text: 'ny', value: 'ny' },
        { text: 'oc', value: 'oc' },
        { text: 'oj', value: 'oj' },
        { text: 'om', value: 'om' },
        { text: 'or', value: 'or' },
        { text: 'os', value: 'os' },
        { text: 'pa', value: 'pa' },
        { text: 'pi', value: 'pi' },
        { text: 'pl', value: 'pl' },
        { text: 'ps', value: 'ps' },
        { text: 'pt', value: 'pt' },
        { text: 'qu', value: 'qu' },
        { text: 'rm', value: 'rm' },
        { text: 'rn', value: 'rn' },
        { text: 'ro', value: 'ro' },
        { text: 'ru', value: 'ru' },
        { text: 'rw', value: 'rw' },
        { text: 'sa', value: 'sa' },
        { text: 'sc', value: 'sc' },
        { text: 'sd', value: 'sd' },
        { text: 'se', value: 'se' },
        { text: 'sg', value: 'sg' },
        { text: 'si', value: 'si' },
        { text: 'sk', value: 'sk' },
        { text: 'sl', value: 'sl' },
        { text: 'sm', value: 'sm' },
        { text: 'sn', value: 'sn' },
        { text: 'so', value: 'so' },
        { text: 'sq', value: 'sq' },
        { text: 'sr', value: 'sr' },
        { text: 'ss', value: 'ss' },
        { text: 'st', value: 'st' },
        { text: 'su', value: 'su' },
        { text: 'sv', value: 'sv' },
        { text: 'sw', value: 'sw' },
        { text: 'ta', value: 'ta' },
        { text: 'te', value: 'te' },
        { text: 'tg', value: 'tg' },
        { text: 'th', value: 'th' },
        { text: 'ti', value: 'ti' },
        { text: 'tk', value: 'tk' },
        { text: 'tl', value: 'tl' },
        { text: 'tn', value: 'tn' },
        { text: 'to', value: 'to' },
        { text: 'tr', value: 'tr' },
        { text: 'ts', value: 'ts' },
        { text: 'tt', value: 'tt' },
        { text: 'tw', value: 'tw' },
        { text: 'ty', value: 'ty' },
        { text: 'ug', value: 'ug' },
        { text: 'uk', value: 'uk' },
        { text: 'ur', value: 'ur' },
        { text: 'uz', value: 'uz' },
        { text: 've', value: 've' },
        { text: 'vi', value: 'vi' },
        { text: 'vo', value: 'vo' },
        { text: 'wa', value: 'wa' },
        { text: 'wo', value: 'wo' },
        { text: 'xh', value: 'xh' },
        { text: 'yi', value: 'yi' },
        { text: 'yo', value: 'yo' },
        { text: 'za', value: 'za' },
        { text: 'zh', value: 'zh' },
        { text: 'zu', value: 'zu' }
      ]
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    }
  },
  mounted () {
    this.loadLicenses()
    this.modify.is_public = this.database.is_public
    this.modify.publisher = this.database.publisher
    this.modify.description = this.database.description
    this.modify.publication_year = this.database.publication_year
    this.modify.language = this.database.language
    this.modify.license = this.database.license
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close-dialog')
    },
    async loadLicenses () {
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/license`)
        this.licenses = res.data
        console.debug('licenses', this.licenses)
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to fetch licenses.')
      }
      this.loading = false
    },
    async updateDatabase () {
      try {
        this.loading = true
        const res = await this.$axios.put(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.modify, this.config)
        this.database = res.data
        console.debug('database', this.database)
        this.$toast.success('Successfully updated the database.')
      } catch (err) {
        this.error = true
        this.loading = false
        this.$toast.error('Failed to update database.')
        return
      }
      this.loading = false
      this.cancel()
    }
  }
}
</script>
