<template>
  <div id="persist">
    <v-toolbar flat>
      <v-btn
        icon="mdi-arrow-left"
        size="small"
        :to="backTo" />
      <v-toolbar-title :text="pageTitle" />
      <v-spacer />
      <v-btn
        v-if="!isUpdate"
        prepend-icon="mdi-content-save-outline"
        class="mb-1"
        color="primary"
        variant="flat"
        :loading="loading"
        :disabled="!formValid || !validPublicationMonth || !validPublicationDay || loading"
        :text="($vuetify.display.xl ? $t('toolbars.identifier.create.xl') + ' ' : '') + $t('toolbars.identifier.create.permanent')"
        @click="save" />
      <v-btn
        v-if="isUpdate"
        prepend-icon="mdi-content-save-outline"
        class="mb-1"
        color="primary"
        variant="flat"
        :loading="loading"
        :disabled="!formValid || !validPublicationMonth || !validPublicationDay || loading"
        :text="($vuetify.display.xl ? $t('toolbars.identifier.update.xl') + ' ' : '') + $t('toolbars.identifier.update.permanent')"
        @click="save" />
    </v-toolbar>
    <v-form
      ref="form"
      v-model="formValid">
      <v-card
        variant="flat"
        rounded="0"
        :title="$t('pages.identifier.creators.title')"
        :subtitle="$t('pages.identifier.subpages.create.creators.subtitle')">
        <v-card-text>
          <v-stepper
            v-for="(creator, i) in identifier.creators"
            :key="`c-${i}`"
            vertical
            multiple
            variant="flat">
            <v-stepper-header>
              <v-stepper-item
                :value="i+1" />
            </v-stepper-header>
            <v-stepper-window
              direction="vertical">
              <v-container>
                <v-row dense>
                  <v-col cols="8">
                    <v-text-field
                      v-model="creator.name_identifier"
                      :label="$t('pages.identifier.subpages.create.creators.identifier.label')"
                      clearable
                      :variant="inputVariant"
                      :hint="$t('pages.identifier.subpages.create.creators.identifier.hint')"
                      :loading="creator.name_loading"
                      persistent-hint
                      @focusout="retrieveCreator(creator)" />
                  </v-col>
                  <v-col cols="4">
                    <v-btn
                      icon="mdi-arrow-up"
                      class="mr-2"
                      :disabled="!canShiftUp(creator, i)"
                      size="small"
                      :color="canShiftUp(creator, i) ? 'tertiary' : ''"
                      :variant="buttonVariant"
                      @click="shiftUp(i)" />
                    <v-btn
                      icon="mdi-arrow-down"
                      class="mr-2"
                      :disabled="!canShiftDown(creator, i)"
                      size="small"
                      :color="canShiftUp(creator, i) ? 'tertiary' : ''"
                      :variant="buttonVariant"
                      @click="shiftDown(i)" />
                    <v-btn
                      v-if="canInsertSelf"
                      class="mr-2"
                      size="small"
                      color="secondary"
                      variant="flat"
                      :text="$t('pages.identifier.subpages.create.creators.insert.text')"
                      @click="insertSelf(creator)" />
                    <v-btn
                      v-if="i > 0"
                      size="small"
                      color="error"
                      variant="flat"
                      :text="$t('pages.identifier.subpages.create.creators.remove.text')"
                      @click="deleteCreator(i)" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="8">
                    <v-radio-group
                      v-model="creator.name_type"
                      row>
                      <v-radio
                        :label="$t('pages.identifier.subpages.create.creators.person.label')"
                        value="Personal" />
                      <v-radio
                        :label="$t('pages.identifier.subpages.create.creators.organization.label')"
                        value="Organizational" />
                    </v-radio-group>
                  </v-col>
                </v-row>
                <v-row
                  v-if="isPerson(creator)"
                  dense>
                  <v-col cols="8">
                    <v-text-field
                      v-model="creator.firstname"
                      :label="$t('pages.identifier.subpages.create.creators.given-name.label')"
                      clearable
                      :variant="inputVariant"
                      :hint="$t('pages.identifier.subpages.create.creators.given-name.hint')"
                      persistent-hint
                      @focusout="suggestName(creator)" />
                  </v-col>
                </v-row>
                <v-row
                  v-if="isPerson(creator)"
                  dense>
                  <v-col cols="8">
                    <v-text-field
                      v-model="creator.lastname"
                      :label="$t('pages.identifier.subpages.create.creators.family-name.label')"
                      clearable
                      :variant="inputVariant"
                      :hint="$t('pages.identifier.subpages.create.creators.family-name.hint')"
                      persistent-hint
                      @focusout="suggestName(creator)" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="8">
                    <v-text-field
                      v-model="creator.creator_name"
                      :label="$t('pages.identifier.subpages.create.creators.name.label')"
                      clearable
                      :variant="inputVariant"
                      :hint="$t('pages.identifier.subpages.create.creators.name.hint')"
                      persistent-hint
                      :rules="[v => !!v || $t('validation.required')]"
                      required />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="8">
                    <v-text-field
                      v-model="creator.affiliation_identifier"
                      :label="$t('pages.identifier.subpages.create.creators.affiliation-identifier.label')"
                      :variant="inputVariant"
                      :loading="creator.affiliation_loading"
                      :hint="$t('pages.identifier.subpages.create.creators.affiliation-identifier.hint')"
                      persistent-hint
                      clearable
                      @focusout="retrieveAffiliation(creator)" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="8">
                    <v-text-field
                      v-model="creator.affiliation"
                      :label="$t('pages.identifier.subpages.create.creators.affiliation.label')"
                      :variant="inputVariant"
                      clearable
                      :hint="$t('pages.identifier.subpages.create.creators.affiliation.hint')"
                      persistent-hint />
                  </v-col>
                </v-row>
              </v-container>
            </v-stepper-window>
          </v-stepper>
        </v-card-text>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-btn
                size="small"
                color="tertiary"
                :variant="buttonVariant"
                :text="$t('pages.identifier.subpages.create.creators.add')"
                @click="addCreator" />
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
      <v-card
        variant="flat"
        rounded="0"
        :title="$t('pages.identifier.titles.title')"
        :subtitle="$t('pages.identifier.subpages.create.titles.subtitle')">
        <v-card-text>
          <v-stepper
            v-for="(title, i) in identifier.titles"
            :key="`t-${i}`"
            vertical
            multiple
            variant="flat">
            <v-stepper-header>
              <v-stepper-item
                :value="i+1" />
            </v-stepper-header>
            <v-stepper-window
              direction="vertical">
              <v-container>
                <v-row dense>
                  <v-col cols="8">
                    <v-text-field
                      v-model="title.title"
                      :label="$t('pages.identifier.subpages.create.titles.title.label')"
                      clearable
                      :variant="inputVariant"
                      :hint="$t('pages.identifier.subpages.create.titles.title.hint')"
                      persistent-hint
                      :rules="[v => !!v || $t('validation.required')]"
                      required />
                  </v-col>
                  <v-col cols="4">
                    <v-btn
                      v-if="i > 0"
                      color="error"
                      size="small"
                      variant="flat"
                      :text="$t('pages.identifier.subpages.create.titles.remove.text')"
                      @click="deleteTitle(i)" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="8">
                    <v-select
                      v-model="title.type"
                      :label="$t('pages.identifier.subpages.create.titles.type.label')"
                      clearable
                      :variant="inputVariant"
                      :hint="$t('pages.identifier.subpages.create.titles.type.hint')"
                      variant="underlined"
                      :items="titleType"
                      item-title="value"
                      item-value="value" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="8">
                    <v-autocomplete
                      v-model="title.language"
                      :label="$t('pages.identifier.subpages.create.titles.language.label')"
                      clearable
                      :variant="inputVariant"
                      :hint="$t('pages.identifier.subpages.create.titles.language.hint')"
                      variant="underlined"
                      :items="languages"
                      item-title="name"
                      item-value="code" />
                  </v-col>
                </v-row>
              </v-container>
            </v-stepper-window>
          </v-stepper>
        </v-card-text>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-btn
                size="small"
                color="tertiary"
                :variant="buttonVariant"
                :text="$t('pages.identifier.subpages.create.titles.add.text')"
                @click="addTitle" />
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
      <v-card
        variant="flat"
        rounded="0"
        :title="$t('pages.identifier.descriptions.title')"
        :subtitle="$t('pages.identifier.subpages.create.descriptions.subtitle')">
        <v-card-text>
          <v-stepper
            v-for="(description, i) in identifier.descriptions"
            :key="`d-${i}`"
            vertical
            multiple
            variant="flat">
            <v-stepper-header>
              <v-stepper-item
                :value="i+1" />
            </v-stepper-header>
            <v-stepper-window
              direction="vertical">
              <v-container>
                <v-row dense>
                  <v-col cols="8">
                    <v-text-field
                      v-model="description.description"
                      :label="$t('pages.identifier.subpages.create.descriptions.description.label')"
                      clearable
                      :variant="inputVariant"
                      :hint="$t('pages.identifier.subpages.create.descriptions.description.hint')"
                      persistent-hint
                      :rules="[v => !!v || $t('validation.required')]"
                      required />
                  </v-col>
                  <v-col cols="4">
                    <v-btn
                      v-if="i > 0"
                      size="small"
                      color="error"
                      variant="flat"
                      :text="$t('pages.identifier.subpages.create.descriptions.remove.text')"
                      @click="deleteDescription(i)" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="8">
                    <v-select
                      v-model="description.type"
                      :label="$t('pages.identifier.subpages.create.descriptions.type.label')"
                      clearable
                      :variant="inputVariant"
                      :hint="$t('pages.identifier.subpages.create.descriptions.type.hint')"
                      persistent-hint
                      variant="underlined"
                      :items="descriptionType"
                      item-title="value"
                      item-value="value" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="8">
                    <v-autocomplete
                      v-model="description.language"
                      :label="$t('pages.identifier.subpages.create.descriptions.language.label')"
                      clearable
                      :variant="inputVariant"
                      :hint="$t('pages.identifier.subpages.create.descriptions.language.hint')"
                      variant="underlined"
                      :items="languages"
                      item-title="name"
                      item-value="code" />
                  </v-col>
                </v-row>
              </v-container>
            </v-stepper-window>
          </v-stepper>
        </v-card-text>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-btn
                size="small"
                color="tertiary"
                :variant="buttonVariant"
                :text="$t('pages.identifier.subpages.create.descriptions.add.text')"
                @click="addDescription" />
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
      <v-card
        variant="flat"
        rounded="0"
        :title="$t('pages.identifier.subpages.create.publisher.title')"
        :subtitle="$t('pages.identifier.subpages.create.publisher.subtitle')">
        <v-card-text>
          <v-container>
            <v-row dense>
              <v-col cols="8">
                <v-text-field
                  v-model="identifier.publisher"
                  :variant="inputVariant"
                  :label="$t('pages.identifier.subpages.create.publisher.label')"
                  :hint="$t('pages.identifier.subpages.create.publisher.hint')"
                  persistent-hint
                  :rules="[v => !!v || $t('validation.required')]"
                  required />
              </v-col>
            </v-row>
            <v-row dense>
              <v-col cols="2">
                <v-text-field
                  v-model.number="identifier.publication_day"
                  type="number"
                  :variant="inputVariant"
                  :label="$t('pages.identifier.subpages.create.publication-day.label')"
                  :hint="$t('pages.identifier.subpages.create.publication-day.hint')"
                  persistent-hint
                  :rules="[validPublicationDay || $t('validation.day')]"
                  clearable />
              </v-col>
              <v-col cols="2">
                <v-text-field
                  v-model.number="identifier.publication_month"
                  type="number"
                  :variant="inputVariant"
                  :label="$t('pages.identifier.subpages.create.publication-month.label')"
                  :hint="$t('pages.identifier.subpages.create.publication-month.hint')"
                  persistent-hint
                  :rules="[validPublicationMonth || $t('validation.month')]"
                  clearable />
              </v-col>
              <v-col cols="2">
                <v-text-field
                  v-model.number="identifier.publication_year"
                  type="number"
                  :variant="inputVariant"
                  :label="$t('pages.identifier.subpages.create.publication-year.label')"
                  :hint="$t('pages.identifier.subpages.create.publication-year.hint')"
                  persistent-hint
                  :rules="[v => !!v || $t('validation.required')]"
                  required />
              </v-col>
            </v-row>
          </v-container>
        </v-card-text>
      </v-card>
      <v-card
        variant="flat"
        rounded="0"
        :title="$t('pages.identifier.subpages.create.related-identifiers.title')"
        :subtitle="$t('pages.identifier.subpages.create.related-identifiers.subtitle')">
        <v-card-text>
          <v-stepper
            v-for="(related, i) in identifier.related_identifiers"
            :key="`r-${i}`"
            vertical
            multiple
            variant="flat">
            <v-stepper-header>
              <v-stepper-item
                :value="i+1" />
            </v-stepper-header>
            <v-stepper-window
              direction="vertical">
              <v-container>
                <v-row dense>
                  <v-col cols="4">
                    <v-text-field
                      v-model="related.value"
                      :variant="inputVariant"
                      :label="$t('pages.identifier.subpages.create.related-identifiers.identifier.label')"
                      :hint="$t('pages.identifier.subpages.create.related-identifiers.identifier.hint')"
                      persistent-hint
                      :rules="[v => !!v || $t('validation.required')]"
                      required />
                  </v-col>
                  <v-col cols="2">
                    <v-select
                      v-model="related.type"
                      :items="relatedTypes"
                      item-value="value"
                      item-title="value"
                      :variant="inputVariant"
                      :label="$t('pages.identifier.subpages.create.related-identifiers.type.label')"
                      :hint="$t('pages.identifier.subpages.create.related-identifiers.type.hint')"
                      persistent-hint
                      clearable
                      variant="underlined" />
                  </v-col>
                  <v-col cols="2">
                    <v-select
                      v-model="related.relation"
                      :items="relationTypes"
                      item-value="value"
                      item-title="value"
                      :variant="inputVariant"
                      :label="$t('pages.identifier.subpages.create.related-identifiers.relation.label')"
                      :hint="$t('pages.identifier.subpages.create.related-identifiers.relation.hint')"
                      persistent-hint
                      clearable
                      variant="underlined" />
                  </v-col>
                  <v-col cols="2" class="mt-5">
                    <v-btn
                      size="small"
                      color="error"
                      variant="flat"
                      :text="$t('pages.identifier.subpages.create.related-identifiers.remove.text')"
                      @click="deleteRelatedIdentifier(i)" />
                  </v-col>
                </v-row>
              </v-container>
            </v-stepper-window>
          </v-stepper>
        </v-card-text>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-btn
                size="small"
                color="tertiary"
                :variant="buttonVariant"
                :text="$t('pages.identifier.subpages.create.related-identifiers.add.text')"
                @click="addRelatedIdentifier" />
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
      <v-card
        variant="flat"
        rounded="0"
        :title="$t('pages.identifier.subpages.create.licenses.title')"
        :subtitle="$t('pages.identifier.subpages.create.licenses.subtitle')">
        <v-card-text>
          <v-alert
            v-if="identifier.licenses.length > 0"
            color="tertiary">
            <p>
              <a :href="identifier.licenses[0].uri" target="_blank">
                <strong v-text="identifier.licenses[0].identifier" />&nbsp;<sup><v-icon x-small>mdi-open-in-new</v-icon></sup>
              </a>
            </p>
            <p
              v-if="identifier.licenses[0].description"
              class="mt-2"
              v-text="identifier.licenses[0].description" />
          </v-alert>
        </v-card-text>
        <v-card-text>
          <v-container>
            <v-row dense>
              <v-col cols="8">
                <v-select
                  v-model="identifier.licenses"
                  return-object
                  :items="licenses"
                  multiple
                  clearable
                  :variant="inputVariant"
                  item-title="identifier"
                  :label="$t('pages.identifier.subpages.create.licenses.license.label')"
                  :rules="[ v => !!v || $t('validation.required') ]"
                  required>
                  <template v-slot:item="{ props, item }">
                    <v-list-item
                      v-bind="props"
                      :subtitle="item.raw.description" />
                  </template>
                </v-select>
              </v-col>
            </v-row>
          </v-container>
        </v-card-text>
      </v-card>
      <v-card
        variant="flat"
        rounded="0"
        :title="$t('pages.identifier.subpages.create.language.title')"
        :subtitle="$t('pages.identifier.subpages.create.language.subtitle')">
        <v-card-text>
          <v-container>
            <v-row dense>
              <v-col cols="8">
                <v-autocomplete
                  v-model="identifier.language"
                  :label="$t('pages.identifier.subpages.create.language.language.label')"
                  clearable
                  :variant="inputVariant"
                  :hint="$t('pages.identifier.subpages.create.language.language.hint')"
                  persistent-hint
                  :items="languages"
                  item-title="name"
                  item-value="code" />
              </v-col>
            </v-row>
          </v-container>
        </v-card-text>
      </v-card>
      <v-card
        variant="flat"
        rounded="0"
        :title="$t('pages.identifier.subpages.create.funders.title')"
        :subtitle="$t('pages.identifier.subpages.create.funders.subtitle')">
        <v-card-text>
          <v-stepper
            v-for="(funder, i) in identifier.funders"
            :key="`f-${i}`"
            vertical
            multiple
            variant="flat">
            <v-stepper-header>
              <v-stepper-item
                :value="i+1" />
            </v-stepper-header>
            <v-stepper-window
              direction="vertical">
              <v-container>
                <v-row dense>
                  <v-col cols="8">
                    <v-text-field
                      v-model="funder.funder_identifier"
                      :label="$t('pages.identifier.subpages.create.funders.identifier.label')"
                      :hint="$t('pages.identifier.subpages.create.funders.identifier.hint')"
                      :loading="funder.loading"
                      persistent-hint
                      :variant="inputVariant"
                      clearable
                      @focusout="retrieveFunder(funder)" />
                  </v-col>
                  <v-col cols="4" class="mt-5">
                    <v-btn
                      color="error"
                      variant="flat"
                      size="small"
                      :text="$t('pages.identifier.subpages.create.funders.remove.text')"
                      @click="deleteFunder(i)" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="8">
                    <v-text-field
                      v-model="funder.funder_name"
                      :label="$t('pages.identifier.subpages.create.funders.name.label')"
                      :hint="$t('pages.identifier.subpages.create.funders.name.hint')"
                      persistent-hint
                      :variant="inputVariant"
                      :rules="[v => !!v || $t('validation.required')]"
                      required />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="8">
                    <v-text-field
                      v-model="funder.award_number"
                      :variant="inputVariant"
                      :label="$t('pages.identifier.subpages.create.funders.award-number.label')"
                      :hint="$t('pages.identifier.subpages.create.funders.award-number.hint')"
                      clearable />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="8">
                    <v-text-field
                      v-model="funder.award_title"
                      :variant="inputVariant"
                      :label="$t('pages.identifier.subpages.create.funders.award-title.label')"
                      :hint="$t('pages.identifier.subpages.create.funders.award-title.hint')"
                      clearable />
                  </v-col>
                </v-row>
              </v-container>
            </v-stepper-window>
          </v-stepper>
        </v-card-text>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-btn
                size="small"
                color="tertiary"
                :variant="buttonVariant"
                :text="$t('pages.identifier.subpages.create.funders.add.text')"
                @click="addFunding" />
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
      <v-divider />
      <v-card
        variant="flat"
        rounded="0"
        :title="$t('pages.identifier.subpages.create.summary.title')"
        :subtitle="$t('pages.identifier.subpages.create.summary.subtitle')">
        <v-card-text>
            <v-list
              density="compact">
              <v-list-item>
                <v-list-item-title>
                  {{ $t('pages.identifier.subpages.create.summary.record') }} {{ resourceHumanDescription.prefix }}
                  &quot;<strong v-text="resourceHumanDescription.info" />&quot;
                </v-list-item-title>
                <template v-slot:prepend>
                  <v-icon
                    icon="mdi-check"
                    color="success"/>
                </template>
              </v-list-item>
              <v-list-item
                :title="identifier.creators.length + ' ' + $t('pages.identifier.subpages.create.summary.creators')">
                <template v-slot:prepend>
                  <v-icon
                    icon="mdi-check"
                    color="success"/>
                </template>
              </v-list-item>
              <v-list-item>
                <v-list-item-title
                  v-if="identifier.licenses.length > 0">
                  {{ $t('pages.identifier.subpages.create.summary.license') }}
                  &quot;<strong v-text="identifier.licenses[0].identifier" />&quot;
                </v-list-item-title>
                <v-list-item-title
                  v-else>
                  {{ $t('pages.identifier.subpages.create.summary.no-license') }}
                </v-list-item-title>
                <template v-slot:prepend>
                  <v-icon
                    :icon="identifier.licenses.length > 0 ? 'mdi-check' : 'mdi-alert-outline'"
                    :color="identifier.licenses.length > 0 ? 'success' : 'warning'"/>
                </template>
              </v-list-item>
              <v-list-item
                v-if="identifier.publisher">
                <v-list-item-title>
                  {{ $t('pages.identifier.subpages.create.summary.publisher') }}
                  &quot;<strong v-text="identifier.publisher" />&quot;
                </v-list-item-title>
                <template v-slot:prepend>
                  <v-icon
                    icon="mdi-check"
                    color="success"/>
                </template>
              </v-list-item>
              <v-list-item>
                <v-list-item-title
                  v-if="willMintDoi">
                  {{ $t('pages.identifier.subpages.create.summary.doi') }}
                </v-list-item-title>
                <v-list-item-title
                  v-else>
                  {{ $t('pages.identifier.subpages.create.summary.no-doi') }}
                </v-list-item-title>
                <template v-slot:prepend>
                  <v-icon
                    icon="mdi-check"
                    color="success"/>
                </template>
              </v-list-item>
            </v-list>
        </v-card-text>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { formatYearUTC, formatMonthUTC, formatDayUTC, languages } from '@/utils'
