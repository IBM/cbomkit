<template>
  <div>
    <div class="search">
      <cv-search
        :light="true"
        class="search-bar"
        placeholder="Enter Git URL or Package URL to scan"
        v-model="model.codeOrigin.scanUrl"
        @keyup.enter="connectAndScan(advancedOptions()[0], advancedOptions()[1], advancedOptions()[2])"
      />
      <cv-button
        class="search-button"
        :icon="ArrowRight24"
        @click="connectAndScan(advancedOptions()[0], advancedOptions()[1], advancedOptions()[2])"
        :disabled="!model.codeOrigin.scanUrl"
        >Scan</cv-button
      >
    </div>
    <div style="color: var(--cds-text-secondary)">
      <cv-checkbox
        class="filter-checkbox"
        label="Advanced options"
        v-model="filterOpen"
        value="filter"
      />
    </div>
    <Transition name="filters">
      <div v-show="filterOpen">
        <cv-tabs style="padding-top: 15px; padding-bottom: 10px">
          <cv-tab label="Scan">
            <cv-text-input
              class="filter-input"
              label="Branch"
              placeholder="Specify a specific branch"
              v-model="gitBranch"
            />
            <cv-text-input
              class="filter-input"
              label="Subfolder"
              placeholder="Specify a specific subfolder to scan"
              v-model="gitSubfolder"
            />
          </cv-tab>
          <cv-tab label="Authentication">
            <cv-text-input
                class="filter-input"
                label="Username"
                placeholder="If using an access Token (PAT), leave blank"
                v-model="username"
            />
            <cv-text-input
                type="password"
                class="filter-input"
                label="Password / Access Token (PAT)"
                placeholder="The password for the user or anccess token (PAT) for authentication"
                v-model="passwordOrPAT"
            />
          </cv-tab>
        </cv-tabs>
      </div>
    </Transition>
  </div>
</template>

<script>
import { model } from "@/model.js";
import { connectAndScan } from "@/helpers";
import { ArrowRight24 } from "@carbon/icons-vue";

export default {
  name: "SearchBar",
  data() {
    return {
      model,
      connectAndScan,
      ArrowRight24,
      filterOpen: false,
      gitBranch: null,
      gitSubfolder: null,
      username: null,
      passwordOrPAT: null,
    };
  },
  methods: {
    advancedOptions: function () {
      if (this.filterOpen) {
        return [this.gitBranch, this.gitSubfolder, { username: this.username, passwordOrPAT: this.passwordOrPAT }];
      } else {
        return [null, null, null];
      }
    },
  },
};
</script>

<style scoped>
.search {
  display: flex;
  padding-bottom: 1%;
}
.search-button {
  width: 110px;
}

.search-bar {
  padding-right: 15px;
}
.filter-input {
  padding-top: 10px;
}
/* Transition for advanced options */
.filters-enter-active,
.filters-leave-active {
  transition: all 0.4s;
  /* max-height should be larger than the tallest element: https://stackoverflow.com/questions/42591331/animate-height-on-v-if-in-vuejs-using-transition */
  max-height: 250px;
}
.filters-enter,
.filters-leave-to {
  opacity: 0;
  max-height: 0;
}
</style>
