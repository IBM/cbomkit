<template>
  <div>
    <div class="search">
      <cv-search
        :light="true"
        class="search-bar"
        placeholder="Enter the Git URL to scan"
        v-model="model.codeOrigin.gitLink"
        @keyup.enter="connectAndScan(gitInfo()[0], gitInfo()[1])"
      />
      <cv-button
        class="search-button"
        :icon="ArrowRight24"
        @click="connectAndScan(gitInfo()[0], gitInfo()[1])"
        :disabled="!model.codeOrigin.gitLink"
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
    };
  },
  methods: {
    gitInfo: function () {
      if (this.filterOpen) {
        return [this.gitBranch, this.gitSubfolder];
      } else {
        return [null, null];
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
  max-height: 150px;
}
.filters-enter,
.filters-leave-to {
  opacity: 0;
  max-height: 0px;
}
</style>