import { useCacheStore } from '@/stores/cache'
import { useUserStore } from '@/stores/user'

export default {
  props: {
    type: {
      type: String,
      default: 'subset'
    },
    database: {
      type: Object,
      default () {
        return {}
      }
    },
    query: {
      type: Object,
      default () {
        return {}
      }
    },
    view: {
      type: Object,
      default () {
        return {}
      }
    },
    table: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
      formValid: false,
      loading: false,
      error: false, // XXX: `error` is never changed
      licenses: [],
      identifier: {
        database_id: parseInt(this.$route.params.database_id),
        query_id: parseInt(this.$route.params.subset_id),
        view_id: parseInt(this.$route.params.view_id),
        table_id: parseInt(this.$route.params.table_id),
        titles: [],
        descriptions: [],
        publisher: this.$config.public.pid.default.publisher,
        publication_year: parseInt(formatYearUTC(Date.now())),
        publication_month: parseInt(formatMonthUTC(Date.now())),
        publication_day: parseInt(formatDayUTC(Date.now())),
        licenses: [],
        type: this.type,
        creators: [],
        related_identifiers: [],
        funders: []
      },
      titleType: [
        { value: 'AlternativeTitle' },
        { value: 'Subtitle' },
        { value: 'TranslatedTitle' },
        { value: 'Other' }
      ],
      descriptionType: [
        { value: 'Abstract' },
        { value: 'Methods' },
        { value: 'SeriesInformation' },
        { value: 'TableOfContents' },
        { value: 'TechnicalInfo' },
        { value: 'Other' }
      ],
      languages: languages(),
      relatedTypes: [
        { value: 'DOI' },
        { value: 'URL' },
        { value: 'URN' },
        { value: 'ARK' },
        { value: 'arXiv' },
        { value: 'bibcode' },
        { value: 'EAN13' },
        { value: 'EISSN' },
        { value: 'Handle' },
        { value: 'IGSN' },
        { value: 'ISBN' },
        { value: 'ISTC' },
        { value: 'LISSN' },
        { value: 'LSID' },
        { value: 'PMID' },
        { value: 'PURL' },
        { value: 'UPC' },
        { value: 'w3id' }
      ],
      relationTypes: [
        { value: 'IsCitedBy' },
        { value: 'Cites' },
        { value: 'IsSupplementTo' },
        { value: 'IsSupplementedBy' },
        { value: 'IsContinuedBy' },
        { value: 'Continues' },
        { value: 'IsDescribedBy' },
        { value: 'Describes' },
        { value: 'HasMetadata' },
        { value: 'IsMetadataFor' },
        { value: 'HasVersion' },
        { value: 'IsVersionOf' },
        { value: 'IsNewVersionOf' },
        { value: 'IsPreviousVersionOf' },
        { value: 'IsPartOf' },
        { value: 'HasPart' },
        { value: 'IsPublishedIn' },
        { value: 'IsReferencedBy' },
        { value: 'References' },
        { value: 'IsDocumentedBy' },
        { value: 'Documents' },
        { value: 'IsCompiledBy' },
        { value: 'Compiles' },
        { value: 'IsVariantFormOf' },
        { value: 'IsOriginalFormOf' },
        { value: 'IsIdenticalTo' },
        { value: 'IsReviewedBy' },
        { value: 'Reviews' },
        { value: 'IsDerivedFrom' },
        { value: 'IsSourceOf' },
        { value: 'IsRequiredBy' },
        { value: 'Requires' },
        { value: 'IsObsoletedBy' },
        { value: 'Obsoletes' }
      ],
      cacheStore: useCacheStore(),
      userStore: useUserStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser.value
    },
    isSubset () {
      return this.type === 'subset'
    },
    isDatabase () {
      return this.type === 'database'
    },
    isView () {
      return this.type === 'view'
    },
    isTable () {
      return this.type === 'table'
    },
    willMintDoi () {
      return this.$config.public.doi.enabled
    },
    backTo () {
      if (this.isSubset) {
        return `/database/${this.$route.params.database_id}/subset/${this.$route.params.subset_id}`
      } else if (this.isDatabase) {
        return `/database/${this.$route.params.database_id}/info`
      } else if (this.isView) {
        return `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`
      } else if (this.isTable) {
        return `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`
      }
      return null
    },
    resourceHumanDescription () {
      switch (this.type) {
        case 'subset':
          return {
            prefix: 'subset with query ',
            info: this.query.query
          }
        case 'database':
          return {
            prefix: 'database with name ',
            info: this.database.name
          }
        case 'view':
          return {
            prefix: 'view with name ',
            info: this.view.name
          }
        case 'table':
          return {
            prefix: 'table with name ',
            info: this.table.name
          }
      }
    },
    pageTitle () {
      return (this.isUpdate ? 'Update' : 'Create') + ' Identifier'
    },
    isUpdate () {
      return 'id' in this.identifier && this.identifier.id
    },
    canInsertSelf () {
      if (!this.user) {
        return false
      }
      return this.user.given_name || this.user.family_name || this.user.attributes.affiliation || this.user.attributes.orcid
    },
    prefix () {
      if (this.isSubset) {
        return 'Subset'
      } else if (this.isDatabase) {
        return 'Database'
      } else if (this.isView) {
        return 'View'
      } else if (this.isTable) {
        return 'Table'
      }
      return ''
    },
    validPublicationDay () {
      const day = this.identifier.publication_day
      if (day === null) {
        return true
      }
      return day >= 1 && day <= 31
    },
    validPublicationMonth () {
      const month = this.identifier.publication_month
      if (month === null) {
        return true
      }
      return month >= 1 && month <= 12
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    }
  },
  watch: {
    database () {
      this.init()
    },
    query () {
      this.init()
    },
    view () {
      this.init()
    }
  },
  mounted () {
    this.addCreator()
    this.addTitle()
    this.addDescription()
    this.loadLicenses()
    this.init()
  },
  methods: {
    cancel () {
      this.$emit('close', { action: 'closed' })
    },
    retrieveCreator (creator) {
      if (!creator || !creator.name_identifier) {
        creator.name_identifier_scheme = null
        return
      }
      creator.name_loading = true
      const identifierService = useIdentifierService()
      identifierService.suggest(creator.name_identifier)
        .then((metadata) => {
          creator.success = true
          creator.firstname = metadata?.given_names
          creator.lastname = metadata?.family_name
          creator.name_type = metadata.type ? metadata.type : creator.name_type /* default to preset value if erroneous */
          if (metadata.type === 'Organizational' && metadata.affiliations) {
            creator.creator_name = metadata.affiliations[0].organization_name
            creator.affiliation = null
          } else {
            if (creator.firstname && creator.lastname) {
              creator.creator_name = (creator.lastname + ', ' + creator.firstname)
            }
            if (metadata.affiliations.length > 0) {
              creator.affiliation = metadata.affiliations[0].organization_name
            }
          }
          creator.name_identifier_scheme = UserMapper.nameIdentifierToNameIdentifierScheme(creator.name_identifier)
        })
        .catch(() => {
          creator.success = false
        })
        .finally(() => {
          creator.name_loading = false
        })
    },
    suggestName (creator) {
      if (!creator.firstname || !creator.lastname) {
        return
      }
      creator.creator_name = creator.lastname + ', ' + creator.firstname
    },
    isPerson (creator) {
      if (!creator || !creator.name_type) {
        return false
      }
      return creator.name_type === 'Personal'
    },
    retrieveAffiliation (creator) {
      if (!creator || !creator.affiliation_identifier) {
        creator.affiliation_identifier_scheme = null
        return
      }
      creator.affiliation_loading = true
      const identifierService = useIdentifierService()
      identifierService.suggest(creator.affiliation_identifier)
        .then((metadata) => {
          creator.success = true
          if (creator.type === 'Organizational') {
            creator.creator_name = metadata.affiliations[0].organization_name
          } else {
            creator.affiliation = metadata.affiliations[0].organization_name
          }
          creator.affiliation_identifier_scheme = UserMapper.nameIdentifierToNameIdentifierScheme(creator.affiliation_identifier)
        })
        .catch(() => {
          creator.success = false
        })
        .finally(() => {
          creator.affiliation_loading = false
        })
    },
    retrieveFunder (funder) {
      if (!funder || !funder.funder_identifier) {
        funder.funder_identifier_scheme = null
        return
      }
      funder.loading = true
      const identifierService = useIdentifierService()
      identifierService.suggest(funder.funder_identifier)
        .then((metadata) => {
          if (metadata.type === 'Organizational' && metadata.affiliations) {
            funder.funder_name = metadata.affiliations[0].organization_name
          }
          funder.funder_identifier_scheme = UserMapper.nameIdentifierToNameIdentifierScheme(funder.name_identifier)
        })
        .catch(() => {
          funder.success = false
        })
        .finally(() => {
          funder.loading = false
        })
    },
    addCreator () {
      this.identifier.creators.push({
        firstname: null,
        lastname: null,
        affiliation: null,
        affiliation_identifier: null,
        affiliation_identifier_scheme: null,
        name_identifier: null,
        name_identifier_scheme: null,
        name_type: 'Personal',
        creator_name: null,
        name_loading: false /* removed later */,
        affiliation_loading: false /* removed later */,
        success: false /* removed later */
      })
    },
    addTitle () {
      this.identifier.titles.push({
        title: null,
        type: null,
        language: null
      })
    },
    addFunding () {
      this.identifier.funders.push({
        funder_name: null,
        funder_identifier: null,
        funder_identifier_type: null,
        award_number: null,
        award_title: null,
        loading: false /* removed later */,
        success: false /* removed later */
      })
    },
    addDescription () {
      this.identifier.descriptions.push({
        description: null,
        type: null,
        language: null
      })
    },
    addRelatedIdentifier () {
      this.identifier.related_identifiers.push({
        value: null,
        relation: 'Cites',
        type: 'DOI'
      })
    },
    deleteCreator (index) {
      if (index === 0) {
        return
      }
      this.identifier.creators.splice(index, 1)
    },
    deleteFunder (index) {
      this.identifier.funders.splice(index, 1)
    },
    deleteTitle (index) {
      if (index === 0) {
        return
      }
      this.identifier.titles.splice(index, 1)
    },
    deleteDescription (index) {
      if (index === 0) {
        return
      }
      this.identifier.descriptions.splice(index, 1)
    },
    deleteRelatedIdentifier (index) {
      this.identifier.related_identifiers.splice(index, 1)
    },
    save () {
      this.loading = true
      const identifierService = useIdentifierService()
      const payload = identifierService.identifierToIdentifierSave(this.identifier)
      if (this.isUpdate) {
        identifierService.update(this.identifier.id, payload)
          .then(() => {
            this.cacheStore.reloadDatabase()
            this.$router.push(this.backTo)
            this.$toast.success(this.$t('success.pid.updated'))
          })
          .catch(() => {
            this.loading = false
          })
          .finally(() => {
            this.loading = false
          })
      } else {
        identifierService.create(payload)
          .then(() => {
            this.cacheStore.reloadDatabase()
            this.$router.push(this.backTo)
            this.$toast.success(this.$t('success.pid.created'))
          })
          .catch(() => {
            this.loading = false
          })
          .finally(() => {
            this.loading = false
          })
      }
    },
    loadLicenses () {
      this.loading = true
      const licenseService = useLicenseService()
      licenseService.findAll()
        .then((licenses) => {
          this.licenses = licenses
          this.loading = false
        })
        .catch(() => {
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    init () {
      if (this.isDatabase && this.database && 'identifier' in this.database && this.database.identifier) {
        this.identifier = Object.assign(this.database.identifier, {})
      } else if (this.isSubset && this.query && 'identifier' in this.query && this.query.identifier) {
        this.identifier = Object.assign(this.query.identifier, {})
      } else if (this.isView && this.view && 'identifier' in this.view && this.view.identifier) {
        this.identifier = Object.assign(this.view.identifier, {})
      }
    },
    insertSelf (creator) {
      if (this.user.attributes.orcid) {
        creator.name_identifier = this.user.attributes.orcid
        this.retrieveCreator(creator)
        return
      }
      creator.firstname = this.user.given_name
      creator.lastname = this.user.family_name
      creator.creator_name = (creator.lastname ? creator.lastname + ', ' : '') + creator.firstname
      creator.affiliation = this.user.attributes.affiliation
    },
    canShiftUp (creator, idx) {
      return !(this.identifier.creators.length === 1 || idx === 0);

    },
    canShiftDown (creator, idx) {
      return !(this.identifier.creators.length === 1 || idx + 1 === this.identifier.creators.length);

    },
    shiftUp (idx) {
      this.arrayMove(this.identifier.creators, idx, idx - 1)
    },
    shiftDown (idx) {
      this.arrayMove(this.identifier.creators, idx, idx + 1)
    },
    arrayMove (array, fromIndex, toIndex) {
      const element = array[fromIndex]
      array.splice(fromIndex, 1)
      array.splice(toIndex, 0, element)
    }
  }
}
</script>
